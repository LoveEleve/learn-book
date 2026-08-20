# D-4 RPC 调用 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: InvokerInvocationHandler → ProtocolFilterWrapper (dubbo-rpc) → DubboInvoker → ExchangeClient; 同步阻塞 (Future → get) |
| 2.7.x | 异步化改造: AsyncRpcResult + CompletableFuture 底座; InvokeMode 三模式; RpcContext 重构 |
| 3.x | **ProtocolFilterWrapper 迁至 dubbo-cluster (rpc.cluster.filter)** + **FilterChainBuilder SPI** (默认 DefaultFilterChainBuilder, 链节点为 FilterChainBuilder.java 内部类家族: FilterChainNode/ClusterFilterChainNode/CallbackRegistrationInvoker/CopyOfFilterChainNode) + InvocationInterceptorBuilder SPI (拦截器并行面) |
| 3.3.x | 多 ModuleModel 过滤合并 + 去重保序; Profiler 简单追踪 |

## 痕迹证据

- ProtocolFilterWrapper.java:40-45 构造: getFilterChainBuilder (L61-64) — FilterChainBuilder SPI (getDefaultExtension) (3.x 锚)
- FilterChainBuilder.java: 链节点内部类家族 — FilterChainNode (L61) / ClusterFilterChainNode (L162) / CallbackRegistrationInvoker (L184) / ClusterCallbackRegistrationInvoker (L279) / CopyOfFilterChainNode (L311) / CopyOfClusterFilterChainNode (L394, "replace ClusterFilterChainNode with this one when proved stable enough" 注释) (3.x 锚)
- AbstractCluster.java:92: buildClusterInvokerChain — **集群级 Filter 链 join 时织入** (3.x 锚)
- InvocationInterceptorBuilder.java: @SPI("default") — 3.x 拦截器并行面 (3.x 锚)
- ThreadPool SPI: fixed/cached/limited/eager 4 实现 (dubbo-common internal 注册表) + ExecutorRepository (manager/)
- DubboInvoker.java:145-148: "save for 2.6.x compatibility, for example, TraceFilter in Zipkin uses com.alibaba.xxx.FutureAdapter" (历史注释锚 — 迁移面实证)
- AsyncRpcResult.java:237-247: InvokeMode 三模式 (2.7+ 锚); RpcUtils.getInvokeMode (L215-230): FUTURE 返回值自动检测 / ASYNC async 配置 / SYNC 兜底
- 执行计划 DB-4 断言 "ProtocolFilterWrapper" 未提模块 — 实为 dubbo-cluster (09 审计修正)

## 推断标注

- "2.x 同步阻塞" — Dubbo 2.x 公知版本线 (标注)
- "2.7.x 异步化" — AsyncRpcResult/InvokeMode 存在性推断 (标注)
- "3.x FilterChainBuilder" — 类名/包路径实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- Spring AOP 责任链 (4-2 Advice 链): 拦截器顺序 vs Dubbo Filter 倒序包装 — 责任链对照
- Feign (F-3 代理生成): InvocationHandler → 同步 HTTP vs Dubbo 异步底座 — 客户端对照
- Netty (N-2 EventLoop): 线程模型 vs Dubbo 全异步 — 并发对照
