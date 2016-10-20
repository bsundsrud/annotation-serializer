package com.github.bsundsrud.serializers;

import com.github.bsundsrud.serializers.annotations.FromField;
import com.github.bsundsrud.serializers.annotations.SerializedFrom;
import com.github.bsundsrud.serializers.annotations.Synthesized;
import com.github.bsundsrud.serializers.annotations.WithSerializer;
import com.github.bsundsrud.serializers.processors.BaseValueProcessor;
import com.github.bsundsrud.serializers.processors.ChainedProcessor;
import com.github.bsundsrud.serializers.processors.SyntheticFieldProcessor;
import com.github.bsundsrud.serializers.processors.ValueProcessor;
import com.github.bsundsrud.serializers.util.SerializerException;
import com.github.bsundsrud.serializers.util.SerializerUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Processes annotation-based serializations and translate instances of a source object into a serializable representation.
 *
 * Example Usage:
 *
 * <pre>
 *{@literal @}SerializedFrom(TestSource.class)
 * public class TestSerializer {
 *     //public fields without annotation are retrieved from matching fields in the source using 'get&lt;Field&gt;()' methods
 *     public String docId;
 *     public String sender;
 *     public String receiver;
 *     public String sendAndReceive;
 *     public Date date;
 *
 *     // if field names don't match, make the mapping explicit with @FromField
 *    {@literal @}FromField("sender")
 *     public String mySender;
 *    
 *     // Sub-serializers can be used for nested objects (Sub-serializers are anything tagged with @SerializedFrom)
 *     // 'fields' is an array of fields to include from the sub-serializer
 *     // @FromField works on methods as well
 *    {@literal @}FromField("sub")
 *    {@literal @}WithSerializer(value = TestSubSerializer.class, fields = "baz")
 *     public TestSubSerializer subthing;
 *    
 *     //Private members are set using 'set&lt;Field&gt;()' methods
 *     private TestSubSerializer subthing2;
 *    
 *     //@Synthesized fields take one or more source fields, do some processing, and then return a value to be assigned to 'target'
 *    {@literal @}Synthesized(target = "date", from = "timestampMillis")
 *     public Date setDate(long millis) {
 *         return new Date(millis);
 *     }
 *    
 *    {@literal @}Synthesized(target = "sendAndReceive", from = {"sender", "receiver"})
 *     public String setSendAndReceive(String sender, String receiver) {
 *         return sender + ":" + receiver;
 *     }
 *    
 *     // setter for 'subthing2' above.  includes all fields
 *     // if the serializer can be inferred from the types, the serializer class can be omitted.
 *    {@literal @}FromField("sub")
 *    {@literal @}WithSerializer
 *     public void setSubthing2(TestSubSerializer sub) {
 *         this.subthing2 = sub;
 *     }
 * }
 * </pre>
 *
 * Invocation of the above looks like this:
 * <pre>
 * TestSource t = new TestSource();
 * AnnotationSerializer&lt;TestSerializer&gt; sap = AnnotationSerializer.serializerForClass(TestSerializer.class);
 * TestSerializer out = sap.serialize(src);
 * Map&lt;String, Object&gt; outMap = sap.serializeToMap(src);
 *
 * </pre>
 *
 * GSON-ified object is this: (int fields get defaulted to 0, using Integer would solve this)
 *
 * <pre>
 * {
 *     "docId": "1",
 *     "sender": "3",
 *     "receiver": "2",
 *     "sendAndReceive": "3:2",
 *     "date": "Feb 1, 2016 7:58:40 AM",
 *     "mySender": "3",
 *     "subthing": {
 *         "foo": 0,
 *         "baz": "why"
 *     },
 *     "subthing2": {
 *         "foo": 1,
 *         "bar": "what",
 *         "baz": "why"
 *     }
 * }
 * </pre>
 *
 * GSON-ified Map looks like this (more correct):
 * <pre>
 * {
 *     "date": "Feb 1, 2016 7:58:40 AM",
 *     "docId": "1",
 *     "mySender": "3",
 *     "receiver": "2",
 *     "sendAndReceive": "3:2",
 *     "sender": "3",
 *     "subthing": {
 *         "baz": "why"
 *     },
 *     "subthing2": {
 *         "bar": "what",
 *         "baz": "why",
 *         "foo": 1
 *     }
 * }
 * </pre>
 *
 * @param <T> The type of the serializer class
 */
public class AnnotationSerializer<T> {
    private Map<String, ValueProcessor> valueSerializerMap = new HashMap<String, ValueProcessor>();
    private List<String> includedFields = new ArrayList<String>();
    private Class<T> resultClass;

    private AnnotationSerializer() {
    }

    private static Class<?> serializedFrom(Class<?> resultClass) throws SerializerException {
        if (!resultClass.isAnnotationPresent(SerializedFrom.class)) {
            throw new SerializerException("Serializer '" + resultClass.getName() + "' not annotated with @SerializedFrom");
        }
        SerializedFrom from = resultClass.getAnnotation(SerializedFrom.class);
        return from.value();
    }

    /**
     * Create and initialize a new AnnotationSerializer instance
     *
     * @param target Class of serialization target
     * @param includedFields varargs list of fields to include in serialization.  If specified, only those named fields will be set on the resulting object.
     * @param <T> The type of the serializer class
     * @return a fully initialized AnnotationSerializer, ready to serialize instances of source objects
     * @throws SerializerException on failure to initialize mapping for given serializer class
     */
    public static <T> AnnotationSerializer<T> serializerForClass(Class<T> target, String... includedFields) throws SerializerException {
        AnnotationSerializer<T> sap = new AnnotationSerializer<T>();
        sap.init(target, Arrays.asList(includedFields));
        return sap;
    }

    private void scanMethods(Class<?> srcClass, Class<T> resultClass) throws SerializerException {
        for (Method m : resultClass.getDeclaredMethods()) {
            // Assume field on source object matches field on target object
            String srcFieldName = SerializerUtils.methodToField(m.getName());
            String tgtFieldName = SerializerUtils.methodToField(m.getName());
            // Check for presence of field mapping
            if (m.isAnnotationPresent(FromField.class)) {
                FromField mapping = m.getAnnotation(FromField.class);
                srcFieldName = mapping.value();
            }
            // Is this a computed field?
            if (m.isAnnotationPresent(Synthesized.class)) {
                Synthesized f = m.getAnnotation(Synthesized.class);
                String targetField = f.target();
                List<Method> getters = SerializerUtils.gettersForFields(srcClass, f.from());
                if (getters.size() != m.getParameterCount()) {
                    throw new SerializerException("Parameter count mismatch for synthesizer method '" + m.getName()
                            + "'. Expected " + getters.size() + " but found " + m.getParameterCount());
                }
                // Check types on all the field getters to make sure they match the method's arguments and order
                for (int i = 0; i < getters.size(); i++) {
                    Method getter = getters.get(i);
                    if (getter.getReturnType() != m.getParameterTypes()[i]) {
                        throw new SerializerException("Type mismatch for parameter " + i
                                + " on method '" + m.getName()
                                + "': Method expected '" + m.getParameterTypes()[i]
                                + "but field is of type '" + getter.getReturnType() + "'");
                    }
                }

                valueSerializerMap.put(targetField, new SyntheticFieldProcessor(targetField, m, SerializerUtils.setterForField(resultClass, targetField), getters));
            } else if (m.getName().startsWith("set") && m.getParameterCount() == 1) { // is this a setter method?  Setters are assumed to start with "set" and take only 1 parameter
                if (m.isAnnotationPresent(WithSerializer.class)) { // is this a sub-serializer?
                    WithSerializer ws = m.getAnnotation(WithSerializer.class);
                    Class otherSerializer = ws.value();
                    Class<?> firstParamType = m.getParameterTypes()[0];
                    // if the type is omitted, default to the parameter's type
                    if (otherSerializer.equals(void.class)) {
                        otherSerializer = firstParamType;
                    } else {
                        // type-check params vs annotation parameter
                        if (otherSerializer != firstParamType) {
                            throw new SerializerException("Type of serializer '" + otherSerializer.getName()
                                    + "' does not match parameter type '" + firstParamType.getName()
                                    + "' for setter '" + m.getName() + "'");
                        }
                    }

                    AnnotationSerializer sap = serializerForClass(otherSerializer, ws.fields());
                    valueSerializerMap.put(tgtFieldName, new ChainedProcessor(sap, tgtFieldName, SerializerUtils.findGetterForFieldName(srcClass, srcFieldName), m));
                } else { // standard setter method
                    Method getter = SerializerUtils.findGetterForSetter(srcClass, srcFieldName, m);
                    valueSerializerMap.put(tgtFieldName, new BaseValueProcessor(tgtFieldName, getter, m));
                }
            }
        }
    }

    private void scanFields(Class<?> srcClass, Class<T> resultClass) throws SerializerException {
        for (Field f : resultClass.getDeclaredFields()) {
            int modifiers = f.getModifiers();
            // Only consider public non-transient fields that don't already have a mapping from the methods
            if (Modifier.isPublic(modifiers) && !Modifier.isTransient(modifiers) && !valueSerializerMap.containsKey(f.getName())) {
                String sourceFieldName;
                // If a field mapping is present, set source field appropriately
                if (f.isAnnotationPresent(FromField.class)) {
                    FromField mapping = f.getAnnotation(FromField.class);
                    sourceFieldName = mapping.value();
                } else {
                    sourceFieldName = f.getName();
                }
                // Is this field a sub-serializer?
                if (f.isAnnotationPresent(WithSerializer.class)) {
                    WithSerializer ws = f.getAnnotation(WithSerializer.class);
                    Class<?> otherSerializer = ws.value();

                    // If the type is omitted, default to the field's type
                    if (otherSerializer.equals(void.class)) {
                        otherSerializer = f.getType();
                    } else {
                        // type-check field vs annotation value
                        if (otherSerializer != f.getType()) {
                            throw new SerializerException("Type of serializer '" + otherSerializer.getName()
                                    + "' does not match field type '" + f.getType().getName()
                                    + "' for field '" + f.getName() + "'");
                        }
                    }

                    AnnotationSerializer sap = serializerForClass(otherSerializer, ws.fields());
                    Method getter = SerializerUtils.findGetterForFieldName(srcClass, sourceFieldName);
                    valueSerializerMap.put(f.getName(), new ChainedProcessor(sap, f.getName(), getter, null));
                } else { //standard field
                    BaseValueProcessor bvs = new BaseValueProcessor(f.getName(), SerializerUtils.findGetterForFieldName(srcClass, sourceFieldName));
                    valueSerializerMap.put(f.getName(), bvs);
                }
            }
        }
    }

    private T newInstance() throws SerializerException {
        try {
            return resultClass.newInstance();
        } catch (InstantiationException e) {
            throw new SerializerException("Could not instantiate instance of type '" + resultClass.getName() + "'", e);
        } catch (IllegalAccessException e) {
            throw new SerializerException("Could not access constructor of type '" + resultClass.getName() + "'", e);
        }
    }

    private void assertCanSerializeFrom(Class<?> srcClass) throws SerializerException {
        // assert that the serialization source of resultClass is the same as srcClass
        Class<?> target = serializedFrom(resultClass);
        if (!srcClass.equals(target)) {
            throw new SerializerException("Source Object does not match Serializer target: " + srcClass.getName() + " vs " + target.getName());
        }
    }

    private void init(Class<T> resultClass, List<String> includedFields) throws SerializerException {
        this.resultClass = resultClass;
        this.includedFields = includedFields;
        Class<?> srcClass = serializedFrom(resultClass);

        scanMethods(srcClass, resultClass);
        scanFields(srcClass, resultClass);
    }

    /**
     * Convenience method for {@link #serialize(Object, List)}
     *
     * @param source Source object to serialize
     * @param includedFields varargs list of fields to include in the result
     * @return an instance of the type parameter T
     * @throws SerializerException on failures in mapping from source to T
     */
    public T serialize(Object source, String... includedFields) throws SerializerException {
        return serialize(source, Arrays.asList(includedFields));
    }

    /**
     * Serialize a given source object to an instance of type T
     *
     * @param source Source object to serialize
     * @param includedFields list of fields to include in the result
     * @return an instance of the type parameter T
     * @throws SerializerException on failures in mapping from source to T
     */
    public T serialize(Object source, List<String> includedFields) throws SerializerException {
        if (source == null) {
            return null;
        }

        assertCanSerializeFrom(source.getClass());

        T resultObj = newInstance();

        if (includedFields.size() == 0) {
            includedFields = this.includedFields;
        }

        for (String field : valueSerializerMap.keySet()) {
            if (includedFields.size() > 0 && !includedFields.contains(field)) {
                continue;
            }
            ValueProcessor vs = valueSerializerMap.get(field);
            vs.serialize(source, resultObj);

        }

        return resultObj;
    }

    /**
     * Convenience method for {@link #serializeToMap(Object, List)}
     *
     * @param source source object to serialize
     * @param includedFields varargs list of fields to include in result map
     * @return {@link java.util.Map} of the serialization result
     * @throws SerializerException on failures in mapping from source to type T
     */
    public Map<String, Object> serializeToMap(Object source, String... includedFields) throws SerializerException {
        return serializeToMap(source, Arrays.asList(includedFields));
    }

    /**
     * Serialize a source object to a {@link java.util.Map} of &lt;String, Object&gt;
     *
     * @param source source object to serialize
     * @param includedFields list of fields to include in result map
     * @return {@link java.util.Map} of the serialization result
     * @throws SerializerException on failures in mapping from source to type T
     */
    public Map<String, Object> serializeToMap(Object source, List<String> includedFields) throws SerializerException {
        if (source == null) {
            return new HashMap<String, Object>();
        }

        assertCanSerializeFrom(source.getClass());

        T resultObj = newInstance();

        if (includedFields.size() == 0) {
            includedFields = this.includedFields;
        }

        Map<String, Object> results = new TreeMap<String, Object>();
        for (String field : valueSerializerMap.keySet()) {
            if (includedFields.size() > 0 && !includedFields.contains(field)) {
                continue;
            }
            ValueProcessor vs = valueSerializerMap.get(field);
            vs.serializeToMap(source, resultObj, results);
        }
        return results;
    }

}
