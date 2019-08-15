package uk.gov.caz.taxiregister.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;
import uk.gov.caz.taxiregister.dto.ErrorCode;

@ExtendWith(MockitoExtension.class)
class RegistrationErrorAttributesTest {

  @Mock
  private WebRequest webRequest;
  @Mock
  private HttpServletRequest httpServletRequest;
  @Mock
  private HttpServletResponse httpServletResponse;
  @Mock
  private DefaultErrorAttributes defaultErrorAttributes;

  @InjectMocks
  private RegistrationErrorAttributes registrationErrorAttributes;

  static Stream<Arguments> statusCodeAndCustomCodeProvider() {
    return Stream.of(
        Arguments.arguments(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.CAZ00099),
        Arguments.arguments(HttpStatus.BAD_REQUEST.value(), ErrorCode.CAZ00020),
        Arguments.arguments(HttpStatus.UNAUTHORIZED.value(), ErrorCode.CAZ00060),
        Arguments.arguments(HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCode.CAZ00070),
        Arguments.arguments(HttpStatus.UNPROCESSABLE_ENTITY.value(), ErrorCode.CAZ00080),
        Arguments.arguments(HttpStatus.TOO_MANY_REQUESTS.value(), ErrorCode.CAZ00090)
    );
  }

  @ParameterizedTest
  @MethodSource("statusCodeAndCustomCodeProvider")
  public void shouldEmbellishErrorAttributesWithCustomCode(int statusCode, ErrorCode errorCode) {
    when(defaultErrorAttributes.getErrorAttributes(webRequest, false))
        .thenReturn(Maps.newHashMap(Collections.singletonMap("status", statusCode)));

    Map<String, Object> errorAttributes = registrationErrorAttributes
        .getErrorAttributes(webRequest, false);

    assertThat(errorAttributes).containsEntry("code", errorCode);
  }

  @Test
  public void shouldReturnUnknownErrorCodeForNotSupportedStatusCode() {
    int notSupportedStatusCode = 522;
    when(defaultErrorAttributes.getErrorAttributes(webRequest, false))
        .thenReturn(Maps.newHashMap(Collections.singletonMap("status", notSupportedStatusCode)));

    Map<String, Object> errorAttributes = registrationErrorAttributes
        .getErrorAttributes(webRequest, false);

    assertThat(errorAttributes).containsEntry("code", ErrorCode.UNKNOWN);
  }

  @Test
  public void shouldReturnUnknownErrorCodeForAbsentStatus() {
    when(defaultErrorAttributes.getErrorAttributes(webRequest, false))
        .thenReturn(Maps.newHashMap());

    Map<String, Object> errorAttributes = registrationErrorAttributes
        .getErrorAttributes(webRequest, false);

    assertThat(errorAttributes).containsEntry("code", ErrorCode.UNKNOWN);
  }

  @Test
  public void shouldReturnUnknownErrorCodeForNonNumericalStatus() {
    when(defaultErrorAttributes.getErrorAttributes(webRequest, false))
        .thenReturn(Maps.newHashMap(Collections.singletonMap("status", new Object())));

    Map<String, Object> errorAttributes = registrationErrorAttributes
        .getErrorAttributes(webRequest, false);

    assertThat(errorAttributes).containsEntry("code", ErrorCode.UNKNOWN);
  }

  @Test
  public void shouldDelegateCallToGetError() {
    registrationErrorAttributes.getError(webRequest);

    verify(defaultErrorAttributes).getError(webRequest);
  }

  @Test
  public void shouldDelegateCallToResolveException() {
    Object handler = new Object();
    Exception e = new Exception();

    registrationErrorAttributes
        .resolveException(httpServletRequest, httpServletResponse, handler, e);

    verify(defaultErrorAttributes)
        .resolveException(httpServletRequest, httpServletResponse, handler, e);
  }

  @Test
  public void shouldHaveHighestPrecedence() {
    assertThat(registrationErrorAttributes.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
  }
}