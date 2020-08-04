package uk.gov.caz.retrofit.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Helper class to transport all histories and total count.
 */
@Value
@Builder
public class RetrofitVehicleHistoricalInfo {

  /**
   * The total number of history changes associated with this vehicle.
   */
  int totalChangesCount;

  /**
   * A list of history changes associated with this vehicle.
   */
  List<RetrofitVehicleHistory> changes;
}
