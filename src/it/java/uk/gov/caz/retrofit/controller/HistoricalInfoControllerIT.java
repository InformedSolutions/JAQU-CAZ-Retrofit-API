package uk.gov.caz.retrofit.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.caz.retrofit.controller.Constants.CORRELATION_ID_HEADER;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class HistoricalInfoControllerIT {

  private static final String VRN = "JO32VXX";
  private static final String START_DATE = "2020-07-01";
  private static final String END_DATE = "2020-07-30";
  private static final String PAGE_NUMBER = "0";
  private static final String PAGE_SIZE = "3";

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void shouldFetchMultipleRowsForGivenVehicle() throws Exception {
    //given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, VRN)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", START_DATE)
        .param("endDate", END_DATE)
        .param("pageNumber", PAGE_NUMBER)
        .param("pageSize", PAGE_SIZE);

    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(3)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(3))
        .andExpect(jsonPath("$.perPage").value(3))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-07-27"))
        .andExpect(jsonPath("$.changes[0].dateOfRetrofit").value("2019-04-13"));
  }

  @Test
  public void shouldFetchSingleRow() throws Exception {
    //given
    String vrn = "DS98UDG";
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrn)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", START_DATE)
        .param("endDate", END_DATE)
        .param("pageNumber", PAGE_NUMBER)
        .param("pageSize", PAGE_SIZE);

    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(1)))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.totalChangesCount").value(1))
        .andExpect(jsonPath("$.perPage").value(3))
        .andExpect(jsonPath("$.pageCount").value(1))
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-07-27"))
        .andExpect(jsonPath("$.changes[0].dateOfRetrofit").value("2019-03-11"));
  }

  @Test
  public void shouldNotFetchSecondPageIfThereArentEnoughRows() throws Exception {
    //given
    String vrn = "abc01";
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrn)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", START_DATE)
        .param("endDate", END_DATE)
        .param("pageNumber", "89")
        .param("pageSize", PAGE_SIZE);

    //when
    ResultActions perform = mockMvc.perform(accept);

    //then
    perform.andExpect(status().isOk())
        .andExpect(jsonPath("$.changes", hasSize(0)))
        .andExpect(jsonPath("$.page").value(89))
        .andExpect(jsonPath("$.totalChangesCount").value(0))
        .andExpect(jsonPath("$.perPage").value(3))
        .andExpect(jsonPath("$.pageCount").value(0));
  }

  @Test
  public void shouldReturnDataForTheProvidedDateRangeUsingTheirLocalTimeWhenBST() throws Exception {
    String vrn = "BST1235";

    // given
    MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrn)
        .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .accept(MediaType.APPLICATION_JSON_VALUE)
        .param("startDate", "2020-07-16") // 2020-07-15 in UTC
        .param("endDate", "2020-07-16")
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
        .andExpect(jsonPath("$.changes[0].modifyDate").value("2020-07-16"));
  }

  @Disabled
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

  @Nested
  class BadRequest {

    @Test
    void shouldReturnNotFoundIfVrmIsNull() throws Exception {
      //given
      String vrn = null;
      MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrn)
          .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .param("startDate", START_DATE)
          .param("endDate", END_DATE)
          .param("pageNumber", PAGE_NUMBER)
          .param("pageSize", PAGE_SIZE);

      //when
      ResultActions perform = mockMvc.perform(accept);

      //then
      perform.andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @MethodSource(
        "uk.gov.caz.retrofit.controller.HistoricalInfoControllerIT#nullableRequestProperties")
    void shouldReturnBadRequestIfAnyRequestPropertyIsNull(String vrm, String startDate,
        String endDate,
        String pageNumber, String pageSize) throws Exception {
      //given
      MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrm)
          .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE);
      Optional.ofNullable(startDate).ifPresent(s -> accept.param("startDate", s));
      Optional.ofNullable(endDate).ifPresent(s -> accept.param("endDate", s));
      Optional.ofNullable(pageNumber).ifPresent(s -> accept.param("pageNumber", s));
      Optional.ofNullable(pageSize).ifPresent(s -> accept.param("pageSize", s));

      //when
      ResultActions perform = mockMvc.perform(accept);

      //then
      perform.andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource(
        "uk.gov.caz.retrofit.controller.HistoricalInfoControllerIT#notParsableRequestProperties")
    void shouldReturnBadRequestIfAnyRequestPropertyIsNotParsable(String vrm, String startDate,
        String endDate, String pageNumber, String pageSize) throws Exception {
      //given
      MockHttpServletRequestBuilder accept = get(HistoricalInfoController.BASE_PATH, vrm)
          .header(CORRELATION_ID_HEADER, UUID.randomUUID().toString())
          .contentType(MediaType.APPLICATION_JSON_VALUE)
          .accept(MediaType.APPLICATION_JSON_VALUE)
          .param("startDate", startDate)
          .param("endDate", endDate)
          .param("pageNumber", pageNumber)
          .param("pageSize", pageSize);
      //when
      ResultActions perform = mockMvc.perform(accept);
      //then
      perform.andExpect(status().isBadRequest());
    }
  }

  static Stream<Arguments> notParsableRequestProperties() {
    return Stream.of(
        Arguments.arguments(VRN, "not date", END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRN, "20200101", END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, "not date", PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, "20200101", PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, END_DATE, "-1", PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, END_DATE, PAGE_NUMBER, "-1"),
        Arguments.arguments(VRN, START_DATE, END_DATE, PAGE_NUMBER, "0"),
        Arguments.arguments(VRN, START_DATE, END_DATE, "some text", PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, END_DATE, PAGE_NUMBER, "some text")
    );
  }

  static Stream<Arguments> nullableRequestProperties() {
    return Stream.of(
        Arguments.arguments(VRN, null, END_DATE, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, null, PAGE_NUMBER, PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, END_DATE, null, PAGE_SIZE),
        Arguments.arguments(VRN, START_DATE, END_DATE, PAGE_NUMBER, null)
    );
  }

}