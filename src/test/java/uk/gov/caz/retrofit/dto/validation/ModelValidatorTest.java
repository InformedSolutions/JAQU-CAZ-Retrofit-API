package uk.gov.caz.retrofit.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

class ModelValidatorTest {
  private static final String ANY_VRN = "ZC62OMB";

  private ModelValidator validator = new ModelValidator();

  @Nested
  class Format {

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenModelIsInvalid() {
        // given
        String invalidModel = "tooLooooooooooooooooooooongModel";
        int lineNumber = 59;
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicleWithLineNumber(invalidModel, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRN,
                invalidFormatError(invalidModel),
                lineNumber
            )
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenModelIsInvalid() {
        // given
        String invalidModel = "tooLooooooooooooooooooooongModel";
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicle(invalidModel);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRN, invalidFormatError(invalidModel))
        );
      }
    }
  }

  private String invalidFormatError(String invalidModel) {
    return String.format(
        ModelValidator.INVALID_MODEL_MESSAGE_TEMPLATE,
        ModelValidator.MAX_LENGTH,
        invalidModel.length()
    );
  }

  private RetrofittedVehicleDto createRetrofittedVehicle(String model) {
    return RetrofittedVehicleDto.builder()
        .vrn(ANY_VRN)
        .model(model)
        .build();
  }

  private RetrofittedVehicleDto createRetrofittedVehicleWithLineNumber(String model, int lineNumber) {
    return RetrofittedVehicleDto.builder()
        .vrn(ANY_VRN)
        .model(model)
        .lineNumber(lineNumber)
        .build();
  }
}