package uk.gov.caz.taxiregister;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = {
    "uk.gov.caz.taxiregister.configuration",
    "uk.gov.caz.taxiregister.controller",
    "uk.gov.caz.taxiregister.service",
    "uk.gov.caz.taxiregister.amazonaws",
    "uk.gov.caz.taxiregister.repository",
})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
