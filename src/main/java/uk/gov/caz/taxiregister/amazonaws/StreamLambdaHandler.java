package uk.gov.caz.taxiregister.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import uk.gov.caz.taxiregister.util.AwsHelpers;

@Slf4j
public class StreamLambdaHandler implements RequestStreamHandler {

  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    String input = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    log.info("Incoming attributes: " + dump(new ByteArrayInputStream(input.getBytes())));
    log.info("Incoming context: " + dump(context));
    initializeHandlerIfNull();
    handler.proxyStream(new ByteArrayInputStream(input.getBytes()), outputStream, context);
  }

  private String dump(InputStream inputStream) {
    try {
      ObjectMapper obj = new ObjectMapper();
      JsonNode node = obj.readTree(inputStream);
      return node.toString();
    } catch (Exception e) {
      log.error("Error: ", e);
      throw new RuntimeException(e);
    }
  }

  private String dump(Context context) {
    StringBuilder sb = new StringBuilder();
    CognitoIdentity ci = context.getIdentity();
    sb.append("Full context: ").append(context.toString());
    sb.append("IdentityId: ").append(ci.getIdentityId());
    sb.append("IdentityPoolId: ").append(ci.getIdentityPoolId());
    return sb.toString();
  }

  private void initializeHandlerIfNull() {
    if (handler == null) {
      handler = AwsHelpers.initSpringBootHandler();
    }
  }
}
