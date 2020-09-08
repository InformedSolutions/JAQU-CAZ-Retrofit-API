package uk.gov.caz.retrofit;

import java.time.LocalDate;
import lombok.experimental.UtilityClass;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;

@UtilityClass
public class TestVehicles {
  public static final RetrofittedVehicle VALID_MILITARY_VEHICLE_1 = RetrofittedVehicle.builder()
      .vrn("8839GF")
      .vehicleCategory("Military Vehicle")
      .model("T-34/85 Rudy 102")
      .dateOfRetrofitInstallation(LocalDate.parse("2007-12-03"))
      .build();

  public static final RetrofittedVehicle VALID_NORMAL_VEHICLE_1 = RetrofittedVehicle.builder()
      .vrn("1839GF")
      .vehicleCategory("Normal Vehicle")
      .model("Skoda Octavia")
      .dateOfRetrofitInstallation(LocalDate.parse("2007-12-03"))
      .build();
}
