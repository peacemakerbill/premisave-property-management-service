package com.premisave.property.health;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs before every controller method. If the method (or its controller) is annotated with
 * {@link RequiresServices}, the listed services are checked first, and an offline one produces
 * the friendly 503 from GlobalExceptionHandler. It runs after Spring Security, so anonymous
 * callers still get their 401 and never learn which services are up.
 */
@Component
@RequiredArgsConstructor
public class ServiceAvailabilityInterceptor implements HandlerInterceptor {

    private final ServiceHealthMonitor healthMonitor;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequiresServices required = handlerMethod.getMethodAnnotation(RequiresServices.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequiresServices.class);
        }
        if (required == null) {
            return true;
        }

        healthMonitor.requireOnline(required.action(), required.reassurance(), required.value());
        return true;
    }
}