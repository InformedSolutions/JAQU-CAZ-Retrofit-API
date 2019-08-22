package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static uk.gov.caz.testutils.TestObjects.MODIFIED_REGISTER_JOB_VALIDATION_ERRORS;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import java.util.List;
import java.util.UUID;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.CsvFindResult;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.repository.RetrofittedVehicleDtoCsvRepository;

@ExtendWith(MockitoExtension.class)
class RegisterFromCsvCommandTest {

  @Mock
  private RetrofittedVehicleDtoCsvRepository csvRepository;

  private RegisterFromCsvCommand registerFromCsvCommand;

  @BeforeEach
  public void setup() {
    registerFromCsvCommand = new RegisterFromCsvCommand(
        new RegisterServicesContext(null, null, null, null, csvRepository), S3_REGISTER_JOB_ID,
        TYPICAL_CORRELATION_ID, "bucket", "filename");
  }

  @Test
  public void shouldFetchCsvResultAndReturnResults() {
    //given
    UUID uploaderId = UUID.randomUUID();
    List<RetrofittedVehicleDto> licences = Lists
        .list(RetrofittedVehicleDto.builder().vrn("abc").build());
    List<ValidationError> validationErrors = MODIFIED_REGISTER_JOB_VALIDATION_ERRORS;
    CsvFindResult csvFindResult = new CsvFindResult(uploaderId, licences, validationErrors);

    given(csvRepository.findAll(any(), any())).willReturn(csvFindResult);

    //when
    registerFromCsvCommand.beforeExecute();

    //then
    assertThat(registerFromCsvCommand.getLicencesParseValidationErrors())
        .isEqualTo(MODIFIED_REGISTER_JOB_VALIDATION_ERRORS);
    assertThat(registerFromCsvCommand.getLicencesToRegister()).isEqualTo(licences);
  }

  @Test
  public void shouldThrowExceptionIfCsvFindResultIsNull() {
    //given
    given(csvRepository.findAll(any(), any())).willReturn(null);

    //when
    registerFromCsvCommand.beforeExecute();

    //then
    assertThrows(IllegalStateException.class,
        () -> registerFromCsvCommand.getLicencesParseValidationErrors());
    assertThrows(IllegalStateException.class, () -> registerFromCsvCommand.getLicencesToRegister());
  }
}