# Z-1 Leader 选举 — 时空溯源 (git 实证)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.6 (2014) | Notification CURRENTVERSION=0x2 (L117 版本锚); 双队列 Messenger 骨架 |
| 3.4.10 (2016) | **weight 加权投票** (totalOrderPredicate weight==0 排除 — 注释 L736-742) — 异构权重 |
| ZK-107 (3.5.x) | **40B 协议** (peerEpoch + version; 28B 兼容注释 L251-257) — peer epoch 进入通知 |
| ZOOKEEPER-3922 (3.6+) | **FOLLOWING/LEADING 行为分离 + 2 节点 QuorumOracleMaj** (L1073-1097 长注释) |
| 3.9.x | 动态 reconfig 双 QuorumVerifier (getVoteTracker L761-767); 指数退避可配 (min/maxNotificationInterval 系统属性 L81-91) |

## 痕迹证据

- FastLeaderElection.java:117: CURRENTVERSION=0x2 (3.4.6 锚)
- FastLeaderElection.java:245-257: 28B/40B 兼容注释 (ZK-107 锚)
- FastLeaderElection.java:732-734: weight 排除 (3.4.10 锚)
- FastLeaderElection.java:1073-1097: ZOOKEEPER-3922 注释 (3.6 锚)
- FastLeaderElection.java:81-91: 系统属性 (3.9 可配面)

## git 实证

```
git log --oneline -3 -- zookeeper-server/src/main/java/org/apache/zookeeper/server/quorum/FastLeaderElection.java
```

> 待执行 (Pass 3 时空溯源阶段): fetch --unshallow 后 git log 验证版本锚。

## 推断标注

- "3.4.6/3.4.10" — 注释与代码内锚推断 (标注); CURRENTVERSION/weight 注释为强证据
- "3.5.x/3.6+" — ZK-107/3922 编号与版本对应关系推断 (标注)
- 未做 git 考古前版本线为代码结构推断
