package uk.gov.caz.taxiregister.configuration;

import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.caz.taxiregister.error.RegistrationErrorAttributes;

@Configuration
public class ErrorAttributesConfiguration {

  @Bean
  public RegistrationErrorAttributes registrationErrorAttributes() {
    DefaultErrorAttributes defaultErrorAttributes = new DefaultErrorAttributes(false);
    return new RegistrationErrorAttributes(defaultErrorAttributes);
  }

  @Bean
  public ErrorAttributes errorAttributes(RegistrationErrorAttributes registrationErrorAttributes) {
    return registrationErrorAttributes;
  }
}
