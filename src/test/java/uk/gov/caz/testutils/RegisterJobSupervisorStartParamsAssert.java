package uk.gov.caz.testutils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import java.util.UUID;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.AsyncBackgroundJobStarter;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;

public class RegisterJobSupervisorStartParamsAssert extends
    AbstractAssert<RegisterJobSupervisorStartParamsAssert, StartParams> {

  public RegisterJobSupervisorStartParamsAssert(StartParams actual) {
    super(actual, RegisterJobSupervisorStartParamsAssert.class);
  }

  public static RegisterJobSupervisorStartParamsAssert assertThat(StartParams actual) {
    return new RegisterJobSupervisorStartParamsAssert(actual);
  }

  public RegisterJobSupervisorStartParamsAssert hasJobNameSuffix(String expectedJobNameSuffix) {
    Assertions.assertThat(actual.getRegisterJobNameSuffix()).isEqualTo(expectedJobNameSuffix);
    return this;
  }

  public RegisterJobSupervisorStartParamsAssert hasCorrelationId(String expectedCorrelationId) {
    Assertions.assertThat(actual.getCorrelationId()).isEqualTo(expectedCorrelationId);
    return this;
  }

  public RegisterJobSupervisorStartParamsAssert wasTriggeredBy(RegisterJobTrigger expectedTrigger) {
    Assertions.assertThat(actual.getRegisterJobTrigger()).isEqualByComparingTo(expectedTrigger);
    return this;
  }

  public RegisterJobSupervisorStartParamsAssert wasUploadedBy(UUID expectedUploader) {
    Assertions.assertThat(actual.getUploaderId()).isEqualByComparingTo(expectedUploader);
    return this;
  }

  public RegisterJobSupervisorStartParamsAssert invokedJob(
      AsyncBackgroundJobStarter mockedAsyncBackgroundJobStarter, String expectedS3Bucket,
      String expectedFilename) {
    RegisterJobSupervisor.RegisterJobInvoker invoker = actual.getRegisterJobInvoker();
    Assertions.assertThat(invoker).isNotNull();
    invoker.invoke(S3_REGISTER_JOB_ID);
    verify(mockedAsyncBackgroundJobStarter)
        .fireAndForgetRegisterCsvFromS3Job(S3_REGISTER_JOB_ID, expectedS3Bucket,
            expectedFilename, TYPICAL_CORRELATION_ID);
    verifyNoMoreInteractions(mockedAsyncBackgroundJobStarter);
    return this;
  }
}
