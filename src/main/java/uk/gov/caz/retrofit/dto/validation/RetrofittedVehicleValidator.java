package uk.gov.caz.retrofit.dto.validation;

import java.util.List;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

public interface RetrofittedVehicleValidator {
  List<ValidationError> validate(RetrofittedVehicleDto retrofittedVehicleDto);
}
