package uk.gov.caz.retrofit.service;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("development | integration-tests")
@Slf4j
public class AsyncJavaBackgroundJobStarter implements AsyncBackgroundJobStarter {

  private SourceAwareRegisterService registerService;

  public AsyncJavaBackgroundJobStarter(SourceAwareRegisterService registerService) {
    this.registerService = registerService;
  }

  @Override
  public void fireAndForgetRegisterCsvFromS3Job(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    logCallDetails(registerJobId, s3Bucket, fileName, correlationId);
    CompletableFuture.supplyAsync(
        () -> registerService.register(s3Bucket, fileName, registerJobId, correlationId));
  }

  private void logCallDetails(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    log.info(
        "Starting Async, fire and forget, Register job with parameters: JobID: {}, S3 Bucket: {}, "
            + "CSV File: {}, Correlation: {} and runner implementation: {}",
        registerJobId, s3Bucket, fileName, correlationId,
        AsyncJavaBackgroundJobStarter.class.getSimpleName());
  }
}
