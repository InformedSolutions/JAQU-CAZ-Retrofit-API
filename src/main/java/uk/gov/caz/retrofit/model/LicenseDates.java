package uk.gov.caz.retrofit.model;

import java.time.LocalDate;
import lombok.Value;

@Value
public class LicenseDates {
  LocalDate start;
  LocalDate end;
}
