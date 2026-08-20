# G-2 服务端 — 知识规划 (KP)

> 域级: 🔴 A (定义特征: HTTP/2 服务端是 gRPC 服务能力的一半) | 模块: core/ServerImpl (980) + ServerCallImpl (399) + netty/NettyServer (494) + NettyServerHandler (1294) + ProtocolNegotiators (1249) + NettyServerBuilder (800) + api/ServerCall (269)
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(装配链) q2(注册表) q3(请求链路) q4(拦截器) q5(状态机) q6(连接生命周期) q7(协商) q8(执行器) q9(停机)

## 一、机制提取 (逐源)

### M1 装配链 (q1)
- NettyServerBuilder extends ForwardingServerBuilder, **委托** ServerImplBuilder (NettyServerBuilder.java:176, new ServerImplBuilder(new NettyClientTransportServersBuilder()))
- ServerImplBuilder.build() (L257-263): `new ServerImpl(this, clientTransportServersBuilder.buildClientTransportServers(...), Context.ROOT)`
- buildTransportServers (L710-741): new NettyServer(..., negotiator, ...) — 协议协商器是参数
- ServerImpl.start() → `transportServer.start(listener)` (ServerImpl.java:187); NettyServer.start 配 ServerBootstrap (NettyServer.java:218-240)

### M2 注册表 (q2)
- InternalHandlerRegistry (L30-78): services List + methods Map (fullMethodName → ServerMethodDefinition); **build 时扁平化** (L67-78), 不可变
- addService: 按 service 名 HashMap 覆盖 (L59-65)
- lookupMethod: `methods.get(methodName)` O(1) (L51-54) + fallbackRegistry (ServerImpl.java:545) → 无 → **UNIMPLEMENTED "Method not found"** (L548-559) + NOOP_LISTENER + context.cancel

### M3 请求接收链路 (q3)
- streamCreated (ServerImpl.java:490 区): 解压器协商 (L490-497) → lookupMethod (L501) → **createContext (L503, CancellableContext)** → **JumpToApplicationThreadServerStreamListener** (L510-513) → **MethodLookup** (L524-564, serializing executor 排队, "setListener() is called before any callbacks", L515-517) → wrapMethod (L561) → maySwitchExecutor (L562)
- startWrappedCall (L692-699): handler.startCall → listener → ServerStreamListenerImpl (ServerCallImpl.java:238-240)

### M4 拦截器链 (q4)
- wrapMethod (ServerImpl.java:660-675): for 循环 `interceptCallHandlerCreate(interceptor, handler)` (L669-670) — **每请求包装**; 次序: 最后添加的最外层
- InterceptCallHandler (ServerInterceptors.java): `interceptor.interceptCall(call, headers, callHandler)` (L44-46) — 拦截器决定是否包 call (PartialForwardingServerCall)
- binlog 钩子 (L674)

### M5 ServerCallImpl 状态机 (q5)
- sendHeadersInternal (L104-133): 一次性 (checkState); **压缩协商**: 客户端 ACCEPT_ENCODING 检查 → 降级 Identity.NONE (L111-121); writeHeaders(headers, !serverSendsOneMessage()) (L133)
- sendMessageInternal (L156-174): 须先 sendHeaders (L158); unary 二次 → TOO_MANY_RESPONSES (L162-164); unary 不 flush
- setCompression: sendHeaders 前 (L188-197)
- closeInternal (L216-230): 一次性; unary 缺响应 → MISSING_RESPONSE (L221-223)

### M6 连接生命周期 (q6)
- 双 GOAWAY 优雅关闭 (NettyServerHandler.java:1071-1145): GOAWAY(MAX) 拒新 (L1097-1102) → PING + 超时兜底 (L1109-1115) → ack/超时 → GOAWAY(lastStreamCreated) (L1127-1131) + close
- 触发: max_age 定时器 (L401-408)/max_idle (L421-423)/GoAway (L862-868)
- keepAliveManager 服务端主动 ping (L430-434, KeepAlivePinger L1030)
- keepAliveEnforcer 客户端限频 (L261-273): pingAcceptable (L995) → GOAWAY ENHANCE_YOUR_CALM (L997) + RESOURCE_EXHAUSTED "Too many pings" (L999)

### M7 协议协商 (q7)
- ServerTlsHandler (ProtocolNegotiators.java:424-487): SslHandler 插入 (L443-449) → ALPN 检查 (L463-469, "Unable to find compatible protocol") → pipeline 替换 (L471) → 安全属性事件 (L476-487, SecurityLevel.PRIVACY_AND_INTEGRITY + SSLSession)
- 工厂: serverPlaintext (L339)/serverTlsFactory (L359)/httpProxy (L489)

### M8 执行器 (q8)
- executorPool (ObjectPool, ServerImpl.java:99,145,188,361-362)
- SerializingExecutor 桥 (q3); maySwitchExecutor: `executorSupplier.getExecutor(call, headers)` → `((SerializingExecutor) wrappedExecutor).setExecutor(...)` — 请求级切换, 发生在排队阶段无竞态
- ServerCallExecutorSupplier (api:33)

### M9 停机 (q9)
- beginShutdown (L260-268): transportServer.shutdown() (GOAWAY 拒新)
- shutdownNow (L274-297): shutdown() + 遍历 transports `shutdownNow(UNAVAILABLE "Server shutdownNow invoked")` (L277,290); 幂等短路 (L281-282)
- awaitTermination/checkForTermination

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M3 请求链路 (双执行器跳转+顺序保证) / M5 状态机 (压缩协商+严格校验) / M6 双 GOAWAY / M4 拦截器包装次序 |
| P2 | M1 装配委托 / M2 注册表扁平化 / M7 pipeline 替换 / M9 停机语义 |
| P3 | M8 动态执行器 / binlog 钩子 |

## 三、叙事线

场景: `ServerBuilder.forPort(8080).addService(new HelloServiceImpl()).build().start()`. 读者疑问链: 一行 start() 背后建了什么 (M1) → 我的服务注册到哪 (M2) → 请求进来怎么找到我的方法 (M3) → 拦截器在哪一层 (M4) → 服务端怎么回消息/压缩 (M5) → 连接怎么保活/下线 (M6/M9) → TLS 怎么协商 (M7) → 回调跑在哪个线程 (M8)。
