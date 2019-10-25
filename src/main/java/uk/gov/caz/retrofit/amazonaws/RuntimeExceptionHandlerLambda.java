package uk.gov.caz.retrofit.amazonaws;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.CloudWatchLogsEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Stopwatch;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.gov.caz.retrofit.dto.CloudWatchDataMessage;
import uk.gov.caz.retrofit.dto.CloudWatchDataMessage.LogEvent;
import uk.gov.caz.retrofit.dto.RegisterCsvFromS3LambdaInput;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobStatus;
import uk.gov.caz.retrofit.service.RegisterJobSupervisor;

@Slf4j
/**
 * Lambda function which handles Timeout and OutOfMemory error
 * that might occur during MOD csv import
 */
public class RuntimeExceptionHandlerLambda extends LambdaHandler implements RequestStreamHandler  {
  private RegisterJobSupervisor registerJobSupervisor;

  private final String errorMessage = "Fail to cancel dangling jobs,"
      + " please see the CloudWatch logs for more details";

  /**
   * Lambda function handler.
   */
  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
      throws IOException {

    String input = StreamUtils.copyToString(inputStream, Charset.defaultCharset());
    Stopwatch timer = Stopwatch.createStarted();
    initializeService();
    log.info("Handler initialization took {}ms", timer.elapsed(TimeUnit.MILLISECONDS));
    try {
      EventProcessor eventProcessor = new EventProcessor(registerJobSupervisor);
      eventProcessor.process(input);
    } catch (Exception e) {
      log.error("Error: ", e);
      // The exception will be recorded under in Lambda Errors metrics
      // which allows it to be handled appropriately if necessary
      throw new IOException(errorMessage);
    }
    log.info("Jobs cancelling took {}ms", timer.stop().elapsed(TimeUnit.MILLISECONDS));
  }

  /**
   * Initialize the Application Context.
   */
  private void initializeService() {
    if (handler == null) {
      registerJobSupervisor = getBean(handler, RegisterJobSupervisor.class);
    }
  }

  /**
   * Get a Bean instance.
   * @param handler The servlet context container.
   * @param beanClass the Bean Java class.
   * @return a Bean instance of parameterized Class.
   */
  private <T> T getBean(SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler,
      Class<T> beanClass) {
    return WebApplicationContextUtils
        .getWebApplicationContext(handler.getServletContext()).getBean(beanClass);
  }

  /**
   * Lambda Timeout and OutOfMemory event processor.
   */
  public static class EventProcessor {
    private RegisterJobSupervisor registerJobSupervisor;
    public static final String LAMBDA_TIMEOUT_EXCEPTION = "Lambda timeout exception. "
        + "Please contact administrator to get assisstance.";
    public static final String LAMBDA_OUTOFMEMORY_EXCEPTION = "OutOfMemory exception. "
        + "Please contact administrator to get assisstance.";

    /**
     * Creates an {@link RuntimeExceptionHandlerLambda.EventProcessor}.
     * @param registerJobSupervisor a RegisterJobSupervisor instance.
     */
    public EventProcessor(RegisterJobSupervisor registerJobSupervisor) {
      this.registerJobSupervisor = registerJobSupervisor;
    }

    /**
     * Process event.
     * @param input The event.
     * @throws Exception If the function is unable to cancel the Job
     */
    public void process(String input) throws Exception {
      Object event;
      try {
        event = convert(input,SNSEvent.class);
      } catch (Exception e) {
        event = convert(input,CloudWatchLogsEvent.class);
      }
      if (event != null) {
        if (event instanceof SNSEvent) {
          processSnsEvent((SNSEvent)event);
        } else if (event instanceof CloudWatchLogsEvent) {
          processCloudWatchLogsEvent((CloudWatchLogsEvent)event);
        }
      }
    }

    /**
     * Process OutOfMemrory CloudWatch Log Event.
     * @param event A CloudWatch Log Event.
     */
    private void processCloudWatchLogsEvent(CloudWatchLogsEvent event) throws Exception {
      byte[] decodedBytes = Base64.getDecoder().decode(event.getAwsLogs().getData());
      String decodedString = decompressByteArray(decodedBytes);
      CloudWatchDataMessage cloudWatchDataMessage = convert(decodedString,
          CloudWatchDataMessage.class);
      for (LogEvent logEvent : cloudWatchDataMessage.getLogEvents()) {
        String message = logEvent.getMessage();
        String input = message.substring(message.indexOf("{"));
        RegisterCsvFromS3LambdaInput originalInput = convert(input,
            RegisterCsvFromS3LambdaInput.class);
        cancelJob(originalInput.getRegisterJobId(), EventProcessor.LAMBDA_OUTOFMEMORY_EXCEPTION);
      }
    }

    /**
     * Process Lambda Timeout SNS Event.
     * @param event A SNS Event.
     */
    private void processSnsEvent(SNSEvent event) throws Exception {

      for (SNSRecord record : event.getRecords()) {
        RegisterCsvFromS3LambdaInput originalInput = convert(record.getSNS().getMessage(),
            RegisterCsvFromS3LambdaInput.class);
        cancelJob(originalInput.getRegisterJobId(), EventProcessor.LAMBDA_TIMEOUT_EXCEPTION);
      }
    }

    /**
     * Cancel the dangling job.
     * @param jobId The job Id.
     */
    private void cancelJob(int jobId, String reason) throws Exception {
      registerJobSupervisor.markFailureWithValidationErrors(jobId,
          RegisterJobStatus.ABORTED,
          Arrays.asList(ValidationError.requestProcessingError(reason)));
    }

    /**
     * Deserialize Json content into Java object.
     * @param input The Json content.
     * @param eventClass A Java class.
     * @return A Java instance of the parameterized Class.
     */
    private <T> T convert(String input, Class<T> eventClass)
        throws IOException, JsonParseException, JsonMappingException {
      ObjectMapper obj = new ObjectMapper();
      obj.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
      obj.registerModule(new JodaModule());
      return obj.readValue(input, eventClass);
    }

    /**
     * Decompress GZip data.
     * @param bytes A byte array of data compressed in GZIP format.
     * @return A String represent the decompressed data.
     */
    private String decompressByteArray(byte[] bytes) throws Exception {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
      GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
      InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream,
          StandardCharsets.UTF_8);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        output.append(line);
      }
      return output.toString();
    }
  }
}