package com.github.bsundsrud.serializers.processors;

import com.github.bsundsrud.serializers.annotations.WithSerializer;
import com.github.bsundsrud.serializers.util.SerializerException;
import com.github.bsundsrud.serializers.util.SerializerUtils;
import com.github.bsundsrud.serializers.AnnotationSerializer;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Handles fields/methods annotated with {@link WithSerializer} to include the serializer results as a sub-object.
 */
public class ChainedProcessor extends BaseValueProcessor {

    private AnnotationSerializer serializer;

    /**
     * Construct a new instance of a ChainedProcessor.
     *
     * @param serializer serializer instance to use for source field
     * @param targetField field on target object to set the result to
     * @param getter getter on source object
     * @param setter setter on target object.  If null, <code>targetField</code> is assumed to be <code>public</code>
     */
    public ChainedProcessor(AnnotationSerializer serializer, String targetField, Method getter, Method setter) {
        super(targetField, getter, setter);
        this.serializer = serializer;
    }

    /**
     * @return serializer instance that will be used to serialize this sub-object
     */
    public AnnotationSerializer getSerializer() {
        return serializer;
    }

    /**
     * Serialize a field from source to target.  Tries to use a setter if available, falls back to direct field access otherwise
     *
     * @param source instance of the source class
     * @param target instance of the target class
     * @throws SerializerException if calling the getter, serialization of the sub-object, or setting the value fails
     * @see ValueProcessor
     */
    @Override
    public void serialize(Object source, Object target) throws SerializerException {
        Object value = SerializerUtils.invokeGetter(source, valueSource);
        Object serialized = getSerializer().serialize(value);
        if (valueTarget != null) {
            SerializerUtils.setFieldWithSetter(target, valueTarget, serialized);
        } else {
            SerializerUtils.setFieldValue(target, targetField, serialized);
        }
    }

    /**
     * Serialize a field from the source into the result map.
     * The result of the getter is run through the serializer instance and then put into the map as <code>(targetField, serializedValue)</code>
     *
     * @param source instance of the source class
     * @param target instance of the target class (used if methods on the target class need to be called)
     * @param map Map to insert results into
     * @throws SerializerException if calling the getter or serialization of the sub-object fails
     * @see ValueProcessor
     */
    @Override
    public void serializeToMap(Object source, Object target, Map<String, Object> map) throws SerializerException {
        Object value = SerializerUtils.invokeGetter(source, valueSource);

        Object serialized = getSerializer().serializeToMap(value);
        map.put(targetField, serialized);
    }
}
