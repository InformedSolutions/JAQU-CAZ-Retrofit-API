package uk.gov.caz.taxiregister.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.taxiregister.controller.Constants.API_KEY_HEADER;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.taxiregister.controller.RegisterController.INVALID_UPLOADER_ID_ERROR_MESSAGE;
import static uk.gov.caz.taxiregister.controller.RegisterController.NULL_VEHICLE_DETAILS_ERROR_MESSAGE;
import static uk.gov.caz.testutils.RegisterJobSupervisorStartParamsAssert.assertThat;
import static uk.gov.caz.testutils.TestObjects.API_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.testutils.TestObjects.VALID_API_KEY;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.caz.taxiregister.DateHelper;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.dto.Vehicles;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.SourceAwareRegisterService;

@WebMvcTest(RegisterController.class)
class RegisterControllerTest {

  private static final RegisterJobName FAKE_REGISTER_JOB_NAME = new RegisterJobName(
      API_REGISTER_JOB_NAME);
  private static final RegisterJob SUCCESS_REGISTER_JOB = RegisterJob.builder()
      .jobName(FAKE_REGISTER_JOB_NAME)
      .trigger(RegisterJobTrigger.API_CALL)
      .status(RegisterJobStatus.FINISHED_SUCCESS)
      .correlationId(TYPICAL_CORRELATION_ID)
      .uploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID)
      .build();

  @Value("${api.max-licences-count}")
  private int maxLicencesCount;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private RegisterJobSupervisor registerJobSupervisor;

  @MockBean
  private SourceAwareRegisterService registerService;

  @Autowired
  private MockMvc mockMvc;

  @Captor
  private ArgumentCaptor<StartParams> startParamsArgumentCaptor;

  @BeforeEach
  public void beforeEach() {
    Mockito.reset(registerJobSupervisor);
  }

  @Test
  public void shouldReturnValidEntity() throws Exception {
    // given
    String validVrm = "1289J";
    String payload = buildPayloadWith(validVrm);
    mockValidationSuccess();

    // when
    ResultActions callResult = performCallWith(payload, VALID_API_KEY);

    // then
    callResult.andExpect(status().isCreated());
    StartParams startParams = verifyThatSupervisorStartedJobAndCaptureItsParams();
    assertThat(startParams)
        .wasTriggeredBy(RegisterJobTrigger.API_CALL)
        .hasCorrelationId(TYPICAL_CORRELATION_ID)
        .hasJobNameSuffix("")
        .wasUploadedBy(TYPICAL_REGISTER_JOB_UPLOADER_ID)
        .invokedJob(registerService, Collections.singletonList(
            createVehicleWithWheelchairAccessibleVehicle(validVrm)));
  }

  @Test
  public void shouldReturnValidEntityWhenVehicleIsWithoutWheelchairAccessibleVehicle()
      throws Exception {
    String validVrm = "PC00SNK";
    String payload = buildPayloadWithoutWheelchairAccessibleVehicle(validVrm);
    mockValidationSuccess();

    performCallWith(payload, VALID_API_KEY).andExpect(status().isCreated());
  }

  @Test
  public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
    // given
    String validVrm = "1289J";
    String payload = buildPayloadWith(validVrm);
    mockValidationSuccess();

    // when
    performCallWithPayloadAndApiKeyButWithoutCorrelationHeader(payload, VALID_API_KEY)
        .andExpect(status().isBadRequest())
        .andExpect(content().string(
            "Missing request header 'X-Correlation-ID' for method parameter of type String"));
  }

  @Test
  public void missingApiKeyShouldResultIn400AndValidMessage() throws Exception {
    // given
    String validVrm = "1289J";
    String payload = buildPayloadWith(validVrm);
    mockValidationSuccess();

    // when
    performCallWithPayloadAndApiKeyButWithoutApiKeyHeader(payload)
        .andExpect(status().isBadRequest())
        .andExpect(content().string(
            "Missing request header 'x-api-key' for method parameter of type String"));
  }

  @Test
  public void shouldReturnNotAcceptableWhenJobIsRunningOrStarting() throws Exception {
    String validVrm = "1289J";
    String payload = buildPayloadWith(validVrm);
    mockAlreadyRunningOrStartingJob();

    performCallWith(payload, VALID_API_KEY).andExpect(status().isNotAcceptable());
  }

  @Test
  public void shouldReturn500ServerErrorIfRegisterJobSupervisorCantFindItsJustFinishedJob()
      throws Exception {
    String validVrm = "1289J";
    String payload = buildPayloadWith(validVrm);
    mockValidationSuccessAndSupervisorFindJobToReturn(Optional.empty());

    performCallWith(payload, VALID_API_KEY).andExpect(status().is5xxServerError());
  }

  private void mockAlreadyRunningOrStartingJob() {
    given(registerJobSupervisor.hasActiveJobs(any())).willReturn(true);
  }

  @Nested
  class Validation {

    @Test
    public void shouldReturn400BadRequestStatusCodeWhenValidationFails() throws Exception {
      String vrm = "A99A99A";
      String payload = buildPayloadWith(vrm);
      mockValidationError();

      performCallWith(payload, VALID_API_KEY).andExpect(status().isBadRequest());
    }

    @Test
    public void shouldNotAcceptEmptyInput() throws Exception {
      String payload = "";

      performCallWith(payload, VALID_API_KEY)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrm").value(""))
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"));
    }

    @Test
    public void shouldNotAcceptEmptyJson() throws Exception {
      String payload = "{}";

      performCallWith(payload, VALID_API_KEY)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrm").value(""))
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(jsonPath("$.errors[0].detail").value(NULL_VEHICLE_DETAILS_ERROR_MESSAGE));
    }

    @Test
    public void shouldNotAcceptMalformedJson() throws Exception {
      String payload = "{,}";

      performCallWith(payload, VALID_API_KEY)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrm").value(""))
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"));
    }

    @Test
    public void shouldReturnErrorResponseUponUnhandledException() throws Exception {
      String validVrm = "1289J";
      String payload = buildPayloadWith(validVrm);
      mockValidationErrorWithException(new NullPointerException());

      performCallWith(payload, VALID_API_KEY)
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.INTERNAL_SERVER_ERROR.value()))
          .andExpect(jsonPath("$.errors[0].vrm").value(""))
          .andExpect(jsonPath("$.errors[0].title").value("Unknown error"));
    }

    @Test
    public void shouldNotAcceptPayloadWithExceededNumberOfVehicles() throws Exception {
      int numberOfLicences = maxLicencesCount + 1;
      String payload = buildPayloadWithNumberOfLicences(numberOfLicences);

      performCallWith(payload, VALID_API_KEY)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrm").value(""))
          .andExpect(jsonPath("$.errors[0].title").value("Validation error"))
          .andExpect(jsonPath("$.errors[0].detail")
              .value(StringStartsWith.startsWith("Max number of vehicles exceeded")));
    }

    @Test
    public void shouldReturnErrorResponseWhenUploaderIdIsMalformed() throws Exception {
      String malformedUploaderId = "this-is-not-uuid";
      String payload = buildPayloadWith("1289J");

      performCallWith(payload, malformedUploaderId)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.errors[0].status").value(HttpStatus.BAD_REQUEST.value()))
          .andExpect(jsonPath("$.errors[0].vrm").value(""))
          .andExpect(jsonPath("$.errors[0].detail").value(INVALID_UPLOADER_ID_ERROR_MESSAGE));
    }
  }

  private ResultActions performCallWith(String payload, String xApiKey) throws Exception {
    return mockMvc.perform(post(RegisterController.PATH + "/taxiphvdatabase")
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
        .header(API_KEY_HEADER, xApiKey));
  }

  private ResultActions performCallWithPayloadAndApiKeyButWithoutCorrelationHeader(String payload,
      String xApiKey) throws Exception {
    return mockMvc.perform(post(RegisterController.PATH + "/taxiphvdatabase")
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(API_KEY_HEADER, xApiKey));
  }

  private ResultActions performCallWithPayloadAndApiKeyButWithoutApiKeyHeader(String payload)
      throws Exception {
    return mockMvc.perform(post(RegisterController.PATH + "/taxiphvdatabase")
        .content(payload)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID));
  }

  private void mockValidationErrorWithException(Exception e) {
    given(registerJobSupervisor.start(any())).willThrow(e);
  }

  private void mockValidationSuccess() {
    mockValidationSuccessAndSupervisorFindJobToReturn(Optional.of(SUCCESS_REGISTER_JOB));
  }

  private void mockValidationSuccessAndSupervisorFindJobToReturn(
      Optional<RegisterJob> registerJob) {
    given(registerJobSupervisor.hasActiveJobs(any())).willReturn(false);
    given(registerJobSupervisor.start(any()))
        .willReturn(FAKE_REGISTER_JOB_NAME);
    given(registerJobSupervisor.findJobWithName(FAKE_REGISTER_JOB_NAME))
        .willReturn(registerJob);
  }

  private void mockValidationError() {
    given(registerJobSupervisor.hasActiveJobs(any())).willReturn(false);
    given(registerJobSupervisor.start(any()))
        .willReturn(FAKE_REGISTER_JOB_NAME);

    RegisterJob registerJobWithErrors = SUCCESS_REGISTER_JOB.toBuilder()
        .status(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS)
        .errors(Collections.singletonList(RegisterJobError.withDetailOnly("Invalid VRM")))
        .build();

    given(registerJobSupervisor.findJobWithName(FAKE_REGISTER_JOB_NAME))
        .willReturn(Optional.of(registerJobWithErrors));
  }

  @SneakyThrows
  private String buildPayloadWithNumberOfLicences(int n) {
    List<VehicleDto> vehicles = IntStream.rangeClosed(1, n)
        .mapToObj(index -> createVehicleWithWheelchairAccessibleVehicle(String.valueOf(index)))
        .collect(Collectors.toList());
    return objectMapper.writeValueAsString(new Vehicles(vehicles));
  }

  @SneakyThrows
  private String buildPayloadWith(String vrm) {
    Vehicles vehicles = new Vehicles(Collections.singletonList(
        createVehicleWithWheelchairAccessibleVehicle(vrm)));
    return objectMapper.writeValueAsString(vehicles);
  }

  @SneakyThrows
  private String buildPayloadWithoutWheelchairAccessibleVehicle(String vrm) {
    Vehicles vehicles = new Vehicles(Collections.singletonList(
        createVehicleWithoutWheelchairAccessibleVehicle(vrm)));
    return objectMapper.writeValueAsString(vehicles);
  }

  private VehicleDto createVehicleWithWheelchairAccessibleVehicle(String vrm) {
    return VehicleDto.builder()
        .vrm(vrm)
        .start(DateHelper.today().toString())
        .end(DateHelper.yesterday().toString())
        .taxiOrPhv("PHV")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("la-plate-1")
        .wheelchairAccessibleVehicle(true)
        .build();
  }

  private VehicleDto createVehicleWithoutWheelchairAccessibleVehicle(String vrm) {
    return VehicleDto.builder()
        .vrm(vrm)
        .start(DateHelper.today().toString())
        .end(DateHelper.yesterday().toString())
        .taxiOrPhv("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("la-plate-1")
        .build();
  }

  private StartParams verifyThatSupervisorStartedJobAndCaptureItsParams() {
    verify(registerJobSupervisor).start(startParamsArgumentCaptor.capture());
    return startParamsArgumentCaptor.getValue();
  }
}