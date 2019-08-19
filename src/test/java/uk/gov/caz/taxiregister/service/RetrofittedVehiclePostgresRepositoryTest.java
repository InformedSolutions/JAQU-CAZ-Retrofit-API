package uk.gov.caz.taxiregister.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.taxiregister.model.RetrofittedVehicle;
import uk.gov.caz.taxiregister.repository.RetrofittedVehiclePostgresRepository;

@ExtendWith(MockitoExtension.class)
class RetrofittedVehiclePostgresRepositoryTest {

  private static final int ANY_BATCH_SIZE = 1;

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

  private static final Set<RetrofittedVehicle> RETROFITTED_VEHICLES = Sets.newHashSet(
      MILITARY_VEHICLE_1,
      NORMAL_VEHICLE_1
  );

  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setup() {
    retrofittedVehiclePostgresRepository = new RetrofittedVehiclePostgresRepository(
        jdbcTemplate, ANY_BATCH_SIZE
    );
  }

  @Test
  void shouldInsertInBatches() {
    //given
    Set<RetrofittedVehicle> retrofittedVehicles = RETROFITTED_VEHICLES;

    //when
    retrofittedVehiclePostgresRepository.insert(retrofittedVehicles);

    //then
    verify(jdbcTemplate, times(2))
        .batchUpdate(eq(RetrofittedVehiclePostgresRepository.INSERT_SQL),
            any(BatchPreparedStatementSetter.class));
  }

  @Test
  void shouldRemoveAllRowsTableOnDelete() {
    //given
    retrofittedVehiclePostgresRepository.deleteAll();

    //then
    verify(jdbcTemplate)
        .update(eq(RetrofittedVehiclePostgresRepository.DELETE_ALL_SQL));
  }
}