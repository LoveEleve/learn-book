# J-5 存储与 RPC — 知识规划 (KP)

> 域级: 🟡 B | 模块: storage/impl/RocksDBLogStorage (769) + storage/log/RocksDBSegmentLogStorage (1214) + SegmentFile (903) + CheckpointFile (119) + AbortFile (74) + LibC (59) + rpc/ (52 文件) + extension/rpc-grpc-impl (GrpcRaftRpcFactory)
> 日期: 2026-08-15 | 版本: 1.4.1 | RFC: rfcs/0001-new-log-storage.md

## 一、机制提取 (逐源)

### M1 RFC-0001 动机与蓝图
- **动机** (0001-new-log-storage.md:24-28): ①用户被绑定 SOFAJRaft 指定的 RocksDB 版本 ②RocksDB 依赖加大包体积 → 目标纯 Java 日志存储
- 蓝图 (L31-63): 三 DB (LogDB/IndexDB/ConfDB) + FileManager 文件管理 + AllocateFileService 预分配 + 组提交 (L213-225)
- **落地差异**: 1.4.1 未实现 RFC 的完全文件化 (无 LogitLogStorage), 而是 **RocksDBSegmentLogStorage extends RocksDBLogStorage** (RocksDBSegmentLogStorage.java:65) — 类内混合存储, 兼容老数据

### M2 双介质写路径 (4K 阈值)
- **4K 阈值** (RocksDBSegmentLogStorage.java:157-161, 系统属性 jraft.log_storage.segment.value.threshold.bytes)
- 小值 (<4K): 直接进 RocksDB default CF (RocksDBLogStorage.java:519); 段文件只更新 lastLogIndex (L1085-1090)
- **大值 (≥4K)**: SegmentFile.write 顺序写 mmap 段文件 (L1092) → 返回 **16B 位置元数据** (magic 2B + reserved 2B + firstLogIndex 8B + wrotePos 4B, encodeLocationMetadata L1109-1116) → 元数据进 RocksDB
- 写顺序屏障: onDataAppend → db.put → joinAll → doSync (RocksDBLogStorage.java:517-523) — 段文件落盘后才算提交
- **设计**: RocksDB 只存小日志和"指针", 大日志绕开 LSM 写放大; 顺序追加段文件

### M3 批量写 + BarrierWriteContext
- WriteBatch 一次提交 (RocksDBLogStorage.java:542-560); startJob/finishJob 计数 + joinAll 等待异步段写入 (BarrierWriteContext L86-127); addFinishHook 段切换挂回调 (L377)

### M4 分段文件管理
- 段文件 ".s" 后缀 (L129), 文件名 = `%019d` 序号 (L442-445), 正则 [0-9]+\.s (L455); nextFileSequence 单调 (L269) — 列表按 firstLogIndex 有序, 二分查找 (L1118-1152)
- **预分配生产者-消费者** (L264-267, 399-416, 646-656): SegmentAllocator 守护线程维持 blankSegments ≥ 2 (PRE_ALLOCATE_SEGMENT_COUNT=2, L67); 写入侧取已 mmap 文件 (emptyCond/fullCond); 创建失败延迟抛 (L679)
- **段切换** (L330-397): 写不下 → CAS 防并发建 (L367-370) → 旧段置只读 + **旧段 sync 并行** (L379-388) → 新段从预分配队列取
- 文件预热 madvise(MADV_WILLNEED) (SegmentFile.java:351-360, LibC.java:35) — JNA 调内核建议预读

### M5 内存段换出/换入
- 只保留最近 3 段在内存 (MEM_SEGMENT_COUNT=3, L68); 老段 hintUnload (MADV_DONTNEED) + Utils.unmap (SegmentFile.java:367-397); 1 分钟冷却 (L385-387) 仅只读段; 读时按需 swapIn 重新 mmap (L304-319)

### M6 启动恢复 scan
- checkpoint 定位恢复起点 (L466); **abort 文件判正常退出** (L471-475)
- 三分类: blank (header 未写) 复用 / corrupted (仅允许最后一个文件, 改名 .corrupted 保留, 多个则 fatal L623-641) / 正常
- recoverFiles (L557-603): checkpoint 指向文件从 committedPos 起; 尾部脏数据 truncateFile (SegmentFile.java:634-711)
- 完成后: checkpoint 周期任务 (5s, L759-765) + 重建 abort + 启动 allocator

### M7 Checkpoint/Abort 文件
- Checkpoint {segFilename + committedPos} (CheckpointFile.java:41-51), ProtoBufFile 存 LocalFileMeta (L96-105)
- Abort: 启动创建, 正常关闭删除 (AbortFile.java:42-73) — 崩溃残留 = 恢复信号

### M8 SegmentFile 布局与写
- **布局** (SegmentFile.java:53-65): 18B 头 (magic 0x20 0x20 + firstLogIndex 8B + reserved) + [0x57 0x8A | 4B 长度 | 数据] 记录流
- mmap: 新文件创建即按 maxSegmentFileSize (默认 1G, RocksDBSegmentLogStorage.java:153-155) 映射整个大小 (L474-507) — "预分配"的本质
- **异步写** (L738-772): 锁内占位 (wrotePos 推进 + 索引更新 + 返回位置) → writeExecutor 异步 memcpy (默认 core=cpus/max=cpus*3/CallerRunsPolicy, L300-304); 完成后 ctx.finishJob()
- **wrotePos vs committedPos** (L838-865): sync 前移 committedPos + buffer.force(); 读限 committedPos 以内 (L806-811); fsync >1s warn

### M9 截断 (混合架构特色)
- 前缀: 二分找段区间整段 destroy + checkpoint (L880-919)
- 后缀: 先销毁 keptFile 之后段 (L967-969); **段内借助 RocksDB 元数据 (16B 定位记录) 扫描正确 logWrotePos** (L979-1042) → truncateSuffix + clear 64B 洞 (SegmentFile.java:445-462)

### M10 读路径
- RocksDB.get(index) → 16B 元数据 (magic 校验 L1196-1201) → 按 firstLogIndex 二分找段 (L1154-1188) → file.read(logIndex, pos) — 校验 index 范围 + pos < committedPos (SegmentFile.java:800-811)

### M11 RocksDBLogStorage 恢复
- 双 CF: "Configuration" (confHandle) + default (L226-229); CF 参数 useFixedLengthPrefixExtractor(8) + StringAppendOperator (L178-180)
- **load() 扫 conf CF 重建 ConfigurationManager** (L241-280): 8B key 条目解码 ConfigurationEntry (oldPeers 检测联合配置 L253-259); firstLogIndex 元数据 key "meta/firstLogIndex" (L239) 后台删前缀 (L269-271)
- getFirstLogIndex 惰性: 首次 seekToFirst 缓存 (L396-419)

### M12 RPC 抽象与 SPI
- **RaftRpcFactory 接口** (RaftRpcFactory.java:27-107) + RpcFactoryHelper 静态加载 first() (L27-28); @SPI(priority) 选最大 (JRaftServiceLoader L92-107)
- **切换载体 = META-INF/services 文件**: Bolt (priority 0 默认) / GrpcRaftRpcFactory (@SPI(priority=1), GrpcRaftRpcFactory.java:42-43) — 加 jar 即切换
- BoltRaftRpcFactory: registerProtobufSerializer → CustomSerializerManager (L51-53); ensurePipeline 强制 bolt.dispatch-msg-list-in-default-executor=false (L78-85)

### M13 Bolt 服务器/客户端
- BoltRpcServer.registerProcessor → AsyncUserProcessor 包装 (L100-150): interest 路由 + ExecutorSelector 把 JRaft per-peer 单线程调度传给 Bolt (L131-148)
- BoltRpcClient: invokeSync/invokeAsync (L98-129); 异常转换 InvokeTimeoutException/ConnectionFailureException (L103-109)

### M14 gRPC 扩展
- **单方法复用**: FIXED_METHOD_NAME="_call" (L45); 方法全名 = 消息类名._call (L138); MarshallerRegistry 请求→响应映射 (L58-72)
- GrpcServer: 每 processor 建 UNARY MethodDescriptor 注册 MutableHandlerRegistry (L131-207); AppendEntries 特判 executorSelector (L175-187); interceptor 恢复"连接属性"能力 (L145-146)
- GrpcClient: ManagedChannel 池 + checkConnectivity 状态机 (L224-252); 连续失败 ≥2 移除 channel (RESET_CONN_THRESHOLD=2, L65-66)

### M15 处理器注册清单
- RaftRpcServerFactory.addRaftRequestProcessors (L122-147): **核心 7** (AppendEntries/GetFile/InstallSnapshot/RequestVote/Ping/TimeoutNow/ReadIndex) + **CLI 11** (AddPeer/RemovePeer/ResetPeer/ChangePeers/GetLeader/Snapshot/TransferLeader/GetPeers/AddLearners/RemoveLearners/ResetLearners)
- NodeRequestProcessor 模板 (L53-72): NodeManager.get → 强转 RaftServerService 直调 NodeImpl; ENOENT 错误响应

### M16 AppendEntries pipeline 并发模型
- **每 (groupId, peer) 一个 MpscSingleThreadExecutor** (L235-262) — 同 replicator 串行, 不同 peer 并行
- **响应按 sequence 排序投递** (L311-348): 非心跳递增序号 → PriorityQueue → 队头 == nextRequiredSequence 才发; 心跳直发 (L463-464)
- 积压 > maxReplicatorInflightMsgs → 关连接 (L339-346); 连接断开清上下文 (L384-393, 497-516)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M2 双介质 4K 分流 | P1 | 新存储核心 |
| M6/M7 崩溃恢复 | P1 | 数据安全 |
| M12 SPI 可插拔 | P1 | 架构亮点 |
| M16 pipeline 并发 | P1 | 性能核心 |
| M4 预分配/异步写 | P2 | 性能工程 |
| M8 布局与 wrotePos | P2 | 实现面 |
| M14 gRPC 细节 | P2 | 扩展面 |

## 三、负面空间

- **不做完全 Java 化存储**: 仍是 RocksDB 依赖 (RFC 蓝图未落地)
- **不做多段并行读**: 段内读串行
- **不做存储加密/压缩**: 原样存储
- **不做 RPC 运行时切换**: 静态 SPI 绑定, 加 jar 才换
- **不做跨语言协议**: Bolt/gRPC 均 Java 生态
