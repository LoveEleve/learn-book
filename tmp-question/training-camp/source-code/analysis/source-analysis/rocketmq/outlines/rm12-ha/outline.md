# RM-12 HA/DLedger+Controller — 复制水位与自动故障转移

> 前置: [[RM-2-存储]] (双组提交: 刷盘+复制独立水位, ackNums=inSyncReplicas) + [[RM-11-路由]] (acting master 全链) + [[RM-5-Broker]] (ReplicasManager/DLedgerRoleChangeHandler) | 引出: [[RM-13-Proxy]]
> 🔴 A | 6 KP | [模式: 复制协议 + 选举状态机 + 水位推进]
> Pass 2 闭环: q1(双水位) q2(主从同步) q3(AutoSwitchHA) q4(DLedger) q5(Controller) q6(切换链)

**读者处境**: 主挂了消息会不会丢? 谁决定新主? 从库怎么把数据追平? 这篇拆 HA 三模式: 经典主从 (拉取同步) → AutoSwitch (epoch 文件) → Controller 自动故障转移 (选举+fenced)。

### 1. 复制双水位 — 刷盘水位 + 复制水位

场景: 主从怎么知道复制到哪了?
源码路径:
- **双水位闭环** (RM-2 已详, 本域引用): 刷盘水位 (flushedWhere, CommitLog 内部) + **复制水位 push2SlaveMaxOffset** (DefaultHAService:56, 主侧已推送上限) + **slaveAckOffset** (DefaultHAConnection:58, 从库确认点)
- **isSlaveOK** (DefaultHAService:98-105): 有连接 && masterPutWhere - push2SlaveMaxOffset < **haMaxGapNotInSync=256MB**
- **inSyncReplicasNums** (L184-200): masterPutWhere - slaveAckOffset < 256MB → in sync → 组提交 ackNums (RM-2: needAckNums=inSyncReplicas)
- **notifyTransferSome** (L107-117): CAS 推进 push2SlaveMaxOffset → 唤醒 GroupTransferService (双链表 swap, RM-2 已详)
关键设计 (q1): **三条水位线**: 主写 (masterPutWhere) > 主推 (push2SlaveMaxOffset) > 从确认 (slaveAckOffset); **组提交等待语义三档**: ackNums<=1 (无 in sync 从库) → 退化等主推水位 push2SlaveMaxOffset / 经典 → 遍历连接数 ack 计数 (含自身) / **AutoSwitch ALL_ACK → syncStateSet 内全副本**; ⚠ **多连接重复计数缺陷** (GroupTransferService:127 TODO 自认: 同从库多连接可虚高 ackNums → 假 PUT_OK 风险)。[模式: 水位推进]

### 2. 主从同步 — 从库主动拉取

场景: 从库怎么拿到主库数据?
源码路径:
- **连接面**: 从库 DefaultHAClient (411 行) 主动 connectMaster (haMasterAddress) → master AcceptSocketService (NIO Selector, haListenPort=**10912**) 接受 → DefaultHAConnection (476 行, 每连接双线程: ReadSocketService + WriteSocketService); **"主推从拉"精确语义**: 连接与节奏控制 (报 offset) 在从库, 数据流动是主库主动推
- **从库状态机** (DefaultHAClient.run L302-345): SHUTDOWN/READY/TRANSFER — 连接失败 5s 重试; 无响应 **haHousekeepingInterval=20s** 断连; 心跳 **haSendHeartbeatInterval=5s** (空 header)
- **拉取协议**: 从库报 offset (REPORT_HEADER_SIZE=8B) → 主推 [masterPhyOffset(8)+bodySize(4)+body] (TRANSFER_HEADER_SIZE=12B); **首连从 0 请求 → 主从最近 1GB 段起点推** (slaveRequestOffset==0 分支, DefaultHAConnection:287-299); 从库落后 → 报自身 maxPhyOffset 续推补差
- **从库落盘**: appendToCommitLog(masterPhyOffset, body) — **offset 强校验 = 对齐检测** (主推 offset != 自身 maxPhyOffset 即断, DefaultHAClient:195-201 — 防错乱非防落后; 落后靠续推补); 每批 reportSlaveMaxOffsetPlus 回报
- **主推限速**: haTransferBatchSize=**32KB** + 流控 canTransferMaxBytes (L341-350); 无新数据 waitNotifyObject 100ms 等 — **主库消息追加即 wakeupAll 唤醒推送** (CommitLog:1310, 非纯轮询); **冷启动边界** ⚠: 主库数据已过期删除时, 首连 0 请求的从库 1GB 起点已删 → null 循环 + 20s housekeeping 断连 (需主库保留窗口)
关键设计 (q2): **"主推从拉"双线程流水线**: 从库控制节奏 (报 offset), 主库按水位推; offset 强校验防错乱。[模式: 拉取复制]

### 3. 5.x AutoSwitchHA — epoch 文件 + 截断恢复

场景: 从库升主时旧主数据怎么裁决?
源码路径:
- **EpochFileCache** (327 行): epoch 文件记录 (epoch, startOffset) — 数据合法区间; storePathEpochFile
- **截断三路径** (AutoSwitchHAService): truncateInvalidMsg (L128, **RocksDB 辅助**截断主传来的不完整消息 — 隐含 RocksDB 依赖) / truncateSuffixByEpoch (L138-141, 从库升主截断旧主写入的**未确认**数据) / truncateEpochFilePrefix (L484)
- **changeToMaster** (L199-209): 追加新 epoch 条目; **confirmOffset** (storeCheckpoint 持久化, DefaultMessageStore:386) — 新主从 confirmOffset 起恢复
- **AutoSwitchHAConnection 头扩展**: HANDSHAKE_HEADER_SIZE=**20B** (4+4+8+4) + TRANSFER_HEADER_SIZE=**28B** + EPOCH_ENTRY_SIZE=**12B** — 握手带 epoch/slaveId
- **AutoSwitchHAClient** (598 行): 从库侧同步 epoch 缓存 + caught-up 判定 (L316)
关键设计 (q3): **epoch 文件 = 数据权威裁决** (谁在哪个区间合法); 切换 = 截断未确认 + 新 epoch 起算 — 防旧主复活污染。[模式: 版本区间]

### 4. DLedger 模式 — Raft 日志复制

场景: 不用主从推送, 怎么保证一致?
源码路径:
- **DLedgerCommitLog**: CommitLog 接入 openmessaging dledger 库 — 写路径走 Raft (多数派落盘) 替代主从复制; 队列/索引照旧
- **DLedgerRoleChangeHandler** (broker/dledger, 36 行): DLedgerLeaderElector.RoleChangeHandler — **changeToSlave / changeToMaster(BrokerRole.SYNC_MASTER)** (L68-91) — 角色回调驱动 broker 切换
- **与 Controller 协同**: DLedger 模式自带选举 (RAFT_BROKER_HEART_BEAT_EVENT=1018), 不依赖 Controller 选主; 5.x 演进为 Controller 模式
关键设计 (q4): **Raft 把复制从"主从 push"变成"多数派日志"** — 选主 + 复制一体化; 写放大更高 (N 副本日志), 换来自动选主。[模式: Raft]

### 5. Controller 模式 — 选举 + fenced + syncStateSet

场景: 5.0 怎么自动故障转移?
源码路径:
- **ControllerManager** (controller 模块): 双实现 — **JRaftController** (279 行, jraft 库, 默认) / **DLedgerController** (600 行); StateMachine apply 链 (JRaftControllerStateMachine:331, 事件驱动 ReplicasInfoManager)
- **ReplicasInfoManager** (689 行, controller 内存状态): brokerId 分配 (**getNextBrokerId/applyBrokerId=1012/1013, registerCheckCode="addr;id" 编码防伪造**) / **electMaster** (L193-274: 首选首个注册者; 旧主存活拒绝; **designateElect 强制指定**; 失败 → CONTROLLER_MASTER_NOT_AVAILABLE) / syncStateSet 维护 (ALTER_SYNC_STATE_SET=1001); **选举触发双轨**: Controller 扫无主 (scanInactiveMasterInterval=5s) + broker 主动尝试 (ReplicasManager:378 brokerElect)
- **选举策略**: DefaultElectPolicy (impl/) — syncStateBrokers 内 **maxOffset 降序, 相同则 electionPriority 升序** (越小越优先); 尝试 electMasterMaxRetryCount=**3**
- **心跳与 fenced**: broker 心跳 **1s 周期** (brokerHeartbeatInterval=1000) + **controllerHeartBeatTimeoutMills=10s 超时** (容 10 次丢失); DefaultBrokerHeartbeatManager 2s 初 + **5s 扫描** (扫描周期 ≠ 心跳周期); 超时 → 心跳事件 (RAFT_BROKER_HEART_BEAT_EVENT=1018) → **makeFenced** (setIsolated+runningFlags 双层拒读写); **fenced 生命周期**: 启动即 true (BrokerController:845) → RUNNING 解封 false (ReplicasManager:205) → 超时再 true
- **broker 侧 ReplicasManager** (600+ 行): 状态机 INITIAL→…; **epoch 递增守卫** (newMasterEpoch > masterEpoch 才生效, L229-233); changeToMaster: 从库先 **handleSlaveSynchronize 追平** → haService.changeToMaster → setBrokerId=MASTER_ID + role=SYNC_MASTER → dataVersion.nextVersion(epoch) → registerBrokerWhenRoleChange; changeToSlave 对称 (stopCheckSyncStateSet + role=SLAVE)
关键设计 (q5): **Controller = 独立 Raft 元数据集群** (1GB 日志) + broker 心跳 + fenced; 选举在三要素 (syncStateSet 内 / 数据最新 / 优先级) 上裁决; epoch 防旧通知。[模式: 元数据 Raft + 状态机]

### 6. 切换全链 — 心跳超时 → 选举 → 角色通知 → 重注册

场景: 主挂后整个链路怎么反应?
源码路径:
- **触发**: namesrv 心跳超时 (RM-11: 2min/5s 扫描) → acting master 通知 (RM-11) 或 Controller 心跳超时 (本域) → electMaster
- **通知链**: controller NotifyService (ControllerManager:309-348, **epoch 单调覆盖: 新 epoch cancel 旧 future**, 3 线程) → broker changeBrokerRole (epoch 守卫) → 切换动作 (changeToMaster/Slave)
- **重注册**: registerBrokerWhenRoleChange → namesrv 更新 (brokerId=0 换主) → **namesrv acting master 伪装/擦权** (RM-11 交叉) → 客户端路由感知 (10-60s 注册周期, RM-5)
- **AutoSwitch 数据面**: 新主 truncateSuffixByEpoch + confirmOffset 恢复 → 从库 AutoSwitchHAClient 追平 → caught-up → syncStateSet 扩员
关键设计 (q6): **故障转移 = 管控面 (选举) + 数据面 (截断/追平) 双轨**; namesrv 路由变化是最后的可见环节。[模式: 故障转移链]

### 负面空间 — HA 刻意不做的事

- **旧主从不做自动故障转移**: 经典模式主挂需人工介入 (5.0 Controller 才自动化)
- **不做跨机房同步**: HA 复制为单集群内; 跨机房靠业务双写/同步工具
- **不做消息级复制确认**: 水位 (offset) 级 ack — 组提交粒度 = 单消息物理段
- **不做副本读负载均衡**: 从库只做有限读 (4.x 从读面, 默认主读)
- **不做 RPO=0 双写**: 异步复制窗口内丢消息 (sync 模式才有组提交等待)
- **DLedger/Controller 写放大**: N 副本全日志复制 — 与 Kafka ISR 类似但无分区级 leader 均衡
- **不做 gossip**: 集群拓扑静态 (brokerName 分组)

→ 引出: 新协议面怎么接入? gRPC 与安全 → [[RM-13-Proxy]]
