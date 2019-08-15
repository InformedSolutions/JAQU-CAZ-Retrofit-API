package uk.gov.caz.taxiregister.dto.validation;

import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public interface LicenceValidator {
  List<ValidationError> validate(VehicleDto vehicleDto);
}
