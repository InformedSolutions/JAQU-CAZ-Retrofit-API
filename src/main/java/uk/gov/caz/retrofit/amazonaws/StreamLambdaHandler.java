package uk.gov.caz.retrofit.amazonaws;

import static uk.gov.caz.awslambda.AwsHelpers.splitToArray;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootProxyHandlerBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StreamUtils;
import uk.gov.caz.retrofit.Application;
import uk.gov.caz.retrofit.dto.LambdaContainerStats;

public class StreamLambdaHandler implements RequestStreamHandler {

  private static final String KEEP_WARM_ACTION = "keep-warm";
  /*
   * This field is `static` to avoid being garbage collected and in turn it prevents the application
   * from being initialized more than once within one Lambda deployment
   */
  private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  static {
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
      throw new RuntimeException("Could not initialize Spring Boot application", e);
    }
  }

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    byte[] inputBytes = StreamUtils.copyToByteArray(inputStream);
    if (isWarmupRequest(toString(inputBytes))) {
      delayToAllowAnotherLambdaInstanceWarming();
      try (Writer osw = new OutputStreamWriter(outputStream)) {
        osw.write(LambdaContainerStats.getStats());
      }
    } else {
      LambdaContainerStats.setLatestRequestTime(LocalDateTime.now());
      handler.proxyStream(toInputStream(inputBytes), outputStream, context);
    }
  }

  /**
   * Converts byte array to {@link InputStream}.
   *
   * @param inputBytes Input byte array.
   * @return {@link InputStream} over byte array.
   * @throws IOException When unable to convert.
   */
  @NotNull
  private InputStream toInputStream(byte[] inputBytes) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(inputBytes)) {
      return inputStream;
    }
  }

  /**
   * Converts {@code inputBytes} to an UTF-8 encoded string.
   */
  private String toString(byte[] inputBytes) {
    return new String(inputBytes, StandardCharsets.UTF_8);
  }

  /**
   * Delay lambda response to allow subsequent keep-warm requests to be routed to a different lambda
   * container.
   *
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

  /**
   * Determine if the incoming request is a keep-warm one.
   *
   * @param action the request under examination.
   * @return true if the incoming request is a keep-warm one otherwise false.
   */
  private boolean isWarmupRequest(String action) {
    return action.contains(KEEP_WARM_ACTION);
  }
}