package uk.gov.caz.taxiregister.service;

import org.springframework.stereotype.Component;

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
}
