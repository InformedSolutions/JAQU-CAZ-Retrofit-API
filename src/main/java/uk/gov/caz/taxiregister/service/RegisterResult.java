package uk.gov.caz.taxiregister.service;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import uk.gov.caz.taxiregister.model.ValidationError;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RegisterResult {

  private static final RegisterResult SUCCESS = new RegisterResult(Collections.emptyList());

  List<ValidationError> validationErrors;

  public boolean isSuccess() {
    return this == SUCCESS;
  }

  public static RegisterResult success() {
    return SUCCESS;
  }

  public static RegisterResult failure(List<ValidationError> validationErrors) {
    return new RegisterResult(validationErrors);
  }

  public static RegisterResult failure(ValidationError validationError) {
    return new RegisterResult(Collections.singletonList(validationError));
  }
}
