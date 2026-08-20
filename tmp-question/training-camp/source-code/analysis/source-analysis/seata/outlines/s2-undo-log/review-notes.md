# S-2 undo_log 机制 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | 执行计划 "beforeImage(SELECT FOR UPDATE)+afterImage+反向SQL" 正确但需补: **undo_log 表有 log_status 双态 (Normal/GlobalFinished)** — 回滚时先查状态, GlobalFinished 忽略 (重复回滚防护, L343-352) | 大纲 §1 |
| 2 | **语义标注** | **!exists → 插入 GlobalFinished 而非报错**: undo 时 undo_log 不存在 = Phase1 可能未提交 (业务超时场景) → **插 GlobalFinished 防 Phase1 后提交** (L390-416, issue #489) — 反直觉语义 | 大纲 §1 |
| 3 | **表述精确化** | "无限重试" 需精确: **for(;;) 只对 SQLIntegrityConstraintViolationException 重试** (L419-423); 其他异常 → Unretriable/Retriable 抛给服务端 (S-7 重试面) — 非真无限 | 大纲 §1 |
| 4 | **补充锚点** | **逆序只在多条时**: sqlUndoLogs.size() > 1 才 reverse (L368-370) — 单条无意义不反转 | 大纲 §1 |
| 5 | **补充锚点** | **压缩默认面**: enable=true / type=zip / threshold=64k (DefaultValues:368-380); needCompress **严格大于** 64k (L571-573) | 大纲 §1 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (AbstractUndoLogManager 62-589 / AbstractUndoExecutor 70-400 / MySQL 三 executor / BaseTransactionalExecutor 403-530 / SelectForUpdateExecutor 85-152 / DefaultValues 368-380) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 反向 SQL 三模板 (harness A)
- 校验三步决策 (harness B)
- 逆序+状态机 (harness C)
- 压缩+重试分类 (harness D)

### 维度2 性能
- 压缩 (>64k → zip)
- 批量删除 (IN 拼接)
- PK 分批查询 (IN 上限面)

### 维度3 内存
- BranchUndoLog 序列化字节
- SERIALIZER_LOCAL ThreadLocal

### 维度4 一致性
- FOR UPDATE 行锁
- 三步校验 (dirty 检测)
- GlobalFinished 防竞态

### 维度5 负面空间 (已写入大纲 6 条)
- 不物理回滚/不数据备份/不级联处理/不并发写保护/不无限保留/不跨方言

## 结论
S-2 锚点 ~30 处验证, 8 闭环完成, harness 19/19 (A-D 4 面), **认知修正 1 + 语义标注 1 + 表述精确化 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1 ✅; 引出 S-11/S-12 ✅; 对照 binlog/AOF/ZK ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (生命周期/校验/方言) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §4 未提 **before==current 幂等分支的深层语义**: 重复 rollback (服务端重试/多进程重放) 时当前数据=before → 跳过 → **幂等安全面** (L259-266) | 大纲 §4 注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (生命周期/反向 SQL/镜像/校验) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (幂等分支 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 行锁防并发 | SELECT FOR UPDATE — undo 与 Phase1 互斥 → 无并发改 ✅ | 通过 |
| V2 | 逆序补偿 | 后执行先补偿 — FK 依赖顺序安全 ✅ | 通过 |
| V3 | 幂等重放 | before==current → 跳过 — 重复 rollback 无害 ✅ | 通过 |
| V4 | GlobalFinished 窗口 | Phase1 未提交 + rollback → 标记 → Phase1 提交时被防 (#489) ✅ | 通过 |
| V5 | 脏数据有界 | dirty → Unretriable — 不无限重试脏数据 ✅ | 通过 |
| V6 | 主键守卫 | UPDATE 行数不等 → 显式异常 — 镜像定位失败即停 ✅ | 通过 |
| V7 | 压缩解压对称 | 压缩记录 compressorType 于 context — undo 时按类型解压 ✅ | 通过 |

## 新发现问题 (2 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **表述与实现不符** | **CHECK_SQL_TEMPLATE 注释过时**: "TODO support multiple primary key" (AbstractUndoExecutor:70) — 但 queryCurrentRecords 已支持多 PK (buildWhereConditionListByPKs + getOrderedPkList, L292-366) — TODO 未移除 | 大纲 §4 注 |
| 11 | **补充锚点** | **SERIALIZER_LOCAL ThreadLocal**: undo 期间 setCurrentSerializer/removeCurrentSerializer (L109-121,364-387) — 方言/格式切换的线程绑定面 | 大纲 §1 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **2 处** (TODO 过时/ThreadLocal), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (多 PK 支持实证/子表拼接/批删 SQL 形状/max_allowed_packet/hasUndoLogTable)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 多 PK 实证? | queryCurrentRecords: pkNameList (L297) + buildWhereConditionListByPKs(pkNameList, pkRowSize, dbType) (L307-308) + 参数双循环 r×c (L318-326) — **多列 PK 完整支持, TODO 注释确认过时** (#10 强化) | 通过 (验证) |
| T2 | 子表拼接? | getRollbackInfo: SUB_ID_KEY → getSubRollbackInfo (默认 UnsupportedOperationException L560-563, 子类实现) → 主+子字节拼接 → 解压 (L519-544) — S-11 机制面 | 通过 (验证) |
| T3 | 批删 SQL? | toBatchDeleteUndoLogSql: DELETE ... WHERE branch_id IN (...) AND xid IN (...) (L196-233) — branchId 与 xid 双 IN 交叉 | 通过 (验证) |
| T4 | max_allowed_packet? | flush: getMaxAllowedPacket (默认空 L508-510, MySQL 子类查 SHOW VARIABLES) → context 记录 (L293-301) — 大包预防面 | 通过 (验证) |
| T5 | hasUndoLogTable? | CHECK_UNDO_LOG_TABLE_EXIST_SQL "SELECT 1 FROM undo_log LIMIT 1" (L87) — 启动建表检查 (S-4 交叉) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 多 PK 定位正确 | PK 顺序按 tableMeta — 多列 WHERE 稳定 ✅ | 通过 |
| V2 | 子表可扩展 | 默认 UnsupportedOperation → 方言按需实现 (MySQL 面) ✅ | 通过 |
| V3 | 双 IN 精确 | branch_id+xid 双条件 — 无错删 ✅ | 通过 |
| V4 | 大包预防 | max_allowed_packet 记录 → 提交前检查 ✅ | 通过 |
| V5 | 校验关闭面 | IS_UNDO_DATA_VALIDATION_ENABLE=false → 跳过校验直接 undo (风险面) ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **max_allowed_packet 面**: flush 时记录数据库 max_allowed_packet 进 context (L293-301, MySQL 子类 SHOW VARIABLES) — 大 rollback_info 预防机制 | 大纲 §1 注 |

## 反写测试 (只读大纲能否写文章)

- §1 生命周期 (flush/undo/重试/GlobalFinished/子表) — 可写 ✅
- §2 反向 SQL (三模板/方言/PK 约定) — 可写 ✅
- §3 镜像采集 (行锁/主键守卫/lockKey) — 可写 ✅
- §4 校验 (三步决策/幂等/TODO 过时) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (max_allowed_packet)。核心认知: **GlobalFinished 是防 Phase1 提交窗口的标记** (反直觉) + **多 PK 已支持但 TODO 注释过时** + **无限重试只针对约束冲突**。harness 19/19 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 序列化/比对/参数约定穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查六个存疑面 (BranchUndoLog 结构/默认序列化器/isRecordsEquals 语义/InsertExecutor 参数约定/锁重试/max_allowed_packet 实现)。

## 追查过程 (六个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | BranchUndoLog 结构? | xid + branchId + sqlUndoLogs 三字段 DTO (BranchUndoLog:27-88) — 序列化面简单 | 通过 (验证) |
| T2 | 默认序列化器? | **DEFAULT_TRANSACTION_UNDO_LOG_SERIALIZATION = "jackson"** (DefaultValues:245) — 6 parser SPI (fastjson2/fastjson/jackson/protostuff/fury/kryo, @LoadLevel); context 记录 serializer → undo 按记录选 parser (L358-361) — **跨版本解码兼容面** | 发现 13 (补锚) |
| T3 | InsertExecutor 参数约定? | **MySQLUndoInsertExecutor 覆盖 undoPrepare — 只绑 PK** (L57-63): INSERT 的 undo=DELETE WHERE pk 无需值参数 — **参数约定因操作类型而异** (基类非 PK 值+PK 最后 vs 此覆盖) | 发现 14 (补锚) |
| T4 | isRecordsEquals 语义? | tableName **ignoreCase** + 行数相等 + compareRows 字段级 (L57-71); **convertType** 处理 DATE/TIME/TIMESTAMP string↔object (L102-120); **fastjson 特例分支** (L73-82: 老 serializer 数值类型比较差异) | 发现 15 (补锚) |
| T5 | 锁重试? | SelectForUpdateExecutor: **LockRetryController** (L83,115) — Phase1 全局锁冲突 sleep 重试 (S-12 交叉) | 发现 16 (补锚) |
| T6 | max_allowed_packet 实现? | MySQLUndoLogManager **getVariableValue("max_allowed_packet")** (L129) + INSERT 7 列 **now(6) 微秒时间戳** (L54-59) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 跨版本解码 | context 记录 serializer → 老日志新代码可读 (升级安全) ✅ | 通过 |
| V2 | 参数约定自洽 | 每 executor 自定 undoPrepare — 与 SQL 模板配套 ✅ | 通过 |
| V3 | 比对严格性 | 表名+行数+字段级 — dirty 检测无漏 ✅ | 通过 |
| V4 | 锁重试有界 | LockRetryController 按配置重试 — 不无限阻塞 ✅ | 通过 |
| V5 | 时间戳精度 | now(6) — 清理/监控精度面 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **补充锚点** | **默认序列化器 jackson** (T2, DefaultValues:245) + 6 parser SPI + context 记录 serializer 跨版本解码 | 大纲 §1 注 |
| 14 | **表述精确化** | **InsertExecutor 只绑 PK** (T3, L57-63): undoPrepare 覆盖 — 参数约定因操作类型而异 | 大纲 §2 注 |
| 15 | **补充锚点** | **fastjson 数值比较特例** (T4, DataCompareUtils:73-82): 老 serializer 数值类型差异 → 专用比较分支 | 大纲 §4 注 |
| 16 | **补充锚点** | **Phase1 锁重试**: SelectForUpdateExecutor LockRetryController (L83,115) — S-12 交叉面 | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 生命周期 (flush/undo/重试/GlobalFinished/序列化器 SPI) — 可写 ✅
- §2 反向 SQL (三模板/方言/PK 约定/参数约定差异) — 可写 ✅
- §3 镜像采集 (行锁/主键守卫/lockKey/锁重试) — 可写 ✅
- §4 校验 (三步决策/幂等/比对语义/fastjson 特例) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

六存疑面全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复** (jackson 默认/InsertExecutor 只绑 PK/fastjson 比较特例/锁重试)。核心认知: **默认序列化器是 jackson 非 fastjson** (context 驱动跨版本解码) + **反向 SQL 参数约定因操作类型而异** (覆盖 undoPrepare)。大纲经修复后反写测试全过。
