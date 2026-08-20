# S-4 DataSource 代理 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | 执行计划 "提交拦截" 需精确: 拦截点不止 commit — **setAutoCommit false→true 也触发 doCommit** (JDBC 规范, L303-309) — autoCommit 切换是第二拦截点 | 大纲 §2 |
| 2 | **语义标注** | **register 双条件守卫**: !hasUndoLog **||** !hasLockKey → 不注册 (L268) — 有 undo 无 lockKey 也不注册 (镜像但无锁键的异常面) | 大纲 §2 + harness B3 |
| 3 | **补充锚点** | **report 成功默认关**: IS_REPORT_SUCCESS_ENABLE=false (DefaultValues:60) — Phase1 完成状态靠 undo_log 存在性推断 (S-2 undo 时 GlobalFinished 面) | 大纲 §2 |
| 4 | **表述精确化** | "LockRetryPolicy" 双面语义: **true && autoCommitChanged → 直通** (重试在 executeAutoCommitTrue 层) vs else doRetryOnLockConflict (L352-363) — 重试层归属不同 | 大纲 §4 |
| 5 | **补充锚点** | **FailFast 降级**: autoCommitChanged && LockKeyConflictFailFast → 降级 LockKeyConflict (L372-376, "local lock is released" 注释) — 单语句场景可重试化 | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (DataSourceProxy 95-252 / ConnectionProxy 184-392 / ConnectionContext 96-373 / AbstractDMLBaseExecutor 81-158 / DefaultValues 38-60) | 记录 |

## 07 五维度

### 维度1 功能正确性
- doCommit 三分支 (harness A)
- register 守卫 (harness B)
- LockRetryPolicy (harness C)
- 上下文生命周期 (harness D)

### 维度2 性能
- 构造期自检 (fast-fail)
- report 成功默认关 (省 RPC)

### 维度3 内存
- ConnectionContext 每连接一个
- savepoint 列表

### 维度4 一致性
- 提交 = 注册+flush+commit 原子链
- reset 归零
- autoCommit 拦截

### 维度5 负面空间 (已写入大纲 6 条)
- 不连接池/不 SQL 改写/不自动建表/不读写分离/不连接缓存/不嵌套事务

## 结论
S-4 锚点 ~30 处验证, 8 闭环完成, harness 15/15 (A-D 4 面), **认知修正 1 + 语义标注 1 + 表述精确化 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1/S-2 ✅; 引出 S-10/S-12 ✅; 对照 MyBatis/HikariCP ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (代理/拦截/上下文/重试) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §3 未提 **globalLockRequire 标志** (S-10 交叉): @GlobalLock 注解场景 — 无全局事务但要求全局锁 (checkLock 面, ConnectionProxy:94-105,237-245) | 大纲 §3 补注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (代理链/提交/上下文/执行链) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (globalLockRequire #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 提交原子链 | register → flush → commit — 任一失败 report(false) ✅ | 通过 |
| V2 | 只读无分支 | 无写不注册 — TC 负担最小化 ✅ | 通过 |
| V3 | autoCommit 合规 | false→true 先 commit — JDBC 语义正确 ✅ | 通过 |
| V4 | 重试不重提交 | LockRetryPolicy 只重试冲突 — 不重复提交 ✅ | 通过 |
| V5 | reset 防泄漏 | 提交/回滚后归零 — 连接池归还无残留 ✅ | 通过 |
| V6 | 嵌套解包 | SeataDataSourceProxy 递归解 — 多代理安全 ✅ | 通过 |
| V7 | fast-fail 合理 | undo 表缺失启动即败 — 不运行期爆雷 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **commit 失败补 rollback**: commit 异常且非 autoCommitChanged → 显式 rollback (L191-195) — 防半提交 | 大纲 §2 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (失败补 rollback), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (resourceId 7 方言/解包递归/undo 表检查代价/savepoint 内嵌/changeAutoCommit)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | resourceId 7 方言? | initResourceId (L236-252): PG/Oracle/MySQL+polardb-x/SQLServer/DM/Oscar/default — 7 分支实证 | 通过 (验证) |
| T2 | 解包递归? | instanceof 单次解 (L95-100) — 循环解包由构造链保证 (每层构造再解) | 通过 (验证) |
| T3 | undo 表检查代价? | 构造期一次 hasUndoLogTable (L167-183) — 连接借还各一次 (借: init; 还: 无) — 启动一次性 | 通过 (验证) |
| T4 | savepoint 内嵌? | 上下文记录但**无内嵌事务语义** (负面空间已写) — 仅回滚到 savepoint 时上下文同步 (L215-225) | 通过 (验证) |
| T5 | changeAutoCommit 链? | changeAutoCommit → setAutoCommitChanged + setAutoCommit(false) (L297-300) — LockRetryPolicy 依据 (q4) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 方言 URL 唯一 | resourceId 含 URL — 集群内资源唯一 ✅ | 通过 |
| V2 | 检查一次性 | 构造期自检 — 运行期零开销 ✅ | 通过 |
| V3 | savepoint 同步 | rollback(savepoint) → 上下文移除 — 一致 ✅ | 通过 |
| V4 | autoCommit 标记链 | changeAutoCommit → 策略分支 — 闭环 ✅ | 通过 |
| V5 | 报告幂等 | report 有 branchId 守卫 (L312-314) — 未注册不报 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **report 的 branchId 守卫**: branchId==null → 直接 return (L312-314) — 未注册分支不发送 report | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 代理链 (解包/init 自检/方言/注册) — 可写 ✅
- §2 提交拦截 (三分支/register 守卫/report/失败补 rollback) — 可写 ✅
- §3 上下文 (xid/undo/lockKeys/savepoint/globalLockRequire) — 可写 ✅
- §4 执行链 (双路径/LockRetryPolicy/FailFast) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (report branchId 守卫)。核心认知: **提交 = 注册+flush+commit 原子链** (失败补 rollback) + **register 双条件守卫** + **report 成功默认关** + **autoCommit 第二拦截点**。harness 15/15 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 底层面穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (AbstractConnectionProxy/executeAutoCommitTrue 收尾/LockRetryController 数学/Statement 层/AsyncWorker)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | AbstractConnectionProxy? | 375 行基类: createStatement → StatementProxy (L101-105); **prepareStatement 带 PK 数组识别** (L107-129: 表元数据主键 → prepareStatement(sql, pkNameArray)) — 镜像采集底层支持; close 直通 target (L151-152) | 发现 12 (补锚) |
| T2 | executeAutoCommitTrue 收尾? | **单语句 autoCommit = 隐式全局事务** (L146-168): changeAutoCommit → LockRetryPolicy(executeAutoCommitFalse + **commit()**) → 失败: branchRollbackOnConflict=false 才 rollback → finally **context.reset + setAutoCommit(true)** — 完整自包含提交链 | 发现 13 (认知修正) |
| T3 | LockRetryController 数学? | sleep (L62-71): **--lockRetryTimes < 0 → LockWaitTimeoutException** (先减再判, 第 N+1 次抛) / **FailFast → 立即抛** (不等重试); 窗口 = interval×times; **GlobalLockConfig 优先** (L76-109, S-1 GlobalLockConfigHolder 交叉) | 发现 14 (补锚) |
| T4 | AsyncWorker? | S-11 交叉面 (229 行): **2 线程 ScheduledExecutor + 10ms 初始/1s 周期**; branchCommit 异步提交入口 (S-1 canBeCommittedAsync 消费面); **UNDOLOG_DELETE_LIMIT_SIZE=1000 分批** (L59,160-162) | 发现 15 (补锚) |
| T5 | Statement 层? | StatementProxy/PreparedStatementProxy — execute → ExecuteTemplate 委托 (S-10 交叉) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | PK 数组闭环 | prepareStatement(sql, pkNameArray) → 主键可读 → 镜像定位 ✅ | 通过 |
| V2 | 单语句提交链 | changeAutoCommit → 执行+commit → reset — 无残留 ✅ | 通过 |
| V3 | 重试有界 | interval×times — 超时抛 LockWaitTimeout ✅ | 通过 |
| V4 | 配置优先 | GlobalLockConfig → 全局配置 — 事务级覆盖 ✅ | 通过 |
| V5 | 异步提交分片 | 1000 分片批删 — 大事务安全 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **prepareStatement PK 数组识别** (T1, L107-129): 表元数据主键 → 可读主键 statement — 镜像采集底层 | 大纲 §4 注 |
| 13 | **认知修正** | **单语句 autoCommit = 隐式全局事务** (T2, L146-168): executeAutoCommitFalse + commit() + finally reset — 自包含提交链 | 大纲 §4 注 |
| 14 | **补充锚点** | **LockRetryController 数学** (T3): 第 N+1 次抛 LockWaitTimeout; FailFast 立即抛; **GlobalLockConfig 优先** (S-1 交叉) | 大纲 §4 注 |
| 15 | **补充锚点** | **AsyncWorker 面** (T4): 2 线程 1s 周期 + 1000 分片批删 — S-11 交叉 | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 代理链 (解包/init 自检/方言/注册) — 可写 ✅
- §2 提交拦截 (三分支/register/report/AsyncWorker 交叉) — 可写 ✅
- §3 上下文 (xid/undo/lockKeys/savepoint/globalLockRequire) — 可写 ✅
- §4 执行链 (双路径/PK 数组/隐式全局事务/锁重试数学) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复** (PK 数组/单语句隐式全局事务/锁重试数学/AsyncWorker)。核心认知: **autoCommit 单语句 = 完整隐式全局事务** (自包含提交链) + **锁重试第 N+1 次抛超时** (GlobalLockConfig 优先) + **AsyncWorker 2 线程 1000 分片**。大纲经修复后反写测试全过。
