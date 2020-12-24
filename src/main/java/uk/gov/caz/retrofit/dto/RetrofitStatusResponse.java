package uk.gov.caz.retrofit.dto;

import java.time.LocalDateTime;
import lombok.Value;

/**
 * Class that represents response holding status of retrofit.
 */
@Value
public class RetrofitStatusResponse {

  /**
   * Indicates whether vehicles exists in DB.
   */
  boolean retrofitStatus;

  /**
   * Insert timestamp of value.
   */
  LocalDateTime addedTimestamp;
}
