package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

class VehicleCategoryValidatorTest {
  private static final String ANY_VRN = "ZC62OMB";

  private VehicleCategoryValidator validator = new VehicleCategoryValidator();

  @Nested
  class MandatoryField {
    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenCategoryIsNull() {
        // given
        String vehicleCategory = null;
        RetrofittedVehicleDto licence = createRetrofittedVehicle(vehicleCategory);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(
                ANY_VRN,
                VehicleCategoryValidator.MISSING_VEHICLE_CATEGORY_MESSAGE
            )
        );
      }
    }

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenCategoryIsNull() {
        // given
        int lineNumber = 63;
        String vehicleCategory = null;
        RetrofittedVehicleDto licence = createRetrofittedVehicleWithLineNumber(vehicleCategory, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRN, VehicleCategoryValidator.MISSING_VEHICLE_CATEGORY_MESSAGE, lineNumber)
        );
      }
    }
  }

  @Nested
  class Format {
    @Nested
    class WithoutLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"", "tooooooooooooLooooooooooooongLicensingAuthorityName"})
      public void shouldReturnValueErrorWhenCategoryIsInvalid(String invalidVehicleCategory) {
        // given
        RetrofittedVehicleDto licence = createRetrofittedVehicle(invalidVehicleCategory);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

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
      @ParameterizedTest
      @ValueSource(strings = {"", "tooooooooooooLooooooooooooongLicensingAuthorityName"})
      public void shouldReturnValueErrorWhenCategoryIsInvalid(String invalidVehicleCategory) {
        // given
        int lineNumber = 90;
        RetrofittedVehicleDto licence = createRetrofittedVehicleWithLineNumber(invalidVehicleCategory, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

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