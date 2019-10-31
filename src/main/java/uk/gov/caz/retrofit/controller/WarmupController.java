package uk.gov.caz.retrofit.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.retrofit.dto.LambdaContainerStats;

@RestController
public class WarmupController implements WarmupControllerApiSpec {

  public static final String PATH = "/v1/retrofit/warmup";

  @Override
  public String warmup() throws JsonProcessingException {
    return LambdaContainerStats.getStats();
  }

}
