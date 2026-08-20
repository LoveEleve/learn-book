# K-7 Purgatory — 全视角提问验证 (completeness)

> 验证时机: 2 篇大纲深审前。身份: 开发者/架构师/性能工程师/SRE/研究者/子系统开发者/学生

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 状态机 | completed 怎么保证只完成一次? | ✅ 01-L2 (双检锁) |
| 2 | 开发者 | 状态机 | forceComplete 与超时冲突? | ✅ 01-L2 (cancel 取消定时) |
| 3 | 开发者 | 注册 | tryCompleteElseWatch 做什么? | ✅ 01-L3 (先试后挂) |
| 4 | 开发者 | 触发 | 条件满足怎么唤醒? | ✅ 01-L3 (checkAndComplete) |
| 5 | 开发者 | 时间轮 | add 三分支是什么? | ✅ 02-L2 (过期/本层/溢出) |
| 6 | 开发者 | 时间轮 | 溢出怎么处理? | ✅ 02-L2 (overflowWheel 升层) |
| 7 | 架构师 | 设计 | 为什么条件+超时双保险? | ✅ 01-L3 (不丢请求) |
| 8 | 架构师 | 设计 | 为什么 watch 分片? | ✅ 01-L3 (SHARDS=512 减锁) |
| 9 | 架构师 | 设计 | 死锁怎么防? | ✅ 01-L3 (DelayedOperationPurgatory.java:L135-154 注释) |
| 10 | 架构师 | 设计 | 为什么不用调度堆? | ✅ 02-L3 (O(1) vs O(log n)) |
| 11 | 性能工程师 | 时间轮 | 精度上限? | ✅ 02-L3 (tick 粒度) |
| 12 | 性能工程师 | 批量 | 桶批量过期省什么? | ✅ 02-L3 (共享调度) |
| 13 | SRE | 超时 | 请求超时会怎样? | ✅ 01-L2 (onExpiration) |
| 14 | SRE | 监控 | 延迟操作数量怎么监控? | ⚠️ 01 未显式提 → 补一句 (estimatedTotalOperations 指标) |
| 15 | 研究者 | 对照 | vs Netty HashedWheelTimer? | ✅ 02-L4 (同构/差异) |
| 16 | 研究者 | 对照 | vs 调度堆? | ✅ 02-L3 (O(1) vs O(log n)) |
| 17 | 子系统开发者 | 衔接 | acks=all 与 K-4 HW? | ✅ 01-L4 (checkEnoughReplicasReachOffset) |
| 18 | 学生 | 概念 | 延迟操作通俗解释? | ✅ 01-L1 (不阻塞线程) |

**统计**: ✅ 17 / ⚠️ 1 / ❌ 0 — ⚠️ 1 项"补一句"级
→ 回补 1 项: 01-L3 补 estimatedTotalOperations 指标

## 回补清单

1. 01-L3: 补一句 "指标: estimatedTotalOperations (DelayedOperationPurgatory.java:46-47) 监控挂起操作量"
