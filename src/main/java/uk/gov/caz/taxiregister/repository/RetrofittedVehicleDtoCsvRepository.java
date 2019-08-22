package uk.gov.caz.taxiregister.repository;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.taxiregister.model.CsvFindResult;
import uk.gov.caz.taxiregister.model.CsvParseResult;
import uk.gov.caz.taxiregister.service.CsvObjectMapper;
import uk.gov.caz.taxiregister.service.exception.S3InvalidUploaderIdFormatException;
import uk.gov.caz.taxiregister.service.exception.S3MaxFileSizeExceededException;
import uk.gov.caz.taxiregister.service.exception.S3MetadataException;

/**
 * A class that is responsible for managing vehicle data located at S3.
 */
@Repository
@Slf4j
public class RetrofittedVehicleDtoCsvRepository {

  public static final String UPLOADER_ID_METADATA_KEY = "uploader-id";
  public static final long MAX_FILE_SIZE_IN_BYTES = 100L * 1024 * 1024; // 100 MB

  private final S3Client s3Client;
  private final CsvObjectMapper csvObjectMapper;

  /**
   * Creates an instance of {@link RetrofittedVehicleDtoCsvRepository}.
   *
   * @param s3Client A client for AWS S3
   * @param csvObjectMapper An instance of {@link CsvObjectMapper}
   */
  public RetrofittedVehicleDtoCsvRepository(S3Client s3Client,
      CsvObjectMapper csvObjectMapper) {
    this.s3Client = s3Client;
    this.csvObjectMapper = csvObjectMapper;
  }

  /**
   * Reads the content of a UTF-8-encoded file located at S3 and maps it to a set of {@link
   * CsvFindResult}. The set is returned alongside the {@code uploaderId} which represents an entity
   * which uploaded the file.
   *
   * @param bucket The name of a S3 bucket
   * @param filename The name (key) of a file within a given bucket
   * @return {@link CsvParseResult} value object which contains the set with parsed vehicles data
   *     and the uploader id
   * @throws NullPointerException if {@code bucket} or {@code filename} is null or empty
   * @throws RuntimeException with {@link IOException} as a cause when {@link IOException}
   *     occurs
   * @throws NoSuchKeyException when the file's does not exist at S3
   * @throws S3MetadataException when the file's content type (set as a metadata) is {@code
   *     null} or not equal to 'text/csv
   */
  public CsvFindResult findAll(String bucket, String filename) {
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(bucket), "Bucket %s cannot be null or empty");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(filename), "Filename %s cannot be null or empty");

    HeadObjectResponse fileMetadata = getFileMetadata(bucket, filename);
    checkMaxFileSizePrecondition(fileMetadata);

    UUID uploaderId = getUploaderId(fileMetadata);
    try (InputStream inputStream = getS3FileInputStream(bucket, filename)) {
      CsvParseResult result = csvObjectMapper.read(inputStream);
      return new CsvFindResult(uploaderId, result.getRetrofittedVehicles(),
          result.getValidationErrors());
    } catch (IOException e) {
      log.error("IOException while reading file {}/{}", bucket, filename);
      throw new RuntimeException(e);
    }
  }

  private void checkMaxFileSizePrecondition(HeadObjectResponse fileMetadata) {
    Long fileSizeInBytes = fileMetadata.contentLength();
    if (fileSizeInBytes != null && fileSizeInBytes > MAX_FILE_SIZE_IN_BYTES) {
      throw new S3MaxFileSizeExceededException();
    }
  }

  private UUID getUploaderId(HeadObjectResponse fileMetadata) {
    try {
      return UUID.fromString(fileMetadata.metadata().get(UPLOADER_ID_METADATA_KEY));
    } catch (IllegalArgumentException e) {
      log.error("Invalid format of uploader-id: {}", e.getMessage());
      throw new S3InvalidUploaderIdFormatException();
    }
  }

  private HeadObjectResponse validateMetadata(HeadObjectResponse fileMetadata) {
    if (!fileMetadata.metadata().containsKey(UPLOADER_ID_METADATA_KEY)) {
      throw new S3MetadataException(
          "The file does not contain required metadata key: " + UPLOADER_ID_METADATA_KEY);
    }
    return fileMetadata;
  }

  private HeadObjectResponse getFileMetadata(String bucket, String filename) {
    try {
      HeadObjectRequest request = HeadObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .build();
      return validateMetadata(s3Client.headObject(request));
    } catch (NoSuchKeyException e) {
      log.error("Exception while getting file's {}/{} metadata - bucket or file does not exist",
          bucket,
          filename);
      throw e;
    }
  }

  private InputStream getS3FileInputStream(String bucket, String filename) {
    try {
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .build();
      return s3Client.getObjectAsBytes(getObjectRequest).asInputStream();
    } catch (NoSuchKeyException | NoSuchBucketException e) {
      log.error("Exception while getting file {}/{} - bucket/file does not exist", bucket,
          filename);
      throw e;
    }
  }
}
