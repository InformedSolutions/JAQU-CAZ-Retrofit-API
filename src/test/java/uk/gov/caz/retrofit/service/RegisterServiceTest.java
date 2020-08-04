package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.AuditingRepository;
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

  private static final UUID ANY_UPLOADER_ID = UUID
      .fromString("c5052136-46b9-4a07-8051-7da01b5c84c5");

  @Mock
  private AuditingRepository auditingRepository;

  @BeforeEach
  void setup() {
    retrofittedRepository = new InMemoryRetrofittedRepository();
    registerService = new RegisterService(retrofittedRepository, auditingRepository);
  }

  @Test
  void shouldRejectUploadingNullSetOfVehicles() {
    //given
    Set<RetrofittedVehicle> vehiclesToPersist = null;

    //then
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> registerService.register(vehiclesToPersist, ANY_UPLOADER_ID))
        .withMessage("retrofittedVehicles cannot be null");
  }

  @Test
  void shouldRegisterVehiclesInEmptyDatabase() {
    //given
    Set<RetrofittedVehicle> vehiclesToPersist = Sets
        .newHashSet(MILITARY_VEHICLE_1, NORMAL_VEHICLE_1);

    //when
    registerService.register(vehiclesToPersist, ANY_UPLOADER_ID);

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
    registerService.register(vehiclesToPersistInFirstJob, ANY_UPLOADER_ID);
    registerService.register(vehiclesToPersistInSecondJob, ANY_UPLOADER_ID);

    //then
    assertThat(retrofittedRepository.retrofittedVehicles)
        .containsExactlyInAnyOrder(NORMAL_VEHICLE_2);
  }

  private static class InMemoryRetrofittedRepository extends
      RetrofittedVehiclePostgresRepository {

    private Set<RetrofittedVehicle> retrofittedVehicles = new HashSet<>();

    public InMemoryRetrofittedRepository() {
      super(null, null, 2);
    }

    @Override
    public void insertOrUpdate(Set<RetrofittedVehicle> retrofittedVehicles) {
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

    @Override
    public void delete(Set<String> vrns) {
      retrofittedVehicles = retrofittedVehicles.stream()
          .filter(vehicle -> !vrns.contains(vehicle.getVrn()))
          .collect(Collectors.toSet());
    }

    @Override
    public boolean existsByVrn(String vrn) {
      return retrofittedVehicles.stream().anyMatch(e -> vrn.equals(e.getVrn()));
    }

    @Override
    public List<String> findAllVrns() {
      return retrofittedVehicles.stream().map(e -> e.getVrn()).collect(Collectors.toList());
    }
  }
}
