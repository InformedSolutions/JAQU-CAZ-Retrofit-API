package uk.gov.caz.testutils;

import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_RETROFIT_REGISTER_JOB_TRIGGER;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_ERRORS;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_RUNNING_REGISTER_JOB_STATUS;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_STARTING_REGISTER_JOB_STATUS;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

public class RegisterJobAssert extends AbstractAssert<RegisterJobAssert, RegisterJob> {

  public RegisterJobAssert(RegisterJob actual) {
    super(actual, RegisterJobAssert.class);
  }

  public static RegisterJobAssert assertThat(RegisterJob actual) {
    return new RegisterJobAssert(actual);
  }

  public RegisterJobAssert hasId(int expectedId) {
    Assertions.assertThat(actual.getId()).isEqualTo(expectedId);
    return this;
  }

  public RegisterJobAssert wasTriggeredBy(RegisterJobTrigger expectedTrigger) {
    Assertions.assertThat(actual.getTrigger()).isEqualByComparingTo(expectedTrigger);
    return this;
  }

  public RegisterJobAssert wasUploadedBy(UUID expectedUploader) {
    Assertions.assertThat(actual.getUploaderId()).isEqualByComparingTo(expectedUploader);
    return this;
  }

  public RegisterJobAssert hasName(String expectedName) {
    Assertions.assertThat(actual.getJobName().getValue()).isEqualTo(expectedName);
    return this;
  }

  public RegisterJobAssert isInStatus(RegisterJobStatus expectedStatus) {
    Assertions.assertThat(actual.getStatus()).isEqualByComparingTo(expectedStatus);
    return this;
  }

  public RegisterJobAssert hasErrors(List<RegisterJobError> expectedErrors) {
    Assertions.assertThat(actual.getErrors()).isEqualTo(expectedErrors);
    return this;
  }

  public RegisterJobAssert hasCorrelationId(String expectedCorrelationId) {
    Assertions.assertThat(actual.getCorrelationId()).isEqualTo(expectedCorrelationId);
    return this;
  }

  public RegisterJobAssert matchesAttributesOfTypicalRunningRegisterJob() {
    hasId(S3_REGISTER_JOB_ID);
    hasName(S3_REGISTER_JOB_NAME);
    wasTriggeredBy(S3_RETROFIT_REGISTER_JOB_TRIGGER);
    wasUploadedBy(TYPICAL_REGISTER_JOB_UPLOADER_ID);
    isInStatus(TYPICAL_RUNNING_REGISTER_JOB_STATUS);
    hasErrors(TYPICAL_REGISTER_JOB_ERRORS);
    hasCorrelationId(TYPICAL_CORRELATION_ID);
    return this;
  }

  public RegisterJobAssert matchesAttributesOfTypicalStartingRegisterJob() {
    hasId(S3_REGISTER_JOB_ID);
    hasName(S3_REGISTER_JOB_NAME);
    wasTriggeredBy(S3_RETROFIT_REGISTER_JOB_TRIGGER);
    wasUploadedBy(TYPICAL_REGISTER_JOB_UPLOADER_ID);
    isInStatus(TYPICAL_STARTING_REGISTER_JOB_STATUS);
    hasErrors(Collections.emptyList());
    hasCorrelationId(TYPICAL_CORRELATION_ID);
    return this;
  }
}
