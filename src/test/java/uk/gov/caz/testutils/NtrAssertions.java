package uk.gov.caz.testutils;

import org.assertj.core.api.Assertions;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;

public class NtrAssertions extends Assertions {

  public static RegisterJobAssert assertThat(RegisterJob actual) {
    return new RegisterJobAssert(actual);
  }

  public static RegisterJobSupervisorStartParamsAssert assertThat(StartParams actual) {
    return new RegisterJobSupervisorStartParamsAssert(actual);
  }
}
