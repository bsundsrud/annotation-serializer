package com.github.bsundsrud.serializers.processors;

import static org.junit.Assert.*;

import com.github.bsundsrud.serializers.annotations.FromField;
import com.github.bsundsrud.serializers.annotations.SerializedFrom;
import com.github.bsundsrud.serializers.annotations.Synthesized;
import com.github.bsundsrud.serializers.annotations.WithSerializer;
import com.github.bsundsrud.serializers.util.SerializerException;
import org.junit.Test;
import com.github.bsundsrud.serializers.AnnotationSerializer;

import java.util.Map;

public class AnnotationSerializerTest {

    public static class Source {
        private int id;
        private SubSource sub;
        private String name;

        public Source(int id, SubSource sub, String name) {
            this.id = id;
            this.sub = sub;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public SubSource getSub() {
            return sub;
        }

        public String getName() {
            return name;
        }
    }

    public static class SubSource {

        private String extra;
        private String required;

        public SubSource(String extra, String required) {
            this.extra = extra;
            this.required = required;
        }

        public String getExtra() {
            return extra;
        }

        public String getRequired() {
            return required;
        }
    }

    @SerializedFrom(Source.class)
    public static class Target {
        public transient int ignored;
        public int id;
        private String name;
        public String combined;
        @FromField("id")
        public int idCopy;

        @FromField("sub")
        @WithSerializer
        public SubTarget otherSubField;

        @WithSerializer(SubTarget.class)
        public SubTarget sub;

        private SubTarget excludedSub;

        private SubTarget extraSub;

        @Synthesized(target = "combined", from = { "id", "name" })
        public String makeCombined(int id, String name) {
            return id + "-" + name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SubTarget getExcludedSub() {
            return excludedSub;
        }

        public void setId(int id, String name) {
            //Not a valid setter, must only have 1 param
            throw new IllegalStateException();
        }

        public SubTarget getExtraSub() {
            return extraSub;
        }

        @FromField("sub")
        @WithSerializer(fields = "required")
        public void setExcludedSub(SubTarget excludedSub) {
            this.excludedSub = excludedSub;
        }

        @FromField("sub")
        @WithSerializer(value = SubTarget.class, fields = "extra")
        public void setExtraSub(SubTarget st) {
            this.extraSub = st;
        }
    }

    @SerializedFrom(SubSource.class)
    public static class SubTarget {
        public String extra;
        public String required;
    }

    @SerializedFrom(Source.class)
    public static class SyntheticErrorTarget {
        public String combined;

        @Synthesized(target = "combined", from = {"id", "name"})
        public String compute(String name, int id) {
            return id + "-" + name;
        }

    }
    @SerializedFrom(Source.class)
    public static class SyntheticMismatchArgCountTarget {
        public String combined;

        @Synthesized(target = "combined", from = {"id", "name"})
        public String compute(String name) {
            return name;
        }

    }

    @SerializedFrom(Source.class)
    public static class WrongOtherSerializerTarget {
        private SubSource sub;

        @WithSerializer(SubTarget.class)
        public void setSub(int id) {

        }
    }

    @SerializedFrom(Source.class)
    public static class WrongOtherSerializerFieldTarget {
        @WithSerializer(SubTarget.class)
        public int sub;

    }

    @SerializedFrom(Source.class)
    public static class PrivateConstructor {
        private PrivateConstructor() {}
        public int id;
    }

    @SerializedFrom(Source.class)
    public static class ExceptionInConstructor {
        public ExceptionInConstructor(int id) {
        }
        public int id;
    }

    public static class NotASerializer {}

    @Test
    public void testHappyPath() throws Exception {
        AnnotationSerializer<Target> as = AnnotationSerializer.serializerForClass(Target.class);
        Source s = new Source(1, new SubSource("extra", "required"), "foo");
        Target t = as.serialize(s);
        assertEquals(t.id, 1);
        assertEquals(t.idCopy, 1);
        assertEquals(t.getName(), "foo");
        assertEquals(t.combined, "1-foo");
        assertEquals(t.sub.extra, "extra");
        assertEquals(t.sub.required, "required");
        assertEquals(t.getExcludedSub().required, "required");
        assertNull(t.getExcludedSub().extra);
        assertEquals(t.getExtraSub().extra, "extra");
    }

    @Test
    public void testMustBeSerializer() {
        try {
            AnnotationSerializer<NotASerializer> as = AnnotationSerializer.serializerForClass(NotASerializer.class);
            assertTrue(false);
        } catch (SerializerException e) {}
    }

    @Test
    public void testSyntheticArgMismatch() {
        try {
            AnnotationSerializer<SyntheticErrorTarget> as = AnnotationSerializer.serializerForClass(SyntheticErrorTarget.class);
            assertTrue(false);
        } catch (SerializerException e) {}

    }

    @Test
    public void testSyntheticArgCountMismatch() {
        try {
            AnnotationSerializer<SyntheticMismatchArgCountTarget> as = AnnotationSerializer.serializerForClass(SyntheticMismatchArgCountTarget.class);
            assertTrue(false);
        } catch (SerializerException e) {}

    }

    @Test
    public void testSubSerializersTypeMatching() {
        try {
            AnnotationSerializer<WrongOtherSerializerTarget> as = AnnotationSerializer.serializerForClass(WrongOtherSerializerTarget.class);
            assertTrue(false);
        } catch (SerializerException e) {}
        try {
            AnnotationSerializer<WrongOtherSerializerFieldTarget> as = AnnotationSerializer.serializerForClass(WrongOtherSerializerFieldTarget.class);
            assertTrue(false);
        } catch (SerializerException e) {}
    }

    @Test
    public void testInstatiation() throws Exception {
        Source s = new Source(1, new SubSource("extra", "required"), "foo");
        try {
            AnnotationSerializer<PrivateConstructor> as = AnnotationSerializer.serializerForClass(PrivateConstructor.class);

            PrivateConstructor p = as.serialize(s);
            assertTrue(false);
        } catch (SerializerException e) {}


        try {
            AnnotationSerializer<ExceptionInConstructor> as = AnnotationSerializer.serializerForClass(ExceptionInConstructor.class);
            ExceptionInConstructor p = as.serialize(s);
            assertTrue(false);
        } catch (SerializerException e) {}
    }

    @Test
    public void testSerializeNull() throws Exception {
        AnnotationSerializer<Target> as = AnnotationSerializer.serializerForClass(Target.class);
        Target t = as.serialize(null);
        assertNull(t);
    }

    @Test
    public void testCannotSerializeFromUnknownObject() throws Exception {
        AnnotationSerializer<Target> as = AnnotationSerializer.serializerForClass(Target.class);
        try {
            as.serializeToMap("Test");
            assertTrue(false);
        } catch (SerializerException e) {}
    }

    @Test
    public void testSerializeToMapHappyPath() throws Exception {
        AnnotationSerializer<Target> as = AnnotationSerializer.serializerForClass(Target.class);
        Source s = new Source(1, new SubSource("extra", "required"), "foo");
        Map<String, Object> m = as.serializeToMap(s);
        assertTrue(m.containsKey("id"));
        assertEquals(m.get("id"), 1);
        assertTrue(m.containsKey("name"));
        assertEquals(m.get("name"), "foo");
        assertTrue(m.containsKey("sub"));
        assertEquals(((Map)m.get("sub")).get("extra"), "extra");
        assertEquals(((Map)m.get("sub")).get("required"), "required");
        assertTrue(m.containsKey("combined"));
        assertTrue(m.containsKey("excludedSub"));
        assertEquals(((Map)m.get("excludedSub")).get("required"), "required");
        assertTrue(m.containsKey("extraSub"));
        assertEquals(((Map)m.get("extraSub")).get("extra"), "extra");
    }

    @Test
    public void testSerializeToMapNull() throws Exception {
        AnnotationSerializer<Target> as = AnnotationSerializer.serializerForClass(Target.class);

        Map<String, Object> m = as.serializeToMap(null);
        assertEquals(m.size(), 0);
    }

}
