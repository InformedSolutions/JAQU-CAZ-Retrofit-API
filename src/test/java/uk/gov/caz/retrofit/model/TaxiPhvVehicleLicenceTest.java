package uk.gov.caz.retrofit.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.caz.retrofit.DateHelper;

class TaxiPhvVehicleLicenceTest {

  @Nested
  class isActive {

    @ParameterizedTest
    @MethodSource("uk.gov.caz.retrofit.model.TaxiPhvVehicleLicenceTest#activeLicenceDatesProvider")
    public void shouldReturnTrueForActiveLicence(LocalDate start, LocalDate end) {
      // given
      TaxiPhvVehicleLicence licence = licenceWith(start, end);

      // when
      boolean active = licence.isActive();

      // then
      assertThat(active).isTrue();
    }

    @ParameterizedTest
    @MethodSource("uk.gov.caz.retrofit.model.TaxiPhvVehicleLicenceTest#inactiveLicenceDatesProvider")
    public void shouldReturnFalseForInactiveLicence(LocalDate start, LocalDate end) {
      // given
      TaxiPhvVehicleLicence licence = licenceWith(start, end);

      // when
      boolean active = licence.isActive();

      // then
      assertThat(active).isFalse();
    }
  }

  static Stream<Arguments> activeLicenceDatesProvider() {
    return Stream.of(
        Arguments.arguments(DateHelper.yesterday(), DateHelper.tomorrow()), // start < today < end
        Arguments.arguments(DateHelper.yesterday(), DateHelper.today()), // start < today <= end
        Arguments.arguments(DateHelper.today(), DateHelper.today()), // start <= today <= end
        Arguments.arguments(DateHelper.today(), DateHelper.tomorrow()) // start <= today < end
    );
  }

  static Stream<Arguments> inactiveLicenceDatesProvider() {
    return Stream.of(
        Arguments.arguments(DateHelper.tomorrow(), DateHelper.nextWeek()), // today < start
        Arguments.arguments(DateHelper.weekAgo(), DateHelper.yesterday()) // end < today
    );
  }

  private TaxiPhvVehicleLicence licenceWith(LocalDate startDate, LocalDate endDate) {
    return TaxiPhvVehicleLicence.builder()
        .id(1)
        .uploaderId(UUID.randomUUID())
        .vrm("AAA999")
        .wheelchairAccessible(true)
        .licensePlateNumber("old")
        .vehicleType(VehicleType.TAXI)
        .licenseDates(new LicenseDates(startDate, endDate))
        .licensingAuthority(
            new LicensingAuthority(99, "la"))
        .build();
  }

}