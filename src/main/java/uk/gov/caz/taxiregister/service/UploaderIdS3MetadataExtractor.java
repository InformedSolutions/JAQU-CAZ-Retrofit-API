package uk.gov.caz.taxiregister.service;

import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.taxiregister.service.exception.S3MetadataException;

/**
 * Tries to get 'uploader-id' metadata from file on S3.
 */
@Slf4j
@Service
public class UploaderIdS3MetadataExtractor {

  @VisibleForTesting
  static final String UPLOADER_ID_METADATA_KEY = "uploader-id";

  private final S3Client s3Client;

  /**
   * Creates an instance of {@link UploaderIdS3MetadataExtractor}.
   *
   * @param s3Client A client for AWS S3
   */
  public UploaderIdS3MetadataExtractor(S3Client s3Client) {
    this.s3Client = s3Client;
  }

  /**
   * Tries to get 'uploader-id' metadata from file on S3 bucket.
   *
   * @param s3Bucket The name of the bucket at S3 where files with vehicles data is stored.
   * @param filename The name of the key at S3 where vehicles data is stored.
   * @return {@link Optional} with {@link UUID} that contain uploader id.
   */
  public Optional<UUID> getUploaderId(String s3Bucket, String filename) {
    try {
      HeadObjectResponse headObjectResponse = getFileMetadata(s3Bucket, filename);
      return Optional.of(getUploaderId(headObjectResponse));
    } catch (NoSuchKeyException e) {
      log.error("Exception while getting file's {}/{} metadata - bucket or file does not exist",
          s3Bucket,
          filename);
    } catch (S3MetadataException e) {
      log.error("No required metadata", e);
    } catch (IllegalArgumentException e) {
      log.error("Invalid format of uploader-id: {}", e.getMessage());
    }
    return Optional.empty();
  }

  private UUID getUploaderId(HeadObjectResponse fileMetadata) throws IllegalArgumentException {
    return UUID.fromString(fileMetadata.metadata().get(UPLOADER_ID_METADATA_KEY));
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
      throws S3MetadataException {
    if (!fileMetadata.metadata().containsKey(UPLOADER_ID_METADATA_KEY)) {
      throw new S3MetadataException(
          "The file does not contain required metadata key: " + UPLOADER_ID_METADATA_KEY);
    }
    return fileMetadata;
  }
}
