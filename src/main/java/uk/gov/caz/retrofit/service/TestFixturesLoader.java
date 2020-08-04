package uk.gov.caz.retrofit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.google.common.collect.Sets;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

@Profile("dev | st | integration-tests")
@Service
public class TestFixturesLoader {

  TestFixturesLoader(
      RetrofittedVehiclePostgresRepository repository,
      @Value("${application.test-fixtures-location}") String fixturesLocation,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.fixturesLocation = fixturesLocation;
    this.objectMapper = createEnhancedObjectMapper(objectMapper);
  }


  private final RetrofittedVehiclePostgresRepository repository;

  private final String fixturesLocation;

  private final ObjectMapper objectMapper;

  /**
   * Deletes all vehicles from the database and imports predefined from a JSON file.
   */
  public void loadTestData() {

    Set<RetrofittedVehicle> vehicleSet = testVehiclesFromFile();

    repository.deleteAll();

    repository.insertOrUpdate(vehicleSet);

  }

  /**
   * Parses a file located at {@code application.test-fixtures-location} containing licences in JSON
   * format and converts it to a collection of {@link RetrofittedVehicle}.
   */
  @SneakyThrows
  private Set<RetrofittedVehicle> testVehiclesFromFile() {
    File vehicleDetailsJson = new ClassPathResource(this.fixturesLocation).getFile();

    List<RetrofittedVehicle> vehiclesArray = objectMapper
        .readValue(vehicleDetailsJson, new TypeReference<List<RetrofittedVehicle>>(){});

    return Sets.newHashSet(vehiclesArray);
  }

  /**
   * Provides enhanced instance of {@link ObjectMapper} which can handle LocalDate field.
   */
  private ObjectMapper createEnhancedObjectMapper(ObjectMapper objectMapper) {
    ObjectMapper mapper = objectMapper.copy();
    JavaTimeModule module = new JavaTimeModule();
    module.addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE);
    mapper.registerModule(module);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

}
