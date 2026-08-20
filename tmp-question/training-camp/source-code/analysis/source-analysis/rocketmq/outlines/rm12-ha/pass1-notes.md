# RM-12 HA/DLedger+Controller — Pass 1 探索笔记

> 域: RM-12 HA/DLedger + Controller | 🔴 A 方案 | 2026-08-14
> 源码: store/ha/ (DefaultHAService 380 + DefaultHAConnection 476 + DefaultHAClient 411 + GroupTransferService 176 + HAService 168 + WaitNotifyObject 112 + HAConnectionStateNotificationService 150 + autoswitch/ 1907) + broker/controller/ReplicasManager (600+) + broker/dledger/DLedgerRoleChangeHandler (36) + controller/ (45 文件/6347 行: ControllerManager + JRaftController 279 + JRaftControllerStateMachine 331 + DLedgerController 600 + ReplicasInfoManager 689 + BrokerHeartbeatManager 族 + ElectPolicy) | RocketMQ 5.3.1

## 调用图

```
经典主从 (3.x):
从库 DefaultHAClient (状态机 READY→TRANSFER): connectMaster → 报 offset → 收 [offset+size+body]
  → appendToCommitLog (offset 强校验) → reportSlaveMaxOffsetPlus
主库: AcceptSocketService (NIO 10912) → DefaultHAConnection (ReadSocket: 收 ack → slaveAckOffset
  → notifyTransferSome → push2SlaveMaxOffset CAS → 唤醒组提交; WriteSocket: 推数据 32KB+流控)
组提交 (RM-2): GroupTransferService 等 slaveAckOffset ≥ needAckNums=inSyncReplicas 水位

AutoSwitchHA (5.x): epoch 文件 + truncateInvalidMsg (RocksDB) + truncateSuffixByEpoch
  + confirmOffset (storeCheckpoint) — 切换数据裁决

DLedger: DLedgerCommitLog (Raft 多数派落盘) + DLedgerRoleChangeHandler (changeToMaster/Slave)

Controller (5.0): broker 心跳 (1018) → DefaultBrokerHeartbeatManager (5s 扫描) → 超时
  → ReplicasInfoManager.electMaster (syncStateSet 内 maxOffset+priority)
  → NotifyService → broker ReplicasManager.changeBrokerRole (epoch 守卫)
  → changeToMaster (追平+setBrokerId=0+重注册) / changeToSlave
  → namesrv 路由更新 (RM-11 acting master 协同)
```

## 基本元素分解

1. **双水位**: masterPutWhere / push2SlaveMaxOffset / slaveAckOffset + isSlaveOK + inSyncReplicas
2. **主从同步**: 从库拉取状态机 + 双线程连接 + offset 强校验 + 限速
3. **AutoSwitchHA**: epoch 文件 + 截断 + confirmOffset
4. **DLedger**: Raft 日志复制 + 角色回调
5. **Controller**: JRaft/DLedger 双实现 + 选举 + fenced + syncStateSet
6. **切换全链**: 心跳超时 → 选举 → 角色通知 → 追平/截断 → 重注册

## 标记问题 (20 问)

1. 三条水位谁推进? (CAS/ack)
2. isSlaveOK 阈值? (256MB)
3. 从库怎么连主? (主动 connectMaster)
4. 首连从哪开始推? (1GB 段起点)
5. offset 强校验? (slavePhyOffset != masterPhyOffset 断)
6. 主推限速? (32KB + 流控)
7. epoch 文件记什么? (epoch, startOffset)
8. 升主截断什么? (未确认数据)
9. confirmOffset 是什么? (复制确认水位)
10. DLedger 写路径? (多数派日志)
11. 角色回调? (DLedgerRoleChangeHandler)
12. Controller 双实现? (JRaft/DLedger)
13. 选举策略? (maxOffset+priority)
14. fenced 干什么? (拒读写)
15. syncStateSet? (ISR 类似)
16. epoch 守卫? (防旧通知)
17. 切换后重注册? (registerBrokerWhenRoleChange)
18. namesrv 协同? (acting master)
19. 心跳超时双判? (namesrv+controller)
20. 与 Kafka 对照? (ISR/LeaderEpoch)

## 时空溯源 (代码内痕迹)

- 3.x: DefaultHAService 主从推送 + GroupTransferService 双水位 (RM-2 已证)
- 4.x: DLedgerCommitLog + DLedgerRoleChangeHandler (openmessaging dledger 接入)
- 5.0: Controller 模式 (JRaftController + ReplicasInfoManager + fenced + syncStateSet)
- 5.x: AutoSwitchHA (epoch 文件 + confirmOffset + RocksDB 截断) + broker 侧 ReplicasManager 状态机

## 大域拆分判断

RM-12 = store/ha (经典+AutoSwitch) + dledger (broker 接入面) + controller (选举面) + broker 侧 ReplicasManager; 单篇 🔴 A (6 闭环); dledger/raft 库本体不展开 (外部依赖面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "主从同步 (HAService: 拉取模式)" | **从库主动 connect + 报 offset + 主推** — 双线程流水线实证 | **接受+精确化** ✅ |
| "GroupTransferService 等 ackNums" | RM-2 已闭环; 本域补 push2SlaveMaxOffset/slaveAckOffset 推进链 | **接受** ✅ |
| "DLedger 模式 (Raft 选举替代主从)" | DLedgerCommitLog + RoleChangeHandler 实证 | **接受** ✅ |
| "Controller 模式 (fenced 初始→选举→解封)" | ReplicasManager setFenced (makeFenced) + electMaster + changeBrokerRole | **接受** ✅ |
| 数字: HA 参数 | haListenPort=10912 / 心跳 5s / housekeeping 20s / 32KB / 256MB / 5s syncFlush | **补充** ✅ |
| 数字: AutoSwitch 头 | HANDSHAKE 20B / TRANSFER 28B / EPOCH_ENTRY 12B | **补充** ✅ |
| 数字: Controller | 5s 扫描 / 2min 心跳超时默认 / electMasterMaxRetryCount=3 / 1GB 日志 | **补充** ✅ |
| 数字: 请求码 | 1001-1013 + 1018 RAFT 心跳事件 | **补充** ✅ |
