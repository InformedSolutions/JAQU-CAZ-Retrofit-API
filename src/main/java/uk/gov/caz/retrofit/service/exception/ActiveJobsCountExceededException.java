package uk.gov.caz.retrofit.service.exception;

import uk.gov.caz.retrofit.model.CsvContentType;

public class ActiveJobsCountExceededException extends RuntimeException {

  private final CsvContentType csvContentType;

  public ActiveJobsCountExceededException(CsvContentType csvContentType) {
    this.csvContentType = csvContentType;
  }

  public CsvContentType getCsvContentType() {
    return csvContentType;
  }
}