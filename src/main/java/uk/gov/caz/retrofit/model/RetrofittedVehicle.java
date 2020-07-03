package uk.gov.caz.retrofit.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RetrofittedVehicle {
  String vrn;

  String vehicleCategory;

  String model;

//  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
//  @JsonDeserialize(using = LocalDateDeserializer.class)
//  @JsonSerialize(using = LocalDateSerializer.class)
  LocalDate dateOfRetrofitInstallation;
}
