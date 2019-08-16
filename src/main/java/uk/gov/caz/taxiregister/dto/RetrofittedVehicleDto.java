package uk.gov.caz.taxiregister.dto;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Value;
import uk.gov.caz.taxiregister.dto.validation.DateOfRetrofitInstallationValidator;
import uk.gov.caz.taxiregister.dto.validation.ModelValidator;
import uk.gov.caz.taxiregister.dto.validation.RetrofittedVehicleValidator;
import uk.gov.caz.taxiregister.dto.validation.VehicleCategoryValidator;
import uk.gov.caz.taxiregister.dto.validation.VrnValidator;
import uk.gov.caz.taxiregister.model.ValidationError;

@Value
@Builder(toBuilder = true)
public class RetrofittedVehicleDto {

  private static final List<RetrofittedVehicleValidator> VALIDATORS = ImmutableList.of(
      new VrnValidator(),
      new VehicleCategoryValidator(),
      new ModelValidator(),
      new DateOfRetrofitInstallationValidator()
  );

  String vrn;

  String vehicleCategory;

  String model;

  String dateOfRetrofitInstallation;

  int lineNumber;

  /**
   * Validates this instance.
   *
   * @return a list of validation errors if there are any. An empty list is returned if validation
   *     succeeds.
   */
  public List<ValidationError> validate() {
    return VALIDATORS.stream()
        .map(validator -> validator.validate(this))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
