# S-1 AT 两阶段提交 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "5种GlobalStatus状态组" — 实证 **GlobalStatus 21 态** (code 0-20, 含 Timeout 四态/AsyncCommitting/Deleting/Stop* 族) — "5 组" 疑为阶段分组, 本域全量固化 | 大纲 §4 全表 |
| 2 | **补充锚点** | **BranchType 5 值含 SAGA_ANNOTATION** (BranchType:29-48) — 执行计划未提; getCore 未注册 → NotSupportYetException (DefaultCore:86-92) | 大纲 §4 |
| 3 | **表述精确化** | "Phase1(begin+undo)" 需精确: TransactionalTemplate 只负责 **begin/commit/rollback 编排**; undo 由 RM 侧 ExecuteTemplate 在业务执行中完成 (S-4/S-10 交叉) — 本域不含 undo 细节 | 大纲 §1 注 |
| 4 | **语义标注** | **非回滚异常提交**: completeTransactionAfterThrowing rollbackOn(ex) ? rollback : **commit** (L195-200) — 与直觉相反 (以为异常必回滚) | 大纲 §1 |
| 5 | **补充锚点** | **钩子异常不中断**: 7 个 trigger 全 catch(Exception) → LOGGER.error 继续 (L322-328) — 钩子失败不破坏主流程 | 大纲 §1 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (TransactionalTemplate 53-407 / DefaultGlobalTransaction 41-159 / DefaultCore 222-549 / GlobalStatus 29-245 / BranchType 29-48 / GlobalSession 237-245 / XID 55-62) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 传播决策矩阵 (harness A)
- 分支遍历方向 (harness B)
- 状态组判定 (harness C)
- 超时数学 (harness D)

### 维度2 性能
- 异步提交 (canBeCommittedAsync/AsyncCommitting)
- PARALLEL_HANDLE_BRANCH 并行 (默认关)
- MDC.put (日志上下文)

### 维度3 内存
- CORE_MAP (BranchType→AbstractCore 单例)
- SuspendedResourcesHolder (传播挂起栈)

### 维度4 一致性
- lockAndExecute 原子状态迁移
- session.close 先于状态迁移 (防新分支竞态)
- 双时钟超时

### 维度5 负面空间 (已写入大纲 6 条)
- 不服务端业务判定/不全局事务日志/不 XA 标准/不同步强一致/不无锁回滚/不全局时钟

## 结论
S-1 锚点 ~30 处验证, 8 闭环完成, harness 23/23 (A-D 4 面), **数字穷举 1 + 表述精确化 1 + 语义标注 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 引出 S-2/S-3/S-4/S-5/S-6 ✅; 对照 Spring/RocketMQ ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (传播/角色/超时/状态机) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §2 未提 **commit 的 suspend(true) 解绑**: DefaultGlobalTransaction.commit finally 中 xid 匹配才 unbind (L154-156) — 事务结束即解绑线程上下文 (嵌套场景关键) | 大纲 §2 补注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (编排/角色/重试/状态映射/遍历/异步/超时/doBranchDelete) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (suspend 解绑 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 角色分离闭环 | Participant 不 begin/commit → 嵌套调用不破坏发起者 ✅ | 通过 |
| V2 | 传播挂起恢复 | suspend 记录 holder → 外层 finally resume → 上下文还原 ✅ | 通过 |
| V3 | 关会话防竞态 | close 先于状态迁移 → 新分支注册必拒 → 提交集冻结 ✅ | 通过 |
| V4 | 失败转重试 | 非重试失败 → queueToRetry → 重试面 (S-7) 接管 ✅ | 通过 |
| V5 | 反向回滚正确 | 后注册先回滚 → 依赖顺序安全 (先建先删依赖) ✅ | 通过 |
| V6 | 双时钟超时一致 | 客户端 commit 前 + TC commit 前 — 任一方超时 → Timeout 族 ✅ | 通过 |
| V7 | 启发式判定 | Finished → CommitHeuristic — 事务不存在两种可能 (已提交/已回滚) ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **doBranchDelete 反直觉语义**: AT branchDelete 返回 PhaseTwo_Committed → 日志 "Delete AT branch failed" 但 **return true** (L164-170) — 语义 = 分支已提交无需删除 (清理视为成功); TCC/XA 走 rollback 语义 — 分支类型决定删除路径 | 大纲 §3 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (doBranchDelete 反直觉 #10), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 竞态穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (commit 双路径/超时竞态/全局锁配置/并行分支/MDC)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | commit 双路径? | commit (L249-280): canBeCommittedAsync → **asyncCommit + 返回 Committed** (L270-272); doGlobalCommit 后 **success && hasBranch && canBeCommittedAsync → asyncCommit** (L270-272) — 异步提交两处触发 | 通过 (验证) |
| T2 | 超时竞态? | 客户端 commit 前检查 (TransactionalTemplate:211) 与 TC commit 前检查 (DefaultCore:243) — 窗口内 TC 先超时 → TimeoutRollbacking 返回 → 客户端映射 Code.Rollbacking (L227-231) — 竞态有明确结果面 | 通过 (验证) |
| T3 | 全局锁配置? | replaceGlobalLockConfig (TransactionalTemplate:175-181): txInfo 的 lockRetryInterval/Times/**lockStrategyMode** 注入 GlobalLockConfigHolder — S-12 交叉面 | 通过 (验证) |
| T4 | 并行分支条件? | doGlobalCommit 并行: **PARALLEL_HANDLE_BRANCH && branchSessions.size() >= 2** (L378) — 默认关, 配置键 ENABLE_PARALLEL_HANDLE_BRANCH_KEY (L44) | 发现 11 (补锚) |
| T5 | MDC 清理? | begin 时 MDC.put(xid) (L226) — 提交/回滚后是否 remove? commit/rollback 路径未见 MDC.remove — **MDC 残留面** (日志上下文泄漏) | 发现 12 (补锚) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 异步提交不阻塞 | 无同步分支 → AsyncCommitting → 后台线程提交 ✅ | 通过 |
| V2 | 超时竞态有界 | 双检查窗口 → 至多 TimeoutRollbacking 返回 ✅ | 通过 |
| V3 | 锁配置隔离 | Holder 替换 + finally 恢复 → 事务级锁配置 ✅ | 通过 |
| V4 | 并行语义安全 | 分支独立提交 + 状态移除 — 无共享可变状态 ✅ | 通过 |
| V5 | 挂起嵌套 | suspend/resume 栈式还原 — 多级嵌套正确 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **并行分支触发条件**: PARALLEL_HANDLE_BRANCH (默认 false) && branches ≥ 2 (DefaultCore:378, L61-62) | 大纲 §3 注 |
| 12 | **语义标注** | **MDC xid 残留面**: begin 时 MDC.put (L226) — commit/rollback 路径未见 remove — 同线程后续日志污染面 | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 编排 (传播决策/角色分离/异常映射/钩子) — 可写 ✅
- §2 生命周期 (begin/commit/rollback/TM 重试/XID/MDC) — 可写 ✅
- §3 TC Phase2 (遍历方向/异步提交/并行/doBranchDelete) — 可写 ✅
- §4 状态面 (21 态/判定组/超时) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复** (并行条件/MDC 残留)。核心认知: **异步提交双触发点** + **超时竞态有界** + **doBranchDelete 分支类型决定删除路径**。harness 23/23 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 服务端面穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查七个存疑面 (isEndStatus 语义/RETRY_DEAD 阈值/clean 语义/并行分组/状态即指令/BranchStatus 枚举/TM→TC 链)。

## 追查过程 (七个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | isEndStatus 语义? | GlobalSession:270 `isEndStatus() { return active; }` — **注释 "status is in end status" 与实现不符: 返回 active 标志非状态枚举**; active=false 由 AbstractSessionManager:155 (会话移除) + RAFT GlobalReleaseLockExecute:38 设置; active=true 由 SessionHolder:261,264 (db 模式重载) | 发现 13 (补锚) |
| T2 | RETRY_DEAD 阈值? | **DEFAULT_RETRY_DEAD_THRESHOLD = 70s / DEFAULT_END_STATE_RETRY_DEAD_THRESHOLD = 10s** (DefaultValues:385-390) — S-7 核心数字提前实证; timeToDeadSession: end 态用 10s 否则 70s (L222-228) | 发现 14 (补锚) |
| T3 | asyncCommit/queueToRetry? | **状态即指令**: asyncCommit → changeGlobalStatus(AsyncCommitting) (L871-873); queueToRetryCommit → CommitRetrying (**StopCommitOrCommitRetry 时 return** L875-880); queueToRetryRollback → TimeoutRollbacking 时 TimeoutRollbackRetrying 否则 RollbackRetrying (Stop 时 return L882-893) | 发现 15 (补锚) |
| T4 | clean() 语义? | **clean() = 全局锁释放**: releaseGlobalSessionLock, 失败抛 TransactionException (L301-309) — 非"清数据"; closeAndClean **仅 AT 分支才 clean** (hasATBranch, L312-318) | 发现 16 (语义标注) |
| T5 | BranchStatus 枚举? | **15 值 code 2-14** (PhaseOne_Done(2)~STOP_RETRY(14); 0/1 空缺) — PhaseTwo_CommitFailed_Unretryable(7)/RollbackFailed_Unretryable(10)/XAER_NOTA(11,12)/PhaseOne_RDONLY(13) | 通过 (验证) |
| T6 | TM→TC 链? | DefaultTransactionManager → Netty → **ServerOnRequestProcessor.onRequest (L216) → transactionMessageHandler.onRequest → DefaultCoordinator.onRequest (L797) → core.begin (L323)/core.commit (L343)/core.rollback (L351)** — 全链实证 | 通过 (验证) |
| T7 | 并行分组语义? | SessionHelper.forEach parallel (L353-385): **按 resourceId 分组 (Map)** → 每组 CompletableFuture.supplyAsync → 逐 future get 首个非 null 短路 — **同资源串行 (防同库锁竞争), 跨资源并行** | 发现 17 (补锚) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | active 驱动阈值 | 会话移除 → active=false → END 阈值 10s → 残留清理快 ✅ | 通过 |
| V2 | 状态即指令闭环 | 非重试失败 → CommitRetrying → S-7 定时器消费 → 重试 → 终态 ✅ | 通过 |
| V3 | 同资源串行正确 | 同库分支并行提交会锁冲突 — 分组后串行 ✅ | 通过 |
| V4 | clean 语义链 | commit 后 clean → 释放全局锁 → 他人可获取 ✅ | 通过 |
| V5 | 短路语义 | 任一分支失败 → 后续 future 结果忽略 (已排队) ✅ | 通过 |

## 新发现问题 (5 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **语义标注** | **isEndStatus 反直觉** (T1): 注释 "status in end status" vs 实现返回 active — timeToDeadSession 阈值分支由 active 驱动 | 大纲 §3 注 |
| 14 | **补充锚点** | **RETRY_DEAD_THRESHOLD 70s/10s** (T2, DefaultValues:385-390) — S-7 数字提前实证 | 大纲 §3 注 |
| 15 | **补充锚点** | **状态即指令** (T3): asyncCommit/queueToRetry* 全部是 changeGlobalStatus — Stop 态守卫 + Timeout 派生 (S-7 交叉) | 大纲 §3 注 |
| 16 | **语义标注** | **clean() = 全局锁释放** (T4): 非清数据; closeAndClean 仅 AT 分支 (S-12 交叉) | 大纲 §3 注 |
| 17 | **补充锚点** | **并行分组语义** (T7): 按 resourceId 分组 — 同资源串行跨资源并行 (SessionHelper:353-385) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 编排 (传播决策/角色分离/异常映射/钩子) — 可写 ✅
- §2 生命周期 (begin/commit/rollback/TM 重试/XID/MDC) — 可写 ✅
- §3 TC Phase2 (遍历方向/异步提交/并行分组/clean 锁释放/状态即指令) — 可写 ✅
- §4 状态面 (21 态/判定组/超时) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

七存疑面全实证 (T1-T7); 推理验证 5 项全过 (V1-V5); **新发现 5 处全部修复** (isEndStatus 反直觉/70s 阈值/状态即指令/clean 锁释放/并行分组)。核心认知: **clean() 实为全局锁释放** (S-12 交叉) + **并行按资源分组** (同资源串行) + **状态即指令** (queueToRetry 全是状态迁移)。大纲经修复后反写测试全过。
