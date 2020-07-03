package uk.gov.caz.retrofit.controller;

import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import uk.gov.caz.retrofit.service.TestFixturesLoader;

@AllArgsConstructor
public class TestDataController implements TestDataControllerApiSpec {

  public static final String PATH = "/load-test-data";

  private TestFixturesLoader testDataService;

  @Override
  public ResponseEntity<Void> testDataPost() {
    try {
      testDataService.loadTestData();
      return ResponseEntity.ok().build();
    } catch (IOException e) {
      throw new RuntimeException((e));
    }
  }
}
