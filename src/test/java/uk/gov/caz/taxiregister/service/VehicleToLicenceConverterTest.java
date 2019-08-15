package uk.gov.caz.taxiregister.service;

import static org.assertj.core.api.BDDAssertions.then;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ConversionResult;
import uk.gov.caz.taxiregister.model.ConversionResults;

@ExtendWith(MockitoExtension.class)
class VehicleToLicenceConverterTest {

  private VehicleToLicenceConverter converter = new VehicleToLicenceConverter();

  @Nested
  class WhenConvertingOneLicence {

    @Test
    public void shouldReturnSuccessWhenConvertVehicleWithWheelchairAccessibleVehicle() {
      // given
      VehicleDto licence = createValidLicenceWithWheelchairAccessible();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getLicence()).isNotNull();
    }

    @Test
    public void shouldReturnSuccessWhenConvertVehicleWithoutWheelchairAccessibleVehicle() {
      // given
      VehicleDto licence = createValidLicenceWithoutWheelchairAccessible();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isTrue();
      then(conversionResult.isFailure()).isFalse();
      then(conversionResult.getValidationErrors()).isEmpty();
      then(conversionResult.getLicence()).isNotNull();
    }

    @Test
    public void shouldReturnFailureWhenValidationFails() {
      // given
      VehicleDto licence = createInvalidLicence();

      // when
      ConversionResult conversionResult = converter.toLicence(licence);

      // then
      then(conversionResult.isSuccess()).isFalse();
      then(conversionResult.isFailure()).isTrue();
      then(conversionResult.getValidationErrors()).isNotEmpty();
      then(conversionResult.getLicence()).isNull();
    }
  }

  @Nested
  class WhenConvertingListOfLicences {

    @Test
    public void shouldReturnConvertedLicencesWithWheelchairAccessibleVehicle() {
      // given
      List<VehicleDto> licences = Collections.singletonList(createValidLicenceWithWheelchairAccessible());

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.getLicences()).hasSize(1);
    }

    @Test
    public void shouldReturnConvertedLicencesWithoutWheelchairAccessibleVehicle() {
      // given
      List<VehicleDto> licences = Collections.singletonList(
          createValidLicenceWithoutWheelchairAccessible());

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isFalse();
      then(conversionResults.getLicences()).hasSize(1);
    }

    @Test
    public void shouldReturnValidationErrorsAndConvertedLicences() {
      // given
      List<VehicleDto> licences = Arrays.asList(createValidLicenceWithWheelchairAccessible(), createValidLicenceWithoutWheelchairAccessible(), createInvalidLicence());

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.hasValidationErrors()).isTrue();
      then(conversionResults.getValidationErrors()).hasSize(1);
      then(conversionResults.getLicences()).hasSize(2);
    }

    @Test
    public void shouldFlattenValidationErrorsFromMoreThanOneLicence() {
      // given
      List<VehicleDto> licences = Arrays.asList(
          createInvalidLicenceWithThreeAttributes(),
          createInvalidLicenceWithTwoAttributes()
      );

      // when
      ConversionResults conversionResults = converter.convert(licences);

      // then
      then(conversionResults.getValidationErrors()).hasSize(5);
      then(conversionResults.getLicences()).isEmpty();
    }
  }

  private VehicleDto createValidLicenceWithWheelchairAccessible() {
    return VehicleDto.builder()
        .vrm("AAA999A")
        .start("2019-01-01")
        .end("2019-02-01")
        .taxiOrPhv("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle(true)
        .build();
  }

  private VehicleDto createValidLicenceWithoutWheelchairAccessible() {
    return VehicleDto.builder()
        .vrm("BW91HUN")
        .start("2019-03-09")
        .end("2019-05-06")
        .taxiOrPhv("taxi")
        .licensingAuthorityName("la-1")
        .licensePlateNumber("yGSJC")
        .build();
  }

  private VehicleDto createInvalidLicence() {
    return VehicleDto.builder()
        .vrm("9AAAA99")
        .start("2019-01-01")
        .end("2019-02-01")
        .taxiOrPhv("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle(true)
        .build();
  }

  private VehicleDto createInvalidLicenceWithThreeAttributes() {
    String invalidStartDate = "2019-01-01-01";
    String invalidEndDate = "2019-02-01-01";
    String invalidVrm = "8AAAA99";
    return VehicleDto.builder()
        .vrm(invalidVrm)
        .start(invalidStartDate)
        .end(invalidEndDate)
        .taxiOrPhv("taxi")
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle(true)
        .build();
  }

  private VehicleDto createInvalidLicenceWithTwoAttributes() {
    String invalidVrm = "AAAA99";
    String invalidTaxiOrPhv = "amphibian";
    return VehicleDto.builder()
        .vrm(invalidVrm)
        .start("2019-01-01")
        .end("2019-02-06")
        .taxiOrPhv(invalidTaxiOrPhv)
        .licensingAuthorityName("la-name-1")
        .licensePlateNumber("plate-1")
        .wheelchairAccessibleVehicle(true)
        .build();
  }
}