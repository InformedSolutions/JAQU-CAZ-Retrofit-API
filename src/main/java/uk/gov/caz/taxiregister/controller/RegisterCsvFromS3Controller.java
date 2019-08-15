package uk.gov.caz.taxiregister.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.taxiregister.dto.ErrorsResponse;
import uk.gov.caz.taxiregister.dto.RegisterCsvFromS3JobHandle;
import uk.gov.caz.taxiregister.dto.RegisterJobStatusDto;
import uk.gov.caz.taxiregister.dto.StartRegisterCsvFromS3JobCommand;
import uk.gov.caz.taxiregister.dto.StatusOfRegisterCsvFromS3JobQueryResult;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJob;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobName;
import uk.gov.caz.taxiregister.model.registerjob.RegisterJobTrigger;
import uk.gov.caz.taxiregister.service.AsyncBackgroundJobStarter;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor;
import uk.gov.caz.taxiregister.service.RegisterJobSupervisor.StartParams;
import uk.gov.caz.taxiregister.service.UploaderIdS3MetadataExtractor;
import uk.gov.caz.taxiregister.service.exception.ActiveJobsCountExceededException;

@RestController
@Slf4j
public class RegisterCsvFromS3Controller implements RegisterCsvFromS3ControllerApiSpec {

  public static final String PATH = "/v1/scheme-management/register-csv-from-s3/jobs";

  private final AsyncBackgroundJobStarter asyncBackgroundJobStarter;
  private final RegisterJobSupervisor registerJobSupervisor;
  private final UploaderIdS3MetadataExtractor uploaderIdS3MetadataExtractor;

  /**
   * Creates new instance of {@link RegisterCsvFromS3Controller} class.
   *
   * @param asyncBackgroundJobStarter Implementation of {@link AsyncBackgroundJobStarter}
   *     interface.
   * @param registerJobSupervisor {@link RegisterJobSupervisor} that supervises whole job run.
   * @param uploaderIdS3MetadataExtractor {@link UploaderIdS3MetadataExtractor} that allows to
   *     get 'uploader-id' metadata from CSV file.
   */
  public RegisterCsvFromS3Controller(
      AsyncBackgroundJobStarter asyncBackgroundJobStarter,
      RegisterJobSupervisor registerJobSupervisor,
      UploaderIdS3MetadataExtractor uploaderIdS3MetadataExtractor) {
    this.asyncBackgroundJobStarter = asyncBackgroundJobStarter;
    this.registerJobSupervisor = registerJobSupervisor;
    this.uploaderIdS3MetadataExtractor = uploaderIdS3MetadataExtractor;
  }

  @Override
  public ResponseEntity<RegisterCsvFromS3JobHandle> startRegisterJob(
      String correlationId, StartRegisterCsvFromS3JobCommand startCommand) {
    UUID uploaderId = getUploaderIdOrThrowIfUnableTo(startCommand);

    checkPreconditions(uploaderId);

    StartParams startParams = prepareStartParams(correlationId, startCommand, uploaderId);
    RegisterJobName registerJobName = registerJobSupervisor.start(startParams);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .header(CORRELATION_ID_HEADER, correlationId)
        .body(new RegisterCsvFromS3JobHandle(registerJobName.getValue()));
  }

  private UUID getUploaderIdOrThrowIfUnableTo(
      StartRegisterCsvFromS3JobCommand startCommand) {
    Optional<UUID> uploaderId = uploaderIdS3MetadataExtractor
        .getUploaderId(startCommand.getS3Bucket(), startCommand.getFilename());

    return uploaderId.orElseThrow(
        () -> new UnableToGetUploaderIdMetadataException(prepareErrorMessage(startCommand)));
  }

  private String prepareErrorMessage(StartRegisterCsvFromS3JobCommand startCommand) {
    return "Unable to fetch \"uploader-id\" metadata from S3 Bucket: " + startCommand.getS3Bucket()
        + "; File: " + startCommand.getFilename();
  }

  @Override
  public ResponseEntity<StatusOfRegisterCsvFromS3JobQueryResult> queryForStatusOfRegisterJob(
      String correlationId, String registerJobName) {

    Optional<RegisterJob> registerJobOptional = registerJobSupervisor
        .findJobWithName(new RegisterJobName(registerJobName));
    return registerJobOptional
        .map(registerJob -> ResponseEntity.ok()
            .header(CORRELATION_ID_HEADER, correlationId)
            .body(toQueryResult(registerJob)))
        .orElseGet(() -> ResponseEntity.notFound()
            .header(CORRELATION_ID_HEADER, correlationId)
            .build());
  }

  private StartParams prepareStartParams(String correlationId,
      StartRegisterCsvFromS3JobCommand startRegisterCsvFromS3JobCommand, UUID uploaderId) {
    return StartParams.builder()
        .registerJobTrigger(RegisterJobTrigger.CSV_FROM_S3)
        .registerJobNameSuffix(stripCsvExtension(startRegisterCsvFromS3JobCommand.getFilename()))
        .correlationId(correlationId)
        .uploaderId(uploaderId)
        .registerJobInvoker(
            asyncRegisterJobInvoker(correlationId, startRegisterCsvFromS3JobCommand))
        .build();
  }

  private String stripCsvExtension(String csvFileName) {
    return csvFileName.replace(".csv", "");
  }

  private RegisterJobSupervisor.RegisterJobInvoker asyncRegisterJobInvoker(String correlationId,
      StartRegisterCsvFromS3JobCommand startRegisterCsvFromS3JobCommand) {
    return registerJobId -> asyncBackgroundJobStarter.fireAndForgetRegisterCsvFromS3Job(
        registerJobId,
        startRegisterCsvFromS3JobCommand.getS3Bucket(),
        startRegisterCsvFromS3JobCommand.getFilename(),
        correlationId
    );
  }

  private StatusOfRegisterCsvFromS3JobQueryResult toQueryResult(RegisterJob registerJob) {
    RegisterJobStatusDto registerJobStatusDto = RegisterJobStatusDto.from(registerJob.getStatus());
    if (thereWereErrors(registerJob)) {
      return StatusOfRegisterCsvFromS3JobQueryResult
          .withStatusAndErrors(registerJobStatusDto, registerJob.getErrors());
    }
    return StatusOfRegisterCsvFromS3JobQueryResult.withStatusAndNoErrors(registerJobStatusDto);
  }

  private boolean thereWereErrors(RegisterJob registerJob) {
    return !registerJob.getErrors().isEmpty();
  }

  private void checkPreconditions(UUID uploaderId) {
    checkActiveJobsPrecondition(uploaderId);
  }

  private void checkActiveJobsPrecondition(UUID uploaderId) {
    if (registerJobSupervisor.hasActiveJobs(uploaderId)) {
      throw new ActiveJobsCountExceededException(uploaderId);
    }
  }

  @ExceptionHandler(UnableToGetUploaderIdMetadataException.class)
  ResponseEntity<String> handleUnableToGetUploaderIdMetadataException(Exception e) {
    log.error(e.getMessage());
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(e.getMessage());
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

  @ExceptionHandler(MissingRequestHeaderException.class)
  ResponseEntity<String> handleMissingHeaderException(MissingRequestHeaderException e) {
    log.error("Missing request header: ", e);
    return ResponseEntity.status(BAD_REQUEST).body(e.getMessage());
  }

  private static class UnableToGetUploaderIdMetadataException extends RuntimeException {

    public UnableToGetUploaderIdMetadataException(String message) {
      super(message);
    }
  }

}
