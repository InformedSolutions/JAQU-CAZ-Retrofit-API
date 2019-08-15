package uk.gov.caz.taxiregister.model;

import java.time.LocalDate;
import lombok.Value;

@Value
public class LicenseDates {
  LocalDate start;
  LocalDate end;
}
