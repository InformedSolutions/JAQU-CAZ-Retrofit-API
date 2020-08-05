package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.retrofit.TestVehicles.VALID_MILITARY_VEHICLE_1;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.retrofit.TestVehicles;
import uk.gov.caz.retrofit.annotation.IntegrationTest;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

@IntegrationTest
class RegisterServiceTestIT {

  @Autowired
  private RegisterService registerService;

  @Autowired
  private RetrofittedVehiclePostgresRepository postgresRepository;

  @BeforeEach
  @AfterEach
  public void setup() {
    postgresRepository.deleteAll();
  }

  @Test
  public void shouldRegisterTwoVehicles() {
    //when
    UUID uploaderId = UUID.randomUUID();
    registerStandardSetOfVehicles(uploaderId);

    //then
    assertThat(postgresRepository.findAll()).containsExactlyInAnyOrder(
        VALID_MILITARY_VEHICLE_1, TestVehicles.VALID_NORMAL_VEHICLE_1
    );
  }

  @Test
  public void shouldDeleteVehicleIfItsNoLongerInTheList() {
    //given
    UUID uploaderId = UUID.randomUUID();
    registerStandardSetOfVehicles(uploaderId);

    //when
    registerService.register(Collections.singleton(VALID_MILITARY_VEHICLE_1), uploaderId);

    //then
    assertThat(postgresRepository.findAll()).containsExactlyInAnyOrder(
        VALID_MILITARY_VEHICLE_1
    );
  }

  @Test
  public void shouldUpdateVehicleIfGivenVrnsIsAlreadyInDb() {
    //given
    UUID uploaderId = UUID.randomUUID();
    registerStandardSetOfVehicles(uploaderId);

    //when
    RetrofittedVehicle modifiedVehicle =
        VALID_MILITARY_VEHICLE_1.toBuilder().model("new-model").build();
    registerService.register(Collections.singleton(modifiedVehicle), uploaderId);

    //then
    assertThat(postgresRepository.findAll()).containsExactlyInAnyOrder(modifiedVehicle);
  }

  private RegisterResult registerStandardSetOfVehicles(UUID uploaderId) {
    return registerService.register(Sets.newHashSet(
        VALID_MILITARY_VEHICLE_1, TestVehicles.VALID_NORMAL_VEHICLE_1
    ), uploaderId);
  }
}