package com.github.bsundsrud.serializers.util;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class SerializerUtilsTest {

    public static class MyObject {
        private String myPrivateField;
        public String myPublicField;
        private int myOtherPrivateField;
        private String exceptionField;

        public String getMyPrivateField() {
            return myPrivateField;
        }

        public void setMyPrivateField(String myPrivateField) {
            this.myPrivateField = myPrivateField;
        }

        private int getMyOtherPrivateField() {
            return myOtherPrivateField;
        }

        private void setMyOtherPrivateField(int myOtherPrivateField) {
            this.myOtherPrivateField = myOtherPrivateField;
        }

        public String getExceptionField() {
            throw new IllegalStateException();
        }

        public void setExceptionField(String exceptionField) {
            throw new IllegalStateException();
        }
    }

    @Test
    public void testFieldNameTransforms() {
        String fieldName = "myObjectField";
        assertEquals(SerializerUtils.fieldToGetter(fieldName), "getMyObjectField");
        assertEquals(SerializerUtils.fieldToSetter(fieldName), "setMyObjectField");
        assertEquals(fieldName,
                SerializerUtils.methodToField(SerializerUtils.fieldToGetter(fieldName)));
        assertEquals(fieldName,
                SerializerUtils.methodToField(SerializerUtils.fieldToSetter(fieldName)));
        assertEquals(fieldName, SerializerUtils.methodToField(fieldName));

    }

    @Test
    public void testFieldOperations() throws Exception {
        Field f = SerializerUtils.fieldForName(MyObject.class, "myPublicField");
        assertNotNull(f);
        f = SerializerUtils.fieldForName(MyObject.class, "doesntExist");
        assertNull(f);
        MyObject o = new MyObject();
        SerializerUtils.setFieldValue(o, "myPublicField", "test");
        assertEquals(o.myPublicField, "test");
        try {
            SerializerUtils.setFieldValue(o, "nope", "test");
            assertTrue(false);
        } catch (SerializerException e) {

        }
        try {
            SerializerUtils.setFieldValue(o, "myPrivateField", "test");
            assertTrue(false);
        } catch (SerializerException e) {

        }
        Method setter = SerializerUtils.setterForField(MyObject.class, "nonexistentPrivate");
        assertNull(setter);
        setter = SerializerUtils.setterForField(MyObject.class, "myPublicField");
        assertNull(setter);
        setter = SerializerUtils.setterForField(MyObject.class, "myPrivateField");
        assertNotNull(setter);
        SerializerUtils.setFieldWithSetter(o, setter, "test2");
        try {
            SerializerUtils.setFieldWithSetter(o, setter, new Date());
            assertTrue(false);
        } catch (SerializerException e) {

        }
        assertEquals(o.getMyPrivateField(), "test2");
        List<Method> getters = SerializerUtils.gettersForFields(MyObject.class, "myPrivateField", "myOtherPrivateField");
        assertEquals(getters.size(), 2);
        try {
            getters = SerializerUtils.gettersForFields(MyObject.class, "myPrivateField", "nonexistent");
            assertTrue(false);
        } catch (SerializerException e) {
            assertTrue(true);
        }

    }

    @Test
    public void testGetterSetterMethods() throws Exception {
        Method setter = SerializerUtils.setterForField(MyObject.class, "myPrivateField");
        Method getter = SerializerUtils.findGetterForSetter(MyObject.class, "myPrivateField", setter);
        assertNotNull(getter);
        MyObject o = new MyObject();
        o.setMyPrivateField("test");
        String result = (String)SerializerUtils.invokeGetter(o, getter);
        assertEquals(result, "test");
        try {
            getter = SerializerUtils.findGetterForSetter(MyObject.class, "myOtherPrivateField", setter);
            assertTrue(false);

        } catch (SerializerException e) {
            assertTrue(true);
        }

        setter = SerializerUtils.setterForField(MyObject.class, "myOtherPrivateField");
        getter = SerializerUtils.findGetterForSetter(MyObject.class, "myOtherPrivateField", setter);
        try {
            Object obj = SerializerUtils.invokeGetter(o, getter);
            assertTrue(false);
        } catch (SerializerException e) {

        }

        try {
            SerializerUtils.setFieldWithSetter(o, setter, 1);
            assertTrue(false);
        } catch (SerializerException e) {

        }

        setter = SerializerUtils.setterForField(MyObject.class, "exceptionField");
        getter = SerializerUtils.findGetterForFieldName(MyObject.class, "exceptionField");
        try {
            SerializerUtils.setFieldWithSetter(o, setter, "");
            assertTrue(false);
        } catch (SerializerException e) {

        }

        try {
            Object obj = SerializerUtils.invokeGetter(o, getter);
            assertTrue(false);
        } catch (SerializerException e) {

        }

    }
}
