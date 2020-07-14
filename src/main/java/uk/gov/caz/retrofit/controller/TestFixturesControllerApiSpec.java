package uk.gov.caz.retrofit.controller;

import static uk.gov.caz.correlationid.Constants.X_CORRELATION_ID_HEADER;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.HttpStatus;

/**
   * Deletes all vehicles from the database and imports the predefined ones from the
   * JSON file.
   */
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;


@RequestMapping(value = TestFixturesController.PATH)
public interface TestFixturesControllerApiSpec {

  /**
   * Imports retrofitted vehicles from a predefined JSON file.
   */
  @ApiOperation(
      value = "${swagger.operations.test-fixture-load.description}"
  )
  @ApiResponses({
      @ApiResponse(code = 500, message = "Internal Server Error / No message available"),
      @ApiResponse(code = 405, message = "Method Not Allowed / Request method 'XXX' not supported"),
      @ApiResponse(code = 400, message = "Missing Correlation Id header"),
      @ApiResponse(code = 204, message = "Successfully imported test fixture data"),
  })
  @ApiImplicitParams({
      @ApiImplicitParam(name = X_CORRELATION_ID_HEADER,
          required = true,
          value = "UUID formatted string to track the request through the enquiries stack",
          paramType = "header"),
  })
  @PostMapping
  @ResponseStatus(value = HttpStatus.CREATED)
  ResponseEntity<Void> fixturesPostEndpoint();

}
