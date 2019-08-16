package uk.gov.caz.taxiregister;

import static org.assertj.core.api.BDDAssertions.then;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketCannedACL;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;
import uk.gov.caz.taxiregister.model.CsvFindResult;
import uk.gov.caz.taxiregister.model.RetrofittedVehicle;
import uk.gov.caz.taxiregister.service.RetrofittedVehicleDtoCsvRepository;
import uk.gov.caz.taxiregister.service.RetrofittedVehiclePostgresRepository;
import uk.gov.caz.taxiregister.service.SourceAwareRegisterService;
import uk.gov.caz.taxiregister.util.DatabaseInitializer;

/**
 * This class provides storage-specific methods for inserting Vehicles.
 *
 * It uses (and thus tests) {@link uk.gov.caz.taxiregister.service.RegisterFromCsvCommand} command.
 */
@IntegrationTest
@Import(DatabaseInitializer.class)
@Slf4j
public class RegisterByCsvTestIT {

  private static final RetrofittedVehicle VEHICLE_1 = RetrofittedVehicle.builder()
      .vrn("OI64EFO")
      .vehicleCategory("category-1")
      .model("\"model\"\",b\"")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-04-30"))
      .build();

  private static final RetrofittedVehicle VEHICLE_2 = RetrofittedVehicle.builder()
      .vrn("ZC62OMB")
      .vehicleCategory("category-1")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-04-27"))
      .build();

  private static final RetrofittedVehicle VEHICLE_3 = RetrofittedVehicle.builder()
      .vrn("NO03KNT")
      .vehicleCategory("category-1")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-03-12"))
      .build();

  private static final RetrofittedVehicle VEHICLE_4 = RetrofittedVehicle.builder()
      .vrn("DS98UDG")
      .vehicleCategory("a & b'c & d")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-03-11"))
      .build();

  private static final RetrofittedVehicle VEHICLE_5 = RetrofittedVehicle.builder()
      .vrn("ND84VSX")
      .vehicleCategory("category-1")
      .model("model-1")
      .dateOfRetrofitInstallation(LocalDate.parse("2019-04-13"))
      .build();

  private static final UUID FIRST_UPLOADER_ID = UUID.randomUUID();

  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");
  private static final int FIRST_UPLOADER_TOTAL_VEHICLES_COUNT = 5;

  private static final String BUCKET_NAME = String.format(
      "retrofitted-vehicles-data-%d",
      System.currentTimeMillis()
  );

  private static final Map<String, String[]> UPLOADER_TO_FILES = ImmutableMap.of(
      FIRST_UPLOADER_ID.toString(), new String[]{
          "first-uploader-records-all.csv"}
  );

  @Autowired
  private DatabaseInitializer databaseInitializer;

  @Autowired
  private S3Client s3Client;

  @Autowired
  private RetrofittedVehicleDtoCsvRepository csvRepository;

  @Autowired
  private SourceAwareRegisterService registerService;

  @Autowired
  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  @BeforeEach
  private void setUp() {
    createBucketAndFilesInS3();
    initializeDatabase();
  }

  @AfterEach
  private void tearDown() {
    deleteBucketAndFilesFromS3();
    cleanDatabase();
  }

  @Test
  public void shouldParseCsvFromS3() {
    // given
    String inputFilename = "first-uploader-records-all.csv";

    // when
    CsvFindResult csvFindResult = csvRepository.findAll(BUCKET_NAME,
        inputFilename);

    // then
    then(csvFindResult.getLicences()).hasSize(FIRST_UPLOADER_TOTAL_VEHICLES_COUNT);
    then(csvFindResult.getUploaderId()).isEqualTo(FIRST_UPLOADER_ID);
    then(csvFindResult.getValidationErrors()).isEmpty();
  }

  @Test
  void shouldRegisterVehiclesFromCsv() {
    //given
    String inputFilename = "first-uploader-records-all.csv";

    //when
    registerService
        .register(BUCKET_NAME, inputFilename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    //then
    then(retrofittedVehiclePostgresRepository.findAll()).containsExactlyInAnyOrder(
        VEHICLE_1, VEHICLE_2, VEHICLE_3, VEHICLE_4, VEHICLE_5
    );
  }


  private void createBucketAndFilesInS3() {
    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
    uploadFilesToS3(UPLOADER_TO_FILES);
  }

  private void deleteBucketAndFilesFromS3() {
    deleteFilesFromS3(filesToDelete());
    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
  }

  @SneakyThrows
  private void initializeDatabase() {
    log.info("Initialing database : start");
    databaseInitializer.initWithoutLicenceData();
    databaseInitializer.initRegisterJobData();
    log.info("Initialing database : finish");
  }

  private void cleanDatabase() {
    log.info("Clearing database : start");
    databaseInitializer.clear();
    log.info("Clearing database : finish");
  }

  private void uploadFilesToS3(Map<String, String[]> uploaderToFilesMap) {
    for (Entry<String, String[]> uploaderToFiles : uploaderToFilesMap.entrySet()) {
      String uploaderId = uploaderToFiles.getKey();
      String[] files = uploaderToFiles.getValue();

      for (String filename : files) {
        s3Client.putObject(builder -> builder.bucket(BUCKET_NAME)
                .key(filename)
                .metadata(
                    Collections.singletonMap(
                        RetrofittedVehicleDtoCsvRepository.UPLOADER_ID_METADATA_KEY,
                        uploaderId
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
}
