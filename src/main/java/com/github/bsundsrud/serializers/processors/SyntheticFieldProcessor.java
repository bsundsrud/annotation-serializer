package com.github.bsundsrud.serializers.processors;

import com.github.bsundsrud.serializers.annotations.Synthesized;
import com.github.bsundsrud.serializers.util.SerializerException;
import com.github.bsundsrud.serializers.util.SerializerUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Serializes methods annotated with {@link Synthesized}
 */
public class SyntheticFieldProcessor extends BaseValueProcessor implements ValueProcessor {
    private List<Method> inputFieldGetters;
    private Method combinator;

    /**
     * Construct a new instance of this serializer
     *
     * @param targetField field on target object to set the result to
     * @param combinator method used to compute result value
     * @param setter setter on target object.  If null, <code>targetField</code> is assumed to be <code>public</code>
     * @param inputGetters List of {@link java.lang.reflect.Method}s used to retrieve arguments to pass to the <code>combinator</code>
     */
    public SyntheticFieldProcessor(String targetField, Method combinator, Method setter, List<Method> inputGetters) {
        super(targetField, null, setter);
        this.combinator = combinator;
        this.inputFieldGetters = inputGetters;
    }

    private Object[] getArgumentValues(Object source) throws SerializerException {
        Object[] args = new Object[inputFieldGetters.size()];
        for (int i = 0; i < inputFieldGetters.size(); i++) {
            Method m = inputFieldGetters.get(i);
            try {
                args[i] = m.invoke(source);
            } catch (IllegalAccessException e) {
                throw new SerializerException("Could not access '" + m.getName()
                        + "' on type '" + source.getClass().getName() + "'", e);
            } catch (InvocationTargetException e) {
                throw new SerializerException("Could not invoke '" + m.getName()
                        + "' on type '" + source.getClass().getName() + "'", e);
            }
        }
        return args;
    }

    /**
     * Calls all passed getters to construct an argument list, which is then passed to the <code>combinator</code> and invoked.
     * The <code>targetField</code> is set to the result via setter or public field access.
     *
     * @param source instance of the source class
     * @param target instance of the target class
     * @throws SerializerException if calling the combinator or setting the value fails
     * @see ValueProcessor
     */
    @Override
    public void serialize(Object source, Object target) throws SerializerException {
        Object[] args = getArgumentValues(source);
        Object result;
        try {
            result = combinator.invoke(target, args);

        } catch (IllegalAccessException e) {
            throw new SerializerException("Could not access '" + combinator.getName()
                    + "' on type '" + target.getClass().getName() + "'", e);
        } catch (InvocationTargetException e) {
            throw new SerializerException("Could not invoke '" + combinator.getName()
                    + "' on type '" + target.getClass().getName() + "'", e);
        }
        if (valueTarget != null) {
            SerializerUtils.setFieldWithSetter(target, valueTarget, result);
        } else {
            SerializerUtils.setFieldValue(target, targetField, result);
        }
    }

    /**
     * Calls all passed getters to construct an argument list, which is then passed to the <code>combinator</code> and invoked.
     * The result is put into the map as <code>(targetField, result)</code>
     *
     * @param source instance of the source class
     * @param target instance of the target class (used if methods on the target class need to be called)
     * @param map Map to insert results into
     * @throws SerializerException if calling the combinator fails
     * @see ValueProcessor
     */
    @Override
    public void serializeToMap(Object source, Object target, Map<String, Object> map) throws SerializerException {
        Object[] args = getArgumentValues(source);
        try {
            Object result = combinator.invoke(target, args);
            map.put(targetField, result);
        } catch (IllegalAccessException e) {
            throw new SerializerException("Could not access '" + combinator.getName()
                    + "' on type '" + target.getClass().getName() + "'", e);
        } catch (InvocationTargetException e) {
            throw new SerializerException("Could not invoke '" + combinator.getName()
                    + "' on type '" + target.getClass().getName() + "'", e);
        }
    }
}
