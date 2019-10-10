package uk.gov.caz.retrofit.dto;

import java.io.Serializable;
import java.util.Map;
import lombok.Data;

@Data
public class CloudWatchDataMessage {
  private String owner;
  private String messageType;
  private String logGroup;
  private String logStream;
  private String[] subscriptionFilters;
  private LogEvent[] logEvents;
  
  @Data
  public static class LogEvent implements Serializable {
    private String id;
    private long timestamp;
    private String message;
    private Map<String,String> extractedFields;
  }
}

