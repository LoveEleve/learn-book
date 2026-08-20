import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * MiniCodec — RD-3 Codec 序列化体系 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 Redisson 4.6.2 源码):
 *   A. 接口契约 + 原型: 4 组编解码器 (value/mapKey/mapValue) + StringCodec 零开销 ByteBuf 直读写
 *      (client/codec/Codec.java:30-79, StringCodec.java:44-53)
 *   B. CompositeCodec 三委托: mapKey/mapValue/value 转发, 2 参构造 value=null 合法 (Stream 场景)
 *      (org.redisson.codec/CompositeCodec.java:30-45, RedissonReliableTopic.java:82)
 *   C. 类型保真三档: 丢类型 (readValue Object.class) vs 指定类型 (TypeReference) vs 内置类型 (ClassAndObject)
 *      (JsonJacksonCodec.java:105, TypedJsonJacksonCodec.java:40, Kryo5Codec.java:202)
 *
 * 使用 Java 原生序列化模拟对象流, 机制复现非完整库。
 */
public class MiniCodec {

    // ===== 基础字节编解码 (Encoder 产 byte[], Decoder 收 byte[]) =====
    interface Decoder<T> { T decode(byte[] buf); }
    interface Encoder { byte[] encode(Object in); }

    // ==== A1. 接口契约: 4 组编解码器 (对照 Codec.java:30-79) ====
    interface Codec {
        Decoder<Object> getMapValueDecoder();
        Encoder getMapValueEncoder();
        Decoder<Object> getMapKeyDecoder();
        Encoder getMapKeyEncoder();
        Decoder<Object> getValueDecoder();
        Encoder getValueEncoder();
    }

    // ==== A2. StringCodec 原型: 零开销直读写, INSTANCE 单例 (StringCodec.java:34-53) ====
    static class StringCodec implements Codec {
        static final StringCodec INSTANCE = new StringCodec();
        Encoder encoder = in -> in.toString().getBytes(StandardCharsets.UTF_8);
        Decoder<Object> decoder = buf -> new String(buf, StandardCharsets.UTF_8);
        public Decoder<Object> getMapValueDecoder() { return decoder; }
        public Encoder getMapValueEncoder() { return encoder; }
        public Decoder<Object> getMapKeyDecoder() { return decoder; }
        public Encoder getMapKeyEncoder() { return encoder; }
        public Decoder<Object> getValueDecoder() { return decoder; }
        public Encoder getValueEncoder() { return encoder; }
    }

    // 数值原型: 仿真 LongCodec 直转 (client/codec)
    static class LongCodec implements Codec {
        static final LongCodec INSTANCE = new LongCodec();
        Encoder encoder = in -> String.valueOf(in).getBytes(StandardCharsets.UTF_8);
        Decoder<Object> decoder = buf -> Long.parseLong(new String(buf, StandardCharsets.UTF_8));
        // map 系列转发 value (BaseCodec 兜底语义 BaseCodec.java:54-69)
        public Decoder<Object> getMapValueDecoder() { return decoder; }
        public Encoder getMapValueEncoder() { return encoder; }
        public Decoder<Object> getMapKeyDecoder() { return decoder; }
        public Encoder getMapKeyEncoder() { return encoder; }
        public Decoder<Object> getValueDecoder() { return decoder; }
        public Encoder getValueEncoder() { return encoder; }
    }

    // ==== B. CompositeCodec 三委托 (CompositeCodec.java:30-45) ====
    static class CompositeCodec implements Codec {
        final Codec mapKeyCodec, mapValueCodec, valueCodec;
        CompositeCodec(Codec mapKeyCodec, Codec mapValueCodec) {
            this(mapKeyCodec, mapValueCodec, null); // 2 参: value=null 合法
        }
        CompositeCodec(Codec mapKeyCodec, Codec mapValueCodec, Codec valueCodec) {
            this.mapKeyCodec = mapKeyCodec; this.mapValueCodec = mapValueCodec; this.valueCodec = valueCodec;
        }
        public Decoder<Object> getMapValueDecoder() { return mapValueCodec.getMapValueDecoder(); }
        public Encoder getMapValueEncoder() { return mapValueCodec.getMapValueEncoder(); }
        public Decoder<Object> getMapKeyDecoder() { return mapKeyCodec.getMapKeyDecoder(); }
        public Encoder getMapKeyEncoder() { return mapKeyCodec.getMapKeyEncoder(); }
        public Decoder<Object> getValueDecoder() { return valueCodec.getValueDecoder(); } // 无 null 兜底 (契约)
        public Encoder getValueEncoder() { return valueCodec.getValueEncoder(); }
    }

    // ==== C. 类型保真三档 (JsonJackson:105 / Typed:40 / Kryo5:202) ====
    // C1. 丢类型: 反序列化到普通 Object, 需已知型
    static class UntypedCodec implements Codec {
        Decoder<Object> decoder = buf -> javaSerializeDecode(buf); // Object.class 语义
        Encoder encoder = in -> javaSerializeEncode(in);
        // 其余转发
        public Decoder<Object> getMapValueDecoder() { return decoder; }
        public Encoder getMapValueEncoder() { return encoder; }
        public Decoder<Object> getMapKeyDecoder() { return decoder; }
        public Encoder getMapKeyEncoder() { return encoder; }
        public Decoder<Object> getValueDecoder() { return decoder; }
        public Encoder getValueEncoder() { return encoder; }
    }

    // C2/C3 共用类型保留序列化: 把 Class 名写进流 (ClassAndObject 语义 Kryo5.java:202)
    static byte[] typefulEncode(Object in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeUTF(in.getClass().getName()); // 类型写进流
            oos.writeObject(in);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    static <T> T typefulDecode(byte[] buf) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf))) {
            ois.readUTF(); // 读类名 (真实场景用它按类加载)
            return (T) ois.readObject();
        }
    }

    @SuppressWarnings("unchecked")
    static <T> T javaSerializeDecode(byte[] buf) {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buf))) {
            return (T) ois.readObject(); // Object.class 语义 — 返回真实类型但调用方不知
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    static byte[] javaSerializeEncode(Object in) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) { oos.writeObject(in); }
            return baos.toByteArray();
        } catch (IOException e) { throw new RuntimeException(e); }
    }
}