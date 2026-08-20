# RM-12 HA/DLedger+Controller — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | 经典主从: DefaultHAService (AcceptSocketService NIO) + DefaultHAConnection (双线程) + DefaultHAClient (从库拉取状态机) + GroupTransferService 组提交 (RM-2 已证) — haListenPort=10912/32KB/256MB 常量风格 |
| 4.x | **DLedger 接入**: DLedgerCommitLog (openmessaging dledger 库, Raft 多数派) + DLedgerRoleChangeHandler (RoleChangeHandler 回调) — 选主+复制一体化 |
| 5.0 | **Controller 模式**: controller 模块 (JRaftController 默认 / DLedgerController) + ReplicasInfoManager + DefaultBrokerHeartbeatManager + fenced + syncStateSet + 1000 系请求码 + namesrv enableControllerInNamesrv 内嵌 |
| 5.x | **AutoSwitchHA**: epoch 文件 (EpochFileCache) + confirmOffset (storeCheckpoint) + RocksDB 截断 + 协议头扩展 (20/28/12B) + broker 侧 ReplicasManager 状态机 + brokerElectionPriority 选举优先级 |

## 痕迹证据

- MessageStoreConfig.java:204-220: ha 六参数 (10912/5s/20s/32KB/256MB/5s) — 3.x 常量
- DefaultHAConnection.java:48,260: TRANSFER_HEADER_SIZE=12B (3.x 协议)
- AutoSwitchHAConnection.java:56-72: HANDSHAKE_HEADER_SIZE=20B/TRANSFER=28B/EPOCH_ENTRY=12B (5.x 协议扩展)
- RequestCode.java:253-290: 1001-1013 + 1018 (5.0 Controller 系)
- ControllerConfig.java:36-88: 5s/16/50000/1GB/3 次重试 (5.0)
- EpochFileCache.java: 327 行独立文件 (5.x)
- DefaultMessageStore.java:386: confirmOffset (5.x 复制确认水位)
- DLedgerRoleChangeHandler.java:36: RoleChangeHandler 接口 (4.x)

## 推断标注

- "3.x 经典主从" — 公知版本线 + 常量风格推断 (标注); 高置信 (3.x 架构文档同构)
- "4.x DLedger" — openmessaging dledger 项目年代推断 (标注)
- "5.0 Controller / 5.x AutoSwitch" — 配置项 + 新类推断 (标注); 未做 git 考古
