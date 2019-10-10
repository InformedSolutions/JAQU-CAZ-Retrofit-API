package uk.gov.caz.retrofit.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.correlationid.Constants;
import uk.gov.caz.retrofit.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.retrofit.service.RegisterResult;
import uk.gov.caz.retrofit.service.SourceAwareRegisterService;
import uk.gov.caz.retrofit.util.AwsHelpers;

@Slf4j
public class RetrofitRegisterCsvFromS3Lambda implements
    RequestHandler<RegisterCsvFromS3LambdaInput, String> {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
  private SourceAwareRegisterService sourceAwareRegisterService;

  @Override
  public String handleRequest(RegisterCsvFromS3LambdaInput registerCsvFromS3LambdaInput,
      Context context) {
    if (isWarmerPing(registerCsvFromS3LambdaInput)) {
      return "OK";
    }
    Preconditions.checkArgument(!Strings.isNullOrEmpty(registerCsvFromS3LambdaInput.getS3Bucket()),
        "Invalid input, 's3Bucket' is blank or null");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(registerCsvFromS3LambdaInput.getFileName()),
        "Invalid input, 'fileName' is blank or null");
    Preconditions
        .checkArgument(!Strings.isNullOrEmpty(registerCsvFromS3LambdaInput.getCorrelationId()),
            "Invalid input, 'correlationId' is blank or null");
    Stopwatch timer = Stopwatch.createStarted();
    ObjectMapper obj = new ObjectMapper();
    String registerResult = "false";
    initializeHandlerAndService();
    log.info("Handler initialization took {}", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      setCorrelationIdInMdc(registerCsvFromS3LambdaInput.getCorrelationId());
      RegisterResult result = sourceAwareRegisterService.register(
            registerCsvFromS3LambdaInput.getS3Bucket(),
            registerCsvFromS3LambdaInput.getFileName(),
            registerCsvFromS3LambdaInput.getRegisterJobId(),
            registerCsvFromS3LambdaInput.getCorrelationId());
      registerResult = String.valueOf(result.isSuccess());
      log.info("Register method took {}", timer.stop().elapsed(TimeUnit.MILLISECONDS));
    } catch (OutOfMemoryError error) {
      try {
        log.info("OutOfMemoryError RegisterCsvFromS3Lambda {}",
            obj.writeValueAsString(registerCsvFromS3LambdaInput));
      } catch (JsonProcessingException e) {
        log.error("JsonProcessingException", e);
      } 
    } finally {
      removeCorrelationIdFromMdc();
    }
    return registerResult;
  }

  private void setCorrelationIdInMdc(String correlationId) {
    MDC.put(Constants.X_CORRELATION_ID_HEADER, correlationId);
  }

  private void removeCorrelationIdFromMdc() {
    MDC.remove(Constants.X_CORRELATION_ID_HEADER);
  }

  private boolean isWarmerPing(RegisterCsvFromS3LambdaInput registerCsvFromS3LambdaInput) {
    String action = registerCsvFromS3LambdaInput.getAction();
    if (Strings.isNullOrEmpty(action)) {
      return false;
    }
    return action.equalsIgnoreCase("keep-warm");
  }

  private void initializeHandlerAndService() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler();
      sourceAwareRegisterService = getBean(handler, SourceAwareRegisterService.class);
    }
  }

  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> exampleServiceClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(exampleServiceClass);
  }
}
