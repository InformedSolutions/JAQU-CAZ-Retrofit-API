package uk.gov.caz.retrofit.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "swagger.api")
@Data
public class SwaggerApiInfo {

  String title;
  String description;
  String version;
}
