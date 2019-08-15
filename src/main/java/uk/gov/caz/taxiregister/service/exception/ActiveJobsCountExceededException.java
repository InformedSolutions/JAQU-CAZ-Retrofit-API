package uk.gov.caz.taxiregister.service.exception;

import java.util.UUID;

public class ActiveJobsCountExceededException extends RuntimeException {

  private final UUID uploaderId;

  public ActiveJobsCountExceededException(UUID uploaderId) {
    this.uploaderId = uploaderId;
  }

  public UUID getUploaderId() {
    return uploaderId;
  }
}