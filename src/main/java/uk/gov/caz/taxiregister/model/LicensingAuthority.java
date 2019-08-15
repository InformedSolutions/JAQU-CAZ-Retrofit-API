package uk.gov.caz.taxiregister.model;

import com.google.common.base.Preconditions;
import lombok.NonNull;
import lombok.Value;

@Value
public class LicensingAuthority {
  Integer id;

  @NonNull
  String name;

  public static LicensingAuthority withNameOnly(String name) {
    Preconditions.checkNotNull(name, "Name cannot be null");
    return new LicensingAuthority(null, name);
  }
}
