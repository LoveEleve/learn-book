# 闭环笔记 Q4 — ProtoUtils/MessageMarshaller: 零拷贝 + 防护三元组

假设: marshaller 不只是 parse/stream 封装 — 内含性能优化 (线程本地缓冲/内存零拷贝) 与安全防护 (递归深度限制/干净结束检查)。

验证过程:
- grep `ProtoUtils.marshaller` → 门面委托 ProtoLiteUtils (ProtoUtils.java:54-56), 实现在 protobuf-lite 模块 MessageMarshaller (ProtoLiteUtils.java:133)
- **ThreadLocal<Reference<byte[]>> bufs** (L136) + 复用逻辑 (L197-200): 已知长度消息复用线程本地缓冲, WeakReference 防 ThreadLocal 泄漏
- **内存传输零拷贝** (L168-188): `if (stream instanceof ProtoInputStream)` 且 `protoStream.parser() == parser` → **直接返回消息对象** (L180-181) — "Optimization for in-memory transport. Returning provided object is safe since protobufs are immutable" — inprocess/测试等内存传输免序列化
- **setSizeLimit(Integer.MAX_VALUE)** (L229): gRPC 侧取消 protobuf 尺寸限制 — **消息大小限制上移到 ClientCall 层** (maxInboundMessageSize, G-3)
- **setRecursionLimit(recursionLimit)** (L231-232): 递归深度限制 (marshallerWithRecursionLimit 入口, ProtoUtils.java:65, @since 1.56.0) — 防深度嵌套 protobuf DoS (protobuf 默认 100)
- **checkLastTagWas(0)** (L246): 解析后验证流干净结束
- 错误映射: InvalidProtobufBufferException → `Status.INTERNAL "Invalid protobuf byte sequence"` (L238-239) — 协议错误 → 内部错误码

代码类型: Implementation (性能 + 安全实现)

结论: marshaller 三防护: ① 递归深度限制 (recursionLimit, 1.56 引入) ② 干净结束检查 (checkLastTagWas) ③ 尺寸限制上移 (Integer.MAX_VALUE, 由 ClientCall 层管); 两优化: ① ThreadLocal 缓冲复用 (WeakReference) ② in-memory 传输零拷贝 (parser 相同则直接返回对象 — protobuf 不可变性是前提)。 (ProtoLiteUtils.java:133-246, ProtoUtils.java:54-67)
