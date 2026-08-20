# D-5 注册中心 — Pass 2 闭环 Q2: 失败重试面 (FailbackRegistry)

> 核心: FailbackRegistry (support/) + retry/ 包 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 注册/订阅失败怎么办? 重试机制怎么设计?**

## 机制链 (已实证)

```
FailbackRegistry (support/FailbackRegistry.java)
"A template implementation of registry service that provides auto-retry ability" (L47 注释)
├── 4 种失败任务 (retry/ 包, 全部继承 AbstractRetryTask 模板):
│   ├── FailedRegisteredTask — 注册失败重试 (doRetry → doRegister + removeFailedRegisteredTask)
│   ├── FailedSubscribedTask — 订阅失败重试
│   ├── FailedUnregisteredTask — 注销失败重试
│   └── FailedUnsubscribedTask — 退订失败重试
├── HashedWheelTimer retryTimer (L68) — 时间轮定时器
│   └── 注释 (L67): "regular check if there is a request for failure, and if there is, an unlimited retry"
├── retryPeriod (L65) — 重试周期 (REGISTRY_RETRY_PERIOD_KEY 可配)
├── ⚠ 重试次数: REGISTRY_RETRY_TIMES 默认 **-1 = 无限** (Constants.java:70)
│   └── >0 时: AbstractRetryTask L112 "retryTimes > 0 && times > retryTimes" → 停止重试
└── 模板方法: 实现类继承 doRegister/doSubscribe 等, 失败 → 任务入队 → 定时重试
```

## 关键设计 (why)

1. **模板方法模式**: 实现类只写"真操作" (doRegister/doUnregister/doSubscribe/doUnsubscribe), 重试框架统一处理
2. **4 任务分类 + AbstractRetryTask 父模板**: 注册/订阅/注销/退订独立重试 — 失败互不拖累; 次数/周期/取消统一管理
3. **HashedWheelTimer 无限重试 (默认)**: 时间轮定时检查失败队列 — 默认 -1 无限 (L67 注释 + Constants.java:70 实证), **可配次数限制** (L112)
4. **网络抖动自愈**: 与 AbstractRegistry 本地缓存配合 — 抖动期间用缓存, 恢复后重试补注册

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| FailbackRegistry 模板 + retryPeriod | support/FailbackRegistry.java:47-68 |
| 4 种失败任务 | registry/retry/Failed*Task.java |
| 无限重试注释 | FailbackRegistry.java:67 |
| AbstractRegistry.notify (缓存兜底) | AbstractRegistry.java:545-587 |

## 负面空间 (Q2 面)

- 不指数退避 (固定 retryPeriod, 非退避)
- 不重试上限 (unlimited — 直到成功/销毁)
- 不持久化失败队列 (重启丢失, 靠 AbstractRegistry 缓存 + 重新订阅恢复)
