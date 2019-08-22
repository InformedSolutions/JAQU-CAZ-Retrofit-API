package uk.gov.caz.retrofit.dto;

import uk.gov.caz.retrofit.model.registerjob.RegisterJobStatus;

/**
 * If you change anything in this class remember to update API documentation in {@link
 * StatusOfRegisterCsvFromS3JobQueryResult} if necessary.
 */
public enum RegisterJobStatusDto {
  RUNNING,
  SUCCESS,
  FAILURE;

  /**
   * Create {@link RegisterJobStatusDto} enum from {@link RegisterJobStatus} model status.
   *
   * @param registerJobStatus Current status of model related enum (much more detailed).
   * @return {@link RegisterJobStatusDto} value.
   */
  public static RegisterJobStatusDto from(RegisterJobStatus registerJobStatus) {
    switch (registerJobStatus) {
      case STARTING:
      case RUNNING:
        return RUNNING;
      case FINISHED_SUCCESS:
        return SUCCESS;
      default:
        return FAILURE;
    }
  }
}
