package uk.gov.caz.retrofit.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobError;

@Value
@Builder
public class ErrorResponse {

  private static final String NO_VRM = "";
  private static final String VALIDATION_ERROR_TITLE = "Validation error";

  String vrm;
  String title;
  String detail;
  Integer status;

  /**
   * Static factory method.
   *
   * @param validationError An instance of {@link ValidationError} that will be mapped to {@link
   *     ErrorResponse}
   * @return an instance of {@link ErrorResponse}
   */
  public static ErrorResponse from(ValidationError validationError) {
    return ErrorResponse.builder()
        .vrm(validationError.getVrm())
        .title(validationError.getTitle())
        .detail(validationError.getDetail())
        .status(HttpStatus.BAD_REQUEST.value())
        .build();
  }

  /**
   * Static factory method that maps an instance of {@link RegisterJobError} to {@link
   * ErrorResponse}.
   */
  public static ErrorResponse from(RegisterJobError registerJobError) {
    return ErrorResponse.builder()
        .vrm(registerJobError.getVrm())
        .title(registerJobError.getTitle())
        .detail(registerJobError.getDetail())
        .status(HttpStatus.BAD_REQUEST.value())
        .build();
  }

  /**
   * Creates a validation error response, i.e. its title is fixed and equal to 'Validation error',
   * vrm is an empty string, status is equal to 400 and detail is set to the parameter.
   */
  public static ErrorResponse validationErrorResponse(String detail) {
    return ErrorResponse.builder()
        .vrm(NO_VRM)
        .title(VALIDATION_ERROR_TITLE)
        .detail(detail)
        .status(HttpStatus.BAD_REQUEST.value())
        .build();
  }

  public int getStatus() {
    return status;
  }
}
