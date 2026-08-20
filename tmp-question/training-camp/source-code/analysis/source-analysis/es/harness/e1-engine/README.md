# E-1 Index Engine — harness 验证记录 (MiniEngine 15/15)

> 跑法: `javac MiniEngine.java MiniEngineTest.java && java MiniEngineTest` (JDK 21)
> 结果: **15/15 PASS** (首跑全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A2 | 计划-执行 | 新文档 UPDATE 计划 → 执行 version=1 | InternalEngine.java:1305-1383 (planIndexingAsPrimary) | PASS |
| A3-A4 | 版本冲突 | 冲突在计划阶段拒绝, 不落盘 | InternalEngine.java:1352-1373 (skipDueToVersionConflict) | PASS |
| A5 | 版本递增 | 匹配更新 version+1 | InternalEngine.java:1375 (updateVersion) | PASS |
| B1-B2 | append-only | 新文档 APPEND + 水位更新 | InternalEngine.java:1059-1090 (canOptimizeAddDocument) | PASS |
| B3 | 重试幂等 | 重试降级 UPDATE 防重复 | InternalEngine.java:1143-1173 (时间戳 happens-before) | PASS |
| B4-B5 | 水位判定 | 旧时间戳降级 / 新时间戳 APPEND | maxUnsafeAutoIdTimestamp 语义 | PASS |
| C1 | realtime get | 写入后立即可见 (版本表) | InternalEngine.java:865-924 (realtimeGetUnderLock) | PASS |
| C2-C3 | NRT | 未 refresh 不可见 → refresh 后可见 | InternalEngine.java:2022 (refresh) | PASS |
| C4 | 删除标记 | tombstone → realtime 返回 null | LiveVersionMap.java:210 (tombstones) | PASS |
| C5 | 并发 | 同文档 8 线程串行化最终 version=8 | InternalEngine.java:1140 (versionMap.acquireLock) | PASS |

## 验证意义

- 计划-执行两阶段 / append-only 幂等水位 / NRT 三级可见性 / 并发 uid 锁 — 4 大机制全部可复现
- **未验证面** (harness 边界): 真实 Lucene IndexWriter 行为、LiveVersionMap 双 map 切换 (refresh listener)、tragic 失败分级 (需异常注入)、soft deletes 持久化
- 结论: "写入决策 → 落盘 → 可见性" 的 Engine 生命周期理解验证到位
