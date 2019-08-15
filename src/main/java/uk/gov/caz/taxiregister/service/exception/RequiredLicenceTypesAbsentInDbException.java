package uk.gov.caz.taxiregister.service.exception;

public class RequiredLicenceTypesAbsentInDbException extends RuntimeException {
  public RequiredLicenceTypesAbsentInDbException(String message) {
    super(message);
  }
}
