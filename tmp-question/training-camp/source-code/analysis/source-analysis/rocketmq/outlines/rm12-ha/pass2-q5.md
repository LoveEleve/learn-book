# 闭环笔记 q5: Controller 模式 — 选举 + fenced + syncStateSet

## 假设
Controller = 独立 Raft 元数据集群, 管 broker 组选主 + 心跳 + fenced。

## 验证过程
- **ControllerManager** (装配): 双实现 — **JRaftController** (279 行, jraft 库, controllerType=JRAFT 默认) / **DLedgerController** (600 行, controllerDLegerPeers); 心跳 Manager 独立 (BrokerHeartbeatManager.newBrokerHeartbeatManager L86)
- **状态机**: JRaftControllerStateMachine (331 行) — apply 链: AlterSyncStateSet(1001)/ElectMaster(1002)/RegisterBroker(1003)/GetReplicaInfo(1004)/GetNextBrokerId(1012)/ApplyBrokerId(1013)/**RaftBrokerHeartBeatEvent(1018)**/BrokerCloseChannel → **事件驱动 ReplicasInfoManager** (L167)
- **ReplicasInfoManager** (689 行, controller 内存状态):
  - **electMaster** (L193-274): 从未有主 → 首个注册者为主 (L213-216); 旧主存活 → 拒绝 (L227); 失败 → CONTROLLER_MASTER_NOT_AVAILABLE (L270)
  - **getNextBrokerId/applyBrokerId** (L290-313): brokerId 分配 (0=主, 其余从) + registerCheckCode (防伪造)
  - syncStateSet: ALTER_SYNC_STATE_SET 维护 — 参与组提交的副本集 (≈ISR)
  - **hessian 序列化** apply 日志 (L84-94)
- **选举策略**: DefaultElectPolicy (elect/impl/) — syncStateBrokers 内 **maxOffset 降序, 相同 electionPriority 升序** (越小越优先); 尝试 electMasterMaxRetryCount=**3** (ControllerManager:195)
- **心跳与 fenced**: DefaultBrokerHeartbeatManager — 2s 初 + **scanNotActiveBrokerInterval=5s**; 超时=请求头携带 (controllerHeartBeatTimeoutMills, 默认 2min 级) → isBrokerActive=false → 心跳事件 → **makeFenced** (ReplicasManager:878-880: brokerController.setIsolated + runningFlags.makeFenced) — **fenced 拒绝读写**
- **broker 侧 ReplicasManager** (600+ 行): 状态 INITIAL→…; **epoch 递增守卫** (changeBrokerRole L229-233: newMasterEpoch > masterEpoch 才生效); **changeToMaster** (L237-284): 主未变 → changeToMasterWhenLastRoleIsMaster; 从升主 → **handleSlaveSynchronize 追平** → haService.changeToMaster(epoch) → setBrokerId=MASTER_ID + role=SYNC_MASTER + changeSpecialServiceStatus → dataVersion.nextVersion(epoch) → registerBrokerWhenRoleChange; **changeToSlave** (L286+): 主未变 → changeToSlaveWhenMasterNotChange; 变从 → stopCheckSyncStateSet + role=SLAVE
- **控制器地址维护**: ReplicasManager 定时发现 controller leader (L719-720 updateControllerLeaderAddress); 心跳带 maxOffset+confirmOffset+priority (sendHeartbeatToController L392+)

## 代码类型
Architecture (元数据 Raft + 状态机)

## 跨域关联
- RM-11 (路由): Controller 内嵌 namesrv (enableControllerInNamesrv) / CONTROLLER_REGISTER_BROKER=1003 / stateVersion 仲裁
- RM-5 (Broker): ReplicasManager 注册 (RM-5 已见 fenced)
- RM-2 (存储): syncStateSet → inSyncReplicas 组提交协同

## 结论
Controller = 独立 Raft 元数据集群 (1GB 日志, 16 线程) + 心跳超时 → fenced + 选举 (syncStateSet 内 maxOffset+priority, 3 次重试) + epoch 守卫的角色通知 + broker 侧追平/切换/重注册。
源码位置: ReplicasInfoManager.java:193-274,290-313; DefaultBrokerHeartbeatManager.java:59,81-90; ReplicasManager.java:229-233,237-300,878-880; DefaultElectPolicy
