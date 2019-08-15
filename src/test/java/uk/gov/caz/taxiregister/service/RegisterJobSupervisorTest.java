package uk.gov.caz.taxiregister.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.MODIFIED_REGISTER_JOB_ERRORS;
import static uk.gov.caz.testutils.TestObjects.MODIFIED_REGISTER_JOB_VALIDATION_ERRORS;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_RETROFIT_REGISTER_JOB_TRIGGER;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;

@ExtendWith(MockitoExtension.class)
class RegisterJobSupervisorTest {

  private static final String CSV_FILE = "csv-file";

  @Mock
  private RegisterJobRepository mockedRegisterJobRepository;

  @Mock
  private RegisterJobNameGenerator mockedRegisterJobNameGenerator;

  @InjectMocks
  private RegisterJobSupervisor registerJobSupervisor;

  @Captor
  private ArgumentCaptor<RegisterJob> registerJobArgumentCaptor;

  @Test
  public void testStartingAndSupervisingNewRegisterJob() {
    // given
    prepareMocksForNameGenerationAndRegisterJobInsertion();
    AtomicBoolean capturedJobStarted = new AtomicBoolean(false);
    AtomicInteger capturedRegisterJobId = new AtomicInteger();

    // when
    StartParams startParams = prepareStartParams(capturedJobStarted, capturedRegisterJobId);
    RegisterJobName registerJobName = registerJobSupervisor.start(startParams);

    // then
    assertThat(registerJobName.getValue()).isEqualTo(S3_REGISTER_JOB_NAME);
    assertThat(capturedRegisterJobId.get()).isEqualTo(S3_REGISTER_JOB_ID);
    assertThat(capturedJobStarted).isTrue();

    RegisterJob capturedRegisterJob =
        verifyThatNewRegisterJobWasInsertedIntoRepositoryAndCaptureIt();
    assertThat(capturedRegisterJob)
        .matchesAttributesOfTypicalStartingRegisterJob();
  }

  @Test
  public void testUpdateStatus() {
    // when
    registerJobSupervisor
        .updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);

    // then
    verify(mockedRegisterJobRepository)
        .updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);
  }

  @Test
  public void testUpdateErrors() {
    // when
    registerJobSupervisor
        .addErrors(S3_REGISTER_JOB_ID, MODIFIED_REGISTER_JOB_VALIDATION_ERRORS);

    // then
    verify(mockedRegisterJobRepository)
        .updateErrors(S3_REGISTER_JOB_ID, MODIFIED_REGISTER_JOB_ERRORS);
  }

  @Test
  public void isRunningOrStartingJob() {
    // when
    registerJobSupervisor.hasActiveJobs(TYPICAL_REGISTER_JOB_UPLOADER_ID);

    // then
    verify(mockedRegisterJobRepository)
        .countActiveJobsByUploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID);
  }

  @Test
  public void testFindByName() {
    // when
    registerJobSupervisor.findJobWithName(new RegisterJobName(S3_REGISTER_JOB_NAME));

    // then
    verify(mockedRegisterJobRepository).findByName(S3_REGISTER_JOB_NAME);
  }

  @Test
  public void testMarkFailureWithValidationErrors() {
    // given
    RegisterJobStatus jobStatus = RegisterJobStatus.STARTUP_FAILURE_NO_ACCESS_TO_S3;
    ValidationError internalError = ValidationError.internal();
    ValidationError unknownError = ValidationError.unknown();
    List<ValidationError> validationErrors = Arrays.asList(internalError, unknownError);
    List<RegisterJobError> errors = Stream.of(internalError, unknownError)
        .map(RegisterJobError::from).collect(Collectors.toList());

    // when
    registerJobSupervisor.markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        jobStatus, validationErrors);

    // then
    verify(mockedRegisterJobRepository).updateStatus(S3_REGISTER_JOB_ID, jobStatus);
    verify(mockedRegisterJobRepository).updateErrors(S3_REGISTER_JOB_ID, errors);
  }

  @Nested
  class activeJobs {

    @Test
    public void testHasActiveJobsIsFalseWhenNoJobsArePresent() {
      // given
      given(
          mockedRegisterJobRepository.countActiveJobsByUploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID))
          .willReturn(null);

      // when
      boolean areThereActiveJobs = registerJobSupervisor
          .hasActiveJobs(TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      assertThat(areThereActiveJobs).isFalse();
    }

    @Test
    public void testHasActiveJobsIsFalseWhenAllJobsAreFinished() {
      // given
      given(
          mockedRegisterJobRepository.countActiveJobsByUploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID))
          .willReturn(0);

      // when
      boolean areThereActiveJobs = registerJobSupervisor
          .hasActiveJobs(TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      assertThat(areThereActiveJobs).isFalse();
    }

    @Test
    public void testHasActiveJobsIsTrueWhenAtLeastOneJobHasNotFinished() {
      // given
      given(
          mockedRegisterJobRepository.countActiveJobsByUploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID))
          .willReturn(1);

      // when
      boolean areThereActiveJobs = registerJobSupervisor
          .hasActiveJobs(TYPICAL_REGISTER_JOB_UPLOADER_ID);

      // then
      assertThat(areThereActiveJobs).isTrue();
    }
  }

  private StartParams prepareStartParams(AtomicBoolean capturedJobStarted,
      AtomicInteger capturedRegisterJobId) {
    return StartParams.builder()
        .registerJobTrigger(S3_RETROFIT_REGISTER_JOB_TRIGGER)
        .registerJobNameSuffix(CSV_FILE)
        .correlationId(TYPICAL_CORRELATION_ID)
        .uploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID)
        .registerJobInvoker(registerJobId -> {
          capturedJobStarted.set(true);
          capturedRegisterJobId.set(registerJobId);
        })
        .build();
  }

  private void prepareMocksForNameGenerationAndRegisterJobInsertion() {
    given(mockedRegisterJobNameGenerator.generate(CSV_FILE, S3_RETROFIT_REGISTER_JOB_TRIGGER))
        .willReturn(new RegisterJobName(S3_REGISTER_JOB_NAME));
    given(mockedRegisterJobRepository.insert(any(RegisterJob.class)))
        .willReturn(S3_REGISTER_JOB_ID);
  }

  private RegisterJob verifyThatNewRegisterJobWasInsertedIntoRepositoryAndCaptureIt() {
    verify(mockedRegisterJobRepository).insert(registerJobArgumentCaptor.capture());
    return registerJobArgumentCaptor.getValue();
  }
}
