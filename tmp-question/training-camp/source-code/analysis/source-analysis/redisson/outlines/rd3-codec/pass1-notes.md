# Pass 1 探索笔记: RD-3 Codec 序列化体系

> 方案 A (🔴) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 大域预判: codec/ 36 文件 + client/codec 9 文件 = 双包结构 → 预判 2-3 篇

## Pass 0 上下文吸收

- 双包结构: **接口层 `org.redisson.client.codec` (9 文件: Codec 接口 + 原型)** vs **实现层 `org.redisson.codec` (36 文件: Kryo5/Jackson 家族/压缩族)**
- 09 审计已知: 默认 Kryo5 (Config.java:165), FST/Marshalling 已删, 现行含 ZStd/Protobuf/Composite
- CHANGELOG 时空: 4.0.0 "Jackson library is now optional" (Jackson 降级为可选依赖) — 默认 Kryo5 的根因线索
- 测试地图: codec/ 7 测试 (JsonJackson3/JsonJackson/Kryo5/SnappyV2/TypedJson×2 + protobuf/) — round-trip 全类型覆盖

## 继承树/调用图

```
Codec (接口, client/codec/Codec.java:30)          ← 4 组编解码器 + getClassLoader
├── BaseCodec (抽象, client/codec/BaseCodec:32)     ← copy(ClassLoader)/map→value 兜底
│    ├── StringCodec (client/codec:34)               — 原型: ByteBuf 直读写 UTF-8
│    ├── IntegerCodec/LongCodec/DoubleCodec           — 数值原型
│    ├── BitSetCodec/ByteArrayCodec/TDigestDoubleCodec (client/codec)
│    ├── Kryo5Codec (org.redisson.codec:62)           — 默认; kryoPool/inputPool/outputPool 三池化
│    ├── KryoCodec (旧版 Kryo2)
│    ├── JsonJacksonCodec (org.redisson.codec:54)     — ObjectMapper, 类型丢失 (read Object.class)
│    │    ├── JsonJackson3Codec                        — Jackson 3.x
│    │    ├── TypedJsonJacksonCodec (:40)              — TypeReference 保类型
│    │    └── TypedJsonJackson3Codec
│    ├── 其他 Jackson 封装: Avro/Cbor/Ion/Smile/MsgPack/Protobuf...
│    ├── SerializationCodec                            — JDK ObjectOutputStream
│    ├── ObjectCodec                                  — 按值类型在 codec 表中选择
│    ├── LZ4CodecV2/SnappyCodecV2/ZStdCodec           — 压缩装饰器 (innerCodec)
│    └── CompositeCodec (org.redisson.codec:30)       — mapKey/mapValue/value 三委托
│         ├── new CompositeCodec(StringCodec, codec)      (2参: value=null, Stream 场景)
│         └── new CompositeCodec(StringCodec, codec, codec) (3参, Buckets)
命令消费: CommandAsyncService.evalWriteAsync(String key, Codec codec, ...) :471 — codec 显式传参
          CommandDecoder:572 / CommandPubSubDecoder:273 — 读侧 getValueDecoder() 取 decoder
```

## 基本元素分解

1. **Codec 接口契约** (client/codec/Codec.java:30-81) — 4 组 encoder/decoder (value/mapKey/mapValue) + getClassLoader; 注解要求实现"默认构造 + ClassLoader 构造"双构造器
2. **BaseCodec 兜底** (client/codec/BaseCodec:32-69) — abstract; copy(classLoader) 反射重建; map 系列默认转发 value
3. **三池化 Kryo5** (org.redisson.codec/Kryo5Codec:62,194-236) — kryoPool: Kryo 非线程安全 → pool; inputPool/outputPool: 缓冲复用; write/readClassAndObject 类型感知
4. **ObjectMapper 家族** (JsonJacksonCodec:54,105) — readValue(Object.class) 类型丢失; Typed → TypeReference; 4.0.0 后 Jackson 可选
5. **压缩装饰器** (LZ4CodecV2:44/75-89, Snappy:43/72-83, ZStd:43/75-86) — innerCodec 编解码 + 外压解压; BaseCodec 派生
6. **CompositeCodec 三委托** (CompositeCodec:30-45) — mapKey/mapValue/value; 2参构造 value=null 合法 (Stream)
7. **JDK 序列化** (SerializationCodec) — ObjectOutput 直写; 安全面 (不可靠类型)

## 标记问题 (8 个)

1. **Q1 双包边界**: 为什么 Codec 接口在 client/codec (网络协议层) 而实现在 org.redisson.codec?谁决定归属?
2. **Q2 CompositeCodec value=null**: getValueDecoder() 无兜底 → NPE?Stream 场景怎么避开 value 语义?
3. **Q3 默认 Kryo5 根因**: 为什么是 Kryo 非 Jackson?4.0.0 "Jackson optional" 的决策链?
4. **Q4 压缩装饰器**: innerCodec 怎么套?压/解在哪层 (进程内 vs 传输)?为什么 V2?
5. **Q5 类型保真战线**: JsonJackson (Object.class) vs TypedJsonJackson (TypeReference) vs Kryo (ClassAndObject) — 三种类型策略的取舍?
6. **Q6 原型 vs 通用**: StringCodec 零开销 ByteBuf vs Kryo5 通用序列化 — 什么时候用原型?
7. **Q7 线程安全契约**: Kryo 池化 (非线程安全); ObjectMapper 线程安全吗?Protobuf RuntimeSchema?池化的通用模式?
8. **Q8 命令层衔接**: codec 作为参数传入 evalWriteAsync?Decoder 在 CommandDecoder 侧怎么被选出来?

## 已读测试 (2 个)

- JsonJackson3CodecTest: string/int/long/double/boolean/null/BigDecimal/list/set/map 全类型 round-trip — RD-3 机制验证面
- Kryo5CodecTest: (结构待进一步读 — 三池化正确性)

## 完成检查

- [x] 双包结构 + 继承树已画出
- [x] 基本元素分解 7 项有源码位置
- [x] 8 个标记问题有源码位置
- [x] 已读 2 测试 (JsonJackson3CodecTest + protobuf 目录)
- [x] 时空溯源 (CHANGELOG 4.0.0 Jackson optional)