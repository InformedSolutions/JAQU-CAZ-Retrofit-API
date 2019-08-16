package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class VehicleCategoryValidator implements RetrofittedVehicleValidator {

  @VisibleForTesting
  static final String MISSING_VEHICLE_CATEGORY_MESSAGE = "Vehicle does not include the "
      + "'vehicleCategory' field which is mandatory.";

  @VisibleForTesting
  static final String INVALID_VEHICLE_CATEGORY_MESSAGE_TEMPLATE = "'vehicleCategory'"
      + " should have from 1 to %d characters instead of %d.";

  @VisibleForTesting
  static final int MAX_LENGTH = 50;

  @Override
  public List<ValidationError> validate(RetrofittedVehicleDto retrofittedVehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    VehicleCategoryErrorResolver errorResolver =
        new VehicleCategoryErrorResolver(retrofittedVehicleDto);

    String vrn = retrofittedVehicleDto.getVrn();
    String vehicleCategory = retrofittedVehicleDto.getVehicleCategory();

    if (vehicleCategory == null) {
      validationErrorsBuilder.add(errorResolver.missing(vrn));
    }

    if (vehicleCategory != null
        && (vehicleCategory.isEmpty() || vehicleCategory.length() > MAX_LENGTH)) {
      validationErrorsBuilder.add(errorResolver.invalidFormat(vrn, vehicleCategory.length()));
    }

    return validationErrorsBuilder.build();
  }

  private static class VehicleCategoryErrorResolver extends ValidationErrorResolver {

    private VehicleCategoryErrorResolver(RetrofittedVehicleDto retrofittedVehicleDto) {
      super(retrofittedVehicleDto);
    }

    private ValidationError missing(String vrm) {
      return missingFieldError(vrm, MISSING_VEHICLE_CATEGORY_MESSAGE);
    }

    private ValidationError invalidFormat(String vrm, int vehicleCategoryLength) {
      return valueError(vrm, invalidFormatMessage(vehicleCategoryLength));
    }

    private String invalidFormatMessage(int vehicleCategoryLength) {
      return String.format(INVALID_VEHICLE_CATEGORY_MESSAGE_TEMPLATE, MAX_LENGTH,
          vehicleCategoryLength);
    }
  }
}
