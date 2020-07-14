package uk.gov.caz.retrofit.controller;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.retrofit.service.TestFixturesLoader;

@Profile("dev | st | integration-tests")
@RestController
@AllArgsConstructor
public class TestFixturesController implements TestFixturesControllerApiSpec {

  public static final String PATH = "/v1/load-test-data";

  private final TestFixturesLoader testDataService;

  @Override
  public ResponseEntity<Void> fixturesPostEndpoint() {
    testDataService.loadTestData();
    return ResponseEntity.noContent().build();
  }
}
