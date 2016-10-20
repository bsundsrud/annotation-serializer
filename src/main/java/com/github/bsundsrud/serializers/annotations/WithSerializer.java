package com.github.bsundsrud.serializers.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates a field or method to use the given serializer to serialize this field.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WithSerializer {
    /**
     * Class to use as a serializer.  If omitted, it is inferred from the field type or return type of the annotation target.
     *
     * @return Class of serializer
     */
    Class<?> value() default void.class;

    /**
     * Fields to include in result.
     * If no fields are specified, all fields will be included.
     * If any fields are specified, only those will be included.
     *
     * @return String[] of included field names
     */
    String[] fields() default {};
}
