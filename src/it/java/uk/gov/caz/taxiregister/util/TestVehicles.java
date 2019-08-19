package uk.gov.caz.taxiregister.util;

import java.time.LocalDate;
import lombok.experimental.UtilityClass;
import uk.gov.caz.taxiregister.model.RetrofittedVehicle;

@UtilityClass
public class TestVehicles {

  public static final RetrofittedVehicle VEHICLE_1 = RetrofittedVehicle.builder()
      .vrn("OI64EFO")
      .vehicleCategory("category-1")
      .model("model\",b")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-04-30"))
      .build();

  public static final RetrofittedVehicle VEHICLE_2 = RetrofittedVehicle.builder()
      .vrn("ZC62OMB")
      .vehicleCategory("category-1")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-04-27"))
      .build();

  public static final RetrofittedVehicle VEHICLE_3 = RetrofittedVehicle.builder()
      .vrn("NO03KNT")
      .vehicleCategory("category-1")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-03-12"))
      .build();

  public static final RetrofittedVehicle VEHICLE_4 = RetrofittedVehicle.builder()
      .vrn("DS98UDG")
      .vehicleCategory("a & b'c & d")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-03-11"))
      .build();

  public static final RetrofittedVehicle VEHICLE_5 = RetrofittedVehicle.builder()
      .vrn("ND84VSX")
      .vehicleCategory("category-1")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-04-14"))
      .build();
}
