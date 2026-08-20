# 闭环笔记 q2: 刷盘三服务 — 组提交/定时/转储

## 假设
SYNC → GroupCommitService (批量+等待); ASYNC → FlushRealTimeService (定时/页数); 池启用 → CommitRealTimeService (转储)。

## 验证过程
- **选择** (CommitLog.DefaultFlushManager L2118-2127): SYNC_FLUSH → GroupCommitService; ASYNC → FlushRealTimeService; **池启用额外启动 CommitRealTimeService** (L2129-2131)
- **GroupCommitService** (L1614-1660+): 双链表 (requestsWrite/requestsRead **swap 交换** — 无锁读侧); putRequest (PutMessageSpinLock 自旋锁 + wakeup); doCommit: **flushOK = flushedWhere >= 请求 nextOffset** (L1639) → 不满足循环 flush(0) 最多 1000 次 + **sleep(1ms) 等待 commit** (L1647-1653, 池启用时 writeBuffer 未 commit 场景注释) → wakeupCustomer (PUT_OK/FLUSH_DISK_TIMEOUT)
- **FlushRealTimeService** (L1487+): 三条件 — ①flushIntervalCommitLog=500ms ②**待刷页 ≥ flushCommitLogLeastPages=4** ③flushCommitLogThoroughInterval=10s 强刷 (L1575-1590 区域)
- **CommitRealTimeService** (L1432+): 池启用时 writeBuffer→fileChannel 转储 (CommitLog.mappedFileQueue.commit), 同样 4 页/间隔条件
- **同步等待** (handleDiskFlush L2133-2156): SYNC + isWaitStoreMsgOK → GroupCommitRequest + future.get(syncFlushTimeout) → 超时 FLUSH_DISK_TIMEOUT; 非 wait → 仅 wakeup (异步组提交)
- **FlushManager 接口** (FlushManager.java): start/shutdown/wakeUpFlush/wakeUpCommit/handleDiskFlush×2 (5.x 接口化)

## 代码类型
Algorithmic (刷盘调度)

## 跨域关联
- RM-3 (CommitLog): 写入后回调
- RM-5 (Broker): flush 配置面

## 结论
刷盘 = 按 FlushDiskType 选服务: 组提交 (双链表 swap + flushedWhere 判定 + 1000 次重试) / 定时 (500ms/4 页/10s 三条件) / 转储 (池启用); 同步语义 = future 等待 + 超时降级。
源码位置: CommitLog.java:1432-1660,2109-2156; FlushManager.java
