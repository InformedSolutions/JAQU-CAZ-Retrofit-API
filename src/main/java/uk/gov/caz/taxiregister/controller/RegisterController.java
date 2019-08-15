package uk.gov.caz.taxiregister.controller;

import static uk.gov.caz.taxiregister.controller.Constants.API_KEY_HEADER;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.taxiregister.model.registerjob.RegisterJobStatus.FINISHED_SUCCESS;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.dto.ErrorResponse;
import uk.gov.caz.taxiregister.dto.ErrorsResponse;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.dto.Vehicles;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.SourceAwareRegisterService;
import uk.gov.caz.taxiregister.service.exception.ActiveJobsCountExceededException;

@RestController
@Slf4j
public class RegisterController implements RegisterControllerApiSpec {

  public static final String PATH = "/v1/scheme-management";
  static final String INVALID_UPLOADER_ID_ERROR_MESSAGE = "Invalid format of uploader-id, "
      + "expected: UUID";
  static final String NULL_VEHICLE_DETAILS_ERROR_MESSAGE = "'vehicleDetails' cannot be null";
  private static final String REGISTER_JOB_NAME_SUFFIX = "";

  private final RegisterJobSupervisor registerJobSupervisor;
  private final SourceAwareRegisterService registerService;
  private final int maxLicencesCount;

  /**
   * Creates an instance of {@link RegisterController}.
   */
  public RegisterController(RegisterJobSupervisor registerJobSupervisor,
      SourceAwareRegisterService registerService,
      @Value("${api.max-licences-count}") int maxLicencesCount) {
    this.registerJobSupervisor = registerJobSupervisor;
    this.registerService = registerService;
    this.maxLicencesCount = maxLicencesCount;
    log.info("Set {} as the maximum number of vehicles handled by REST API", maxLicencesCount);
  }

  @Override
  public ResponseEntity<?> register(@RequestBody Vehicles vehicles,
      @RequestHeader(CORRELATION_ID_HEADER) String correlationId,
      @RequestHeader(API_KEY_HEADER) String apiKey) {
    UUID uploaderId = extractUploaderId(apiKey);
    checkPreconditions(vehicles, uploaderId);

    RegisterJobName registerJobName = registerJobSupervisor.start(
        StartParams.builder()
            .correlationId(correlationId)
            .uploaderId(uploaderId)
            .registerJobNameSuffix(REGISTER_JOB_NAME_SUFFIX)
            .registerJobTrigger(RegisterJobTrigger.API_CALL)
            .registerJobInvoker(
                registerJobId -> registerService.register(vehicles.getVehicleDetails(),
                    uploaderId, registerJobId, correlationId))
            .build());

    return registerJobSupervisor.findJobWithName(registerJobName)
        .map(registerJob -> toResponseEntity(registerJob, correlationId))
        .orElseThrow(() -> new IllegalStateException(
            "Unable to fetch register job instance for job name: " + registerJobName.getValue()));
  }

  private UUID extractUploaderId(String apiKey) {
    try {
      return UUID.fromString(apiKey);
    } catch (IllegalArgumentException e) {
      throw new InvalidUploaderIdFormatException(INVALID_UPLOADER_ID_ERROR_MESSAGE);
    }
  }

  private ResponseEntity<?> toResponseEntity(RegisterJob registerJob, String correlationId) {
    if (registerJob.getStatus() == FINISHED_SUCCESS) {
      return createSuccessRegisterResponse(correlationId);
    }
    return createFailureRegisterResponse(correlationId, registerJob);
  }

  private ResponseEntity<ErrorsResponse> createFailureRegisterResponse(String correlationId,
      RegisterJob registerJob) {
    List<ErrorResponse> errors = registerJob.getErrors()
        .stream()
        .map(ErrorResponse::from)
        .collect(Collectors.toList());
    return ResponseEntity.badRequest()
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(ErrorsResponse.from(errors));
  }

  private ResponseEntity<Map<String, String>> createSuccessRegisterResponse(String correlationId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(Collections.singletonMap("detail", "Register updated successfully."));
  }

  private void checkPreconditions(Vehicles vehicles, UUID uploaderId) {
    checkNotNullPrecondition(vehicles.getVehicleDetails());
    checkMaxLicencesCountPrecondition(vehicles);
    checkActiveJobsPrecondition(uploaderId);
  }

  private void checkNotNullPrecondition(List<VehicleDto> vehicleDetails) {
    if (vehicleDetails == null) {
      throw new PayloadValidationException(NULL_VEHICLE_DETAILS_ERROR_MESSAGE);
    }
  }

  private void checkMaxLicencesCountPrecondition(Vehicles vehicles) {
    if (vehicles.getVehicleDetails().size() > maxLicencesCount) {
      throw new PayloadValidationException(
          String.format("Max number of vehicles exceeded. Expected: up to %d, actual: %d. "
                  + "Please contact the system administrator for further information.",
              maxLicencesCount, vehicles.getVehicleDetails().size())
      );
    }
  }

  private void checkActiveJobsPrecondition(UUID uploaderId) {
    if (registerJobSupervisor.hasActiveJobs(uploaderId)) {
      throw new ActiveJobsCountExceededException(uploaderId);
    }
  }

  @ExceptionHandler({PayloadValidationException.class, HttpMessageConversionException.class,
      InvalidUploaderIdFormatException.class})
  ResponseEntity<ErrorsResponse> handleValidationException(Exception e) {
    log.info("Validation error: {}", e.getMessage());
    return ResponseEntity.badRequest()
        .body(ErrorsResponse.singleValidationErrorResponse(e.getMessage()));
  }

  @ExceptionHandler(ActiveJobsCountExceededException.class)
  ResponseEntity<ErrorsResponse> handleActiveJobCountExceededException(
      ActiveJobsCountExceededException e) {
    log.warn("Given uploaderId {} has already active job", e.getUploaderId());
    return ResponseEntity
        .status(HttpStatus.NOT_ACCEPTABLE)
        .body(ErrorsResponse
            .singleValidationErrorResponse("Given uploaderId has already active job"));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorsResponse> handle(Exception e) {
    log.error("Unhandled exception: ", e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorsResponse.internalError());
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<String> handleMissingHeaderException(MissingRequestHeaderException e) {
    log.error("Missing request header: ", e);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
  }

  private static class PayloadValidationException extends RuntimeException {

    public PayloadValidationException(String message) {
      super(message);
    }
  }

  private static class InvalidUploaderIdFormatException extends IllegalArgumentException {

    public InvalidUploaderIdFormatException(String message) {
      super(message);
    }
  }
}
