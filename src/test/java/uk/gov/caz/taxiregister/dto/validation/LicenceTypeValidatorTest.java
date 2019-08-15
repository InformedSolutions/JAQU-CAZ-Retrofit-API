package uk.gov.caz.taxiregister.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

class LicenceTypeValidatorTest {
  private static final String ANY_VRM = "ZC62OMB";

  private LicenceTypeValidator validator = new LicenceTypeValidator();

  @Nested
  class MandatoryField {

    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenLicenceTypeIsNull() {
        // given
        String licenceType = null;
        VehicleDto licence = createLicence(licenceType);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicenceTypeValidator.MISSING_LICENCE_TYPE_MESSAGE)
        );
      }
    }

    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenLicenceTypeIsNull() {
        // given
        int lineNumber = 45;
        String licenceType = null;
        VehicleDto licence = createLicenceWithLineNumber(licenceType, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRM, LicenceTypeValidator.MISSING_LICENCE_TYPE_MESSAGE, lineNumber)
        );
      }
    }
  }
  @Nested
  class Format {

    @Nested
    class WithoutLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"TAXI", "tAxI", "phv", "PhV", "unknown"})
      public void shouldReturnValueErrorWhenLicenceTypeIsInvalid(String invalidLicenceType) {
        // given
        VehicleDto licence = createLicence(invalidLicenceType);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, LicenceTypeValidator.INVALID_LICENCE_TYPE_FORMAT_MESSAGE)
        );
      }
    }

    @Nested
    class WithLineNumber {
      @ParameterizedTest
      @ValueSource(strings = {"TAXI", "tAxI", "phv", "PhV", "unknown"})
      public void shouldReturnValueErrorWhenLicenceTypeIsInvalid(String invalidLicenceType) {
        // given
        int lineNumber = 31;
        VehicleDto licence = createLicenceWithLineNumber(invalidLicenceType, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(licence);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRM, LicenceTypeValidator.INVALID_LICENCE_TYPE_FORMAT_MESSAGE, lineNumber)
        );
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"taxi", "PHV"})
  public void shouldReturnEmptyListWhenLicenceTypeIsValid(String licenceType) {
    // given
    VehicleDto licence = createLicence(licenceType);

    // when
    List<ValidationError> validationErrors = validator.validate(licence);

    // then
    then(validationErrors).isEmpty();
  }

  private VehicleDto createLicence(String licenceType) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .taxiOrPhv(licenceType)
        .build();
  }

  private VehicleDto createLicenceWithLineNumber(String licenceType, int lineNumber) {
    return VehicleDto.builder()
        .vrm(ANY_VRM)
        .taxiOrPhv(licenceType)
        .lineNumber(lineNumber)
        .build();
  }
}