package uk.gov.caz.retrofit.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobStatus;

class RegisterJobStatusDtoTest {

  static Stream<Arguments> statusCodeAndCustomCodeProvider() {
    return Stream.of(
        Arguments.arguments(RegisterJobStatus.STARTING, RegisterJobStatusDto.RUNNING),
        Arguments.arguments(RegisterJobStatus.RUNNING, RegisterJobStatusDto.RUNNING),
        Arguments.arguments(RegisterJobStatus.FINISHED_SUCCESS, RegisterJobStatusDto.SUCCESS),
        Arguments.arguments(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
            RegisterJobStatusDto.FAILURE),
        Arguments.arguments(RegisterJobStatus.STARTUP_FAILURE_NO_ACCESS_TO_S3,
            RegisterJobStatusDto.FAILURE),
        Arguments.arguments(RegisterJobStatus.STARTUP_FAILURE_NO_UPLOADER_ID,
            RegisterJobStatusDto.FAILURE),
        Arguments.arguments(RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID,
            RegisterJobStatusDto.FAILURE),
        Arguments.arguments(RegisterJobStatus.STARTUP_FAILURE_NO_S3_BUCKET_OR_FILE,
            RegisterJobStatusDto.FAILURE),
        Arguments.arguments(RegisterJobStatus.STARTUP_FAILURE_TOO_LARGE_FILE,
            RegisterJobStatusDto.FAILURE),
        Arguments.arguments(RegisterJobStatus.UNKNOWN_FAILURE, RegisterJobStatusDto.FAILURE)
    );
  }

  @ParameterizedTest
  @MethodSource("statusCodeAndCustomCodeProvider")
  public void testConversionFromModelEnum(RegisterJobStatus modelEnumValue,
      RegisterJobStatusDto expectedDtoValue) {
    assertThat(RegisterJobStatusDto.from(modelEnumValue)).isEqualByComparingTo(expectedDtoValue);
  }
}