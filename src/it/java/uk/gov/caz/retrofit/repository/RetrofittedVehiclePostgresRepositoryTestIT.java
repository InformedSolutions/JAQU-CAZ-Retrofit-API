package uk.gov.caz.retrofit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.retrofit.annotation.IntegrationTest;
import uk.gov.caz.retrofit.model.RetrofitVrnInfo;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;

@IntegrationTest
class RetrofittedVehiclePostgresRepositoryTestIT {

  public static final RetrofittedVehicle MILITARY_VEHICLE_1 = RetrofittedVehicle.builder()
      .vrn("8839GF")
      .vehicleCategory("Military Vehicle")
      .model("T-34/85 Rudy 102")
      .dateOfRetrofitInstallation(LocalDate.parse("2007-12-03"))
      .build();

  public static final RetrofittedVehicle NORMAL_VEHICLE_1 = RetrofittedVehicle.builder()
      .vrn("1839GF")
      .vehicleCategory("Normal Vehicle")
      .model("Skoda Octavia")
      .dateOfRetrofitInstallation(LocalDate.parse("2007-12-03"))
      .build();

  @Autowired
  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  @BeforeEach
  public void setup() {
    retrofittedVehiclePostgresRepository.insertOrUpdate(
        Sets.newHashSet(MILITARY_VEHICLE_1, NORMAL_VEHICLE_1)
    );
  }

  @AfterEach
  public void cleanup() {
    retrofittedVehiclePostgresRepository.deleteAll();
  }

  @Test
  public void shouldDeleteByVrns() {
    //when
    retrofittedVehiclePostgresRepository.delete(
        Sets.newHashSet(
            NORMAL_VEHICLE_1.getVrn(), MILITARY_VEHICLE_1.getVrn()
        )
    );

    //then
    assertThat(retrofittedVehiclePostgresRepository.findAll())
        .isEmpty();
  }

  @Test
  public void shouldFetchAllVrns() {
    //when
    List<String> allVrns = retrofittedVehiclePostgresRepository.findAllVrns();

    //then
    assertThat(allVrns)
        .containsExactlyInAnyOrder(NORMAL_VEHICLE_1.getVrn(), MILITARY_VEHICLE_1.getVrn());
  }

  @Test
  public void shouldFetchVrnInfo() {
    //when
    RetrofitVrnInfo retrofitVrnInfo = retrofittedVehiclePostgresRepository
        .infoByVrn(NORMAL_VEHICLE_1.getVrn());

    // then
    assertThat(retrofitVrnInfo.getRowCount()).isEqualTo(1);
  }
}