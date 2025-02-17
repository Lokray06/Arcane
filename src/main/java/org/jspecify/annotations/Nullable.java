package main.java.org.jspecify.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A stub annotation for @Nullable.
 * This is provided only to satisfy compilation and can be removed if you add the actual dependency.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
public @interface Nullable {
}
