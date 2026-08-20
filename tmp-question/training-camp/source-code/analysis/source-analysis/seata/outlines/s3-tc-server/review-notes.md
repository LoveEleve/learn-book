# S-3 TC Server — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "6个ScheduledThreadPool" — **6 个实证** (retryRollbacking/retryCommitting/asyncCommitting/timeoutCheck/undoLogDelete/syncProcessing, L182-198) 全 1 线程; 另有 branchRemoveExecutor (ThreadPool cores×2) | 大纲 §1 |
| 2 | **表述精确化** | 执行计划 "5种GlobalStatus状态组" — 实证为 **5 组筛选条件数组** (3/1/1/1/4) 非"21 态分 5 组"; retryRollbacking 组**不含 Rollbacking 本身** (含 Timeout 族+Retrying) | 大纲 §2 |
| 3 | **认知修正** | **MAX_COMMIT/ROLLBACK_RETRY_TIMEOUT 默认 -1L** (DefaultValues:520-527): isRetryTimeout `timeout >= 0 && ...` → **-1 → 永不超时 → 默认永远重试** — 靠 RETRY_DEAD_THRESHOLD 70s + timeToDeadSession 终止 | 大纲 §2 ⚠ |
| 4 | **补充锚点** | **动态延迟数学**: 无会话 → 70s; 有 → 排序后**首个 timeToDeadSession>0 的会话驱动 delay = max(time, period)** (L592-598) — harness D2 自抓修正 (首版误判为 period) | 大纲 §1 |
| 5 | **补充锚点** | **undoLogDelete 广播面**: ChannelManager.getRmChannels → 逐 RM 渠道 UndoLogDeleteRequest (saveDays, L548-570) — S-11 触发面 | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~35 锚点 + 跨文件 grep (DefaultCoordinator 108-929 / SessionHolder 429 / DefaultValues 465-527 / GlobalSession 222-228) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 调度表 (harness A)
- 状态组筛选 (harness B)
- 重试边界数学 (harness C)
- 动态延迟+分布式锁 (harness D)

### 维度2 性能
- 分区线程池 (互不阻塞)
- 动态延迟 (无会话 70s 低频)
- branchRemoveExecutor 异步删

### 维度3 内存
- 会话懒加载 (lazyLoadBranch)
- 状态组数组常量

### 维度4 一致性
- distributedLockAndExecute 防重
- 动态延迟按 beginTime 排序
- 重试有界 (dead 阈值)

### 维度5 负面空间 (已写入大纲 6 条)
- 不业务执行/不跨节点调度/不实时推/不优先队列/不背压/不无限重试

## 结论
S-3 锚点 ~35 处验证, 8 闭环完成, harness 16/16 (A-D 4 面, 自抓 1 处), **数字穷举 1 + 表述精确化 1 + 认知修正 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1/S-2 ✅; 引出 S-7/S-8 ✅; 对照 RocketMQ Broker ✅; 读者处境场景化 ✅; 锚点 ~35 ✅; 负面空间 6 条 ✅; 横切 (调度/分发/重试/会话) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §2 未提 **handleRetryCommitting 的 Committed 特判**: 状态已 Committed + 分支空 → endCommitted 收尾 (L506-509) — 重试路径的状态收束面 | 大纲 §2 补注 |
| 9 | 通过项 | 其余 ~28 句机制描述逐句对源码一致 ✅ (调度/分发/限流/会话管理/防重) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (Committed 特判 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 分区互不阻塞 | 6 独立池 — retryRollback 卡住不影响 timeoutCheck ✅ | 通过 |
| V2 | 防重闭环 | 分布式锁未获 → 重排 — 多节点只一执行 ✅ | 通过 |
| V3 | 动态频率正确 | 最早到期会话驱动 — 无会话低频 ✅ | 通过 |
| V4 | 重试终止有界 | MAX=-1 但 dead 70s → 最终 CommitFailed/RollbackFailed ✅ | 通过 |
| V5 | 限流可插拔 | RateLimiterHandler 装饰 — 默认关 ✅ | 通过 |
| V6 | undo 清理闭环 | 定时广播 → RM 执行删除 (S-11) ✅ | 通过 |
| V7 | destroy 幂等 | 三步 + instance=null — 重复调用安全 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **timeoutCheck 的 close 语义**: 超时会话 **先 close 再 changeGlobalStatus(TimeoutRollbacking)** (L433-434) — 与 S-1 commit 路径同序 (先关会话防新分支) | 大纲 §2 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (超时 close 顺序), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (endSchedule 触发条件/undoLogDelete 无 RM 渠道/distributedLock 键/RaftCoordinator/branchRemoveExecutor 条件)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | endSchedule 触发? | handleEndStatesByScheduled (L701-737): endStatuses 4 态 → **isRetryTimeout(MAX_ROLLBACK_RETRY_TIMEOUT) 才 handleEndStateSession** (L725-727) — 延迟到 retry 超时后才清终态会话 | 发现 11 (补锚) |
| T2 | undoLogDelete 无渠道? | rmChannels 空 → 直接 return (L550-554) — 无 RM 连接时跳过 (启动初期常见) | 通过 (验证) |
| T3 | distributedLock 键? | 键 = Constants (RETRY_ROLLBACKING/COMMITTING/ASYNC_COMMITTING/TX_TIMEOUT_CHECK/UNDOLOG_DELETE/ROLLBACKING/COMMITTING/END) — 每任务独立键 | 通过 (验证) |
| T4 | RaftCoordinator? | getInstance: SessionMode.RAFT → **RaftCoordinator(remotingServer)** (L253-256) — 存储模式驱动协调器 | 通过 (验证) |
| T5 | branchRemoveExecutor 条件? | **enableBranchAsyncRemove && SessionMode != FILE** (L234) — FILE 模式同步删 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 终态延迟清理 | end 态等 retry 超时 → processEndState — 防过早删锁/残留 ✅ | 通过 |
| V2 | 空渠道安全 | 无 RM → 跳过 — 不空转 ✅ | 通过 |
| V3 | 键隔离 | 每任务独立锁键 — 互不阻塞 ✅ | 通过 |
| V4 | FILE 模式同步 | 单机无并发 — 无需异步 ✅ | 通过 |
| V5 | 超时顺序一致 | close → TimeoutRollbacking 与 commit 同序 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **终态延迟清理**: end 态会话延迟到 **isRetryTimeout(MAX_ROLLBACK_RETRY_TIMEOUT) 后才 processEndState** (L725-727) — 终态清理时机面 | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 调度面 (6 池/动态延迟/防重/分支异步删) — 可写 ✅
- §2 状态组重试 (5 组/MAX=-1/超时扫描/Committed 特判) — 可写 ✅
- §3 消息分发 (onRequest/限流/doXxx/RAFT 多态) — 可写 ✅
- §4 会话管理 (SessionHolder/防重/undo 广播/终态延迟) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (终态延迟清理)。核心认知: **6 池/5 组全实证** (执行计划数字验证通过) + **MAX=-1 默认永远重试** (dead 阈值兜底) + **动态延迟 = max(剩余, period)** (harness 自抓修正)。harness 16/16 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 实现面穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (异常模板/限流实现/分布式锁实现/终态处理/RAFT 差异)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 异常处理模板? | AbstractTCInboundHandler.handle → **exceptionHandleTemplate** (AbstractExceptionHandler 模板方法); **StoreException → TransactionExceptionCode.FailedStore** (L67-69) — 存储失败异常面 | 发现 12 (补锚) |
| T2 | 限流实现? | **TokenBucketLimiter** (@LoadLevel "token-bucket-limiter" SPI): **每秒令牌/最大令牌/初始令牌 三参数** (L47-57); 拒绝 → "TransactionException[rate limit exception]" (RateLimiterHandler:90); 处理器链 AbstractTransactionRequestHandler | 发现 13 (补锚) |
| T3 | 分布式锁实现? | acquireDistributedLock → **DISTRIBUTED_LOCKER.acquireLock(new DistributedLockDO(lockKey, ip:port, EXPIRE_TIME))** (SessionHolder:400-420) — **DistributedLockerFactory 按 sessionMode** (db/raft) — S-8 交叉面 | 发现 14 (补锚) |
| T4 | 终态处理? | **processEndState 双路径**: Committed/Finished → endCommitted(true); Rollbacked/TimeoutRollbacked → endRollbacked(true) (SessionHelper:471-478) | 通过 (验证) |
| T5 | RaftCoordinator 差异? | **extends DefaultCoordinator implements ApplicationListener<ClusterChangeEvent>** (L37) — 集群变更事件监听 (S-8 交叉) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 存储失败显式码 | StoreException → FailedStore — 客户端可区分 ✅ | 通过 |
| V2 | 限流可配置 | SPI 令牌桶 3 参数 — 默认关可开 ✅ | 通过 |
| V3 | 锁持有者标识 | DistributedLockDO 含 ip:port — 可诊断可续租 ✅ | 通过 |
| V4 | 终态双路径 | commit 系/rollback 系各归其位 — 清理无遗漏 ✅ | 通过 |
| V5 | RAFT 事件驱动 | ClusterChangeEvent 监听 — 集群变更即感知 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **异常处理模板** (T1): exceptionHandleTemplate + StoreException→FailedStore — 统一异常面 | 大纲 §3 注 |
| 13 | **补充锚点** | **TokenBucketLimiter 三参数** (T2): 每秒/最大/初始令牌 — 限流实现实证 (默认关) | 大纲 §3 注 |
| 14 | **补充锚点** | **分布式锁实现** (T3): DistributedLockerFactory 按 sessionMode + DistributedLockDO(lockKey/ip:port/expire) — S-8 交叉面 | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 调度面 (6 池/动态延迟/防重/分支异步删) — 可写 ✅
- §2 状态组重试 (5 组/MAX=-1/超时扫描/Committed 特判) — 可写 ✅
- §3 消息分发 (onRequest/限流令牌桶/doXxx/异常模板/RAFT 多态) — 可写 ✅
- §4 会话管理 (SessionHolder/分布式锁实现/undo 广播/终态双路径) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (异常模板/令牌桶/分布式锁实现)。核心认知: **限流 = TokenBucketLimiter SPI** (3 参数) + **分布式锁 = DistributedLockerFactory 按存储模式** (DistributedLockDO 含持有者) + **终态双路径分派**。大纲经修复后反写测试全过。
