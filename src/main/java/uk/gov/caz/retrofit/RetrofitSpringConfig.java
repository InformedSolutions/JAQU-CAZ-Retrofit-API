package uk.gov.caz.retrofit;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import uk.gov.caz.ApplicationConfiguration;

@ComponentScan(basePackages = {
    "uk.gov.caz.retrofit.configuration",
    "uk.gov.caz.retrofit.controller",
    "uk.gov.caz.retrofit.service",
    "uk.gov.caz.retrofit.amazonaws",
    "uk.gov.caz.retrofit.repository",
})
@Configuration
public class RetrofitSpringConfig implements ApplicationConfiguration {

}
