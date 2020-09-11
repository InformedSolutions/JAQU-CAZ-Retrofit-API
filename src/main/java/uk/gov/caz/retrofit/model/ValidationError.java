package uk.gov.caz.retrofit.model;

import com.google.common.base.Preconditions;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobError;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationError {

  private static final String VALUE_ERROR_TITLE = "Value error";
  private static final String MANDATORY_FIELD_MISSING_ERROR_TITLE = "Mandatory field missing";
  private static final String S3_ERROR_TITLE = "S3 error";
  private static final String REQUEST_PROCESSING_ERROR_TITLE = "Request processing error";
  private static final int EMPTY_LINE_NUMBER = 0;

  private static final ValidationError UNKNOWN_ERROR = new ValidationError(null, "Unknown error",
      "Unknown error occurred while processing registration", EMPTY_LINE_NUMBER);
  private static final ValidationError INTERNAL_ERROR = new ValidationError(null, "Internal error",
      "An internal error occurred while processing registration, please contact the "
          + "system administrator", EMPTY_LINE_NUMBER);

  String vrn;
  String title;
  String detail;
  @Getter(AccessLevel.NONE)
  int lineNumber;

  public Optional<Integer> getLineNumber() {
    return lineNumber == EMPTY_LINE_NUMBER ? Optional.empty() : Optional.of(lineNumber);
  }

  public String getDetail() {
    return getLineNumber().map(line -> "Line " + line + ": " + detail).orElse(detail);
  }

  public String getRawDetail() {
    return detail;
  }

  public static ValidationError missingFieldError(String vrn, String detail) {
    return new ValidationError(vrn, MANDATORY_FIELD_MISSING_ERROR_TITLE, detail, EMPTY_LINE_NUMBER);
  }

  public static ValidationError missingFieldError(String vrn, String detail, int lineNumber) {
    checkLineNumberPrecondition(lineNumber);
    return new ValidationError(vrn, MANDATORY_FIELD_MISSING_ERROR_TITLE, detail, lineNumber);
  }

  public static ValidationError valueError(String vrn, String detail) {
    return new ValidationError(vrn, VALUE_ERROR_TITLE, detail, EMPTY_LINE_NUMBER);
  }

  public static ValidationError valueError(String vrn, String detail, int lineNumber) {
    checkLineNumberPrecondition(lineNumber);
    return new ValidationError(vrn, VALUE_ERROR_TITLE, detail, lineNumber);
  }

  public static ValidationError valueError(String detail, int lineNumber) {
    checkLineNumberPrecondition(lineNumber);
    return new ValidationError(null, VALUE_ERROR_TITLE, detail, lineNumber);
  }

  public static ValidationError valueError(String detail) {
    return new ValidationError(null, VALUE_ERROR_TITLE, detail, EMPTY_LINE_NUMBER);
  }

  public static ValidationError s3Error(String detail) {
    return new ValidationError(null, S3_ERROR_TITLE, detail, EMPTY_LINE_NUMBER);
  }

  /**
   * Maps an instance of {@link RegisterJobError} to {@link ValidationError}.
   */
  public static ValidationError from(RegisterJobError registerJobError) {
    return new ValidationError(
        registerJobError.getVrn(),
        registerJobError.getTitle(),
        registerJobError.getDetail(),
        EMPTY_LINE_NUMBER
    );
  }

  /**
   * Creates a new instance of {@link ValidationError} with detail set to {@code newDetail} and all
   * remaining fields from {@code source}.
   */
  public static ValidationError copyWithNewDetail(ValidationError source, String newDetail) {
    return new ValidationError(
        source.getVrn(),
        source.getTitle(),
        newDetail,
        source.getLineNumber().orElse(EMPTY_LINE_NUMBER)
    );
  }

  public static ValidationError unknown() {
    return UNKNOWN_ERROR;
  }

  public static ValidationError internal() {
    return INTERNAL_ERROR;
  }

  private static void checkLineNumberPrecondition(int lineNumber) {
    Preconditions.checkArgument(lineNumber > 0, "Line number must be positive");
  }
  
  /**
   * Creates {@link ValidationError} for a case of the request processing error.
   *
   * @param detail Detailed description of the error.
   * @return {@link ValidationError} with proper data.
   */
  public static ValidationError requestProcessingError(String detail) {
    return new ValidationError(null, REQUEST_PROCESSING_ERROR_TITLE, detail,
        EMPTY_LINE_NUMBER);
  }
}
