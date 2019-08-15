package uk.gov.caz.taxiregister.model;

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversionResult {
  List<ValidationError> validationErrors;

  TaxiPhvVehicleLicence licence;

  public boolean isSuccess() {
    return licence != null;
  }

  public boolean isFailure() {
    return !isSuccess();
  }

  public static ConversionResult success(TaxiPhvVehicleLicence licence) {
    return new ConversionResult(Collections.emptyList(), licence);
  }

  public static ConversionResult failure(List<ValidationError> validationErrors) {
    return new ConversionResult(validationErrors, null);
  }
}
