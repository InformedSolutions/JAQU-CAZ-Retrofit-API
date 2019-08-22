package uk.gov.caz.retrofit.service.exception;

public class CsvInvalidCharacterParseException extends IllegalArgumentException {
  @Override
  public String getMessage() {
    return  "Line contains invalid characters or a trailing comma";
  }
}
