package uk.gov.caz.retrofit.service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ConversionResult;
import uk.gov.caz.retrofit.model.ConversionResults;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.model.ValidationError;

@Component
public class RetrofittedVehicleDtoToModelConverter {

  /**
   * Converts the passed list of {@link RetrofittedVehicleDto}s to {@link ConversionResults} with
   * the respect to the total number of validation errors that occur while converting. If {@code
   * maxErrorsCount} is zero, then no conversion takes place and an instance of {@link
   * ConversionResults} is returned with empty vehicles and validation errors.
   *
   * @param vehicles A list of {@link RetrofittedVehicleDto} which are to be mapped to a list of
   *     {@link RetrofittedVehicle} wrapped in {@link ConversionResults}.
   * @param maxErrorsCount The total number of validation errors that can happen during the
   *     conversion. If the size of validation errors reaches or exceeds this number the conversion
   *     is stopped immediately and the list of validation errors is truncated to satisfy the
   *     predicate: {@code ConversionResults.getValidationErrors().size() <= maxErrorsCount}.
   * @return An instance of {@link ConversionResults} which contains a list of converted vehicles
   *     into vehicles and a list of validation errors whose size does not exceed {@code
   *     maxErrorsCount} if {@code maxErrorsCount > 0}. If {@code maxErrorsCount == 0} an instance
   *     of {@link ConversionResults} with with empty vehicles and validation errors is returned.
   * @throws IllegalArgumentException if {@code maxErrorsCount < 0}.
   */
  public ConversionResults convert(List<RetrofittedVehicleDto> vehicles, int maxErrorsCount) {
    Preconditions.checkArgument(maxErrorsCount >= 0, "Expected maxErrorsCount >= 0, but %s < 0",
        maxErrorsCount);

    List<ConversionResult> conversionResults = Lists.newArrayListWithExpectedSize(vehicles.size());
    Iterator<RetrofittedVehicleDto> it = vehicles.iterator();
    int errorsCountLeft = maxErrorsCount;
    while (errorsCountLeft > 0 && it.hasNext()) {
      ConversionResult conversionResult = toRetrofittedVehicle(it.next());
      if (conversionResult.isFailure()) {
        conversionResult = truncateValidationErrorsToMatchLimit(errorsCountLeft, conversionResult);
        errorsCountLeft -= conversionResult.getValidationErrors().size();
      }
      conversionResults.add(conversionResult);
    }
    return ConversionResults.from(conversionResults);
  }

  private ConversionResult truncateValidationErrorsToMatchLimit(int errorsCountLeft,
      ConversionResult conversionResult) {
    List<ValidationError> validationErrors = conversionResult.getValidationErrors();
    if (validationErrors.size() > errorsCountLeft) {
      // assertion: validationErrors.size() > errorsCountLeft > 0
      return ConversionResult.failure(validationErrors.subList(0, errorsCountLeft));
    }
    return conversionResult;
  }

  /**
   * Converts the passed instance of {@link RetrofittedVehicleDto} to {@link ConversionResult}.
   */
  @VisibleForTesting
  ConversionResult toRetrofittedVehicle(RetrofittedVehicleDto vehicleDto) {
    List<ValidationError> validationResult = vehicleDto.validate();
    if (validationResult.isEmpty()) {
      RetrofittedVehicle retrofittedVehicle = RetrofittedVehicle.builder()
          .vrn(vehicleDto.getVrn())
          .vehicleCategory(vehicleDto.getVehicleCategory())
          .model(vehicleDto.getModel())
          .dateOfRetrofitInstallation(LocalDate.parse(vehicleDto.getDateOfRetrofitInstallation()))
          .build();
      return ConversionResult.success(retrofittedVehicle);
    }
    return ConversionResult.failure(validationResult);
  }
}