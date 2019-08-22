package uk.gov.caz.retrofit.model.registerjob;

/**
 * Statuses available for register job.
 */
public enum RegisterJobStatus {
  STARTING,
  STARTUP_FAILURE_NO_S3_BUCKET_OR_FILE,
  STARTUP_FAILURE_NO_ACCESS_TO_S3, // TODO: can we detect this failure? Not set anywhere now
  STARTUP_FAILURE_NO_UPLOADER_ID,
  STARTUP_FAILURE_INVALID_UPLOADER_ID,
  STARTUP_FAILURE_TOO_LARGE_FILE,
  RUNNING,
  FINISHED_SUCCESS,
  FINISHED_FAILURE_VALIDATION_ERRORS,
  UNKNOWN_FAILURE;
}
