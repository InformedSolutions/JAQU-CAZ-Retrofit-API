package uk.gov.caz.taxiregister;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import uk.gov.caz.taxiregister.annotation.IntegrationTest;
import uk.gov.caz.taxiregister.util.DatabaseInitializer;

/**
 * This class provides storage-specific methods for inserting Vehicles. Please check {@link
 * RegisterVehiclesAbstractTest} to get better understanding of tests steps that will be executed.
 *
 * It uses(and thus tests) {@link uk.gov.caz.taxiregister.service.RegisterFromCsvCommand} command.
 */
@IntegrationTest
@Import(DatabaseInitializer.class)
@Slf4j
public class RegisterByCsvTestIT extends RegisterVehiclesAbstractTest {

  //
  // TODO: uncomment and fix when tables are created
  //

//  private static final int FIRST_UPLOADER_TOTAL_VEHICLES_COUNT = 19;
//  private static final int SECOND_UPLOADER_TOTAL_VEHICLES_COUNT = 12;
//
//  private static final String BUCKET_NAME = String.format(
//      "ntr-data-%d",
//      System.currentTimeMillis()
//  );
//
//  private static final Map<String, String[]> UPLOADER_TO_FILES = ImmutableMap.of(
//      FIRST_UPLOADER_ID.toString(), new String[]{
//          "first-uploader-records-all.csv",
//          "first-uploader-records-all-but-five.csv",
//          "first-uploader-records-all-but-five-and-five-modified.csv"},
//      SECOND_UPLOADER_ID.toString(), new String[]{
//          "second-uploader-records-all.csv",
//          "second-uploader-records-all-and-five-modified.csv"}
//  );
//
//  private static final Path FILE_BASE_PATH = Paths.get("src", "it", "resources", "data", "csv");
//
//  @Autowired
//  private SourceAwareRegisterService registerService;
//
//  @Autowired
//  private S3Client s3Client;
//
//  @BeforeEach
//  private void prepareDataInS3() {
//    s3Client.createBucket(builder -> builder.bucket(BUCKET_NAME).acl(BucketCannedACL.PUBLIC_READ));
//    uploadFilesToS3(UPLOADER_TO_FILES);
//  }
//
//  @AfterEach
//  private void clearS3() {
//    deleteFilesFromS3(filesToDelete());
//    s3Client.deleteBucket(builder -> builder.bucket(BUCKET_NAME));
//  }
//
//  @Override
//  void whenVehiclesAreRegisteredAgainstEmptyDatabaseByFirstUploader() {
//    registerVehiclesFrom("first-uploader-records-all.csv");
//  }
//
//  @Override
//  void whenVehiclesAreRegisteredBySecondUploaderWithNoDataFromTheFirstOne() {
//    registerVehiclesFrom("second-uploader-records-all.csv");
//  }
//
//  @Override
//  void whenThereAreFiveVehicleLessRegisteredByFirstUploader() {
//    registerVehiclesFrom("first-uploader-records-all-but-five.csv");
//  }
//
//  @Override
//  void whenThreeVehiclesAreUpdatedByBothUploaders() {
//    registerVehiclesFrom("first-uploader-records-all-but-five-and-five-modified.csv");
//    registerVehiclesFrom("second-uploader-records-all-and-five-modified.csv");
//  }
//
//  @Override
//  int getFirstUploaderTotalVehiclesCount() {
//    return FIRST_UPLOADER_TOTAL_VEHICLES_COUNT;
//  }
//
//  @Override
//  int getSecondUploaderTotalVehiclesCount() {
//    return SECOND_UPLOADER_TOTAL_VEHICLES_COUNT;
//  }
//
//  private void registerVehiclesFrom(String filename) {
//    registerService
//        .register(BUCKET_NAME, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);
//  }
//
//  @Override
//  int getRegisterJobId() {
//    return S3_REGISTER_JOB_ID;
//  }
//
//  private void uploadFilesToS3(Map<String, String[]> uploaderToFilesMap) {
//    for (Entry<String, String[]> uploaderToFiles : uploaderToFilesMap.entrySet()) {
//      String uploaderId = uploaderToFiles.getKey();
//      String[] files = uploaderToFiles.getValue();
//
//      for (String filename : files) {
//        s3Client.putObject(builder -> builder.bucket(BUCKET_NAME)
//                .key(filename)
//                .metadata(
//                    Collections.singletonMap(
//                        TaxiPhvLicenceCsvRepository.UPLOADER_ID_METADATA_KEY,
//                        uploaderId
//                    )
//                ),
//            FILE_BASE_PATH.resolve(filename));
//      }
//    }
//  }
//
//  private void deleteFilesFromS3(List<String> filenames) {
//    for (String filename : filenames) {
//      s3Client.deleteObject(builder -> builder.bucket(BUCKET_NAME).key(filename));
//    }
//  }
//
//  private List<String> filesToDelete() {
//    return UPLOADER_TO_FILES.values()
//        .stream()
//        .map(Arrays::asList)
//        .flatMap(List::stream)
//        .collect(Collectors.toList());
//  }
}
