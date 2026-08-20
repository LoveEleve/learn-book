# D-7 集群容错 — 装配→重试→选择→策略族

> 前置: [[D-1-SPI微内核]] [[D-3-服务引用]] [[D-5-注册中心]] [[D-6-负载均衡]] | 引出: [[D-8a-传输抽象]] | 对照: Sentinel 降级 + Ribbon 重试
> 🔴 A (🟡→🔴 升级) | 8 KP | [模式: 策略族 + 重试 + 装饰链]
> Pass 2 闭环: q1(装配面) q2(重试面) q3(选择粘滞) q4(策略族+路由+Mock)

**读者处境**: 集群里调用失败怎么办? 9 种容错策略怎么选? 路由在哪执行? 这篇拆 AbstractCluster.join + FailoverClusterInvoker + select 粘滞 + 策略族。

### 1. 装配面 — Cluster SPI + join 三层包装

场景: 集群容错怎么装配? join 时发生了什么?
源码路径:
- **Cluster @SPI(Cluster.DEFAULT)** (Cluster.java:34) — 默认 failover; **SPI 注册表 11 项** (9 实现 + mock/scope 2 装饰 wrapper)
- **AbstractCluster.join** (support/wrapper/AbstractCluster.java:57-64): buildFilterChain → **buildClusterInterceptors(doJoin(directory))**
  - 1) **ClusterFilterInvoker** (L82-108): FilterChainBuilder.**buildClusterInvokerChain** — D-4 集群级 Filter 链钩子兑现!
  - 2) **buildInterceptorInvoker** (L66-75): InvocationInterceptorBuilder → 3.x 拦截器
  - 3) CLUSTER_INTERCEPTOR_COMPATIBLE_KEY → 2.7 兼容模式
- **doJoin** 抽象 → FailoverCluster.doJoin → new FailoverClusterInvoker(directory)
关键设计 (q1): **join 三层包装 (策略→集群 Filter 链→拦截器) + Wrapper 装饰 (mock/scope)**。[模式: 装配面]

### 2. 重试面 — FailoverClusterInvoker

场景: 默认策略怎么重试?
源码路径:
- **doInvoke** (L57-127): **calculateInvokeTimes** (L129-142: 方法级 RETRIES+1, ⚠ **DEFAULT_RETRIES=2** CommonConstants:419, RpcContext 附件覆盖) → 重试循环
- 循环: i>0 时 **list(invocation) 重新拉目录** (地址变化生效) → **select(..., invoked)** (D-6 selected 钩子) → invokeWithContext
- **业务异常不重试**: e.isBiz() → 直接抛 (L104-107)
- 失败收集 providers → 全部失败抛 "Tried N times (X/Y)" 完整错误
关键设计 (q2): **换节点重试 + 重试前刷新目录 + 业务异常不重试**。[模式: 重试面]

### 3. 选择面 — select 粘滞 + reselect

场景: 集群选择与单节点选择有何不同?
源码路径:
- **select** (AbstractClusterInvoker.java:155-185): **sticky 粘滞** (⚠ **CLUSTER_STICKY_KEY 默认 false** Constants.java:75, 同方法连续同节点) — invokers 不含自动解除 / availableCheck / 记录 stickyInvoker
- **reselect** (L254-328): **三段式** — ①未选过且可用 → loadbalance.select (⚠ **reselectCount 限制**, 防大集群挂起 L268-271) ②空 → 复查 selected 里可用的 (兜底放宽) ③最终; "**selected > available**" 规则 (L142-143 注释)
- list (L452): directory.list — 路由链执行点
关键设计 (q3): **会话亲和 (粘滞) + 重试不撞车 (selected 规则)**。[模式: 选择面]

### 4. 策略族 + 路由 + Mock

场景: 9 种策略差异? 路由在哪? 降级?
源码路径:
- **9 策略** (doInvoke 穷举): Failover (重试, 默认) / Failfast (立即抛) / Failsafe (吞错) / **Failback** (RetryTimerTask + failTimer 异步重试, RETRY_FAILED_PERIOD) / **Forking** (FORKS_KEY 并行 + invokeWithContextAsync 先到先得) / **Broadcast** (全广播 + ⚠ **broadcast.fail.percent 失败阈值 0~100, 默认 100=最后才抛** — PR #7174) / Available (第一个可用) / **Mergeable** (MERGER_KEY + MergerFactory 合并, 无 merger 只调一组) / **ZoneAware** (3.x: **三级优先** — ①preferred=true 注册中心最高优先 L67-69 ②REGISTRY_ZONE 附件/ZoneDetector 检测 zone L72-78 ③同 zone provider 优先 L80+)
- **Router 链 7 族** (router/ 包): condition / script / tag / state / file / affinity / mesh / mock — 执行点 directory.list (D-5 预路由缓存 → 实际路由)
- **Mock 降级** (MockClusterWrapper:28-38): wrapper 织入 — mock 调用/默认值兜底
关键设计 (q4): **策略 = 失败语义选择 + 路由两段式 + Mock 不侵入**。[模式: 策略族]

## 代码类型
Architecture (策略族) + Concurrency (重试/并行)

## 负面空间 (D-7, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不动态换策略 | join 时定死, 换需重建引用 (q1) |
| 不重试业务异常 | isBiz 直接抛 (q2) |
| 不无限重试 | retries+1 有限 (q2) |
| 不强制粘滞 | 粘滞优先可用性 (q3) |
| 不策略自动选择 | 显式 CLUSTER_KEY (q4) |
| 不做请求级幂等 | 重试幂等由用户保证 (q2) |

## 结尾桥 OUTBOUND

- → [[D-8a-传输抽象]]: ClusterInvoker 末端 → ExchangeClient — 网络层深潜
- → 对照: Sentinel 降级 (规则驱动) vs Dubbo Mock (配置驱动) / Ribbon 重试
- → 阶段 5.9 Sentinel (ST-1~ST-4): 容错对照
