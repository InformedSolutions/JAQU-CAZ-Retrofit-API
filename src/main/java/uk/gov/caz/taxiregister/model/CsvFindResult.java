package uk.gov.caz.taxiregister.model;

import java.util.List;
import java.util.UUID;
import lombok.Value;
import uk.gov.caz.taxiregister.dto.VehicleDto;

@Value
public class CsvFindResult {

  UUID uploaderId;
  List<VehicleDto> licences;
  List<ValidationError> validationErrors;
}
