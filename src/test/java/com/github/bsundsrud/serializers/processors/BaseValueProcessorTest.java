package com.github.bsundsrud.serializers.processors;


import static org.junit.Assert.*;

import org.junit.Test;
import com.github.bsundsrud.serializers.util.SerializerUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class BaseValueProcessorTest {

    public static class TestSource {
        private String pub;
        private int priv;

        public int getPriv() {
            return priv;
        }

        public void setPriv(int priv) {
            this.priv = priv;
        }

        public String getPub() {
            return pub;
        }

        public void setPub(String pub) {
            this.pub = pub;
        }
    }

    public static class TestTarget {
        public String pub;
        private int priv;

        public int getPriv() {
            return priv;
        }

        public void setPriv(int priv) {
            this.priv = priv;
        }
    }

    @Test
    public void testSerialize() throws Exception {
        Method getter = SerializerUtils.findGetterForFieldName(TestSource.class, "pub");
        BaseValueProcessor s = new BaseValueProcessor("pub", getter);
        TestSource src = new TestSource();
        src.pub = "test";
        TestTarget tgt = new TestTarget();
        s.serialize(src, tgt);
        assertEquals(tgt.pub, src.pub);

        getter = SerializerUtils.findGetterForFieldName(TestSource.class, "priv");
        Method setter = SerializerUtils.setterForField(TestTarget.class, "priv");
        s = new BaseValueProcessor("priv", getter, setter);
        src = new TestSource();
        src.setPriv(2);
        tgt = new TestTarget();
        s.serialize(src, tgt);
        assertEquals(src.getPriv(), tgt.getPriv());
    }

    @Test
    public void testSerializeToMap() throws Exception {
        Method getter = SerializerUtils.findGetterForFieldName(TestSource.class, "pub");
        BaseValueProcessor s = new BaseValueProcessor("pub", getter);
        TestSource src = new TestSource();
        src.pub = "test";
        TestTarget tgt = new TestTarget();
        Map<String, Object> map = new HashMap<String, Object>();
        s.serializeToMap(src, tgt, map);
        assertTrue(map.containsKey("pub"));
        assertEquals(map.get("pub"), src.pub);
    }
}
