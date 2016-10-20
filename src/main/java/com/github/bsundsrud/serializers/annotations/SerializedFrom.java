package com.github.bsundsrud.serializers.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a serializer for a given Class.  Valid at type-level only.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SerializedFrom {
    /**
     * Class object to serialize from
     *
     * @return Class object
     */
    Class<?> value();
}
