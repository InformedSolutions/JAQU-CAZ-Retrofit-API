package uk.gov.caz.retrofit.service;

/**
 * Starts arbitrary background tasks.
 */
public interface AsyncBackgroundJobStarter {

  /**
   * Starts arbitrary background task which registers Taxi and PHV licences from CSV file located on
   * S3.
   * <p>
   * Runs task in 'fire and forget' mode - so returns void and allows task to continue in the
   * background unsupervised.
   * </p>
   *
   * @param registerJobId ID of register job that is being supervised by {@link
   *     RegisterJobSupervisor}. Running job internals must use this ID to tell supervisor about
   *     progress and status changes.
   * @param s3Bucket Name of S3 bucket that holds CSV file.
   * @param fileName Name of CSV file.
   * @param correlationId UUID formatted string to track the request through the enquiries
   *     stack.
   */
  void fireAndForgetRegisterCsvFromS3Job(int registerJobId, String s3Bucket, String fileName,
      String correlationId);
}
