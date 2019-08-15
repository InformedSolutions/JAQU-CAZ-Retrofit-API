package uk.gov.caz.taxiregister.error;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.caz.taxiregister.dto.ErrorCode;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class RegistrationErrorAttributes implements ErrorAttributes, HandlerExceptionResolver,
    Ordered {

  private static final Map<Integer, ErrorCode> STATUS_TO_ERROR_CODE =
      ImmutableMap.<Integer, ErrorCode>builder()
      .put(HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.CAZ00099)
      .put(HttpStatus.BAD_REQUEST.value(), ErrorCode.CAZ00020)
      .put(HttpStatus.UNAUTHORIZED.value(), ErrorCode.CAZ00060)
      .put(HttpStatus.METHOD_NOT_ALLOWED.value(), ErrorCode.CAZ00070)
      .put(HttpStatus.UNPROCESSABLE_ENTITY.value(), ErrorCode.CAZ00080)
      .put(HttpStatus.TOO_MANY_REQUESTS.value(), ErrorCode.CAZ00090)
      .build();

  private final DefaultErrorAttributes delegate;

  public RegistrationErrorAttributes(DefaultErrorAttributes delegate) {
    this.delegate = delegate;
  }

  @Override
  public Map<String, Object> getErrorAttributes(WebRequest webRequest, boolean includeStackTrace) {
    Map<String, Object> errorAttributes = delegate.getErrorAttributes(webRequest, false);
    errorAttributes.put("code",
        STATUS_TO_ERROR_CODE.getOrDefault(extractStatusCode(errorAttributes), ErrorCode.UNKNOWN));
    return errorAttributes;
  }

  private Integer extractStatusCode(Map<String, Object> errorAttributes) {
    Object status = errorAttributes.getOrDefault("status", -1);
    return status instanceof Integer ? (Integer) status : -1;
  }

  @Override
  public Throwable getError(WebRequest webRequest) {
    return delegate.getError(webRequest);
  }

  @Override
  public ModelAndView resolveException(HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse, Object handler, Exception e) {
    return delegate.resolveException(httpServletRequest, httpServletResponse, handler, e);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE;
  }
}
