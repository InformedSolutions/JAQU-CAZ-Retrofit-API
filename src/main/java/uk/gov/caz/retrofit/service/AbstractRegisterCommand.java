package uk.gov.caz.retrofit.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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

  /**
   * Gets {@code uploader-id}.
   *
   * @return {@link UUID} which represents the uploader.
   */
  abstract UUID getUploaderId();

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

      ConversionResults conversionResults = vehiclesConverter.convert(getVehiclesToRegister());

      if (conversionResults.hasValidationErrors() || hasParseValidationErrors()) {
        return prepareFailureResult(conversionResults);
      }

      RegisterResult result = registerService.register(
          conversionResults.getRetrofittedVehicles(),
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

  /**
   * Prepares a failure result for the registration process.
   * @param conversionResults business conversion results.
   * @return failure register result.
   */
  private RegisterResult prepareFailureResult(ConversionResults conversionResults) {
    List<ValidationError> businessErrors = conversionResults.getValidationErrors();
    List<ValidationError> parseErrors = getParseValidationErrors();
    log.info("There was total of {} business and {} parse errors",
        businessErrors.size(), parseErrors.size());

    List<ValidationError> initialErrorList = mergeBusinessAndParseErrors(
        businessErrors, parseErrors
    );
    List<ValidationError> errors = initialErrorList.stream()
        .sorted(Comparator.comparing(
            validationError -> validationError.getLineNumber().orElse(0)))
        .limit(maxValidationErrorCount)
        .collect(Collectors.toList());
    markJobFailed(RegisterJobStatus.FINISHED_FAILURE_VALIDATION_ERRORS, errors);

    return RegisterResult.failure(errors);
  }

  private boolean hasParseValidationErrors() {
    return !getParseValidationErrors().isEmpty();
  }

  private List<ValidationError> mergeBusinessAndParseErrors(
      List<ValidationError> businessErrors, List<ValidationError> parseErrors) {
    return Stream.of(businessErrors, parseErrors).flatMap(Collection::stream)
        .collect(Collectors.toList());
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
