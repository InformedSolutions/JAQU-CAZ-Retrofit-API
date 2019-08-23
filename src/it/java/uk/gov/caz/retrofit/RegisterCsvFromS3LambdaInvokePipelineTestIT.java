package uk.gov.caz.retrofit;

import static org.mockito.BDDMockito.given;
import static uk.gov.caz.retrofit.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.retrofit.model.CsvContentType.RETROFIT_LIST;
import static uk.gov.caz.retrofit.repository.RetrofittedVehicleDtoCsvRepository.UPLOADER_ID_METADATA_KEY;
import static uk.gov.caz.retrofit.service.CsvFileOnS3MetadataExtractor.CSV_CONTENT_TYPE_METADATA_KEY;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import uk.gov.caz.retrofit.annotation.FullyRunningServerIntegrationTest;
import uk.gov.caz.retrofit.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.retrofit.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.retrofit.dto.RegisterJobStatusDto;
import uk.gov.caz.retrofit.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.retrofit.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.model.registerjob.RegisterJob;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobName;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobStatus;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;
import uk.gov.caz.retrofit.service.RegisterJobNameGenerator;
import uk.gov.caz.retrofit.service.RegisterJobSupervisor;
import uk.gov.caz.retrofit.service.SourceAwareRegisterService;
import uk.gov.caz.retrofit.util.DatabaseInitializer;

@FullyRunningServerIntegrationTest
@Import(DatabaseInitializer.class)
@Slf4j
public class RegisterCsvFromS3LambdaInvokePipelineTestIT {

  private static final String BUCKET_NAME = String.format(
      "ntr-data-%d",
      System.currentTimeMillis()
  );
  private static final String CSV_FILE = "first-uploader-records-all.csv";
  private static final int CSV_FILE_VEHICLES_COUNT = 8;
  private static final String JOB_SUFFIX = "first-uploader-records-all";
  private static final String JOB_NAME = "prefixed_" + JOB_SUFFIX;
  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");

  private static final Map<String, String[]> UPLOADER_TO_FILES = ImmutableMap.of(
      TYPICAL_REGISTER_JOB_UPLOADER_ID.toString(), new String[]{
          "first-uploader-records-all.csv"}
  );

  @LocalServerPort
  int randomServerPort;

  @Autowired
  private DatabaseInitializer databaseInitializer;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private RegisterJobSupervisor registerJobSupervisor;

  @Autowired
  private SourceAwareRegisterService registerFromCsvService;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  @MockBean
  private LambdaClientBuilder mockedLambdaClientBuilder;

  @MockBean
  private RegisterJobNameGenerator mockedRegisterJobNameGenerator;

  @BeforeEach
  public void init() throws Exception {
    prepareDataInS3();
    databaseInitializer.clear();
    databaseInitializer.init();
    RestAssured.port = randomServerPort;
    RestAssured.baseURI = "http://localhost";
    RestAssured.basePath = "/v1/retrofit";

    MockedLambdaClient mockedLambdaClient = new MockedLambdaClient();
    given(mockedLambdaClientBuilder.build()).willReturn(mockedLambdaClient);
    given(mockedRegisterJobNameGenerator
        .generate(JOB_SUFFIX, RegisterJobTrigger.RETROFIT_CSV_FROM_S3))
        .willReturn(new RegisterJobName(JOB_NAME));
  }

  @AfterEach
  public void clear() {
    clearS3();
    databaseInitializer.clear();
  }

  @Test
  public void testJobCreationAndThenPollingUntilItFinishesSuccessfully()
      throws JsonProcessingException {

    // See MockedLambdaClient.registerFromCsvInTheBackground method for details regarding
    // Register Job workload. In summary:
    // 1. It waits for 2 seconds keeping freshly started job in 'STARTING' status.
    // 2. After 2 seconds starts register job. It changes status to 'RUNNING'.
    // 3. Register job reads CSV file and adds vehicles.
    // 4. After that changes status to 'SUCCESS' and finishes successfully.

    // Job Startup - Job starts with 'STARTING' status
    RegisterCsvFromS3JobHandle registerJobHandle = startRegisterJobInTheBackgroundAndObtainItsHandle();
    assertThat(registerJobHandle.getJobName()).isEqualTo(JOB_NAME);

    // Poll for status in 100 milliseconds interval. Expect to change job status to 'RUNNING' after
    // no longer than 3 seconds. This simulates job waiting to be pulled and then executed by some background
    // task executor (AWS Lambda or Java ThreadPoolExecutor or any other with similar functionality).
    Awaitility.with()
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .await("Waiting for Register Job to change status from STARTING to RUNNING")
        .atMost(3, TimeUnit.SECONDS)
        .until(() -> registerJobStatusHasChangedToRunning(registerJobHandle));

    // Poll for status in 100 milliseconds interval. Expect to change job status to 'SUCCESS'
    // after no longer than 5 seconds. This simulates job workload/algorithm/computation being
    // executed.
    Awaitility.with()
        .pollInterval(100, TimeUnit.MILLISECONDS)
        .await("Waiting for Register Job to change status from RUNNING to FINISHED_OK_NO_ERRORS")
        .atMost(5, TimeUnit.SECONDS)
        .until(() -> registerJobStatusHasChangedToSuccess(registerJobHandle));

    // And finally check if Register Job in database has correct values matching finished job.
    checkIfRegisterJobInDatabaseIsPresentAndHasCorrectValues(registerJobHandle);
    // And if vehicles have been successfully inserted
    checkIfVehiclesHaveBeenCorrectlyInserted();
  }

  @Test
  public void shouldNotRunSecondJobWhenJobAlreadyStartingOrRunning()
      throws JsonProcessingException {

    startRegisterJobInTheBackgroundAndObtainItsHandle();

    assertThat(registerJobSupervisor.hasActiveJobs(TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .isTrue();

    attemptToStartSecondJob();
  }

  private void attemptToStartSecondJob() throws JsonProcessingException {
    RestAssured
        .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
        .body(prepareSamplePayload())
        .when()
        .post("/register-csv-from-s3/jobs")
        .then()
        .statusCode(HttpStatus.NOT_ACCEPTABLE.value())
        .extract();
  }

  private RegisterCsvFromS3JobHandle startRegisterJobInTheBackgroundAndObtainItsHandle()
      throws JsonProcessingException {
    return RestAssured
        .given()
        .accept(ContentType.JSON)
        .contentType(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
        .body(prepareSamplePayload())
        .when()
        .post("/register-csv-from-s3/jobs")
        .then()
        .statusCode(HttpStatus.CREATED.value())
        .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
        .extract()
        .as(RegisterCsvFromS3JobHandle.class);
  }

  private String prepareSamplePayload() throws JsonProcessingException {
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(BUCKET_NAME, CSV_FILE);
    return objectMapper.writeValueAsString(cmd);
  }

  private Boolean registerJobStatusHasChangedToRunning(
      RegisterCsvFromS3JobHandle registerJobHandle) {
    StatusOfRegisterCsvFromS3JobQueryResult queryForJobStatus = pollForStatusOfRunningJobWithHandle(
        registerJobHandle);
    return queryForJobStatus.getStatus() == RegisterJobStatusDto.RUNNING;
  }

  private Boolean registerJobStatusHasChangedToSuccess(
      RegisterCsvFromS3JobHandle registerJobHandle) {
    StatusOfRegisterCsvFromS3JobQueryResult queryForJobStatus = pollForStatusOfRunningJobWithHandle(
        registerJobHandle);
    return queryForJobStatus.getStatus() == RegisterJobStatusDto.SUCCESS;
  }

  private StatusOfRegisterCsvFromS3JobQueryResult pollForStatusOfRunningJobWithHandle(
      RegisterCsvFromS3JobHandle registerJobHandle) {
    return RestAssured.given()
        .accept(ContentType.JSON)
        .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
        .when()
        .get("/register-csv-from-s3/jobs/{registerJobName}",
            registerJobHandle.getJobName())
        .then()
        .statusCode(HttpStatus.OK.value())
        .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
        .extract()
        .as(StatusOfRegisterCsvFromS3JobQueryResult.class);
  }

  private void checkIfRegisterJobInDatabaseIsPresentAndHasCorrectValues(
      RegisterCsvFromS3JobHandle registerJobHandle) {
    Optional<RegisterJob> registerJobAfterFinishedOptional = registerJobSupervisor
        .findJobWithName(new RegisterJobName(registerJobHandle.getJobName()));
    assertThat(registerJobAfterFinishedOptional).isPresent();
    assertThat(registerJobAfterFinishedOptional.get())
        .isInStatus(RegisterJobStatus.FINISHED_SUCCESS)
        .hasCorrelationId(TYPICAL_CORRELATION_ID)
        .hasName(JOB_NAME)
        .hasErrors(Collections.emptyList())
        .wasTriggeredBy(RegisterJobTrigger.RETROFIT_CSV_FROM_S3)
        .wasUploadedBy(TYPICAL_REGISTER_JOB_UPLOADER_ID);
  }

  private void checkIfVehiclesHaveBeenCorrectlyInserted() {
    List<RetrofittedVehicle> vehicles = retrofittedVehiclePostgresRepository
        .findAll();
    assertThat(vehicles).hasSize(CSV_FILE_VEHICLES_COUNT);
  }

  private void prepareDataInS3() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
    uploadFilesToS3(UPLOADER_TO_FILES);
  }

  private void uploadFilesToS3(Map<String, String[]> uploaderToFilesMap) {
    for (Entry<String, String[]> uploaderToFiles : uploaderToFilesMap.entrySet()) {
      String uploaderId = uploaderToFiles.getKey();
      String[] files = uploaderToFiles.getValue();

      for (String filename : files) {
        s3Client.putObject(builder -> builder.bucket(BUCKET_NAME)
                .key(filename)
                .metadata(
                    ImmutableMap
                        .<String, String>of(CSV_CONTENT_TYPE_METADATA_KEY, RETROFIT_LIST.toString(),
                            UPLOADER_ID_METADATA_KEY,
                            uploaderId)
                ),
            FILE_BASE_PATH.resolve(filename));
      }
    }
  }

  private void clearS3() {
    deleteFilesFromS3(filesToDelete());
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
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

  private class MockedLambdaClient implements LambdaClient {

    @Override
    public InvokeResponse invoke(InvokeRequest invokeRequest)
        throws AwsServiceException, SdkClientException {
      try {
        RegisterCsvFromS3LambdaInput registerCsvFromS3LambdaInput = objectMapper
            .readValue(invokeRequest.payload().asUtf8String(), RegisterCsvFromS3LambdaInput.class);
        CompletableFuture.runAsync(
            () -> registerFromCsvInTheBackground(registerCsvFromS3LambdaInput.getRegisterJobId()));
        return InvokeResponse.builder().build();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void registerFromCsvInTheBackground(int registerJobId) {
      try {
        Thread.sleep(2000);
        registerFromCsvService
            .register(BUCKET_NAME, CSV_FILE, registerJobId, TYPICAL_CORRELATION_ID);
      } catch (InterruptedException e) {
        log.error("Job interrupted", e);
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public String serviceName() {
      return null;
    }

    @Override
    public void close() {
    }
  }
}
