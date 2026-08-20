# 闭环笔记 q3: Sync 刷盘 — 批量 + snapCount 快照

## 假设
写批量落盘; 读旁路; snapCount 随机化防风暴。

## 验证过程
- **SyncRequestProcessor** (281): queuedRequests → **toFlush (ArrayDeque maxBatchSize, L85-92)** → flush() (L227+: 写 txnlog) → nextProcessor
- **批处理** (L160-215): poll 超时 (maxWriteQueuePollTime) → 无数据 flush; 满批 → flush (**BATCH_SIZE 指标 L232**)
- **snapCount 快照判定** (L144-151): `logCount > snapCount/2 + randRoll` — **randRoll = nextInt(snapCount/2) 随机抖动** (L151) 防同步快照风暴; 触发 → **zks.takeSnapshot()** (L193)
- **DEFAULT_SNAP_COUNT=100000** (ZooKeeperServer:224) + snapCount ≥2 校验 (L1290-1294)
- **读旁路** (L203-211): toFlush 空 → 直接 nextProcessor (读不落盘)
- **Flushable 协议**: nextProcessor 实现 Flushable → 显式 flush (L207-209) — CommitProcessor flush 语义

## 代码类型
Implementation (批量刷盘)

## 跨域关联
- Z-9: flush → FileTxnLog.append (事务日志)
- Z-3: takeSnapshot → 树序列化

## 结论
Sync = 批量 toFlush (maxBatchSize + 超时双触发) + snapCount/2+randRoll 随机快照 + 读旁路; 写必落盘。
源码位置: SyncRequestProcessor.java:85-92,144-151,160-232; ZooKeeperServer.java:224,1290-1294
