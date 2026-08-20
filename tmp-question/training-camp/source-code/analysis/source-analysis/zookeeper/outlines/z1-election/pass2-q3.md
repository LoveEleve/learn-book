# 闭环笔记 q3: 收票判定 — totalOrderPredicate + 多数 + 稳定窗口

## 假设
三要素全序比较; 多数达成后还需稳定窗口防抖动。

## 验证过程
- **totalOrderPredicate** (L723-749): `weight==0 → false` (L732-734) + 三条件 (L744-748):
  `newEpoch > curEpoch || (newEpoch == curEpoch && (newZxid > curZxid || (newZxid == curZxid && newId > curId)))` — **peerEpoch > zxid > sid 全序**
- **LOOKING 通知三分支** (L966-1038):
  1. n.electionEpoch > logicalclock → **logicalclock.set + recvset.clear + 重算提议** (对方轮次更新 = 自己落后, 换轮!) (L980-993)
  2. n.electionEpoch < logicalclock → break (旧轮票忽略) (L996-999)
  3. 同轮 → totalOrderPredicate 比较 → 更新提议 + sendNotifications (L1001-1035)
- **收票入集** (L1043): `recvset.put(n.sid, Vote(...))` → **getVoteTracker** (L761-767: 当前 QuorumVerifier + lastSeen 更高版本 — reconfig) → **hasAllQuorums()**
- **finalizeWait=200ms 稳定窗口** (L1047-1066): 达成多数 → 继续 poll: 更高票 → `recvqueue.put(n)` 放回重来 (L1051-1054); n==null → **setPeerState(proposedLeader, voteSet) + leaveInstance(endVote) + return** (当选)
- **checkLeader** (FOLLOWING 路径 L1154): 验证多数投同一 leader

## 代码类型
Implementation (全序裁决 + 稳定窗口)

## 跨域关联
- Z-4: setPeerState 触发状态切换 (QuorumPeer)
- Z-2: leaveInstance → Leader.lead

## 结论
收票 = 三要素全序比较 (epoch>zxid>sid, weight 排除) + 换轮清集 + 双视图多数 + 200ms 稳定窗口防抖。
源码位置: FastLeaderElection.java:723-749,761-767,966-1066
