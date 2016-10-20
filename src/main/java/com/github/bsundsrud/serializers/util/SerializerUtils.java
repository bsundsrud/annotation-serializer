package com.github.bsundsrud.serializers.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public class SerializerUtils {

    /**
     * Get a list of getters for the varargs list of fields on source object
     *
     * @param source source class to find the getters on
     * @param fields fields to retrieve getters for
     * @return {@link java.util.List} of getter {@link Method}s
     * @throws SerializerException if a field cannot be found
     */
    public static List<Method> gettersForFields(Class<?> source, String... fields) throws SerializerException {
        List<Method> results = new ArrayList<Method>();
        for (String field : fields) {
            results.add(SerializerUtils.findGetterForFieldName(source, field));
        }
        return results;
    }

    /**
     * Returns a getter name for a given field name.
     * Computed by "get" + fieldName[0].toUpperCase + fieldName[1..]
     *
     * @param field field name to compute getter name for
     * @return String of getter name
     */
    public static String fieldToGetter(String field) {
        return "get" +
                Character.toTitleCase(field.charAt(0)) +
                field.substring(1);
    }

    /**
     * Returns a setter name for a given field name.
     * Computed by "set" + fieldName[0].toUpperCase + fieldName[1..]
     *
     * @param field field name to compute setter name for
     * @return String of setter name
     */
    public static String fieldToSetter(String field) {
        return "set" +
                Character.toTitleCase(field.charAt(0)) +
                field.substring(1);
    }

    /**
     * Compute field name from given getter/setter method name.
     *
     * @param method method name to compute field name for
     * @return String field name
     */
    public static String methodToField(String method) {
        String fieldName;
        if (method.startsWith("get")) {
            fieldName = method.replaceFirst("get", "");
        } else if (method.startsWith("set")) {
            fieldName = method.replaceFirst("set", "");
        } else {
            fieldName = method;
        }
        return Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1);
    }

    /**
     * Returns a setter method for a field name on a given class, or null if no method is found.
     *
     * @param cls class to search for a setter
     * @param fieldName field name to find a setter for
     * @return {@link Method} if setter is found otherwise <code>null</code>
     */
    public static Method setterForField(Class<?> cls, String fieldName) {
        String setterName = fieldToSetter(fieldName);
        Field f = fieldForName(cls, fieldName);
        if (f == null) {
            return null;
        }
        try {
            return cls.getDeclaredMethod(setterName, f.getType());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Returns a {@link Field} on a given class for a given field name, or null if no such field is found.
     *
     * @param cls class to search for a field
     * @param fieldName field name to search for
     * @return {@link Field} if field is found, otherwise <code>null</code>
     */
    public static Field fieldForName(Class<?> cls, String fieldName) {
        try {
            return cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }


    /**
     * Returns a getter {@link Method} on the given class for a given field name.
     *
     * @param srcClass class to search for the getter
     * @param srcFieldName field name to find a getter for
     * @return getter {@link Method}
     * @throws SerializerException if there is no such method
     */
    public static Method findGetterForFieldName(Class<?> srcClass, String srcFieldName) throws SerializerException {
        String getterName = fieldToGetter(srcFieldName);
        try {
            return srcClass.getDeclaredMethod(getterName);
        } catch (NoSuchMethodException e) {
            throw new SerializerException("No method named '" + getterName +
                    "' on class '" + srcClass.getName()
                    + "'", e);
        }
    }

    /**
     * Finds a getter on a given class with a given field name, ensuring that the signatures are compatible.
     * "Compatible" is defined as the setter's parameter type is assignable from the getter's return type.
     *
     * @param srcClass class to search for getter
     * @param srcFieldName getter's field name
     * @param setter setter method for type-checking
     * @return getter Method object
     * @throws SerializerException if the method cannot be found or getter/setter types do not agree
     */
    public static Method findGetterForSetter(Class<?> srcClass, String srcFieldName, Method setter) throws SerializerException {
        Method m = findGetterForFieldName(srcClass, srcFieldName);
        Class<?> paramType = setter.getParameterTypes()[0];
        if (!paramType.isAssignableFrom(m.getReturnType())) {
            throw new SerializerException("Return type of '" + m.getName() +
                    "' on class '" + srcClass.getName()
                    + "' not assignable from setter '" + setter.getName()
                    + "': expected '" + paramType.getName()
                    + "' but found '" + m.getReturnType() + "'");
        }
        return m;
    }

    /**
     * Invokes the getter method on the given object and returns the value
     *
     * @param source instance object to call getter on
     * @param getter Method object to invoke on <code>source</code> (0 arguments are assumed)
     * @return return value of <code>getter</code>'s invocation
     * @throws SerializerException if the getter cannot be accessed or invoked
     */
    public static Object invokeGetter(Object source, Method getter) throws SerializerException {
        try {
            return getter.invoke(source);
        } catch (IllegalAccessException e) {
            throw new SerializerException("Could not access '" + getter.getName()
                    + "'on object of type '" + source.getClass().getName() + "'", e);
        } catch (InvocationTargetException e) {
            throw new SerializerException("Could not invoke '" + getter.getName()
                    + "'on object of type '" + source.getClass().getName() + "'", e);
        }
    }

    /**
     * Sets a field value on target object using the specified setter
     *
     * @param target instance object to call setter on
     * @param setter Method to use as setter (1 argument is assumed)
     * @param value Value passed to <code>setter</code> on invocation
     * @throws SerializerException if the setter cannot be accessed or invoked
     */
    public static void setFieldWithSetter(Object target, Method setter, Object value) throws SerializerException {
        try {
            setter.invoke(target, value);
        } catch (IllegalAccessException e) {
            throw new SerializerException("Could not access '" + setter.getName()
                    + "'on object of type '" + target.getClass().getName() + "'", e);
        } catch (InvocationTargetException e) {
            throw new SerializerException("Could not invoke '" + setter.getName()
                    + "'on object of type '" + target.getClass().getName() + "'", e);
        } catch (IllegalArgumentException e) {
            throw new SerializerException("Illegal argument of type '" + value.getClass().getName()
                    + "' for '" + setter.getName() + "' on clas '" + target.getClass().getName() + "'", e);
        }
    }

    /**
     * Sets a field value directly
     *
     * @param target instance object to set the value of the field on
     * @param fieldName field name to set the value to
     * @param value value to be set
     * @throws SerializerException if no such field exists or if the field cannot be accessed
     */
    public static void setFieldValue(Object target, String fieldName, Object value) throws SerializerException {
        Class<?> targetClass = target.getClass();
        Field f;
        try {
            f = targetClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new SerializerException("No field '" + fieldName
                    + "' on object of type '" + targetClass.getName(), e);
        }
        try {
            f.set(target, value);
        } catch (IllegalAccessException e) {
            throw new SerializerException("Could not access field '" + fieldName
                    + "' on object of type '" + target.getClass().getName() + "'", e);
        }
    }
}
