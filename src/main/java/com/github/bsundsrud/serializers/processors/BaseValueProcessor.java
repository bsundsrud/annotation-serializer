package com.github.bsundsrud.serializers.processors;

import com.github.bsundsrud.serializers.util.SerializerException;
import com.github.bsundsrud.serializers.util.SerializerUtils;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Serializes a field.  If present, a setter will be used to set the resulting value in {@link #serialize(Object, Object)}.
 */
public class BaseValueProcessor implements ValueProcessor {

    protected String targetField;
    protected Method valueSource;
    protected Method valueTarget;

    /**
     * Set up a mapping from a getter on the source object to a field on the target object, set via a setter
     *
     * @param targetField Field name on target object
     * @param valueSource getter Method on source object
     * @param valueTarget setter Method on target object.  If null, <code>targetField</code> will be assumed to be <code>public</code>.
     */
    public BaseValueProcessor(String targetField, Method valueSource, Method valueTarget) {
        this.targetField = targetField;
        this.valueSource = valueSource;
        this.valueTarget = valueTarget;
    }

    /**
     * Convenience constructor for {@link #BaseValueProcessor(String, Method, Method)} when constructing a serializer for a public field
     *
     * @param targetField Public field name on target object
     * @param valueSource getter Method on source object
     */
    public BaseValueProcessor(String targetField, Method valueSource) {
        this(targetField, valueSource, null);
    }

    /**
     * Serialize a field from source to target.  Tries to use a setter if available, falls back to direct field access otherwise
     *
     * @param source instance of the source class
     * @param target instance of the target class
     * @throws SerializerException if calling the getter or setting the value fails
     * @see ValueProcessor
     */
    @Override
    public void serialize(Object source, Object target) throws SerializerException {
        Object value = SerializerUtils.invokeGetter(source, valueSource);
        if (valueTarget != null) {
            SerializerUtils.setFieldWithSetter(target, valueTarget, value);
        } else {
            SerializerUtils.setFieldValue(target, targetField, value);
        }
    }

    /**
     * Serialize a field from the source into the result map.
     * The result of the getter is put into the map as <code>(targetField, value)</code>
     *
     * @param source instance of the source class
     * @param target instance of the target class (used if methods on the target class need to be called)
     * @param map Map to insert results into
     * @throws SerializerException if calling the getter fails
     * @see ValueProcessor
     */
    @Override
    public void serializeToMap(Object source, Object target, Map<String, Object> map) throws SerializerException {
        Object value = SerializerUtils.invokeGetter(source, valueSource);
        map.put(targetField, value);
    }
}
