# 闭环笔记 q4: DLedger 模式 — Raft 日志复制

## 假设
DLedger 用 Raft 多数派日志替代主从推送, 自带选主。

## 验证过程
- **DLedgerCommitLog**: store 内 CommitLog 的 DLedger 变体 — 消息追加走 openmessaging dledger 库 (AppendEntry → 多数派落盘 → ack), 非主从 HA 推送
- **DLedgerRoleChangeHandler** (broker/dledger, 36 行): `implements DLedgerLeaderElector.RoleChangeHandler` (L36):
  - **changeToSlave(brokerId)** (L68-72): 旧主降级 (可能因连续丢失选举)
  - **changeToMaster(BrokerRole.SYNC_MASTER)** (L91): 当选 → broker 切 SYNC_MASTER
  - 回调链: dledger 库内部选主 (心跳超时/日志对比) → 回调 broker 侧角色动作
- **RAFT_BROKER_HEART_BEAT_EVENT=1018**: DLedger 模式 broker 心跳事件进 raft 日志 (Controller 模式同码复用 — 心跳统一)
- **与 Controller 的关系**: DLedger 模式自带选主 (独立 raft 组 per broker 组); Controller 模式选主外置 (controller raft 组管所有 broker 组) — **演进方向: DLedger → Controller** (Controller 5.0 起)

## 代码类型
Integration (Raft 接入)

## 跨域关联
- RM-1 (协议): 1018 请求码
- RM-5 (Broker): DLedgerRoleChangeHandler 注册 (RM-5 已见 BrokerController:797-798)
- RM-12 q5: Controller 模式对照

## 结论
DLedger = 写路径换 Raft 多数派 (选主+复制一体), 角色回调驱动 broker 切换; 与 Controller 模式是"内嵌选举 vs 外置选举"的演进对照。
源码位置: broker/dledger/DLedgerRoleChangeHandler.java:36,68-91; RequestCode.java:290
