package uk.gov.caz.taxiregister.dto;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Value;

/**
 * Command class that holds data required to start registering CSV from S3 job.
 */
@Value
public class StartRegisterCsvFromS3JobCommand {

  /**
   * Name of S3 Bucket in which CSV file is stored.
   */
  @ApiModelProperty(
      notes = "Name of S3 Bucket in which CSV file is stored"
  )
  @NotNull
  @NotBlank
  String s3Bucket;

  /**
   * Name of CSV file that should be registered.
   */
  @ApiModelProperty(
      notes = "Name of CSV file that should be registered"
  )
  @NotNull
  @NotBlank
  String filename;
}
