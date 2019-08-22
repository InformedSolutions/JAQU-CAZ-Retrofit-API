package uk.gov.caz.retrofit.model.registerjob;

import com.google.common.base.Preconditions;
import lombok.Value;

@Value
public class RegisterJobName {

  String value;

  /**
   * Creates an instance of {@link RegisterJobName}.
   *
   * @throws NullPointerException if {@code value} is null
   */
  public RegisterJobName(String value) {
    Preconditions.checkNotNull(value, "RegisterJobName cannot be null");

    this.value = value;
  }
}
