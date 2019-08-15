package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class RegisterFromRestApiCommandTest {

  private RegisterFromRestApiCommand registerFromRestApiCommand = new RegisterFromRestApiCommand(
      Collections.emptyList(), TYPICAL_REGISTER_JOB_UPLOADER_ID, S3_REGISTER_JOB_ID,
      new RegisterServicesContext(null, null, null, null, null),
      TYPICAL_CORRELATION_ID
  );

  @Test
  public void shouldNotReturnAnyErrors() {
    assertThat(registerFromRestApiCommand.getLicencesParseValidationErrors()).isEmpty();
  }
}