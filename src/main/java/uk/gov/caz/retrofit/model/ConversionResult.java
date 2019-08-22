package uk.gov.caz.retrofit.model;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversionResult {
  List<ValidationError> validationErrors;

  RetrofittedVehicle retrofittedVehicle;

  public boolean isSuccess() {
    return retrofittedVehicle != null;
  }

  public boolean isFailure() {
    return !isSuccess();
  }

  public static ConversionResult success(RetrofittedVehicle retrofittedVehicle) {
    return new ConversionResult(Collections.emptyList(), retrofittedVehicle);
  }

  public static ConversionResult failure(List<ValidationError> validationErrors) {
    return new ConversionResult(validationErrors, null);
  }
}
