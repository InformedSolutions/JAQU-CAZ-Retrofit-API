package uk.gov.caz.retrofit.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.caz.retrofit.dto.RetrofitInfoHistoricalRequest;
import uk.gov.caz.retrofit.dto.RetrofitInfoHistoricalResponse;

@RequestMapping(
    value = HistoricalInfoController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface HistoricalInfoControllerApiSpec {
  /**
   * Looks up vehicle's information about its historical data for given VRN inside provided date
   * range.
   *
   * @return {@link RetrofitInfoHistoricalRequest} wrapped in {@link ResponseEntity}.
   */
  @ApiOperation(
      value = "${swagger.operations.retrofit-info-historical.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 404, message = "Vehicle not found"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 200, message = "Historical data for given VRN inside provided date range")
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header")
  })
  @GetMapping
  ResponseEntity<RetrofitInfoHistoricalResponse> search(@PathVariable String vrn,
      RetrofitInfoHistoricalRequest request);
}
