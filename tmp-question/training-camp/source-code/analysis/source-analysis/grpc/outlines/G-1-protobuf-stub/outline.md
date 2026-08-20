# G-1 ProtoBuf 与 Stub 生成 — 一行 .proto, 一座 RPC 工厂: 从 IDL 到可调用的 Stub

> 前置: 无 (叶子域) | 引出: [[G-2-服务端]] (ServerCalls 消费生成面) + [[G-3-客户端]] (ClientCalls/MessageDeframer 消费) | 对照: protobuf 二进制格式 + gRPC-web
> 🔴 A | 8 KP | [模式: 代码生成 + 适配器 + 帧协议]
> Pass 2 闭环: q1(CRTP) q2(blocking) q3(适配器) q4(marshaller) q5(compiler) q6(ServerCalls) q7(帧格式) q8(工具面)

**读者处境**: 你在 .proto 里写 `rpc SayHello(HelloRequest) returns (HelloReply)`, 跑一个 protoc, 拿到 TestServiceGrpc.java。IDE 里敲 `stub.sayHello(...)` 就完成一次 RPC。这中间到底生成了什么?为什么调用是"异步"的, 却又能"阻塞"返回?消息是怎么变成线上字节的?

### 1. 代码生成面 — 一个 service 编译出 7 个类型

场景: 编译产物长什么样?为什么生成代码那么啰嗦?
源码路径:
- **StubType 枚举 5 种** (java_generator.cpp:585-594): ASYNC_INTERFACE (服务端) + 4 种客户端 (Async/Future/Blocking/**BlockingV2**)
- golden 实证: `TestServiceGrpc` (**final + 私有构造** = 纯静态门面, TestService.java.txt:14-15) = 静态 **MethodDescriptor DCL 双重检查锁** (TestService.java.txt:31-35) + 4 个 stub 工厂 (TestService.java.txt:288 newBlockingV2Stub) + **AsyncService 接口** (TestService.java.txt:335) + TestServiceImplBase (TestService.java.txt:433)
- **MethodHandlers**: 一个类 implements 全部 4 种 Method 接口, `switch(methodId)` 分派 (java_generator.cpp:937-1000); 方法 id 按 client_streaming 稳定排序 (java_generator.cpp:941-948)
- 每方法: setType/setFullMethodName/**ProtoUtils.marshaller**/setSchemaDescriptor + **@RpcMethod 注解** (TestService.java.txt:23-29)
关键设计 (q5): **生成代码啰嗦是刻意的** — volatile + DCL 让 MethodDescriptor 零初始化开销又线程安全; MethodHandlers 用 switch 表而非每方法一个类, 服务端分派 O(1); @RpcMethod 注解供工具/拦截器反射读取。 [模式: 静态注册表]

### 2. 三形态 stub — CRTP 链式不可变

场景: `stub.withDeadlineAfter(5, SECONDS).withCompression("gzip")` 链式调用, 每次返回新 stub — 为什么不是可变配置?
源码路径:
- `AbstractStub<S extends AbstractStub<S>>` (AbstractStub.java:54) 泛型自引用; build() 抽象 (AbstractStub.java:105)
- **全部 with* = 复制新实例**: withDeadlineAfter (AbstractStub.java:151-152)/withInterceptors (AbstractStub.java:215)/withWaitForReady (AbstractStub.java:238)
- 三形态叶子: AbstractAsyncStub (69 行)/AbstractBlockingStub (70)/AbstractFutureStub (70); 差异只在 build() 返回类型
- **选择指南**: 阻塞 = 同步编程心智 (服务端密集); 异步 = 回调/背压控制; Future = 异步 + 可取消/组合
- 生成 stub 委托: `getChannel().newCall(getUnaryCallMethod(), getCallOptions())` (TestService.java.txt:469)
关键设计 (q1): **不可变链式 = 线程安全免费** — stub 可被任意共享; 每个 with* 只换 CallOptions 引用 (不可变, 引用复用); CRTP 让 with* 返回精确子类型, 用户无需强转。 [模式: 不可变构建器] [并发: 引用赋值原子性]

### 3. 客户端分派 — 异步/阻塞/Future 三通道

场景: 同一 ClientCall, 三种调用风格怎么共存?
源码路径:
- async 4 形态 (ClientCalls.java:81-127): 回调通道
- **blocking V1**: (channel,...) 版用 **ThreadlessExecutor + waitAndDrain 循环** (ClientCalls.java:155-183) — 回调在调用线程直接执行, **不占额外线程**; 中断时 `call.cancel("Thread interrupted")` (ClientCalls.java:169) 后**等待 onClose** (ClientCalls.java:170, 让拦截器清理)
- **blocking V2 (issue 10918)**: blockingV2UnaryCall = checked StatusException (ClientCalls.java:192-200); blockingBidiStreamingCall = ThreadSafeThreadlessExecutor + **BlockingClientCall 对象** (ClientCalls.java:300-312) — read/write/cancel 直接控制
- V1 泄漏自述: "iterator can result in leaks if not completely consumed" (ClientCalls.java:224)
- future: GrpcFuture 继承 AbstractFuture (ClientCalls.java:648)
关键设计 (q2): **ThreadlessExecutor 是阻塞调用的灵魂** — 回调在调用线程内联执行, 阻塞调用零线程占用; V2 修复 V1 三问题 (Iterator 泄漏/无 cancel/无双向); 中断时等 onClose 是"优雅取消" (拦截器链必须看到完整关闭)。 [并发: 线程内联执行 vs 线程池]

### 4. 适配器矩阵 — StreamObserver ↔ ClientCall.Listener 双向桥

场景: 底层回调是 onMessage/onClose, 用户接口是 onNext/onCompleted — 谁翻译谁?
源码路径:
- **CallToStreamObserverAdapter** (ClientCalls.java:443): onNext→`call.sendMessage` (ClientCalls.java:468)/onError→`call.cancel` (ClientCalls.java:473)/onCompleted→`call.halfClose` (ClientCalls.java:479)
- **freeze() 冻结窗口** (ClientCalls.java:460-462): start 后改配置抛 IllegalStateException (ClientCalls.java:490-507), 提示 "Use ClientResponseObserver" — **beforeStart 钩子** (ClientCalls.java:547-552)
- **unary request(2)**: "ask for two responses from flow-control so that if a misbehaving server sends more than one response, we can catch it" (ClientCalls.java:515-518)
- **StreamObserverToCallListenerAdapter** (ClientCalls.java:535): 响应方向 + **firstResponseReceived 多响应检测** (ClientCalls.java:562, 第二个消息抛 INTERNAL)
关键设计 (q3): **适配器是协议守卫** — 不只翻译, 还执行协议不变式: unary 恰好一条响应 (双向 request(2) + 多响应检测); 冻结窗口把配置窗口锁死在 start 前; aborted/completed 检查防流终止后写入 (ClientCalls.java:466-467)。 **被放弃的方案: 让用户直接实现 ClientCall.Listener** — gRPC 选择提供 StreamObserver 抽象, 把底层回调协议 (onHeaders/onMessage/onClose 顺序) 与超集能力 (isReady 背压/取消) 全部封装, 用户只写三个方法。 [模式: 门面适配器]

### 5. 服务端分派 — onHalfClose 才调用业务方法

场景: 服务端收到请求, 什么时候真正执行你的 serviceImpl 方法?
源码路径:
- asyncUnaryCall → UnaryServerCallHandler (ServerCalls.java:49-52, L112); streaming → StreamingServerCallHandler (ServerCalls.java:219)
- **startCall: call.request(2)** 对称双响应防护 (ServerCalls.java:131-134, "misbehaving client")
- **延迟调用**: onMessage 只存请求 (ServerCalls.java:166), **onHalfClose 才 method.invoke** (ServerCalls.java:182) — "make sure the client half-closes"
- invoke 后 freeze (ServerCalls.java:184) + onReady 补偿 (ServerCalls.java:185-189, "missed the onReady event")
- 防护: TOO_MANY_REQUESTS (ServerCalls.java:155-161)/MISSING_REQUEST (ServerCalls.java:174-179); onCancel→cancelled 标志 (ServerCalls.java:193-200)
- 业务异常路径: serviceImpl 抛异常 → 适配器捕获转 `call.close(Status)` — 详细映射在 G-2 展开
关键设计 (q6): **延迟到 halfClose 调用 = 客户端语义完整性** — unary 的协议是"一请求+半关闭", 服务端在完整收到后执行, 与客户端 request(2) 形成闭环守卫; 冻结与客户端对称, 配置一致性协议贯穿两端。 **被放弃的方案: onMessage 收到即调用** — 那样服务端可能在客户端尚未 halfClose 时就执行业务 (丢失流语义), 且无法在收到第二条违规消息前拒绝。 [模式: 状态机延迟执行]

### 6. 序列化 marshaller — 零拷贝与防护三元组

场景: HelloRequest 对象怎么变成流?解析时怎么防攻击?
源码路径:
- ProtoUtils 门面 → MessageMarshaller (ProtoLiteUtils.java:133)
- **ThreadLocal<Reference<byte[]>> 缓冲复用** (ProtoLiteUtils.java:136, L197-200, WeakReference 防泄漏)
- **内存传输零拷贝**: `parser() == parser` 则直接返回对象 (ProtoLiteUtils.java:168-188) — "protobufs are immutable"
- **setSizeLimit(Integer.MAX_VALUE)** (ProtoLiteUtils.java:229): 尺寸限制上移 ClientCall 层; **setRecursionLimit** (ProtoLiteUtils.java:231-232) — 递归深度防 DoS
- checkLastTagWas(0) (ProtoLiteUtils.java:246); 错误映射 INTERNAL "Invalid protobuf byte sequence" (ProtoLiteUtils.java:238)
关键设计 (q4): **三个防护**: 递归限制 (1.56 引入, 防深度嵌套 DoS)/干净结束检查/尺寸限制上移 (责任分层: protobuf 只管解析, 大小由传输层管); **两个优化**: ThreadLocal 缓冲 (弱引用防泄漏)/内存零拷贝 (immutable 前提)。 **被放弃的方案: 直接传 byte[]** — marshaller 抽象让"对象↔流"的转换可插拔 (protobuf/json/自定义), 且保留零拷贝优化点; ThreadLocal 弱引用 vs 强引用池 — 弱引用避免跨 RPC 的大缓冲长期驻留。 [序列化: protobuf varint/嵌套结构] [安全: 递归攻击]

### 7. 消息帧 — 5 字节头与压缩炸弹

场景: 序列化后的消息在线路上长什么样?gzip 压缩的"炸弹"怎么防?
源码路径:
- **帧格式**: [1B 压缩标志][4B 长度][消息体] (MessageDeframer.java:44-45, L384-393; MessageFramer.java:70-72, L226-247)
- **双重尺寸校验**: 压缩前 (MessageDeframer.java:396) + **解压后** (MessageDeframer.java:529, "Decompressed... exceeds maximum size") — 压缩炸弹防护; 无法预知解压后大小 (MessageDeframer.java:412-413)
- **Compressor SPI**: Codec.Identity.NONE 默认 (MessageFramer.java:82); **gzip = `new Codec.Gzip()` (CompressorRegistry.java:34; 内部类 Codec.java:35, JDK GZIP 流)**
- **协商头 grpc-encoding**: 压缩器经 MESSAGE_ENCODING_KEY ("grpc-encoding", GrpcUtil.java:105-106,192) 在流头中协商 — 发送方声明消息体用的压缩器, 接收方按头选解压器 (AbstractClientStream.java:324 读头)
关键设计 (q7): **压缩标志位 + 长度前缀 = gRPC-over-HTTP/2 的消息封装层**; 双重校验把压缩炸弹 (zip bomb: 小帧大解压) 掐死在第二道; 尺寸限制唯一执行点在此 — protobuf 层已放开。 **被放弃的方案: 流级压缩协商 (一次握手全程压缩)** — 逐帧标志允许同一流内按消息开关压缩 (如大消息压缩小消息不压), 且保持无状态解帧; 代价是每帧 1 字节开销。 [HTTP/2: DATA 帧内嵌 gRPC 帧] [安全: zip bomb]

### 8. 工具面收尾 — 头注入/流控拷贝/错误详情

场景: 静态头怎么加?错误详情 (grpc-status-details-bin) 是什么?
源码路径:
- MetadataUtils: newAttachHeadersInterceptor (MetadataUtils.java:50)/newCaptureMetadataInterceptor (MetadataUtils.java:92)/服务端版 (MetadataUtils.java:168)
- StreamObservers: copyWithFlowControl 尊重目标流控 (StreamObservers.java:56-99)
- StatusProto: google.rpc.Status ↔ 异常桥 (StatusProto.java:51/L153); **grpc-status-details-bin** trailer (StatusProto.java:37) — Rich Error Model
关键设计 (q8): 头注入走**拦截器工厂**不改 call 语义; 流控拷贝防背压溢出; Rich Error Model 让错误带结构化 details。 [序列化: google.rpc.Status 扩展]

### 核心悬念

"异步的 gRPC 调用怎么做到'阻塞'返回, 还不占一个线程?" — 下一域 [[G-2-服务端]] 会看到这些 Stub/Handler 如何在 ServerImpl 里注册分派; [[G-3-客户端]] 将展开 ThreadlessExecutor 的调用链与 MessageDeframer 的帧处理落点。

### 负面空间 (不做)

1. 不写 protobuf 语法本身 (varint/字段编码) — 只讲 gRPC 的封装层
2. 不写 protoc 命令行/构建集成 (gradle plugin)
3. 不写四种 MethodType 的语义细节 (各域的流式语义在 G-2/G-3 展开)
4. 不写 Metadata 二进制编码 (BINARY_HEADER_SUFFIX 机制在支撑面)
5. 不写 StatusRuntimeException 全 API (支撑面)
6. 不写压缩算法细节 (gzip 是 JDK 流, 只讲协商与帧标志)
