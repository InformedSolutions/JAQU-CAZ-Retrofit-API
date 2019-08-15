package uk.gov.caz.taxiregister.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * If you change anything in this class remember to update API documentation in {@link VehicleDto}
 * if necessary.
 */
public enum VehicleType {
  @JsonProperty("taxi")
  TAXI,
  PHV
}
