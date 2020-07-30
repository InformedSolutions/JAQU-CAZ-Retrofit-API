package uk.gov.caz.retrofit.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.ApplicationRuntimeException;

/**
 * Exception which is thrown when server receives request with invalid parameters.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestPayloadException extends ApplicationRuntimeException {

  public InvalidRequestPayloadException(String message) {
    super(message);
  }
}
