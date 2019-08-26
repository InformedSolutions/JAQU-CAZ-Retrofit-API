package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static uk.gov.caz.testutils.TestObjects.MODIFIED_REGISTER_JOB_VALIDATION_ERRORS;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.BDDAssertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ConversionResults;
import uk.gov.caz.retrofit.model.CsvFindResult;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobStatus;
import uk.gov.caz.retrofit.repository.RetrofittedVehicleDtoCsvRepository;
import uk.gov.caz.testutils.TestObjects;

@ExtendWith(MockitoExtension.class)
class RegisterFromCsvCommandTest {

  private static final int ANY_MAX_ERRORS_COUNT = 10;

  @Mock
  private RetrofittedVehicleDtoCsvRepository csvRepository;

  @Mock
  private RegisterService registerService;

  @Mock
  private RegisterJobSupervisor jobSupervisor;

  @Mock
  private RegisterFromCsvExceptionResolver exceptionResolver;

  @Mock
  private RetrofittedVehicleDtoToModelConverter converter;

  private RegisterFromCsvCommand registerFromCsvCommand;

  @BeforeEach
  public void setup() {
    RegisterServicesContext context = new RegisterServicesContext(registerService,
        exceptionResolver, jobSupervisor, converter, csvRepository, ANY_MAX_ERRORS_COUNT);
    registerFromCsvCommand = new RegisterFromCsvCommand(context, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID, "bucket", "filename");
  }

  @Test
  public void shouldFetchCsvResultAndReturnResults() {
    //given
    UUID uploaderId = UUID.randomUUID();
    List<RetrofittedVehicleDto> vehicles = Lists
        .list(RetrofittedVehicleDto.builder().vrn("abc").build());
    List<ValidationError> validationErrors = MODIFIED_REGISTER_JOB_VALIDATION_ERRORS;
    CsvFindResult csvFindResult = new CsvFindResult(uploaderId, vehicles, validationErrors);

    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);

    //when
    registerFromCsvCommand.beforeExecute();

    //then
    assertThat(registerFromCsvCommand.getParseValidationErrors())
        .isEqualTo(MODIFIED_REGISTER_JOB_VALIDATION_ERRORS);
    assertThat(registerFromCsvCommand.getVehiclesToRegister()).isEqualTo(vehicles);
  }

  @Test
  public void shouldThrowExceptionIfCsvFindResultIsNull() {
    //given
    given(csvRepository.findAll(any(), any())).willReturn(null);

    //when
    registerFromCsvCommand.beforeExecute();

    //then
    assertThrows(IllegalStateException.class,
        () -> registerFromCsvCommand.getParseValidationErrors());
    assertThrows(IllegalStateException.class, () -> registerFromCsvCommand.getVehiclesToRegister());
  }

  @Test
  public void shouldMarkJobFailedWhenExceptionOccursDuringExecution() {
    // given
    RuntimeException exception = new RuntimeException();
    given(csvRepository.findAll(any(), any())).willThrow(exception);
    given(exceptionResolver.resolve(exception)).willReturn(RegisterResult.failure(Collections.emptyList()));

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(jobSupervisor).markFailureWithValidationErrors(anyInt(), any(), anyList());
  }

  @Test
  public void shouldMarkJobFailedWhenThereAreParseValidationErrors() {
    // given
    ValidationError parseValidationError = ValidationError.valueError("detail", 1);
    List<RetrofittedVehicleDto> vehicles = Collections.singletonList(RetrofittedVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(
        TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID, vehicles, Collections.singletonList(parseValidationError)
    );
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), anyInt())).willReturn(ConversionResults.from(Collections.emptyList()));

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(jobSupervisor).markFailureWithValidationErrors(anyInt(), eq(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS), anyList());
  }

  @Test
  public void shouldMarkJobFailedWhenRegistrationFails() {
    // given
    List<RetrofittedVehicleDto> vehicles = Collections.singletonList(RetrofittedVehicleDto.builder().vrn("abc").build());
    CsvFindResult csvFindResult = new CsvFindResult(TestObjects.TYPICAL_REGISTER_JOB_UPLOADER_ID, vehicles, Collections.emptyList());
    ConversionResults conversionResults = ConversionResults.from(Collections.emptyList());
    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);
    given(converter.convert(eq(vehicles), anyInt())).willReturn(conversionResults);
    given(registerService.register(conversionResults.getRetrofittedVehicles())).willReturn(RegisterResult.failure(Collections.emptyList()));

    // when
    RegisterResult result = registerFromCsvCommand.execute();

    // then
    BDDAssertions.then(result.isSuccess()).isFalse();
    verify(jobSupervisor).markFailureWithValidationErrors(anyInt(), eq(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS), anyList());
  }
}