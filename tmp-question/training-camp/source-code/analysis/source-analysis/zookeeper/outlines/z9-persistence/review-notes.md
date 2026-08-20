# Z-9 持久化 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | 执行计划 "checksum+length+bytes 记录" 正确但需精确: **CRC 只覆盖 payload, 不覆盖 len 与 0x42** — Javadoc L77-78 "calculated across payload -- Txnlen, TxnHeader, Record and 0x42" **与实现不符** (append L316-319: crc.update(buf) 先于 writeLong 与 0x42) — harness A2 字节布局 + B1 实证 | 大纲 §1 ⚠ |
| 2 | **语义标注 (静默截断)** | **len 域无 CRC 保护**: len 损坏 → readBuffer 读巨大长度 → EOFException → **静默 EOF**, 该文件后续事务全部丢弃 (只 LOG.debug) — harness B3 实证 (0 条返回, 非致命) | 大纲 §1 注 |
| 3 | **表述精确化** | "尾部残缺容忍" 需三分: **缺 0x42 / len 截断 / 空 buffer** 三种都走 EOF 静默; 只有 **payload 内容损坏走 CRC 致命** — 崩溃安全设计 (append-only: 尾部=崩溃现场) — harness B1/B2/B3 实证 | 大纲 §1 |
| 4 | **补充锚点** | **快照 dbid=-1 常量** (FileSnap:51) vs **txnlog dbid 动态** (append L297) — 不对称 (dbid 语义仅日志侧有意义) | 大纲 §2 注 |
| 5 | **补充锚点** | **空快照文件删除** (FileTxnSnapLog:485-495): save 失败且文件空 → delete — full disk 循环防护 (注释 L487-490) | 大纲 §2 |
| 6 | 行号验证 | 全函数 ~35 锚点 + 跨文件 grep (FileTxnLog 60-501/624-858 / Util 84-252 / FileSnap 50-288 / SnapStream 64-327 / FileTxnSnapLog 112-535 / PurgeTxnLog 39-166 / ZKDatabase 365-376 / SyncRequestProcessor 146-151) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 格式闭环 (harness A)
- CRC 损坏检测 (harness B)
- seal 校验 (harness C)
- restore 边界 (harness D)

### 维度2 性能
- 64MB 预分配 (FilePadding:30)
- commit 批量 force (streamsToFlush 关旧留新)
- getLastLoggedZxid 全文件扫描 (L369-388)

### 维度3 内存
- streamsToFlush 队列 (保留 1 个, 其余关闭)
- committedLog 窗口 (ZKDatabase:98)

### 维度4 一致性
- 快照+WAL 双路径恢复
- fuzzy 快照容错 (NONODE/NODEEXISTS 忽略)
- TRUNC exclusive 边界

### 维度5 负面空间 (已写入大纲 6 条)
- 不单文件库/不内联落盘/不组提交/不压缩加密/不中部自愈/不无限保留

## 结论
Z-9 锚点 ~35 处验证, 8 闭环完成, harness 11/11 (A-D 4 面), **认知修正 1 + 语义标注 1 + 表述精确化 1 + 补充锚点 2**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 Z-3/Z-4/Z-5/Z-2 ✅; 对照 ES Translog/Redis AOF ✅; 读者处境场景化 ✅; 锚点 ~35 ✅; 负面空间 6 条 ✅; 横切 (格式/恢复/清理/压缩) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §1 未提 **fsync 观测指标面**: syncElapsedMS > 1000ms 阈值告警 + serverStats.incrementFsyncThresholdExceedCount + **ServerMetrics FSYNC_TIME** (FileTxnLog:108-137,407-428) — 运维观测 (fsync 延迟是 ZK 写延迟主因) | 大纲 §1 补注 |
| 9 | 通过项 | 其余 ~28 句机制描述逐句对源码一致 ✅ (格式/预分配/commit/seal/回退/边界/清理/压缩) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (digest 覆盖警告 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 崩溃安全 | 任何时刻崩溃 → 尾部残缺 → EOF 静默 → 已 commit 事务完整 ✅ | 通过 |
| V2 | 恢复完整性 | 快照 + zxid+1 重放 → 覆盖到日志末端 → lastProcessedZxid 准确 ✅ | 通过 |
| V3 | 快照回退 | 最多 100 代 — 连续损坏容忍面 ✅ | 通过 |
| V4 | TRUNC 对称 | learner 截到 maxCommittedLog → leader 重发 → 无缺口 ✅ | 通过 |
| V5 | 空库安全 | trustEmptyDB 空快照 → 0 → 投票参与 (initialize 文件) ✅ | 通过 |
| V6 | 清理安全 | getSnapshotLogs 守卫 — 跨界日志不删 → 快照可恢复 ✅ | 通过 |
| V7 | 原子快照 | AtomicFileOutputStream rename — 半写快照不可见 ✅ | 通过 |

## 新发现问题 (2 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **getLastLoggedZxid 全文件扫描** (FileTxnLog:369-388): 每次调用扫描最新日志文件内全部事务 — 启动/校验成本面 (restore 空库分支调用) | 大纲 §3 注 |
| 11 | **语义标注** | **fuzzy 快照容错** (FileTxnSnapLog:445-453): 快照进行中后期事务混入 → 重放 NONODE/NODEEXISTS **安全忽略** (rc.err 仅 debug) — 与 Z-4 "multi 原子性在 Prep" 交叉: 树层重放容错 | 大纲 §2 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **2 处** (全文件扫描/fuzzy 容错), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边界穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个边界面 (0x42 位置/truncate 定位/快照 100 回退/streamsToFlush 关闭时机/检查目录交叉污染)。

## 追查过程 (五个边界面全部实证)

| # | 边界面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 0x42 位置? | append: writeLong(CRC) → writeTxnBytes(buffer+0x42) (FileTxnLog:318-319 + Util:205-208); 读: readLong → readBuffer → readByte=='B' (Util:165-168) — **0x42 在 CRC 之后写, 读时在 buffer 之后** — harness A 字节布局实证 | 通过 (验证) |
| T2 | truncate 定位? | FileTxnIterator(logDir, zxid) fastForward=true → 流位置在首个 ≥zxid 事务之后 → setLength(pos) → **exclusive (移除 ≥zxid)** — LearnerHandler TRUNC zxid=maxCommittedLog (L842-846) 交叉一致 | 通过 (验证) |
| T3 | 快照 100 回退? | findNValidSnapshots(100) (FileSnap:77) — 注释 "run through 100 snapshots" (L74-76); 每候选 deserialize + seal 三重校验失败才换下一个 | 通过 (验证) |
| T4 | streamsToFlush 关闭时机? | commit: flush+force 全部 → **while (size>1) poll().close()** (L430-432) — 只留最新流; 下次 append 旧流已关 | 通过 (验证) |
| T5 | 目录交叉污染? | checkLogDir (log 目录有 snapshot 文件 → 拒绝) + checkSnapDir (L180-204) — **仅 dataDir≠snapDir 时检查** (L164) — 同目录跳过 (历史兼容) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 关闭时机闭环 | 保留最新流 → 后续 append 正确; 旧流已 force → 数据不丢 ✅ | 通过 |
| V2 | 同目录兼容 | 交叉污染检查跳过 — 3.4 单目录布局兼容 ✅ | 通过 |
| V3 | 预分配边界 | position+4096 ≥ fileSize 才 pad — 4KB 余量避免频繁 pad ✅ | 通过 |
| V4 | 快照回退成本 | 每次 deserialize 全量树 — 回退代价高但仅损坏时发生 ✅ | 通过 |
| V5 | 尾部容忍窗口 | 最后一条未 commit 事务可能完整落盘但未 force — 重启后重放 → 幂等 (zxid≤lastZxidSeen warn) ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **交叉污染检查仅异目录时执行** (FileTxnSnapLog:164): dataDir==snapDir 跳过 (3.4 单目录布局兼容面) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 TxnLog (格式/CRC 实证/预分配/commit/容错二分) — 可写 ✅
- §2 快照 (seal 三段/回退 100/原子写/fuzzy 容错) — 可写 ✅
- §3 restore (双路径/边界/逃生舱/truncate/目录校验) — 可写 ✅
- §4 清理 (Purge/尺寸双面/压缩/工具) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五边界面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (同目录跳过交叉污染检查)。核心认知: **CRC 只覆盖 payload (Javadoc 不符)** + **len/0x42 无保护 → 静默截断** + **尾部容忍/中部致命二分** + **zxid+1 与 exclusive truncate 双侧边界对称**。harness 11/11 全过 (含自抓 1 处: C2 首次破坏 len 字节导致 EOF 而非校验失败 — 修正破坏点后通过)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (truncate 无 <zxid 文件/快照重试脏树/原子写实证/学习者同步起点/Toolkit 修复面)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | truncate 无 <zxid 文件? | FileTxnIterator init (L679-696): 全文件 fzxid ≥ zxid 时无 break → storedFiles 全含 → goToNextLog 取最小文件 → next() 读首个 ≥zxid 事务 → position 在其后 → **setLength 保留文件头+首个 ≥zxid 事务 — 不完全截断** (purge 删旧后可达); 靠 fuzzy 容错 (NONODE/NODEEXISTS 忽略) 兜底 | 发现 13 (补锚) |
| T2 | 快照重试脏树? | DataTree.deserialize (L1347): **只 nodes.clear/pTrie.clear/nodeDataSize.set(0)** — **ephemerals/containers/ttls/aclCache 无 clear** (全文件仅 1347 一处 clear) → 100 快照重试循环中**中途失败 (父缺失 IOException) 残留合并进下一快照** — zombie 面 | 发现 14 (补锚) |
| T3 | 原子写实证? | AtomicFileOutputStream.close: flush → **fd.sync()** → super.close → **tmpFile.renameTo(origFile)** (Windows 回退 delete+rename) — 临时文件+fsync+rename 三段确认 (AtomicFileOutputStream.java:close) | 通过 (验证) |
| T4 | 学习者同步起点? | getProposalsFromTxnLog (ZKDatabase:386-420): **readTxnLog(startZxid, false) — fastForward=false** + 守卫 "首事务 zxid > startZxid → EMPTY" (L393-400) + sizeLimit (snapshotSizeFactor) — 精确起点语义 (Z-2 交叉强化) | 发现 15 (补锚) |
| T5 | Toolkit 修复面? | TxnLogToolkit: main (L117-131) — **dump 模式 (交互) / chop 模式 (CRC 修复)** + "-y 非交互" (L401) — 人工修复工具确认 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 重试残留危害域 | 残留 ephemerals → killSession 删不存在节点 → NoNode 忽略; 残留 aclCache 引用 → 引用计数虚高 — 危害有限但存在 ✅ | 通过 |
| V2 | 不完全截断兜底 | 幸存 ≥zxid 事务 → 重启重放 → fuzzy 容错忽略; 不造成状态错误 ✅ | 通过 |
| V3 | 学习者起点正确 | fastForward=false + 首事务守卫 → 要么整文件连续可重放, 要么 EMPTY → leader 转 SNAP ✅ | 通过 |
| V4 | 原子写闭环 | fd.sync 先于 rename → 重命名后文件必完整 (崩溃时临时文件残留无害) ✅ | 通过 |
| V5 | 尾部容忍一致性 | len=0 (pad 区) / EOF / 缺 0x42 三路全走 EOF — harness B 面一致 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **补充锚点** | **truncate 不完全截断边沿** (T1): 无 <zxid 日志文件 (purge 删旧) → 首个 ≥zxid 事务幸存 — 靠 fuzzy 容错兜底 | 大纲 §3 注 |
| 14 | **语义标注** | **快照重试残留面** (T2): DataTree.deserialize 只清 nodes/pTrie, ephemerals/containers/ttls/aclCache 不清 → 100 重试中途失败残留合并 (zombie 面) | 大纲 §2 注 |
| 15 | **补充锚点** | **学习者同步起点守卫** (T4): getProposalsFromTxnLog fastForward=false + 首事务 zxid 守卫 + sizeLimit (ZKDatabase:386-420) — Z-2 交叉面补全 | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 TxnLog (格式/CRC 实证/预分配/commit/容错二分/fsync 指标) — 可写 ✅
- §2 快照 (seal 三段/回退 100/原子写/fuzzy 容错/重试残留) — 可写 ✅
- §3 restore (双路径/边界/逃生舱/truncate 边沿/目录校验) — 可写 ✅
- §4 清理 (Purge/尺寸双面/压缩/工具/学习者守卫) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (truncate 不完全截断/快照重试残留/学习者起点守卫)。核心认知: **重试循环只清树不清分类集合** (zombie 面) + **truncate 依赖 fuzzy 容错兜底** (边沿不完全截断) + **原子写三段实证** (fsync→rename)。大纲经修复后反写测试全过。
