package uk.gov.caz.taxiregister.service;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResults;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus;

/**
 * Abstract class which is responsible for registering vehicles. It is not aware of underlying
 * vehicle storage (let it be REST API, CSV in S3).
 */
@Slf4j
public abstract class AbstractRegisterCommand {

  private final int registerJobId;
  private final String correlationId;

  private final RegisterService registerService;
  private final RegisterFromCsvExceptionResolver exceptionResolver;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final RetrofittedVehicleDtoToModelConverter licenceConverter;

  /**
   * Creates an instance of {@link AbstractRegisterCommand}.
   */
  public AbstractRegisterCommand(RegisterServicesContext registerServicesContext, int registerJobId,
      String correlationId) {
    this.registerService = registerServicesContext.getRegisterService();
    this.exceptionResolver = registerServicesContext.getExceptionResolver();
    this.registerJobSupervisor = registerServicesContext.getRegisterJobSupervisor();
    this.licenceConverter = registerServicesContext.getLicenceConverter();
    this.registerJobId = registerJobId;
    this.correlationId = correlationId;
  }

  abstract void beforeExecute();

  abstract UUID getUploaderId();

  abstract List<RetrofittedVehicleDto> getLicencesToRegister();

  abstract List<ValidationError> getLicencesParseValidationErrors();

  /**
   * Method that executes logic common for all providers eg. S3 or REST API.
   *
   * @return {@link RegisterResult} register result of given rows.
   */
  public final RegisterResult execute() {
    try {
      log.info("Processing registration, correlation-id: '{}' : start", getCorrelationId());

      markJobRunning();

      beforeExecute();

      ConversionResults conversionResults = licenceConverter.convert(getLicencesToRegister());

      if (conversionResults.hasValidationErrors() || hasParseValidationErrors()) {
        List<ValidationError> errors = merge(conversionResults.getValidationErrors(),
            getLicencesParseValidationErrors());
        markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS, errors);
        return RegisterResult.failure(errors);
      }

      RegisterResult result = registerService.register(
          // TODO leave set as it was
          Lists.newArrayList(conversionResults.getRetrofittedVehicles()),
          getUploaderId()
      );

      postProcessRegistrationResult(result);

      return result;
    } catch (Exception e) {
      RegisterResult result = exceptionResolver.resolve(e);
      markJobFailed(exceptionResolver.resolveToRegisterJobFailureStatus(e),
          result.getValidationErrors());
      return result;
    } finally {
      log.info("Processing registration, correlation-id: '{}' : finish", getCorrelationId());
    }
  }

  private boolean hasParseValidationErrors() {
    return !getLicencesParseValidationErrors().isEmpty();
  }

  private List<ValidationError> merge(List<ValidationError> a, List<ValidationError> b) {
    return Stream.of(a, b).flatMap(Collection::stream).collect(Collectors.toList());
  }

  private void postProcessRegistrationResult(RegisterResult result) {
    if (result.isSuccess()) {
      markJobFinished();
    } else {
      markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS,
          result.getValidationErrors());
    }
  }

  private void markJobFinished() {
    registerJobSupervisor.updateStatus(getRegisterJobId(), RegisterJobStatus.FINISHED_SUCCESS);
    log.info("Marked job '{}' as finished", getRegisterJobId());
  }

  private void markJobFailed(
      RegisterJobStatus jobStatus, List<ValidationError> validationErrors) {
    registerJobSupervisor.markFailureWithValidationErrors(
        getRegisterJobId(),
        jobStatus,
        validationErrors
    );
    log.warn("Marked job '{}' as failed with status '{}', the number of validation errors: {}",
        getRegisterJobId(), jobStatus, validationErrors.size());
  }

  private void markJobRunning() {
    registerJobSupervisor.updateStatus(getRegisterJobId(), RegisterJobStatus.RUNNING);
  }

  private int getRegisterJobId() {
    return registerJobId;
  }

  private String getCorrelationId() {
    return correlationId;
  }
}
