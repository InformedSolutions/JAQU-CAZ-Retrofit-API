package uk.gov.caz.retrofit.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

class DateOfRetrofitInstallationValidatorTest {

  private static final String ANY_VRN = "ZC62OMB";

  private final DateOfRetrofitInstallationValidator validator = new DateOfRetrofitInstallationValidator();

  @Nested
  class MandatoryFields {
    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenDateIsNull() {
        // given
        int lineNumber = 71;
        String date = null;
        RetrofittedVehicleDto vehicle = createRetrofittedVehicleWithLineNumber(date, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(vehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRN, DateOfRetrofitInstallationValidator.MISSING_DATE_MESSAGE, lineNumber));
      }
    }
    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnMissingFieldErrorWhenDateIsNull() {
        // given
        String date = null;
        RetrofittedVehicleDto vehicle = createRetrofittedVehicle(date);

        // when
        List<ValidationError> validationErrors = validator.validate(vehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(ANY_VRN, DateOfRetrofitInstallationValidator.MISSING_DATE_MESSAGE));
      }
    }
  }
  @Nested
  class Format {
    @Nested
    class WithoutLineNumber {
      @Test
      public void shouldReturnValueErrorsWhenDateHaveInvalidFormat() {
        // given
        String date = "2019-05-17-01";
        RetrofittedVehicleDto vehicle = createRetrofittedVehicle(date);

        // when
        List<ValidationError> validationErrors = validator.validate(vehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRN, DateOfRetrofitInstallationValidator.INVALID_DATE_FORMAT_MESSAGE)
        );
      }
    }
    @Nested
    class WithLineNumber {
      @Test
      public void shouldReturnValueErrorsWhenDateHaveInvalidFormat() {
        // given
        int lineNumber = 74;
        String date = "2019-05-17-01";
        RetrofittedVehicleDto vehicle = createRetrofittedVehicleWithLineNumber(date, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(vehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(ANY_VRN, DateOfRetrofitInstallationValidator.INVALID_DATE_FORMAT_MESSAGE, lineNumber)
        );
      }
    }
  }

  @Test
  public void shouldReturnEmptyListWhenDateIsValid() {
    // given
    String date = "2019-05-17";
    RetrofittedVehicleDto vehicle = createRetrofittedVehicle(date);

    // when
    List<ValidationError> validationErrors = validator.validate(vehicle);

    // then
    then(validationErrors).isEmpty();
  }

  private RetrofittedVehicleDto createRetrofittedVehicle(String dateOfRetrofitInstallation) {
    return RetrofittedVehicleDto.builder()
        .vrn(ANY_VRN)
        .dateOfRetrofitInstallation(dateOfRetrofitInstallation)
        .build();
  }

  private RetrofittedVehicleDto createRetrofittedVehicleWithLineNumber(String dateOfRetrofitInstallation, int lineNumber) {
    return RetrofittedVehicleDto.builder()
        .vrn(ANY_VRN)
        .dateOfRetrofitInstallation(dateOfRetrofitInstallation)
        .lineNumber(lineNumber)
        .build();
  }
}