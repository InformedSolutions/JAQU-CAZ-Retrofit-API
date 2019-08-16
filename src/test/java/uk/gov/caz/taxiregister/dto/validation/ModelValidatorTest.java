package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

class ModelValidatorTest {
  private static final String ANY_VRM = "ZC62OMB";

  private ModelValidator validator = new ModelValidator();

  @Nested
  class MandatoryField {

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenModelIsNull() {
        // given
        int lineNumber = 14;
        String licencePlateNumber = null;
        RetrofittedVehicleDto licence = createRetrofittedVehicleWithLineNumber(licencePlateNumber, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, ModelValidator.MISSING_MODEL_MESSAGE, lineNumber)
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenModelIsNull() {
        // given
        String licencePlateNumber = null;
        RetrofittedVehicleDto licence = createRetrofittedVehicle(licencePlateNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, ModelValidator.MISSING_MODEL_MESSAGE)
        );
      }
    }
  }
  @Nested
  class Format {

    @Nested
    class WithLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"", "tooLooooooooooooooooooooongModel"})
      public void shouldReturnMissingFieldErrorWhenModelIsInvalid(String invalidModel) {
        // given
        int lineNumber = 59;
        RetrofittedVehicleDto licence = createRetrofittedVehicleWithLineNumber(invalidModel, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRM,
                invalidFormatError(invalidModel),
                lineNumber
            )
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"", "tooLooooooooooooooooooooongModel"})
      public void shouldReturnMissingFieldErrorWhenModelIsInvalid(String invalidModel) {
        // given
        RetrofittedVehicleDto licence = createRetrofittedVehicle(invalidModel);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, invalidFormatError(invalidModel))
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
        .vrn(ANY_VRM)
        .model(model)
        .build();
  }

  private RetrofittedVehicleDto createRetrofittedVehicleWithLineNumber(String model, int lineNumber) {
    return RetrofittedVehicleDto.builder()
        .vrn(ANY_VRM)
        .model(model)
        .lineNumber(lineNumber)
        .build();
  }
}