# S-12 全局锁体系 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "6种Locker实现" — 实证 **7 个实现** (行锁 4: DataBase/File/Redis/RedisLuaLocker + 分布式 3: DataBase/Redis/RaftDistributedLocker) — 表述精确化 | 大纲 §2 |
| 2 | **语义标注** | **lockQuery 只查不锁**: isLockable — SELECT 检查不写入 (harness C3 实证) — 与 acquireLock 检查-插入对比 | 大纲 §3 |
| 3 | **补充锚点** | **lockKey 格式**: "table:pk1,pk2;t2:pk3" 分号/冒号/逗号三层解析 → RowLock (L122-165) | 大纲 §1 |
| 4 | **补充锚点** | **检查-插入两段**: rowKey 去重 → autoCommit false → checkLock (IN) → dbXID != currentXID 冲突 → insert (LockStoreDataBaseDAO:109-160+) | 大纲 §3 |
| 5 | **补充锚点** | **LockStatus 2 值**: Locked(0)/Rollbacking(1) — S-8 状态联动 | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (AbstractLockManager 38-194 / LockerManagerFactory 32-52 / LockStoreDataBaseDAO 109-160+ / LockStatus 25-30 / 4 模式 lock 目录) | 记录 |

## 07 五维度

### 维度1 功能正确性
- lockKey 解析 (harness A)
- 检查-插入 (harness B)
- 只查不锁 (harness C)
- 状态/释放 (harness D)

### 维度2 性能
- IN 批量检查
- 只查不锁 (零写入)
- 模式 SPI (无反射热路径)

### 维度3 内存
- rowKey 去重
- LockDO 列表

### 维度4 一致性
- 检查-插入防覆盖
- 冲突 failFast
- 默认不解锁

### 维度5 负面空间 (已写入大纲 6 条)
- 不 RW 锁/不锁粒度协商/不锁超时自动释放/不公平队列/不锁监控/不跨表原子锁

## 结论
S-12 锚点 ~30 处验证, 8 闭环完成, harness 12/12 (A-D 4 面), **数字穷举 1 + 语义标注 1 + 补充锚点 3**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-2/S-4/S-8 ✅; 对照 ZK WriteLock ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (管理器/Locker/存储/状态) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §3 未提 **failFast 面**: 冲突时 LockKeyConflictFailFast 码 (S-4 FailFast 降级面) — 检查即失败 vs 重试面 | 大纲 §3 补注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (管理器/Locker/存储/状态) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (failFast #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 检查防覆盖 | 先查后写 — 异 xid 不覆盖 ✅ | 通过 |
| V2 | 只查不锁 | lockQuery 无写入 — 查询语义 ✅ | 通过 |
| V3 | 幂等重获 | 同 xid 去重 — 重试安全 ✅ | 通过 |
| V4 | 状态联动 | Rollbacking 标记 — 并发读感知 ✅ | 通过 |
| V5 | 释放两级 | 分支/全局 — 粒度完整 ✅ | 通过 |
| V6 | 模式可插 | SPI 4 模式 — 存储解耦 ✅ | 通过 |
| V7 | 默认人工 | 不解锁 — 防误 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **skipCheckLock 语义**: acquireLock(…, skipCheckLock) — 跳过检查直接插入 (S-7 重试路径用?) — 检查-插入的旁路面 | 大纲 §3 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (skipCheckLock), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (分布式锁 SQL/锁表列/updateLockStatus SQL/方言工厂/释放幂等)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 分布式锁 SQL? | DataBaseDistributedLocker → lock 表 (DistributedLockDO: lockKey/owner/expire) — 与行锁表分离 | 通过 (验证) |
| T2 | 锁表列? | LockDO: rowKey/xid/transactionId/branchId/status (core/store) — 5 列 | 通过 (验证) |
| T3 | updateLockStatus SQL? | LockStoreDataBaseDAO.updateLockStatus — xid 级更新 (S-8 联动面) | 通过 (验证) |
| T4 | 方言工厂? | LockStoreSqlFactory (SPI) — 13 方言 | 通过 (验证) |
| T5 | 释放幂等? | releaseLock 重复调用 — NoNode 语义? 删除不存在行无害 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 双表分离 | 行锁/分布式锁独立存储 ✅ | 通过 |
| V2 | 锁行结构 | 5 列齐备 ✅ | 通过 |
| V3 | 状态可更新 | xid 级 — 粒度够 ✅ | 通过 |
| V4 | 方言扩展 | 工厂 SPI ✅ | 通过 |
| V5 | 释放无害 | 删不存在行 — 幂等 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **LockDO 5 列结构**: rowKey/xid/transactionId/branchId/status (core/store/LockDO) — lock_table 建表依据 | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 锁管理器 (统一入口/lockKey 解析/模式 SPI) — 可写 ✅
- §2 Locker 族 (行锁 4 + 分布式 3) — 可写 ✅
- §3 LockStore (检查-插入/只查不锁/5 列/方言) — 可写 ✅
- §4 状态释放 (LockStatus/两级释放/默认人工) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (LockDO 5 列)。核心认知: **Locker 7 实现 (4 行锁 + 3 分布式)** (执行计划 "6种" 修正) + **检查-插入两段 + 只查不锁** + **LockStatus 2 值联动**。harness 12/12 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 分支注册锁链穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (分支注册锁链/applicationData 控制/AbstractCore 空实现/AbstractLocker 基类/StoreException 包装)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 分支注册锁链? | **branchRegister (AbstractCore:87-120) → lockAndExecute → newBranchByGlobal → branchSessionLock → branchSession.lock → LockManager.acquireLock** → 冲突抛 **LockKeyConflict** ("Global lock acquire failed xid = ...", ATCore:57-95) — S-1 分支注册面闭环 | 发现 12 (补锚) |
| T2 | applicationData 控制? | **客户端 AUTO_COMMIT + SKIP_CHECK_LOCK 经 applicationData 传递** (ATCore:60-75) — 客户端控制锁检查行为 (skipCheckLock 的客户端源) | 发现 13 (补锚) |
| T3 | AbstractCore 空实现? | **branchSessionLock/Unlock 空** (L151-154) — 非 AT 模式无行锁 (TCC/XA/SAGA) | 发现 14 (补锚) |
| T4 | AbstractLocker 基类? | **core/lock/AbstractLocker** (L31): getLockStore 抽象 + cleanAllLocks 空 — 行锁公共面 | 通过 (验证) |
| T5 | StoreException 包装? | ATCore:85-94: cause 是 BranchTransactionException → **重包 LockKeyConflict 码** | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 锁获取时机 | 分支注册即锁 — Phase1 完成前锁齐 ✅ | 通过 |
| V2 | 客户端可控 | applicationData 传递 — 灵活面 ✅ | 通过 |
| V3 | 模式无锁 | 非 AT 空实现 — 无多余开销 ✅ | 通过 |
| V4 | 异常码稳定 | StoreException 重包 — 客户端可识别 ✅ | 通过 |
| V5 | 冲突即失败 | LockKeyConflict → RM 侧重试 (S-4) ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **分支注册锁链** (T1): branchRegister → ATCore.branchSessionLock → branchSession.lock → acquireLock → LockKeyConflict | 大纲 §1 注 |
| 13 | **补充锚点** | **applicationData 客户端控制** (T2): AUTO_COMMIT + SKIP_CHECK_LOCK 跨服务传递 — skipCheckLock 客户端源 | 大纲 §3 注 |
| 14 | **补充锚点** | **AbstractCore 空实现** (T3): 非 AT 模式无行锁 (TCC/XA/SAGA) | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 锁管理器 (统一入口/lockKey 解析/分支注册锁链) — 可写 ✅
- §2 Locker 族 (行锁 4 + 分布式 3/非 AT 无锁) — 可写 ✅
- §3 LockStore (检查-插入/客户端控制/只查不锁) — 可写 ✅
- §4 状态释放 (LockStatus/两级释放/默认人工) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (分支注册锁链/applicationData 控制/非 AT 空实现)。核心认知: **锁获取发生在分支注册** (Phase1 完成前锁齐) + **客户端经 applicationData 控制锁行为** (AUTO_COMMIT/SKIP_CHECK_LOCK) + **非 AT 模式无行锁**。大纲经修复后反写测试全过。
