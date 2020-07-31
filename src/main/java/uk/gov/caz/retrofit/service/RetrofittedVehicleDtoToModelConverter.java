package uk.gov.caz.retrofit.service;

import com.google.common.annotations.VisibleForTesting;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ConversionResult;
import uk.gov.caz.retrofit.model.ConversionResults;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.model.ValidationError;

@Component
public class RetrofittedVehicleDtoToModelConverter {

  /**
   * Converts the passed list of {@link RetrofittedVehicleDto}s to {@link ConversionResults}.
   *
   * @param vehicles A list of {@link RetrofittedVehicleDto} which are to be mapped to a list of
   *     {@link RetrofittedVehicle} wrapped in {@link ConversionResults}.
   * @return An instance of {@link ConversionResults} which contains a list of converted vehicles
   *     into vehicles
   */
  public ConversionResults convert(List<RetrofittedVehicleDto> vehicles) {
    List<ConversionResult> conversionResults = vehicles.stream()
        .map(vehicle -> toRetrofittedVehicle(vehicle))
        .collect(Collectors.toList());
    return ConversionResults.from(conversionResults);
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