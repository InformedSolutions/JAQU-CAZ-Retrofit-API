package uk.gov.caz.taxiregister.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class LicenceDatesValidator implements LicenceValidator {

  @VisibleForTesting
  static final String MISSING_DATE_MESSAGE_TEMPLATE = "vehicle does not include the '%s' field "
      + "which is mandatory.";

  @VisibleForTesting
  static final String INVALID_DATE_FORMAT_TEMPLATE = "Invalid format of licence %s date, "
      + "should be ISO 8601.";

  @VisibleForTesting
  static final String INVALID_ORDER_DATE_MESSAGE = "'start' must be before 'end'.";

  @Override
  public List<ValidationError> validate(VehicleDto vehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    LicenceDatesErrorMessageResolver errorResolver = new LicenceDatesErrorMessageResolver(
        vehicleDto);

    String vrm = vehicleDto.getVrm();
    String start = vehicleDto.getStart();
    String end = vehicleDto.getEnd();

    LocalDate convertedStartDate = tryParsingDate(start, "start", vrm, errorResolver,
        validationErrorsBuilder);
    LocalDate convertedEndDate = tryParsingDate(end, "end", vrm, errorResolver,
        validationErrorsBuilder);

    if (convertedStartDate != null
        && convertedEndDate != null
        && convertedEndDate.isBefore(convertedStartDate)) {
      validationErrorsBuilder.add(errorResolver.invalidOrder(vrm));
    }

    return validationErrorsBuilder.build();
  }

  private LocalDate tryParsingDate(String stringifiedDate,
      String startOrEnd,
      String vrm,
      LicenceDatesErrorMessageResolver errorResolver,
      Builder<ValidationError> validationErrorsBuilder) {
    LocalDate convertedDate = null;
    if (stringifiedDate == null) {
      validationErrorsBuilder.add(errorResolver.missing(startOrEnd, vrm));
    } else {
      try {
        convertedDate = LocalDate.parse(stringifiedDate);
      } catch (DateTimeParseException e) {
        validationErrorsBuilder.add(errorResolver.invalidFormat(startOrEnd, vrm));
      }
    }
    return convertedDate;
  }

  private static class LicenceDatesErrorMessageResolver extends ValidationErrorResolver {

    private LicenceDatesErrorMessageResolver(VehicleDto vehicleDto) {
      super(vehicleDto);
    }

    private ValidationError missing(String startOrEnd, String vrm) {
      return missingFieldError(vrm, missingDateMessage(startOrEnd));
    }

    private ValidationError invalidFormat(String startOrEnd, String vrm) {
      return valueError(vrm, invalidDateFormatMessage(startOrEnd));
    }

    private ValidationError invalidOrder(String vrm) {
      return valueError(vrm, INVALID_ORDER_DATE_MESSAGE);
    }

    private String invalidDateFormatMessage(String startOrEnd) {
      return String.format(INVALID_DATE_FORMAT_TEMPLATE, startOrEnd);
    }

    private String missingDateMessage(String startOrEnd) {
      return String.format(MISSING_DATE_MESSAGE_TEMPLATE, startOrEnd);
    }
  }
}
