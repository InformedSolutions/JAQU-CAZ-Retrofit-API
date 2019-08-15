package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class LicensingAuthorityNameValidator implements LicenceValidator {

  @VisibleForTesting
  static final String MISSING_LICENCING_AUTHORITY_NAME_MESSAGE = "Vehicle does not include the "
      + "'licensingAuthorityName' field which is mandatory.";

  @VisibleForTesting
  static final String INVALID_LICENCING_AUTHORITY_NAME_MESSAGE_TEMPLATE = "'licensingAuthorityName'"
      + " should have from 1 to %d characters instead of %d.";

  @VisibleForTesting
  static final int MAX_LENGTH = 50;

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicensingAuthorityNameErrorMessageResolver errorMessageResolver =
        new LicensingAuthorityNameErrorMessageResolver(vehicleDto);

    String vrm = vehicleDto.getVrm();
    String licensingAuthorityName = vehicleDto.getLicensingAuthorityName();

    if (licensingAuthorityName == null) {
      validationErrorsBuilder.add(errorMessageResolver.missing(vrm));
    }

    if (licensingAuthorityName != null
        && (licensingAuthorityName.isEmpty() || licensingAuthorityName.length() > MAX_LENGTH)) {
      validationErrorsBuilder.add(errorMessageResolver.invalidFormat(vrm,
          licensingAuthorityName.length()));
    }

    return validationErrorsBuilder.build();
  }

  private static class LicensingAuthorityNameErrorMessageResolver extends
      ValidationErrorResolver {

    private LicensingAuthorityNameErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String vrm) {
      return missingFieldError(vrm, MISSING_LICENCING_AUTHORITY_NAME_MESSAGE);
    }

    private ValidationError invalidFormat(String vrm, int licensingAuthorityNameLength) {
      return valueError(vrm, invalidFormatMessage(licensingAuthorityNameLength));
    }

    private String invalidFormatMessage(int licensingAuthorityNameLength) {
      return String.format(INVALID_LICENCING_AUTHORITY_NAME_MESSAGE_TEMPLATE, MAX_LENGTH,
          licensingAuthorityNameLength);
    }
  }
}
