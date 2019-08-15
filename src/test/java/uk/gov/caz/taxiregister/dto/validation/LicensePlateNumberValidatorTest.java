package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

class LicensePlateNumberValidatorTest {
  private static final String ANY_VRM = "ZC62OMB";

  private LicensePlateNumberValidator validator = new LicensePlateNumberValidator();

  @Nested
  class MandatoryField {

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenPlateNumberIsNull() {
        // given
        int lineNumber = 14;
        String licencePlateNumber = null;
        VehicleDto licence = createLicenceWithLineNumber(licencePlateNumber, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensePlateNumberValidator.MISSING_LICENCE_PLATE_NUMBER_MESSAGE, lineNumber)
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenPlateNumberIsNull() {
        // given
        String licencePlateNumber = null;
        VehicleDto licence = createLicence(licencePlateNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicensePlateNumberValidator.MISSING_LICENCE_PLATE_NUMBER_MESSAGE)
        );
      }
    }
  }
  @Nested
  class Format {

    @Nested
    class WithLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"", "tooLongLicencePlateNumber"})
      public void shouldReturnValueErrorWhenLicencePlateNumberIsInvalid(String invalidLicencePlateNumber) {
        // given
        int lineNumber = 59;
        VehicleDto licence = createLicenceWithLineNumber(invalidLicencePlateNumber, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(
                ANY_VRM,
                invalidFormatError(invalidLicencePlateNumber),
                lineNumber
            )
        );
      }
    }

    @Nested
    class WithoutLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"", "tooLongLicencePlateNumber"})
      public void shouldReturnValueErrorWhenLicencePlateNumberIsInvalid(String invalidLicencePlateNumber) {
        // given
        VehicleDto licence = createLicence(invalidLicencePlateNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, invalidFormatError(invalidLicencePlateNumber))
        );
      }
    }
  }

  private String invalidFormatError(String invalidLicencePlateNumber) {
    return String.format(
        LicensePlateNumberValidator.INVALID_PLATE_NUMBER_MESSAGE_TEMPLATE,
        LicensePlateNumberValidator.MAX_LENGTH,
        invalidLicencePlateNumber.length()
    );
  }

  private VehicleDto createLicence(String licencePlateNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .licensePlateNumber(licencePlateNumber)
        .build();
  }

  private VehicleDto createLicenceWithLineNumber(String licencePlateNumber, int lineNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .licensePlateNumber(licencePlateNumber)
        .lineNumber(lineNumber)
        .build();
  }
}