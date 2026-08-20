# C-3 temporal-trace — 时空溯源

> 本地 git 为 release 单提交浅克隆; 以下为代码内证据 + 与 ZK 官方 recipes 的对照演进。

## 演进线

| 代际 | 事件 | 证据 |
|---|---|---|
| Netflix Curator 1.x | InterProcessMutex/LockInternals 基础定型 (顺序节点+前驱 watch) | 结构稳定至今 |
| ~2.0 | InterProcessReadWriteLock 引入; 前缀等长约束 | 代码注释 "must be the same length" (InterProcessReadWriteLock.java:60-62) |
| ~2.0 | Revocable/Revoker 撤销机制 | InterProcessMutex.makeRevocable (L157-170) |
| 未知 | "use getData() instead of exists() to avoid leaving unneeded watchers" — watcher 资源泄漏修复 | LockInternals.java:242-243 注释 |
| 未知 | InterProcessSemaphore → V2: SharedCount 动态上限 + lease 模式 | V2 类注释双模式 (L59-66) |
| 至今 | guaranteed 删除 + FailedDeleteManager 后台重删 | LockInternals.java:283 |

## 与 ZK 官方 recipes WriteLock 对照 (阶段4.3 Z-8 已分析)

| 维度 | ZK 官方 WriteLock | Curator InterProcessMutex |
|---|---|---|
| 重试 | 固定 RETRY_COUNT=10 线性退避 | RetryPolicy 可插拔 (C-1) |
| 可重入 | 无 | threadData lockCount |
| 超时 | 无 (前驱永不死则永等) | acquire(time, unit) |
| 幂等创建 | sessionId 前缀手动扫描 | withProtection GUID (框架层) |
| 前驱消失 | 只 log.warn (悬挂) | 重查/重试策略 |
| 会话失效 | SessionExpired 直抛 | 状态机联动 (LOST) + 重连重试 |
| 释放 | delete(-1) 无 guaranteed | guaranteed 后台重删 |

**结论**: Curator 锁 = ZK 官方锁算法 + 框架层五件套 (重试/超时/可重入/幂等/可靠删除)。教学上可与 Z-8 对照着讲。
