package uk.gov.caz.retrofit.amazonaws;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StreamUtils;
import uk.gov.caz.retrofit.Application;

@Slf4j
public class StreamLambdaHandler implements RequestStreamHandler {

  private static final String KEEP_WARM_ACTION = "keep-warm";
  private static final Splitter SPLITTER = Splitter.on(',')
      .trimResults()
      .omitEmptyStrings();
  
  private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

  /**
   * Private method for instantiating a Spring handler to serve the inbound
   * request.
   */
  private void initialiseSpringHandler() {
    if (handler == null) {
      try {
        String listOfActiveSpringProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (listOfActiveSpringProfiles != null) {
          handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class,
              splitToArray(listOfActiveSpringProfiles));
        } else {
          handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(Application.class);
        }
      } catch (ContainerInitializationException e) {
        // if we fail here. We re-throw the exception to force another cold
        // start
        log.error(e.getMessage());
        throw new RuntimeException(
            "Could not initialize Spring Boot application", e);
      }
    }
  }
  
  /**
   * Private helper for splitting a comma delimited string to an array.
   */
  private String[] splitToArray(String activeProfiles) {
    return SPLITTER.splitToList(activeProfiles).toArray(new String[0]);
  }
  
  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {
    
    // Initialise Spring handler under both real request conditions
    // as well as warmup requests.
    initialiseSpringHandler();

    byte[] inputBytes = StreamUtils.copyToByteArray(inputStream);

    if (isWarmupRequest(toString(inputBytes))) {
      delayToAllowAnotherLambdaInstanceWarming();
      try (Writer osw = new OutputStreamWriter(outputStream)) {
        osw.write(LambdaContainerStats.getStats());
      }
    } else {
      log.info("Processing API request");
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
    boolean isWarmupRequest = action.contains(KEEP_WARM_ACTION);

    if (isWarmupRequest) {
      log.info("Received lambda warmup request");
    }

    return isWarmupRequest;
  }

  /**
   * Contain information about the lambda container.
   */
  static class LambdaContainerStats {

    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static LocalDateTime latestRequestTime;

    private LambdaContainerStats() {
      throw new IllegalStateException("Utility class");
    }

    /**
     * Set the time that the container serves a request.
     */
    public static void setLatestRequestTime(LocalDateTime dt) {
      latestRequestTime = dt;
    }

    /**
     * Get the time that the container last served a request.
     */
    public static LocalDateTime getLatestRequestTime() {
      return latestRequestTime;
    }

    /**
     * Get the container stats.
     *
     * @return a string that contains lambda container Id and (optionally) the time that the
     *     container last serve a request.
     */
    public static String getStats() {
      try {
        ObjectMapper obj = new ObjectMapper();
        Map<String, String> retVal = new HashMap<>();
        retVal.put("instanceId", INSTANCE_ID);
        if (latestRequestTime != null) {
          retVal.put("latestRequestTime", latestRequestTime.format(formatter));
        }
        return obj.writeValueAsString(retVal);
      } catch (JsonProcessingException ex) {
        return String.format("\"instanceId\": \"%s\"", INSTANCE_ID);
      }
    }
  }
}