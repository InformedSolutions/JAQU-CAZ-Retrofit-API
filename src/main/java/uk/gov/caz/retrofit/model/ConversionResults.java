package uk.gov.caz.retrofit.model;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversionResults {

  List<ValidationError> validationErrors;

  Set<RetrofittedVehicle> retrofittedVehicles;

  public boolean hasValidationErrors() {
    return !validationErrors.isEmpty();
  }

  /**
   * Creates an instance of {@link ConversionResults} from a list of {@link ConversionResult}. All
   * validation errors from each {@link ConversionResult} are flattened to one list. All
   * successfully converted vehicles are collected to a set in {@code retrofittedVehicles}
   * attribute.
   */
  public static ConversionResults from(List<ConversionResult> conversionResults) {
    List<ValidationError> validationErrors = conversionResults.stream()
        .filter(ConversionResult::isFailure)
        .map(ConversionResult::getValidationErrors)
        .flatMap(List::stream)
        .collect(Collectors.toList());

    Set<RetrofittedVehicle> vehicles = conversionResults.stream()
        .filter(ConversionResult::isSuccess)
        .map(ConversionResult::getRetrofittedVehicle)
        .collect(Collectors.toSet());

    return new ConversionResults(validationErrors, vehicles);
  }
}
