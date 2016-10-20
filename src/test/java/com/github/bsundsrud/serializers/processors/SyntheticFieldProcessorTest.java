package com.github.bsundsrud.serializers.processors;

import org.junit.Test;
import com.github.bsundsrud.serializers.util.SerializerException;
import com.github.bsundsrud.serializers.util.SerializerUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class SyntheticFieldProcessorTest {

    public static class Source {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void getException() {
            throw new IllegalStateException();
        }
    }

    public static class ErrorSource {
        private int id;
        private String name;

        private int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        private String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

    }

    public static class Target {
        public String combined;

        public String doCombination(int id, String name) {
            return id + "-" + name;
        }

        public void setCombined(String combined) {
            this.combined = combined;
        }
    }

    public static class ErrorTarget {
        public String combined;

        private String doCombination(int id, String name) {
            return id + "-" + name;
        }

        public String exceptionCombination(int id, String name) {
            throw new IllegalStateException();
        }

        private String accessCombination(int id, String name) {
            return "";
        }
    }

    @Test
    public void testSerialize() throws Exception {
        Method combinator = Target.class.getDeclaredMethod("doCombination", int.class, String.class);
        List<Method> getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        SyntheticFieldProcessor sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);

        Source s = new Source();
        s.setId(3);
        s.setName("Foo");
        Target t = new Target();
        sfs.serialize(s, t);
        assertEquals(t.combined, "3-Foo");

        Method setter = SerializerUtils.setterForField(Target.class, "combined");
        sfs = new SyntheticFieldProcessor("combined", combinator, setter, getters);
        s = new Source();
        s.setId(4);
        s.setName("Bar");
        t = new Target();
        sfs.serialize(s, t);
        assertEquals(t.combined, "4-Bar");
    }

    @Test
    public void testSerializeErrors() throws Exception {
        Method combinator = ErrorTarget.class.getDeclaredMethod("doCombination", int.class, String.class);
        List<Method> getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        SyntheticFieldProcessor sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);

        Source s = new Source();
        s.setId(3);
        s.setName("Foo");
        Target t = new Target();
        try {
            sfs.serialize(s, t);
            assertTrue(false);
        } catch (SerializerException e) {}

        getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(ErrorSource.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(ErrorSource.class, "name"));
        sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);
        s = new Source();
        s.setId(3);
        s.setName("Foo");
        t = new Target();
        try {
            sfs.serialize(s, t);
            assertTrue(false);
        } catch (SerializerException e) {}

        getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "exception"));
        sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);
        s = new Source();
        s.setId(3);
        s.setName("Foo");
        t = new Target();
        try {
            sfs.serialize(s, t);
            assertTrue(false);
        } catch (SerializerException e) {}

        combinator = ErrorTarget.class.getDeclaredMethod("exceptionCombination", int.class, String.class);
        getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);
        s = new Source();
        s.setId(3);
        s.setName("Foo");
        ErrorTarget et = new ErrorTarget();
        try {
            sfs.serialize(s, et);
            assertTrue(false);
        } catch (SerializerException e) {}

        combinator = ErrorTarget.class.getDeclaredMethod("accessCombination", int.class, String.class);
        getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);
        s = new Source();
        s.setId(3);
        s.setName("Foo");
        et = new ErrorTarget();
        try {
            sfs.serialize(s, et);
            assertTrue(false);
        } catch (SerializerException e) {}
    }

    @Test
    public void testSerializeToMapErrors() throws Exception {
        Method combinator = ErrorTarget.class.getDeclaredMethod("exceptionCombination", int.class, String.class);
        List<Method> getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        SyntheticFieldProcessor sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);
        Map<String, Object> map = new HashMap<String, Object>();
        Source s = new Source();
        s.setId(3);
        s.setName("Foo");
        ErrorTarget et = new ErrorTarget();
        try {
            sfs.serializeToMap(s, et, map);
            assertTrue(false);
        } catch (SerializerException e) {}

        combinator = ErrorTarget.class.getDeclaredMethod("accessCombination", int.class, String.class);
        getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);
        s = new Source();
        s.setId(3);
        s.setName("Foo");
        et = new ErrorTarget();
        try {
            sfs.serializeToMap(s, et, map);
            assertTrue(false);
        } catch (SerializerException e) {}
    }

    @Test
    public void testSerializeToMap() throws Exception {
        Method combinator = Target.class.getDeclaredMethod("doCombination", int.class, String.class);
        List<Method> getters = new ArrayList<Method>();
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "id"));
        getters.add(SerializerUtils.findGetterForFieldName(Source.class, "name"));
        SyntheticFieldProcessor sfs = new SyntheticFieldProcessor("combined", combinator, null, getters);

        Source s = new Source();
        s.setId(3);
        s.setName("Foo");
        Target t = new Target();
        Map<String, Object> map = new HashMap<String, Object>();
        sfs.serializeToMap(s, t, map);
        assertTrue(map.containsKey("combined"));
        assertEquals(map.get("combined"), "3-Foo");

    }
}
