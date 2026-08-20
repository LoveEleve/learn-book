# G-2 服务端 — 一行 start(), 一座 HTTP/2 服务器: 从 Builder 到优雅下线

> 前置: [[G-1-Protobuf与Stub生成]] (ServerCalls/生成面消费) | 引出: [[G-3-客户端]] (对称的 Channel 生命周期) + [[G-6-流控重试]] (UNAVAILABLE 触发重试) | 对照: Netty HTTP/2 服务端 (阶段1)
> 🔴 A | 9 KP | [模式: 委托装配 + 状态机 + 双 GOAWAY]
> Pass 2 闭环: q1(装配) q2(注册表) q3(链路) q4(拦截器) q5(状态机) q6(连接生命周期) q7(协商) q8(执行器) q9(停机)

**读者处境**: 你写 `ServerBuilder.forPort(8080).addService(new HelloServiceImpl()).build().start()`, 三行代码, 一个可服务的 gRPC 服务器就起来了。可这个服务器的另一半——HTTP/2 帧怎么接收、请求怎么路由到你的方法、服务怎么优雅下线——你一行都没写。这三行背后发生了什么?

### 1. 装配链 — Builder 的委托魔术

场景: `build()` 到底建了什么?为什么 NettyServerBuilder 里没有一行 Netty 绑定的代码?
源码路径:
- NettyServerBuilder extends **ForwardingServerBuilder**, 构造时 `new ServerImplBuilder(new NettyClientTransportServersBuilder())` (NettyServerBuilder.java:176) — **委托**: 用户面 Builder 转发给 core 面 Builder
- ServerImplBuilder.build(): `new ServerImpl(this, clientTransportServersBuilder.buildClientTransportServers(...), Context.ROOT)` (ServerImplBuilder.java:257-263) — **ServerImpl 是主体, 传输是参数**
- buildTransportServers: `new NettyServer(..., negotiator, ...)` (NettyServerBuilder.java:710-741)
- start: `transportServer.start(listener)` (ServerImpl.java:187) → NettyServer 配 ServerBootstrap (NettyServer.java:218-240)
关键设计 (q1): **依赖注入式装配** — 传输工厂以回调接口 (ServerImplBuilder.java:109) 注入, inprocess/binder 传输零改动复用 ServerImpl 全部逻辑。**被放弃的方案: NettyServerBuilder 直接 extends ServerImplBuilder** — 会把 netty 依赖打进 core。 [模式: 委托+回调注入]

### 2. 注册表 — 服务的扁平化快照

场景: `addService()` 之后, 请求怎么在一毫秒内找到你的方法?
源码路径:
- InternalHandlerRegistry (L30-78): **build 时扁平化** — 遍历每个 service 的方法, `map.put(fullMethodName, method)` (L67-78), 产出不可变 Map
- addService: 按 service 名**原子覆盖** (L59-65, "services are added/replaced atomically")
- 请求时 `lookupMethod: methods.get(methodName)` (L51-54) O(1) → fallbackRegistry (ServerImpl.java:545) → 找不到 → **UNIMPLEMENTED "Method not found: "** (ServerImpl.java:548-549)
关键设计 (q2): **构建时快照 = 无锁读**; 双注册表 (主 + fallback 反射注册) 兜底; **被放弃的方案: 请求时遍历 service 匹配** — 快照扁平化把 O(n) 降为 O(1) 哈希。 [模式: 不可变快照]

### 3. 请求接收链路 — 跨两条线程的接力

场景: HTTP/2 帧到达 Netty event loop, 你的方法跑在应用线程 — 中间怎么接力?
源码路径:
- NettyServerHandler 帧→流: inboundDataWithEndStreamShouldForwardToStreamListener (NettyServerHandlerTest.java:262) 实证
- streamCreated (ServerImpl.java:490 区): 解压器协商 (L490-497) → lookupMethod (L501) → **createContext (L503, 元数据→可取消 Context)** → **JumpToApplicationThreadServerStreamListener** (L510-513, 传输线程→应用线程跳板)
- **MethodLookup** (L524-564): serializing executor 排队 — 注释实证: "so jumpListener.setListener() is called before any callbacks are delivered" (L515-517) — **顺序保证: 回调绝不先于 setup**
- wrapMethod (L561, 拦截器包装) → maySwitchExecutor (L562) → startWrappedCall (L692-699)
关键设计 (q3): **双执行器跳转**: Netty event loop 收帧, serializing executor 排队 setup, 应用 executor 执行业务; 顺序由排队保证, 杜绝竞态。**被放弃的方案: 在传输线程直接执行用户代码** — 用户阻塞会卡死整个连接。 [并发: 线程切换点] [HTTP/2: DATA 帧→流]

### 4. 拦截器链 — 洋葱的包装次序

场景: 多个 ServerInterceptor 的执行顺序?和客户端一样吗?
源码路径:
- wrapMethod: **for 循环逐个包装** `handler = interceptCallHandlerCreate(interceptor, handler)` (ServerImpl.java:669-670)
- InterceptCallHandler (ServerInterceptors.java:250) startCall (L266) → `interceptor.interceptCall(call, headers, callHandler)` (L269) — 拦截器拿原始 call + 下一层 handler
- **次序推导**: [i0,i1,i2] → H2(i2, H1(i1, H0(i0, orig))) — **最后添加的最外层先执行** (与直觉相反)
- 包装时机: **每请求一次** (MethodLookup 里, ServerImpl.java:561) — 携带每请求 statsTraceCtx
- 拦截器可包 call: PartialForwardingServerCall (ServerInterceptors.java)
关键设计 (q4): 拦截器是 **handler 级洋葱** 而非 call 级; 请求时包装让拦截器看到每请求状态。**被放弃的方案: 注册时包装一次** — 无法携带请求级 Context。 [模式: 装饰器链]

### 5. ServerCallImpl 状态机 — headers→messages→close 单向道

场景: 服务端怎么回消息?压缩怎么协商?回错了会怎样?
源码路径:
- **sendHeadersInternal** (ServerCallImpl.java:104-133): 一次性 (checkState L105-106); **压缩协商**: 客户端 MESSAGE_ACCEPT_ENCODING 不含所选压缩器 → 降级 Identity.NONE (L111-121); unary 时 `writeHeaders(headers, !serverSendsOneMessage())` (L133, 头即终帧)
- **sendMessageInternal** (L156-174): 必须先 sendHeaders (L158, "sendHeaders has not been called"); unary 第二消息 → **TOO_MANY_RESPONSES** (L162-164, 呼应 G-1 的 request(2) 双响应防护)
- **closeInternal** (L216-230): 一次性; unary 未发消息 → **MISSING_RESPONSE** (L221-223)
- setCompression: 只能 headers 前 (L188-197)
- **忘记 close**: 流保持打开不回收 — 由传输层超时/客户端取消兜底 (G-3 对称面)
关键设计 (q5): **状态机硬编码**: 调用方编程错误 (顺序违规) → IllegalStateException 立即失败 (运行时即时暴露, 开发者快速定位); 协议违规 (多/缺响应) → INTERNAL Status 正常关闭流。**被放弃的方案: 柔性容错 (自动补 headers)** — 严格状态机让错误清晰化。 [序列化: grpc-encoding 协商链 (G-1 帧格式)]

### 6. 连接生命周期 — 双 GOAWAY 优雅下线

场景: 服务要下线了, 在途请求怎么办?客户端疯狂 ping 怎么防?
源码路径:
- **双 GOAWAY 协议** (GracefulShutdown, NettyServerHandler.java:1071-1145): ① GOAWAY(lastStreamId=MAX_VALUE) 拒新流 (L1097-1102) ② 发 PING + 超时兜底 (L1109-1115) ③ ack/超时 → GOAWAY(lastStreamCreated) 等旧流完成 (L1127-1131) + close
- 触发: max_age (L401-408)/max_idle (L421-423)/客户端 GoAway (L862-868)/服务端 shutdown
- **服务端主动保活**: keepAliveManager (L430-434, KeepAlivePinger L1030)
- **客户端 ping 限频**: keepAliveEnforcer (NettyServerHandler.java:261-273) → `pingAcceptable()` (L995) → GOAWAY ENHANCE_YOUR_CALM (L997) + **RESOURCE_EXHAUSTED "Too many pings from client"** (L999)
- **每连接并发流上限**: maxConcurrentCallsPerConnection 默认 Integer.MAX_VALUE (NettyServerBuilder.java:105), 经 SETTINGS_MAX_CONCURRENT_STREAMS 通告 (NettyServerHandler.java:283) — 服务端背压第一道闸
关键设计 (q6): **两阶段下线**: 先拒新, 再等旧; PING 作为"你是否还活着/是否已完成"的确认信号。**被放弃的方案: 直接 close** — 会丢在途请求。ping 限频是 HTTP/2 DoS 防线。 [HTTP/2: GOAWAY/PING 帧] [分布式: 优雅下线协议]

### 7. 协议协商 — pipeline 的运行时重排

场景: 明文/ TLS / ALPN 是怎么共存的?
源码路径:
- **ServerTlsHandler** (ProtocolNegotiators.java:424-487): 握手时插入 SslHandler (L443-449) → **ALPN 检查** (L463-469, "Unable to find compatible protocol") → 成功 → `pipeline().replace(...)` 换成 HTTP/2 handler (L471)
- **安全属性事件**: ProtocolNegotiationEvent 携带 SecurityLevel.PRIVACY_AND_INTEGRITY + SSLSession (L476-487) → 应用从 Attributes 读
- 工厂: serverPlaintext (L339)/serverTlsFactory (L359)/httpProxy (L489, HTTP CONNECT 组合)
关键设计 (q7): 协商 = **pipeline 动态重排**: SslHandler 握手完即替换, 省去每帧检查。**被放弃的方案: SslHandler 常驻固定 pipeline** — 替换后零 TLS 检查开销。 [TLS: ALPN 扩展] [HTTP/2: h2/h2c 选择]

### 8. 执行器 — 池 + 串行 + 动态切换

场景: 你的回调跑在哪个线程?为什么 gRPC 保证回调不并发?
源码路径:
- **池**: executorPool (ObjectPool, ServerImpl.java:99,188), 停机 returnObject (L361-362)
- **串行**: 所有回调经 SerializingExecutor (q3), 单个调用内严格串行
- **动态切换**: maySwitchExecutor → `executorSupplier.getExecutor(call, headers)` → `((SerializingExecutor) wrappedExecutor).setExecutor(switchingExecutor)` — 按请求换执行器, 且发生在排队阶段无竞态
- ServerCallExecutorSupplier (api:33)
关键设计 (q8): 三层执行器模型; 切换时机 (MethodLookup 阶段) 是设计巧点。**被放弃的方案: 每请求 new 执行器** — 池化防线程风暴。 [并发: 串行化执行器]

### 9. 停机语义 — 优雅与立即

场景: shutdown() 和 shutdownNow() 差在哪?
源码路径:
- **beginShutdown** (ServerImpl.java:260-268): `transportServer.shutdown()` → 传输层 GOAWAY 拒新 (q6)
- **shutdownNow** (L274-297): shutdown() + 遍历 transports `shutdownNow(UNAVAILABLE "Server shutdownNow invoked")` (L277,290) — 活动调用立即取消; **幂等短路** (L281-282)
- awaitTermination/checkForTermination 配合
- 测试实证: shutdownNowAfterSlowShutdown (ServerImplTest.java:419)
关键设计 (q9): 停机两阶段; UNAVAILABLE 让客户端 (G-6 重试) 换目标重放 — 优雅下线的分布式语义。**被放弃的方案: 停机即杀连接** — 在途请求会丢。 [分布式: 优雅下线→客户端重试闭环]

### 核心悬念

"客户端怎么知道服务端要下线了?——GOAWAY 是 HTTP/2 的'告别信'。" 下一域 [[G-3-客户端]] 将看到对称的 Channel 生命周期: 客户端如何消费 GOAWAY、如何重建连接; [[G-6-流控重试]] 将消费停机时抛出的 UNAVAILABLE。

### 负面空间 (不做)

1. 不写 Netty ServerBootstrap/event loop 细节 (阶段1 已讲)
2. 不写 TLS 证书/密钥管理 (只讲协商流程)
3. 不写 ServerStreamTracer/可观测 (排除面)
4. 不写服务端流式调用的四种 MethodType 语义细节 (G-3 客户端对称展开)
5. 不写 bindService/ServerServiceDefinition 构建细节 (G-1 生成面已覆盖)
6. 不写 per-RPC 限额/限流 (G-6 流控域)
