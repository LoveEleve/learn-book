# 闭环笔记 q2: 两阶段提案 — propose → ACK → tryToCommit

## 假设
提案全序广播; ACK 收集; 顺序 + quorum 双守卫提交。

## 验证过程
- **propose** (Leader.java:1288-1340): 请求 → `lastProposed++` (zxid 分配) → `outstandingProposals.put(lastProposed, p)` → broadcast (PROPOSAL); `pendingSyncs.computeIfAbsent` (sync 等待请求)
- **processAck** (L1047-1115):
  - `allowedToCommit` 守卫 (L1048-1050: leader 变更后停止提交)
  - 低 32 位 == 0 → 忽略 (NEWLEADER/UPTODATE ack 走别路, L1061-1068)
  - `lastCommitted >= zxid` → 幂等忽略 (L1074-1081)
  - `outstandingProposals.get(zxid) == null` → 未来提案警告 (L1083-1086)
  - `p.addAck(sid)` → tryToCommit
  - **reconfig 链式提交** (L1105-1114): reconfig 提交后尝试后续 (单 outstanding reconfig 注释)
- **tryToCommit** (L963-1027):
  1. **顺序守卫**: `outstandingProposals.containsKey(zxid - 1)` → false (前序未提交不提交, L971-973)
  2. **quorum 守卫**: `!p.hasAllQuorums()` → false (双 verifier, L978-980)
  3. outstandingProposals.remove + toBeApplied.add (L991-995)
  4. reconfig: designatedLeader + processReconfig + **allowedToCommit=false** + commitAndActivate (L999-1022)
  5. 普通: **commit(zxid)** (COMMIT 广播) + **inform(p)** (observer 路径, L1023-1027)

## 代码类型
Implementation (两阶段 + 顺序提交)

## 跨域关联
- Z-4: toBeApplied → ToBeAppliedRequestProcessor → FinalRequestProcessor (应用)
- Z-1: allowedToCommit 与 leader 变更联动

## 结论
提案 = zxid 递增全序广播; 提交 = 顺序守卫 (zxid-1) + 双 verifier 多数 + 幂等忽略; reconfig 有 designatedLeader 特例。
源码位置: Leader.java:963-1027,1047-1115,1288-1340
