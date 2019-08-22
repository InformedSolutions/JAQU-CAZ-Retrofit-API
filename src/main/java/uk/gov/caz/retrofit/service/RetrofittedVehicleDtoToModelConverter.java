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
   */
  public ConversionResults convert(List<RetrofittedVehicleDto> vehicles) {
    List<ConversionResult> conversionResultList = vehicles.stream()
        .map(this::toRetrofittedVehicle)
        .collect(Collectors.toList());
    return ConversionResults.from(conversionResultList);
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