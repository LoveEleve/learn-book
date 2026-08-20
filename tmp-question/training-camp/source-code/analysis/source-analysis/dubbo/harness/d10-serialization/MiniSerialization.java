import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiniSerialization {

    // ---- A: Serialization SPI (serializeType → 实现) ----
    interface Serialization { String name(); byte id(); byte[] serialize(Object o); Object deserialize(byte[] d); }
    static class Hessian2Serialization implements Serialization {
        public String name() { return "hessian2"; }
        public byte id() { return 2; } // HESSIAN2_SERIALIZATION_ID = 2
        public byte[] serialize(Object o) { return ("H2:" + o).getBytes(); }
        public Object deserialize(byte[] d) { return new String(d).substring(3); }
    }
    static class FastJson2Serialization implements Serialization {
        public String name() { return "fastjson2"; }
        public byte id() { return 23; } // FASTJSON2_SERIALIZATION_ID = 23
        public byte[] serialize(Object o) { return ("{\"v\":\"" + o + "\"}").getBytes(); }
        public Object deserialize(byte[] d) { return new String(d); }
    }

    static class DefaultMultipleSerialization {
        static final Map<String, Serialization> SPI = new ConcurrentHashMap<>();
        static { SPI.put("hessian2", new Hessian2Serialization()); SPI.put("fastjson2", new FastJson2Serialization()); }
        static Serialization select(String serializeType) {
            serializeType = convertHessian(serializeType); // 兼容转换
            return SPI.get(serializeType);
        }
        static String convertHessian(String type) { return "hessian".equals(type) ? "hessian2" : type; }
    }

    // ---- B: 选择器 (默认 hessian2 + 覆盖链) ----
    static class DefaultSerializationSelector {
        static final String DEFAULT = "hessian2"; // 默认
        static String resolve(String fromProperty, String fromEnv) {
            if (fromProperty != null) return fromProperty;
            if (fromEnv != null) return fromEnv;
            return DEFAULT;
        }
    }

    // ---- C: hessian2 对象图 (循环引用) ----
    static class Node { String name; Node ref; } // 循环引用

    static class Hessian2ObjectGraph {
        static String serializeNode(Node n) { return "GRAPH(" + n.name + (n.ref != null ? "→" + n.ref.name : "") + ")"; }
    }

    // ---- D: 反序列化安全 (类检查) ----
    static class Fastjson2SecurityManager {
        static boolean checkSerializable = true;
        static String deserializeSafe(byte[] data) {
            if (checkSerializable && !isAllowed(new String(data))) return "DENIED"; // 类白名单
            return "OK";
        }
        static boolean isAllowed(String json) { return json.contains("SafeClass"); }
        static void notifyCheckSerializable(boolean v) { checkSerializable = v; } // 动态开关
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        // ============ A: SPI 选择 + ID ============
        Serialization h2 = DefaultMultipleSerialization.select("hessian2");
        Serialization fj2 = DefaultMultipleSerialization.select("fastjson2");
        Serialization compat = DefaultMultipleSerialization.select("hessian"); // convertHessian
        System.out.println("A1 select: " + h2.name() + "(id=" + h2.id() + ") / " + fj2.name() + "(id=" + fj2.id() + ") / 兼容→" + compat.name());
        System.out.println("A2 proto : " + new String(fj2.serialize("hi")));

        // ============ B: 默认 + 覆盖 ============
        System.out.println("B1 def   : " + DefaultSerializationSelector.resolve(null, null));
        System.out.println("B2 cover : " + DefaultSerializationSelector.resolve("fastjson2", null));

        // ============ C: 对象图 ============
        Node n1 = new Node(); Node n2 = new Node();
        n1.name = "A"; n2.name = "B"; n1.ref = n2; n2.ref = n1; // 循环引用
        System.out.println("C1 graph : " + Hessian2ObjectGraph.serializeNode(n1));

        // ============ D: 安全 ============
        System.out.println("D1 safe  : " + Fastjson2SecurityManager.deserializeSafe("{\"v\":\"EvilClass\"}".getBytes()));
        Fastjson2SecurityManager.notifyCheckSerializable(false); // 动态关
        System.out.println("D2 off   : " + Fastjson2SecurityManager.deserializeSafe("{\"v\":\"EvilClass\"}".getBytes()));

        // 断言
        pass += h2.id() == 2 && fj2.id() == 23 ? 1 : 0;   // ID 值
        pass += compat.name().equals("hessian2") ? 1 : 0; // 兼容转换
        pass += DefaultSerializationSelector.resolve(null, null).equals("hessian2") ? 1 : 0; // 默认
        pass += Hessian2ObjectGraph.serializeNode(n1).contains("→B") ? 1 : 0; // 对象图
        pass += Fastjson2SecurityManager.deserializeSafe("{\"v\":\"EvilClass\"}".getBytes()).equals("OK") ? 1 : 0; // 开关已关
        fail = 5 - pass;
        System.out.println("PASS=" + pass + "/5 FAIL=" + fail);
        if (fail > 0) System.exit(1);
    }
}
