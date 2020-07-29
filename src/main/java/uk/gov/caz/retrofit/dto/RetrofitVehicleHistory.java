package uk.gov.caz.retrofit.dto;

import java.io.Serializable;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/**
 * Class that contains historical info of licence.
 */
@Value
@Builder
public class RetrofitVehicleHistory implements Serializable {

  private static final long serialVersionUID = -1697344242305227788L;

  /**
   * Date when licence was modified.
   */
  LocalDate modifyDate;

  /**
   * Action on vehicle, ie. Updated, Created or Removed
   */
  String action;

  /**
   * Category of the vehicle.
   */
  String vehicleCategory;

  /**
   * Model of the vehicle.
   */
  String model;

  /**
   * A date when retrofit happened for this vehicle.
   */
  LocalDate dateOfRetrofit;
}