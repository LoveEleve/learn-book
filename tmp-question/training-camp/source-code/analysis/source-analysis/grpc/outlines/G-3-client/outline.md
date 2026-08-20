# G-3 客户端 — 一次 newCall 的旅程: 懒通道、串行状态与调用状态机

> 前置: [[G-1-Protobuf与Stub生成]] (stub 委托 newCall) + [[G-2-服务端]] (对称生命周期) | 引出: [[G-6-流控重试]] (RetriableStream 内嵌 ClientCallImpl) + [[G-4-负载均衡]] (Subchannel 选址) + [[G-5-命名解析]] (解析驱动) | 对照: Netty 客户端 (阶段1)
> 🔴 A | 8 KP | [模式: 懒加载 + 串行化 + 缓冲]
> Pass 2 闭环: q1(syncContext) q2(生命周期) q3(newCall) q4(CallImpl) q5(Context) q6(Deadline) q7(Delayed) q8(传输)

**读者处境**: `channel.newCall(method, callOptions)` 返回一个 ClientCall, `call.start(listener, headers)` 之后, 消息就发出去了。但通道第一次使用前真的什么都没做吗?调用在地址解析完成前发起会怎样?超时是怎么"到点取消"的?取消怎么从你的线程传到网络层?

### 1. SynchronizationContext — 无锁单线程状态模型

场景: ManagedChannelImpl 有几百个状态字段, 注释全是 "Must be accessed from the syncContext" — 它用什么保证并发安全?
源码路径:
- 结构: **ConcurrentLinkedQueue + AtomicReference<Thread> drainingThread** (SynchronizationContext.java:65-66)
- **drain** (L87-106): `drainingThread.compareAndSet(null, 当前线程)` CAS 抢"排空权" (L89) → 队列轮询执行 → finally 释放 → **do-while 重查** (L105, "must check queue again here to catch any added prior to clearing drainingThread")
- **execute = executeLater + drain** (L126-129) — 可能**内联执行** (调用线程直接跑任务); **drain 内可重入**: 任务里再 execute 直接内联跑 (同一排空线程, 无死锁)
- **throwIfNotInThisSynchronizationContext** (L135-137): `checkState(Thread.currentThread() == drainingThread.get())` — 运行期断言
- 用法: ManagedChannelImpl 字段注释 (L219-286), createSubchannel 断言 (L362), 异步事件 execute(new Shutdown()) (L722)
关键设计 (q1): **无锁单线程模型**: 不是专用线程, 而是"任何线程借道执行, 同一时刻仅一人" (CAS 抢权); 状态访问 = 一次 CAS + 队列轮询, 免锁竞争免专用线程。**被放弃的方案: 每字段加锁** (读路径也要锁, 易死锁); **专用状态线程** (多一次切换)。[并发: CAS 抢权] [跨域: G-2 SerializingExecutor 同思想但更轻]

### 2. 通道生命周期 — 懒启动与空闲回收

场景: 为什么第一次 RPC 之前, 通道像不存在一样?
源码路径:
- **exitIdleMode** (ManagedChannelImpl.java:389-417): throwIfNotIn → shutdown/panic 短路 (L391-393) → idle 定时器取消/重排 (L398-407, "a racing due timer will not put Channel on idleness") → **首次: newLoadBalancer + gotoState(CONNECTING) + nameResolver.start** (L412-416)
- **enterIdleMode** (L420-438): shutdownNameResolverAndLoadBalancer (L423) → delayedTransport.reprocess (L424) → IDLE (L426) → **pending 调用检测: 有就重新 exit** (L429-432, "gives these calls a chance to be processed")
- IDLE_TIMEOUT_MILLIS_DISABLE = -1 (L130); **默认 idle 超时 30 分钟** (ManagedChannelImplBuilder, 零流量后回收); shutdown → syncContext.execute(Shutdown) (L722)
关键设计 (q2): **按需资源**: 首个调用触发全链路创建 (LB+解析器), 空闲超时释放; enterIdleMode 的错误回收防护 (pending 检测)。**被放弃的方案: 启动即建全链路** — 零流量时资源白耗。与 G-2 服务端 SHUTDOWN (不收新) 对称: 客户端 IDLE = 不建, 服务端 SHUTDOWN = 不收。 [分布式: 两端生命周期对称] [并发: idle 定时器竞态防护]

### 3. newCall 三路径 — 就绪直通 / 内联优化 / 缓冲排队

场景: 解析还没完成, newCall 返回什么?
源码路径:
- **就绪直通**: `configSelector.get() != INITIAL_PENDING_SELECTOR → newClientCall` (L858-860) — AtomicReference 无锁读 (L831)
- **内联优化**: 未就绪 → `syncContext.execute(exitIdleMode)` (L864-868) → 再查 → 直通 (L870-874) — 注释: "optimization for the case (typically with InProcessTransport) when name resolution result is immediately available"
- **shutdown**: FailingClientCall `onClose(SHUTDOWN_STATUS)` (L878-895) — 立即失败
- **PendingCall 缓冲** (L897-919): context/method/callOptions **快照** → pendingCalls (LinkedHashSet) → updateConfigSelector 逐个 reprocess 放行 (L925-933)
关键设计 (q3): **条件路由**: 就绪零开销; 未就绪先触发懒启动并内联重试; 还不行就缓冲 (Context 快照 = 调用语义冻结, 放行后按发起时上下文执行)。**被放弃的方案: 阻塞等待解析** — 浪费调用线程; 缓冲异步无感。 [跨域: G-1 STUB_TYPE_OPTION 经 CallOptions 传播] [并发: AtomicReference]

### 4. ClientCallImpl 状态机 — 短路与异常优先级

场景: 调用已经取消/压缩器不存在, 还会发请求吗?用户回调抛异常会怎样?
源码路径:
- **startInternal** (ClientCallImpl.java:188-235): 一次性 (checkState L189-190); **Context 已取消短路** → `NoopClientStream.INSTANCE` (L199, 不建流) → ContextRunnable 回调 onClose (ClientCallImpl.java:206-213)
- 压缩器缺失 → INTERNAL "Unable to find compressor" 立即回调 (L223-231)
- **Context 取消监听** (L382-391): 超时 → formatDeadlineExceededStatus (L386); 否则 stream.cancel(statusFromCancelled) (L391) — **取消传播: Context → 流**; 调用结束时 removeListener 清理 (L375-377 区)
- **用户异常优先级** (L588-594): exceptionStatus 暂存 + stream.cancel — "we can only call onClose() when we are sure there will be no further callbacks. We set the status here and overwrite the onClose() details when it arrives"
关键设计 (q4): **提前失败**: 可预见的失败 (取消/坏压缩器) start 即短路, 零流创建; 不可预见的 (回调异常) 用 exceptionStatus **覆盖服务端状态** — 用户错误不静默。**被放弃的方案: 异常直接抛给调用线程** — 异步场景无调用线程可抛。 [跨域: G-2 服务端状态机对称] [并发: 回调归属 callExecutor]

### 5. Context — 不可变快照与跨线程传播

场景: 一次调用在 3 个线程间穿梭, 它的 Context 怎么跟着走?
源码路径:
- **不可变快照**: `Node<Key<?>, Object> keyValueEntries` (Context.java:180) — **PHAMT 持久化** (L344-367): withValue 生成新节点共享旧路径, O(log n)
- **ThreadLocal 存储**: ThreadLocalContextStorage (ThreadLocalContextStorage.java:32) — current() 零锁, null→ROOT (Context.java:171-176)
- **ContextRunnable** (core): `run(): previous = context.attach(); runInContext(); finally context.detach(previous)` (ContextRunnable.java:34-40) — 目标线程恢复调用上下文
- **取消链**: CancellableContext → 取消 → ClientCallImpl.cancelled (L382) → stream.cancel
关键设计 (q5): 调用级线程本地状态: PHAMT 让 withValue O(log n) 且天然线程安全 (不可变共享); ContextRunnable 让任务在目标线程恢复上下文。**被放弃的方案: ① 可变 Map 拷贝 (O(n)); ② InheritableThreadLocal** (线程池复用污染)。 [算法: HAMT 持久化] [跨域: G-2 createContext 服务端对称]

### 6. Deadline — 单调时钟与调度取消

场景: 超时用 System.currentTimeMillis() 有什么问题?gRPC 怎么算的?
源码路径:
- **单调时钟**: `Ticker SYSTEM_TICKER = new SystemTicker()` (Deadline.java:38, nanoTime 底座); **MAX_OFFSET ±100 年防环绕** (L40-43)
- **偏移转绝对**: `Deadline.after(duration)` (L69-70) — 注释 (L31-35): "a timeout can be converted to a Deadline at the start of the operation" — 计时起点固定
- **调度取消**: CancellationHandler — remainingNanos (ClientCallImpl.java:345-348) → `deadlineCancellationExecutor.schedule(...)` (L361-362) → `stream.cancel(formatDeadlineExceededStatus())` (L385)
- **start 时过期检查** (L248-260): "ClientCall started after %s deadline was exceeded %.9f seconds ago" — 已过期立即失败
关键设计 (q6): **单调时钟免疫 NTP 回拨** (墙上时钟回拨会提前/延后超时); 偏移转绝对让剩余时间与链路延迟无关; 调度器定时取消与 Context 取消链汇合。**被放弃的方案: currentTimeMillis** (NTP 敏感); **轮询检查** (无谓 CPU)。 [跨域: G-2 grpc-timeout 服务端对称] [并发: 定时取消竞态]

### 7. DelayedClientCall — 缓冲期语义冻结与原子放行

场景: 就绪前的调用, start/sendMessage/cancel 都"发生过"了 — 怎么重放?
源码路径:
- **start** (DelayedClientCall.java:206-230): **DelayedListener 包装** (DelayedClientCall.java:217, 缓冲回调) + startHeaders 暂存 (L218); 已 cancel → CloseListenerRunnable 立即失败 (L224-226)
- **cancel** (L232-260): realCall 未建 → NOOP_CALL + error (L246-255); 已建 → delayOrExecute 转发 (L262-267)
- **delayOrExecute** (L270-278): `!passThrough → pendingRunnables.add` 排队保序
- **drainPendingCalls** (L300-320): 锁内取批 → 锁外执行 → **重查** ("new Runnables may be added after we drop the lock", L317-319) → `passThrough = true` (L307-309) — **一次性切换永不回头**
关键设计 (q7): **语义冻结 + 原子放行**: 缓冲期操作全部记录, 放行后按原序重放; DelayedListener 保回调顺序。**被放弃的方案: 阻塞调用线程** — 浪费线程。与 PendingCall (q3) 同模式两层缓冲 (Channel 层 → Transport 层)。 [模式: 缓冲重放] [并发: 锁+队列]

### 8. Netty 客户端传输 — 连接复用与双向保活

场景: 最后一跳: ClientCall 的消息怎么上 HTTP/2?
源码路径:
- **newStream** (NettyClientTransport.java:197-220): channel null → **FailingClientStream** (L202-204); NettyClientStream + TransportState (L210-219)
- **start** (L222-260): **ClientTransportLifecycleManager** (L223-225) → eventLoop 选择 (L227) → **KeepAliveManager** (L240-244, **keepAliveWithoutCalls** — 无调用也保活) → NettyClientHandler.newHandler (L247-260)
- **NettyClientHandler** (1186): **PingCountingFrameWriter** (NettyClientHandler.java:239) + 流控窗口 (L160-191)
- **消息接收链落点**: HTTP/2 DATA 帧 → NettyClientHandler → Http2ClientStream → **MessageDeframer** 解析 gRPC 帧 (5B 头+双重校验, 格式面在 G-1 §7) → Listener 回调 — 帧格式的客户端消费实现
关键设计 (q8): **连接复用** (HTTP/2 多路复用, 不每调用建连接); 客户端主动 ping 保活 ↔ 服务端 keepAliveEnforcer 限频 (G-2 对称); 未连接通道快速失败。**被放弃的方案: 每调用建连接** — 连接建立是 RPC 最大开销之一。 [HTTP/2: 复用/窗口] [跨域: G-4 InternalSubchannel 连接池]

### 核心悬念

"一次调用从 `stub.sayHello()` 到字节上线的完整路径: stub (G-1) → newCall 三路径 → DelayedClientCall 缓冲 → ClientCallImpl 状态机 → Context/Deadline 守护 → Netty 传输 — 那么地址怎么来?选哪个连接?" 下一域 [[G-5-命名解析]] 与 [[G-4-负载均衡]] 回答地址来源与选址; [[G-6-流控重试]] 将揭示 ClientCallImpl 内嵌的 RetriableStream 如何在失败时重放整个调用。

### 负面空间 (不做)

1. 不写 HTTP/2 帧格式细节 (Netty 阶段1 已讲)
2. 不写 ConnectionClientTransport 接口全族 (传输抽象已在 v4 归类)
3. 不写 ManagedChannelBuilder 配置项穷举 (只讲关键默认值)
4. 不写 NameResolver/LoadBalancer SPI (G-4/G-5 专属)
5. 不写 ProxyDetector 细节 (对照面, G-3 只讲存在)
6. 不写流量控制窗口算法细节 (G-6 流控域)
