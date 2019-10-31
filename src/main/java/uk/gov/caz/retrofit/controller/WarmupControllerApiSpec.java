package uk.gov.caz.retrofit.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lambda warm up api specification.
 */
public interface WarmupControllerApiSpec {
  /**
   * Warm up the lambda container.
   * 
   * @return a string that contains lambda container Id and (optionally) the time
   *         that the container last serve a request.
   */
  @GetMapping(WarmupController.PATH)
  String warmup() throws JsonProcessingException;
}