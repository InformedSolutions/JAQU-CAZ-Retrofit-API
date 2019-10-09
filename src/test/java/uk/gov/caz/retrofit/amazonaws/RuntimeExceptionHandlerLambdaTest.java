package uk.gov.caz.retrofit.amazonaws;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import uk.gov.caz.retrofit.amazonaws.RuntimeExceptionHandlerLambda.EventProcessor;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobStatus;
import uk.gov.caz.retrofit.service.RegisterJobSupervisor;

public class RuntimeExceptionHandlerLambdaTest {
  
  private RegisterJobSupervisor mockedRegisterJobSupervisor;
  @Captor
  private ArgumentCaptor<List<ValidationError>> validationErrors;
  @Captor
  private ArgumentCaptor<RegisterJobStatus> valueCapture;
  @Captor
  private ArgumentCaptor<Integer> jobIdCaptured;
 
  @BeforeEach
  public void init() {
    MockitoAnnotations.initMocks(this);
    mockedRegisterJobSupervisor = mock(RegisterJobSupervisor.class);
  }
  
  @Test
  public void givenValidSnsEventJobWillBeCancelled() throws Exception {
    String eventString = "{" + 
        "    \"Records\": [" + 
        "        {" + 
        "            \"EventSource\": \"aws:sns\"," + 
        "            \"EventVersion\": \"1.0\"," + 
        "            \"EventSubscriptionArn\": \"arn:aws:sns:eu-west-2:018330602464:RegisterCsvFromS3DeadLetterTopic:1e39cd2a-3f7e-4534-b1f0-6490061dd633\"," + 
        "            \"Sns\": {" + 
        "                \"Type\": \"Notification\"," + 
        "                \"MessageId\": \"b8ca6a4f-91f0-5b4c-bdb0-3fa5edb8b0e4\"," + 
        "                \"TopicArn\": \"arn:aws:sns:eu-west-2:018330602464:RegisterCsvFromS3DeadLetterTopic\"," + 
        "                \"Subject\": null," + 
        "                \"Message\": \"{\\\"registerJobId\\\":48,\\\"s3Bucket\\\":\\\"s3Bucket\\\",\\\"fileName\\\":\\\"fileName\\\",\\\"correlationId\\\":\\\"correlationId\\\",\\\"action\\\":\\\"action\\\"}\"," + 
        "                \"Timestamp\": \"2019-09-05T02:59:20.437Z\"," + 
        "                \"SignatureVersion\": \"1\"," + 
        "                \"Signature\": \"PvRd3JtfQ6xM0N0dNUMPye1Rh8uXT5CxfZm54yqMYGuDFyqXyQ8E/sirZnQle6yFWAwlKPv8Iv3W3i6jV2iFaIw+j5a7j5GdehNXrMm0rzQz9QIe3KuUhFkkkwPcRZyb6fdT+Nla8uYnBCf9PhT4jg2kcNUmQTfbxCZzIqFfUqRqvgbbaHVOoV29ylZlz0FPXSo8n5ZFoftD0S5bnN7nkzAOj3pl1nO+U9/aV94+sTafpiC9xkPC7cpRNxtBITBZRuXBowKqFRMxcmsek+/+8LFL0z/eOy2ZozKpj9uT81reZ7Ee/gVApaF22rooPrw2sjSnndG/qJOlKgwS3NzFaw==\"," + 
        "                \"SigningCertUrl\": \"https://sns.eu-west-2.amazonaws.com/SimpleNotificationService-6aad65c2f9911b05cd53efda11f913f9.pem\"," + 
        "                \"UnsubscribeUrl\": \"https://sns.eu-west-2.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-2:018330602464:RegisterCsvFromS3DeadLetterTopic:1e39cd2a-3f7e-4534-b1f0-6490061dd633\"," + 
        "                \"MessageAttributes\": {" + 
        "                    \"RequestID\": {" + 
        "                        \"Type\": \"String\"," + 
        "                        \"Value\": \"7809dbde-10aa-4014-93ce-48c8954055b5\"\n" + 
        "                    }," + 
        "                    \"ErrorCode\": {" + 
        "                        \"Type\": \"String\"," + 
        "                        \"Value\": \"200\"" + 
        "                    }," + 
        "                    \"ErrorMessage\": {" + 
        "                        \"Type\": \"String\"," + 
        "                        \"Value\": \"2019-09-05T02:59:20.251Z 7809dbde-10aa-4014-93ce-48c8954055b5 Task timed out after 30.02 seconds\"" + 
        "                    }" + 
        "                }" + 
        "            }" + 
        "        }" + 
        "    ]" + 
        "}";
        
    doNothing().when(mockedRegisterJobSupervisor).markFailureWithValidationErrors(jobIdCaptured.capture().intValue(),
                                                                                  valueCapture.capture(),
                                                                                  validationErrors.capture());
    EventProcessor eventProcessor = new EventProcessor(mockedRegisterJobSupervisor);
    eventProcessor.process(eventString);
    assertEquals(new Integer(48), jobIdCaptured.getValue());
    assertEquals(RegisterJobStatus.ABORTED, valueCapture.getValue());
    assertEquals(EventProcessor.LAMBDA_TIMEOUT_EXCEPTION, validationErrors.getValue().get(0).getDetail());
  }
  
  @Test
  public void givenValidCloudWatchLogEventJobWillBeCancelled() throws Exception {
    String eventString = "{" + 
        "\"awslogs\": {" + 
        "        \"data\": \"H4sIAAAAAAAAAM2TW2/aMBTHv0pk7ZEE32PnjW0EMdEiAeoLoMkkBmXLhTkJbYX47jvJxspaVXtdFMnH/3Px8S8nZ1TYujYHu3o+WhShz6PV6OvdeLkcTcZogKrH0jqQMVGMYYkplxzkvDpMXNUewTM0j/UwN8UuNcOFPWR1Y92n+hS7qliyuC2TJqvKXynLxllTQA7FRA8xvGK4/jAbrcbL1ZboXSj2VDORSm4F0VjRkBDLLKYk5AmUqNtdnbjs2FWMsxwOqlG0Rt/Mj9YvssRVtXWnLLG1P2+b+f7OFpV7Hj8l9ibDJ7N4MV/M2MOEPCi07Rsbn2zZdLXOKEuhP8a10JoTJghVlApNMSehliEnWComsQiZlpJxAQ9WAkvKsMICemwy4NmYAtAQIUMpQq0UIXhw5dzBFN3lOwge5hFjEVYBhHje9D6eex7xfN/31t7LU5is3HptcAiSoAlM8IbzrOd/DY+82/s7VznvvYzzBrnfri/VbppuUMTVYINq9rFNvtsG9rcb8Oyz3N6bwvaelw14kso5m5uOdV/ojQIxph+H3nk1L5tuPOxT40CxaZzZPIVvcUbX4ekOAGrvXAFyyV9MQaCdcEMWJAZShxdMDiaBVcAKpMGSYL3CDWoI6j+ZQ5yCuAjWa7/T8tg2oPwPbKGtPz8AtPR6LtDlsr38BFns9Q0DBAAA\"" + 
        "  }" + 
        "}";
    doNothing().when(mockedRegisterJobSupervisor).markFailureWithValidationErrors(jobIdCaptured.capture().intValue(),
                                                                                  valueCapture.capture(),
                                                                                  validationErrors.capture());
    EventProcessor eventProcessor = new EventProcessor(mockedRegisterJobSupervisor);
    eventProcessor.process(eventString);
    assertEquals(new Integer(48), jobIdCaptured.getValue());
    assertEquals(RegisterJobStatus.ABORTED, valueCapture.getValue());
    assertEquals(EventProcessor.LAMBDA_OUTOFMEMORY_EXCEPTION, validationErrors.getValue().get(0).getDetail());
  }
  
  @Test
  public void givenInvalidEventWillThrowException() throws Exception {
    String invalidEventString = "Invalid Event";
    EventProcessor eventProcessor = new EventProcessor(mockedRegisterJobSupervisor);
    assertThrows(Exception.class, () -> eventProcessor.process(invalidEventString));
  }
}
