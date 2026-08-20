# S-11 undo_log 可靠性 — 压缩/子表/批删/清理

> 前置: [[S-2-undo_log]] (机制面) + [[S-4-DataSource代理]] (AsyncWorker) + [[S-3-TC-Server]] (清理广播) | 对照: ZK 快照压缩 (阶段4.3)
> 🔴 A | 8 KP | [模式: 大包拆分 + 异步批删 + 分页清理]
> Pass 2 闭环: q1(压缩面) q2(子表面) q3(AsyncWorker) q4(清理面)

**读者处境**: 大 undo_log 怎么存? 提交后谁清理? 这篇拆压缩链 + 子表拆分 + AsyncWorker 批删 + 定时清理。

### 1. 压缩面 — 阈值 + 链式解压

场景: 大日志怎么压缩?
源码路径:
- **压缩触发** (AbstractUndoLogManager:571-573): **enable (默认 true) && length > 64k 严格大于** → 默认 zip
- **context 记录** (L287-302): COMPRESSOR_TYPE_KEY — 解压按记录恢复
- **解压链** (L519-544): compressorType from context (默认 NONE) → decompress — 无压缩跳过
- **SPI**: CompressorFactory — **CompressorType 8 值** (NONE/GZIP/ZIP/SEVENZ/BZIP2/LZ4/DEFLATER/ZSTD, L19-47); 对照 ZK 快照压缩
关键设计 (q1): **>64k 触发 + context 驱动解压**。[模式: 压缩链]

### 2. 子表面 — 80% 阈值拆分 + 主/子行

场景: 超包大小怎么拆?
源码路径:
- **拆分阈值** (MySQLUndoLogManager:136-143): **limit = maxAllowedPacket × 0.8** — context 记录 (默认 **1MB** mysql 5.6)
- **拆分写入** (L150-167): 超限 → **首片主行 (branchId)** + UUID 子片子行 (SUB_SPLIT_KEY ",") → context **SUB_ID_KEY**; ⚠ **片数 = ceil((len-limit)/limit)** (harness 自抓 2 处); **子行 BRANCH_ID_KEY 关联键** (L150-151)
- **读取拼接** (L83-120): SELECT branch_id IN (...) AND xid → **主+子字节拼接** → 解压
- **同步清理**: DELETE_SUB_UNDO_LOG_SQL (S-2) — 主/子行联动
关键设计 (q2): **80% 阈值拆分 + 首片主行 + UUID 子片**。[模式: 子表拆分]

### 3. AsyncWorker — 缓冲/分组/分片/requeue

场景: 提交后怎么批删?
源码路径:
- **缓冲** (AsyncWorker:61-79): **LinkedBlockingQueue(10000)** + 2 线程 1s 周期 (10ms 初始)
- **branchCommit** (L81-85): 入队 → **立即返回 Committed** — S-1 canBeCommittedAsync 消费
- **满时背压** (L88-97): offer 失败 → **紧急清 + 重入** (注释 L88-90, 队列不无限增长)
- **消费流** (L113-139): drainTo → **按 resourceId 分组** → **Lists.partition(1000)** → batchDeleteUndoLog → commit (autoCommit false 才 commit, L182-184); ⚠ **1000 分片双语义** (批删 + MySQL IN 上限规避)
- **requeue 无限重试** (L149,165,189): 资源缺失/SQLException → addAllToCommitQueue — 失败不丢
关键设计 (q3): **10000 缓冲 + 满时紧急 + 1000 分片 + requeue**。[模式: 异步批删]

### 4. 清理面 — 定时删除 + GlobalFinished

场景: 孤儿日志怎么清?
源码路径:
- **定时清理** (MySQLUndoLogManager:61-66): **DELETE WHERE log_created <= ? LIMIT ?** — 分页防锁表; ⚠ **RM 接收链**: RmUndoLogProcessor → RMHandlerAT.deleteUndoLog (**LIMIT_ROWS=3000 + do-while 迭代** 直到删不满, L43,67)
- **触发链** (S-3:548-570): TC undoLogDelete 广播 → RM 执行 (saveDays 保留天数)
- **GlobalFinished 防护** (#489, S-2): 无 undo_log → 标记防 Phase1 提交 — 并发回滚保护
- **方言实现**: deleteUndoLogByLogCreated 基类默认 0 (AbstractUndoLogManager:546-549) — MySQL 已实现
关键设计 (q4): **分页删除 + 广播触发 + GlobalFinished 并发保护**。[模式: 清理治理]

## 代码类型
Architecture (可靠性面)

## 负面空间 — undo_log 可靠性刻意不做的事

- **不做无限保留**: log_created 定时清理 (saveDays) — 保留窗口
- **不做异地备份**: 无 undo_log 副本 (对照 binlog 同步)
- **不做片内压缩**: 子表拆分 + 压缩独立 (拆分后每片不再压)
- **不做清理限速配置**: LIMIT 固定 (batchDelete 1000) — 无动态节流
- **不做孤儿检测**: 无主行孤儿子行扫描 (删主行时子行联动, 但崩溃残留无扫描)
- **不做延迟删除确认**: requeue 重试无限但无告警阈值

→ 引出: 全局锁体系 → [[S-12-全局锁体系]]
