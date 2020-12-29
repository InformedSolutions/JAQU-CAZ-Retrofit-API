package uk.gov.caz.retrofit.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
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
  @JsonFormat(shape = Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  LocalDateTime addedTimestamp;
}
