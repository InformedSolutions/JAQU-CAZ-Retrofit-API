package uk.gov.caz.taxiregister.service.validation;

import static org.assertj.core.api.BDDAssertions.then;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CsvAwareValidationMessageModifierTest {

  private CsvAwareValidationMessageModifier messageModifier = new CsvAwareValidationMessageModifier();

  @Test
  public void shouldIncludeInfoAboutTrailingRow() {
    // given
    String message = "Hello kitty.";

    // when
    String output = messageModifier.addTrailingRowInfoSuffix(message);

    // then
    then(output).isEqualTo(message + " " + CsvAwareValidationMessageModifier.PRESENT_TRAILING_ROW_MESSAGE_SUFFIX);
  }

  @ParameterizedTest
  @ValueSource(ints = {7, 8, 99})
  public void shouldNotIncludeInfoAboutHeaderForAllButFirstLine(int lineNumber) {
    // given
    String message = "Hello kitty.";

    // when
    String output = messageModifier.addHeaderRowInfoSuffix(message, lineNumber);

    // then
    then(output).doesNotEndWith(CsvAwareValidationMessageModifier.PRESENT_HEADER_MESSAGE_SUFFIX);
  }

  @Test
  public void shouldIncludeInfoAboutHeaderForFirstLine() {
    // given
    int lineNumber = 1;
    String message = "Hello kitty.";

    // when
    String output = messageModifier.addHeaderRowInfoSuffix(message, lineNumber);

    // then
    String expected = message + " " + CsvAwareValidationMessageModifier.PRESENT_HEADER_MESSAGE_SUFFIX;
    then(output).isEqualTo(expected);
  }
}