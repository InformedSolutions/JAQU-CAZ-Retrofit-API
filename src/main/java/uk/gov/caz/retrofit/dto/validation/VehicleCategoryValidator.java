package uk.gov.caz.retrofit.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

public class VehicleCategoryValidator implements RetrofittedVehicleValidator {

  @VisibleForTesting
  static final String INVALID_VEHICLE_CATEGORY_MESSAGE_TEMPLATE = "'vehicleCategory'"
      + " should have from 1 to %d characters instead of %d.";

  @VisibleForTesting
  static final int MAX_LENGTH = 40;

  @Override
  public List<ValidationError> validate(RetrofittedVehicleDto retrofittedVehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    VehicleCategoryErrorResolver errorResolver =
        new VehicleCategoryErrorResolver(retrofittedVehicleDto);

    String vrn = retrofittedVehicleDto.getVrn();
    String vehicleCategory = retrofittedVehicleDto.getVehicleCategory();

    if (!Strings.isNullOrEmpty(vehicleCategory) && vehicleCategory.length() > MAX_LENGTH) {
      validationErrorsBuilder.add(errorResolver.invalidFormat(vrn, vehicleCategory.length()));
    }

    return validationErrorsBuilder.build();
  }

  private static class VehicleCategoryErrorResolver extends ValidationErrorResolver {

    private VehicleCategoryErrorResolver(RetrofittedVehicleDto retrofittedVehicleDto) {
      super(retrofittedVehicleDto);
    }

    private ValidationError invalidFormat(String vrn, int vehicleCategoryLength) {
      return valueError(vrn, invalidFormatMessage(vehicleCategoryLength));
    }

    private String invalidFormatMessage(int vehicleCategoryLength) {
      return String.format(INVALID_VEHICLE_CATEGORY_MESSAGE_TEMPLATE, MAX_LENGTH,
          vehicleCategoryLength);
    }
  }
}
