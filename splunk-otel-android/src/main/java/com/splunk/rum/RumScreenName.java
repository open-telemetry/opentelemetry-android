package com.splunk.rum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to customize the {@code screen.name} attribute
 * for an instrumented Fragment or Activity.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RumScreenName {
    /**
     * @return The customized screen name
     */
    String value();
}
