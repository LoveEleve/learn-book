# G-1 Pass 1 扫描笔记 — ProtoBuf 与 Stub 生成

> 日期: 2026-08-16 | 版本: 1.83.1 | 🔴 A | 模块: stub/ (18) + protobuf/ (6) + compiler (C++ 3) + 压缩面

## 继承树/调用图

```
.gproto → protoc → java_generator.cpp (PrintService L1203)
  → TestServiceGrpc (final, 生成代码 golden)
    ├── 静态 MethodDescriptor (DCL 懒加载, volatile + synchronized)
    ├── newStub/StubFactory → AbstractStub 子类
    ├── AbstractAsyncStub<TestServiceStub> ← TestServiceStub
    ├── AbstractBlockingStub<TestServiceBlockingStub> ← TestServiceBlockingStub
    └── AbstractFutureStub<TestServiceFutureStub> ← TestServiceFutureStub

AbstractStub<S extends AbstractStub<S>> (CRTP)
  ├── AbstractAsyncStub (69) → asyncUnaryCall (ClientCalls:81)
  ├── AbstractBlockingStub (70) → blockingUnaryCall (ClientCalls:140/192)
  └── AbstractFutureStub (70) → futureUnaryCall (ClientCalls:321)

ClientCalls (997)
  ├── 适配器: CallToStreamObserverAdapter (L443) / StreamObserverToCallListenerAdapter (L535)
  ├── 适配器: UnaryStreamToFuture (L603) / GrpcFuture (L648) / BlockingResponseStream (L684)
  ├── 适配器: BlockingClientCall (63) — V2 阻塞调用
  └── StartableListener (L439)

ServerCalls (506) — 服务端: asyncUnaryCall (L49) → MethodHandler → 拦截
ProtoUtils (protobuf/6) — marshaller(defaultInstance) (L54) → ProtobufMessageMarshaller
MessageDeframer (550)/MessageFramer (45) — 帧格式 1B 标志 + 4B 长度 + new Codec.Gzip() (CompressorRegistry.java:34, 内部类 Codec.java:35)
```

## 基本元素分解

1. **AbstractStub 族** (CRTP 泛型自引用): 每个 with*(deadline/interceptors/compression...) 都 `build(channel, callOptions)` 新实例 (AbstractStub:141-260),链式不可变
2. **ClientCalls 分派面**: async 4 形态 (L81-127) + blocking V1 (L140-228) + blocking V2 BlockingClientCall (L248-300) + future (L321)
3. **适配器矩阵**: 双向转换 StreamObserver ↔ ClientCall.Listener, 阻塞 ↔ 回调 ↔ Future 三通道
4. **ProtoUtils**: marshaller 用 defaultInstance 的 parser (protobuf 消息 ↔ InputStream), -bin metadata key
5. **compiler (C++ protoc 插件)**: PrintStub (L600)/PrintAbstractClassStub (L908)/PrintMethodHandlerClass (L937)/PrintBindServiceMethod (L1144) + @RpcMethod/@GrpcGenerated
6. **消息帧**: 1B 压缩标志 + 4B 长度前缀, gzip 压缩经 new Codec.Gzip() (CompressorRegistry.java:34, 内部类 Codec.java:35)

## 标记问题 (7)

1. **Q1 CRTP 链式**: 三形态 stub 用 `S extends AbstractStub<S>` 自引用泛型,with* 每次 build 新对象 — 为什么不用可变 builder?链式调用链长度有代价吗? (AbstractStub:54,105)
2. **Q2 blocking V1/V2 两代**: blockingUnaryCall (L140) 与 blockingV2UnaryCall (L192) 并存,BlockingClientCall (63) 是什么?为什么 1.83 引入 V2?老 API 的阻塞实现 (BlockingResponseStream L684) 有什么问题?
3. **Q3 适配器矩阵**: CallToStreamObserverAdapter (L443) 怎么桥接 StreamObserver 接口与 ClientCall.Listener 回调?双向各自解决什么问题?
4. **Q4 ProtoUtils marshaller**: marshallerWithRecursionLimit (L65) 的 recursion limit 是什么?protobuf 解析的防护?keyForProto (L75) 为什么 -bin 后缀?
5. **Q5 compiler 生成面**: 一个 service 生成哪些类/方法?StubFactory 是什么?@RpcMethod 注解数据怎么被运行时消费 (ServerCalls/ClientCalls)?
6. **Q6 ServerCalls 分派**: asyncUnaryCall (L49) 如何把 ServerCall 包成 StreamObserver?四种 method type 怎么分派?拦截器 (ServerInterceptor) 在哪层?
7. **Q7 消息帧与压缩**: MessageDeframer 的 1B 压缩标志+4B 长度怎么解析?压缩协商 (grpc-encoding header) 在哪层?RecursionLimit 与消息大小的关系?

## 已读测试

- ClientCallsTest.java (980 行): unaryBlockingCallSuccess (L126) 用 NoopClientCall 模拟回调 → 验证阻塞解包; blockingUnaryCall2_interruptedWaitsForOnClose (L195) — 中断时等待 onClose; blockingUnaryCall_HasBlockingStubType (L273)
- ProtoUtilsTest.java (48 行): testRoundtrip (L36) marshaller 往返; keyForProto (L44) "-bin" 后缀
