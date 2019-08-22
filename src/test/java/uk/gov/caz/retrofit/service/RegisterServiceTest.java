package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

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
      .dateOfRetrofitInstallation(LocalDate.parse("2007-10-03"))
      .build();

  public static final RetrofittedVehicle NORMAL_VEHICLE_2 = RetrofittedVehicle.builder()
      .vrn("7639GF")
      .vehicleCategory("Normal Vehicle 2")
      .model("Hyundai i30")
      .dateOfRetrofitInstallation(LocalDate.parse("2007-10-03"))
      .build();

  private InMemoryRetrofittedRepository retrofittedRepository;

  private RegisterService registerService;

  @BeforeEach
  void setup() {
    retrofittedRepository = new InMemoryRetrofittedRepository();
    registerService = new RegisterService(retrofittedRepository);
  }

  @Test
  void shouldRejectUploadingNullSetOfVehicles() {
    //given
    Set<RetrofittedVehicle> vehiclesToPersist = null;

    //then
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> registerService.register(vehiclesToPersist))
        .withMessage("retrofittedVehicles cannot be null");
  }

  @Test
  void shouldRegisterVehiclesInEmptyDatabase() {
    //given
    Set<RetrofittedVehicle> vehiclesToPersist = Sets
        .newHashSet(MILITARY_VEHICLE_1, NORMAL_VEHICLE_1);

    //when
    registerService.register(vehiclesToPersist);

    //then
    assertThat(retrofittedRepository.findAll())
        .containsExactlyInAnyOrder(MILITARY_VEHICLE_1, NORMAL_VEHICLE_1);
  }

  @Test
  void shouldRegisterVehiclesInDatabaseThatAlreadyContainsRetrofittedVehicles() {
    //given
    Set<RetrofittedVehicle> vehiclesToPersistInFirstJob = Sets
        .newHashSet(MILITARY_VEHICLE_1, NORMAL_VEHICLE_1);
    Set<RetrofittedVehicle> vehiclesToPersistInSecondJob = Sets.newHashSet(NORMAL_VEHICLE_2);

    //when
    registerService.register(vehiclesToPersistInFirstJob);
    registerService.register(vehiclesToPersistInSecondJob);

    //then
    assertThat(retrofittedRepository.retrofittedVehicles)
        .containsExactlyInAnyOrder(NORMAL_VEHICLE_2);
  }

  private static class InMemoryRetrofittedRepository extends
      RetrofittedVehiclePostgresRepository {

    private Set<RetrofittedVehicle> retrofittedVehicles = new HashSet<>();

    public InMemoryRetrofittedRepository() {
      super(null, 2);
    }

    @Override
    public void insert(Set<RetrofittedVehicle> retrofittedVehicles) {
      this.retrofittedVehicles.addAll(retrofittedVehicles);
    }

    @Override
    public void deleteAll() {
      this.retrofittedVehicles.clear();
    }

    @Override
    public List<RetrofittedVehicle> findAll() {
      return Lists.newArrayList(retrofittedVehicles);
    }
  }
}