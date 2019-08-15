package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class LicensePlateNumberValidator implements LicenceValidator {

  @VisibleForTesting
  static final int MAX_LENGTH = 15;

  @VisibleForTesting
  static final String MISSING_LICENCE_PLATE_NUMBER_MESSAGE = "Vehicle does not include the "
      + "'licensePlateNumber' field which is mandatory.";

  @VisibleForTesting
  static final String INVALID_PLATE_NUMBER_MESSAGE_TEMPLATE = "'licensePlateNumber' should have "
      + "from 1 to %d characters instead of %d.";

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicencePlateNoErrorMessageResolver errorMessageResolver =
        new LicencePlateNoErrorMessageResolver(vehicleDto);

    String vrm = vehicleDto.getVrm();
    String plateNumber = vehicleDto.getLicensePlateNumber();

    if (plateNumber == null) {
      validationErrorsBuilder.add(errorMessageResolver.missing(vrm));
    }

    if (plateNumber != null && (plateNumber.isEmpty() || plateNumber.length() > MAX_LENGTH)) {
      validationErrorsBuilder.add(errorMessageResolver.invalidFormat(vrm, plateNumber.length()));
    }

    return validationErrorsBuilder.build();
  }

  private static class LicencePlateNoErrorMessageResolver extends ValidationErrorResolver {

    private LicencePlateNoErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String vrm) {
      return missingFieldError(vrm, MISSING_LICENCE_PLATE_NUMBER_MESSAGE);
    }

    private ValidationError invalidFormat(String vrm, int plateNumberLength) {
      return valueError(vrm, invalidPlateNumberMessage(plateNumberLength));
    }

    private String invalidPlateNumberMessage(int plateNumberLength) {
      return String.format(INVALID_PLATE_NUMBER_MESSAGE_TEMPLATE, MAX_LENGTH, plateNumberLength);
    }
  }
}
