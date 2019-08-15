package uk.gov.caz.taxiregister;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.taxiregister.controller.Constants.API_KEY_HEADER;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.caz.taxiregister.annotation.MockedMvcIntegrationTest;
import uk.gov.caz.taxiregister.controller.RegisterController;
import uk.gov.caz.taxiregister.dto.VehicleDto;
import uk.gov.caz.taxiregister.dto.Vehicles;
import uk.gov.caz.taxiregister.util.DatabaseInitializer;

@MockedMvcIntegrationTest
@Import(DatabaseInitializer.class)
@Slf4j
public class RegisterTestIT {

  private static final String ANY_CORRELATION_ID = UUID.randomUUID().toString();
  private static final String ANY_API_KEY = UUID.randomUUID().toString();
  private static final String INVALID_VRM = "A99A99A";
  private static final String VALUE_ERROR_MESSAGE = "Value error";
  private static final int EXPECTED_STATUS = 400;
  private static final String VALID_VRM = "1289J";

  @Autowired
  private DatabaseInitializer databaseInitializer;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private MockMvc mockMvc;

  @BeforeEach
  public void init() throws Exception {
    initializeDatabase();
  }

  @AfterEach
  public void clear() {
    cleanDatabase();
  }

  @Test
  public void registerTest() throws Exception {
    // validation tests
    String payloadWithInvalidVrm = buildPayloadWithInvalidVrm();
    mockMvc.perform(post(RegisterController.PATH + "/taxiphvdatabase")
        .content(payloadWithInvalidVrm)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .header(API_KEY_HEADER, ANY_API_KEY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].status").value(EXPECTED_STATUS))
        .andExpect(jsonPath("$.errors[0].vrm").value(INVALID_VRM))
        .andExpect(jsonPath("$.errors[0].title").value(VALUE_ERROR_MESSAGE))
        .andExpect(
            jsonPath("$.errors[0].detail").value("Invalid format of VRM (regex validation)."));

    // this is to ensure the the generated job id does not clash with the previous one
    sleepOneSecond();

    String payloadWithWrongLicenceDatesOrdering = buildPayloadWithWrongLicenceDatesOrdering();
    mockMvc.perform(post(RegisterController.PATH + "/taxiphvdatabase")
        .content(payloadWithWrongLicenceDatesOrdering)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .header(API_KEY_HEADER, ANY_API_KEY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].status").value(EXPECTED_STATUS))
        .andExpect(jsonPath("$.errors[0].vrm").value(VALID_VRM))
        .andExpect(jsonPath("$.errors[0].title").value(VALUE_ERROR_MESSAGE))
        .andExpect(jsonPath("$.errors[0].detail").value("'start' must be before 'end'."));

    sleepOneSecond();

    String payloadWithInvalidLicenceDateFormat = buildPayloadWithInvalidLicenceDateFormat();
    mockMvc.perform(post(RegisterController.PATH + "/taxiphvdatabase")
        .content(payloadWithInvalidLicenceDateFormat)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .header(API_KEY_HEADER, ANY_API_KEY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].status").value(EXPECTED_STATUS))
        .andExpect(jsonPath("$.errors[0].vrm").value(VALID_VRM))
        .andExpect(jsonPath("$.errors[0].title").value(VALUE_ERROR_MESSAGE))
        .andExpect(jsonPath("$.errors[0].detail").value(startsWith("Invalid format of licence")));

    sleepOneSecond();

    String payloadWithValidLicenceFormat = buildPayloadWith(VALID_VRM);
    mockMvc.perform(post(RegisterController.PATH + "/taxiphvdatabase")
        .content(payloadWithValidLicenceFormat)
        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .accept(MediaType.APPLICATION_JSON_UTF8_VALUE)
        .header(CORRELATION_ID_HEADER, ANY_CORRELATION_ID)
        .header(API_KEY_HEADER, ANY_API_KEY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.detail").value("Register updated successfully."));
  }

  private void sleepOneSecond() throws InterruptedException {
    TimeUnit.SECONDS.sleep(1);
  }

  private String buildPayloadWithInvalidVrm() throws JsonProcessingException {
    return buildPayloadWith(INVALID_VRM);
  }

  private String buildPayloadWith(String vrm) throws JsonProcessingException {
    Vehicles vehicles = new Vehicles(
        Collections.singletonList(
            VehicleDto.builder()
                .vrm(vrm)
                .start(DateHelper.today().toString())
                .end(DateHelper.tomorrow().toString())
                .taxiOrPhv("PHV")
                .licensingAuthorityName("la-1")
                .licensePlateNumber("la-plate-1")
                .wheelchairAccessibleVehicle(true)
                .build()
        )
    );
    return objectMapper.writeValueAsString(vehicles);
  }

  private String buildPayloadWithWrongLicenceDatesOrdering() throws JsonProcessingException {
    Vehicles vehicles = new Vehicles(
        Collections.singletonList(
            VehicleDto.builder()
                .vrm(VALID_VRM)
                .start(DateHelper.today().toString())
                .end(DateHelper.yesterday().toString())
                .taxiOrPhv("PHV")
                .licensingAuthorityName("la-name-1")
                .licensePlateNumber("la-plate-1")
                .wheelchairAccessibleVehicle(true)
                .build()
        )
    );
    return objectMapper.writeValueAsString(vehicles);
  }

  private String buildPayloadWithInvalidLicenceDateFormat() throws JsonProcessingException {
    Vehicles vehicles = new Vehicles(
        Collections.singletonList(
            VehicleDto.builder()
                .vrm("1289J")
                .start(DateTimeFormatter.BASIC_ISO_DATE.format(DateHelper.today()))
                .end(DateTimeFormatter.BASIC_ISO_DATE.format(DateHelper.tomorrow()))
                .taxiOrPhv("PHV")
                .licensingAuthorityName("la-name-1")
                .licensePlateNumber("la-plate-1")
                .wheelchairAccessibleVehicle(true)
                .build()
        )
    );
    return objectMapper.writeValueAsString(vehicles);
  }

  private void cleanDatabase() {
    log.info("Clearing database : start");
    databaseInitializer.clear();
    log.info("Clearing database : finish");
  }


  private void initializeDatabase() throws Exception {
    log.info("Initialing database : start");
    databaseInitializer.init();
    log.info("Initialing database : finish");
  }
}
