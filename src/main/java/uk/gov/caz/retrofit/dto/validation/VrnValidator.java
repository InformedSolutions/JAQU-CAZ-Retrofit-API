package uk.gov.caz.retrofit.dto.validation;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.regex.Pattern;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ValidationError;

public class VrnValidator implements RetrofittedVehicleValidator {

  @VisibleForTesting
  static final int MAX_LENGTH = 7;

  @VisibleForTesting
  static final String MISSING_VRN_MESSAGE = "Data does not include the 'vrn' field which "
      + "is mandatory.";

  @VisibleForTesting
  static final String INVALID_LENGTH_MESSAGE_TEMPLATE = "VRN should have from 1 to %d characters "
      + "instead of %d.";

  @VisibleForTesting
  static final String INVALID_VRN_FORMAT_MESSAGE = "Invalid format of VRN (regex validation).";

  public static final String REGEX = "^"
      + "([A-Za-z]{3}[0-9]{1,4})"
      + "|([A-Za-z][0-9]{1,3}[A-Za-z]{3})"
      + "|([A-Za-z]{3}[0-9]{1,3}[A-Za-z])"
      + "|([A-Za-z]{2}[0-9]{2}[A-Za-z]{3})"
      + "|([A-Za-z]{1,3}[0-9]{1,3})"
      + "|([0-9]{1,4}[A-Za-z]{1,3})"
      + "|([A-Za-z]{1,2}[0-9]{1,4})"
      + "$";

  private static final Pattern vrnPattern = Pattern.compile(REGEX);

  @Override
  public List<ValidationError> validate(RetrofittedVehicleDto retrofittedVehicleDto) {
    ImmutableList.Builder<ValidationError> validationErrorsBuilder = ImmutableList.builder();
    VrnErrorMessageResolver errorResolver = new VrnErrorMessageResolver(
        retrofittedVehicleDto);

    String vrn = retrofittedVehicleDto.getVrn();

    if (vrn == null) {
      validationErrorsBuilder.add(errorResolver.missing());
    }

    if (vrn != null && (vrn.isEmpty() || vrn.length() > MAX_LENGTH)) {
      validationErrorsBuilder.add(errorResolver.invalidLength(vrn));
    }

    if (vrn != null && !vrn.isEmpty() && vrn.length() <= MAX_LENGTH
        && !vrnPattern.matcher(vrn).matches()) {
      validationErrorsBuilder.add(errorResolver.invalidFormat(vrn));
    }

    return validationErrorsBuilder.build();
  }

  private static class VrnErrorMessageResolver extends ValidationErrorResolver {

    private VrnErrorMessageResolver(RetrofittedVehicleDto retrofittedVehicleDto) {
      super(retrofittedVehicleDto);
    }

    private ValidationError missing() {
      return missingFieldError(null, MISSING_VRN_MESSAGE);
    }

    private ValidationError invalidLength(String vrn) {
      return valueError(vrn, invalidLengthMessage(vrn));
    }

    private ValidationError invalidFormat(String vrn) {
      return valueError(vrn, INVALID_VRN_FORMAT_MESSAGE);
    }

    private String invalidLengthMessage(String vrn) {
      return String.format(INVALID_LENGTH_MESSAGE_TEMPLATE, MAX_LENGTH, vrn.length());
    }
  }
}
