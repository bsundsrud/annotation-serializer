package com.github.bsundsrud.serializers.processors;


import com.github.bsundsrud.serializers.util.SerializerException;

import java.util.Map;

/**
 * Base interface for serializing a field, method, or annotation type.
 */
public interface ValueProcessor {
    /**
     * Serialize to an instance of the serializer class
     *
     * @param source instance of the source class
     * @param target instance of the target class
     * @throws SerializerException on any error in serialization
     */
    void serialize(Object source, Object target) throws SerializerException;

    /**
     * Serialize to a Map&lt;String, Object&gt; given an instance of the source, target, and the result map.
     *
     * @param source instance of the source class
     * @param target instance of the target class (used if methods on the target class need to be called)
     * @param map Map to insert results into
     * @throws SerializerException on any error in serialization
     */
    void serializeToMap(Object source, Object target, Map<String, Object> map) throws SerializerException;
}
