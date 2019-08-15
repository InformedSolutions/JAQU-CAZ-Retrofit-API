package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class LicenceTypeValidator implements LicenceValidator {

  private static final Set<String> ALLOWABLE_VALUES = ImmutableSet.of("taxi", "PHV");

  @VisibleForTesting
  static final String MISSING_LICENCE_TYPE_MESSAGE = "Vehicle does not include the 'taxiOrPHV' "
      + "field which is mandatory.";

  @VisibleForTesting
  static final String INVALID_LICENCE_TYPE_FORMAT_MESSAGE = String.format("Invalid value of '"
      + "taxiOrPHV' field. Allowable values: %s.", ALLOWABLE_VALUES);

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicenceTypeErrorMessageResolver errorMessageResolver = new LicenceTypeErrorMessageResolver(
        vehicleDto);

    String vrm = vehicleDto.getVrm();
    String licenceType = vehicleDto.getTaxiOrPhv();

    if (licenceType == null) {
      validationErrorsBuilder.add(errorMessageResolver.missing(vrm));
    }

    if (licenceType != null && !ALLOWABLE_VALUES.contains(licenceType)) {
      validationErrorsBuilder.add(errorMessageResolver.invalidFormat(vrm));
    }

    return validationErrorsBuilder.build();
  }

  private static class LicenceTypeErrorMessageResolver extends ValidationErrorResolver {

    private LicenceTypeErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String vrm) {
      return missingFieldError(vrm, MISSING_LICENCE_TYPE_MESSAGE);
    }

    private ValidationError invalidFormat(String vrm) {
      return valueError(vrm, INVALID_LICENCE_TYPE_FORMAT_MESSAGE);
    }
  }
}
