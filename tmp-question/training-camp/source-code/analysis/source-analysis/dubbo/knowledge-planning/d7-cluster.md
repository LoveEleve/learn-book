# D-7 集群容错 — 9 策略与重试

> 项目: Dubbo | 🔴 Deep (🟡→🔴) / 1 篇 | Cluster SPI+AbstractCluster+FailoverClusterInvoker+AbstractClusterInvoker+Router 族
> 基线: DUBBO-PLAN D-7 (治理, 数字修正 8→9) — 前置: **D-6 (selected 重选)** — 展开 装配→重试→选择→策略族

---

## §0.8

- 🔴 Deep，1篇 — 装配(**Cluster @SPI(Cluster.DEFAULT) L34 默认 failover; SPI 注册表 11 项[9 实现+mock/scope 装饰]; AbstractCluster.join L57-64→buildClusterInterceptors[ClusterFilterInvoker L82-108 集群 Filter 链[D-4 双链兑现]+buildInterceptorInvoker L66-75[InvocationInterceptorBuilder]+2.7 兼容开关]**) → 重试(**FailoverClusterInvoker.doInvoke L57-127: calculateInvokeTimes[RETRIES 默认 2+1 次 L129-142]→重试循环[重试前 list 刷新目录→select(...,invoked)→业务异常 isBiz 不重试 L104-107→失败收集 providers]→"Tried N times (X/Y)" 完整错误**) → 选择(**AbstractClusterInvoker.select L155-185: sticky 粘滞[默认 false]+doSelect+记录; reselect L254-328 三段式[未选过且可用→loadbalance.select→空则复查 selected→兜底; reselectCount 限制防大集群挂起]**) → 策略族(**9 策略: Failover 重试/Failfast 立即抛/Failsafe 吞错/Failback 异步[RetryTimerTask]/Forking 并行先到先得[FORKS_KEY]/Broadcast 广播+fail.percent 阈值[PR #7174]/Available 首可用/Mergeable 合并[MERGER_KEY+MergerFactory]/ZoneAware 三级优先[preferred→zone→同 zone]**) → 路由(**Router 7 族 condition/script/tag/state/file/affinity/mesh; 执行点 directory.list L452[D-5 预路由兑现]**) → 降级(**MockClusterWrapper L28-38: wrapper 织入 mock 调用兜底**)
- 设计模式: [模式: 策略族+装饰链+重试+路由]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Cluster.java:34 | 默认 | **@SPI(Cluster.DEFAULT) = failover; SPI 注册表 11 项 (9 实现+2 装饰)** | High |
| AbstractCluster.java:57-64 | join | **join = buildClusterInterceptors(doJoin): ClusterFilterInvoker (集群链) + InvocationInterceptor (3.x)** | High |
| FailoverClusterInvoker.java:129-142 | 次数 | **calculateInvokeTimes: RETRIES+1 (默认 2→共 3 次), RpcContext 附件可覆盖** | High |
| FailoverClusterInvoker.java:104-107 | isBiz | **业务异常不重试 (e.isBiz() 直接抛)** — 幂等语义 | High |
| AbstractClusterInvoker.java:254-328 | reselect | **三段式 + reselectCount 限制** — 防大集群挂起 | High |
| BroadcastClusterInvoker.java:58-74 | 阈值 | **broadcast.fail.percent 0~100 默认 100 (最后才抛) — PR #7174** | High |
| MergeableClusterInvoker.java:64-65 | 合并 | **MERGER_KEY + MergerFactory** — 无 merger 只调一组 | High |
| ZoneAwareClusterInvoker.java:60-110 | zone | **三级优先: preferred=true → REGISTRY_ZONE/ZoneDetector → 同 zone** | High |
| AbstractClusterInvoker.java:452 | 路由 | **list → directory.list — 路由链执行点 (D-5 预路由兑现)** | High |
| MockClusterWrapper.java:38 | 降级 | **mock 调用兜底 — wrapper 织入不侵入策略** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 集群容错单机制 (装配+重试+选择+策略) — 1篇按四段展开; 三钩子兑现 (D-4 doInvoke 黑盒/D-5 预路由/D-6 selected 重选)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | join 三层包装 (策略+集群链+拦截器) | 🔴 | **为什么🔴**: 装配核心 |
| P1-2 | Failover 重试 (换节点/isBiz/目录刷新) | 🔴 | **为什么🔴**: 默认策略 |
| P1-3 | sticky + reselect 三段式 | 🔴 | **为什么🔴**: 选择语义 |
| P1-4 | 9 策略差异 (失败语义选择) | 🔴 | **为什么🔴**: 策略族 |
| P2-1 | ZoneAware 三级优先 | 🟡 | **为什么🟡**: 区域亲缘 |
| P2-2 | Broadcast fail.percent 阈值 | 🟡 | **为什么🟡**: 3.x 新增 |
| P2-3 | 路由链 + Mock 降级 | 🟡 | **为什么🟡**: 路由/降级面 |
| P3-1 | 2.7 兼容开关 | 🟢 | **为什么🟢**: 迁移细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **装配三层包装** | 🔴 | 主线 |
| B | **重试策略** | 🔴 | 默认行为 |
| C | **选择与粘滞** | 🔴 | 语义 |
| D | **策略族+路由** | 🟡 | 扩展面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 装配面 | join 三层包装: doJoin (容错策略) → ClusterFilterInvoker (D-4 集群链钩子兑现, buildClusterInvokerChain) → InvocationInterceptor (3.x); Wrapper 装饰 (mock/scope) 是 D-1 实例 | AbstractCluster.java:45-108 |
| q2 | 重试面 | Failover: RETRIES 默认 2 (共 3 次) + RpcContext 附件覆盖; **重试前 list 刷新目录 (地址变化生效); 业务异常 isBiz 不重试; invoked 列表换节点不撞车** | FailoverClusterInvoker.java:57-142 |
| q3 | 选择面 | sticky 默认 false (CLUSTER_STICKY_KEY); reselect 三段式: 未选过且可用→selected 兜底; **reselectCount 限制防大集群挂起** | AbstractClusterInvoker.java:155-185,254-328 |
| q4 | 策略族 | 策略=失败语义选择: 幂等读 failover/非幂等写 failfast/日志 failsafe; **Broadcast fail.percent 阈值 (PR #7174); ZoneAware 三级 (preferred→zone→同 zone); Forking 并行先到先得; Mergeable MergerFactory**; 路由链在 directory.list 执行 (两段式) | 各 *ClusterInvoker.java |

→ 引出 D-8a 传输抽象: ClusterInvoker 末端 → ExchangeClient 网络层深潜; 对照 Sentinel 治理。
