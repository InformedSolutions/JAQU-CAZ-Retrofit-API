package uk.gov.caz.retrofit.service;

import lombok.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.retrofit.repository.RetrofittedVehicleDtoCsvRepository;

@Component
@Value
public class RegisterServicesContext {
  RegisterService registerService;
  RegisterFromCsvExceptionResolver exceptionResolver;
  RegisterJobSupervisor registerJobSupervisor;
  RetrofittedVehicleDtoToModelConverter licenceConverter;
  RetrofittedVehicleDtoCsvRepository csvRepository;
}
