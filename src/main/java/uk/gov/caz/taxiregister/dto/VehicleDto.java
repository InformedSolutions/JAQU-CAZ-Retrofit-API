package uk.gov.caz.taxiregister.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import uk.gov.caz.taxiregister.dto.validation.LicenceDatesValidator;
import uk.gov.caz.taxiregister.dto.validation.LicenceTypeValidator;
import uk.gov.caz.taxiregister.dto.validation.LicenceValidator;
import uk.gov.caz.taxiregister.dto.validation.LicensePlateNumberValidator;
import uk.gov.caz.taxiregister.dto.validation.LicensingAuthorityNameValidator;
import uk.gov.caz.taxiregister.dto.validation.VrmValidator;
import uk.gov.caz.taxiregister.model.ValidationError;

@Value
@Builder(toBuilder = true)
public class VehicleDto {

  private static final List<LicenceValidator> VALIDATORS = ImmutableList.of(
      new VrmValidator(),
      new LicenceDatesValidator(),
      new LicenceTypeValidator(),
      new LicensingAuthorityNameValidator(),
      new LicensePlateNumberValidator()
  );

  @ApiModelProperty(value = "${swagger.model.descriptions.vehicle.vrm}")
  @NotNull
  @Size(min = 1, max = 7)
  @Pattern(regexp = VrmValidator.REGEX)
  String vrm;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.start}")
  @NotNull
  @DateTimeFormat(iso = ISO.DATE)
  @Size(min = 8, max = 10)
  String start;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.end}")
  @NotNull
  @DateTimeFormat(iso = ISO.DATE)
  @Size(min = 8, max = 10)
  String end;

  @ApiModelProperty(
      notes = "${swagger.model.descriptions.vehicle.taxi-or-phv}",
      allowableValues = "taxi, PHV"
  )
  @NotNull
  @JsonProperty("taxiOrPHV")
  String taxiOrPhv;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.licensing-authority-name}")
  @NotNull
  @Size(min = 1, max = 50)
  String licensingAuthorityName;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.license-plate-number}")
  @NotNull
  @Size(min = 1, max = 15)
  String licensePlateNumber;

  @ApiModelProperty(notes = "${swagger.model.descriptions.vehicle.wheelchair-accessible-vehicle}")
  Boolean wheelchairAccessibleVehicle;

  @ApiModelProperty(hidden = true)
  int lineNumber;

  /**
   * Validates this instance.
   *
   * @return a list of validation errors if there are any. An empty list is returned if validation
   *     succeeds.
   */
  public List<ValidationError> validate() {
    return VALIDATORS.stream()
        .map(validator -> validator.validate(this))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
