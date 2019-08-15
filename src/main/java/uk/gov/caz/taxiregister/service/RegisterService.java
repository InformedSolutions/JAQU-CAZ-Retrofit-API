package uk.gov.caz.taxiregister.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.VehicleType;
import uk.gov.caz.taxiregister.service.exception.RequiredLicenceTypesAbsentInDbException;

/**
 * Class which is responsible for registering vehicles. It determines which vehicles are to be
 * inserted, updated or deleted in the database when they are being registered.
 */
@Service
@Slf4j
public class RegisterService {

  private static final String MISMATCH_LICENSING_AUTHORITY_NAME_ERROR_MESSAGE_TEMPLATE = "Vehicle's"
      + " licensing authority %s does not match any existing ones.";
  private static final EnumSet<VehicleType> ALLOWABLE_LICENCE_TYPES = EnumSet
      .allOf(VehicleType.class);
  private static final String MISSING_LICENCE_TYPES_IN_DB_MESSAGE_TEMPLATE = "Required licence "
      + "types '%s' not found in the database";

  private final TaxiPhvLicencePostgresRepository vehicleRepository;
  private final LicensingAuthorityPostgresRepository licensingAuthorityRepository;
  private final TaxiPhvTypeRepository taxiPhvTypeRepository;

  /**
   * Creates an instance of {@link RegisterService}.
   *
   * @param vehicleRepository An instance of {@link TaxiPhvLicencePostgresRepository}
   * @param licensingAuthorityRepository An instance of {@link LicensingAuthorityPostgresRepository}
   * @param taxiPhvTypeRepository An instance of {@link TaxiPhvTypeRepository}
   */
  public RegisterService(TaxiPhvLicencePostgresRepository vehicleRepository,
      LicensingAuthorityPostgresRepository licensingAuthorityRepository,
      TaxiPhvTypeRepository taxiPhvTypeRepository) {
    this.vehicleRepository = vehicleRepository;
    this.licensingAuthorityRepository = licensingAuthorityRepository;
    this.taxiPhvTypeRepository = taxiPhvTypeRepository;
  }

  /**
   * Registers {@code licences} in the database for a given {@code uploaderId}.
   *
   * <p>For a given uploader id: - records which are present in the database, but absent in {@code
   * licences} will be deleted - records which are a) present in the database, b) present in {@code
   * licences} and c) CHANGED any of their attributes will be updated - records which are a) present
   * in the database, b) present in {@code licences} and c) did NOT change any of their attributes
   * will NOT be updated - records which are absent in the database, but present in {@code licences}
   * will be inserted</p>
   *
   * @param licences {@link Set} of linces {@link TaxiPhvVehicleLicence} which need to be
   *     registered
   * @param uploaderId An identifier the entity which registers {@code licences}
   */
  RegisterResult register(Set<TaxiPhvVehicleLicence> licences, UUID uploaderId) {
    Preconditions.checkNotNull(licences, "licences cannot be null");
    Preconditions.checkNotNull(uploaderId, "uploaderId cannot be null");
    checkLicenceTypesPresentInDbPrecondition();
    // assertion: for every vehicle in 'taxiPhvVehicleLicences' id is null
    // assertion: for every vehicle in 'taxiPhvVehicleLicences' LicensingAuthority.id is null
    log.info("Registering {} vehicle(s) for uploader '{}' : start", licences.size(), uploaderId);

    Context context = createContext(licences, uploaderId);

    List<ValidationError> validationErrors = validateMatchingLicensingAuthority(licences, context);
    if (!validationErrors.isEmpty()) {
      log.warn("Licensing authority name mismatch detected for {} records, aborting registration",
          validationErrors.size());
      return RegisterResult.failure(validationErrors);
    }

    Set<Integer> toBeDeleted = computeRecordsToBeDeleted(context);
    Set<TaxiPhvVehicleLicence> toBeUpdated = computeRecordsToBeUpdated(context);
    Set<TaxiPhvVehicleLicence> toBeInserted = computeRecordsToBeInserted(context);

    log.info("The number of records to be inserted, updated and deleted respectively: {}/{}/{}",
        toBeInserted.size(), toBeUpdated.size(), toBeDeleted.size());

    vehicleRepository.delete(toBeDeleted);
    vehicleRepository.update(toBeUpdated);
    vehicleRepository.insert(toBeInserted);

    log.info("Registering {} vehicle(s) for uploader '{}' : finish", licences.size(), uploaderId);
    return RegisterResult.success();
  }

  private void checkLicenceTypesPresentInDbPrecondition() {
    Set<VehicleType> licenceTypesFromDb = taxiPhvTypeRepository.findAll();
    if (!licenceTypesFromDb.equals(ALLOWABLE_LICENCE_TYPES)) {
      log.error("The lack of required licence types detected, which prevents from registering ANY "
          + "taxi/phv data. Please insert the required records into the database");
      throw new RequiredLicenceTypesAbsentInDbException(
          String.format(MISSING_LICENCE_TYPES_IN_DB_MESSAGE_TEMPLATE, ALLOWABLE_LICENCE_TYPES)
      );
    }
  }

  private List<ValidationError> validateMatchingLicensingAuthority(
      Set<TaxiPhvVehicleLicence> licences, Context context) {
    Map<String, LicensingAuthority> byName = context.getLicensingAuthorityByName();
    return licences.stream()
        .filter(licence -> !matchesExistingLicensingAuthority(licence, byName))
        .map(this::createLicensingAuthorityMismatchError)
        .collect(Collectors.toList());
  }

  private ValidationError createLicensingAuthorityMismatchError(TaxiPhvVehicleLicence licence) {
    return ValidationError.valueError(
        licence.getVrm(),
        String.format(MISMATCH_LICENSING_AUTHORITY_NAME_ERROR_MESSAGE_TEMPLATE,
            licence.getLicensingAuthority().getName())
    );
  }

  private Context createContext(Set<TaxiPhvVehicleLicence> newLicences, UUID uploaderId) {
    Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentByAttributes = vehicleRepository
        .findByUploaderId(uploaderId)
        .stream()
        .collect(Collectors.toMap(UniqueLicenceAttributes::from, Function.identity()));
    return new Context(
        uploaderId,
        newLicences,
        currentByAttributes,
        licensingAuthorityRepository.findAll()
    );
  }

  //------ DELETE helper methods : begin

  private Set<Integer> computeRecordsToBeDeleted(Context context) {
    Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicences = context
        .getCurrentLicences();

    Set<UniqueLicenceAttributes> toBeDeleted = computeToBeDeleted(
        toUniqueLicenceAttributesSet(context.getNewLicences()),
        currentLicences.keySet()
    );

    return mapToIds(toBeDeleted, currentLicences);
  }

  private Set<UniqueLicenceAttributes> computeToBeDeleted(Set<UniqueLicenceAttributes> newLicences,
      Set<UniqueLicenceAttributes> currentLicences) {
    return Sets.difference(currentLicences, newLicences);
  }

  private Set<Integer> mapToIds(Set<UniqueLicenceAttributes> toBeMapped,
      Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicencesIdByAttrs) {
    return toBeMapped.stream()
        .map(currentLicencesIdByAttrs::get)
        .map(TaxiPhvVehicleLicence::getId)
        .collect(Collectors.toSet());
  }

  private Set<UniqueLicenceAttributes> toUniqueLicenceAttributesSet(
      Set<TaxiPhvVehicleLicence> licences) {
    return licences.stream()
        .map(UniqueLicenceAttributes::from)
        .collect(Collectors.toSet());
  }

  //------ DELETE helper methods : end

  //------ UPDATE helper methods : begin

  private Set<TaxiPhvVehicleLicence> computeRecordsToBeUpdated(Context context) {
    Set<TaxiPhvVehicleLicence> newLicences = context.getNewLicences();

    return newLicences.stream()
        .filter(newVehicle -> presentInDatabase(newVehicle, context))
        .filter(newVehicle -> attributesChanged(newVehicle, context))
        .map(newVehicle -> setDatabaseRelatedAttributesForUpdate(newVehicle, context))
        .collect(Collectors.toSet());
  }

  private boolean presentInDatabase(TaxiPhvVehicleLicence newLicence, Context context) {
    return context.getCurrentLicences().containsKey(UniqueLicenceAttributes.from(newLicence));
  }

  private boolean absentInDatabase(TaxiPhvVehicleLicence newLicence, Context context) {
    return !presentInDatabase(newLicence, context);
  }

  private boolean matchesExistingLicensingAuthority(TaxiPhvVehicleLicence newLicence,
      Map<String, LicensingAuthority> licensingAuthorityByName) {
    boolean matches = licensingAuthorityByName.containsKey(
        newLicence.getLicensingAuthority().getName()
    );
    if (!matches) {
      log.warn("Licence's (vrm='{}') licensing authority '{}' does not exist in the database",
          newLicence.getVrm(), newLicence.getLicensingAuthority().getName());
    }
    return matches;
  }

  private boolean attributesChanged(TaxiPhvVehicleLicence newLicence, Context context) {
    TaxiPhvVehicleLicence currentLicence = context.getCurrentLicences()
        .get(UniqueLicenceAttributes.from(newLicence));

    VehicleType newVehicleType = newLicence.getVehicleType();
    VehicleType currentVehicleType = currentLicence.getVehicleType();
    if (newVehicleType != currentVehicleType) {
      logValueChanged("Vehicle type", currentLicence, currentVehicleType, newVehicleType);
      return true;
    }

    Boolean newWheelchairAccessible = newLicence.getWheelchairAccessible();
    Boolean currentWheelchairAccessible = currentLicence.getWheelchairAccessible();
    if (hasWheelchairAccessibleFlagChanged(newWheelchairAccessible, currentWheelchairAccessible)) {
      logValueChanged("Wheelchair accessible", currentLicence, currentWheelchairAccessible,
          newWheelchairAccessible);
      return true;
    }
    return false;
  }

  private boolean hasWheelchairAccessibleFlagChanged(Boolean newWheelchairAccessible,
      Boolean currentWheelchairAccessible) {
    return !Objects.equals(newWheelchairAccessible, currentWheelchairAccessible);
  }

  private TaxiPhvVehicleLicence setDatabaseRelatedAttributesForUpdate(
      TaxiPhvVehicleLicence newLicence, Context context) {
    LicensingAuthority licensingAuthority = context.getLicensingAuthorityByName()
        .get(newLicence.getLicensingAuthority().getName());

    TaxiPhvVehicleLicence matchingLicenceInDatabase = context.getCurrentLicences().get(
        UniqueLicenceAttributes.from(newLicence));
    return newLicence.toBuilder()
        .id(matchingLicenceInDatabase.getId())
        .uploaderId(context.getUploaderId())
        .licensingAuthority(licensingAuthority)
        .build();
  }

  //------ UPDATE helper methods : end

  //------ INSERT helper methods : begin

  private Set<TaxiPhvVehicleLicence> computeRecordsToBeInserted(Context context) {
    Set<TaxiPhvVehicleLicence> newLicences = context.getNewLicences();

    return newLicences.stream()
        .filter(newVehicle -> absentInDatabase(newVehicle, context))
        .map(newVehicle -> setDatabaseRelatedAttributesForInsert(newVehicle, context))
        .collect(Collectors.toSet());
  }

  private TaxiPhvVehicleLicence setDatabaseRelatedAttributesForInsert(
      TaxiPhvVehicleLicence newLicence, Context context) {
    LicensingAuthority licensingAuthority = context.getLicensingAuthorityByName()
        .get(newLicence.getLicensingAuthority().getName());

    return newLicence.toBuilder()
        .uploaderId(context.getUploaderId())
        .licensingAuthority(licensingAuthority)
        .build();
  }

  //------ INSERT helper methods : end

  private <T> void logValueChanged(String attributeName, TaxiPhvVehicleLicence currentLicence,
      T currentValue, T newValue) {
    log.trace(
        "{} changed for vehicle (id={}, vrm={}), current value: '{}', new value: '{}'",
        attributeName,
        currentLicence.getId(),
        currentLicence.getVrm(),
        currentValue,
        newValue
    );
  }

  @Value
  @Builder
  private static class UniqueLicenceAttributes {

    String vrm;
    LocalDate start;
    LocalDate end;
    String licensingAuthorityName;
    String licencePlateNumber;

    public static UniqueLicenceAttributes from(TaxiPhvVehicleLicence licence) {
      return UniqueLicenceAttributes.builder()
          .vrm(licence.getVrm())
          .start(licence.getLicenseDates().getStart())
          .end(licence.getLicenseDates().getEnd())
          .licensingAuthorityName(licence.getLicensingAuthority().getName())
          .licencePlateNumber(licence.getLicensePlateNumber())
          .build();
    }
  }

  @Value
  private static class Context {

    UUID uploaderId;
    Set<TaxiPhvVehicleLicence> newLicences;
    Map<UniqueLicenceAttributes, TaxiPhvVehicleLicence> currentLicences;
    Map<String, LicensingAuthority> licensingAuthorityByName;
  }
}
