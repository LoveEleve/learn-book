# D-7 集群容错 — Pass 2 闭环 Q3: 选择与粘滞面 (AbstractClusterInvoker.select)

> 核心: select sticky + reselect | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 集群选择有什么特殊? 粘滞是什么? 重选规则怎么保证不漏节点?**

## 机制链 (已实证)

```
AbstractClusterInvoker.select (L155-185):
├── 空列表 → null
├── sticky 粘滞 (⚠ CLUSTER_STICKY_KEY 默认 false, Constants.java:75; 方法级):
│   ├── stickyInvoker 失效: invokers 不再包含 → 置空 (节点下线自动解除)
│   ├── sticky 命中 && 可用 (availableCheck) → 直接返回 (同节点连续调用)
│   └── sticky 记录: doSelect 后 stickyInvoker = invoker
├── doSelect(loadbalance, invocation, invokers, selected) — D-6 负载均衡调用点
└── 注释 (L140-141): "a) Firstly, select an invoker using loadbalance. If this invoker is in
    previously selected list, or if this invoker is unavailable, then continue step b (reselect)"

reselect (L254-328) — 三段式 "selected > available":
├── 1) 未选过且可用 (reselectCount 限制候选数, 防大集群挂起 L268-271) → loadbalance.select
├── 2) 候选空 → 复查 selected 里可用的 (兜底放宽)
└── 3) 最终兜底
注释 (L142-143): "the validation rule for reselection: selected > available. This rule
    guarantees that the selected invoker has the minimum chance to be one in the previously selected list"
```

## 关键设计 (why)

1. **粘滞 = 会话亲和**: 同方法连续打同节点 — 本地缓存/JIT 友好; 节点下线自动解除
2. **selected > available 重选规则**: 重选优先"未选过且可用" — 重试不撞车
3. **粘滞不跨节点列表**: invokers 变化 → stickyInvoker 置空 — 目录更新不残留
4. **availableCheck**: 粘滞命中也要检查可用性 — 不粘已死节点

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| select sticky 完整 | AbstractClusterInvoker.java:155-185 |
| 重选注释 (selected > available) | AbstractClusterInvoker.java:140-143 |
| reselect 实现 | AbstractClusterInvoker.java:254-328 |
| list (目录刷新) | AbstractClusterInvoker.java:452 |

## 负面空间 (Q3 面)

- 不做强制粘滞 (粘滞优先可用性)
- 不做粘滞持久化 (进程内存)
- 不粘不可用节点 (availableCheck)
