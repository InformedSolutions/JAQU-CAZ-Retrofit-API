package uk.gov.caz.retrofit.amazonaws;

import static uk.gov.caz.awslambda.AwsHelpers.splitToArray;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.util.StreamUtils;
import uk.gov.caz.retrofit.Application;
import uk.gov.caz.retrofit.dto.LambdaContainerStats;

@Slf4j
public class StreamLambdaHandler implements RequestStreamHandler {

  private static final String KEEP_WARM_ACTION = "keep-warm";
  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  /**
   * Default constructor.
   */
  public StreamLambdaHandler() {
    long startTime = Instant.now().toEpochMilli();
    try {
      // For applications that take longer than 10 seconds to start, use the async builder:
      String listOfActiveSpringProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
      LambdaContainerHandler.getContainerConfig().setInitializationTimeout(60_000);
      if (listOfActiveSpringProfiles != null) {
        handler = new SpringBootProxyHandlerBuilder()
            .defaultProxy()
            .asyncInit(startTime)
            .springBootApplication(Application.class)
            .profiles(splitToArray(listOfActiveSpringProfiles))
            .buildAndInitialize();
      } else {
        handler = new SpringBootProxyHandlerBuilder()
            .defaultProxy()
            .asyncInit(startTime)
            .springBootApplication(Application.class)
            .buildAndInitialize();
      }
    } catch (ContainerInitializationException e) {
      // if we fail here. We re-throw the exception to force another cold start
      throw new IllegalStateException("Could not initialize Spring Boot application", e);
    }
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    String input = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    log.info("Incoming attributes: " + dump(new ByteArrayInputStream(input.getBytes())));
    log.info("Incoming context: " + dump(context));
    if (isWarmupRequest(input)) {
      delayToAllowAnotherLambdaInstanceWarming();
      try (Writer osw = new OutputStreamWriter(outputStream)) {
        osw.write(LambdaContainerStats.getStats());
      }
    } else {
      LambdaContainerStats.setLatestRequestTime(LocalDateTime.now());
      handler.proxyStream(new ByteArrayInputStream(input.getBytes()), outputStream, context);
    }
  }

  /**
   * Delay lambda response to allow subsequent keep-warm requests
   * to be routed to a different lambda container.
   * @throws IOException when it is impossible to pause the thread
   */
  private void delayToAllowAnotherLambdaInstanceWarming() throws IOException {
    try {
      Thread.sleep(Integer.parseInt(
          Optional.ofNullable(
              System.getenv("thundra_lambda_warmup_warmupSleepDuration"))
          .orElse("100")));
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private String dump(InputStream inputStream) {
    try {
      ObjectMapper obj = new ObjectMapper();
      JsonNode node = obj.readTree(inputStream);
      return node.toString();
    } catch (Exception e) {
      log.error("Error: ", e);
      throw new JsonParseException(e);
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
  
  /**
   * Determine if the incoming request is a keep-warm one.
   *
   * @param action the request under examination.
   * @return true if the incoming request is a keep-warm one
   *         otherwise false.
   */
  private boolean isWarmupRequest(String action) {
    return action.indexOf(KEEP_WARM_ACTION) >= 0;
  }
}
