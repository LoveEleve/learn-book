# 闭环笔记 q1: lead 启动 — ZabState 三阶段

## 假设
lead = 发现 → 同步 → 广播三阶段; 新 epoch 提议 + 等多数同步。

## 验证过程
- **ZabState 三阶段** (Leader.java:644,713,765): DISCOVERY → SYNCHRONIZATION → BROADCAST
- **DISCOVERY** (L644-654): `zk.loadData()` + leaderStateSummary (当前 epoch + lastProcessedZxid) + `cnxAcceptor = new LearnerCnxAcceptor()` 启动
- **epoch 提议** (L655-663): `getEpochToPropose(self.getMyId(), self.getAcceptedEpoch())` + **`zk.setZxid(ZxidUtils.makeZxid(epoch, 0))`** — 新 epoch 低 32 位归零; newLeaderProposal.packet = NEWLEADER 包
- **reconfig 版本** (L669-704): curQV version==0 && lastSeenQV 同 → **lastSeenQV.setVersion(zk.getZxid())** (ZOOKEEPER-1783 注释: 初始 config version=0 需协商; NEWLEADER 携带 lastSeenQV)
- **同步等待** (L710-716): waitForEpochAck → setCurrentEpoch → SYNCHRONIZATION → **waitForNewLeaderAck (等多数 NEWLEADER ack)**; 失败 → shutdown + initTicks 提示 (L717-738)
- **BROADCAST 主循环** (L774-816): **每 tickTime/2 迭代**: SyncedLearnerTracker (双 verifier) + 自身 ack + synced learners — quorum 失守 → shutdown

## 代码类型
Implementation (状态机 + 等待)

## 跨域关联
- Z-1: FastLeaderElection → QuorumPeer 切 LEADING → lead()
- Z-9: loadData (快照+日志恢复)
- Z-4: startZkServer → 处理器链启动

## 结论
lead = 三阶段 (发现/同步/广播) + 新 epoch zxid 提议 + NEWLEADER 多数等待 + 每半 tick quorum 自检。
源码位置: Leader.java:632-772,774-816
