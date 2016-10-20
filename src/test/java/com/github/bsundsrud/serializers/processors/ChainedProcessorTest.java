package com.github.bsundsrud.serializers.processors;

import org.junit.Test;
import com.github.bsundsrud.serializers.AnnotationSerializer;
import com.github.bsundsrud.serializers.annotations.SerializedFrom;
import com.github.bsundsrud.serializers.util.SerializerUtils;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ChainedProcessorTest {

    public static class Parent {
        private Source source;

        public Source getSource() {
            return source;
        }

        public void setSource(Source source) {
            this.source = source;
        }
    }

    public static class Source {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }

    public static class Target {
        public TargetSerializer target;

        public void setTarget(TargetSerializer target) {
            this.target = target;
        }
    }

    @SerializedFrom(Source.class)
    public static class TargetSerializer {
        public int id;
    }

    @Test
    public void testSerialize() throws Exception {
        Method getter = SerializerUtils.findGetterForFieldName(Parent.class, "source");
        AnnotationSerializer<TargetSerializer> as = AnnotationSerializer.serializerForClass(TargetSerializer.class);
        ChainedProcessor cs = new ChainedProcessor(as, "target", getter, null);
        Parent p = new Parent();
        Source s = new Source();
        s.setId(2);
        p.setSource(s);
        Target t = new Target();
        cs.serialize(p, t);
        assertEquals(t.target.id, s.getId());
        Method setter = SerializerUtils.setterForField(Target.class, "target");
        cs = new ChainedProcessor(as, "target", getter, setter);
        s.setId(3);
        t = new Target();
        cs.serialize(p, t);
        assertEquals(t.target.id, s.getId());
    }

    @Test
    public void testSerializeToMap() throws Exception {
        Method getter = SerializerUtils.findGetterForFieldName(Parent.class, "source");
        AnnotationSerializer<TargetSerializer> as = AnnotationSerializer.serializerForClass(TargetSerializer.class);
        ChainedProcessor cs = new ChainedProcessor(as, "target", getter, null);
        Parent p = new Parent();
        Source s = new Source();
        s.setId(2);
        p.setSource(s);
        Target t = new Target();
        Map<String, Object> map = new HashMap<String, Object>();
        cs.serializeToMap(p, t, map);
        assertTrue(map.containsKey("target"));
        Map<String, Object> tmap = (Map<String, Object>)map.get("target");
        assertTrue(tmap.containsKey("id"));
        assertEquals(tmap.get("id"), s.getId());

        Method setter = SerializerUtils.setterForField(Target.class, "target");
        cs = new ChainedProcessor(as, "target", getter, setter);
        s.setId(3);
        t = new Target();
        cs.serialize(p, t);
        map = new HashMap<String, Object>();
        cs.serializeToMap(p, t, map);
        assertTrue(map.containsKey("target"));
        tmap = (Map<String, Object>)map.get("target");
        assertTrue(tmap.containsKey("id"));
        assertEquals(tmap.get("id"), s.getId());
    }
}
