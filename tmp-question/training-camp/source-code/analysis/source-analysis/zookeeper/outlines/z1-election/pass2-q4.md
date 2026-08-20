# 闭环笔记 q4: 双集合语义 — recvset vs outofelection

## 假设
当前轮裁决与历史学习分离; 迟到者快速跟随。

## 验证过程
- **recvset** (L849-855 注释): 当前轮投票 (electionEpoch == logicalclock) — **多数判定依据**
- **outofelection** (L855-862 注释): 历史轮 + FOLLOWING/LEADING 通知 — **迟到者 (更高 logicalclock) 学习已存在 leader**
- **receivedFollowingNotification** (L1146-1164):
  - 同 epoch → recvset.put + hasAllQuorums + **checkLeader** → setPeerState 当选 (L1151-1159)
  - 异 epoch → **outofelection 验证多数跟随同一 leader** (L1163-1164)
- **FOLLOWING/LEADING 分离** (L1073-1118, ZOOKEEPER-3922): LEADING 先走 following 逻辑, null 再问 Oracle — **2 节点场景**: majority=2 恢复节点永远达不成 → **Oracle 拒绝 = 该 leader 有效** (L1080-1097 长注释)
- **OBSERVING 忽略** (L1069-1071)
- **QuorumOracleMaj** (L980-994): 2 实例配置特殊路径 — revalidateVoteset + setPeerState 直接当选

## 代码类型
Implementation (双集合 + 例外路径)

## 跨域关联
- Z-4: LeaderAndIsr 类似状态切换 (对照 RM-12)
- Curator (4.5): 客户端侧 leader 选举对照

## 结论
双集合 = "当前轮多数裁决" (recvset) 与 "历史 leader 学习" (outofelection) 语义隔离; 2 节点 QuorumOracleMaj 例外 (majority=2 无法达成的 Oracle 裁决)。
源码位置: FastLeaderElection.java:849-862,1069-1118,1146-1164
