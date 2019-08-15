package uk.gov.caz.taxiregister.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import uk.gov.caz.taxiregister.dto.VehicleDto;

@Component
public class RegisterCommandFactory {

  private final RegisterServicesContext registerServicesContext;

  public RegisterCommandFactory(RegisterServicesContext registerServicesContext) {
    this.registerServicesContext = registerServicesContext;
  }

  public RegisterFromCsvCommand createRegisterFromCsvCommand(String bucket, String filename,
      int registerJobId, String correlationId) {
    return new RegisterFromCsvCommand(registerServicesContext, registerJobId, correlationId, bucket,
        filename);
  }

  public RegisterFromRestApiCommand createRegisterFromRestApiCommand(
      List<VehicleDto> licences, int registerJobId,
      String correlationId, UUID uploaderId) {
    return new RegisterFromRestApiCommand(licences, uploaderId, registerJobId,
        registerServicesContext, correlationId);
  }
}
