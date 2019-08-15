package uk.gov.caz.taxiregister.service.exception;

public class FatalErrorWithCsvFileMetadataException extends RuntimeException {

  public FatalErrorWithCsvFileMetadataException(String message) {
    super(message);
  }
}
