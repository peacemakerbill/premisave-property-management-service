package com.premisave.property.health;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method (or a whole controller) that cannot work unless the given
 * services are online. Before the method runs, ServiceAvailabilityInterceptor asks
 * ServiceHealthMonitor and, if one is unreachable, answers 503 with a user-friendly
 * "service is offline" message — before any validation, database work or money movement.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresServices {

    ExternalService[] value();

    /** What the user was doing, phrased to follow "We can't ...", e.g. "process payments". Blank => "complete this request". */
    String action() default "";

    /** Optional sentence after "...is offline.", e.g. "Nothing has been charged." */
    String reassurance() default "";
}