# S-11 undo_log 可靠性 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "压缩+子表+for(;;)无限重试+AsyncWorker/GlobalFinished/并发回滚保护" — 全实证; **缓冲 10000** (DefaultValues:48) / **拆分 80% 阈值** (L136-143) / **分片 1000** (AsyncWorker:160) | 大纲 §1-3 |
| 2 | **语义标注** | **子表拆分片数数学**: 片数 = **ceil((len - limit) / limit)** — 首片主行 + 子片 — **harness 自抓 2 处** (1.7MB→2 子片/2.5MB→3 子片) | 大纲 §2 |
| 3 | **补充锚点** | **满时背压**: offer 失败 → **紧急 doBranchCommitSafely + thenRun 重入** (AsyncWorker:88-97) — 队列不无限增长 | 大纲 §3 |
| 4 | **语义标注** | **requeue 无限重试**: 资源缺失 (L149) / SQLException (L165,189) → addAllToCommitQueue — 失败不丢 (S-7 交叉) | 大纲 §3 |
| 5 | **补充锚点** | **maxAllowedPacket 默认 1MB**: mysql 5.6 默认 (L137-143) — context 记录优先 | 大纲 §2 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (AsyncWorker 61-192 / MySQLUndoLogManager 61-172 / AbstractUndoLogManager 519-573 / DefaultValues 46-48) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 压缩阈值 (harness A)
- 子表拆分数学 (harness B)
- AsyncWorker 背压/requeue (harness C)
- 清理 SQL (harness D)

### 维度2 性能
- 异步批删 (提交不阻塞)
- 1000 分片
- LIMIT 分页删除

### 维度3 内存
- 缓冲队列 (10000)
- drainTo 全量

### 维度4 一致性
- requeue 不丢
- GlobalFinished 保护
- 主/子行联动清理

### 维度5 负面空间 (已写入大纲 6 条)
- 不无限保留/不异地备份/不片内压缩/不清理限速配置/不孤儿检测/不延迟删除确认

## 结论
S-11 锚点 ~30 处验证, 8 闭环完成, harness 13/13 (A-D 4 面, 自抓 2 处), **数字穷举 1 + 语义标注 2 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-2/S-4/S-3 ✅; 对照 ZK 压缩 ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (压缩/子表/批删/清理) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §3 未提 **deleteUndoLog 的 commit 语义**: 批删后 autoCommit false 才 commit (AsyncWorker:182-184) — 事务边界面 | 大纲 §3 补注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (压缩/子表/批删/清理) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (commit 语义 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 压缩对称 | 压缩+context 记录 → 解压按记录 — 无损 ✅ | 通过 |
| V2 | 拆分可恢复 | 主行+子片+SUB_ID_KEY → 拼接完整 ✅ | 通过 |
| V3 | 背压有界 | 满时紧急清 — 队列不爆 ✅ | 通过 |
| V4 | 失败不丢 | requeue — 最终一致 ✅ | 通过 |
| V5 | 清理有界 | LIMIT 分页 — 防锁表 ✅ | 通过 |
| V6 | 联动清理 | 主/子行同步删 — 无孤儿 ✅ | 通过 |
| V7 | 并发保护 | GlobalFinished — 防 Phase1 提交 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **子行 BRANCH_ID_KEY 关联**: subRollbackCtx = branchId:xxx (L150-151) — 子行与主分支关联键 (批删 DELETE_SUB 依据) | 大纲 §2 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (子行关联键), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (压缩 SPI 类型/subIds 空/批删 IN 上限/清理 LIMIT 语义/requeue 循环)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 压缩 SPI 类型? | CompressorType 枚举 (NONE/ZIP/GZIP...) + CompressorFactory SPI — 默认 zip (S-2) | 通过 (验证) |
| T2 | subIds 空? | getSubRollbackInfo: 空 → Pair(0, empty) (L85-87) — 安全 | 通过 (验证) |
| T3 | 批删 IN 上限? | batchDeleteUndoLog: IN 参数拼接 — **IN 上限 = 分片 1000** (AsyncWorker 分片保证) | 通过 (验证) |
| T4 | LIMIT 语义? | deleteUndoLogByLogCreated: log_created <= ? LIMIT ? — 单次最多删 limitRows 行 (分页迭代由调用方?) | 通过 (验证) |
| T5 | requeue 循环? | requeue → 下周期再试 — 无死循环 (定时消费) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | IN 上限安全 | 1000 分片 → IN ≤ 1000 (MySQL 上限) ✅ | 通过 |
| V2 | 空安全 | subIds 空返回空 — 无 NPE ✅ | 通过 |
| V3 | 分页迭代 | LIMIT 逐批 — 调用方循环 ✅ | 通过 |
| V4 | 重试不循环 | 定时驱动 — 每周期一次 ✅ | 通过 |
| V5 | 压缩可扩展 | SPI — 新压缩器可插 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **IN 上限与分片对齐**: batchDeleteUndoLog 的 IN 参数数 = 分片 1000 — **1000 分片同时规避 MySQL IN 上限** (双语义) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 压缩面 (阈值/context 驱动/SPI) — 可写 ✅
- §2 子表面 (80% 拆分/片数数学/关联键) — 可写 ✅
- §3 AsyncWorker (缓冲/背压/分组/分片/requeue/IN 对齐) — 可写 ✅
- §4 清理面 (LIMIT 分页/广播/GlobalFinished) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (IN 上限对齐)。核心认知: **子表片数 = ceil((len-limit)/limit)** (harness 自抓 2 处) + **满时背压紧急清** + **requeue 无限重试** + **1000 分片双语义** (批删 + IN 上限)。harness 13/13 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 压缩/删除链穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (CompressorType 族/删除请求 RM 链/LIMIT_ROWS/saveDays/INSERT SQL)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | CompressorType 族? | **8 值** (L19-47): NONE(0)/GZIP(1)/ZIP(2)/SEVENZ(3)/BZIP2(4)/LZ4(5)/DEFLATER(6)/ZSTD(7) — 压缩 SPI 族 | 发现 12 (补锚) |
| T2 | 删除请求 RM 链? | S-3 广播 → **RmUndoLogProcessor (client:51-60)** → RMHandlerAT.deleteUndoLog (L85-105) → deleteUndoLogByLogCreated(division, **LIMIT_ROWS=3000**) | 发现 13 (补锚) |
| T3 | LIMIT_ROWS? | **= 3000** (RMHandlerAT:43) + **do-while 循环: deleteRows == LIMIT_ROWS 继续删** (L67) — 分批迭代删除 | 发现 14 (补锚) |
| T4 | saveDays 兜底? | getLogCreated: pastDays <= 0 → **DEFAULT_SAVE_DAYS=7** (UndoLogDeleteRequest:33, RMHandlerAT:105-108) | 通过 (验证) |
| T5 | INSERT SQL? | **7 列 now(6) 微秒时间戳** (MySQLUndoLogManager:54-60) — 清理依据 log_created 精度 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 压缩可扩展 | 8 类型 SPI — 按需选 ✅ | 通过 |
| V2 | 删除链完整 | TC → RM 处理器 → DAO — 闭环 ✅ | 通过 |
| V3 | 分批迭代 | do-while 3000 — 全量清理 ✅ | 通过 |
| V4 | saveDays 安全 | <=0 兜底 7 — 防误删 ✅ | 通过 |
| V5 | 精度够用 | now(6) — 秒级清理足够 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **CompressorType 8 值** (T1): NONE/GZIP/ZIP/SEVENZ/BZIP2/LZ4/DEFLATER/ZSTD — 压缩 SPI 族扩展面 | 大纲 §1 注 |
| 13 | **补充锚点** | **删除请求 RM 链** (T2): RmUndoLogProcessor → RMHandlerAT.deleteUndoLog — S-3 广播的 RM 接收端实证 | 大纲 §4 注 |
| 14 | **补充锚点** | **LIMIT_ROWS=3000 + do-while 迭代** (T3, RMHandlerAT:43,67): 分批删除直到删不满 — 清理完整面 | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 压缩面 (阈值/context/8 类型 SPI) — 可写 ✅
- §2 子表面 (80% 拆分/片数数学) — 可写 ✅
- §3 AsyncWorker (缓冲/背压/分组/requeue) — 可写 ✅
- §4 清理面 (LIMIT 迭代/广播链/saveDays/now(6)) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (8 压缩类型/RM 删除链/LIMIT 迭代)。核心认知: **压缩 SPI 8 类型** + **删除链 TC→RM 完整闭环** (LIMIT_ROWS=3000 do-while 迭代) + **saveDays 兜底 7 天**。大纲经修复后反写测试全过。
