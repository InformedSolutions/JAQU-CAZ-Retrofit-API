package uk.gov.caz.taxiregister.controller;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.NOT_EXISTING_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_RUNNING_REGISTER_JOB;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_CSV_CONTENT_TYPE;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.taxiregister.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.AsyncBackgroundJobStarter;
import uk.gov.caz.taxiregister.service.CsvFileOnS3MetadataExtractor;
import uk.gov.caz.taxiregister.service.CsvFileOnS3MetadataExtractor.CsvMetadata;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.exception.FatalErrorWithCsvFileMetadataException;

@WebMvcTest(RegisterCsvFromS3Controller.class)
class RegisterCsvFromS3ControllerTest {

  private static final String S3_BUCKET = "s3Bucket";
  private static final String CSV_FILE = "fileName.csv";

  @MockBean
  private AsyncBackgroundJobStarter mockedAsyncBackgroundJobStarter;

  @MockBean
  private RegisterJobSupervisor mockedRegisterJobSupervisor;

  @MockBean
  private CsvFileOnS3MetadataExtractor mockedCsvFileOnS3MetadataExtractor;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Captor
  private ArgumentCaptor<RegisterJobSupervisor.StartParams> startParamsArgumentCaptor;

  @BeforeEach
  public void beforeEach() {
    Mockito.reset(mockedAsyncBackgroundJobStarter, mockedRegisterJobSupervisor);
  }

  @Test
  public void testRegisterJobStartWhenSuccessfullyObtainedMetadata() throws Exception {
    // given
    mockSupervisor();
    mockCsvFileOnS3MetadataExtractorForSuccess();
    mockSupervisorForNotFindingStartingOrRunningJob();

    // when
    postToStartRegisterJobAndCheckIfItStartedOk();

    // then
    StartParams startParams = verifyThatSupervisorStartedJobAndCaptureItsParams();
    assertThat(startParams)
        .wasTriggeredBy(RegisterJobTrigger.RETROFIT_CSV_FROM_S3)
        .hasCorrelationId(TYPICAL_CORRELATION_ID)
        .hasJobNameSuffix("fileName")
        .wasUploadedBy(TYPICAL_REGISTER_JOB_UPLOADER_ID)
        .invokedJob(mockedAsyncBackgroundJobStarter, S3_BUCKET, CSV_FILE);
  }

  @Test
  public void testRegisterJobStartWhenUnableToObtainMetadata() throws Exception {
    // given
    mockSupervisor();
    mockCsvFileOnS3MetadataExtractorForError();
    mockSupervisorForNotFindingStartingOrRunningJob();

    // when
    postToStartRegisterJobAndCheckIfItReturns500WithMessage(
        "Fatal Error with Metadata");
  }

  @Test
  public void testQueryForExistingRegisterJobStatus() throws Exception {
    mockSupervisorForFindingRegisterJob();
    mockSupervisorForNotFindingStartingOrRunningJob();

    mockMvc.perform(
        get(RegisterCsvFromS3Controller.PATH + "/{registerJobName}",
            S3_REGISTER_JOB_NAME)
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isOk())
        .andExpect(header().string(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID))
        .andExpect(jsonPath("$.status").value(RegisterJobStatus.RUNNING.toString()))
        .andExpect(jsonPath("$.errors[*]").value(hasItems("error 1", "error 2")));
  }

  @Test
  public void testQueryForNotExistingRegisterJobStatus() throws Exception {
    mockSupervisorForNotFindingRegisterJob();
    mockSupervisorForNotFindingStartingOrRunningJob();

    mockMvc.perform(
        get(RegisterCsvFromS3Controller.PATH + "/{registerJobName}",
            NOT_EXISTING_REGISTER_JOB_NAME)
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isNotFound())
        .andExpect(header().string(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID));
  }

  @Test
  public void testRegisterNotStartJobWhenJobIsAlreadyRunningOrStarting() throws Exception {
    // given
    mockForSuccess();

    // when
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(S3_BUCKET, CSV_FILE);

    // then
    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(objectMapper.writeValueAsString(cmd))
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isNotAcceptable());
  }

  @Test
  public void missingCorrelationIdShouldResultIn400AndValidMessage() throws Exception {
    // given
    mockForSuccess();

    // when
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(S3_BUCKET, CSV_FILE);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(objectMapper.writeValueAsString(cmd))
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(
            "Missing request header 'X-Correlation-ID' for method parameter of type String"));
  }

  private void mockSupervisor() {
    given(mockedRegisterJobSupervisor.start(Mockito.any(StartParams.class)))
        .willReturn(new RegisterJobName(S3_REGISTER_JOB_NAME));
  }

  private void mockCsvFileOnS3MetadataExtractorForSuccess() {
    given(mockedCsvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, CSV_FILE))
        .willReturn(new CsvMetadata(TYPICAL_REGISTER_JOB_UPLOADER_ID,
            TYPICAL_REGISTER_JOB_CSV_CONTENT_TYPE));
  }

  private void mockCsvFileOnS3MetadataExtractorForError() {
    given(mockedCsvFileOnS3MetadataExtractor.getRequiredMetadata(S3_BUCKET, CSV_FILE))
        .willThrow(new FatalErrorWithCsvFileMetadataException("Fatal Error with Metadata"));
  }

  private void mockSupervisorForFindingRegisterJob() {
    given(mockedRegisterJobSupervisor.findJobWithName(new RegisterJobName(S3_REGISTER_JOB_NAME)))
        .willReturn(Optional.of(S3_RUNNING_REGISTER_JOB));
  }

  private void mockSupervisorForNotFindingRegisterJob() {
    given(mockedRegisterJobSupervisor
        .findJobWithName(new RegisterJobName(NOT_EXISTING_REGISTER_JOB_NAME)))
        .willReturn(Optional.empty());
  }

  private void mockSupervisorForFindingStartingOrRunningJob() {
    given(mockedRegisterJobSupervisor.hasActiveJobs(TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .willReturn(true);
  }

  private void mockSupervisorForNotFindingStartingOrRunningJob() {
    given(mockedRegisterJobSupervisor.hasActiveJobs(TYPICAL_REGISTER_JOB_UPLOADER_ID))
        .willReturn(false);
  }

  private void mockForSuccess() {
    mockSupervisor();
    mockCsvFileOnS3MetadataExtractorForSuccess();
    mockSupervisorForFindingStartingOrRunningJob();
  }

  private void postToStartRegisterJobAndCheckIfItStartedOk() throws Exception {
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(S3_BUCKET, CSV_FILE);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(objectMapper.writeValueAsString(cmd))
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isCreated())
        .andExpect(header().string(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID))
        .andExpect(jsonPath("$.jobName")
            .value(S3_REGISTER_JOB_NAME));
  }

  private void postToStartRegisterJobAndCheckIfItReturns500WithMessage(String expectedMessage)
      throws Exception {
    StartRegisterCsvFromS3JobCommand cmd = new
        StartRegisterCsvFromS3JobCommand(S3_BUCKET, CSV_FILE);

    mockMvc.perform(
        post(RegisterCsvFromS3Controller.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .content(objectMapper.writeValueAsString(cmd))
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isInternalServerError())
        .andExpect(content().string(expectedMessage));
  }

  private StartParams verifyThatSupervisorStartedJobAndCaptureItsParams() {
    verify(mockedRegisterJobSupervisor).start(startParamsArgumentCaptor.capture());
    return startParamsArgumentCaptor.getValue();
  }
}
