package uk.gov.caz.retrofit.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Class holding information about Retfrofit Status for Vrn.
 */
@Data
public class RetrofitStatus {

  LocalDateTime insertTimestamp;

  int rowCount;

  /**
   * Returns information whether Retrofit exists for a VRN.
   */
  public boolean exists() {
    return rowCount > 0;
  }

  private RetrofitStatus() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    int rowCount;
    LocalDateTime insertTimestamp;

    /**
     * Sets rowcount for a builder object.
     */
    public Builder rowCount(int rowCount) {
      this.rowCount = rowCount;
      return this;
    }

    /**
     * Sets timestamp for a builder object.
     */
    public Builder insertTimestamp(Timestamp insertTimestamp) {
      if (insertTimestamp != null) {
        this.insertTimestamp = insertTimestamp.toLocalDateTime();
      }
      return this;
    }

    /**
     * Builds an object.
     */
    public RetrofitStatus build() {
      RetrofitStatus info = new RetrofitStatus();
      info.setRowCount(this.rowCount);
      info.setInsertTimestamp(this.insertTimestamp);
      return info;
    }
  }
}
