package uk.gov.caz.taxiregister.amazonaws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import uk.gov.caz.taxiregister.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.taxiregister.service.AsyncBackgroundJobStarter;

@Component
@Profile("!development")
@Slf4j
public class AsyncLambdaBackgroundJobStarter implements AsyncBackgroundJobStarter {

  private final ObjectMapper objectMapper;
  private final LambdaClientBuilder lambdaClientBuilder;
  private final String lambdaName;

  /**
   * Constructs new instance of {@link AsyncLambdaBackgroundJobStarter} class.
   *
   * @param objectMapper Jackson mapper.
   * @param lambdaClientBuilder An implementation of {@link LambdaClientBuilder} interface that
   *     will be used to get instance of {@link LambdaClient}.
   * @param lambdaName Name of Lambda function that should be invoked.
   */
  public AsyncLambdaBackgroundJobStarter(ObjectMapper objectMapper,
      LambdaClientBuilder lambdaClientBuilder,
      @Value("${registerjob.lambda.name}") String lambdaName) {
    this.objectMapper = objectMapper;
    this.lambdaClientBuilder = lambdaClientBuilder;
    this.lambdaName = lambdaName;
  }

  @Override
  public void fireAndForgetRegisterCsvFromS3Job(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    logCallDetails(registerJobId, s3Bucket, fileName, correlationId);
    try {
      String lambdaJsonPayload = prepareLambdaJsonPayload(registerJobId, s3Bucket, fileName,
          correlationId);
      InvokeRequest invokeRequest = prepareInvokeRequestForFunction(lambdaName,
          lambdaJsonPayload);
      invokeLambda(invokeRequest);
    } catch (Exception e) {
      log.error("Error during invoking '" + lambdaName + "' Lambda", e);
    }
  }

  private void logCallDetails(int registerJobId, String s3Bucket, String fileName,
      String correlationId) {
    log.info(
        "Starting Async, fire and forget, Register job with parameters: JobID: {}, S3 Bucket: {}, "
            + "CSV File: {}, Correlation: {} and runner implementation: {}",
        registerJobId, s3Bucket, fileName, correlationId,
        AsyncLambdaBackgroundJobStarter.class.getSimpleName());
  }

  private String prepareLambdaJsonPayload(int registerJobId, String s3Bucket, String fileName,
      String correlationId)
      throws JsonProcessingException {
    RegisterCsvFromS3LambdaInput input = RegisterCsvFromS3LambdaInput.builder()
        .registerJobId(registerJobId).s3Bucket(s3Bucket)
        .fileName(fileName).correlationId(correlationId).build();
    return objectMapper.writeValueAsString(input);
  }

  private InvokeRequest prepareInvokeRequestForFunction(String lambdaFunctionName,
      String lambdaJsonPayload) {
    SdkBytes payloadSdkBytes = SdkBytes.fromUtf8String(lambdaJsonPayload);
    return InvokeRequest.builder()
        .invocationType(InvocationType.EVENT)
        .functionName(lambdaFunctionName)
        .payload(payloadSdkBytes)
        .build();
  }

  private void invokeLambda(InvokeRequest invokeRequest) {
    try (LambdaClient lambdaClient = lambdaClientBuilder.build()) {
      InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
      log.info(
          "Successfully invoked (asynchronously) '{}' Lambda. "
              + "InvokeResponse: {}",
          lambdaName, invokeResponse.toString());
    }
  }
}
