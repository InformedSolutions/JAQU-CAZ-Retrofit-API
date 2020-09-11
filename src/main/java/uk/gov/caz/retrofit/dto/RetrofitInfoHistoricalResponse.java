package uk.gov.caz.retrofit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Value object that represents whitelist info historical response.
 */
@Value
@Builder
public class RetrofitInfoHistoricalResponse {

  /**
   * Page that has been retrieved.
   */
  int page;

  /**
   * Total number of pages available (with current page size).
   */
  int pageCount;

  /**
   * The current page size.
   */
  int perPage;

  /**
   * The total number of changes associated with this vehicle.
   */
  int totalChangesCount;

  /**
   * A list of changes associated with this vehicle.
   */
  List<Change> changes;

  @Value
  @Builder
  public static class Change {

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Date of modification.
     */
    @JsonFormat(pattern = DATE_FORMAT)
    LocalDate modifyDate;

    /**
     * Status of current VRM for a specific date range.
     */
    String action;

    /**
     * Category type.
     */
    String vehicleCategory;

    /**
     * Model of the vehicle.
     */
    String model;

    /**
     * Date of modification.
     */
    @JsonFormat(pattern = DATE_FORMAT)
    LocalDate dateOfRetrofit;

    /**
     * Maps {@link RetrofitVehicleHistory} to {@link Change}.
     *
     * @param history An instance of {@link RetrofitVehicleHistory} to be mapped
     * @return An instance of {@link Change} mapped from {@link RetrofitVehicleHistory}
     */
    public static Change from(RetrofitVehicleHistory history) {
      return Change.builder()
          .modifyDate(history.getModifyDate())
          .action(history.getAction())
          .vehicleCategory(history.getVehicleCategory())
          .model(history.getModel())
          .dateOfRetrofit(history.getDateOfRetrofit())
          .build();
    }
  }
}
