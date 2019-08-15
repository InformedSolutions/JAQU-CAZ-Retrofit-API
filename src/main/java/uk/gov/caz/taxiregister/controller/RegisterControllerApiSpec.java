package uk.gov.caz.taxiregister.controller;

import static uk.gov.caz.taxiregister.controller.Constants.API_KEY_HEADER;
import static uk.gov.caz.taxiregister.controller.Constants.CORRELATION_ID_HEADER;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.caz.taxiregister.configuration.SwaggerConfiguration;
import uk.gov.caz.taxiregister.dto.Vehicles;

@Api(value = RegisterController.PATH, tags = {SwaggerConfiguration.TAG_REGISTER_CONTROLLER})
@RequestMapping(
    value = RegisterController.PATH,
    produces = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE},
    consumes = {MediaType.APPLICATION_JSON_UTF8_VALUE, MediaType.APPLICATION_XML_VALUE}
)
public interface RegisterControllerApiSpec {

  /**
   * Register new Vehicles.
   *
   * @param vehicles List of POJOs with vehicle data.
   * @return List of POJOs with created vehicles data including status.
   */
  @ApiOperation(
      value = "${swagger.operations.register.description}",
      response = Vehicles.class
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 404, message = "Not Found / No message available"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 400, message = "Bad Request"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = "timestamp",
          required = true,
          value =
              "ISO 8601 formatted datetime string indicating time that the request was initialised",
          paramType = "header"),
      @ApiImplicitParam(name = CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
      @ApiImplicitParam(name = "x-api-key",
          required = true, value = "API key used to access the service",
          paramType = "header"),
      @ApiImplicitParam(name = "Authorization",
          required = true,
          value = "TBD",
          paramType = "header")
  })
  @PostMapping("taxiphvdatabase")
  @ResponseStatus(value = HttpStatus.CREATED)
  ResponseEntity<?> register(@RequestBody Vehicles vehicles,
      @RequestHeader(CORRELATION_ID_HEADER) String correlationId, 
      @RequestHeader(API_KEY_HEADER) String apiKey);
}