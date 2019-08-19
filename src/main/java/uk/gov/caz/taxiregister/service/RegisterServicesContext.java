package uk.gov.caz.taxiregister.service;

import lombok.Value;
import org.springframework.stereotype.Component;

@Component
@Value
public class RegisterServicesContext {
  RegisterService registerService;
  RegisterFromCsvExceptionResolver exceptionResolver;
  RegisterJobSupervisor registerJobSupervisor;
  RetrofittedVehicleDtoToModelConverter licenceConverter;
  RetrofittedVehicleDtoCsvRepository csvRepository;
}
