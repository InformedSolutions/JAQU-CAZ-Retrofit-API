package uk.gov.caz.retrofit.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

class VehicleCategoryValidatorTest {
  private static final String ANY_VRN = "ZC62OMB";

  private VehicleCategoryValidator validator = new VehicleCategoryValidator();

  @Nested
  class Format {
    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnValueErrorWhenCategoryIsInvalid() {
        // given
        String invalidVehicleCategory = "tooooooooooooLooooooooooooongLicensingAuthorityName";
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicle(invalidVehicleCategory);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRN,
                invalidFormatError(invalidVehicleCategory)
            )
        );
      }
    }

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnValueErrorWhenCategoryIsInvalid() {
        // given
        String invalidVehicleCategory = "tooooooooooooLooooooooooooongLicensingAuthorityName";
        int lineNumber = 90;
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicleWithLineNumber(invalidVehicleCategory, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRN,
                invalidFormatError(invalidVehicleCategory),
                lineNumber
            )
        );
      }
    }
  }

  private String invalidFormatError(String invalidLicensingAuthorityName) {
    return String.format(
        VehicleCategoryValidator.INVALID_VEHICLE_CATEGORY_MESSAGE_TEMPLATE,
        VehicleCategoryValidator.MAX_LENGTH,
        invalidLicensingAuthorityName.length()
    );
  }

  private RetrofittedVehicleDto createRetrofittedVehicle(String vehicleCategory) {
    return RetrofittedVehicleDto.builder()
        .vrn(ANY_VRN)
        .vehicleCategory(vehicleCategory)
        .build();
  }

  private RetrofittedVehicleDto createRetrofittedVehicleWithLineNumber(String vehicleCategory, int lineNumber) {
    return RetrofittedVehicleDto.builder()
        .vrn(ANY_VRN)
        .vehicleCategory(vehicleCategory)
        .lineNumber(lineNumber)
        .build();
  }
}