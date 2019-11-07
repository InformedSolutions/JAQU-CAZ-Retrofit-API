package uk.gov.caz.retrofit.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Contain information about the lambda container.
 */
public class LambdaContainerStats {
  private static final String INSTANCE_ID = UUID.randomUUID().toString();
  private static final DateTimeFormatter formatter = 
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
  private static LocalDateTime latestRequestTime;
  
  private LambdaContainerStats() {
    throw new IllegalStateException("Utility class");
  }
  
  /**
   * Set the time that the container serves a request.
   */
  public static void setRequestTime(LocalDateTime dt) {
    latestRequestTime = dt;
  }
  
  /**
   * Get the container stats.
   * 
   * @return a string that contains lambda container Id and (optionally) the time
   *         that the container last serve a request.
   */
  public static String getStats() {
    try {
      ObjectMapper obj = new ObjectMapper();
      Map<String, String> retVal = new HashMap<>();
      retVal.put("instanceId", INSTANCE_ID);
      if (latestRequestTime != null) {
        retVal.put("latestRequestTime",latestRequestTime.format(formatter));
      }
      return obj.writeValueAsString(retVal);
    } catch (JsonProcessingException ex) {
      return String.format("\"instanceId\": \"%s\"", INSTANCE_ID);
    }
  }
}
