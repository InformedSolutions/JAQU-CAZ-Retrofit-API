package uk.gov.caz.taxiregister.dto.validation;

import uk.gov.caz.taxiregister.dto.RetrofittedVehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.service.validation.CsvAwareValidationMessageModifier;

abstract class ValidationErrorResolver {
  private static final CsvAwareValidationMessageModifier messageModifier =
      new CsvAwareValidationMessageModifier();

  private final boolean lineNumberAware;
  private final int lineNumber;

  ValidationErrorResolver(RetrofittedVehicleDto retrofittedVehicleDto) {
    this.lineNumberAware = retrofittedVehicleDto.getLineNumber() > 0;
    this.lineNumber = lineNumberAware ? retrofittedVehicleDto.getLineNumber() : -1;
  }

  private String addCsvHeaderPresentInfoIfApplicable(String message) {
    return isLineNumberAware()
        ? messageModifier.addHeaderRowInfoSuffix(message, lineNumber)
        : message;
  }

  final ValidationError missingFieldError(String vrn, String message) {
    String embellishedMessage = addCsvHeaderPresentInfoIfApplicable(message);
    return isLineNumberAware()
        ? ValidationError.missingFieldError(vrn, embellishedMessage, lineNumber)
        : ValidationError.missingFieldError(vrn, embellishedMessage);
  }

  final ValidationError valueError(String vrn, String message) {
    String embellishedMessage = addCsvHeaderPresentInfoIfApplicable(message);
    return isLineNumberAware()
        ? ValidationError.valueError(vrn, embellishedMessage, lineNumber)
        : ValidationError.valueError(vrn, embellishedMessage);
  }

  private boolean isLineNumberAware() {
    return lineNumberAware;
  }
}
