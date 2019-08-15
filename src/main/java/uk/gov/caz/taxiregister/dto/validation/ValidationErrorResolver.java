package uk.gov.caz.taxiregister.dto.validation;

import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;
import uk.gov.caz.taxiregister.service.validation.CsvAwareValidationMessageModifier;

abstract class ValidationErrorResolver {
  private static final CsvAwareValidationMessageModifier messageModifier =
      new CsvAwareValidationMessageModifier();

  private final boolean lineNumberAware;
  private final int lineNumber;

  ValidationErrorResolver(VehicleDto vehicleDto) {
    this.lineNumberAware = vehicleDto.getLineNumber() > 0;
    this.lineNumber = lineNumberAware ? vehicleDto.getLineNumber() : -1;
  }

  private String addCsvHeaderPresentInfoIfApplicable(String message) {
    return isLineNumberAware()
        ? messageModifier.addHeaderRowInfoSuffix(message, lineNumber)
        : message;
  }

  final ValidationError missingFieldError(String vrm, String message) {
    String embellishedMessage = addCsvHeaderPresentInfoIfApplicable(message);
    return isLineNumberAware()
        ? ValidationError.missingFieldError(vrm, embellishedMessage, lineNumber)
        : ValidationError.missingFieldError(vrm, embellishedMessage);
  }

  final ValidationError valueError(String vrm, String message) {
    String embellishedMessage = addCsvHeaderPresentInfoIfApplicable(message);
    return isLineNumberAware()
        ? ValidationError.valueError(vrm, embellishedMessage, lineNumber)
        : ValidationError.valueError(vrm, embellishedMessage);
  }

  private boolean isLineNumberAware() {
    return lineNumberAware;
  }
}
