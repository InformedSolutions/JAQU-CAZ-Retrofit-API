package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.VehicleType;

@Component
public class VehicleToLicenceConverter {

  /**
   * Converts the passed list of {@link VehicleDto}s to {@link ConversionResults}.
   */
  public ConversionResults convert(List<VehicleDto> vehicles) {
    List<ConversionResult> conversionResultList = vehicles.stream()
        .map(this::toLicence)
        .collect(Collectors.toList());
    return ConversionResults.from(conversionResultList);
  }

  /**
   * Converts the passed instance of {@link VehicleDto} to {@link ConversionResult}.
   */
  @VisibleForTesting
  ConversionResult toLicence(VehicleDto vehicleDto) {
    List<ValidationError> validationResult = vehicleDto.validate();
    if (validationResult.isEmpty()) {
      TaxiPhvVehicleLicence licence = TaxiPhvVehicleLicence.builder()
          .vrm(vehicleDto.getVrm())
          .licenseDates(
              new LicenseDates(
                  LocalDate.parse(vehicleDto.getStart()), LocalDate.parse(vehicleDto.getEnd())
              )
          )
          .vehicleType(VehicleType.valueOf(vehicleDto.getTaxiOrPhv().toUpperCase()))
          .licensingAuthority(
              LicensingAuthority.withNameOnly(vehicleDto.getLicensingAuthorityName()))
          .licensePlateNumber(vehicleDto.getLicensePlateNumber())
          .wheelchairAccessible(vehicleDto.getWheelchairAccessibleVehicle())
          .build();
      return ConversionResult.success(licence);
    }
    return ConversionResult.failure(validationResult);
  }
}