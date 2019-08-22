package uk.gov.caz.retrofit;

import java.time.LocalDate;

public class DateHelper {

  public static LocalDate today() {
    return LocalDate.now();
  }

  public static LocalDate tomorrow() {
    return today().plusDays(1);
  }

  public static LocalDate nextWeek() {
    return today().plusWeeks(1);
  }

  public static LocalDate yesterday() {
    return today().minusDays(1);
  }

  public static LocalDate weekAgo() {
    return today().minusWeeks(1);
  }

  public static LocalDate monthAgo() {
    return today().minusMonths(1);
  }

  public static LocalDate yearAgo() {
    return today().minusYears(1);
  }
}
