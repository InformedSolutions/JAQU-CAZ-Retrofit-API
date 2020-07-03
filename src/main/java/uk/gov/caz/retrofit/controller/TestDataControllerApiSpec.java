package uk.gov.caz.retrofit.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@RequestMapping(value = TestDataController.PATH)
public interface TestDataControllerApiSpec {

  @PostMapping
  @ResponseStatus(value = HttpStatus.CREATED)
  ResponseEntity<Void> testDataPost();

}
