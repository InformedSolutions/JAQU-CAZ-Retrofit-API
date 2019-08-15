package uk.gov.caz.taxiregister.model;

import java.util.List;
import lombok.Value;
import uk.gov.caz.taxiregister.dto.VehicleDto;

@Value
public class CsvParseResult {
  List<VehicleDto> licences;
  List<ValidationError> validationErrors;
}
