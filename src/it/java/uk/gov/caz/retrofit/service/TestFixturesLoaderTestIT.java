package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import uk.gov.caz.retrofit.annotation.IntegrationTest;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

@IntegrationTest
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
public class TestFixturesLoaderTestIT {

  private static final String MALFORMED_FIXTURES_LOCATION = "/data/json/malformed-fixtures.json";
  @Value("${application.test-fixtures-location}")
  private String testFixturesLocation;

  @Autowired
  private RetrofittedVehiclePostgresRepository vehicleRepository;

  private RetrofittedVehicle someRandomVehicle() {
    return RetrofittedVehicle.builder()
        .vrn(RandomStringUtils.randomAlphabetic(3))
        .dateOfRetrofitInstallation(LocalDate.now())
        .build();
  }

  @Test
  public void whenLoaderDoesHisJobOldDataIsRemovedAndNewDataIsInsertedFromJson() {
    // given
    insertTwoRandomVehiclesAndCheckIt();
    TestFixturesLoader testFixturesLoader = new TestFixturesLoader(vehicleRepository,
        testFixturesLocation, new ObjectMapper());

    // when
    testFixturesLoader.loadTestData();

    // then
    List<RetrofittedVehicle> vehiclesInDatabaseAfterLoader = vehicleRepository.findAll();
    assertThat(vehiclesInDatabaseAfterLoader).hasSize(3);
  }

  @Test
  public void whenLoaderEncountersMalformedFileNoDataIsInserted() {
    // given
    insertTwoRandomVehiclesAndCheckIt();
    TestFixturesLoader testFixturesLoader = new TestFixturesLoader(vehicleRepository,
        MALFORMED_FIXTURES_LOCATION, new ObjectMapper());

    // when
    Throwable throwable = catchThrowable(() -> testFixturesLoader.loadTestData());

    List<RetrofittedVehicle> vehiclesInDatabaseAfterLoader = vehicleRepository.findAll();
    // we still have old 2 vehicles in DB
    assertThat(vehiclesInDatabaseAfterLoader).hasSize(2);
    assertThat(throwable).isInstanceOf(JsonMappingException.class);
  }

  private void insertTwoRandomVehiclesAndCheckIt() {
    Set vehiclesToInsert = Sets.newHashSet(someRandomVehicle(), someRandomVehicle());
    vehicleRepository.insertOrUpdate(vehiclesToInsert);
    List<RetrofittedVehicle> vehiclesInDatabase = vehicleRepository.findAll();
    assertThat(vehiclesInDatabase).hasSize(2);
  }

}
