package uk.gov.caz.taxiregister.model.registerjob;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Value;
import uk.gov.caz.taxiregister.model.ValidationError;

@Value
@JsonInclude(Include.NON_EMPTY)
public class RegisterJobError {

  String vrm;
  String title;
  String detail;

  /**
   * Creates an instance of {@link RegisterJobError} based on of an instance of {@link
   * ValidationError}.
   */
  public static RegisterJobError from(ValidationError validationError) {
    return new RegisterJobError(
        validationError.getVrm(),
        validationError.getTitle(),
        validationError.getDetail()
    );
  }

  public static RegisterJobError withDetailOnly(String detail) {
    return new RegisterJobError(null, null, detail);
  }
}
