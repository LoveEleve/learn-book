# 闭环笔记 q5: 类型保真战线 — Object.class vs TypeReference vs ClassAndObject

## 假设
三种序列化策略对应三种类型保真度: JsonJackson 丢类型 (反序列化到 Object), TypedJsonJackson 由调用方指定类型 (TypeReference), Kryo5 自带类型 (ClassAndObject)。

## 验证过程
- JsonJacksonCodec (54): `mapObjectMapper.readValue(stream, Object.class)` (L105) — **类型全丢**, 反读需调用方已知类型 (RedisTemplate 语义)
- TypedJsonJacksonCodec (40, extends JsonJacksonCodec): 字段 `valueTypeReference/mapKeyTypeReference/mapValueTypeReference` (L73-75) + `createDecoder(valueClass, valueTypeReference)` (L57-63): `readValue(stream, valueTypeReference)` — **调用方构造时指定泛型类型**
- Kryo5Codec (62): `kryo.writeClassAndObject / readClassAndObject` (L202/224) — **类型写进流** (类名 + 字段)
- JsonJackson 家族都在 org.redisson.codec — 通用序列化面; StringCodec 特殊 (走 JsonCodec)
- 三选型: RList<String> 等**泛型已知**用 JsonJackson; 存**异构对象** (接口/多态) 用 TypedJsonJackson 或 Kryo5; **最大自由度** (类型即对象) 用 Kryo5
- 面试对比: JSON=可读性/跨语言, 类型弱; Kryo=类型强/紧凑二进制, 不可读/Java only

## 代码类型
Interface (序列化策略) — 类型保真是核心权衡面

## 跨域关联
- RD-5 (RMap cache) → mapValueTypeReference 场景
- Q3 (Kryo) → ClassAndObject 类型内置
- 面试高频: "Redis 存对象为什么反序列化丢类型"

## 结论
类型保真三档: **丢类型 (JsonJackson, readValue Object.class) → 指定类型 (TypedJsonJackson, TypeReference 构造时定) → 内置类型 (Kryo5, ClassAndObject 写进流)**。选型: 泛型已知场景 Jackson (人类可读), 异构/多态场景 Kryo5 (类型强). 这是"序列化体验 vs 跨语言" 的经典权衡。
源码位置: JsonJacksonCodec.java:105, TypedJsonJacksonCodec.java:40,57-75, Kryo5Codec.java:202,224