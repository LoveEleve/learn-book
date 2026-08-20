# G-3 Pass 1 扫描笔记 — 客户端

> 日期: 2026-08-16 | 版本: 1.83.1 | 🔴 A | 模块: core/ManagedChannelImpl (2200) + ClientCallImpl (784) + ManagedChannelImplBuilder (1071) + DelayedClientCall (642) + DelayedStream (551) + MessageDeframer (550) + context (Context 1124/Deadline 288/PHAMT 301) + netty/NettyClientTransport (477) + NettyClientHandler (1186) + NettyChannelBuilder (928)

## 继承树/调用图

```
Grpc.newChannelBuilder (L111) → ManagedChannelRegistry → NettyChannelBuilder (L80)
  → 传输工厂 → ManagedChannelImpl (2200, extends ManagedChannel)
  ├── syncContext (SynchronizationContext) — 单线程状态模型 ("Must be accessed from the syncContext")
  ├── 生命周期: exitIdleMode/enterIdleMode/shutdown/terminated
  ├── newCall (L809) → ClientCallImpl (784) | DelayedClientCall (642, 就绪前缓冲)
  ├── NameResolver (G-5) + LoadBalancer (G-4) 装配
  └── 连接: InternalSubchannel (G-4) → NettyClientTransport (477) → NettyClientHandler (1186)
ClientCallImpl (72): start (L181)/sendMessage (L508)/halfClose (L492)/cancel (L452)
  ├── Context 取消: cancelled (L382)
  └── closeObserver (L564, 回调线程控制)
DelayedClientCall (50): pending calls 排队 → drainPendingCalls (L300)
Context (api/src/context): PHAMT (301) + ThreadLocalContextStorage (73)
Deadline (288): 计时/超时
```

## 基本元素分解

1. **ManagedChannelImpl**: 通道状态机 (IDLE/CONNECTING/READY/TRANSIENT_FAILURE/SHUTDOWN) + syncContext 单线程 + NameResolver/LB 装配 + 生命周期
2. **ClientCallImpl**: 单个调用状态机 + Context 取消 + 回调线程控制 (closeObserver)
3. **DelayedClientCall/DelayedStream**: 就绪前缓冲 (nameResolver/LB 未就绪)
4. **Context/Deadline**: 跨线程上下文快照 (PHAMT) + 超时取消
5. **NettyClientTransport/Handler**: HTTP/2 客户端传输

## 标记问题 (10)

1. **Q1 syncContext 单线程模型**: 为什么所有状态字段必须在 syncContext 访问?这和 G-2 的 SerializingExecutor 什么关系?createSubchannel_outsideSynchronizationContextShouldThrow (测试 L400) 说明什么?
2. **Q2 通道生命周期**: IDLE→CONNECTING→READY 状态机,exitIdleMode/enterIdleMode (idle 超时 30min 默认?),shutdown 流程与 G-2 对称?
3. **Q3 newCall 流程 (L809)**: 从 channel.newCall 到真实流 — DelayedClientCall 缓冲哪些?配置选择器 (ConfigSelector)?
4. **Q4 ClientCallImpl 状态机**: start/sendMessage/halfClose/cancel 顺序 + closeObserver 回调线程 + "exceptionInOnMessageTakesPrecedence" (测试 L190) 优先级语义
5. **Q5 Context 取消传播**: createContext→CancellableContext,上下文取消→流取消 (cancelled L382);Context 快照怎么跨线程 (PHAMT/ThreadLocalContextStorage/ContextRunnable)
6. **Q6 Deadline**: Deadline 怎么计时 (ticker/System.nanoTime?)/传播到服务端 (grpc-timeout header?)/本地超时取消链
7. **Q7 DelayedClientCall 缓冲**: pending call 队列/真实 call 就绪后 drain/取消语义
8. **Q8 NettyClientTransport/Handler**: HTTP/2 连接建立 (connection preface)/流创建/KeepAliveManager 客户端侧
9. **Q9 代理**: ProxyDetectorImpl (环境代理检测 HTTP CONNECT)
10. **Q10 就绪语义**: startCallBeforeNameResolution (测试 L500) — 解析前调用会怎样?immediateDeadlineExceeded (L487)

## 已读测试

- ManagedChannelImplTest (core): createSubchannel_outsideSynchronizationContextShouldThrow (L400) — syncContext 强制; idleModeDisabled (L473); immediateDeadlineExceeded (L487); startCallBeforeNameResolution (L500)
- ClientCallImplTest (core): statusPropagatedFromStreamToCallListener (L169); exceptionInOnMessageTakesPrecedenceOverServer (L190) — 用户异常优先于服务端状态; authorityPropagatedToStream (L365)
