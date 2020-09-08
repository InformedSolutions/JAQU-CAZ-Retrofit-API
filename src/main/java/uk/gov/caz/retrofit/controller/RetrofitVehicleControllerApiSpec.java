package uk.gov.caz.retrofit.controller;

import static uk.gov.caz.retrofit.controller.Constants.CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.retrofit.dto.RetrofitStatusResponse;

@RequestMapping(
    value = RetrofitVehicleController.BASE_PATH,
    produces = MediaType.APPLICATION_JSON_VALUE
)
public interface RetrofitVehicleControllerApiSpec {

  /**
   * Endpoint that fetches retrofit status for vehicle with given VRN.
   */
  @ApiOperation(
      value = "Fetches retrofit status",
      response = RetrofitStatusResponse.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 400, message = "Correlation Id missing"),
      @ApiResponse(code = 404, message = "Vehicle for given VRN doesn't exist"),
      @ApiResponse(code = 202, message = "Vehicle found, status in the response"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = CORRELATION_ID_HEADER,
          required = true,
          value = "CorrelationID to track the request from the API gateway through"
              + " the Enquiries stack",
          paramType = "header")
  })
  @GetMapping(
      produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE},
      value = "/{vrn}"
  )
  @ResponseStatus(value = HttpStatus.CREATED)
  ResponseEntity<RetrofitStatusResponse> fetchRetrofitStatus(
      @RequestHeader(CORRELATION_ID_HEADER) String correlationId,
      @PathVariable String vrn);
}
