package uk.gov.caz.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;

public class TestObjects {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public static final int S3_REGISTER_JOB_ID = 123;
  public static final int API_CALL_REGISTER_JOB_ID = 456;
  public static final String S3_REGISTER_JOB_NAME = "20190809_154821_CSV_FROM_S3_FILENAME";
  public static final String NOT_EXISTING_REGISTER_JOB_NAME = "NOT_EXISTING_JOB";
  public static final List<RegisterJobError> TYPICAL_REGISTER_JOB_ERRORS = ImmutableList.of(
      RegisterJobError.withDetailOnly("error 1"),
      RegisterJobError.withDetailOnly("error 2")
  );
  public static final String TYPICAL_REGISTER_JOB_ERRORS_JOINED = convertToString(
      TYPICAL_REGISTER_JOB_ERRORS);

  public static final List<RegisterJobError> MODIFIED_REGISTER_JOB_ERRORS = ImmutableList.of(
      RegisterJobError.withDetailOnly("new error 1"),
      RegisterJobError.withDetailOnly("new error 2"),
      RegisterJobError.withDetailOnly("new error 3")
  );

  public static final List<ValidationError> MODIFIED_REGISTER_JOB_VALIDATION_ERRORS = MODIFIED_REGISTER_JOB_ERRORS
      .stream()
      .map(ValidationError::from)
      .collect(Collectors.toList());
  public static final RegisterJobTrigger S3_RETROFIT_REGISTER_JOB_TRIGGER = RegisterJobTrigger.RETROFIT_CSV_FROM_S3;
  public static final RegisterJobStatus TYPICAL_RUNNING_REGISTER_JOB_STATUS = RegisterJobStatus.RUNNING;
  public static final RegisterJobStatus TYPICAL_STARTING_REGISTER_JOB_STATUS = RegisterJobStatus.STARTING;
  public static final UUID TYPICAL_REGISTER_JOB_UPLOADER_ID = UUID
      .fromString("11111111-2222-3333-4444-555555555555");
  public static final String TYPICAL_CORRELATION_ID = "CorrelationId";
  public static String VALID_API_KEY = TYPICAL_REGISTER_JOB_UPLOADER_ID.toString();

  public static final RegisterJob S3_RUNNING_REGISTER_JOB = RegisterJob.builder()
      .id(S3_REGISTER_JOB_ID)
      .uploaderId(TYPICAL_REGISTER_JOB_UPLOADER_ID)
      .jobName(new RegisterJobName(S3_REGISTER_JOB_NAME))
      .status(TYPICAL_RUNNING_REGISTER_JOB_STATUS)
      .trigger(S3_RETROFIT_REGISTER_JOB_TRIGGER)
      .errors(TYPICAL_REGISTER_JOB_ERRORS)
      .correlationId(TYPICAL_CORRELATION_ID)
      .build();

  @SneakyThrows
  private static String convertToString(List<RegisterJobError> typicalRegisterJobErrors) {
    return objectMapper.writeValueAsString(typicalRegisterJobErrors);
  }
}
