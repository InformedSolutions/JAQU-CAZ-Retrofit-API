package uk.gov.caz.retrofit;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.retrofit.controller.Constants.CORRELATION_ID_HEADER;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import uk.gov.caz.retrofit.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.retrofit.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.retrofit.dto.RegisterJobStatusDto;
import uk.gov.caz.retrofit.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.retrofit.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.retrofit.model.CsvContentType;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehicleDtoCsvRepository;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;
import uk.gov.caz.retrofit.service.CsvFileOnS3MetadataExtractor;

/**
 * This class provides storage-specific methods for inserting Vehicles.
 *
 * It uses (and thus tests) {@link uk.gov.caz.retrofit.service.RegisterFromCsvCommand} command.
 */
@FullyRunningServerIntegrationTest
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@Slf4j
public class RegisterTestIT {

  private static final UUID FIRST_UPLOADER_ID = UUID.fromString("6314d1d6-706a-40ce-b392-a0e618ab45b8");
  private static final UUID SECOND_UPLOADER_ID = UUID.fromString("07447271-df3d-4217-9092-41f1252864b8");
  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");
  private static final int FIRST_UPLOADER_TOTAL_VEHICLES_COUNT = 9;

  private static final String BUCKET_NAME = String.format(
      "retrofitted-vehicles-data-%d",
      System.currentTimeMillis()
  );

  private static final Map<String, String[]> UPLOADER_TO_FILES = ImmutableMap.of(
      FIRST_UPLOADER_ID.toString(), new String[]{"first-uploader-records-all.csv"},
      SECOND_UPLOADER_ID.toString(), new String[]{"second-uploader-max-validation-errors-exceeded.csv"}
  );

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${application.validation.max-errors-count}")
  private int maxErrorsCount;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  private StatusOfRegisterCsvFromS3JobQueryResult queryResult;

  @BeforeEach
  private void setUp() {
    createBucketAndFilesInS3();
    setUpRestAssured();
  }

  @AfterEach
  private void tearDown() {
    deleteBucketAndFilesFromS3();
  }

  @Test
  public void registerTest() {
    atTheBeginningThereShouldBeNoVehicles();

    whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader();
    thenAllShouldBeInserted();
    andThereShouldBeNoErrors();

    whenVehiclesAreRegisteredBySecondUploader();
    thenNoVehiclesShouldBeRegisteredBySecondUploader();
    andJobShouldFinishWithFailureStatus();
    andThereShouldBeMaxValidationErrors();
  }

  private void andJobShouldFinishWithFailureStatus() {
    assertThat(queryResult.getStatus()).isEqualTo(RegisterJobStatusDto.FAILURE);
  }

  private void andThereShouldBeMaxValidationErrors() {
    assertThat(queryResult.getErrors()).isNotNull();
    assertThat(queryResult.getErrors()).hasSize(maxErrorsCount);
  }

  private void thenNoVehiclesShouldBeRegisteredBySecondUploader() {
    allVehiclesInDatabaseAreInsertedByFirstUploader();
  }

  private void whenVehiclesAreRegisteredBySecondUploader() {
    registerVehiclesFrom("second-uploader-max-validation-errors-exceeded.csv");
  }

  private void andThereShouldBeNoErrors() {
    boolean hasErrors = queryResult.getErrors() == null || queryResult.getErrors().length == 0;
    assertThat(hasErrors).isTrue();
  }

  private void atTheBeginningThereShouldBeNoVehicles() {
    List<RetrofittedVehicle> vehicles = retrofittedVehiclePostgresRepository.findAll();
    assertThat(vehicles).isEmpty();
  }

  private void whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader() {
    registerVehiclesFrom("first-uploader-records-all.csv");
  }

  private void thenAllShouldBeInserted() {
    allVehiclesInDatabaseAreInsertedByFirstUploader();
  }

  private void allVehiclesInDatabaseAreInsertedByFirstUploader() {
    List<RetrofittedVehicle> vehicles = retrofittedVehiclePostgresRepository.findAll();
    assertThat(vehicles).hasSize(FIRST_UPLOADER_TOTAL_VEHICLES_COUNT);
  }

  private void registerVehiclesFrom(String filename) {
    RegisterCsvFromS3JobHandle jobHandle = startJob(filename);
    Awaitility.with()
        .pollInterval(250, TimeUnit.MILLISECONDS)
        .await("Waiting for Register Job to finish")
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> jobHasFinished(jobHandle));
  }

  private RegisterCsvFromS3JobHandle startJob(String filename) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured
        .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(preparePayload(filename))
        .when()
        .post("/register-csv-from-s3/jobs")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .header(CORRELATION_ID_HEADER, correlationId)
        .extract().as(RegisterCsvFromS3JobHandle.class);
  }

  private boolean jobHasFinished(RegisterCsvFromS3JobHandle jobHandle) {
    StatusOfRegisterCsvFromS3JobQueryResult queryResult = getJobInfo(jobHandle.getJobName());
    this.queryResult = queryResult;
    return queryResult.getStatus() != RegisterJobStatusDto.RUNNING;
  }

  final StatusOfRegisterCsvFromS3JobQueryResult getJobInfo(String jobName) {
    String correlationId = UUID.randomUUID().toString();
    return RestAssured.given()
        .accept(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, correlationId)
        .when()
        .get("/register-csv-from-s3/jobs/{registerJobName}",
            jobName)
        .then()
        .statusCode(HttpStatus.OK.value())
        .header(CORRELATION_ID_HEADER, correlationId)
        .extract().as(StatusOfRegisterCsvFromS3JobQueryResult.class);
  }

  @SneakyThrows
  private String preparePayload(String filename) {
    StartRegisterCsvFromS3JobCommand cmd = new StartRegisterCsvFromS3JobCommand(BUCKET_NAME,
        filename);
    return objectMapper.writeValueAsString(cmd);
  }

  private void createBucketAndFilesInS3() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
    uploadFilesToS3(UPLOADER_TO_FILES);
  }

  private void deleteBucketAndFilesFromS3() {
    deleteFilesFromS3(filesToDelete());
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
  }

  private void uploadFilesToS3(Map<String, String[]> uploaderToFilesMap) {
    for (Entry<String, String[]> uploaderToFiles : uploaderToFilesMap.entrySet()) {
      String uploaderId = uploaderToFiles.getKey();
      String[] files = uploaderToFiles.getValue();

      for (String filename : files) {
        s3Client.putObject(builder -> builder.bucket(BUCKET_NAME)
                .key(filename)
                .metadata(
                    ImmutableMap.of(
                        RetrofittedVehicleDtoCsvRepository.UPLOADER_ID_METADATA_KEY, uploaderId,
                        CsvFileOnS3MetadataExtractor.CSV_CONTENT_TYPE_METADATA_KEY, CsvContentType.RETROFIT_LIST.toString()
                    )
                ),
            FILE_BASE_PATH.resolve(filename));
      }
    }
  }

  private void deleteFilesFromS3(List<String> filenames) {
    for (String filename : filenames) {
      s3Client.deleteObject(builder -> builder.bucket(BUCKET_NAME).key(filename));
    }
  }

  private List<String> filesToDelete() {
    return UPLOADER_TO_FILES.values()
        .stream()
        .map(Arrays::asList)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  private void setUpRestAssured() {
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/retrofit";
  }
}
