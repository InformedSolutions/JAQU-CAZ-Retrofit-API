package uk.gov.caz.retrofit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

@AllArgsConstructor
@Service
public class TestFixturesLoader {

  private RetrofittedVehiclePostgresRepository repository;

  public void loadTestData() throws IOException {

    repository.deleteAll();

    Set<RetrofittedVehicle> vehicleSet = testVehiclesFromFile();

    repository.insert(vehicleSet);

  }

  private Set<RetrofittedVehicle> testVehiclesFromFile() throws IOException {
    File vehicleDetailsJson = new ClassPathResource("/db/fixtures/vehicle-fixtures.json").getFile();

    ObjectMapper mapper = new ObjectMapper();

    JavaTimeModule module = new JavaTimeModule();
    module.addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE);
    mapper.registerModule(module);

    List<RetrofittedVehicle> vehiclesArray = mapper
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .readValue(vehicleDetailsJson, new TypeReference<List<RetrofittedVehicle>>(){});

    return Sets.newHashSet(vehiclesArray);
  }

}
