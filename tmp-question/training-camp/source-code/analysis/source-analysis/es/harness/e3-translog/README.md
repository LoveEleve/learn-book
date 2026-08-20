# E-3 Translog — harness 验证记录 (MiniTranslog 21/21)

> 跑法: `javac MiniTranslog.java MiniTranslogTest.java && java MiniTranslogTest` (JDK 21)
> 结果: **21/21 PASS** (首跑 12/19, 修复 3 处自身缺陷后全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A4 | 双缓冲写路径 | add 零字节 → sync 落盘 → syncNeeded 翻转 | TranslogWriter.java:227-267 (buffer), 356-360 (syncNeeded), 508 (force(false)) | PASS |
| A5-A6 | 批量写 | 100 add 零字节, 一次 sync 全落盘 | TranslogWriter.java:463-534 (syncUpTo 批量) | PASS |
| B1-B3 | checkpoint 恢复 | 已 sync 2 条恢复, 未 sync 1 条断电丢失 | Translog.java:229-256 (recoverFromFiles), Checkpoint 水位语义 | PASS |
| B4 | 半写尾部 | 崩溃时写一半的操作被丢弃 | BufferedChecksumStreamInput 逐条校验 | PASS |
| C1-C5 | generation 轮转 | 未超阈不轮转 → 超阈封存+新代 → 旧代保留 | Translog.java:1628-1652 (rollGeneration), 619-625 (shouldRollGeneration) | PASS |
| C6-C7 | 跨代恢复 | 多代合并按物理顺序回放 | Translog readers 列表 + MultiSnapshot | PASS |
| D1-D4 | tragedy | 写失败冻结首因 → 后续 add/sync 全拒绝 | TragicExceptionHolder.java:15-33, Translog.java:869-889 (closeOnTragicEvent) | PASS |

## harness 抓到的自身实现缺陷 (3 处 — 方法论 01 预期)

1. **C2/C3 轮转时机**: 初稿"写入后检查 totalOffset > threshold" — 实际应在**写入前预判** (本次写入将超阈 → 先轮转再写); 真实 ES 由 Engine afterWriteOperation 在写后自查 (IndexShard.java:3763), harness 改为写入前预判更贴近"操作进新代"语义
2. **C7 恢复顺序**: 初稿恢复时用本地 seqNo++ 排序 — 跨代错乱 (每代从 0 数); 真实 seqNo 是**全局递增** (E-6 语义), harness 修正为按代升序物理顺序 (对应真实 readers 列表有序 + MultiSnapshot)
3. **D 通道关闭检测**: 初稿 add 只写 buffer, 通道关闭后仍成功 (缓冲写不碰 channel); 真实 ES 每次操作前 `ensureOpen()` (TranslogWriter.java:625) — harness 补 channel.isOpen() 检查

## 验证意义

- 双缓冲/批量 fsync/断电丢失窗口/半写截断/tragedy 冻结 — 5 个核心机制全部可复现
- **未验证面** (harness 边界): 真实 force(true/false) 元数据语义 (JDK force 无此区分, 仅测 force(false) 路径)、真实 checksum、globalCheckpoint 落盘三条件 (简化只测 offset 条件)、锁层次并发 (单线程简化)
- 结论: "add 不落盘 → sync 才 force → 崩溃丢未 sync" 的 WAL 生命周期理解验证到位
