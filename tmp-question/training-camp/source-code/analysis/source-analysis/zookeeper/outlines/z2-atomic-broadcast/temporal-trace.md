# Z-2 原子广播 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | 两阶段提案骨架 (propose/processAck/tryToCommit) + syncFollower 三模式 (DIFF/TRUNC/SNAP) — 广播核心定型 |
| 3.5.x | **动态 reconfig**: 双 QuorumVerifier + designatedLeader + allowedToCommit (L999-1022); ZOOKEEPER-1783 初始配置版本协商 (L671-699 注释); chain 提交 (L1105-1114) |
| 3.6+ | **syncThrottler 流控** (INFLIGHT_SNAP/DIFF_COUNT, L566-634); LearnerMaster 接口抽象 (learner 侧可插拔) |
| 3.9.x | 多地址 electionAddr (L220-224); 异步连接 (initiateConnectionAsync L419-427) |

## 痕迹证据

- Leader.java:671-699: ZOOKEEPER-1783 注释 (reconfig 版本协商锚)
- Leader.java:744-754: ZOOKEEPER-1277 低 32 位 rollover 测试钩子
- Leader.java:1105-1114: reconfig 链式提交注释
- Leader.java:1120-1126: ToBeAppliedRequestProcessor 注释 (Final 同步应用前提)
- LearnerHandler.java:258: FORCE_SNAP_SYNC 系统属性
- LearnerHandler.java:566-634: syncThrottler (3.6+ 流控面)
- QuorumCnxManager.java:510-538,635-650: 连接仲裁 (sid 比较)

## 推断标注

- "3.4.x 骨架" — 公知版本线 (ZAB 协议 3.4 定型) (标注)
- "3.5.x reconfig" — ZOOKEEPER-1783 编号年代推断 (标注); 动态 reconfig 3.5.3 引入为公知
- "3.6+ 流控" — syncThrottler 类年代推断 (标注)
- git shallow (1 commit) — 无考古, 全注释锚
