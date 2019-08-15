package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import java.util.UUID;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.taxiregister.model.CsvContentType;
import uk.gov.caz.taxiregister.service.exception.FatalErrorWithCsvFileMetadataException;

/**
 * Tries to get 'uploader-id' and 'csv-content-type' metadata from file on S3.
 */
@Slf4j
@Service
public class CsvFileOnS3MetadataExtractor {

  @VisibleForTesting
  static final String UPLOADER_ID_METADATA_KEY = "uploader-id";

  @VisibleForTesting
  static final String CSV_CONTENT_TYPE_METADATA_KEY = "csv-content-type";

  private final S3Client s3Client;

  /**
   * Creates an instance of {@link CsvFileOnS3MetadataExtractor}.
   *
   * @param s3Client A client for AWS S3
   */
  public CsvFileOnS3MetadataExtractor(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  @Value
  public static class CsvMetadata {

    UUID uploaderId;

    CsvContentType csvContentType;
  }

  /**
   * Tries to get 'uploader-id' and 'csv-content-type' metadata from file on S3 bucket.
   *
   * @param s3Bucket The name of the bucket at S3 where files with vehicles data is stored.
   * @param filename The name of the key at S3 where vehicles data is stored.
   * @return {@link CsvMetadata} with {@link UUID} that contains uploader id and {@link
   *     CsvContentType} that contains type of data inside CSV.
   * @throws FatalErrorWithCsvFileMetadataException if CSV file is unreachable, or has no
   *     metadata or metadata has invalid format.
   */
  public CsvMetadata getRequiredMetadata(String s3Bucket, String filename)
      throws FatalErrorWithCsvFileMetadataException {
    try {
      HeadObjectResponse headObjectResponse = getFileMetadata(s3Bucket, filename);
      return new CsvMetadata(getUploaderId(headObjectResponse),
          getCsvContentType(headObjectResponse));
    } catch (NoSuchKeyException e) {
      String error = String
          .format("Exception while getting file's %s/%s metadata - bucket or file does not exist",
              s3Bucket, filename);
      log.error(error);
      throw new FatalErrorWithCsvFileMetadataException(error);
    }
  }

  private HeadObjectResponse getFileMetadata(String s3Bucket, String filename)
      throws NoSuchKeyException {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(s3Bucket)
        .key(filename)
        .build();
    return validateMetadata(s3Client.headObject(request));
  }

  private HeadObjectResponse validateMetadata(HeadObjectResponse fileMetadata)
      throws FatalErrorWithCsvFileMetadataException {
    if (!fileMetadata.metadata().containsKey(UPLOADER_ID_METADATA_KEY)) {
      throw new FatalErrorWithCsvFileMetadataException(
          "The file does not contain required metadata key: " + UPLOADER_ID_METADATA_KEY);
    }
    if (!fileMetadata.metadata().containsKey(CSV_CONTENT_TYPE_METADATA_KEY)) {
      throw new FatalErrorWithCsvFileMetadataException(
          "The file does not contain required metadata key: " + CSV_CONTENT_TYPE_METADATA_KEY);
    }
    return fileMetadata;
  }

  private UUID getUploaderId(HeadObjectResponse fileMetadata) throws IllegalArgumentException {
    String unparsedUploaderId = fileMetadata.metadata().get(UPLOADER_ID_METADATA_KEY);
    try {
      return UUID.fromString(unparsedUploaderId);
    } catch (IllegalArgumentException e) {
      String error = "Invalid format of uploader-id: " + unparsedUploaderId;
      log.error(error);
      throw new FatalErrorWithCsvFileMetadataException(error);
    }
  }

  private CsvContentType getCsvContentType(HeadObjectResponse fileMetadata) {
    String unparsedCsvContentType = fileMetadata.metadata().get(CSV_CONTENT_TYPE_METADATA_KEY);
    try {
      return CsvContentType.valueOf(unparsedCsvContentType.toUpperCase());
    } catch (IllegalArgumentException e) {
      String error = "Invalid format of csv-content-type: " + unparsedCsvContentType;
      log.error(error);
      throw new FatalErrorWithCsvFileMetadataException(error);
    }
  }
}
