package uk.gov.caz.retrofit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTest
@AutoConfigureMockMvc
public @interface MockedMvcIntegrationTest {

}
