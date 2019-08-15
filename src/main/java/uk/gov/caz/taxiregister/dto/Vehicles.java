package uk.gov.caz.taxiregister.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
public class Vehicles {
  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicles.vehicle-details}")
  @NotNull
  @Valid
  List<VehicleDto> vehicleDetails;
}
