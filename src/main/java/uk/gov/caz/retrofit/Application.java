package uk.gov.caz.retrofit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication(scanBasePackages = {
    "uk.gov.caz.retrofit.configuration",
    "uk.gov.caz.retrofit.controller",
    "uk.gov.caz.retrofit.service",
    "uk.gov.caz.retrofit.amazonaws",
    "uk.gov.caz.retrofit.repository",
})
public class Application extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
