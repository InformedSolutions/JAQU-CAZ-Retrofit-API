package uk.gov.caz.taxiregister.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.taxiregister.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.taxiregister.service.SourceAwareRegisterService;
import uk.gov.caz.taxiregister.util.AwsHelpers;

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
    initializeHandlerAndService();
    return String.valueOf(
        sourceAwareRegisterService.register(
            registerCsvFromS3LambdaInput.getS3Bucket(),
            registerCsvFromS3LambdaInput.getFileName(),
            registerCsvFromS3LambdaInput.getRegisterJobId(),
            registerCsvFromS3LambdaInput.getCorrelationId())
            .isSuccess());
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
