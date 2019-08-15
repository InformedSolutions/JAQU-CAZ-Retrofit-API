package uk.gov.caz.taxiregister.service.exception;

public class CsvInvalidBooleanValueException extends IllegalArgumentException {

  public CsvInvalidBooleanValueException(String message) {
    super(message);
  }
}
