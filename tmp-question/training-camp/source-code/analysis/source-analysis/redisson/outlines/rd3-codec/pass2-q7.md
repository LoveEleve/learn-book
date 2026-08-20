# 闭环笔记 q7: 线程安全契约 — Kryo 池化 vs Jackson 单例 vs Schema 缓存

## 假设
不同 codec 的线程安全策略: Kryo 非线程安全 → 池化; Jackson ObjectMapper 线程安全 → 单例共享; Protostuff RuntimeSchema 线程安全 → 按类缓存。

## 验证过程
- Kryo5Codec: 非线程安全 → `Pool<Kryo>(true, false, 1024)` (L123) 池化拿取; encoder 每次 obtain/free (L197-208/218-233) — **"每次操作一个私有实例"**
- JsonJacksonCodec: `static final INSTANCE` + `protected final ObjectMapper mapObjectMapper` (L57/82) — **单例 + 共享**; Jackson 官方保证 ObjectMapper 读写线程安全 (构建后只读配置)
- ProtobufCodec (43): `RuntimeSchema.getSchema(clazz)` (L189/196) — **按类型缓存 Schema, 读操作线程安全**
- Input/Output 缓冲池 (512 上限) 也是线程安全策略: 缓冲复用不随请求分配
- 失败归还协议: Kryo5 decoder `if (success) inputPool.free(input)` (L207-208) — 失败实例不回池 (防污染)

## 代码类型
Implementation (并发策略) — 三类典型

## 跨域关联
- Q3 (Kryo 池化) → 详情
- RD-1 事件循环 (多线程) → 线程安全是并发调用前提
- 面试题: "Kryo 为什么不线程安全?ObjectMapper 呢?"

## 结论
三策略: **池化 (非线程安全者, Kryo 实例/Input/Output) → 单例共享 (线程安全者, ObjectMapper) → 缓存 (按类型 Schema)**。通用原则: 有状态序列化器池化, 无状态配置共享。
源码位置: Kryo5Codec.java:123,134,149; JsonJacksonCodec.java:57; ProtobufCodec.java:189,196