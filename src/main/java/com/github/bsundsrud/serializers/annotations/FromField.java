package com.github.bsundsrud.serializers.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Sets up a field mapping between the field name specified in the parameter and the annotated field or method.  Valid on public fields or setter methods.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FromField {
    /**
     * Specify the field to map from.  Valid on public fields and methods.
     *
     * @return String representing the field name.  Corresponding field on the source object must have a getter named <code>get&lt;fieldName&gt;()</code>
     */
    String value();
}
