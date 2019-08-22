package uk.gov.caz.retrofit.service;

import com.google.common.base.Strings;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobName;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobTrigger;

/**
 * Generates full Register Job name that can be used by polling and logging operations.
 */
@Component
public class RegisterJobNameGenerator {

  /**
   * Generate full Register Job name that can be used by polling and logging operations.
   *
   * @param suffix Register Job suffix that we want to have at the end of job name. Can be empty
   *     or null and if so it will be ignored.
   * @param registerJobTrigger Informs name generator what was the trigger so it can append
   *     proper part to Register Job Name.
   * @return Full Register Job name that can be used by polling and logging operations.
   */
  public RegisterJobName generate(String suffix, RegisterJobTrigger registerJobTrigger) {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_");
    LocalDateTime now = LocalDateTime.now();
    String jobName = dtf.format(now)
        + registerJobTrigger.name()
        + (Strings.isNullOrEmpty(suffix) ? "" : "_" + suffix);
    return new RegisterJobName(jobName);
  }
}
