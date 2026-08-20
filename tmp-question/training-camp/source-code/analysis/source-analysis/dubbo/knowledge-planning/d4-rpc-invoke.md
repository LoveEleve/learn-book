# D-4 RPC 调用 — 全异步底座与双链 Filter

> 项目: Dubbo | 🔴 Deep / 1 篇 | InvocationUtil+ProtocolFilterWrapper(cluster)+DefaultFilterChainBuilder+DubboInvoker+AsyncRpcResult+RpcUtils
> 基线: DUBBO-PLAN D-4 (调用链) — 前置: **D-1 (getActivateExtension) + D-2/D-3 (协议/invoker)** — 展开 入口→Filter 链→发送→异步结果

---

## §0.8

- 🔴 Deep，1篇 — 入口(**InvocationUtil.invoke L39-106: RpcContext.storeServiceContext 快照→setConsumerUrl→invoker.invoke(rpcInvocation).recreate() 两拍**) → 调用链(**ClusterInvoker[D-7 黑盒]→Filter 链→协议 invoker**) → Filter 链(**ProtocolFilterWrapper cluster/filter:66-73[registry URL 透传/非 registry→buildInvokerChain REFERENCE_FILTER_KEY CONSUMER]; DefaultFilterChainBuilder L43-77[getActivateExtension 激活筛选+多 ModuleModel 去重+倒序包装 CopyOfFilterChainNode+CallbackRegistrationInvoker]; 链节点=FilterChainBuilder.java 内部类家族[FilterChainNode L61/ClusterFilterChainNode L162/CopyOf L311]; 集群级链 buildClusterInvokerChain L53[AbstractCluster L92 join 织入]**) → 发送(**DubboInvoker.doInvoke L89-161: 连接轮询 index%size[Shared/ExclusiveClientsProvider DubboProtocol:462-481]→isOneway[return=false RpcUtils:232]→timeout 前置[TIMEOUT_TERMINATE]→Request→currentClient.request→CompletableFuture→AsyncRpcResult; FutureContext 2.6 兼容**) → 异步结果(**AsyncRpcResult.recreate L237-247: FUTURE→返回 Future/ASYNC→默认值/SYNC→getAppResponse; InvokeMode 推导 RpcUtils:215-230[返回值 Future 自动检测]**) → 线程池(**ExecutorRepository+ThreadPool SPI 4 实现 fixed/cached/limited/eager**) → 上下文(**RpcContext 三件套 getServiceContext L204/getClientAttachment L170/getServerAttachment L179**)
- 设计模式: [模式: 责任链(Filter)+全链路异步+装饰器双链]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| InvocationUtil.java:39-106 | 入口 | **两拍结构: invoke 异步 + recreate 同步化; RpcContext 快照/恢复** | High |
| ProtocolFilterWrapper.java:66-73 | 链织入 | **registry URL 透传; 非 registry→buildInvokerChain(CONSUMER)** — 3.x 在 dubbo-cluster | High |
| DefaultFilterChainBuilder.java:43-77 | 链构建 | **getActivateExtension(url,key,group)→多 ModuleModel 去重→倒序包装→CallbackRegistrationInvoker** | High |
| FilterChainBuilder.java:53,162,311 | 双链 | **集群级链: buildClusterInvokerChain + ClusterFilterChainNode + CopyOfClusterFilterChainNode (L394)** | High |
| AbstractCluster.java:92 | 织入点 | **集群链在 Cluster.join 时织入** — 双链层次 (协议级 refer 织/集群级 join 织) | High |
| DubboInvoker.java:89-161 | 发送 | **连接轮询→oneway→timeout 前置→Request→异步请求→AsyncRpcResult** | High |
| RpcUtils.java:215-230 | InvokeMode | **推导: 返回值 Future→FUTURE 自动检测/async 配置→ASYNC/否则 SYNC** | High |
| AsyncRpcResult.java:237-247 | recreate | **三模式: FUTURE 返回 Future/ASYNC 默认值/SYNC getAppResponse 等待** | High |
| ThreadPool SPI | 线程池 | **fixed/cached/limited/eager 4 实现 + ExecutorRepository** | High |
| RpcContext.java:170-204 | 上下文 | **三件套: service/client/server 分离** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 调用链单管线 (入口→链→发送→结果) — 1篇按四段展开; 容错在 D-7 (导航), 传输在 D-8a (导航), 序列化在 D-10 (导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 两拍结构 (invoke 异步 + recreate 同步化) | 🔴 | **为什么🔴**: 全链路异步底座 |
| P1-2 | Filter 链构建 (激活筛选+倒序包装) | 🔴 | **为什么🔴**: 横切面核心 |
| P1-3 | 双链层次 (协议级+集群级) | 🔴 | **为什么🔴**: 3.x 关键架构 |
| P1-4 | InvokeMode 三模式 (FUTURE 自动检测) | 🔴 | **为什么🔴**: 调用语义 |
| P2-1 | 连接轮询 + oneway + 超时前置 | 🟡 | **为什么🟡**: 发送优化面 |
| P2-2 | ThreadPool SPI + RpcContext 三件套 | 🟡 | **为什么🟡**: 线程/上下文 |
| P3-1 | FutureContext 2.6 兼容 | 🟢 | **为什么🟢**: 迁移细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **入口与异步底座** | 🔴 | 全链路 |
| B | **Filter 双链** | 🔴 | 横切面 |
| C | **协议发送** | 🔴 | 网络入口 |
| D | **异步结果+线程** | 🟡 | 支撑面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 入口与上下文 | RpcContext 快照/恢复防嵌套串扰; 两拍: invoke 全异步 + recreate 最后一拍同步化 — 3.x 全链路异步 | InvocationUtil.java:39-106 |
| q2 | Filter 链 | ProtocolFilterWrapper (D-1 Wrapper 织入!) 在 refer/export 织链; CONSUMER/PROVIDER group 分流; 倒序包装 LIFO 责任链; registry URL 透传 (注册引用不过滤器) | ProtocolFilterWrapper.java:52-73; DefaultFilterChainBuilder.java:43-77 |
| q3 | 发送 | 连接级轮询 (index%size) 区别于 D-6 节点级; isOneway return=false 单向不等; timeout 前置 TIMEOUT_TERMINATE 不白等; 异常分级 TIMEOUT/SERIALIZATION/NETWORK | DubboInvoker.java:89-161 |
| q4 | 异步结果 | InvokeMode 推导: **返回值 Future 自动检测 FUTURE** (非全手动); FUTURE/ASYNC/SYNC 共用一条异步链 — recreate 一拍切换; FutureContext 2.6 兼容 (Zipkin) | RpcUtils.java:215-230; AsyncRpcResult.java:237-247 |

→ 引出 D-5 注册中心 (ClusterInvoker 黑盒→RegistryDirectory); D-7 集群容错 (doInvoke 深潜); D-8a 传输抽象 (ExchangeClient.request 深潜); D-10 序列化 (协议编码)。
