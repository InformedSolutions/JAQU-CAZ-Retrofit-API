package uk.gov.caz.retrofit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.retrofit.controller.Constants.CORRELATION_ID_HEADER;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.caz.retrofit.annotation.MockedMvcIntegrationTest;

@MockedMvcIntegrationTest
@Sql(scripts = "classpath:data/sql/clear.sql", executionPhase = AFTER_TEST_METHOD)
@Sql(scripts = "classpath:data/sql/add-sample-audit-data.sql", executionPhase = BEFORE_TEST_METHOD)
@Slf4j
class HistoricalInfoControllerDroneOnlyIT {

  private static final String PAGE_SIZE = "3";

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void shouldReturnDataForTheProvidedDateRangeUsingTheirLocalTimeWhenWinterTime()
      throws Exception {
    String vrn = "WNTR123";

    // given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrn)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2020-12-15")
        .param("endDate", "2020-12-15")
        .param("pageNumber", "0")
        .param("pageSize", PAGE_SIZE);

    // when
    ResultActions perform = mockMvc.perform(accept);

    // then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(1)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(1))
        .andExpect(jsonPath("$.perPage").value(3))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-12-15"));
  }

}