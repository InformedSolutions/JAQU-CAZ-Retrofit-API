package uk.gov.caz.taxiregister.service;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.service.exception.RequiredLicenceTypesAbsentInDbException;
import uk.gov.caz.taxiregister.service.exception.S3InvalidUploaderIdFormatException;
import uk.gov.caz.taxiregister.service.exception.S3MaxFileSizeExceededException;
import uk.gov.caz.taxiregister.service.exception.S3MetadataException;

@Component
@Slf4j
public class RegisterFromCsvExceptionResolver {

  private static final RegisterResult BUCKET_OR_FILE_NOT_EXISTS = RegisterResult.failure(
      ValidationError.s3Error("S3 bucket or file not found or not accessible"));
  private static final RegisterResult ABSENT_UPLOADER_ID = RegisterResult.failure(
      ValidationError.s3Error("'uploader-id' not found in file's metadata"));
  private static final RegisterResult MALFORMED_UPLOADER_ID = RegisterResult.failure(
      ValidationError.s3Error("Malformed ID of an entity which want to "
          + "register vehicles by CSV file. Expected a unique identifier (UUID)"));
  private static final RegisterResult MAX_FILE_SIZE_EXCEEDED = RegisterResult.failure(
      ValidationError.s3Error("Uploaded file is too large. Maximum allowed: "
          + RetrofittedVehicleDtoCsvRepository.MAX_FILE_SIZE_IN_BYTES + " bytes"));
  private static final RegisterResult INTERNAL_ERROR = RegisterResult.failure(
      ValidationError.internal());

  /**
   * These will be displayed to the end user.
   */
  private static final Map<Class<? extends Exception>, RegisterResult> EXCEPTION_TO_RESULT
      = ImmutableMap.of(
      NoSuchKeyException.class, BUCKET_OR_FILE_NOT_EXISTS,
      S3MetadataException.class, ABSENT_UPLOADER_ID,
      S3InvalidUploaderIdFormatException.class, MALFORMED_UPLOADER_ID,
      S3MaxFileSizeExceededException.class, MAX_FILE_SIZE_EXCEEDED,
      RequiredLicenceTypesAbsentInDbException.class, INTERNAL_ERROR
  );

  /**
   * These will be saved in the databased and used internally.
   */
  private static final Map<Class<? extends Exception>, RegisterJobStatus> EXCEPTION_TO_STATUS
      = ImmutableMap.<Class<? extends Exception>, RegisterJobStatus>builder()
      .put(NoSuchKeyException.class, RegisterJobStatus.STARTUP_FAILURE_NO_S3_BUCKET_OR_FILE)
      .put(S3MetadataException.class, RegisterJobStatus.STARTUP_FAILURE_NO_UPLOADER_ID)
      .put(S3InvalidUploaderIdFormatException.class,
          RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID)
      .put(S3MaxFileSizeExceededException.class, RegisterJobStatus.STARTUP_FAILURE_TOO_LARGE_FILE)
      .put(RequiredLicenceTypesAbsentInDbException.class,
          RegisterJobStatus.STARTUP_FAILURE_MISSING_LICENCE_TYPES)
      .build();

  private static final RegisterResult UNKNOWN = RegisterResult.failure(ValidationError.unknown());

  /**
   * Maps an exception to {@link RegisterResult}. If the exception class is not 'supported' by this
   * resolver an unknown error is returned.
   *
   * @param e An exception which is to be mapped to {@link RegisterResult}
   * @return An instance of {@link RegisterResult} which {@code e} has been mapped to.
   */
  public RegisterResult resolve(Exception e) {
    RegisterResult result = EXCEPTION_TO_RESULT.get(e.getClass());
    if (result == null) {
      log.error("Unknown error occurred while processing registration (CSV)", e);
      return UNKNOWN;
    }
    return result;
  }

  /**
   * Maps an exception to {@link RegisterJobStatus}. If the exception class is not 'supported' by
   * this resolver an 'UNKNOWN_FAILURE' is returned.
   *
   * @param e An exception which is to be mapped to {@link RegisterJobStatus}
   * @return An instance of {@link RegisterJobStatus} which {@code e} has been mapped to.
   */
  public RegisterJobStatus resolveToRegisterJobFailureStatus(Exception e) {
    RegisterJobStatus status = EXCEPTION_TO_STATUS.get(e.getClass());
    if (status == null) {
      log.error("Unknown error occurred while processing registration (CSV)", e);
      return RegisterJobStatus.UNKNOWN_FAILURE;
    }
    return status;
  }
}
