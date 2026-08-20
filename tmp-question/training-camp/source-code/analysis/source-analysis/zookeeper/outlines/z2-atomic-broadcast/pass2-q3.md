# 闭环笔记 q3: 同步五分支 — syncFollower 裁决

## 假设
follower 同步 = 五分支裁决 (空DIFF/TRUNC/DIFF/txnlog/SNAP)。

## 验证过程
- **LearnerHandler** (每 learner 一线程): FOLLOWERINFO (带 peerLastZxid) → syncFollower (L556)
- **五分支** (L780-879+):
  1. `forceSnapSync` → SNAP (L846-848, 测试属性)
  2. `lastProcessedZxid == peerLastZxid` → **空 DIFF** (已同步, L849-857)
  3. `peerLastZxid > maxCommittedLog && !isPeerNewEpochZxid` → **TRUNC** (follower 领先 = 旧 leader 残留 → 截到 maxCommittedLog; **新 epoch zxid (低 32 位=0) 不 TRUNC** — 无 txnlog 语义, L858-867)
  4. `minCommittedLog <= peerLastZxid <= maxCommittedLog` → **DIFF** (committedLog 增量 queueCommittedProposals, L868-873)
  5. `peerLastZxid < minCommittedLog && txnLogSyncEnabled` → **磁盘 txnlog + committedLog 补** (calculateTxnLogSizeLimit); 失败 → **SNAP** (L874-879+)
- **committedLog 空** (L815-828): min=max=lastProcessedZxid 归一化 (简化分支)
- **syncThrottler 流控** (L566-634): INFLIGHT_SNAP/DIFF_COUNT — SNAP 全量昂贵限并发
- **queueOpPacket**: DIFF/TRUNC 前置包入队 (L855-867)

## 代码类型
Implementation (同步裁决)

## 跨域关联
- Z-9: txnlogSyncEnabled / calculateTxnLogSizeLimit (持久化面)
- Z-4: 同步后 learner 收 PROPOSAL 走正常链

## 结论
同步 = 五分支 (已同步空DIFF / 领先TRUNC / 窗口内DIFF / 窗口外txnlog / 兜底SNAP); TRUNC 与新 epoch zxid 互斥; SNAP 流控。
源码位置: LearnerHandler.java:556,780-879+
