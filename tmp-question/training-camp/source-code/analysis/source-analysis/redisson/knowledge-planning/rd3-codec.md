# RD-3 Codec 序列化体系 — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🔴 A / 3 篇 (+harness) | client/codec (9 文件) + org.redisson.codec (36 文件)
> 基线: REDISSON-PLAN RD-3 — 前置: **RD-0 导论** — 展开 接口契约→原型→默认→复合→压缩→类型保真→线程安全→命令衔接
> 双链: 前置 [[rd0-intro]] | 复用 [[r1-object]] (服务端编码对照) | 对照 [[r5-quicklist]] (服务端压缩) [[s45-core-environment]] | 引出 [[rd4-command]] [[rd5-rmap]] [[rd2-rlock]]

---

## §0.8

- 🔴 A，3篇 — 双包边界(**Codec 接口在 client/codec:30 协议面, 实现在 org.redisson.codec:36 实现面; StringCodec implements JsonCodec 跨包证明是分层非隔离**) → 接口契约(**4 组编解码器: value/mapKey/mapValue + getClassLoader, 需双构造器 (默认+ClassLoader) Codec.java:37-79**) → 原型(**StringCodec 零开销: writeCharSequence/toString 直走 ByteBuf, INSTANCE 单例; 已知类型直传**) → 三池化默认(**Kryo5Codec: Pool<Kryo>(1024)+Input(512)+Output(512), write/readClassAndObject 类型内置; 4.0.0 Jackson optional 成默认根因**) → 复合(**CompositeCodec 三委托 mapKey/mapValue/value; 2 参 value=null 合法 — Stream 只玩 map 语义 ReliableTopic:82**) → 压缩装饰器(**LZ4/Snappy/ZStd: decompressionSize 头+解压流+innerCodec.decode; 默认 inner=Kryo5**) → 类型保真三档(**JsonJackson 丢类型 (readValue Object.class) / TypedJsonJackson TypeReference 指定 / Kryo5 ClassAndObject 内置**) → 线程安全三策(**Kryo 池化 / ObjectMapper 单例共享 / Protobuf RuntimeSchema 缓存**) → 命令衔接(**codec 显式参数传 evalWriteAsync, 响应侧 CommandDecoder.selectDecoder 从 CommandData 取 codec**) → Jackson 可选化(**4.0.0 "Jackson library is now optional" — 默认不能依赖它**)
- 设计模式: [模式: 协议接口+零开销原型+池化序列化+委托复合+装饰压缩]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| client/codec/Codec.java:30-79 | 接口契约 | 4 组编解码器 + getClassLoader; 双构造器要求 | High |
| client/codec/StringCodec.java:34-53 | 原型 | INSTANCE 单例; ByteBuf 直读写零分配; implements JsonCodec | High |
| org.redisson.codec/Kryo5Codec.java:97-236 | 默认 | 三池化 (Kryo 1024/Input/Output 512); ClassAndObject; 失败回收 | High |
| org.redisson.codec/CompositeCodec.java:30-45 | 复合 | 三委托; 2 参 value=null 合法 (Stream) | High |
| org.redisson.codec/LZ4CodecV2.java:44-96 | 压缩 | 装饰器; decompressionSize 头; 默认 inner=Kryo5 | High |
| JsonJacksonCodec.java:105 vs TypedJsonJacksonCodec.java:40-75 | 类型保真 | Object.class 丢类型 vs TypeReference 指定 | High |
| JsonJacksonCodec.java:57; ProtobufCodec.java:189 | 线程安全 | 单例共享 vs Schema 缓存 | High |
| CommandAsyncService.java:471; CommandDecoder.java:565-575 | 命令衔接 | codec 显式传参; 响应侧 selectDecoder | High |
| Config.java:165; CHANGELOG 4.0.0 | 默认根因 | Kryo5; Jackson optional | High |

---

## 02-04 聚合+分类+聚类 (3篇+harness)

**3篇理由**: 8 闭环 → 聚类: 篇1 接口与原型 (q1/q6/q8), 篇2 默认与复合 (q2/q3), 篇3 压缩与类型 (q4/q5/q7)。harness 验证 Kryo5/Json round-trip + CompositeCodec 委托。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 双包接口契约 | 🔴 | **为什么🔴**: 一切 codec 的根 |
| P1-2 | 三池化 Kryo5 默认 | 🔴 | **为什么🔴**: 线程安全方案 |
| P1-3 | CompositeCodec 委托 | 🔴 | **为什么🔴**: 组合设计 |
| P1-4 | 类型保真三档 | 🔴 | **为什么🔴**: 面试硬核 |
| P2-1 | 压缩装饰器 | 🟡 | 支撑面 |
| P2-2 | 原型零开销 | 🟡 | 命令面 |
| P2-3 | 命令衔接 | 🟡 | 协议面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **接口与原型** (q1/q6/q8) | 🔴 | 根 |
| B | **默认与复合** (q2/q3) | 🔴 | 常用 |
| C | **压缩与类型** (q4/q5/q7) | 🟡 | 选型 |

---

## 05 闭环结论摘要 (Pass 2 内化)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 双包边界 | 接口在 client/codec (协议面) + 实现在 org.redisson.codec (实现面); StringCodec→JsonCodec 跨包证明分层非隔离 | Codec.java:30 |
| q2 | 复合委托 | 三委托 mapKey/mapValue/value; 2 参 value=null 合法 — Stream 结构只玩 map 语义 | CompositeCodec:30-45, ReliableTopic:82 |
| q3 | 默认 Kryo5 | 三池化 (Kryo 1024/512/512); ClassAndObject 类型内置; 4.0.0 Jackson optional 成默认根因 | Kryo5Codec:97-236, Config:165 |
| q4 | 压缩装饰 | LZ4/Snappy/ZStd 外层压缩字节+内层序列化对象; decompressionSize 头; 默认内层 Kryo5 | LZ4CodecV2:44-96 |
| q5 | 类型保真 | 丢类型 (readValue Object.class) / 指定类型 (TypeReference) / 内置类型 (ClassAndObject) 三档 | JsonJackson:105, Typed:40 |
| q6 | 原型零开销 | 已知类型直走 ByteBuf (writeCharSequence) 零分配; 命令参数/元数据用原型 | StringCodec:44 |
| q7 | 线程安全 | 非线程安全池化 (Kryo), 线程安全单例共享 (ObjectMapper), 按型缓存 (Schema) | Kryo5:123, JsonJackson:57 |
| q8 | 命令衔接 | codec 显式传参不进 RedisCommand; 响应侧 selectDecoder 从 CommandData 取 codec; null 兜底 StringCodec | CommandDecoder:565-575 |

→ 引出 RD-4: 命令流水线在 codec 支撑下执行 — [[rd4-command]]