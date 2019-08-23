package uk.gov.caz.retrofit.dto;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorsResponse {

  private static final ErrorsResponse UNHANDLED_EXCEPTION_RESPONSE = new ErrorsResponse(
      Collections.singletonList(
          ErrorResponse.builder()
              .vrn("")
              .title("Unknown error")
              .detail("Internal server error")
              .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
              .build())
  );

  List<ErrorResponse> errors;

  public static ErrorsResponse singleValidationErrorResponse(String detail) {
    ErrorResponse errorResponse = ErrorResponse.validationErrorResponse(detail);
    return new ErrorsResponse(Collections.singletonList(errorResponse));
  }

  public static ErrorsResponse internalError() {
    return UNHANDLED_EXCEPTION_RESPONSE;
  }

  public static ErrorsResponse from(List<ErrorResponse> validationErrors) {
    return new ErrorsResponse(validationErrors);
  }
}
