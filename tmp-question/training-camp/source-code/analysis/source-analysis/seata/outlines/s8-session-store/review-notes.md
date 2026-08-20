# S-8 Session 存储 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "SessionMode(DB/FILE/REDIS/RAFT)" — 4 值全实证 (SessionMode:19-35); SessionManager 族 4 实现 | 大纲 §1 |
| 2 | **认知修正** | **onClose → setActive(false)** (AbstractSessionManager:154-156) — **S-1 isEndStatus 反直觉 (返回 active) 的根源实证** — active 标志由 SessionManager 维护 | 大纲 §2 |
| 3 | **补充锚点** | **状态联动锁标记**: Rollbacking/TimeoutRollbacking → 全部分支 LockStatus.Rollbacking (L85-87) — 状态变更写存储时同步锁状态 (S-12 交叉) | 大纲 §2 |
| 4 | **表述精确化** | **lockAndExecute 三模式**: FILE 本地锁 (GlobalSessionLock tryLock 2s → FailedLockGlobalTransaction) / **DB 直接 call** (多节点共享存储本地锁无意义) / RAFT 分布式锁 — 非统一实现 | 大纲 §4 |
| 5 | **补充锚点** | **GlobalSessionLock 数学**: ReentrantLock + **tryLock(2s)** + InterruptedException → FailedLockGlobalTransaction (GlobalSession:831-850) | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~35 锚点 + 跨文件 grep (SessionMode 19-35 / SessionHolder 71-154 / AbstractSessionManager 73-195 / FileSessionManager 184-210 / DataBaseSessionManager 160-163 / GlobalSession 831-850 / GlobalTransactionDO) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 模式路由 (harness A)
- 生命周期映射 (harness B)
- 三模式锁 (harness C)
- tryLock 数学 (harness D)

### 维度2 性能
- DB 直通 (无锁开销)
- Lua 单脚本原子
- File 刷盘模式

### 维度3 内存
- GlobalTransactionDO 11 字段
- restoreSessions 缓冲

### 维度4 一致性
- 状态联动锁标记
- Lua 原子性
- reload 恢复

### 维度5 负面空间 (已写入大纲 6 条)
- 不跨模式迁移/不缓存层/不分库分表/不本地锁集群化/不消息队列/不数据压缩

## 结论
S-8 锚点 ~35 处验证, 8 闭环完成, harness 12/12 (A-D 4 面, 自抓 2 处 ReentrantLock 语义), **数字穷举 1 + 认知修正 1 + 表述精确化 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1/S-3/S-7 ✅; 引出 S-12 ✅; 对照 ZK 会话 ✅; 读者处境场景化 ✅; 锚点 ~35 ✅; 负面空间 6 条 ✅; 横切 (路由/生命周期/存储/锁) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §2 未提 **writeSession 失败异常族**: 6 LogOperation → FailedWriteSession (Global/BranchTransactionException 区分) (L173-195) — 写失败显式失败 | 大纲 §2 补注 |
| 9 | 通过项 | 其余 ~28 句机制描述逐句对源码一致 ✅ (路由/生命周期/存储/锁) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (异常族 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 模式路由闭环 | SPI 加载 + reload — 启动恢复 ✅ | 通过 |
| V2 | 生命周期完整 | 6 操作覆盖全事件 ✅ | 通过 |
| V3 | 锁语义正确 | FILE 单机锁 / DB 存储条件 / RAFT 分布式 ✅ | 通过 |
| V4 | Lua 原子 | 单脚本 — 无并发竞态 ✅ | 通过 |
| V5 | 状态联动 | Rollbacking 锁标记 — 锁面同步 ✅ | 通过 |
| V6 | 失败显式 | FailedWriteSession — 不静默 ✅ | 通过 |
| V7 | 恢复完整 | restoreSessions 未处理分支缓冲 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **GlobalSession 双锁**: globalSessionLock + resourceLock (L117-121) — 会话级 + 资源级锁分离 | 大纲 §4 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (双锁), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (File 双 manager/reload 时序/Redis Lua 锁类型/方言工厂/DB 条件更新)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | File 双 manager? | SessionHolder:142-149 load("file") ×2 (root.data + 另一命名) — **root/data 双会话管理器** | 通过 (验证) |
| T2 | reload 时序? | DB 模式 init 后立即 reload (L108-111) — 先加载后恢复 ✅ | 通过 (验证) |
| T3 | Redis Lua 锁类型? | RedisLuaLocker + RedisLocker (Lua vs Java) — RedisLockManager 选择面 | 通过 (验证) |
| T4 | 方言工厂? | LogStoreSqlsFactory/LockStoreSqlFactory (SPI) — 13 方言加载 | 通过 (验证) |
| T5 | DB 条件更新? | DataBaseSessionManager 直通 + 存储层 SQL — 并发靠数据库原子性 (UPDATE 条件) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 双管理器隔离 | root/vgroup 分离 — 职责清晰 ✅ | 通过 |
| V2 | 恢复先于服务 | reload 在 init — 服务前恢复 ✅ | 通过 |
| V3 | Lua 可切换 | Lua/Java 双实现 — 兼容面 ✅ | 通过 |
| V4 | 方言 SPI | 工厂模式 — 新库扩展 ✅ | 通过 |
| V5 | DB 原子性 | 数据库层保证 — 无本地锁正确 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **RedisLocker 双实现**: RedisLuaLocker (Lua 原子) vs RedisLocker (Java) — 可选面 (Lua 默认) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 模式路由 (4 值/SPI/reload) — 可写 ✅
- §2 生命周期 (6 操作/状态联动/异常族) — 可写 ✅
- §3 存储实现 (DB 13 方言/Redis Lua 双实现/File 刷盘) — 可写 ✅
- §4 锁面 (三模式/双锁/tryLock 2s) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (Redis 双实现)。核心认知: **onClose setActive(false) 是 isEndStatus 根源** (S-1 反直觉闭环) + **lockAndExecute 三模式** (FILE 本地 2s/DB 直通/RAFT 分布式) + **ReentrantLock 可重入/持有者解锁语义** (harness 自抓 2 处)。harness 12/12 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + DAO/Lua/reload 穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (DB 写操作细节/锁 SQL/Redis Lua 脚本/reload 状态机/刷盘模式)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | transactionName 截断? | **columnSize 从表结构运行时读取** (LogStoreDataBaseDAO:490) + **超长静默 substring** (L210-211) — 事务名数据丢失面 (无警告) | 发现 12 (语义标注) |
| T2 | DB 写事务粒度? | insertGlobalTransactionDO: **conn.setAutoCommit(true) 每操作独立事务** (L202) — 无批事务, 单写单提交 | 发现 13 (补锚) |
| T3 | Redis Lua 脚本? | **4 个 Lua 文件**: acquireRedisLock/releaseRedisLock/updateRedisLock/lockable (RedisLuaLocker:52-73) + **pipeline 模式** (L86) | 发现 14 (补锚) |
| T4 | reload 状态机? | **终态收尾 + 错误态清理 + 重试态续跑**: TimeoutRollbacked/Rollbacked → endRollbacked; Committed → endCommitted; Finished/UnKnown/CommitFailed/RollbackFailed/TimeoutRollbackFailed → **removeInErrorState** (SessionHolder:185-215) — S-7 重启续跑实证 | 发现 15 (补锚) |
| T5 | 锁 SQL 面? | LockStoreDataBaseDAO.acquireLock: **rowKey 去重 + 检查-插入** (dbExistedRowKeys) — S-12 交叉 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 截断有界 | columnSize 从表结构 — 不超列宽 ✅ | 通过 |
| V2 | 单写单提交 | 每操作独立事务 — 简单但无原子批 ✅ | 通过 |
| V3 | Lua 完备 | 4 脚本覆盖 acquire/release/update/lockable ✅ | 通过 |
| V4 | reload 恢复完整 | 终态收尾/错误态清/重试续 — 三面闭环 ✅ | 通过 |
| V5 | 锁去重 | rowKey distinct — 防重复锁行 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **语义标注** | **transactionName 静默截断** (T1): columnSize 从表结构读 + 超长 substring (L210-211) — 事务名丢失无警告 | 大纲 §3 注 |
| 13 | **补充锚点** | **每写独立事务** (T2): conn.setAutoCommit(true) (L202) — DB 模式无批事务 | 大纲 §3 注 |
| 14 | **补充锚点** | **Redis 4 Lua 脚本 + pipeline** (T3): acquire/release/update/lockable (L52-73) | 大纲 §3 注 |
| 15 | **补充锚点** | **reload 状态机** (T4): 终态收尾/错误态 removeInErrorState/重试续跑 (L185-215) — S-7 重启续跑实证 | 大纲 §1 注 |

## 反写测试 (只读大纲能否写文章)

- §1 模式路由 (4 值/SPI/reload 状态机) — 可写 ✅
- §2 生命周期 (6 操作/状态联动/异常族) — 可写 ✅
- §3 存储实现 (DB 13 方言/截断面/独立事务/Redis 4 脚本) — 可写 ✅
- §4 锁面 (三模式/双锁/检查-插入) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复** (transactionName 截断/独立事务/Redis 4 脚本/reload 状态机)。核心认知: **reload 状态机三面闭环** (终态收尾/错误态清/重试续) + **transactionName 静默截断** (columnSize 从表结构) + **Redis 4 Lua 脚本 pipeline**。大纲经修复后反写测试全过。
