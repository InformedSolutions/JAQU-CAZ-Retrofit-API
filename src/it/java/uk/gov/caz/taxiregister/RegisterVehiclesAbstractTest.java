package uk.gov.caz.taxiregister;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.service.RegisterJobRepository;
import uk.gov.caz.taxiregister.service.TaxiPhvLicencePostgresRepository;
import uk.gov.caz.taxiregister.util.DatabaseInitializer;

@Slf4j
public abstract class RegisterVehiclesAbstractTest {

  static final UUID FIRST_UPLOADER_ID = UUID.randomUUID();
  static final UUID SECOND_UPLOADER_ID = UUID.randomUUID();

  @Autowired
  private TaxiPhvLicencePostgresRepository taxiPhvLicencePostgresRepository;

  @Autowired
  private RegisterJobRepository registerJobRepository;

  @Autowired
  private DatabaseInitializer databaseInitializer;

  @BeforeEach
  private void initializeDatabase() throws Exception {
    log.info("Initialing database : start");
    databaseInitializer.initWithoutLicenceData();
    databaseInitializer.initRegisterJobData();
    log.info("Initialing database : finish");
  }

  @AfterEach
  private void cleanDatabase() {
    log.info("Clearing database : start");
    databaseInitializer.clear();
    log.info("Clearing database : finish");
  }

  @Test
  public void registerTest() {
    atTheBeginningThereShouldBeNoVehicles();

    whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader();
    thenAllShouldBeInsertedByFirstUploader();
    andRegisterJobShouldHaveFinishedSuccessfully();

    whenVehiclesAreRegisteredBySecondUploaderWithNoDataFromTheFirstOne();
    thenAllShouldBeInsertedBySecondUploader();
    andRegisterJobShouldHaveFinishedSuccessfully();

    whenThereAreFiveVehicleLessRegisteredByFirstUploader();
    thenFiveVehiclesShouldBeDeletedByFirstUploader();
    andNoVehiclesShouldBeDeletedBySecondUploader();
    andRegisterJobShouldHaveFinishedSuccessfully();

    whenThreeVehiclesAreUpdatedByBothUploaders();
    thenTotalNumberOfRecordsStaysTheSame();
    andRegisterJobShouldHaveFinishedSuccessfully();
  }

  abstract void whenThreeVehiclesAreUpdatedByBothUploaders();
  abstract void whenThereAreFiveVehicleLessRegisteredByFirstUploader();
  abstract void whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader();
  abstract void whenVehiclesAreRegisteredBySecondUploaderWithNoDataFromTheFirstOne();

  abstract int getSecondUploaderTotalVehiclesCount();
  abstract int getFirstUploaderTotalVehiclesCount();

  abstract int getRegisterJobId();

  private void thenTotalNumberOfRecordsStaysTheSame() {
    assertThatTheNumberOfVehiclesByFirstUploaderIs(getFirstUploaderTotalVehiclesCount() - 5);
    assertThatTheNumberOfVehiclesBySecondUploaderIs(getSecondUploaderTotalVehiclesCount());
  }

  private void andNoVehiclesShouldBeDeletedBySecondUploader() {
    assertThatTheNumberOfVehiclesBySecondUploaderIs(getSecondUploaderTotalVehiclesCount());
  }

  private void thenAllShouldBeInsertedBySecondUploader() {
    assertThatTheNumberOfVehiclesBySecondUploaderIs(getSecondUploaderTotalVehiclesCount());
  }

  private void thenFiveVehiclesShouldBeDeletedByFirstUploader() {
    assertThatTheNumberOfVehiclesByFirstUploaderIs(getFirstUploaderTotalVehiclesCount() - 5);
  }

  private void thenAllShouldBeInsertedByFirstUploader() {
    assertThatTheNumberOfVehiclesIs(getFirstUploaderTotalVehiclesCount());
  }

  private void andRegisterJobShouldHaveFinishedSuccessfully() {
    RegisterJob registerJob = registerJobRepository.findById(getRegisterJobId()).get();
    assertThat(registerJob.getStatus()).isEqualByComparingTo(RegisterJobStatus.FINISHED_SUCCESS);
  }

  private void assertThatTheNumberOfVehiclesByFirstUploaderIs(int expectedVehiclesCount) {
    assertThatTheNumberOfVehiclesByUploaderIs(expectedVehiclesCount, FIRST_UPLOADER_ID);
  }

  private void assertThatTheNumberOfVehiclesBySecondUploaderIs(int expectedVehiclesCount) {
    assertThatTheNumberOfVehiclesByUploaderIs(expectedVehiclesCount, SECOND_UPLOADER_ID);
  }

  private void assertThatTheNumberOfVehiclesByUploaderIs(int expectedVehiclesCount,
      UUID uploaderId) {
    Set<TaxiPhvVehicleLicence> vehicles = taxiPhvLicencePostgresRepository.findByUploaderId(uploaderId);
    assertThat(vehicles).hasSize(expectedVehiclesCount);
  }

  private void assertThatTheNumberOfVehiclesIs(int expectedVehiclesCount) {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = taxiPhvLicencePostgresRepository.findAll();
    assertThat(taxiPhvVehicleLicences).hasSize(expectedVehiclesCount);
  }

  private void atTheBeginningThereShouldBeNoVehicles() {
    List<TaxiPhvVehicleLicence> taxiPhvVehicleLicences = taxiPhvLicencePostgresRepository.findAll();

    assertThat(taxiPhvVehicleLicences).isEmpty();
  }
}
