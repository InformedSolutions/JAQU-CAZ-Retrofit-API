package uk.gov.caz.retrofit.dto;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Value;
import uk.gov.caz.retrofit.model.registerjob.RegisterJobError;

/**
 * Provides status information about register job: whether it is running or finished, if there were
 * some errors and their details.
 */
@Value
public class StatusOfRegisterCsvFromS3JobQueryResult {

  public static StatusOfRegisterCsvFromS3JobQueryResult withStatusAndNoErrors(
      RegisterJobStatusDto registerJobStatusDto) {
    return new StatusOfRegisterCsvFromS3JobQueryResult(registerJobStatusDto, null);
  }

  /**
   * Creates an instance of {@link StatusOfRegisterCsvFromS3JobQueryResult} where {@code status} is
   * set to {@code registerJobStatusDto} and {@code errors} are mapped to an array of {@link
   * RegisterJobError#getDetail()}.
   */
  public static StatusOfRegisterCsvFromS3JobQueryResult withStatusAndErrors(
      RegisterJobStatusDto registerJobStatusDto, List<RegisterJobError> errors) {
    String[] errorsArray = errors.stream()
        .map(RegisterJobError::getDetail)
        .toArray(String[]::new);
    return new StatusOfRegisterCsvFromS3JobQueryResult(registerJobStatusDto, errorsArray);
  }

  /**
   * Status code of register job identified by name.
   */
  @ApiModelProperty(
      notes = "Status code of register job identified by name",
      allowableValues = "RUNNING, "
          + "SUCCESS,  "
          + "FAILURE"
  )
  @NotNull
  RegisterJobStatusDto status;

  /**
   * List of any errors that happened during job processing. They are supposed to be displayed to
   * the end user.
   */
  @ApiModelProperty(
      notes =
          "List of any errors that happened during job processing. They are supposed to be "
              + "displayed to the end user"
  )
  String[] errors;
}
