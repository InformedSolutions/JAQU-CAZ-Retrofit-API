package uk.gov.caz.retrofit.service.validation;

import com.google.common.annotations.VisibleForTesting;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;

@Component
public class CsvAwareValidationMessageModifier {

  @VisibleForTesting
  static final String PRESENT_HEADER_MESSAGE_SUFFIX = "Please make sure you have not "
      + "included a header row.";
  @VisibleForTesting
  static final String PRESENT_TRAILING_ROW_MESSAGE_SUFFIX = "Please make sure you have not "
      + "included a trailing row.";
  private static final BiFunction<String, Integer, String> NO_OP = (message, lineNumber) -> message;
  private static final BiFunction<String, Integer, String> CSV_HEADER_PRESENT_MODIFIER =
      (message, lineNumber) -> message + " " + PRESENT_HEADER_MESSAGE_SUFFIX;

  /**
   * Adds a suffix to {@code message} with the information about the possibly included header row if
   * {@code lineNumber == 1}, otherwise {@code message} is returned.
   */
  public String addHeaderRowInfoSuffix(String message, int lineNumber) {
    return modifier(lineNumber).apply(message, lineNumber);
  }

  /**
   * Adds a suffix to {@code message} with the information about the possibly included trailing
   * row.
   */
  public String addTrailingRowInfoSuffix(String message) {
    return message + " " + PRESENT_TRAILING_ROW_MESSAGE_SUFFIX;
  }

  private BiFunction<String, Integer, String> modifier(int lineNumber) {
    return isFirstLine(lineNumber) ? CSV_HEADER_PRESENT_MODIFIER : NO_OP;
  }

  private boolean isFirstLine(int lineNumber) {
    return lineNumber == 1;
  }
}
