package uk.gov.caz.taxiregister.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class RetrofittedVehicle {
  String vrn;

  String vehicleCategory;

  String model;

  LocalDate dateOfRetrofitInstallation;
}
