# J-3 快照压缩 — 日志的"年轮": 整棵树 vs 每年生长记录

> 前置: [[J-2-日志复制]] (日志模型/截断) + [[J-1-RAFT核心循环]] (FSM 应用) | 引出: [[J-4-成员变更]] (快照中的配置) | 对照: Kafka log compaction + MySQL binlog 清理
> 🔴 A | 11 KP | [模式: 冻结-快照-截断 三段]
> Pass 2 闭环: q1(触发) q2(冻结) q3(复用) q4(安装) q5(截断)

**读者处境**: 集群跑了 3 年, 日志 500 万条。新节点加入要从第 1 条日志开始复制吗? 快照是怎么"拍照"的? 为什么快照期间 follower 要拒绝日志?

### 1. 触发 — 定时器与距离阈值

场景: 快照多久做一次? 怎么防"集群同时打快照"?
源码路径:
- snapshotTimer 周期 = snapshotIntervalSecs*1000 = **3600s** (NodeImpl.java:966, NodeOptions.java:68); **首次触发随机化** (L977-990)
- snapshotLogIndexMargin (NodeOptions.java:77, 默认 0): lastApplied - lastSnapshot < margin → ECANCELED (SnapshotExecutorImpl.java:353-371)
- **单快照串行**: downloading/saving → EBUSY (L330-340)
关键设计 (q1): **快照是低频重操作** — 1 小时一次 + 首触发随机化防"多节点同时 IO 高峰"; margin 防"日志还没涨多少就重复打"。 [模式: 周期 + 防抖]

### 2. 冻结 — FSM 队列上的"暂停"

场景: 状态机还在 apply, 怎么保证快照和状态一致?
源码路径:
- SNAPSHOT_SAVE 任务投递到**与 apply 同一个单消费者 Disruptor** (FSMCallerImpl.java:201-210) — 队列内严格串行 (L407-459)
- SnapshotMeta 构造 (L622-655): **lastAppliedIndex/Term 为快照点** (L624-627) + 该点的配置 (L628-648)
- 用户 fsm.onSnapshotSave(writer, done) (L654)
关键设计 (q2): **"冻结"不是暂停, 是排队** — 快照任务排在 apply 之后执行, 期间 lastApplied 不再前进; 快照点 = 队列里那个时刻的状态 — 一致性由单线程串行天然保证, 无需锁。 [模式: 队列串行化]

### 3. 落盘 — temp 目录与原子改名

场景: 快照写到一半崩溃了, 会不会留下半个快照?
源码路径:
- 数据落 `<uri>/temp` (LocalSnapshotStorage.java:60,313); close 时: sync 刷盘 (L230) → 同 index EEXISTS (L240-245) → **Utils.atomicMoveFile(temp → snapshot_<index>)** (L247-261) → ref/unref 引用计数 (L262-270), 旧快照归零物理删 (L199-206)
- meta 文件 `__raft_snapshot_meta` (Snapshot.java:36); 启动清理 temp 只留最新 (L131-173)
关键设计 (q1): **写临时 + 原子改名 = 崩溃安全** — 半成品在 temp, 崩溃后启动清理; 引用计数让"正在传输的快照"不被 GC 删除。 [模式: 临时+原子提交]

### 4. 传输 — remote:// URI 与硬链接复用

场景: follower 落后了, 快照怎么传? 每次都全量吗?
源码路径:
- Replicator.installSnapshot: 打开本地 reader → generateURIForCopy 注册 FileService → **remote://<ip:port>/<readerId>** (Replicator.java:622-708, L649; LocalSnapshotReader.java:144-151)
- **copier 三步** (LocalSnapshotCopier.java:95-187): ①拉 meta (L211-252) ②**filter 硬链接**: 与本地旧快照比对 checksum, 相同文件 Files.createLink 复用 (L254-328, L305) — 只需下载差异 ③逐文件 copyFile (L123-187)
- 分块 offset/count + readSize 推进 (CopySession.java:280-282, 249) = **断点续传**; EAGAIN 限流不消耗重试 (L233)
关键设计 (q3): **快照传输=文件级增量** — 大部分文件没变, 硬链接零拷贝复用; URI 通道与 RPC 分离 (不占 AppendEntries 链路); 分块下载天然支持断点。 [模式: 文件级复用]

### 5. 限流 — 两端共用一个水龙头

场景: 新节点追快照会把集群带宽打满吗?
源码路径:
- ThroughputSnapshotThrottle: 周期配额 limitPerCycle = throttleThroughputBytes/checkCycleSecs (L55); 配额不足给剩余量 (L58-70)
- **服务端 SnapshotFileReader + 客户端 CopySession 共用同一实例** (SnapshotExecutorImpl.java:241-243); 超限 EAGAIN 退避
关键设计 (q3): **发送端限流是服务端视角的自我保护** — 新节点加入不能拖垮现网; 两端共用保证"客户端知道该等"。 [模式: 令牌桶变体]

### 6. 安装 — 下载完才响应

场景: InstallSnapshot RPC 是异步流式的吗?
源码路径:
- 接收链 (SnapshotExecutorImpl.java:512-580): registerDownloadingSnapshot (L519) → copier.join() 阻塞 (L526) → fsmCaller.onSnapshotLoad (L576) → doSnapshotLoad: ESTALE 拒绝 (FSMCallerImpl.java:716-722) → fsm.onSnapshotLoad (L723) → 推进 lastCommitted/lastApplied (L740-742) → onSnapshotLoadDone: **锁外** setSnapshot (L471-475) + updateConfigurationAfterInstallingSnapshot (L488)
- **RPC 在下载+应用全部完成后才响应** (L506)
- 安装期间: 带日志 AppendEntries → EBUSY (NodeImpl.java:2074-2080); preVote 跳过 (L2791-2796)
关键设计 (q4): **同步阻塞式安装** — 简单可靠: 快照期间拒绝写日志, 避免"边下载边收日志"的一致性纠缠; 代价是安装节点短暂"冻结"。 [模式: 互斥阶段]

### 7. 截断 — 三分支的保守主义

场景: 快照装完, 日志从哪开始删?
源码路径:
- setSnapshot (LogManagerImpl.java:629-689): **不推进 diskId** (L652-659, braft#224 — 防内存日志过早清空)
- **截断三分支** (L661-682): ①term==0 (快照领先日志) → 全截 truncatePrefix(lastIncludedIndex+1) (L661-666) ②term 匹配 → 只截到**上次**快照点+1 (L667-677, 保守 — 快照点附近日志可能仍被 follower 拉取) ③term 不匹配 → reset 整体重建 (L678-682)
关键设计 (q5): **删除最小化** — 只删"确定没人要"的日志; 磁盘删除还走异步事件 (TRUNCATE_PREFIX, L556), 内存立即生效; term 不匹配的极端情况直接重建 DB。 [模式: 保守截断]

### 8. 快照里的配置 — 成员历史的固化

场景: 快照怎么记住"当时的集群配置"?
源码路径:
- SnapshotMeta 含 peers/oldPeers/learners/oldLearners (FSMCallerImpl.java:628-648)
- 安装后 updateConfigurationAfterInstallingSnapshot 重算配置 (NodeImpl.java:3502-3504) — J-4 的 ConfigurationManager 以快照点配置为基底
关键设计 (q4): **快照是配置历史的锚点** — 快照点之后的配置变更日志可能已被截断, 快照里的配置成了恢复的起点。 [模式: 元数据快照]

## 代码类型
Architecture (一致性算法核心)

## 负面空间 — SOFAJRaft 快照刻意不做的事

- **不做后台流式安装**: 同步阻塞式 (下载完才响应) — 对照 braft 同为同步
- **不做增量/分层快照**: 全量 + 文件级硬链接复用, 无 base+diff (对照 Kafka tiered storage)
- **不做快照数据压缩**: 原样存储, checksum 仅校验
- **不做多版本保留**: 启动只留最新快照
- **不做安装期间日志并行**: 拒绝写日志保一致性

→ 引出: 集群加节点/换节点怎么不丢数据? → J-4 成员变更
