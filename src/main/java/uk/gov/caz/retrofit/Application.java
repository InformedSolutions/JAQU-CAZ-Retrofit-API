package uk.gov.caz.retrofit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import uk.gov.caz.retrofit.amazonaws.AsyncLambdaBackgroundJobStarter;
import uk.gov.caz.retrofit.configuration.AwsConfiguration;
import uk.gov.caz.retrofit.configuration.RequestMappingConfiguration;
import uk.gov.caz.retrofit.configuration.SwaggerConfiguration;
import uk.gov.caz.retrofit.configuration.properties.SwaggerApiInfo;
import uk.gov.caz.retrofit.controller.RegisterCsvFromS3Controller;
import uk.gov.caz.retrofit.repository.RegisterJobRepository;
import uk.gov.caz.retrofit.repository.RetrofittedVehicleDtoCsvRepository;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;
import uk.gov.caz.retrofit.service.AsyncJavaBackgroundJobStarter;
import uk.gov.caz.retrofit.service.CsvFileOnS3MetadataExtractor;
import uk.gov.caz.retrofit.service.CsvObjectMapper;
import uk.gov.caz.retrofit.service.RegisterCommandFactory;
import uk.gov.caz.retrofit.service.RegisterFromCsvExceptionResolver;
import uk.gov.caz.retrofit.service.RegisterJobNameGenerator;
import uk.gov.caz.retrofit.service.RegisterJobSupervisor;
import uk.gov.caz.retrofit.service.RegisterService;
import uk.gov.caz.retrofit.service.RegisterServicesContext;
import uk.gov.caz.retrofit.service.RetrofittedVehicleDtoToModelConverter;
import uk.gov.caz.retrofit.service.SourceAwareRegisterService;
import uk.gov.caz.retrofit.service.validation.CsvAwareValidationMessageModifier;

@Import({
    AsyncLambdaBackgroundJobStarter.class, AwsConfiguration.class,
    RequestMappingConfiguration.class, SwaggerConfiguration.class, SwaggerApiInfo.class,
    RegisterCsvFromS3Controller.class, SourceAwareRegisterService.class, RegisterService.class,
    CsvFileOnS3MetadataExtractor.class, RegisterJobSupervisor.class,
    CsvAwareValidationMessageModifier.class, CsvObjectMapper.class,
    RegisterCommandFactory.class, RegisterJobNameGenerator.class,
    AsyncJavaBackgroundJobStarter.class, RegisterServicesContext.class,
    RegisterFromCsvExceptionResolver.class, RetrofittedVehicleDtoToModelConverter.class,
    RegisterJobRepository.class, RetrofittedVehicleDtoCsvRepository.class,
    RetrofittedVehiclePostgresRepository.class
})
@SpringBootApplication
public class Application {
  
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}