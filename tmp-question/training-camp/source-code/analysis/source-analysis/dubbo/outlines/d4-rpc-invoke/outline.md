# D-4 RPC 调用 — 代理→Filter 链→协议发送→异步结果

> 前置: [[D-1-SPI微内核]] [[D-2-服务导出]] [[D-3-服务引用]] | 引出: [[D-5-注册中心]] [[D-7-集群容错]] [[D-8a-传输抽象]] [[D-10-序列化]] | 对照: Feign 调用链 + Spring AOP 责任链
> 🔴 A | 8 KP | [模式: 调用链 + 异步化]
> Pass 2 闭环: q1(入口与上下文) q2(Filter 链) q3(协议发送) q4(异步结果)

**读者处境**: 代理方法调用后, 请求怎么穿过过滤器/协议到达对端? 同步调用为什么底层是异步? 这篇拆 InvocationUtil + Filter 链 + DubboInvoker + AsyncRpcResult。

### 1. 调用入口 — InvocationUtil.invoke 两拍结构

场景: 用户调用代理方法, 第一行代码发生了什么?
源码路径:
- **InvocationUtil.invoke** (proxy/InvocationUtil.java:39-106): **RpcContext.storeServiceContext** (上下文快照, finally 恢复) → setTargetServiceUniqueName + setConsumerUrl → **invoker.invoke(rpcInvocation).recreate()** (两拍: 异步 invoke + 同步化 recreate)
- **RpcContext 3.x 三件套**: getServiceContext (L204, 调用服务上下文) / getClientAttachment (L170) / getServerAttachment (L179) — 三上下文分离
- Profiler 简单追踪 (可选): 耗时 vs timeout 对比 → 超时告警
- invoker = ClusterInvoker (D-7 黑盒, AbstractClusterInvoker.doInvoke L448)
关键设计 (q1): **上下文快照/恢复 + 全异步底座 (同步化只在最后一拍)**。[模式: 入口面]

### 2. Filter 链 — buildInvokerChain + 消费端实例族

场景: 调用穿过哪些过滤器? 谁构建的链?
源码路径:
- **ProtocolFilterWrapper** (cluster/filter/ProtocolFilterWrapper.java:66-73, **3.x 在 dubbo-cluster**): Protocol 的 Wrapper (D-1 织入) — **registry URL 直接透传** / 非 registry → buildInvokerChain(protocol.refer, REFERENCE_FILTER_KEY, **CONSUMER**)
- **DefaultFilterChainBuilder.buildInvokerChain** (L43-77): **getActivateExtension(url, key, group)** (D-1 激活面消费) → 多 ModuleModel 去重保序 → **倒序包装** (CopyOfFilterChainNode LIFO) → CallbackRegistrationInvoker 收尾; ⚠ **链节点均为 FilterChainBuilder.java 内部类** (FilterChainNode L61 / ClusterFilterChainNode L162 / CallbackRegistrationInvoker L184 / CopyOfFilterChainNode L311); builder 是 SPI (getDefaultExtension, 仅 default 实现) + **InvocationInterceptorBuilder SPI 并行面** (@SPI("default"))
- ⚠ **集群级 Filter 链 (3.x 双链层次)**: **buildClusterInvokerChain** (FilterChainBuilder.java:53) + **ClusterFilterChainNode** (L162) + CopyOfClusterFilterChainNode (L394, 注释 "replace ClusterFilterChainNode when proved stable") — **AbstractCluster L92 join 时织入** (集群 invoker 上的过滤器, 如 adaptiveLoadBalance 面)
- **消费端实例族 18 个** (META-INF/dubbo/internal/org.apache.dubbo.rpc.Filter): echo/generic/token/accesslog/classloader/context/exception/executelimit/deprecated/compatible/timeout/tps/active-limit/adaptiveLoadBalance...
关键设计 (q2): **Wrapper 织入 (D-1 实例) + CONSUMER group 分流 + URL 参数驱动过滤**。[模式: 责任链]

### 3. 协议发送 — DubboInvoker.doInvoke

场景: 过滤器末端, 调用怎么变成网络请求?
源码路径:
- **doInvoke** (protocol/dubbo/DubboInvoker.java:89-161): PATH/VERSION 附件 → **clientsProvider 多连接轮询** (index % size; ExchangeClient 族: **SharedClientsProvider 共享 / ExclusiveClientsProvider 独占** + ReferenceCount/LazyConnectExchangeClient, DubboProtocol:462-481) → **isOneway** (RpcUtils.isOneway L232: return=false → 单向 send 不等结果) → **timeout 前置计算** (<=0 → TIMEOUT_TERMINATE) → Request 构建 (payload/version/data)
- **twoWay=true → currentClient.request(request, timeout, executor) → CompletableFuture<AppResponse>** (ExchangeClient 黑盒, D-8 深潜) → FutureContext 2.6 兼容 → AsyncRpcResult
- **executor 来源 (线程池面)**: getCallbackExecutor → **ExecutorRepository** (dubbo-common/threadpool/manager) + **ThreadPool SPI 4 实现** (fixed/cached/limited/eager — internal 注册表实证)
- **timeout 倒计时面**: calculateTimeout (RpcUtils L280-288): TIME_COUNTDOWN 附件 → 方法级 timeout → ENABLE_TIMEOUT_COUNTDOWN 超时倒计时传递远端
- 异常翻译: Timeout → RpcException(TIMEOUT) / Remoting → SERIALIZATION 或 NETWORK (cause 分类)
- **InvokeMode 推导** (RpcUtils.getInvokeMode L215-230): invocation 已有 → 用; **返回值 Future → FUTURE (自动检测)** / async 配置 → ASYNC / 否则 SYNC
关键设计 (q3): **连接级轮询 + 单向通道 + 超时前置终止 + 全异步发送**。[模式: 发送面]

### 4. 异步结果 — AsyncRpcResult 三模式

场景: 异步结果怎么回到同步调用方?
源码路径:
- **recreate 三模式** (AsyncRpcResult.java:237-247): **FUTURE → 返回 Future 本身** / **ASYNC → 返回默认值 (不阻塞)** / **SYNC (默认) → getAppResponse().recreate() 同步等待**
- 结果载体: **AppResponse** (value + exception + attachments 统一对象) + CompletableFuture 链
- 异常: 业务异常在 AppResponse.exception, recreate 时抛; 传输异常已分级翻译
关键设计 (q4): **三模式一拍切换 (同一条异步链) + 全链路 CompletableFuture + 统一结果载体**。[模式: 异步化]

## 代码类型
Architecture (调用链) + Concurrency (异步化)

## 负面空间 (D-4, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不调用级同步 | invoke 返回异步, 同步化在 recreate 最后一拍 (q1) |
| 不过滤器热加载 | 链在 refer 时一次性构建 (q2) |
| 不注册中心引用织链 | registry URL 透传, 不过滤器 (q2) |
| 不请求级重试 | 重试在 D-7 ClusterInvoker (q3) |
| 不连接管理 | 连接生命周期在 remoting D-8 (q3) |
| 不流式背压 | 结果全量缓冲; 流式在 D-9 Triple (q4) |

## 结尾桥 OUTBOUND

- → [[D-5-注册中心]]: RegistryDirectory subscribe/notify 深潜 (本域 ClusterInvoker 黑盒)
- → [[D-7-集群容错]]: ClusterInvoker doInvoke 容错策略 (本域只到黑盒)
- → [[D-8a-传输抽象]]: ExchangeClient.request → exchange 层 (Request/Response/心跳) — 网络层深潜
- → [[D-10-序列化]]: Request.data 编码 (hessian2/protobuf) — 协议背后
