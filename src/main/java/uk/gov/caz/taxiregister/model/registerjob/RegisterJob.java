package uk.gov.caz.taxiregister.model.registerjob;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Singular;

@Data
@Builder(toBuilder = true)
public class RegisterJob {

  int id;

  @NonNull
  RegisterJobName jobName;

  @NonNull
  UUID uploaderId;

  @NonNull
  RegisterJobTrigger trigger;

  @NonNull
  RegisterJobStatus status;

  @Singular
  @NonNull
  List<RegisterJobError> errors;

  @NonNull
  String correlationId;
}
