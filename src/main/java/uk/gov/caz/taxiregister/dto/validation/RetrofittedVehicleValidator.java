package uk.gov.caz.taxiregister.dto.validation;

import java.util.List;
import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public interface RetrofittedVehicleValidator {
  List<ValidationError> validate(RetrofittedVehicleDto retrofittedVehicleDto);
}
