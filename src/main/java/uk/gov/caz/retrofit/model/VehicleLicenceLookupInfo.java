package uk.gov.caz.retrofit.model;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder
public class VehicleLicenceLookupInfo {

  @Getter(AccessLevel.NONE)
  boolean hasAnyOperatingLicenceActive;

  boolean wheelchairAccessible;

  private VehicleLicenceLookupInfo(boolean hasAnyOperatingLicenceActive,
      boolean wheelchairAccessible) {
    Preconditions.checkArgument(hasAnyOperatingLicenceActive || !wheelchairAccessible,
        "Cannot have inactive operating licence with wheelchair accessible flag set to true"
    );
    this.hasAnyOperatingLicenceActive = hasAnyOperatingLicenceActive;
    this.wheelchairAccessible = wheelchairAccessible;
  }

  public boolean hasAnyOperatingLicenceActive() {
    return hasAnyOperatingLicenceActive;
  }
}
