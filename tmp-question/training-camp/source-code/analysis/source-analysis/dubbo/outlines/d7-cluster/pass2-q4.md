# D-7 集群容错 — Pass 2 闭环 Q4: 策略族 + 路由面 + Mock 降级

> 核心: 9 策略差异 + Router 链 + MockCluster | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 9 种容错策略差异? 路由在哪执行? 降级 (mock) 怎么工作?**

## 机制链 (已实证)

```
9 策略差异 (doInvoke 穷举实证):
├── Failover: 重试 N 次 (换节点, q2) ← 默认
├── Failfast: 一次失败立即抛 (不重试)
├── Failsafe: 失败吞掉返回空结果 (静默, 日志)
├── Failback: 失败异步重试 (RetryTimerTask + failTimer.newTimeout RETRY_FAILED_PERIOD, L103-106;
│   "Asynchronous call method must be used here" 注释 L124)
├── Forking: FORKS_KEY 并行调 forks 个 (forks<=0/>=size → 全部, L76-80), invokeWithContextAsync 先到先得
├── Broadcast: 广播调全部; ⚠ broadcast.fail.percent 失败阈值 0~100 (默认 100=最后才抛, L58-74, PR #7174)
├── Available: 遍历第一个可用节点 (L39)
├── Mergeable: MERGER_KEY + MergerFactory 合并 (L64-65); 无 merger → 只调一个 group
└── ZoneAware (3.x): **三级优先** (ZoneAwareClusterInvoker L60-110):
    ├── 1) preferred=true 注册中心最高优先 (L67-69)
    ├── 2) REGISTRY_ZONE 附件 → ZoneDetector.getZoneOfCurrentRequest (L72-78)
    └── 3) 同 zone provider 优先 (L80+) → 兜底其它

Router 链 (router/ 包 7 族):
├── condition (条件路由, 主流) / script / tag (标签路由) / state / file / affinity / mesh / mock
├── 执行点: AbstractClusterInvoker.list → directory.list(invocation) (L452)
│   ← RegistryDirectory 预路由缓存 (D-5 钩子) → 实际路由决策在这里
└── AbstractRouter (基类) + RouterChain 链式执行

Mock 降级 (MockClusterWrapper.java:28-38):
├── wrapper 织入: cluster.join 外包 MockClusterInvoker
├── mock=true/fail → mock 方法调用 / 返回默认值
└── 降级面: 服务不可用时返回兜底, 不抛错
```

## 关键设计 (why)

1. **策略 = 失败语义选择**: 幂等读 (failover 重试) / 非幂等写 (failfast) / 日志类 (failsafe) / 聚合 (mergeable) — 按业务选
2. **Forking 先到先得**: 并行放大延迟收益 (最快响应), 代价是放大流量
3. **ZoneAware 区域亲缘**: 同 zone 优先 — 跨机房延迟/成本优化
4. **路由链在目录层执行**: RegistryDirectory 预路由 (D-5) + 实际路由 (list) — 两段式
5. **Mock 降级 = Wrapper 织入**: 不侵入策略实现 — D-1 机制应用

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 9 策略 doInvoke | dubbo-cluster support/*ClusterInvoker.java |
| ZoneAware preferred 注释 | support/registry/ZoneAwareClusterInvoker.java:46-54 |
| Router 实现族 7 族 | dubbo-cluster router/ 包 |
| 路由执行点 (list) | AbstractClusterInvoker.java:452 |
| MockClusterWrapper | support/wrapper/MockClusterWrapper.java:28-38 |

## 负面空间 (Q4 面)

- 不做策略自动选择 (显式 CLUSTER_KEY)
- 不做 Forking 结果校验 (先到先得, 不校验一致性)
- 不做 Mock 热切换 (引用时定)
