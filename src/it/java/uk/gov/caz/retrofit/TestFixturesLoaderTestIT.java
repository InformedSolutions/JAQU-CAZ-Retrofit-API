package uk.gov.caz.retrofit;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.retrofit.annotation.IntegrationTest;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;
import uk.gov.caz.retrofit.service.TestFixturesLoader;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class TestFixturesLoaderTestIT {

  @Autowired
  private TestFixturesLoader testFixturesLoader;

  @Autowired
  private RetrofittedVehiclePostgresRepository vehicleRepository;

  private RetrofittedVehicle someRandomVehicle() {
    return RetrofittedVehicle.builder()
        .vrn(RandomStringUtils.randomAlphabetic(3))
        .dateOfRetrofitInstallation(LocalDate.now())
        .build();
  }

  @Test
  public void whenLoaderDoesHisJobOldDataIsRemovedAndNewDataIsInsertedFromJson()
      throws IOException {
    Set vehiclesToInsert = Sets.newHashSet(someRandomVehicle(), someRandomVehicle());

    vehicleRepository.insert(vehiclesToInsert);

    List<RetrofittedVehicle> vehiclesInDatabase = vehicleRepository.findAll();

    assertThat(vehiclesInDatabase).hasSize(2);

    testFixturesLoader.loadTestData();

    List<RetrofittedVehicle> vehiclesInDatabaseAfterLoader = vehicleRepository.findAll();
    assertThat(vehiclesInDatabaseAfterLoader).hasSize(10);


  }

}
