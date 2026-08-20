# D-7 集群容错 — Pass 2 闭环 Q2: 重试策略面 (FailoverClusterInvoker)

> 核心: FailoverClusterInvoker.doInvoke (L57-127) — 默认策略 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Failover 怎么重试? 重试几次? 什么异常不重试? 重试时节点列表会变吗?**

## 机制链 (已实证)

```
FailoverClusterInvoker.doInvoke (L57-127):
├── len = calculateInvokeTimes(methodName) (L129-142):
│   ├── len = 方法级 RETRIES_KEY + 1 (⚠ 默认 DEFAULT_RETRIES=2 → 共 3 次)
│   ├── RpcContext RETRIES 附件可覆盖 (Number) → 覆盖后移除附件
│   └── len <= 0 → 1 (至少一次)
├── 重试循环 (for i < len):
│   ├── i > 0: checkWhetherDestroyed → copyInvokers = list(invocation)
│   │   ← 重新拉目录! (invokers 可能已变化) → checkInvokers
│   ├── invoker = select(loadbalance, invocation, copyInvokers, invoked) ← D-6 selected 钩子
│   ├── invoked.add(invoker) + RpcContext.setInvokers
│   ├── result = invokeWithContext(invoker, invocation)
│   ├── 成功 → return (若有先前失败, 记 CLUSTER_FAILED_MULTIPLE_RETRIES 警告)
│   ├── catch RpcException e: e.isBiz() → 直接抛 (业务异常不重试!)
│   │   ← 幂等性判断: 业务异常重试无意义/有副作用
│   └── 失败 → providers.add(失败地址) (收集)
└── 全部失败 → RpcException "Tried N times of the providers (X/Y) from the registry..."
    (含完整失败信息: 尝试次数/失败数/总节点/注册中心/版本)
```

## 关键设计 (why)

1. **重试 = 换节点**: 每次 select 带 invoked 列表 — 不会重试同一节点 (D-6 重选协作)
2. **重试前刷新目录**: list(invocation) 重新拉 — 注册中心地址变化在重试窗口内生效
3. **业务异常不重试**: isBiz() 判断 — 非网络/框架异常重试无意义 (且可能放大副作用)
4. **上下文传播**: invokeWithContext + setInvokers — 粘滞/追踪可读
5. **失败信息完整**: "Tried N times (X/Y)" — 可诊断性 (重试了谁/失败了多少)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| doInvoke 重试循环 | FailoverClusterInvoker.java:57-127 |
| calculateInvokeTimes (RETRIES 覆盖) | FailoverClusterInvoker.java:129-142 |
| 业务异常不重试 (isBiz) | FailoverClusterInvoker.java:104-107 |
| 重试前目录刷新 (list) | FailoverClusterInvoker.java:66-68 |
| 失败收集 + 完整错误 | FailoverClusterInvoker.java:118-127 |

## 负面空间 (Q2 面)

- 不重试业务异常 (isBiz 直接抛)
- 不无限重试 (len = retries + 1, 默认有限)
- 不做请求级幂等保障 (重试幂等由用户保证)
