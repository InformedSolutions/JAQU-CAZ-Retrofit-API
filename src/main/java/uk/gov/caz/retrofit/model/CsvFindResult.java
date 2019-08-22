package uk.gov.caz.retrofit.model;

import java.util.List;
import java.util.UUID;
import lombok.Value;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;

@Value
public class CsvFindResult {

  UUID uploaderId;
  List<RetrofittedVehicleDto> licences;
  List<ValidationError> validationErrors;
}
