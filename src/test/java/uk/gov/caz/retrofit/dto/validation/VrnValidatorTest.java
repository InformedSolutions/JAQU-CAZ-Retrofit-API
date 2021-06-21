package uk.gov.caz.retrofit.dto.validation;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

class VrnValidatorTest {

  private VrnValidator validator = new VrnValidator();

  @Nested
  class MandatoryField {

    @Nested
    class WithLineNumber {

      @Test
      public void shouldReturnMissingFieldErrorWhenVrnIsNull() {
        // given
        int lineNumber = 91;
        String vrn = null;
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicleWithLineNumber(vrn,
            lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(
                vrn,
                VrnValidator.MISSING_VRN_MESSAGE,
                lineNumber
            )
        );
      }
    }

    @Nested
    class WithoutLineNumber {

      @Test
      public void shouldReturnMissingFieldErrorWhenVrnIsNull() {
        // given
        String vrn = null;
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicle(vrn);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.missingFieldError(
                vrn,
                VrnValidator.MISSING_VRN_MESSAGE
            )
        );
      }
    }
  }

  @Nested
  class Format {

    @Nested
    class WithLineNumber {

      @ParameterizedTest
      @ValueSource(strings = {"", "tooLongVrn"})
      public void shouldReturnValueErrorWhenVrnIsBlankOrTooLong(String vrn) {
        // given
        int lineNumber = 87;
        RetrofittedVehicleDto vehicle = createRetrofittedVehicleWithLineNumber(vrn, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(vehicle);

        // then
        then(validationErrors).contains(
            ValidationError.valueError(
                vrn,
                String.format(
                    VrnValidator.INVALID_LENGTH_MESSAGE_TEMPLATE,
                    VrnValidator.MAX_LENGTH,
                    vrn.length()
                ),
                lineNumber
            )
        );
      }

      @ParameterizedTest
      @ValueSource(strings = {"9A99A99", "9A9A99", "9AAAA9"})
      public void shouldRejectVrnsWithInvalidFormat(String invalidVrn) {
        // given
        int lineNumber = 67;
        RetrofittedVehicleDto retrofittedVehicle =
            createRetrofittedVehicleWithLineNumber(invalidVrn, lineNumber);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError
                .valueError(invalidVrn, VrnValidator.INVALID_VRN_FORMAT_MESSAGE, lineNumber)
        );
      }
    }

    @Nested
    class WithoutLineNumber {

      @ParameterizedTest
      @ValueSource(strings = {"", "tooLongVrn"})
      public void shouldReturnValueErrorWhenVrnIsBlankOrTooLong(String vrn) {
        // given
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicle(vrn);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).contains(
            ValidationError.valueError(
                vrn,
                String.format(
                    VrnValidator.INVALID_LENGTH_MESSAGE_TEMPLATE,
                    VrnValidator.MAX_LENGTH,
                    vrn.length()
                )
            )
        );
      }

      @ParameterizedTest
      @ValueSource(strings = {
          "9A99A99", "9A9A99", "9AAAA9", "9A99AA9", "99AAA99", "99AAAA9", "A9AAA99", "A9A9A9A",
          "A9A9A99", "A9AAAA9", "99A999", "A9A9AAA", "99A9A9", "9AA9AA9", "AAA9A99", "999A999",
          "AAA9AA9", "A9AA99", "A99A999", "A999A9", "9AA999", "A99999A", "A99A9AA", "99A99",
          "9AAA9", "A99A9", "A99A9A9", "9A9999A", "99AA9A", "9A999A", "9A99999", "9AA9A9",
          "A9999AA",
          "99AA9", "9A999AA", "999A9A", "99AAAA", "99AA99A", "A99A", "AA9AA9A", "AA999AA", "AAAA9A",
          "AA9AAAA", "AA9A9A", "AAAAAA", "A9A999A", "A9AA9AA", "A9A99AA", "9AAAA9A", "9AA99AA",
          "999AA9", "999AA99", "999AAA9", "A9A99A", "A99AA99", "A999A99", "A99AAA9", "9AA99A9",
          "AA9AA", "9AAAAA9", "9A9AAA", "9A9AA9", "9999", "99A9A9A", "99AAAAA", "99A9", "AA99AA9",
          "99A9AAA", "9AA99", "999A9", "99999A", "999A99A", "A99999", "999A9AA", "999999A",
          "A9AA9A",
          "AAA9AA", "AAA9A9", "A99A9A", "A9AAAA", "A9AA99A", "9AAA99A", "9A9", "A9A9A", "AAAAA",
          "9A9AA9A", "9AAA9AA", "9A99AA", "AAAA999", "9999999", "999A9A9", "AAAA9AA", "9A99A",
          "999A99", "99AAA9", "9A9AA", "9A9AAA9", "AA9AA99", "99A9999", "99AA9A9", "AAAA99",
          "9999A9A", "A9A9", "AA99A99", "AA9AAA9", "99A99AA", "99A99A9", "AAAAA9", "A9A9999",
          "A999A9A", "AA99A", "A9A99A9", "9AAAA99", "A9AAAAA", "9AA9A9A", "9AAAAAA", "99",
          "AAAAA99", "9AA9AAA", "AA9A", "AAA9A9A", "AAAAAA9", "A9A999", "9A9A", "AAAA", "AAA9AAA",
          "9AA9A", "A999AA", "A99A99A", "9A9A99A", "AA9A9", "9AA99A", "99A9A", "9AAAA", "9A9A9AA",
          "9AA9AA", "99A9A99", "AA9A99A", "AA9A999", "AA9999A", "AA9A9AA", "99A9AA9", "9AA9",
          "AA999A", "AA99AA", "A9A9AA9", "A9A", "AAAA99A", "AAA", "99999AA", "99999A9", "A99A99",
          "A9AAA9", "A999999", "9A9999", "A99AA9", "A9999A9", "9A9AA99", "9A999A9", "9AAA99",
          "9A99A9", "99AA999", "9A9A9A", "9AAAAA", "AAAA9A9", "9A99AAA", "A9AA", "AA99A9A",
          "99AAA9A",
          "AAA99A9", "AA9A99", "A9AAA9A", "AA9AAA", "AA9AA9", "99999", "9A9A9", "99A99A", "99A9AA",
          "9999A99", "AA", "9999AA9", "A9999A", "A9A9AA", "A9A9A9", "A999A", "A999AA9", "9AA9A99",
          "999", "A99AA", "9A99", "9A9A999", "999999", "9A9A9A9", "99AA99", "9999A9", "AA99999",
          "AA9A9A9", "999AA9A", "99AA9AA", "99A999A", "AA999A9", "A9AA999", "AA99A9", "999AAAA",
          "9AAA999", "A99AA9A", "A9A99", "AAAA9", "A9AA9A9", "9AA999A", "9AA9999", "9AAA9A",
          "9AAA9A9", "A99AAAA", "9A99A9A", "A9AA9", "9A9AAAA", "AAAAA9A", "AAA99AA", "AAAAAAA",
          "9A999", "ab53ab%", "C111999", "AB", "45", "ABG", "452", "TABG", "4521", "TAFBG",
          "45921", "AHTDSE", "A123B5", "4111929", "C1119C9", "0880V"
      })
      public void shouldRejectVrnsWithInvalidFormat(String invalidVrn) {
        // given
        RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicle(invalidVrn);

        // when
        List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

        // then
        then(validationErrors).containsExactly(
            ValidationError.valueError(invalidVrn, VrnValidator.INVALID_VRN_FORMAT_MESSAGE)
        );
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "A9", "A99", "A999", "A9999", "AA9", "AA99", "AA999", "AA9999", "AAA9", "AAA99", "AAA999",
        "AAA9999", "AAA9A", "AAA99A", "AAA999A", "9A", "9AA", "9AAA", "99A", "99AA", "99AAA",
        "999A", "999AA", "999AAA", "9999A", "9999AA", "A9AAA", "A99AAA", "A999AAA", "AA99AAA",
        "ABC123", "A123BCD", "GAD975C", "ZEA1436", "SK12JKL", "G5", "6W",
        "JK4", "P91", "9RA", "81U", "KAT7", "Y478", "LK31", "8RAD", "87KJ", "111Z", "A7CUD",
        "VAR7A", "FES23", "PG227", "30JFA", "868BO", "1289J", "B8659", "K97LUK", "MAN07U", "546BAR",
        "JU0043", "8839GF",
        "UI1", "UI12", "UI123", "UI1234",
        "ABC9", "ABC98", "ABC987",
        "1AGH", "10AGH", "100AGH",
        "1B", "1BZ", "10Z", "10BZ",
        "IAM000A","IAM00A", "IAM0A",
        "A0MOB", "A01MOB", "A012MOB",
        "XY09BMW",
        "XYZ2021"
    })
    public void shouldAcceptValidVrn(String validVrn) {
      // given
      RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicle(validVrn);

      // when
      List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

      // then
      then(validationErrors).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "007 2CN", "030-NRF", "011-BEV", "0QXE 67", "003-340", "039-HHV", "094-MSR", "018WMO",
        "0EBR 59", "072-CQR", "000 3RR", "0888 XT", "010 0UX", "006-YZG", "06AA659", "068 OIF",
        "08-II36", "0EZ 478", "0XM6622", "0FU 350", "0EI A37", "069 WQD", "068A6", "0RS76",
        "058 IVR", "036 JJQ", "0-6841N", "08O•604", "041-YXR", "0-W4523", "025KA", "052 DPJ",
        "079TX", "003KCJ", "0284", "0BR2836", "084 RWH", "07-DT54", "07F 231", "019 UGR", "028 RXI",
        "0628 EZ", "0K085", "0-B7773", "02Q J42", "0BY 944", "039 WON", "0T976", "0-5996Z", "0651",
        "09F•492", "0GYV 72", "020-BWX", "020-568", "039 DG6", "0PGQ 42", "028 HQH"
    })
    public void shouldPreventUploadWithZeroes(String zeroLeadingVrn) {
      // given
      RetrofittedVehicleDto retrofittedVehicle = createRetrofittedVehicle(zeroLeadingVrn);

      // when
      List<ValidationError> validationErrors = validator.validate(retrofittedVehicle);

      // then
      then(validationErrors).containsExactly(
          ValidationError.valueError(zeroLeadingVrn, VrnValidator.INVALID_VRN_FORMAT_MESSAGE)
      );

    }
  }


  private RetrofittedVehicleDto createRetrofittedVehicle(String vrn) {
    return RetrofittedVehicleDto.builder()
        .vrn(vrn)
        .build();
  }

  private RetrofittedVehicleDto createRetrofittedVehicleWithLineNumber(String vrn, int lineNumber) {
    return RetrofittedVehicleDto.builder()
        .vrn(vrn)
        .lineNumber(lineNumber)
        .build();
  }
}