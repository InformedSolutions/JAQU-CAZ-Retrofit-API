package uk.gov.caz.retrofit.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.retrofit.dto.RetrofittedVehicleDto;
import uk.gov.caz.retrofit.model.ConversionResult;
import uk.gov.caz.retrofit.model.ConversionResults;

@ExtendWith(MockitoExtension.class)
class RetrofittedVehicleDtoToModelConverterTest {

  private RetrofittedVehicleDtoToModelConverter converter = new RetrofittedVehicleDtoToModelConverter();

  @Nested
  class WhenConvertingOneVehicle {

    @Test
    public void shouldReturnSuccessWhenConvertValidVehicle() {
      // given
      RetrofittedVehicleDto retrofittedVehicle = createValidRetrofittedVehicle();

      // when
      ConversionResult conversionResult = converter.toRetrofittedVehicle(retrofittedVehicle);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getRetrofittedVehicle()).isNotNull();
    }

    @Test
    public void shouldReturnFailureWhenValidationFails() {
      // given
      RetrofittedVehicleDto retrofittedVehicle = createInvalidRetrofittedVehicle();

      // when
      ConversionResult conversionResult = converter.toRetrofittedVehicle(retrofittedVehicle);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).isNotEmpty();
      then(conversionResult.getRetrofittedVehicle()).isNull();
    }
  }

  @Nested
  class WhenConvertingListOfVehicles {
    private static final int UNLIMITED_ERROR_COUNT = Integer.MAX_VALUE;

    @Test
    public void shouldReturnConvertedValidVehicles() {
      // given
      List<RetrofittedVehicleDto> retrofittedVehicleDtos = Collections.singletonList(
          createValidRetrofittedVehicle());

      // when
      ConversionResults conversionResults = converter.convert(retrofittedVehicleDtos, UNLIMITED_ERROR_COUNT);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.getRetrofittedVehicles()).hasSize(1);
    }

    @Test
    public void shouldConvertUpToPassedErrorThreshold() {
      // given
      int maxErrorCount = 3;
      List<RetrofittedVehicleDto> vehicles = Arrays.asList(
          createValidRetrofittedVehicle(),
          createInvalidRetrofittedVehicleWithThreeAttributes(),
          createInvalidRetrofittedVehicleWithTwoAttributes()
      );

      // when
      ConversionResults conversionResults = converter.convert(vehicles, maxErrorCount);

      // then
      then(conversionResults.getValidationErrors()).hasSize(maxErrorCount);
      then(conversionResults.getRetrofittedVehicles()).hasSize(1);
    }

    @Test
    public void shouldReturnValidationErrorsAndConvertedVehicles() {
      // given
      List<RetrofittedVehicleDto> retrofittedVehicleDtos = Arrays.asList(
          createValidRetrofittedVehicle(), createInvalidRetrofittedVehicle()
      );

      // when
      ConversionResults conversionResults = converter.convert(retrofittedVehicleDtos, UNLIMITED_ERROR_COUNT);

      // then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
      then(conversionResults.getRetrofittedVehicles()).hasSize(1);
    }

    @Test
    public void shouldConvertAndTruncateErrorsToPassedErrorThreshold() {
      // given
      int maxErrorCount = 4;
      // contains 5 validation errors in total
      List<RetrofittedVehicleDto> licences = Arrays.asList(
          createInvalidRetrofittedVehicleWithTwoAttributes(),
          createInvalidRetrofittedVehicleWithThreeAttributes()
      );

      // when
      ConversionResults conversionResults = converter.convert(licences, maxErrorCount);

      // then
      then(conversionResults.getValidationErrors()).hasSize(maxErrorCount);
      then(conversionResults.getRetrofittedVehicles()).isEmpty();
    }

    @Test
    public void shouldFlattenValidationErrorsFromMoreThanOneLicence() {
      // given
      List<RetrofittedVehicleDto> retrofittedVehicleDtos = Arrays.asList(
          createInvalidRetrofittedVehicleWithThreeAttributes(),
          createInvalidRetrofittedVehicleWithTwoAttributes()
      );

      // when
      ConversionResults conversionResults = converter.convert(retrofittedVehicleDtos, UNLIMITED_ERROR_COUNT);

      // then
      then(conversionResults.getValidationErrors()).hasSize(5);
      then(conversionResults.getRetrofittedVehicles()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1 -2, -15, -100})
    public void shouldThrowIllegalArgumentExceptionIfErrorCountIsNegative(int maxErrorCount) {
      // given
      List<RetrofittedVehicleDto> licences = Collections.singletonList(
          createInvalidRetrofittedVehicleWithTwoAttributes());

      // when
      Throwable throwable = catchThrowable(() -> converter.convert(licences, maxErrorCount));

      // then
      then(throwable).isInstanceOf(IllegalArgumentException.class);
    }
  }

  private RetrofittedVehicleDto createValidRetrofittedVehicle() {
    return RetrofittedVehicleDto.builder()
        .vrn("AAA999A")
        .vehicleCategory("category-1")
        .model("model-1")
        .dateOfRetrofitInstallation("2019-03-09")
        .build();
  }

  private RetrofittedVehicleDto createInvalidRetrofittedVehicle() {
    return RetrofittedVehicleDto.builder()
        .vrn("9AAAA99")
        .vehicleCategory("category-1")
        .model("model-1")
        .dateOfRetrofitInstallation("2019-03-09")
        .build();
  }

  private RetrofittedVehicleDto createInvalidRetrofittedVehicleWithThreeAttributes() {
    String invalidVrn = "8AAAA99";
    String invalidModel = "tooooooooooooooooooooooooLooooooooooooooong";
    String invalidVehicleCategory = "tooooooooooooooooooooooooLooooooooooooooong";
    return RetrofittedVehicleDto.builder()
        .vrn(invalidVrn)
        .vehicleCategory(invalidVehicleCategory)
        .model(invalidModel)
        .dateOfRetrofitInstallation("2019-03-09")
        .build();
  }

  private RetrofittedVehicleDto createInvalidRetrofittedVehicleWithTwoAttributes() {
    String invalidVrn = "8AAAA99";
    String invalidDateOfRetrofitInstallation = "2019-03-09-01";
    return RetrofittedVehicleDto.builder()
        .vrn(invalidVrn)
        .vehicleCategory("category-1")
        .model("model-1")
        .dateOfRetrofitInstallation(invalidDateOfRetrofitInstallation)
        .build();
  }
}