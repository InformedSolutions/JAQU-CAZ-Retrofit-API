package uk.gov.caz.retrofit.dto;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import uk.gov.caz.retrofit.controller.exception.InvalidRequestPayloadException;

/**
 * DTO class that holds request attributes for whitelist info historical search.
 */
@Value
@Builder(toBuilder = true)
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class RetrofitInfoHistoricalRequest {

  private static final Map<Function<RetrofitInfoHistoricalRequest, Boolean>, String> validators =
      ImmutableMap.<Function<RetrofitInfoHistoricalRequest, Boolean>, String>builder()
          .put(startDateNotNull(), "'startDate' cannot be null")
          .put(endDateNotNull(), "'endDate' cannot be null")
          .put(startNotAfterEndDate(), "'startDate' need to be before 'endDate'")
          .put(pageNumberPositiveValue(), "'pageNumber' cannot be null")
          .put(pageSizePositiveValue(), "'pageSize' cannot be null")
          .build();

  @ApiModelProperty(value =
      "${swagger.model.descriptions.retrofit-info-historical-request.start-date}")
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate startDate;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.retrofit-info-historical-request.end-date}")
  @DateTimeFormat(iso = ISO.DATE)
  LocalDate endDate;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.retrofit-info-historical-request.page-number}")
  Integer pageNumber;

  @ApiModelProperty(value =
      "${swagger.model.descriptions.retrofit-info-historical-request.page-size}")
  Integer pageSize;


  /**
   * Public method that validates given object and throws exceptions if validation doesn't pass.
   */
  public void validate() {
    validators.forEach((validator, message) -> {
      boolean isValid = validator.apply(this);

      if (!isValid) {
        throw new InvalidRequestPayloadException(message);
      }
    });
  }

  /**
   * Helper method to convert dates.
   *
   * @return {@link LocalDateTime}
   */
  public LocalDateTime getLocalStartDate() {
    return toLocalDateTime(this.getStartDate(), LocalTime.MIDNIGHT);
  }

  /**
   * Helper method to convert dates.
   *
   * @return {@link LocalDateTime}
   */
  public LocalDateTime getLocalEndDate() {
    return toLocalDateTime(this.getEndDate(), LocalTime.MIDNIGHT.minusNanos(1));
  }

  /**
   * Returns a lambda that verifies if 'start date' is not null.
   */
  private static Function<RetrofitInfoHistoricalRequest, Boolean> startDateNotNull() {
    return request -> Objects.nonNull(request.getStartDate());
  }

  /**
   * Returns a lambda that verifies if 'end date' is not null.
   */
  private static Function<RetrofitInfoHistoricalRequest, Boolean> endDateNotNull() {
    return request -> Objects.nonNull(request.getEndDate());
  }

  /**
   * Returns a lambda that verifies if 'start date' is before 'end date.
   */
  private static Function<RetrofitInfoHistoricalRequest, Boolean> startNotAfterEndDate() {
    return request -> request.getStartDate().isBefore(request.getEndDate())
        || request.getStartDate().equals(request.getEndDate());
  }

  /**
   * Returns a lambda that verifies if 'page number' is not null.
   */
  private static Function<RetrofitInfoHistoricalRequest, Boolean> pageNumberPositiveValue() {
    return request -> Objects.nonNull(request.getPageNumber()) && request.getPageNumber() >= 0;
  }

  /**
   * Returns a lambda that verifies if 'page size' is not null.
   */
  private static Function<RetrofitInfoHistoricalRequest, Boolean> pageSizePositiveValue() {
    return request -> Objects.nonNull(request.getPageSize()) && request.getPageSize() >= 1;
  }

  /**
   * Convert date and time to {@link LocalDateTime}.
   */
  private static LocalDateTime toLocalDateTime(LocalDate date, LocalTime localTime) {
    return LocalDateTime.of(date, localTime).atZone(ZoneId.of("Europe/London"))
        .withZoneSameInstant(ZoneId.of("GMT")).toLocalDateTime();
  }
}
