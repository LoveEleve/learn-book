# 闭环笔记 q1: 双包边界 — Codec 接口为什么在 client/codec

## 假设
Codec 接口放 `org.redisson.client.codec`（网络协议层）是因为"编解码"在 Redisson 里本质是**协议面**——客户端与 Redis 之间传输的字节如何互相转换，属于 client 包的职责；`org.redisson.codec` 是实现细节。

## 验证过程
- `Codec` 接口定义在 `client/codec/Codec.java:30` — 同包有 BaseCodec + 9 个"原型" (StringCodec/LongCodec/IntegerCodec/DoubleCodec/BitSetCodec/ByteArrayCodec/TDigestDoubleCodec)
- `org.redisson.codec` (36 文件) — 高层实现: Kryo5/Jackson 家族/压缩族/Composite/Serialization/Protobuf
- **跨包实现证明**: `StringCodec` (client/codec:34) `implements JsonCodec` (org.redisson.codec:24 import!) — 一个 client 包的原型实现了 org.redisson.codec 的接口 → 双包是**逻辑分层**，非物理隔离
- 接口定义 (Codec.java:37-72) 全是 `getMapKeyDecoder/getMapValueDecoder/getValueDecoder` — 这 4 组恰好是"RESP 命令消息中字节 ↔ 对象"的转换面
- client 包本来就有 protocol (RedisCommand/CommandData) + handler (CommandDecoder) — Codec 放进 client 让三者同居: 协议命令/解码器/编解码契约
- 对比: org.redisson.codec 的 JsonCodec/ObjectCodec 是**向上**的抽象（给结构对象用），StringCodec 同时向下满足两者

## 代码类型
Interface (分层契约) — 逻辑归属设计

## 跨域关联
- RD-4 (命令流水线) → CommandDecoder 消费 getDecoder (CommandDecoder.java:565-575)
- r28-networking (RESP 协议) → 服务端字节共面

## 结论
双包边界 = **协议面 (client/codec: 接口+原型) vs 实现面 (org.redisson.codec: 全家桶)**; 跨包 implement (StringCodec→JsonCodec) 证明这是分层非隔离。Codec 的本质 = "命令消息字节 ↔ 对象" 的转换契约，归属 client 包天经地义。
源码位置: client/codec/Codec.java:30, client/codec/StringCodec.java:34, org.redisson/codec/JsonCodec.java