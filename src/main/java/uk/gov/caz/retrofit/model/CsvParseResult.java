package uk.gov.caz.retrofit.model;

import java.util.List;
import lombok.Value;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;

@Value
public class CsvParseResult {
  List<RetrofittedVehicleDto> retrofittedVehicles;
  List<ValidationError> validationErrors;
}
