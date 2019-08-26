package uk.gov.caz.retrofit.model;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidationErrorTest {
  private static final String ANY_DETAIL = "details";
  private static final String ANY_VRN = "vrn1";

  @ParameterizedTest
  @ValueSource(ints = {-100, -87, -1, 0})
  public void shouldNotAcceptNonPositiveLineNumbers(int lineNumber) {
    // when
    Throwable throwable = catchThrowable(() -> ValidationError.valueError(ANY_DETAIL, lineNumber));

    // then
    then(throwable).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 18, 761})
  public void shouldIncludeLineNumberInDetailForValueErrors(int lineNumber) {
    // when
    ValidationError error = ValidationError.valueError(ANY_DETAIL, lineNumber);

    // then
    then(error.getDetail()).startsWith("Line " + lineNumber);
  }

  @ParameterizedTest
  @ValueSource(ints = {8, 5, 912, 9012})
  public void shouldIncludeLineNumberInDetailForMissingFieldError(int lineNumber) {
    // when
    ValidationError error = ValidationError.missingFieldError(ANY_VRN, ANY_DETAIL, lineNumber);

    // then
    then(error.getDetail()).startsWith("Line " + lineNumber);
  }
}