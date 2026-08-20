# S-7 重试故障恢复 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "6个线程池retry+RETRY_DEAD_THRESHOLD+ROLLBACK_FAILED_UNLOCK_ENABLE" — 6 池 S-3 实证; 死阈值 70s/10s; **ROLLBACK_FAILED_UNLOCK_ENABLE 默认 false** (DefaultValues:532) — **默认重试超时不解锁 (人工校准面)** | 大纲 §4 |
| 2 | **表述精确化** | "重试状态族" 需精确: **三系 14 态** (Commit 6/Rollback 5/Timeout 4 — 有重叠 Timeout 系独立); **Rollbacking 本身不在 retryRollbacking 组** (S-3 实证) | 大纲 §1 |
| 3 | **补充锚点** | **endRollbackFailed 三态分支**: isRetryTimeout → RollbackRetryTimeout(17) / Timeout 系 → TimeoutRollbackFailed(14) / else → RollbackFailed(12) (SessionHelper:249-263) | 大纲 §4 |
| 4 | **补充锚点** | **DELAY_HANDLE_SESSION** (SessionHelper:76): `!(FILE \|\| RAFT)` — DB/REDIS 持久化模式延迟清理 (防锁/分支残留) — 执行计划未提 | 大纲 §4 |
| 5 | **语义标注** | **"need to be handled it manually"** (SessionHelper:257-260): 终态即人工介入点 — 无自动告警通道 (Metrics 除外) | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (SessionHelper 76-263 / SessionStatusValidator 35-50 / DefaultValues 383-390,520-532 / GlobalSession 222-228,263-276,871-893 / DefaultCoordinator 451-574) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 状态族 (harness A)
- 重试链 (harness B)
- 死阈值 (harness C)
- 终态三态 (harness D)

### 维度2 性能
- 动态延迟 (无会话 70s)
- DELAY_HANDLE (DB 模式异步)

### 维度3 内存
- 状态数组常量
- 终态会话滞留 (10s 清理)

### 维度4 一致性
- 状态即指令 (重启可恢复)
- retrying 参数语义
- 默认不解锁

### 维度5 负面空间 (已写入大纲 6 条)
- 不无限重试保证/不自动解锁/不业务感知重试/不优先级/不跨节点/不补偿通知

## 结论
S-7 锚点 ~30 处验证, 8 闭环完成, harness 16/16 (A-D 4 面), **数字穷举 1 + 表述精确化 1 + 语义标注 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1/S-3/S-2 ✅; 对照 RocketMQ 延迟重试 ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (状态族/链/阈值/终态) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §2 未提 **handleRetryCommitting 的 Committed 特判** (S-3 发现): 状态已 Committed + 分支空 → endCommitted 收尾 (DefaultCoordinator:506-509) — 重试路径的状态收束 | 大纲 §2 补注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (状态族/链/阈值/终态) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (Committed 特判 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 状态驱动可恢复 | 状态在存储 → 重启后定时器按状态恢复 ✅ | 通过 |
| V2 | 双阈值有界 | MAX=-1 但 dead 70s → 最终终态 ✅ | 通过 |
| V3 | 终态不重试 | RollbackFailed 不在任何筛选组 → 停重试 ✅ | 通过 |
| V4 | 解锁安全 | 默认 false — 防误解锁 ✅ | 通过 |
| V5 | 延迟清理安全 | DB 模式延迟 — 防锁/分支残留 ✅ | 通过 |
| V6 | 幂等重试 | retrying=true 跳过已处理分支 ✅ | 通过 |
| V7 | 动态频率 | 最早到期驱动 — 无空转 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **endCommitted 的 retryBranch 指标**: retryGlobal 分支记录 retryBranch (CommitRetrying 状态判定, SessionHelper:148-155) — 重试提交与首次提交 Metrics 区分 | 大纲 §4 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (retryBranch 指标), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (Stop 态守卫链/解锁开关组合/终态 Metrics/AsyncWorker 与 retry 竞态/重启恢复)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | Stop 态守卫链? | queueToRetryCommit/Rollback: **StopCommitOrCommitRetry/StopRollbackOrRollbackRetry → return** (GlobalSession:875-893) — Stop 态后不重新入队 | 通过 (验证) |
| T2 | 解锁开关组合? | handleRetryRollbacking: **ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE \|\| ROLLBACK_FAILED_UNLOCK_ENABLE** 任一开才 clean (DefaultCoordinator:463-465,603-608) — 双开关 OR | 通过 (验证) |
| T3 | 终态 Metrics? | postSessionDoneEvent (SessionHelper:264) — 终态埋点 | 通过 (验证) |
| T4 | AsyncWorker 竞态? | AsyncCommitting 异步提交与 retryCommitting 不同池 — 状态互斥 (AsyncCommitting 不在 retry 组) | 通过 (验证) |
| T5 | 重启恢复? | 会话存存储 → 重启后 findGlobalSessions 按状态组找回 → 定时器续跑 — 状态即指令闭环 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | Stop 不可逆 | Stop 态守卫 — 人工干预面 ✅ | 通过 |
| V2 | 双开关语义 | 任一开解锁 — 保守面 (默认双关) ✅ | 通过 |
| V3 | 池隔离 | Async 与 retry 不同池 — 状态互斥无竞态 ✅ | 通过 |
| V4 | 重启续跑 | 存储驱动 — 崩溃恢复面 ✅ | 通过 |
| V5 | 终态不循环 | 终态不在筛选组 — 无死循环 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **双解锁开关 OR 语义** (T2): ROLLBACK_RETRY_TIMEOUT_UNLOCK_ENABLE \|\| ROLLBACK_FAILED_UNLOCK_ENABLE (DefaultCoordinator:463-465) — 任一开启即解锁 (默认双关保守) | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 状态族 (三系 14 态/迁移方向) — 可写 ✅
- §2 重试链 (入队/消费/终态/Committed 特判) — 可写 ✅
- §3 死阈值 (70s/10s/-1/动态延迟) — 可写 ✅
- §4 终态解锁 (三态/延迟/DELAY_HANDLE/双开关) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (双解锁开关 OR)。核心认知: **重试 = 状态驱动闭环** (重启可续跑) + **双阈值** (MAX=-1 永不 + dead 70s/10s) + **默认人工介入** (不解锁 + "handled manually")。harness 16/16 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 基准/校验/Metrics 穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (isRetryTimeout 基准时间/校验器完整族/endRollbacked 特判/Metrics 计时键/配置键)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | isRetryTimeout 基准? | **基准 = getBeginTime()** (L462,498,572-574): 重试窗口**从事务 begin 起算** (非失败时刻) — MAX 配置为正时语义关键 (窗口含正常执行期) | 发现 12 (语义标注) |
| T2 | 校验器完整族? | SessionStatusValidator 4 方法: **isTimeoutGlobalStatus 4 态** / **isTimeoutRollbacking 2 态** / **isRollbackGlobalStatus 5 态 (含终态族)** / **isEndGlobalStatus 4 态** (与 S-3 endStatuses 同) | 发现 13 (补锚) |
| T3 | endRollbacked 特判? | **TimeoutRollbacking → TimeoutRollbacked 专用埋点先发** (L218-221); retryBranch = TimeoutRollbackRetrying\|\|RollbackRetrying (L223-224) — 超时回滚成功有专属 Metrics | 发现 14 (补锚) |
| T4 | Metrics 计时键? | postSessionDoneEvent 重载: **STATUS_VALUE_AFTER_COMMITTED/ROLLBACKED_KEY + beginTime + retryBranch** (MetricsPublisher:55-57) — 提交/回滚耗时指标面 | 通过 (验证) |
| T5 | 配置键? | **RETRY_DEAD_THRESHOLD = SERVER_PREFIX + "retryDeadThreshold"** (ConfigurationKeys:603-605) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | begin 基准语义 | 窗口含正常执行期 — 长事务重试窗口被压缩 (语义精确) ✅ | 通过 |
| V2 | 校验器覆盖 | 4 方法覆盖终态/超时/回滚族 — 无遗漏 ✅ | 通过 |
| V3 | 超时埋点 | TimeoutRollbacked 专属 — 监控区分 ✅ | 通过 |
| V4 | 耗时指标 | beginTime 基准 — 端到端耗时 ✅ | 通过 |
| V5 | 配置可调 | retryDeadThreshold 键可配 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **语义标注** | **isRetryTimeout 基准 = beginTime** (T1): 重试窗口从 begin 起算 — MAX 配置为正时, 长执行期压缩重试窗口 | 大纲 §3 注 |
| 13 | **补充锚点** | **SessionStatusValidator 4 方法族** (T2): isTimeoutGlobalStatus 4 态 / isRollbackGlobalStatus 5 态 (含终态) / isEndGlobalStatus 4 态 | 大纲 §1 注 |
| 14 | **补充锚点** | **endRollbacked Timeout 特判 + retryBranch** (T3): TimeoutRollbacking → TimeoutRollbacked 专用埋点; retryBranch 区分重试 (L218-224) | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 状态族 (三系 14 态/校验器族) — 可写 ✅
- §2 重试链 (入队/消费/终态/特判) — 可写 ✅
- §3 死阈值 (70s/10s/-1/动态延迟/begin 基准) — 可写 ✅
- §4 终态解锁 (三态/延迟/双开关/Timeout 特判) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (begin 基准/校验器 4 方法族/Timeout 特判埋点)。核心认知: **重试窗口从 begin 起算** (非失败时刻, MAX 配置语义) + **校验器 4 方法族覆盖终态** + **Timeout 回滚成功有专属埋点**。大纲经修复后反写测试全过。
