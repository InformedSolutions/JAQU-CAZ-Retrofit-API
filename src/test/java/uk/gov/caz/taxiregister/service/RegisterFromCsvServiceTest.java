package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.caz.testutils.TestObjects.S3_REGISTER_JOB_ID;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import com.google.common.collect.Iterables;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.DateHelper;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.CsvFindResult;
import uk.gov.caz.taxiregister.model.LicenseDates;
import uk.gov.caz.taxiregister.model.LicensingAuthority;
import uk.gov.caz.taxiregister.model.TaxiPhvVehicleLicence;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.VehicleType;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;
import uk.gov.caz.taxiregister.service.exception.S3MetadataException;

@ExtendWith(MockitoExtension.class)
class RegisterFromCsvServiceTest {

  @Mock
  private RegisterService registerService;

  @Mock
  private RegisterJobSupervisor registerJobSupervisor;

  @Mock
  private TaxiPhvLicenceCsvRepository csvRepository;

  @Mock
  private RegisterFromCsvExceptionResolver exceptionResolver;

  @Mock
  private VehicleToLicenceConverter vehicleToLicenceConverter;

  @InjectMocks
  private RegisterServicesContext registerServicesContext;

  private SourceAwareRegisterService registerFromCsvService;

  @BeforeEach
  public void setup() {
     registerFromCsvService = new SourceAwareRegisterService(new RegisterCommandFactory(registerServicesContext));
  }

  @Test
  public void shouldGetDataFromS3AndDelegateToVehicleRegistrationService() {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    UUID uploaderId = UUID.randomUUID();
    List<VehicleDto> taxiPhvVehicleLicences = mockDataAtS3(bucket, filename, uploaderId);
    ConversionResults conversionResults = mockConversionResult(taxiPhvVehicleLicences);
    ValidationError validationError = ValidationError.s3Error("some error");
    RegisterResult registerResult = RegisterResult.failure(validationError);
    given(registerService.register(conversionResults.getLicences(), uploaderId))
        .willReturn(registerResult);

    // when
    RegisterResult actualRegisterResult = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    // then
    assertThat(actualRegisterResult).isEqualTo(registerResult);
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor).markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
        Collections.singletonList(validationError));
    verifyNoMoreInteractions(registerJobSupervisor);
  }

  @Test
  public void shouldProperlyHandleValidationError() {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    UUID uploaderId = UUID.randomUUID();
    List<VehicleDto> taxiPhvVehicleLicences = mockDataAtS3(bucket, filename, uploaderId);
    ConversionResults conversionResults = mockConversionResult(taxiPhvVehicleLicences);
    RegisterResult registerResult = RegisterResult.success();
    given(registerService.register(conversionResults.getLicences(), uploaderId))
        .willReturn(registerResult);

    // when
    RegisterResult actualRegisterResult = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    // then
    assertThat(actualRegisterResult).isEqualTo(registerResult);
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor)
        .updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.FINISHED_SUCCESS);
    verifyNoMoreInteractions(registerJobSupervisor);
  }

  @ParameterizedTest
  @MethodSource("validationErrorsProvider")
  public void shouldConcatenateValidationErrors(List<ValidationError> csvParseErrors,
      List<ValidationError> dtoValidationError) {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    UUID uploaderId = UUID.randomUUID();
    ConversionResults conversionResults = ConversionResults.from(
        Collections.singletonList(ConversionResult.failure(dtoValidationError))
    );
    when(csvRepository.findAll(bucket, filename))
        .thenReturn(new CsvFindResult(uploaderId, Collections.emptyList(), csvParseErrors));
    when(vehicleToLicenceConverter.convert(anyList())).thenReturn(conversionResults);

    // when
    RegisterResult actualRegisterResult = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    assertThat(actualRegisterResult.isSuccess()).isFalse();
    Iterable<ValidationError> allValidationErrors = Iterables
        .concat(csvParseErrors, dtoValidationError);
    assertThat(actualRegisterResult.getValidationErrors())
        .containsExactlyInAnyOrderElementsOf(allValidationErrors);
  }

  @Test
  public void shouldMapExceptionFromResolver() {
    // given
    String bucket = "bucket-1";
    String filename = "records.csv";
    S3MetadataException exception = new S3MetadataException("a");
    ValidationError validationError = ValidationError.s3Error("some error");
    given(csvRepository.findAll(bucket, filename)).willThrow(exception);
    given(exceptionResolver.resolve(exception)).willReturn(RegisterResult.failure(validationError));
    given(exceptionResolver.resolveToRegisterJobFailureStatus(exception))
        .willReturn(RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID);

    // when
    RegisterResult result = registerFromCsvService
        .register(bucket, filename, S3_REGISTER_JOB_ID, TYPICAL_CORRELATION_ID);

    // then
    then(result.getValidationErrors()).containsExactly(validationError);
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor).markFailureWithValidationErrors(S3_REGISTER_JOB_ID, RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID, Collections.singletonList(validationError));
    verify(registerJobSupervisor).updateStatus(S3_REGISTER_JOB_ID, RegisterJobStatus.RUNNING);
    verify(registerJobSupervisor).markFailureWithValidationErrors(S3_REGISTER_JOB_ID,
        RegisterJobStatus.STARTUP_FAILURE_INVALID_UPLOADER_ID,
        Collections.singletonList(validationError));
    verifyNoMoreInteractions(registerJobSupervisor);
  }

  private static Stream<Arguments> validationErrorsProvider() {
    return Stream.of(
        Arguments.arguments(
            Arrays.asList(
                ValidationError.valueError("1", "csv parse error 1"),
                ValidationError.valueError("2", "csv parse error 2")
            ),
            Arrays.asList(
                ValidationError.valueError("3", "dto validation error 1"),
                ValidationError.valueError("4", "dto validation error 2")
            )
        ),
        Arguments.arguments(
            Collections.emptyList(),
            Arrays.asList(
                ValidationError.valueError("3", "dto validation error 1"),
                ValidationError.valueError("4", "dto validation error 2")
            )
        ),
        Arguments.arguments(
            Arrays.asList(
                ValidationError.valueError("1", "csv parse error 1"),
                ValidationError.valueError("2", "csv parse error 2")
            ),
            Collections.emptyList()
        )
    );
  }

  private ConversionResults mockConversionResult(List<VehicleDto> vehicles) {
    VehicleDto vehicleDto = vehicles.iterator().next();
    ConversionResults conversionResults = ConversionResults.from(
        Collections.singletonList(ConversionResult.success(
            TaxiPhvVehicleLicence.builder()
                .vrm(vehicleDto.getVrm())
                .licenseDates(
                    new LicenseDates(
                        LocalDate.parse(vehicleDto.getStart()), LocalDate.parse(vehicleDto.getEnd())
                    )
                )
                .vehicleType(VehicleType.valueOf(vehicleDto.getTaxiOrPhv().toUpperCase()))
                .licensingAuthority(
                    LicensingAuthority.withNameOnly(vehicleDto.getLicensingAuthorityName()))
                .licensePlateNumber(vehicleDto.getLicensePlateNumber())
                .wheelchairAccessible(vehicleDto.getWheelchairAccessibleVehicle())
                .build()
            )
        )
    );
    given(vehicleToLicenceConverter.convert(vehicles)).willReturn(conversionResults);
    return conversionResults;
  }

  private List<VehicleDto> mockDataAtS3(String bucket, String filename, UUID uploaderId) {
    VehicleDto licence = VehicleDto.builder()
        .vrm("8839GF")
        .start(DateHelper.today().toString())
        .end(DateHelper.tomorrow().toString())
        .taxiOrPhv("taxi")
        .licensingAuthorityName("la-name")
        .licensePlateNumber("plate")
        .wheelchairAccessibleVehicle(true)
        .build();
    List<VehicleDto> licences = Collections.singletonList(licence);

    when(csvRepository.findAll(bucket, filename))
        .thenReturn(new CsvFindResult(uploaderId, licences, Collections.emptyList()));
    return licences;
  }
}