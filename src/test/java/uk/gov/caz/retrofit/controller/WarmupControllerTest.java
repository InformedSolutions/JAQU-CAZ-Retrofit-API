package uk.gov.caz.retrofit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.retrofit.controller.Constants.CORRELATION_ID_HEADER;
import static uk.gov.caz.testutils.TestObjects.TYPICAL_CORRELATION_ID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import uk.gov.caz.correlationid.Configuration;

@ContextConfiguration(classes = {Configuration.class, WarmupController.class})
@WebMvcTest
public class WarmupControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void warmupLambdaContainerShouldBeSuccessful() throws Exception {
    sendARequestToWarmupLambdaContainer();
  }
  
  private void sendARequestToWarmupLambdaContainer()
      throws Exception {
     mockMvc.perform(
        get(WarmupController.PATH)
            .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
            .header(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID)
            .accept(MediaType.APPLICATION_JSON_UTF8_VALUE))
        .andExpect(status().isOk())
        .andExpect(header().string(CORRELATION_ID_HEADER, TYPICAL_CORRELATION_ID))
        .andExpect(jsonPath("$.instanceId").exists());
  }
}
