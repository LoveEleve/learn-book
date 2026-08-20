# 闭环笔记 q1: 复制双水位 — 主写/主推/从确认

## 假设
组提交等的是从库确认水位, 不是主库推完。

## 验证过程
- **三条水位**:
  1. masterPutWhere: 主库 CommitLog 最大写入 (调用方传入)
  2. **push2SlaveMaxOffset** (DefaultHAService:56 AtomicLong): 主库已推送上限 — notifyTransferSome CAS 推进 (L107-117)
  3. **slaveAckOffset** (DefaultHAConnection:58 volatile): 从库确认 (已落盘) 点 — ReadSocketService 收报告更新 (L230)
- **isSlaveOK** (L98-105): connectionCount>0 && masterPutWhere - push2SlaveMaxOffset < **haMaxGapNotInSync=256MB** — 推送落后即不可用
- **inSyncReplicasNums** (L184-192): masterPutWhere - slaveAckOffset < 256MB → in sync; +1 (自身) → **needAckNums** (RM-2: GroupTransferService L951-965 已证)
- **通知链**: slaveAckOffset 更新 → notifyTransferSome → CAS push2SlaveMaxOffset → groupTransferService.notifyTransferSome → 唤醒组提交双链表 (RM-2 已详)

## 代码类型
Implementation (水位推进 + CAS)

## 跨域关联
- RM-2 (存储): GroupTransferService 双链表/组提交/needAckNums=inSyncReplicas (已闭环, 本域引用)
- RM-10 (事务): SLAVE_NOT_AVAILABLE 判定同源

## 结论
复制双水位 = 推水位 (push2SlaveMaxOffset) 与确认水位 (slaveAckOffset); 组提交等**确认水位**, in sync 判定 256MB 落后阈值。
源码位置: DefaultHAService.java:56,98-117,184-200; DefaultHAConnection.java:58,230-236
