# 闭环笔记 q2: CompositeCodec 三委托 — valueCodec=null 合法性与 Stream 场景

## 假设
CompositeCodec 的 valueCodec=null (2 参构造) 是合法用法: 某些结构 (Stream) 只走 map 语义, 永不触 value 路径, 所以不是 NPE bug。

## 验证过程
- CompositeCodec (org.redisson.codec:30-45): `mapKeyCodec/mapValueCodec/valueCodec` 三字段; 2 参构造 `this(mapKey, mapValue, null)` (L36-38), 3 参 (L40-45)
- getValueDecoder (L74-81) `return valueCodec.getValueDecoder()` — **无 null 兜底** → 若碰 value 且 value=null → NPE
- 实际调用点:
  - `RedissonReliableTopic.java:82`: `new CompositeCodec(StringCodec.INSTANCE, codec)` (2参) — **value=null 合法场景**
  - `RedissonBuckets.java:83`: `new CompositeCodec(StringCodec.INSTANCE, codec, codec)` (3参) — value 也给了
  - `RedissonMultimap.java:422`: 3参
- ReliableTopic 用 Stream 存储 (RedissonReliableTopic:82): **Stream 本质 hash-like (field=消息ID, value=消息体), 读写全走 mapKey/mapValue 编解码** → value 路径永不进入 → null 安全
- Stream 的 entryId (String) + 消息体 (业务 codec) = StringCodec(mapKey) + codec(mapValue) 天然匹配

## 代码类型
Implementation (委托组合) — 边界契约

## 跨域关联
- RD-5 (RMap) → map 语义消费 mapValue
- RD-1/RD-6 → 订阅消息 (EventsSubscribeService) 用普通 value codec

## 结论
CompositeCodec value=null 非 bug 是设计: **委托构图匹配"结构动词面"**: Stream/Map 类结构只用 map 语义 (所以 2 参够), Buckets 等才用 value。契约是"使用者保证不碰 value 路径"。选型表: 2参=Stream/Map, 3参=混合。
源码位置: CompositeCodec.java:30-45,74-81; RedissonReliableTopic.java:82; RedissonBuckets.java:83