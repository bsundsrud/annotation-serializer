package com.github.bsundsrud.serializers.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as receiving one or more source field values for some amount of processing and then setting the result to a field.  Valid on methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Synthesized {
    /**
     * Field to assign the result of the method to.  Return type must be assignable to the field type.
     *
     * @return String of target field name
     */
    String target();

    /**
     * An array of zero or more values to receive from the source object.  Values are retrieved using <code>get&lt;fieldName&gt;()</code> calls on the source object
     *
     * @return a String[] of field names
     */
    String[] from() default {};
}
