package uk.gov.caz.retrofit.model;

import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TaxiPhvVehicleLicence {

  Integer id;

  UUID uploaderId;

  @NonNull
  String vrm;

  @NonNull
  LicenseDates licenseDates;

  @NonNull
  VehicleType vehicleType;

  @NonNull
  LicensingAuthority licensingAuthority;

  @NonNull
  String licensePlateNumber;

  Boolean wheelchairAccessible;

  /**
   * Checks whether this licence is valid, i.e. {@code startDate() <= now <= endDate()}
   *
   * @return true if this licence is valid, false otherwise
   */
  public boolean isActive() {
    Today today = new Today();
    LicenseDates dates = getLicenseDates();

    return today.isAfterOrEqual(dates.getStart())
        && today.isBeforeOrEqual(dates.getEnd());
  }

  private static class Today {

    private final LocalDate now = LocalDate.now();

    private boolean isAfterOrEqual(LocalDate date) {
      // !(now < date) <=> now >= date
      return !now.isBefore(date);
    }

    private boolean isBeforeOrEqual(LocalDate date) {
      // !(now > date) <=> now <= date
      return !now.isAfter(date);
    }
  }
}
