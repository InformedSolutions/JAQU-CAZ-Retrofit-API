package uk.gov.caz.retrofit.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.caz.retrofit.repository.RetrofittedVehicleDtoCsvRepository;

@Component
@lombok.Value
public class RegisterServicesContext {
  RegisterService registerService;
  RegisterFromCsvExceptionResolver exceptionResolver;
  RegisterJobSupervisor registerJobSupervisor;
  RetrofittedVehicleDtoToModelConverter licenceConverter;
  RetrofittedVehicleDtoCsvRepository csvRepository;
  int maxValidationErrorCount;

  /**
   * Creates an instance of {@link RegisterServicesContext}.
   */
  public RegisterServicesContext(RegisterService registerService,
      RegisterFromCsvExceptionResolver exceptionResolver,
      RegisterJobSupervisor registerJobSupervisor,
      RetrofittedVehicleDtoToModelConverter licenceConverter,
      RetrofittedVehicleDtoCsvRepository csvRepository,
      @Value("${application.validation.max-errors-count}") int maxValidationErrorCount) {
    this.registerService = registerService;
    this.exceptionResolver = exceptionResolver;
    this.registerJobSupervisor = registerJobSupervisor;
    this.licenceConverter = licenceConverter;
    this.csvRepository = csvRepository;
    this.maxValidationErrorCount = maxValidationErrorCount;
  }
}
