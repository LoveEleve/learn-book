# G-1 ProtoBuf 与 Stub 生成 — 知识规划 (KP)

> 域级: 🔴 A (定义特征: .proto→protoc→Stub 是 gRPC 的一切调用载体) | 模块: stub/ (18) + protobuf/ (6) + compiler (C++ 3) + 压缩帧面 (new Codec.Gzip()/MessageFramer/MessageDeframer)
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(CRTP) q2(blocking V1/V2) q3(适配器) q4(marshaller) q5(compiler) q6(ServerCalls) q7(帧格式) q8(工具面)

## 一、机制提取 (逐源)

### M1 代码生成面 (compiler, C++ protoc 插件)
- StubType 枚举: ASYNC_INTERFACE/ASYNC_CLIENT_IMPL/FUTURE_CLIENT_IMPL/BLOCKING_CLIENT_IMPL/**BLOCKING_V2_CLIENT_IMPL** (java_generator.cpp:585-594) — 4 种客户端 stub + 服务端接口
- 生成类型 (golden TestService.java.txt 实证): TestServiceGrpc (final, L14) = 静态 MethodDescriptor (volatile + **DCL 双重检查锁** L31-35) + 4 stub 工厂 (L288 newBlockingV2Stub) + **AsyncService 接口** (L335) + TestServiceImplBase (L433, BindableService+AsyncService) + 4 stub 类
- **MethodHandlers** (java_generator.cpp:937-1000): 一个类 implements 全部 4 种 ServerCalls.Method 接口, **switch(methodId) 分派** (L966-985); 方法 id 按 client_streaming **稳定排序使 switch 表紧凑** (L941-948)
- StubFactory 匿名内部类 (L566-585); @GrpcGenerated (L1366) 标记生成代码
- 每方法 MethodDescriptor: setType/setFullMethodName(generateFullMethodName)/ProtoUtils.marshaller/setSchemaDescriptor(MethodDescriptorSupplier) + **@RpcMethod 注解** (fullMethodName/requestType/responseType/methodType)

### M2 AbstractStub CRTP 三形态 (q1)
- `AbstractStub<S extends AbstractStub<S>>` (AbstractStub.java:54) — 泛型自引用; build() 抽象 (L105)
- **全部 with* = 复制新实例**: withDeadlineAfter (L151-152)/withExecutor (L168)/withCompression (L180)/withCallCredentials (L224)/withWaitForReady (L238) — 不可变链式
- newStub 静态工厂 + StubFactory (L114-129); 三形态叶子: AbstractAsyncStub (69)/AbstractBlockingStub (70)/AbstractFutureStub (70)
- 生成 stub 类委托: `getChannel().newCall(getUnaryCallMethod(), getCallOptions()), request, responseObserver` (golden L469)

### M3 ClientCalls 客户端分派 (q2/q3)
- async 4 形态 (L81-127): asyncUnaryCall/asyncServerStreamingCall/asyncClientStreamingCall/asyncBidiStreamingCall
- **blocking V1**: blockingUnaryCall(call,req) = futureUnaryCall+getUnchecked (L140-142); (channel,..) 版本 = **ThreadlessExecutor + waitAndDrain 循环** (L155-183) — 回调在调用线程执行, 不占线程; 中断时 cancel 并等 onClose (L170)
- **blocking V2 (issue 10918, @ExperimentalApi L247)**: blockingV2UnaryCall = checked StatusException (L192-200); blockingV2ServerStreamingCall = BlockingClientCall.sendSingleRequest+halfClose (L248-256); blockingBidiStreamingCall = ThreadSafeThreadlessExecutor + BlockingClientCall (L300-312)
- V1 问题自述: "the iterator can result in leaks if not completely consumed" (L224)
- futureUnaryCall (L321): GrpcFuture (L648) + UnaryStreamToFuture (L603)

### M4 适配器矩阵 (q3)
- **CallToStreamObserverAdapter** (L443): 用户写方向 onNext→sendMessage (L468)/onError→cancel (L473)/onCompleted→halfClose (L479); **freeze() 冻结窗口** (L460-462, start 后禁改配置 L490-507); **unary request(2) 双响应额度** (L515-518, "misbehaving server"); disableAutoRequestWithInitial 自定义流控 (L503)
- **StreamObserverToCallListenerAdapter** (L535): 响应方向; **ClientResponseObserver.beforeStart 钩子** (L547-552); **firstResponseReceived 多响应检测** (L562, unary 第二个消息抛 INTERNAL)
- StartableListener (L439) onStart 钩子; BlockingResponseStream (L684) 阻塞队列→Iterator

### M5 ServerCalls 服务端分派 (q6)
- asyncUnaryCall→UnaryServerCallHandler (L49-52, L112); asyncClientStreamingCall/asyncBidiStreamingCall→StreamingServerCallHandler (L69-82, L219)
- **startCall: call.request(2)** 对称双响应防护 (L131-134, "misbehaving client")
- **延迟调用**: onMessage 存请求 (L166), **onHalfClose 才 method.invoke** (L182, "make sure the client half-closes") + freeze (L184) + onReady 补偿 (L185-189)
- 防护: TOO_MANY_REQUESTS (L155-161)/MISSING_REQUEST (L174-179); 取消/完成钩子 (L193-215)
- 4 种 Method 接口 (L87-110): Unary/ServerStreaming/ClientStreaming/BidiStreaming — compiler MethodHandlers 的目标

### M6 ProtoUtils/MessageMarshaller (q4)
- ProtoUtils 门面委托 ProtoLiteUtils (ProtoUtils.java:54-56) → MessageMarshaller (ProtoLiteUtils.java:133)
- **ThreadLocal<Reference<byte[]>> 缓冲复用** (L136, L197-200, WeakReference 防泄漏)
- **内存传输零拷贝**: parser 相同则直接返回消息对象 (L168-188, "protobufs are immutable")
- **setSizeLimit(Integer.MAX_VALUE)** (L229) — 尺寸限制上移 ClientCall 层; **setRecursionLimit** (L231-232, marshallerWithRecursionLimit @since 1.56.0)
- checkLastTagWas(0) 干净结束 (L246); 错误映射 INTERNAL "Invalid protobuf byte sequence" (L238)
- keyForProto: fullName + "-bin" (ProtoUtils.java:75-79, BINARY_HEADER_SUFFIX)

### M7 消息帧与压缩 (q7)
- **帧格式**: [1B 压缩标志][4B 长度][消息体] (MessageDeframer.java:44-45,384-393; MessageFramer.java:70-72,226-247)
- **双重尺寸校验**: 压缩前 (L396 "exceeds maximum size") + 解压后 (L529 "Decompressed... exceeds") — 压缩炸弹防护; 无可靠方法预知解压后大小 (L412-413)
- **Compressor SPI**: Codec.Identity.NONE 默认 (MessageFramer.java:82), setCompressor (L110), 压缩标志 `messageCompression && compressor != NONE` (L139); **new Codec.Gzip() (CompressorRegistry.java:34; 内部类 Codec.java:35, JDK GZIP 流)**
- 压缩协商经 grpc-encoding header (G-3 验证)

### M8 工具面三件套 (q8)
- MetadataUtils: newAttachHeadersInterceptor (L50)/newCaptureMetadataInterceptor (L92)/newAttachMetadataServerInterceptor (L168) — 拦截器工厂
- StreamObservers: copyWithFlowControl 尊重目标流控 (L56-99)/nextAndComplete (L36)
- StatusProto: google.rpc.Status ↔ gRPC 异常桥 (toStatusRuntimeException L51/fromThrowable L153); **grpc-status-details-bin** trailer (L37) — Rich Error Model

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 (核心) | M1 代码生成面 / M3 blocking ThreadlessExecutor / M4 适配器冻结+request(2) / M6 marshaller 零拷贝+递归限制 / M7 帧格式+压缩炸弹防护 |
| P2 (重要) | M2 CRTP 不可变链式 / M5 服务端延迟调用 / M8 工具三件套 |
| P3 (对照) | StatusProto Rich Error Model / BlockingV2 演进 (issue 10918) |

## 三、叙事线

场景: 写一个 .proto → 编译出 Stub → 调用一个 RPC。读者疑问链: 生成代码长什么样 (M1) → stub 为什么能链式配置 (M2) → 调用怎么分派到回调/阻塞/Future 三通道 (M3/M4) → 服务端怎么接 (M5) → 消息怎么序列化/压缩/保护 (M6/M7) → 工具面收尾 (M8)。
