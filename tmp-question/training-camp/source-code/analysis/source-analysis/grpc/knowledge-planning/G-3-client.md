# G-3 客户端 — 知识规划 (KP)

> 域级: 🔴 A (定义特征: Channel→Call 状态机是 gRPC 客户端全部能力) | 模块: core/ManagedChannelImpl (2200) + ClientCallImpl (784) + ManagedChannelImplBuilder (1071) + DelayedClientCall (642) + context (Context 1124/Deadline 288/PHAMT 301) + netty/NettyClientTransport (477) + NettyClientHandler (1186) + NettyChannelBuilder (928)
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(syncContext) q2(生命周期) q3(newCall) q4(CallImpl) q5(Context) q6(Deadline) q7(Delayed) q8(传输)

## 一、机制提取 (逐源)

### M1 SynchronizationContext 单线程模型 (q1)
- ConcurrentLinkedQueue + AtomicReference<Thread> drainingThread (SynchronizationContext.java:65-66)
- drain (L87-106): CAS 抢权 → 队列轮询 → do-while 重查; execute = executeLater + drain (L126-129, 可内联)
- throwIfNotInThisSynchronizationContext (L135-137): `checkState(Thread.currentThread() == drainingThread.get())`
- ManagedChannelImpl: 字段全标 "Must be accessed from the syncContext" (L219-286); createSubchannel throwIfNotIn (L362,390,455); execute(new Shutdown()) (L722)
- 测试: createSubchannel_outsideSynchronizationContextShouldThrow (ManagedChannelImplTest.java:400)

### M2 通道生命周期 (q2)
- **懒启动**: exitIdleMode (L389-417): throwIfNotIn → 短路 (shutdown/panic) → idle 定时器取消/重排 (L398-407, 竞态防护) → newLoadBalancer + gotoState(CONNECTING) + nameResolver.start (L412-416)
- **空闲回收**: enterIdleMode (L420-438): shutdownNameResolverAndLoadBalancer → delayedTransport.reprocess → IDLE → pending 调用检测重新 exit (L429-432)
- IDLE_TIMEOUT_MILLIS_DISABLE = -1 (L130); shutdown → syncContext.execute(Shutdown) (L722)
- 测试: idleModeDisabled (L473)/startCallBeforeNameResolution (L500)

### M3 newCall 三路径 (q3)
- 就绪直通: configSelector (AtomicReference, L831) → newClientCall (L858-860)
- 内联优化: syncContext.execute(exitIdleMode) → 再查 → 直通 (L861-874, inprocess 同步解析)
- shutdown: FailingClientCall onClose(SHUTDOWN_STATUS) (L878-895)
- **PendingCall 缓冲** (L897-919): context/method/callOptions 快照 → pendingCalls (LinkedHashSet) → updateConfigSelector reprocess 逐个放行 (L925-933)

### M4 ClientCallImpl 状态机 (q4)
- startInternal (L188-235): 一次性 (checkState L189-190); **Context 已取消短路** → NoopClientStream + ContextRunnable 回调 (L197-216); 压缩器缺失 → INTERNAL 立即失败 (L223-231)
- **Context 取消监听** (L382-391): deadline 超时 → formatDeadlineExceededStatus (L386); 否则 stream.cancel(statusFromCancelled) (L391)
- **用户异常优先级** (L588-594): exceptionStatus 暂存 + stream.cancel — "overwrite the onClose() details when it arrives"
- closeObserver (L564-568): 用户 onClose 异常记 WARNING
- 测试: exceptionInOnMessageTakesPrecedenceOverServer (ClientCallImplTest.java:190)

### M5 Context (q5)
- 不可变快照: `Node<Key<?>, Object> keyValueEntries` (Context.java:180) — **PHAMT 持久化** (L344-367)
- ThreadLocal 存储: ThreadLocalContextStorage (L25-47); current() null→ROOT (Context.java:171-176)
- **ContextRunnable** (core): attach → runInContext → detach (ContextRunnable.java:34-40)
- CancellableContext: 取消 → listener → ClientCallImpl.cancelled → stream.cancel

### M6 Deadline (q6)
- 单调时钟: Ticker = System.nanoTime (Deadline.java:38); MAX_OFFSET ±100 年防环绕 (L40-43)
- after(duration) 偏移转绝对 (L69-70)
- **调度取消**: CancellationHandler — remainingNanos (ClientCallImpl.java:345-348) → deadlineCancellationExecutor.schedule (L361-362) → stream.cancel(DEADLINE_EXCEEDED) (L385)
- start 时过期检查 (L248-260): "started after %s deadline was exceeded"

### M7 DelayedClientCall 缓冲 (q7)
- start: DelayedListener 包装 (L216) + startHeaders 暂存; error → CloseListenerRunnable (L224-226)
- cancel: NOOP_CALL + error (L246-255) / 转发 delayOrExecute (L262-267)
- delayOrExecute (L270-278): 排队 vs 直通
- **drainPendingCalls** (L300-320): 锁内取批 → 锁外执行 → 重查 → passThrough=true 永不回头 (L307-309)

### M8 Netty 客户端传输 (q8)
- newStream: channel null → FailingClientStream (NettyClientTransport.java:202-204); NettyClientStream + TransportState (L210-219)
- start: ClientTransportLifecycleManager (L223-225) + KeepAliveManager (L240-244, keepAliveWithoutCalls) + NettyClientHandler.newHandler (L247-260)
- NettyClientHandler: PingCountingFrameWriter (L239) + 流控窗口 (L160-191)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 syncContext / M2 懒启动 / M3 newCall 三路径 / M4 短路+异常优先级 / M7 缓冲放行 |
| P2 | M5 Context PHAMT / M6 Deadline 单调时钟 / M8 传输生命周期 |
| P3 | KeepAlive 双向 / ProxyDetector (对照) |

## 三、叙事线

场景: `channel.newCall(method, callOptions)` 到 `stub.sayHello()` 返回 — 一次调用穿越多少层?读者疑问链: 通道凭什么懒 (M2) → 所有状态为什么不怕并发 (M1) → 调用怎么等就绪 (M3/M7) → 调用状态机与取消 (M4/M5) → 超时怎么算 (M6) → 最后谁发 HTTP/2 (M8)。
