package uk.gov.caz.retrofit.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;
import static uk.gov.caz.retrofit.controller.RetrofitVehicleController.BASE_PATH;

import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.retrofit.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.retrofit.model.RetrofittedVehicle;
import uk.gov.caz.retrofit.repository.RetrofittedVehiclePostgresRepository;

@MockedMvcIntegrationTest
class RetrofitVehicleControllerTestIT {
  private static final String SOME_CORRELATION_ID = "63be7528-7efd-4f31-ae68-11a6b709ff1c";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private RetrofittedVehiclePostgresRepository retrofittedVehiclePostgresRepository;

  @AfterEach
  public void cleanup() {
    retrofittedVehiclePostgresRepository.deleteAll();
  }

  @Test
  public void shouldGet404IfVehicleDoesntExist() throws Exception {
    mockMvc.perform(get(BASE_PATH + "/CAS310")
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isNotFound());
  }

  @Test
  public void shouldGetRetrofitStatusForExistingVehicle() throws Exception {
    //given
    String existingVrn = "CAS222";
    retrofittedVehiclePostgresRepository.insertOrUpdate(
        Collections.singleton(
            RetrofittedVehicle.builder()
                .vrn(existingVrn)
                .vehicleCategory("Category")
                .model("Model")
                .dateOfRetrofitInstallation(LocalDate.now())
                .build()
        )
    );

    //then
    mockMvc.perform(get(BASE_PATH + "/" + existingVrn)
        .accept(MediaType.APPLICATION_JSON)
        .header(X_CORRELATION_ID_HEADER, SOME_CORRELATION_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.retrofitStatus", is(true)));
  }
}