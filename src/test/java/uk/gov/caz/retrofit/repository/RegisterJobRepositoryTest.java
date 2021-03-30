package uk.gov.caz.retrofit.repository;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.caz.retrofit.repository.RegisterJobRepository.COL_CORRELATION_ID;
import static uk.gov.caz.retrofit.repository.RegisterJobRepository.COL_ERRORS;
import static uk.gov.caz.retrofit.repository.RegisterJobRepository.COL_JOB_NAME;
import static uk.gov.caz.retrofit.repository.RegisterJobRepository.COL_REGISTER_JOB_ID;
import static uk.gov.caz.retrofit.repository.RegisterJobRepository.COL_STATUS;
import static uk.gov.caz.retrofit.repository.RegisterJobRepository.COL_TRIGGER;
import static uk.gov.caz.testutils.NtrAssertions.assertThat;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_NAME;
import static uk.gov.caz.testutils.TestObjects.S3_RETROFIT_REGISTER_JOB_TRIGGER;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_ERRORS_JOINED;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_RUNNING_REGISTER_JOB_STATUS;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.gov.caz.retrofit.model.registerjob.RegisterJob;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobError;
import uk.gov.caz.retrofit.repository.RegisterJobRepository.RegisterJobRowMapper;

class RegisterJobRepositoryTest {

  private static final int MAX_ERRORS_COUNT = 10;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private RegisterJobRepository registerJobRepository;
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  public void initialize() {
    jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    registerJobRepository = new RegisterJobRepository(jdbcTemplate, objectMapper, MAX_ERRORS_COUNT);
  }

  @Nested
  class UpdateErrors {

    @Test
    public void shouldThrowNullPointerExceptionWhenPassedListIsNull() {
      // given
      int registerJobId = 12;
      List<RegisterJobError> errors = null;

      // when
      Throwable throwable = catchThrowable(() ->
          registerJobRepository.updateErrors(registerJobId, errors));

      // then
      then(throwable).isInstanceOf(NullPointerException.class);
      verifyZeroInteractions(jdbcTemplate);
    }

    @Test
    public void shouldUpdateErrorsToNullWhenPassedListIsEmpty() {
      // given
      int registerJobId = 13;
      List<RegisterJobError> errors = Collections.emptyList();

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      verify(jdbcTemplate).update(anyString(), eq(null), eq(registerJobId));
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenParsingErrorFails() throws IOException {
      // given
      int registerJobId = 12;
      List<RegisterJobError> errors = mockInvalidFormatExceptionWhenConvertingToJson();

      // when
      Throwable throwable = catchThrowable(
          () -> registerJobRepository.updateErrors(registerJobId, errors));

      // then
      assertThat(throwable)
          .isInstanceOf(RuntimeException.class)
          .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void shouldSaveJsonListWithAllParameters() {
      // given
      int registerJobId = 12;
      String vrn = "123";
      String title = "Validation error";
      String detail = "some error";
      List<RegisterJobError> errors = Collections.singletonList(new RegisterJobError(vrn,
          title, detail));

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      String expected = new StringBuilder().append('[').append('{')
          .append(jsonField("vrn", vrn))
          .append(',')
          .append(jsonField("title", title))
          .append(',')
          .append(jsonField("detail", detail))
          .append('}').append(']')
          .toString();
      verify(jdbcTemplate).update(anyString(), eq(expected), eq(registerJobId));
    }

    @Test
    public void shouldTruncateErrorsListIfItIsTooBig() {
      // given
      mockRepositoryWithMaxOneErrorAllowed();
      int registerJobId = 12;
      List<RegisterJobError> errors = Arrays.asList(
          new RegisterJobError("123", "Validation error", "some error"),
          new RegisterJobError("124", "Validation error", "some error"));

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      String expected = new StringBuilder().append('[').append('{')
          .append(jsonField("vrn", "123"))
          .append(',')
          .append(jsonField("title", "Validation error"))
          .append(',')
          .append(jsonField("detail", "some error"))
          .append('}').append(']')
          .toString();
      verify(jdbcTemplate).update(anyString(), eq(expected), eq(registerJobId));
    }

    private void mockRepositoryWithMaxOneErrorAllowed() {
      registerJobRepository = new RegisterJobRepository(jdbcTemplate, objectMapper, 1);
    }

    @Test
    public void shouldSaveJsonListWithoutVrnWhenVrnIsNull() {
      // given
      int registerJobId = 12;
      String vrn = null;
      String title = "Validation error";
      String detail = "some error";
      List<RegisterJobError> errors = Collections.singletonList(new RegisterJobError(vrn,
          title, detail));

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      String expected = new StringBuilder().append('[').append('{')
          .append(jsonField("title", title))
          .append(',')
          .append(jsonField("detail", detail))
          .append('}').append(']')
          .toString();
      verify(jdbcTemplate).update(anyString(), eq(expected), eq(registerJobId));
    }

    @Test
    public void shouldSaveJsonListWithoutVrnWhenVrnIsEmpty() {
      // given
      int registerJobId = 12;
      String vrn = "";
      String title = "Validation error";
      String detail = "some error";
      List<RegisterJobError> errors = Collections.singletonList(new RegisterJobError(vrn,
          title, detail));

      // when
      registerJobRepository.updateErrors(registerJobId, errors);

      // then
      String expected = new StringBuilder().append('[').append('{')
          .append(jsonField("title", title))
          .append(',')
          .append(jsonField("detail", detail))
          .append('}').append(']')
          .toString();
      verify(jdbcTemplate).update(anyString(), eq(expected), eq(registerJobId));
    }

    private String jsonField(String key, String value) {
      return String.format("\"%s\":\"%s\"", key, value);
    }

    private List<RegisterJobError> mockInvalidFormatExceptionWhenConvertingToJson()
        throws JsonProcessingException {
      ObjectMapper om = mock(ObjectMapper.class);
      List<RegisterJobError> errors = Collections.singletonList(new RegisterJobError("123",
          "Validation error", "some error"));
      given(om.writeValueAsString(errors))
          .willThrow(new InvalidFormatException((JsonParser) null, null, null, null));
      registerJobRepository = new RegisterJobRepository(jdbcTemplate, om, MAX_ERRORS_COUNT);
      return errors;
    }
  }

  @Nested
  class RowMapper {

    private RegisterJobRowMapper rowMapper = new RegisterJobRowMapper(new ObjectMapper());

    @Test
    public void shouldMapResultSetToRegisterJobWithAnyValidValues() throws SQLException {
      // given
      ResultSet resultSet = mockResultSetWithAnyValidValues();

      // when
      RegisterJob registerJob = rowMapper.mapRow(resultSet, 0);

      // then
      assertThat(registerJob)
          .isNotNull()
          .matchesAttributesOfTypicalRunningRegisterJob();
    }

    @Test
    public void shouldMapResultSetToRegisterJobWithErrorsSetToNullWhenThereAreNoErrors()
        throws SQLException {
      // given
      ResultSet resultSet = mockResultSetWithErrorsEqualTo(null);

      // when
      RegisterJob registerJob = rowMapper.mapRow(resultSet, 0);

      // then
      assertThat(registerJob)
          .isNotNull()
          .hasErrors(Collections.emptyList());
    }

    @Test
    public void shouldThrowRuntimeExceptionWhenParsingErrorFails()
        throws SQLException, IOException {
      // given
      mockInputOutputExceptionWhenParsingJson();
      ResultSet resultSet = mockResultSetWithErrorsEqualTo("");

      // when
      Throwable throwable = catchThrowable(() -> rowMapper.mapRow(resultSet, 0));

      // then
      assertThat(throwable)
          .isInstanceOf(RuntimeException.class)
          .hasCauseInstanceOf(IOException.class);
    }

    private ResultSet mockResultSetWithAnyValidValues() throws SQLException {
      return mockResultSetWithErrorsEqualTo(TYPICAL_REGISTER_JOB_ERRORS_JOINED);
    }

    private ResultSet mockResultSetWithErrorsEqualTo(String errors) throws SQLException {
      ResultSet resultSet = mock(ResultSet.class);

      when(resultSet.getInt(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case COL_REGISTER_JOB_ID:
            return S3_REGISTER_JOB_ID;
        }
        throw new RuntimeException("Value not stubbed!");
      });

      when(resultSet.getObject(anyString(), (Class<?>) any(Class.class)))
          .thenAnswer(answer -> TYPICAL_REGISTER_JOB_UPLOADER_ID);

      when(resultSet.getString(anyString())).thenAnswer(answer -> {
        String argument = answer.getArgument(0);
        switch (argument) {
          case COL_TRIGGER:
            return S3_RETROFIT_REGISTER_JOB_TRIGGER.name();
          case COL_JOB_NAME:
            return S3_REGISTER_JOB_NAME;
          case COL_STATUS:
            return TYPICAL_RUNNING_REGISTER_JOB_STATUS.name();
          case COL_ERRORS:
            return errors;
          case COL_CORRELATION_ID:
            return TYPICAL_CORRELATION_ID;
        }
        throw new RuntimeException("Value not stubbed!");
      });
      return resultSet;
    }
  }

  private void mockInputOutputExceptionWhenParsingJson() throws IOException {
    ObjectMapper om = mock(ObjectMapper.class);
    given(om.readValue(anyString(), any(TypeReference.class))).willThrow(new RuntimeException());
    registerJobRepository = new RegisterJobRepository(jdbcTemplate, objectMapper, MAX_ERRORS_COUNT);
  }
}
