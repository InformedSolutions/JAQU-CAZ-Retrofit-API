package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.taxiregister.service.CsvFileOnS3MetadataExtractor.CSV_CONTENT_TYPE_METADATA_KEY;
import static uk.gov.caz.taxiregister.service.CsvFileOnS3MetadataExtractor.UPLOADER_ID_METADATA_KEY;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import com.google.common.collect.ImmutableMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.taxiregister.model.CsvContentType;
import uk.gov.caz.taxiregister.service.CsvFileOnS3MetadataExtractor.CsvMetadata;
import uk.gov.caz.taxiregister.service.exception.FatalErrorWithCsvFileMetadataException;

@ExtendWith(MockitoExtension.class)
class CsvFileOnS3MetadataExtractorTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String FILENAME = "filename";

  @Mock
  private S3Client mockedS3Client;

  @InjectMocks
  private CsvFileOnS3MetadataExtractor csvFileOnS3MetadataExtractor;

  @Test
  public void correctMetadataOnValidS3BucketAndFileShouldProduceProperResult() {
    // given
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString(),
        CsvContentType.RETROFIT_LIST.name());

    // when
    CsvMetadata csvMetadata = csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME);

    // then
    assertThat(csvMetadata).isNotNull();
    assertThat(csvMetadata.getUploaderId()).isEqualByComparingTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
    assertThat(csvMetadata.getCsvContentType()).isEqualByComparingTo(CsvContentType.RETROFIT_LIST);
  }

  @Test
  public void correctLowercaseMetadataOnValidS3BucketAndFileShouldProduceProperResult() {
    // given
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString().toLowerCase(),
        CsvContentType.RETROFIT_LIST.name().toLowerCase());

    // when
    CsvMetadata csvMetadata = csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME);

    // then
    assertThat(csvMetadata).isNotNull();
    assertThat(csvMetadata.getUploaderId()).isEqualByComparingTo(TYPICAL_REGISTER_JOB_UPLOADER_ID);
    assertThat(csvMetadata.getCsvContentType()).isEqualByComparingTo(CsvContentType.RETROFIT_LIST);
  }

  @Test
  public void whenUploaderIdHasInvalidUUIDSyntaxItShouldThrow() {
    // given
    prepareMockResponseFromS3Client("Invalid UUID", CsvContentType.RETROFIT_LIST.name());

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("Invalid format of uploader-id: Invalid UUID");
  }

  @Test
  public void whenCsvContentTypeHasInvalidSyntaxItShouldThrow() {
    // given
    prepareMockResponseFromS3Client(TYPICAL_REGISTER_JOB_UPLOADER_ID.toString(),
        "Invalid Csv Content Type");

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("Invalid format of csv-content-type: Invalid Csv Content Type");
  }

  @Test
  public void whenThereIsNoUploaderIdMetadataItShouldThrow() {
    // given
    prepareMockResponseFromS3ClientWithoutUploaderIdMetadata();

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("The file does not contain required metadata key: uploader-id");
  }

  @Test
  public void whenThereIsNoCsvContentTypeMetadataItShouldThrow() {
    // given
    prepareMockResponseFromS3ClientWithoutCsvContentTypeMetadata();

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage("The file does not contain required metadata key: csv-content-type");
  }

  @ParameterizedTest
  @MethodSource("s3ExceptionsProvider")
  public void exceptionsDuringMetadataFetchingAndParsingShouldProduceEmptyResult(
      Exception s3Exception) {
    // given
    prepareS3ClientToThrow(s3Exception);

    // when
    Throwable throwable = catchThrowable(
        () -> csvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, FILENAME));

    // then
    then(throwable)
        .isInstanceOf(FatalErrorWithCsvFileMetadataException.class)
        .hasMessage(
            "Exception while getting file's s3Bucket/filename metadata - bucket or file does not exist");
  }

  private void prepareMockResponseFromS3Client(String uploaderIdToReturn,
      String csvContentTypeToReturn) {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(
            ImmutableMap
                .of(UPLOADER_ID_METADATA_KEY, uploaderIdToReturn,
                    CSV_CONTENT_TYPE_METADATA_KEY, csvContentTypeToReturn))
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareMockResponseFromS3ClientWithoutUploaderIdMetadata() {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(
            ImmutableMap
                .of(CSV_CONTENT_TYPE_METADATA_KEY, CsvContentType.RETROFIT_LIST.name()))
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareMockResponseFromS3ClientWithoutCsvContentTypeMetadata() {
    HeadObjectResponse response = HeadObjectResponse.builder()
        .metadata(
            ImmutableMap
                .of(UPLOADER_ID_METADATA_KEY, TYPICAL_REGISTER_JOB_UPLOADER_ID.toString()))
        .build();
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willReturn(response);
  }

  private void prepareS3ClientToThrow(Exception s3Exception) {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(S3_BUCKET)
        .key(FILENAME)
        .build();
    given(mockedS3Client.headObject(request)).willThrow(s3Exception);
  }

  static Stream<Exception> s3ExceptionsProvider() {
    return Stream.of(
        NoSuchKeyException.builder().build()   // No S3 Bucket or file
    );
  }
}