package uk.gov.caz.retrofit.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ConversionResults;
import uk.gov.caz.retrofit.model.ValidationError;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobStatus;

/**
 * Abstract class which is responsible for registering vehicles. It is not aware of underlying
 * vehicle storage (let it be REST API, CSV in S3).
 */
@Slf4j
public abstract class AbstractRegisterCommand {

  private final int registerJobId;
  private final String correlationId;
  private final int maxValidationErrorCount;

  private final RegisterService registerService;
  private final RegisterFromCsvExceptionResolver exceptionResolver;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final RetrofittedVehicleDtoToModelConverter vehiclesConverter;

  /**
   * Creates an instance of {@link AbstractRegisterCommand}.
   */
  public AbstractRegisterCommand(RegisterServicesContext registerServicesContext, int registerJobId,
      String correlationId) {
    this.registerService = registerServicesContext.getRegisterService();
    this.exceptionResolver = registerServicesContext.getExceptionResolver();
    this.registerJobSupervisor = registerServicesContext.getRegisterJobSupervisor();
    this.vehiclesConverter = registerServicesContext.getDtoToModelConverter();
    this.maxValidationErrorCount = registerServicesContext.getMaxValidationErrorCount();
    this.registerJobId = registerJobId;
    this.correlationId = correlationId;
  }

  abstract void beforeExecute();

  abstract List<RetrofittedVehicleDto> getVehiclesToRegister();

  abstract List<ValidationError> getParseValidationErrors();

  /**
   * Returns a boolean indicating whether the job should be marked as failed.
   */
  abstract boolean shouldMarkJobFailed();

  /**
   * Method hook before marking job as failed.
   */
  abstract void onBeforeMarkJobFailed();

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

      // assertion: conversionMaxErrorCount >= 0
      int conversionMaxErrorCount = maxValidationErrorCount - parseValidationErrorCount();

      ConversionResults conversionResults = vehiclesConverter.convert(
          getVehiclesToRegister(), conversionMaxErrorCount
      );

      if (conversionResults.hasValidationErrors() || hasParseValidationErrors()) {
        List<ValidationError> errors = merge(conversionResults.getValidationErrors(),
            getParseValidationErrors());
        markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS, errors);
        return RegisterResult.failure(errors);
      }

      RegisterResult result = registerService.register(conversionResults.getRetrofittedVehicles());

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
    return !getParseValidationErrors().isEmpty();
  }

  private int parseValidationErrorCount() {
    return getParseValidationErrors().size();
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
    onBeforeMarkJobFailed();

    if (shouldMarkJobFailed()) {
      registerJobSupervisor.markFailureWithValidationErrors(
          getRegisterJobId(),
          jobStatus,
          validationErrors
      );
      log.warn("Marked job '{}' as failed with status '{}', the number of validation errors: {}",
          getRegisterJobId(), jobStatus, validationErrors.size());
    }
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
