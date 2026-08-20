# 闭环笔记 q6: 原型 vs 通用 — StringCodec 零开销与分界线

## 假设
client/codec 的原型 (StringCodec 等) 走 Netty ByteBuf 直接读写 → 零对象分配; 与 Kryo5 的通用序列化分界在"类型是否已知 + 是否需要对象图"。

## 验证过程
- StringCodec (34): `INSTANCE = new StringCodec()` 单例 (L36); Encoder `out.writeCharSequence(in.toString(), charset)` (L44, 零分配), Decoder `buf.toString(charset) + readerIndex(可读尾部)` (L52-53)
- 其他原型: LongCodec/IntegerCodec/DoubleCodec 同理 — 原生类型直转 ByteBuf
- 关键: StringCodec **implements JsonCodec** (L34) — 既是 client 原型又是 Json 兼容 → 通用性
- 分界线: 
  - **已知类型** (String/Long/Integer/byte[]) → 原型 (零序列化开销, 输出即网络格式)
  - **任意对象** → Kryo5/Jackson (序列化语义, 类型/对象图)
- 用法: RedisCommands 的命令参数用原型 (LongCodec for TTL, StringCodec for key); 结构存储值用业务 codec

## 代码类型
Implementation (零开销原型) — 网络格式与对象格式的分段

## 跨域关联
- RD-4 (命令参数) → 命令 key/参数用原型
- R-4 SDS (服务端字符串) → 对照: sds 是服务端 string 存 object/文名, 客户端 StringCodec 是字节直传

## 结论
原型 = 已知类型的零开销直传 (写 CharSequence/数值→ByteBuf, 输出即 RESP 负载); 通用 = 类型/对象图序列化。分界在"值是否由原始类型承载"。这解释为什么命令元数据 (key/TTL 参数) 永远用原型 — 它们不该被序列化成对象。
源码位置: StringCodec.java:36,44,52-53; LongCodec/IntegerCodec (client/codec)