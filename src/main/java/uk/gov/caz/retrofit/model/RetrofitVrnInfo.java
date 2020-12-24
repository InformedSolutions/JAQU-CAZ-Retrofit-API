package uk.gov.caz.retrofit.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetrofitVrnInfo {

  boolean exists;

  int rowCount;

  LocalDateTime insertTimestamp;

}
