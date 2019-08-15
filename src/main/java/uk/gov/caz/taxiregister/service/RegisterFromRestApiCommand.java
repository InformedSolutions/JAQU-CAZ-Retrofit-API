package uk.gov.caz.taxiregister.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.model.ValidationError;

public class RegisterFromRestApiCommand extends AbstractRegisterCommand {

  private final UUID uploaderId;
  private final List<VehicleDto> licences;

  /**
   * Creates an instance of {@link RegisterFromRestApiCommand}.
   */
  public RegisterFromRestApiCommand(List<VehicleDto> licences, UUID uploaderId, int registerJobId,
      RegisterServicesContext registerServicesContext, String correlationId) {
    super(registerServicesContext, registerJobId, correlationId);
    this.uploaderId = uploaderId;
    this.licences = licences;
  }

  @Override
  void beforeExecute() {
  }

  @Override
  UUID getUploaderId() {
    return uploaderId;
  }

  @Override
  List<VehicleDto> getLicencesToRegister() {
    return licences;
  }

  @Override
  List<ValidationError> getLicencesParseValidationErrors() {
    return Collections.emptyList();
  }
}
