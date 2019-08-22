package uk.gov.caz.retrofit.model;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.caz.retrofit.model.VehicleLicenceLookupInfo.VehicleLicenceLookupInfoBuilder;

@ExtendWith(MockitoExtension.class)
class VehicleLicenceLookupInfoTest {

  @ParameterizedTest
  @MethodSource("validVehicleLicenceLookupInfoArgumentsProvider")
  public void shouldNotThrowIllegalArgumentExceptionWhen(boolean hasAnyOperatingLicenceActive,
      boolean wheelchairAccessible) {
    // given
    VehicleLicenceLookupInfoBuilder builder = VehicleLicenceLookupInfo
        .builder()
        .hasAnyOperatingLicenceActive(hasAnyOperatingLicenceActive)
        .wheelchairAccessible(wheelchairAccessible);

    // when
    Throwable throwable = catchThrowable(builder::build);

    // then
    then(throwable).isNull();
  }

  @Test
  public void shouldThrowIllegalArgumentExceptionWhenLicenceIsInactiveAndIsWheelchairAccessible() {
    // given
    VehicleLicenceLookupInfoBuilder builder = VehicleLicenceLookupInfo
        .builder()
        .hasAnyOperatingLicenceActive(false)
        .wheelchairAccessible(true);

    // when
    Throwable throwable = catchThrowable(builder::build);

    // then
    then(throwable)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Cannot have inactive operating licence with wheelchair accessible flag set to true");
  }

  static Stream<Arguments> validVehicleLicenceLookupInfoArgumentsProvider() {
    return Stream.of(
        Arguments.arguments(true, true),
        Arguments.arguments(true, false),
        Arguments.arguments(false, false)
    );
  }
}