package uk.gov.caz.taxiregister.model;

import java.util.List;
import lombok.Value;
import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;

@Value
public class CsvParseResult {
  List<RetrofittedVehicleDto> retrofittedVehicles;
  List<ValidationError> validationErrors;
}
