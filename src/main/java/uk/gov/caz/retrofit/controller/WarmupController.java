package uk.gov.caz.retrofit.controller;

import org.springframework.web.bind.annotation.RestController;
import uk.gov.caz.retrofit.dto.LambdaContainerStats;

@RestController
public class WarmupController implements WarmupControllerApiSpec {

  public static final String PATH = "/v1/warmup";

  @Override
  public String warmup() {
    return LambdaContainerStats.getStats();
  }
}