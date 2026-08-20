# J-3 快照压缩 — 知识规划 (KP)

> 域级: 🔴 A | 模块: storage/snapshot/SnapshotExecutorImpl (780) + local/ (LocalSnapshotStorage 382/LocalSnapshotWriter 154/LocalSnapshotReader 179/LocalSnapshotMetaTable 186/LocalSnapshotCopier 442/SnapshotFileReader 93) + remote/ (CopySession/RemoteFileCopier) + ThroughputSnapshotThrottle (82) + NodeImpl 快照路径
> 日期: 2026-08-15 | 版本: 1.4.1

## 一、机制提取 (逐源)

### M1 触发 (定时器 + 日志距离)
- snapshotTimer 周期 = snapshotIntervalSecs*1000 = **3600s** (NodeImpl.java:966; NodeOptions.java:68); 首次随机化 (L977-990); handleSnapshotTimeout 丢独立线程 (L607-618)
- **snapshotLogIndexMargin** (NodeOptions.java:77, 默认 0): distance = lastApplied - lastSnapshot < margin → ECANCELED (SnapshotExecutorImpl.java:353-371)
- **单快照串行**: downloadingSnapshot/savingSnapshot → EBUSY (L330-340)

### M2 doSnapshot 生成 (FSM 串行化 = "冻结")
- 快照任务投递到**与 apply 同一个单消费者 Disruptor** (FSMCallerImpl.java:201-210) — COMMITTED 与 SNAPSHOT 严格串行 (L407-459)
- **SnapshotMeta 构造** (FSMCallerImpl.java:622-655): lastAppliedIndex/Term 为快照点 (L624-627); 从 logManager.getConfiguration(lastAppliedIndex) 取配置 (L628-648)
- doSnapshot 主链 (SnapshotExecutorImpl.java:314-398): snapshotStorage.create() (L373) → fsmCaller.onSnapshotSave (L385) → 用户 fsm.onSnapshotSave (FSMCallerImpl.java:654)
- **onSnapshotSaveDone** (L400-461): ESTALE 防旧覆盖 (L407-416) → writer.saveMeta + close (L422-432) → **锁外** logManager.setSnapshot (L445)

### M3 writer 落盘 (临时目录 + 原子 rename)
- 写快照数据落 `<uri>/temp` (LocalSnapshotStorage.java:60,313); close: sync → 同 index EEXISTS (L240-245) → **Utils.atomicMoveFile(temp → snapshot_<index>)** (L247-261, ATOMIC_MOVE 回退) → ref/unref 引用计数切换 (L262-270), 旧快照引用归零物理删 (L199-206)
- meta 文件 `__raft_snapshot_meta` (Snapshot.java:36); 目录前缀 snapshot_ (L40)

### M4 文件布局与 meta 表
- `<uri>/snapshot_<index>/` + 数据文件 + `__raft_snapshot_meta` (protobuf LocalSnapshotPbMeta: SnapshotMeta + 文件名→LocalFileMeta 映射, checksum 在 LocalFileMeta)
- LocalSnapshotMetaTable.saveToFile (L113-124); 远程整体传输 saveToByteBufferAsRemote (L61-73)
- LocalSnapshotStorage.init 启动清理 temp, 只留最大 index 快照 (L131-173)
- reader open() ref 防 GC (L328-331); generateURIForCopy 注册 FileService → `remote://<ip:port>/<readerId>` (L144-151)

### M5 服务端文件读取 (限流)
- GetFile RPC → FileService 分发 reader (FileService.java:84); 限流抛 RetryAgainException → EAGAIN (L120-123), 客户端重试不消耗次数
- meta 文件整读返回 EOF (SnapshotFileReader.java:66-72)

### M6 远程下载 LocalSnapshotCopier
- 三步: ①拉 meta 解析 (L98, L211-252) ②**filter 硬链接复用**: 与本地旧快照比对 checksum, 相同文件 Files.createLink 复用 (L254-328, L305) — 避免全量下载 ③逐文件 copyFile (L123-187): 路径穿越防护 canonical 校验 (L189-209) + session 下载 + addFile/sync
- 完成 storage.open() (L118-120)

### M7 分块下载与断点续传 (CopySession)
- 分块 offset/count, 块 = maxByteCountPerRpc (CopySession.java:280-282); readSize 推进 offset (L249) = 天然断点续传
- 重试: count 归零重发 (L223), retryIntervalMs 定时, 超 maxRetry 失败 (L233-243); **EAGAIN 不消耗重试** (L233)
- 落盘 BufferedOutputStream close 时 FD.sync() (RemoteFileCopier.java:134-141)

### M8 下载限流 ThroughputSnapshotThrottle
- 周期配额 limitPerCycle = throttleThroughputBytes / checkCycleSecs (L55); 配额不足给剩余量 (L58-70); 返回 0 退避
- **服务端+客户端共用同一 throttle 实例** (SnapshotExecutorImpl.java:241-243)

### M9 并发下载会话管理
- 同 index 重试替换旧会话 (L644-650, 685-692); 更新快照 cancel 旧的 (L660-681); stepDown/shutdown interruptDownloadingSnapshots (L706-725), 加载中不可中断 (L715-718)

### M10 InstallSnapshot RPC 接收
- 处理器转发 handleInstallSnapshot (InstallSnapshotRequestProcessor.java:53); writeLock 校验 active/term/stepDown/leader 冲突 (NodeImpl.java:3451-3482)
- 主流程 (SnapshotExecutorImpl.java:512-534): registerDownloadingSnapshot + startToCopyFrom (L519) → **copier.join() 阻塞等整包下载** (L526) → loadDownloadingSnapshot (L536-580) → fsmCaller.onSnapshotLoad (L576) → doSnapshotLoad (FSMCallerImpl.java:697-744): ESTALE 拒绝 (L716-722) → fsm.onSnapshotLoad (L723) → 推进 lastCommitted/lastApplied (L740-742) → onSnapshotLoadDone (L463-510): **锁外** setSnapshot (L471-475) + updateConfigurationAfterInstallingSnapshot (L488)
- **RPC 在下载完成后才响应** (L506) — 同步阻塞式安装
- 安装期间: 带日志的 AppendEntries → EBUSY (NodeImpl.java:2074-2080); preVote 跳过 (L2791-2796)

### M11 快照与日志截断联动 (setSnapshot)
- setSnapshot (LogManagerImpl.java:629-689): 旧快照忽略 (L634); configManager.setSnapshot 注入 (L642); **不推进 diskId** (L652-659, braft#224)
- **截断三分支** (L661-682): term==0 → truncatePrefix(lastIncludedIndex+1) 全截 (L661-666); term 匹配 → 只截到上次快照点+1 (L667-677, 保守); term 不匹配 → reset 整体重置 (L678-682)
- truncatePrefix (L986-1005): 内存立即 + 磁盘 TRUNCATE_PREFIX 事件异步 (L556; RocksDBLogStorage.java:570-578)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M2 FSM 串行化冻结 | P1 | 快照一致性的根基 |
| M6 硬链接复用 | P1 | 性能关键设计 |
| M10 同步安装链 | P1 | 完整生命周期 |
| M11 截断三分支 | P1 | 数据安全 |
| M7 断点续传 | P2 | 传输细节 |
| M8 限流 | P2 | 生产必备 |
| M3/M4 落盘布局 | P2 | 实现面 |

## 三、负面空间

- **不做后台流式安装**: InstallSnapshot 阻塞到下载完才响应 (对照 braft 同步式)
- **不做增量快照**: 每次全量 (无 base+diff, 对照 MySQL binlog 增量)
- **不做快照压缩**: 数据文件不压缩 (meta checksum 仅校验)
- **不做多快照保留**: 启动只留最新 (无版本保留策略)
- **不做快照与日志的并行读写**: 安装期间拒收日志
