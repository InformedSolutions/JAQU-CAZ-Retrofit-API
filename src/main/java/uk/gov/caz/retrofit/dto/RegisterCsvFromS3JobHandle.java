package uk.gov.caz.retrofit.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Value;

/**
 * Class that represents a handle that uniquely identifies job that registers CSV file from S3.
 */
@Value
public class RegisterCsvFromS3JobHandle {

  /**
   * Name that uniquely identifies job of registering CSV from S3.
   */
  @ApiModelProperty(
      notes = "Name that uniquely identifies job of registering CSV from S3"
  )
  @NotNull
  @NotBlank
  String jobName;
}
