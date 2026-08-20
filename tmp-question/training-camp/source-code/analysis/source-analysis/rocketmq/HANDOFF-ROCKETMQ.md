# RocketMQ 源码分析 — 超详细交接文档 V14 (阶段 4.1, 13/13 收官 ✅)

> **⚠ 本文取代 V1-V13 为 RocketMQ 分域唯一入口** — 前版内容全量并入并升级; 新会话只需读本文 + 规划 (ROCKETMQ-PLAN.md)
>
> **日期**: 2026-08-14 | RocketMQ 5.3.1 (pom.xml:31 实证, 19 Maven 模块) | 本 V14 为**自包含全量交接** (固化全部 13 域核心知识, 新会话零回溯)
> **入口关系**: 阶段4.1 总入口 (执行计划) | 域规划 `ROCKETMQ-PLAN.md` (13 域, 09 审计 v1 + 客户端缺口 v2 + 定级 v3/v3.1) | 本文 V14 为唯一入口 — **13 域全部交付, 阶段 4.1 收官**
> **给新 AI**: 读 §零 (13/13 ✅) → §一 (13 域速查) 开工; 后续域工作: 无 (收官) — 如需深挖 TieredStore (🟡 C) 或补 harness 见 §二。

---

## §零 状态总览 (2026-08-14, 13/13 收官 ✅)

### 完成状态表

| 域 | 目录 | 类型 | 大纲行数 | 闭环 | questions | 行号验证 | REVIEW 发现 | harness |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| RM-1 remoting 协议层 | rm1-remoting | 🔴 | 80 | 6 | 20 | ~40 | 20 | 无* |
| RM-2 存储底层 | rm2-store | 🔴 | 82 | 6 | 20 | ~45 | 18 | 无* |
| RM-3 CommitLog+CQ+Index | rm3-commitlog | 🔴 | 76 | 6 | 20 | ~45 | 15 | 无* |
| RM-4 延迟消息 | rm4-delay | 🟡 | 76 | 6 | 20 | ~35 | 15 | 无 |
| RM-5 Broker 启动 | rm5-broker | 🔴 | 69 | 6 | 20 | ~40 | 14 | 无* |
| RM-6 消息过滤 | rm6-filter | 🟡 | 72 | 6 | 20 | ~35 | 14 | 无 |
| RM-7 Producer 发送 | rm7-producer | 🔴 | 71 | 6 | 20 | ~40 | 13 | 无* |
| RM-8 消费-Push | rm8-push | 🔴 | 66 | 6 | 20 | ~45 | 15 | 无* |
| RM-9 Rebalance+LitePull | rm9-rebalance | 🔴 | 67 | 6 | 20 | ~40 | 12 | 无* |
| RM-10 顺序+事务 | rm10-order-tx | 🟡 | 83 | 6 | 20 | ~40 | 15 | 无 |
| RM-11 Namesrv 路由 | rm11-namesrv | 🟡 | 78 | 6 | 20 | ~38 | 16 | 无 |
| RM-12 HA/DLedger+Controller | rm12-ha | 🔴 | 81 | 6 | 20 | ~42 | 15 | 无* |
| RM-13 Proxy+安全 | rm13-proxy | 🟡 | 73 | 6 | 20 | ~36 | 15 | 无 |

> *🔴 A 方案本应 harness — 已记录取舍: 13 域均为 Java 生态面, 官方测试覆盖充足 (filter 2129 行/consumer 5345 行/store 52 文件等), 自建 harness 边际价值低; 若需验证核心算法 (布隆数学/AVG 分配/组提交/hash 取模/批量注销/双水位) 可后续补 (记录于 §二)。
> REVIEW 发现 = 深审 + 多次 REVIEW 累计编号 (每域 11-20 处)。

### 执行序 (已完成 13/13 — 全部收官 ✅)

```
✅ RM-1 → RM-2 → RM-3 → RM-4 → RM-5 → RM-6 → RM-7 → RM-8 → RM-9 → RM-10 → RM-11 → RM-12 → RM-13
```

### 分层进展

- **协议与存储 (5)**: remoting 协议层 ✅ → 存储底层 ✅ → 主链路 (CommitLog/CQ/Index) ✅ → 延迟消息 ✅ → Broker 装配 ✅
- **生产与过滤 (2)**: 消息过滤 ✅ → Producer 发送 ✅
- **消费面 (2)**: 消费-Push ✅ → Rebalance/起点/LitePull ✅
- **顺序与事务 (1)**: 顺序+事务 ✅ (选择器绑定队列 + 半消息回查)
- **路由与 HA (2)**: Namesrv 路由 ✅ → HA/DLedger+Controller ✅
- **网关与安全 (1)**: Proxy+安全 ✅ (gRPC 层 + 双协议翻译 + 认证一句话)

### 交付物统计

- 大纲 (outline.md) 13 域 / 13 篇 / 总计 **974 行** (2026-08-14 实测: 12 域 901 + RM-13 73; 域文件全量 **6617 行** — 含 RM-13 540, 12 域 6129)
- 闭环 78 (每域 6) / questions 260 (每域 20)
- 行号穷举验证累计 **~521 处** (各域 35-45 处)
- 深审+多次 REVIEW 发现问题累计 **197 处** (各域编号 12-20)
- 全部 13 域完成 Pass 0-3 + 六层深审 + 时空溯源 + 每域 4-5 轮 REVIEW (RM-10/11/12/13 五次)

> **📋 V14 文档自审记录 (2026-08-14, 收官)**: 交付物统计与文件全量核对一致。RM-13 大纲 73 行逐文件实测 (域合计 541 行); REVIEW 发现编号 15 (深审 5 + 二次 2 + 三次 2 + 四次 3 + **五次 3**); 五次 REVIEW 核心认知: **多协议协商在 8080** (MultiProtocolRemotingServer + enableRemotingLocalProxyGrpc), 'PRI ' magic → HTTP/2 **本地转发 LOCAL_HOST:8081** — 单端口暴露方案; gRPC 8081 独立 io.grpc server。**阶段 4.1 RocketMQ 13 域全量收官**。

---

## §一 13 域核心知识速查 (全量固化)

> 每域格式: 核心机制表 / 时空溯源 / 深审发现 / 负面空间。行号均为 5.3.1 实测。

### RM-1 remoting 协议层 — 私有协议与请求-响应分发

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **帧格式**: [4B 总长][4B 头长][header][body]; markProtocolType = **类型<<24 \| 低 24 位头长** (高 8 位 serializeType, 头长上限 16MB); flag 仅 2 位 (RPC_TYPE bit0/ONEWAY bit1); **suspended 独立字段仅 JSON 路径** | RemotingCommand.java:245-247,395-409,50-51; RocketMQSerializable.java:57-80 |
| **双序列化**: JSON=0 默认 / **ROCKETMQ=1 二进制** (5.x, 头序: code short→language byte→version short→opaque int→flag int→remark→map[FastCodesHeader 免反射+extFields short 键/int 值]); 系统属性可配; **零拷贝 fastEncodeHeader** (ByteBuf 直写) | SerializeType.java; RocketMQSerializable.java:57-80; RemotingCommand.java:450-473 |
| **编解码**: LengthFieldBasedFrameDecoder (16MB, 偏移 0 长度 4) + RemotingCommand.decode/encode; **协议错误即断连** | NettyDecoder.java:34-62 |
| **分发**: REQUEST/RESPONSE 分派; processorTable (code → Pair<processor, 独立线程池>) + default 兜底 + **rejectRequest 限流** (处理器实现); 响应 opaque → responseTable 匹配 → 回调/唤醒; writeResponse opaque 回填+markResponseType+metrics | NettyRemotingAbstract.java:96,177-302,385-400 |
| **客户端三模式**: invokeSync (超时从取通道扣减 + 分级关闭 left>100ms 或 >timeout/4) / Async (预注册+回调) / **Oneway 双端无响应** (服务端 writeResponse 直接返回); 长连接缓存 channelTables; **中断拉取唤醒** (code 11/361) | NettyRemotingClient.java:540-632,593-606 |
| **服务端面**: registerProcessor; **160 请求码** (PULL_MESSAGE=11/HEART_BEAT=34/LITE_PULL=361 5.x/RAFT_BROKER_HEART_BEAT=1018); **双实现** (Netty + proxy 内嵌翻译层 RemotingProtocolHandler) | RequestCode.java (160 常量); NettyRemotingServer.java:339-351 |
| **扩展面**: RPCHook (认证挂点); **rpc/ 层 = RemotingClient 高层抽象** (RpcClientImpl, BrokerOuterAPI 消费 — 非 gRPC); **proxy RemotingProtocolHandler 双协议翻译** (老客户端 remoting→grpc); TLS (系统属性); metrics (OTel); ChannelEventListener | RPCHook.java; TlsSystemConfig.java:24-30; rpc/ 12 文件 |

**时空溯源**: 3.x 协议定型 (4+4 帧/JSON/opaque/processorTable 至今未变) → 4.x TLS/RPCHook → 5.0 二进制序列化+rpc/ 桥+metrics → 5.1+ fastEncodeHeader 零拷贝+FastCodesHeader+361 长轮询码

**深审 (20 处, 4 轮 REVIEW)**: 认知修正 2 (markProtocolType 高 8 位类型 — pass1 猜反 / **rpc/ 层=RemotingClient 高层抽象非 gRPC 桥**); 表述修正 1 (suspended 双路径差异 — 二进制头无字段); 补锚 5 (二进制头字段序/rejectRequest 实现方/proxy 翻译层/namesrv 轮询切换/线程默认值 8-3-120s); 推理验证 22 项全过 (头长位宽闭环/oneway 全链/二进制头对称/160 码增量等)

**负面空间**: 不协议协商 (serializeType 全局配置)/不背压 (拒绝即错)/不多路复用/不压缩/不追踪内建

### RM-2 存储底层 — 内存映射文件与刷盘三服务

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **MappedFile**: mmap (fileChannel.map READ_WRITE) + **双缓冲** (writeBuffer 堆外池 vs mappedByteBuffer; getAppendBuffer 选择); append→commit (transferTo 页缓存)→flush (force) 三阶段; appendMessageUsingFileChannel (池满直写 "never both"); 读面 mmap 切片 + **引用计数延迟卸载 (hold 语义: 读/刷/转储前置)**; isLoaded0 页统计 | DefaultMappedFile.java:83-95,168,301-302,361-412,503-591 |
| **刷盘**: SYNC→GroupCommitService (双链表 swap 无锁读侧 + flushedWhere 水位 + 1000 次重试) / ASYNC→FlushRealTimeService (**500ms 周期 + leastPages=4 攒页 + 每 10s leastPages 降 0 强刷全量**) / 池启用→CommitRealTimeService 转储; **双组提交**: 刷盘 (CommitLog 内部) + **复制 (ha/GroupTransferService 独立类, doWaitTransfer 等从库水位)**; needAckNums=**inSyncReplicas** (L951-965); 同步 = future.get(syncFlushTimeout=5s) + thenCombine 双结果; **磁盘保护在 store 模块** (DefaultMessageStore L2340+: warning/cleanForcibly 双 ratio + diskFull 标志) | CommitLog.java:1432-1660,2109-2156; ha/GroupTransferService.java; DefaultMessageStore.java:2340-2360; MessageStoreConfig:220 |
| **文件段序列**: 1GB 对齐段 (createOffset 取模对齐); shouldRoll 滚动; **索引除法定位 + 边界校验 + 线性兜底 + returnFirstOnNotFound**; 批量 commit/flush 推进水位; 过期删除; 5.x MultiPath 多盘 | MappedFileQueue.java:210-344,670-700 |
| **预分配+锁**: AllocateMappedFileService 后台 mmap (路径去重 + ServiceLoader 自定义实现 5.x + **创建即 warm = 逐页写 0+mlock 锁页** + >10ms 告警); 写锁**默认自旋** (CommitLog:133) + 组提交内部自旋 | AllocateMappedFileService.java:154-210; DefaultMappedFile.java:621-660 |
| **堆外池**: 预分配 5×1GB + **mlock 锁页**; ConcurrentLinkedDeque 借还; **<40% 水位告警**; 默认关闭 | TransientStorePool.java:30-85; MessageStoreConfig:237-238 |
| **写读链**: putMessage → 锁 → 定位 → doAppend 编码 → **flush+HA thenCombine 并行合并**; 状态 PUT_OK/FLUSH_DISK_TIMEOUT/SLAVE_NOT_AVAILABLE; 5.x FlushDiskWatcher 刷盘监控 | CommitLog.java:1071-1290 |

**时空溯源**: 3.x 骨架定型 (mmap+1GB 段+SYNC/ASYNC) → 4.x 堆外池+预分配+自旋锁 → 5.x FlushManager 接口化+Watcher+多盘+isLoaded0

**深审 (18 处, 4 轮 REVIEW)**: 机制缺口 1 (**双组提交服务: 刷盘+复制独立水位等待, ackNums=inSyncReplicas**); 归位修正 1 (磁盘保护在 store 非 broker); 补锚 6 (写锁默认自旋/syncFlushTimeout=5s/thenCombine/warm=写0+mlock/持久化定时链/10s 强刷=leastPages 降 0); 推理验证 24 项全过 (1GB 对齐/组提交重试上限/hint 判定式等量变换等)

**负面空间**: 不磁盘压缩 (对照 Kafka compaction)/不稀疏存储/不 DirectIO/不聚合/不分层 (TieredStore 才引入)

### RM-3 CommitLog+ConsumeQueue+IndexFile — 消息主链路与双索引

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **消息编码 18 段**: TOTALSIZE/MAGICCODE/BODYCRC/QUEUEID/FLAG/QUEUEOFFSET/PHYSICALOFFSET(doAppend 回填)/SYSFLAG/BORNTIMESTAMP/BORNHOST/STORETIMESTAMP/STOREHOST/RECONSUMETIMES/PreparedTxOffset/BODY/TOPIC/PROPERTIES/**属性 CRC32 (4B, enabledAppendPropCRC)**; calMsgLength 预算 + V6 标志 (8/20B); 双上限 (message 4MB/body) + 属性超限 (Short.MAX); V2 topic short | MessageExtEncoder.java:41-278; MessageStoreConfig:168 |
| **写后分发链**: dispatcherList = BuildCQ + BuildIndex + Compaction (5.1); doDispatch && !isFileEnd; **恢复双路径**: 正常退出不重放 (L337 "normal recover doesn't require dispatching") / 异常退出重放重建 (L695); DispatchRequest 契约 + CQ 30 次重试/isCQWriteable | DefaultMessageStore.java:266-272,418,1989-1995,2108-2112; CommitLog.java:338-352,695 |
| **ConsumeQueue 20B 单元**: commitLogOffset(8)+size(4)+tagsCode(8, **= tags.hashCode()**); **Ext (enableConsumeQueueExt 配置)**: 位图/时间转址 (isExtAddr); **Batch CQ 单元 46B**; 文件 300000×20≈5.7MB + Ext 48MB; 独立刷盘 1s; **5.x 四实现** (默认/Batch/Sparse/RocksDB) | ConsumeQueue.java:59,1099-1103; MessageExtBrokerInner.java:43-47; queue/BatchConsumeQueue.java:63 |
| **IndexFile**: 40B 头 (BeginTs/EndTs/BeginPhy/EndPhy/SlotCount/IndexCount) + 500 万×4B 槽表 + 20B 索引项 (keyHash/phyOffset/timeDiff/prevIndex 链); key=topic#uniqKey + **多 keys 逐个索引**; **TRANSACTION_ROLLBACK 跳过**; putKey full → 新文件 | IndexHeader.java:36-45; IndexFile.java:32-58; IndexService.java:213-258 |
| **读面+恢复**: getMessage 守卫 → CQ 定位 → mmap 切片 → 双限 (条数/字节); **getMessageAsync = completedFuture 同步包装 (非异步 — 长轮询在 broker PullRequestHoldService, RM-8)**; **恢复顺序: CQ 先恢复定界 → CommitLog 对齐截断** (L1890-1902) | DefaultMessageStore.java:781-830,1890-1902,984-990 |
| **测试面**: 7 专项 (ConsumeQueue/Batch/Sparse/Store/RocksDB/IndexFile/Ext) | store/src/test |

**时空溯源**: 3.x 三文件体系定型 → 4.x Ext+批量+CRC → 5.0 queue/ 多实现 (RocksDB) + getMessageAsync → 5.1 Compaction

**深审 (15 处, 4 轮 REVIEW)**: 认知修正 2 (**Ext=配置开启非 tagsCode 溢出** / **getMessageAsync=completedFuture 同步包装, 长轮询在 broker PullRequestHoldService**); 表述修正 3 (恢复双路径/CRC32=属性校验和/恢复顺序 CQ 先定界); 补锚 4 (maxMessageSize=4MB/tagsCode 生成/rollback 跳过+多 key/Batch 46B); 推理验证 24 项全过 (calMsgLength 对称/CQ 容量 5.72MB/40B+20B 位宽等)

**负面空间**: 不单条删除 (文件级截断)/不二级索引 (仅 uniqKey)/不 CQ 压缩/不过滤下推/不多主写

### RM-4 延迟消息 — 等级队列 + 定时投递

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **等级表**: messageDelayLevel **18 级字符串可配置** (s/m/h/d 单位表) → delayLevelTable (ConcurrentSkipListMap) + maxDelayLevel; **越界钳制**; level-1=queueId | MessageStoreConfig:224; ScheduleMessageService:74,98-102,299-325; CommitLog:541-544 |
| **写路径**: **HookUtils.transformDelayLevelMessage** (L226-236 三步: 钳制+REAL 备份+topic 改写) → SCHEDULE_TOPIC_XXXX + queueId=level-1; **消费重试延迟**: delayLevel=3+reconsumeTimes (AbstractSendMessageProcessor L209-212); **tagsCode = storeTimestamp+delay 存到期绝对时间** (CQ 槽复用) | HookUtils.java:226-236; CommitLog.java:536-552 |
| **投递循环**: **maxDelayLevel 线程池 (每级一线程)** + TimerTask (1s 首启); 未到期 100ms 重查 / 到期投递 / 队尾 10s; **correctDeliverTimestamp 防永久等待 (代价: 时钟回拨提前投递)**; **batch 断言 (不支持批量)**; Ext 丢失重算; TRANS_HALF discard | ScheduleMessageService:134-156,387-393,401-485 |
| **还原**: messageTimeUp — 清延迟属性 (DELAY_TIME_LEVEL/TIMER_DELIVER_MS/TIMER_DELAY_SEC) + topic=REAL_TOPIC + queueId=REAL_QUEUE_ID + tagsCode 重算; **REAL_* 属性残留标注**; waitStoreMsgOK=false | ScheduleMessageService:340-380 |
| **投递+持久化**: syncDeliver 等待 / asyncDeliver (5.x: 流控 2000 + Blocked + **HandlePutResultTask 完成回调 SUCCESS/RUNNING/FAILED**); offset JSON 持久化 (初始 10s + flushDelayOffsetInterval) + load 双路径 (DLedger) + 版本计数 | ScheduleMessageService:117-127,494-570 |
| **配置/测试**: enableScheduleAsyncDeliver=false 默认; 常量 1s/100ms/10s/5s/10ms; 测试 3 用例 | MessageStoreConfig:255-256; ScheduleMessageServiceTest |

**时空溯源**: 3.x 初版 (SCHEDULE_TOPIC+TimerTask+tagsCode 到期) → 4.x 持久化+校正 → 5.0 async 投递+指标+版本计数

**深审 (15 处, 4 轮 REVIEW)**: 精确化 2 (写前改道归位 **HookUtils 三步** / correctDeliverTimestamp 防永久等待); 补锚 5 (线程模型每级一线程/重试延迟 3+n/batch 不支持/持久化定时链/HandlePutResultTask); 机制缺口 1 (**投递 at-least-once: updateOffset 窗口重复, 无去重**); 推理验证 24 项全过 (等级数 18/单位换算/回拨恒真推导/钳制数学等)

**负面空间**: 不秒级以下 (等级粒度)/不精度保证 (±100ms-10s 轮询)/不堆积告警/不等级热增 (重启生效)/不事务混合 (TRANS_HALF discard)/**不投递去重 (at-least-once)**/不支持批量延迟

### RM-5 Broker 启动 — 装配枢纽与生命周期

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **启动链**: main → createBrokerController (三配置装配, 失败 exit) → initialize → start (**disappearTimeAfterStart 默认 -1 禁用** 防风暴 + startBasicService + namesrv 10s 初/**10-60s 周期注册钳制**); 从库代主隔离 (isIsolated) | BrokerStartup:51-248; BrokerController:1705-1746 |
| **三阶段**: initializeMetadata (**6+1 configManager.load**) → initializeMessageStore (**双实现 Default/RocksDB + DLedger 角色注册 + 存储插件链 + CalcBitMap + TimerWheel**) → recoverAndInitService (**ReplicasManager fenced + 恢复序 store→schedule→插件 + 服务链**: 处理器/定时/**事务 SPI (TransactionalMessageBridge)**/**认证管线 (Authorization/AuthenticationPipeline, RM-13)**/TLS 热加载) | BrokerController:773-900,989-1064 |
| **处理器注册**: **46 处 = 20 码×双服务 + 6 码单注册 (POP 族仅主端口)**; **fastRemotingServer = listenPort-2 高频生产+消费管理通道** (20 码含心跳/查询/ACK/END_TRANSACTION, 非纯发送); 分组线程池 | BrokerController:478-486,1070-1151 |
| **定时任务**: 8 核心 (统计每日零点/offset+filter+order 持久化/**protectBroker=慢消费者自动禁用**/水位/积压/主从 syncAll/主从差) + 条件任务 (namesrv 刷新/controller) | BrokerController:608-770,1202-1220 |
| **5.x 新面**: ReplicasManager (controller fenced) / RocksDB+CQ 双写 / 存储插件+附件插件 / TimerWheel / **pop 三服务** (PopLongPolling/PopBufferMerge/QueueLockManager) / ackRevive / storeHost | BrokerController:478-486,787-819,855-870,1603-1650 |
| **关闭面**: **shutdownBasicService (unregister→双 remoting→metrics→housekeeping→pullRequestHold)** + scheduledFutures 取消 + 收尾持久化 + JVM shutdownHook; 测试: testBrokerRestart 重启幂等 / testHeadSlowTimeMills | BrokerController:1358-1400,1565-1640; BrokerStartup:224 |

**时空溯源**: 3.x 三阶段骨架定型 → 4.x DLedger/事务/ACL/fast 端口 → 5.0 ReplicasManager/RocksDB/插件/BrokerIdentity → 5.x TimerWheel/pop 三服务/REPLY

**深审 (14 处, 4 轮 REVIEW)**: 数字修正 2 (**46 注册 = 20×2+6, POP 族单注册** / configManager 6+1); 表述修正 1 (**fast=高频生产+消费管理通道 非纯发送**); 补锚 5 (startBasicService 序/事务 SPI/认证管线/shutdownBasicService 链/disappearTimeAfterStart 默认); 推理验证 24 项全过 (46 计数/fast 端口容错/namesrv 钳制 [10s,60s]/认证管线 pipe 序等)

**负面空间**: 不热配置全面化/不灰度启动/不深自检/不模块热插拔

### RM-6 消息过滤 — SQL92/TAG 双类型 + 布隆两级过滤

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **双类型**: SQL92 (ActiveMQ 风格完整语法: AND/OR/NOT/BETWEEN/IN/IS NULL/=TRUE) vs TAG (tag1\|\|tag2, null/* 全订阅); isTagType 默认 TAG; **类过滤模式两级放行 (遗留)**; codeSet HashSet 无大小限制 | common/filter/ExpressionType.java:37-53 |
| **解析求值**: JavaCC (SelectorParser.jj) → AST 家族 (ActiveMQ 移植: Logic/Comparison/UnaryIn/Property/**Now=当前毫秒**); evaluate = context.get; compiledExpression 注册期编译; **精筛异常 → 消息被滤 (激进一致)** | parser/SelectorParser.jj; expression/ 家族; ExpressionMessageFilter:150-165 |
| **布隆两级**: 参数数学 f→**k=ceil(log(0.5,f))**, n→**m=n×log2(1/f)×log2(e)** (8 对齐; 默认 20%/64 → 3 哈希/216 位); 双哈希 (murmur3 hash1+i×hash2, Kirsch-Mitzenmacher); **无假阴性: 未命中=确定不匹配**; CQ 位图粗筛 → CommitLog 精筛兜底 + **延迟属性解析** (粗筛通过才 decodeProperties — 省解析价值链) | BloomFilter.java:70-108; ExpressionMessageFilter.java:60-165 |
| **写时位图**: CalcBitMap 分发 (**enableCalcFilterBitMap 默认关**) — 落盘时逐 SQL92 订阅者 evaluate → hashTo 置位 → CQ Ext; **evaluate 异常=不置位=消息被滤 (激进语义)** | CommitLogDispatcherCalcBitMap.java:35-100; BrokerConfig:160 |
| **元数据**: bloomFilter 全局共享 (createByFn(20,64)) + bloomFilterData 每订阅者; ConsumerFilterData (compiledExpression transient/born/dead/clientVersion); **isMsgInLive = msgStoreTime > bornTime** (订阅前消息回退精筛); 位图失败回退 (null/位宽不符→放行) | ConsumerFilterManager.java:53-155; ConsumerFilterData.java:32-58 |
| **重试+测试**: ExpressionForRetryMessageFilter (RETRY 前缀 → realFilterData 精筛); 测试 7 文件 2129 行 (Parser 边界/表达式运算符/Bloom checkFalseHit/SPI/BitsArray + broker 端到端) | ExpressionForRetryMessageFilter.java:33-84 |

**时空溯源**: 3.x TAG+**FilterServer (5.x 废弃, 残留)** → 4.x SQL92+布隆两级 → 5.x FilterFactory SPI+生命周期

**深审 (14 处, 4 轮 REVIEW)**: 认知修正 1 (**FilterServer 已废弃** — 执行计划表述过时); 补锚 6 (isMsgInLive 精确语义/bloomFilter 共享+数据独立/**延迟属性解析**/精筛异常同样滤掉/NOW/codeSet 无界); 语义标注 1 (evaluate 异常→被滤); 推理验证 22 项全过 (布隆数学 3 哈希 216 位/无假阴性/误判成本 20% 省 80% 解析等)

**负面空间**: 不 FilterServer 独立进程 (执行计划过时)/不正则表达式/不属性索引/不联合过滤/不运行时表达式更新

### RM-7 Producer 发送 — 三模式 + 故障感知负载均衡

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **门面三模式**: send→SYNC / +Callback→ASYNC (**MQClientAPIImpl 回调层独立重试 2 次, 换 broker**) / sendOneway→ONEWAY (无重试); 配置: retryTimesWhenSendFailed=2 + **retryTimesWhenSendAsyncFailed=2 双配置** / **retryResponseCodes 默认 8 码** (TOPIC_NOT_EXIST/SERVICE_NOT_AVAILABLE/SYSTEM_ERROR/SYSTEM_BUSY/NO_PERMISSION/NO_BUYER_ID/NOT_IN_CURRENT_UNIT/GO_AWAY); **消息体压缩+cloneMessage 恢复 (ASYNC)**; hook 扩展 | DefaultMQProducer:76-139,134; MQClientAPIImpl:674-787 |
| **重试编排** (sendDefaultImpl): **SYNC 1+2 次 (sendDefaultImpl 层)**; **ASYNC 重试在 MQClientAPIImpl 回调层** (sendMessageAsync onException→换 broker 递归, 配置独立); ONEWAY 无重试; **ASYNC 故障度量=发送发起耗时 (异步本质)**; 异常三分类 — MQClientException (continue) / **RemotingException→隔离 10s** / **MQBrokerException→不可达+白名单判定**; 超时预算逐次扣减; resetIndex+lastBroker 防重复 | DefaultMQProducerImpl:733-880; MQClientAPIImpl:674-787 |
| **故障策略**: **7 档映射** latencyMax{50,100,550,1800,3000,5000,15000}ms → notAvailableDuration{0,0,2000,5000,6000,10000,30000}ms; **isolation 固定 10s** (恰好命中 5s-15s 档); **sendLatencyFaultEnable 默认 false**; 三级选择 (available→reachable→兜底); 关闭时 brokerFilter 排除上次; **双维度 (延迟+可达)** + 5.x 服务探测 | MQFaultStrategy:27-187; LatencyFaultToleranceImpl:36-126 |
| **路由与均衡**: namesrv 发现缓存 (tryToFindTopicPublishInfo); **ThreadLocalIndex 递增取模轮询** (线程独立); 重试重置索引 | TopicPublishInfo:75-112; DefaultMQProducerImpl:883-900 |
| **kernel + 批量**: 头构造 (group/topic/queue/bornHost/uniqId) + hook 链 + invoke 三模式; **ProduceAccumulator** (5.x: 延迟 1ms-30s/字节 1B-2MB 双条件; **AggregateKey 四维 topic/mq/tag/waitStoreMsgOK**); 重试属性面 | DefaultMQProducerImpl:900-1130; ProduceAccumulator:45-312 |
| **选择器+异步**: hash (顺序, RM-10)/机房间/随机; **callbackExecutor 可注入** (remoting 层); TransactionMQProducer/Request-Reply (5.x) | selector/ 3 文件; DefaultMQProducerImpl:1550 |

**时空溯源**: 3.x 三模式+轮询+重试 2 → 4.x 故障策略+选择器 → 5.x 批量累积+服务探测+请求响应

**深审 (13 处, 4 轮 REVIEW)**: **认知修正 1 (双层重试: sendDefaultImpl SYNC 1+2 + MQClientAPIImpl ASYNC 回调 2, 配置独立)**; 补锚 5 (默认 false/8 重试码/AggregateKey 四维/回调注入/重试属性面); 语义标注 1 (ASYNC 故障度量失真); 推理验证 22 项全过 (7 档映射数学/隔离 10s 档位命中/超时扣减等)

**负面空间**: 不无限重试/不严格负载均衡 (无权重)/不发送事务保证/不端到端确认 (SEND_OK=存储确认)/不压缩内建

### RM-8 消费-Push — 拉取调度 + 缓存 + 消费服务

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **拉取调度**: PullMessageService (阻塞队列) + 立即/延迟双入队; **MessageRequest 接口 (PullRequest=PULL/PopRequest=POP 双实现)**; 延迟分级 (异常 3s/缓存流控 50ms/broker 流控 20ms/**暂停 1s**) | PullMessageService:31-81; MessageRequest.java; PushConsumerImpl:101-113 |
| **长轮询**: sysFlag 四 bit (commitOffset/suspend/subscription/classFilter) + **broker 挂 15s/客户端等 30s**; pullBatchSize=32/consumeTimeout=15 分钟; **回调状态机: FOUND→ProcessQueue / NO_NEW_MSG→立即重拉 (长轮询即等待, 非 1s) / OFFSET_ILLEGAL→freeze offset+rebalance 修复** | PushConsumerImpl:300-480,380-430 |
| **ProcessQueue**: TreeMap 双缓冲 (msgTreeMap+有序子集); **流控三阈值** (span 2000/条数 1000/字节 100MiB — 拉取前置检查); commit/removeMessage 推进 | ProcessQueue:46-306; PushConsumer:176-217 |
| **消费服务**: Concurrently (线程池 min/max=20 可调 60s 回收 + batchMaxSize=1 + 失败 sendMessageBack, **超 maxReconsumeTimes=16→DLQ** + **cleanExpireMsg: 超时队首消息固定 delayLevel=3 送回重试** + 消费 hooks) / **Orderly (MessageQueueLock 队列锁 + 锁失败 10ms/3s 重试)** | ConcurrentlyService:57-322; ProcessQueue:75-128; PushConsumerImpl:645-794,887-894 |
| **进度双实现**: 集群 RemoteBrokerOffsetStore (persist→broker ConsumerOffsetManager, 重平衡共享) vs 广播 LocalFileOffsetStore (本地文件); **readOffset 三类型 (MEMORY/STORE/MEMORY_FIRST_THEN_STORE)**; 提交链 commit→update→persist (拉取后+定时+关闭) | store/RemoteBrokerOffsetStore:59-163; ReadOffsetType:23-31 |
| **5.x POP**: ConsumeMessagePopConcurrently/Orderly + **PopProcessQueue.ack()** (pop+ack 可见性窗口, 对照 Kafka); broker PopLongPollingService (RM-5); 测试 5345 行 (rebalance 6 算法素材) | Pop 族 975 行; client/src/test/consumer |

**时空溯源**: 3.x 骨架 (PullMessageService+ProcessQueue+长轮询) → 4.x 服务化+offsetStore+流控 → 5.0 POP+MessageRequest 重构

**深审 (15 处, 4 轮 REVIEW)**: **认知修正 1 (空拉取立即重拉非 1s; 长轮询挂起即等待; 1s 属暂停场景)**; 补锚 6 (maxReconsumeTimes 16→DLQ/pullBatchSize 32+consumeTimeout 15min/线程池可调/OFFSET_ILLEGAL 冻结修复面/cleanExpireMsg 固定 delayLevel=3/MessageRequest 双实现); 推理验证 22 项全过 (长轮询时序/重试上限链/流控三阈值等)

**负面空间**: 不服务端推送 (长轮询模拟)/不自动均衡参与 (RM-9)/不消费幂等 (at-least-once)/不本地持久化/不深度 backpressure

### RM-9 Rebalance+offset+LitePull — 队列再平衡与消费起点

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **再平衡调度**: **20s/1s 自适应** (balanced?20s:1s, 系统属性); 触发面 = 周期 + rebalanceImmediately (OFFSET_ILLEGAL 修复) + **心跳变更感知链** (HEART_BEAT 注册 → broker consumerTable → findConsumerIdList 感知, RM-5) | RebalanceService:22-40 |
| **双模式分配**: 客户端 (广播=全队列无分配 / 集群=**双排序 mqAll+cidAll (确定性前提)** + 算法) vs **broker 分配** (5.x queryAssignment, 3s×3 重试; **失败回退客户端模式**) | RebalanceImpl:237-345 |
| **差集处理**: 删 (drop+unlock+remove) / 增 (putIfAbsent + **computePullFromWhere 起点** + setLocked); messageQueueChanged 通知 | RebalanceImpl:311-370 |
| **6 分配算法**: **AVG** (均分, 前 mod 个多 1; startIndex 数学) / ByCircle / ConsistentHash (虚拟节点构造参数) / ByConfig / MachineRoom / Nearby (5.x) | rebalance/ 6 文件 413 行 |
| **消费起点 5 模式**: 有进度续读 / 无进度 (重试 topic 从 0, 普通 **maxOffset 尾部**) / FIRST→0 / TIMESTAMP 时间查询; OFFSET_ILLEGAL → 冻结修复 (RM-8) | RebalancePushImpl:166-230 |
| **有序锁**: broker 侧 **LOCK_BATCH_MQ 批量锁** (lockAll/unlockAll → setLocked); 再平衡锁随归属转移; 锁过期暂停消费 | RebalanceImpl:98-200 |
| **LitePull**: subscribe/assign **互斥** (SUBSCRIPTION_CONFLICT); poll 手动节奏 (对照 Kafka); AssignedMessageQueue + messageQueueLock; RebalanceLitePullImpl 共享核心 | LitePullImpl:93-219 |

**时空溯源**: 3.x RebalanceService+AVG+有序锁 → 4.x 算法族+起点模式 → 5.0 **broker 分配+LitePull+1s 收敛** → 5.x Nearby+冻结修复

**深审 (12 处, 4 轮 REVIEW)**: 补锚 6 (广播无分配/双排序确定性/broker 分配失败回退 graceful/心跳感知链/consumerId 感知面/AVG 数学); 推理验证 22 项全过 (AVG startIndex/双排序确定性/锁随归属/起点冻结闭环等)

**负面空间**: 不默认中心化分配 (对照 Kafka)/不粘性分配 (大迁移)/不迁移优化 (drop 丢缓冲)/不再平衡事件持久化/不分区级均衡

### RM-10 顺序+事务消息 — 队列绑定 + 半消息回查

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **顺序发送**: MessageQueueSelector 接口 + **SelectMessageQueueByHash = arg.hashCode() % size (负数 Math.abs)** — 业务键定队列; **sendSelectImpl 单次发送 — SYNC 无重试** (失败即抛保序); **ASYNC 回调层重试同 broker 同队列** (topicPublishInfo=null → retryBrokerName=brokerName, request 复用 queueId); MachineRoom 选择器**返回 null 桩** | SelectMessageQueueByHash:28-31; DefaultMQProducerImpl:1314-1353; MQClientAPIImpl:778-782 |
| **顺序消费协同**: 队列锁跨客户端 (broker LOCK_BATCH_MQ + 客户端 MessageQueueLock, RM-8/9) + **再平衡 isOrder 立即锁** (RebalanceImpl:521); **起点无 isOrder 特殊分支 (5.3.1)** — "顺序从 0 读"旧说法过时 | RebalanceImpl:521; RebalancePushImpl:166-248 |
| **半消息写路径**: 客户端置 TRAN_MSG=true + PGROUP (DefaultMQProducerImpl:1434-1435) → SendMessageProcessor 判定 (4.6.1 兼容: 重试+延迟不当 prepare; rejectTransactionMessage 拒收 NO_PERMISSION) → **parseHalfMessageInner 改道**: UNIQ_KEY→__transactionId__ + REAL_TOPIC/REAL_QID 备份 + sysFlag 清 NOT_TYPE + **HALF topic queueId=0**; 半/OP topic 各 1 队列; 事务消息不支持延迟 (清 DelayTimeLevel) | SendMessageProcessor:304-318; TransactionalMessageBridge:219-233; TopicConfigManager:195-215 |
| **本地事务+END**: SEND_OK 才执行本地事务 (异常→localException); **FLUSH_* / SLAVE_NOT_AVAILABLE → 强制 ROLLBACK** (半消息可能已落盘); endTransaction → END_TRANSACTION(37) **oneway** (失败仅 log — 不回滚本地已提交业务); **EndTransactionProcessor**: SLAVE 拒收 → 三校验 (PGROUP/tranStateTableOffset/commitLogOffset) → 超免疫期 ILLEGAL_OPERATION(604) → commit: endMessageTransaction 还原真实 topic/qid + tagsCode 重算 + **PreparedTransactionOffset=半消息物理 offset** → 重投 (PUT_OK/FLUSH_*/SLAVE 均算 SUCCESS) → deletePrepareMessage 写 OP (tag=d, body=offset 列表) / rollback: 直接写 OP | DefaultMQProducerImpl:1418-1548; EndTransactionProcessor:64-68,199-273,275-348 |
| **回查对账**: TransactionalMessageCheckService **30s 周期** → check (6s 免疫/15 上限): OP 队列对账 (removeMap, OP_MSG_PULL_NUMS=32) → **needDiscard ≥15 → TRANS_CHECK_MAX_TIME_TOPIC 弃置** (checkTimes 免疫期也递增 — 长免疫 >480s 未查即弃 ⚠) → needSkip (born>72h) → 免疫期 max(自定义×1000, 6s) → **isNeedCheck 三条件** (无 OP 超免疫 / OP 老化 born-startTime>6s / **时钟回拨 born 未来**) → 半消息重写回队列 (TRAN_PREPARED_QUEUE_OFFSET 闭环) → CHECK_TRANSACTION_STATE(39) 发客户端 → checkExecutor (默认 1 线程/2000 队列) → checkLocalTransaction → endTransactionOneway (fromTransactionCheck=true, 固定 3s 超时); 单队列 60s 处理上限 | TransactionalMessageServiceImpl:161-354,379-473; AbstractTransactionalMessageCheckListener:51-70; DefaultMQProducerImpl:361-449 |
| **5.x 新面**: OP 批量 (deleteContext 攒批, TransactionalOpBatchService **3s/4096B**) / 事务指标 (TransactionMetrics + 3s Flush) / **事务 SPI** (ServiceLoader 可替换, BrokerController:989-1006) / **从库代主 EscapeBridge** (半消息还原转发, 退避 `100*(2^cnt)` — **XOR 非幂** ⚠) / 4.x TransactionCheckListener 废弃 | TransactionalMessageServiceImpl:596-752; TransactionalOpBatchService:46-64; BrokerController:989-1006; EscapeBridge:95-116 |

**时空溯源**: 3.x 事务骨架 (半+OP topic/回查/TransactionCheckListener) → 4.6.1 TransactionListener 新 API → 5.0 事务 SPI 化+旧 API 废弃 → 5.x OP 批量+指标+从库代主; 顺序面 3.x 选择器+锁定型至今

**深审 (15 处, 5 轮 REVIEW)**: 认知修正 2 (**MachineRoom 选择器=null 桩** / **顺序起点无 isOrder 分支**); 机制缺口 2 (**免疫时间双实现不一致** — 回查侧不钳制 vs END 侧钳制 ≥6s / checkTimes 免疫期递增 → 长免疫未查即弃); 语义标注 4 (FLUSH_* 强制回滚/escape 退避 XOR 非幂/END 双超时/**END-回查竞态双投递窗口**); 表述修正 1 (**三校验防伪造防串不防重复** — EndTransactionProcessor 无 op 查询); 补锚 4 (SYNC 无重试+ASYNC 同 broker/单队列 60s/双端 executor 策略不对称/**半队列膨胀**); 推理验证 21 项全过 (hash 无溢出/对账闭环/重写链单条单轮/竞态推导/终止性/计数数学等)

**负面空间**: 不全局序 (仅队列内)/不选择器重试 (SYNC 失败即抛)/不协议级 2PC (最大努力最终一致)/不本地补偿/不回查去重 (at-least-once)/不幂等存储 (业务 uniqKey)/MachineRoom 选择器桩

### RM-11 Namesrv 路由 — 无状态注册中心与路由发现

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **六表存储** (PLAN"三表"表述修正 — 三表是协议结构): topicQueueTable (topic→brokerName→QueueData) / brokerAddrTable (brokerName→BrokerData: brokerId→addr) / clusterAddrTable (cluster→brokerName 集) / brokerLiveTable (BrokerAddrInfo→BrokerLiveInfo: 心跳+DataVersion+Channel) / filterServerTable (遗留) / **topicQueueMappingInfoTable (5.x 静态 topic)**; ReentrantReadWriteLock 读写分离; **无持久化 — 重启靠 broker 10s 初/10-60s 重注册重建** | RouteInfoManager:71-90,1125-1195; NamesrvConfig |
| **注册**: REGISTER_BROKER=103 — crc32 校验 + V3_0_11 版本分支; 六表 upsert + **同 IP:PORT 去重** (主从切换) + **stateVersion 仲裁** (旧>新拒注册, 防僵尸复活顶掉新主) + registerFirst/DataVersion 驱动 topic 增量; **isPrimeSlave+enableActingMaster → 擦写权限**; 心跳超时随请求头 (默认 2min); 从库注册返回 master 地址 | DefaultRequestProcessor:223-281; RouteInfoManager:226-409 |
| **注销**: 三入口 (显式 UNREGISTER=104 / 通道三事件 / 心跳超时) → **BatchUnregistrationService 异步批量** (3000 队列, take+drainTo 合并) → 六表级联清理 (删 addr→空删 name→空删 cluster→topic 表全扫+无主擦权); 队列满→显式注销报错, 事件注销静默→**5s 扫描兜底** | BatchUnregistrationService:61-76; RouteInfoManager:571-698 |
| **路由查询**: GET_ROUTEINFO_BY_TOPIC=105 **独立线程池 (8/50000 隔离)** + 45s 就绪门禁 (needWaitForService 默认 false; 门禁失效双路径: 时间短路+命中 disable); 快照 + **BrokerData clone 防篡改** + filterServer/静态映射附加; **acting master 伪装** (无主+enableActingMaster → 最小 brokerId 以 MASTER_ID 返回); ZoneRouteRPCHook 后置 zone 过滤 (master down 保留全从库); V4_9_4+ 标准 JSON | ClientRequestProcessor:65-108; RouteInfoManager:700-801; ZoneRouteRPCHook:43-95 |
| **心跳与清理**: **三来源** (BROKER_HEARTBEAT=904 轻量 / QUERY_DATA_VERSION=322 兼心跳 / 注册 upsert); scanNotActiveBroker **5s 周期**: lastUpdate+2min(默认, 可覆盖) < now → closeChannel+注销; **余量**: broker 10-60s 心跳 vs 2min — 容 2-12 次丢失; 无锁遍历弱一致 | RouteInfoManager:803-818; NamesrvController:116-118 |
| **5.x 新面**: Controller 内嵌 (enableControllerInNamesrv + CONTROLLER_REGISTER_BROKER=1003) / **acting master 全链四步** (注册擦权→注销擦权→查询伪装→minId oneway 通知 — **通知在写锁内发起** ⚠) / 写权限管理面 (WIPE=205/ADD=206, ADD 强制 READ\|WRITE) / zone 过滤 / 批注销 / 顺序消息 KV 附加 | NamesrvStartup:182-219; RouteInfoManager:343-347,676-681,787-789,914-953; operateWritePermOfBroker:532-555 |

**时空溯源**: 3.x 四表骨架+注册+2min 超时+5s 扫描 → 4.x KV 配置+filterServer+901 → 5.0 静态映射表+Controller 内嵌 → 5.x acting master 全链+zone+批注销+904

**深审 (16 处, 5 轮 REVIEW)**: 认知修正 1 (**PLAN"三表"实为六表** — filterServer 遗留+静态映射表); 表述精确化 2 (45s 门禁默认关+失效双路径 / 门禁单一路径不全); 语义标注 5 (**minId 通知在写锁内 oneway** / 批注销满失败兜底 / **时间戳非 volatile 可见性窗口** / **WIPE/ADD 一次性权限** / **stateVersion 冲突静默**); 补锚 5 (心跳三来源/acting 全链四步/写权限管理面/2-12 次心跳余量/**锁使用不一致**); 推理验证 21 项全过 (六表闭环/注册幂等/stateVersion 仲裁/地址去重/prime slave/drainTo 合并/心跳判定/clone 防篡改/可见性窗口/WIPE 生命周期等)

**负面空间**: 不路由持久化 (无状态=特性)/不节点共识 (多 namesrv 独立, 客户端容错 — 对照 ZK/ETCD)/不主动推送/不 broker 探活 (仅心跳被动)/不存消息进度/zone 过滤后置/acting master 默认关

### RM-12 HA/DLedger+Controller — 复制水位与自动故障转移

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **复制双水位** (RM-2 组提交闭环引用): masterPutWhere (主写) > push2SlaveMaxOffset (主推, CAS 推进) > slaveAckOffset (从确认, volatile); **组提交等待语义三档**: ackNums<=1 → 退化等主推水位 / 经典 → ack 计数 (含自身) / AutoSwitch ALL_ACK → syncStateSet 全副本; ⚠ **多连接重复计数缺陷** (GroupTransferService:127 TODO 自认); isSlaveOK/inSync 判定 = 落后 < **haMaxGapNotInSync=256MB** | DefaultHAService:56,98-117,184-200; DefaultHAConnection:58,230-236; GroupTransferService:103-132 |
| **主从同步**: 从库 DefaultHAClient 状态机 (READY 连主 5s 重试 → TRANSFER) **主动 connect + 报 offset 驱动, 主库推** ("主推从拉"精确语义); 主 AcceptSocketService (NIO, haListenPort=**10912**) → 每连接双线程; 首连 0 → **1GB 段起点补全**; **offset 强校验=对齐检测** (主推 offset != 自身 maxPhyOffset 即断); 12B 头 [offset+size]; 32KB 批量 + 字节流控; 5s 心跳 / **20s housekeeping** | DefaultHAClient:251-370; DefaultHAConnection:211-384; DefaultHAService:259-379 |
| **5.x AutoSwitchHA**: **EpochFileCache (epoch,startOffset) 数据合法区间**; 三截断 (truncateInvalidMsg **RocksDB 辅助** / truncateSuffixByEpoch 截旧主未确认 / truncateEpochFilePrefix); **confirmOffset** (storeCheckpoint 持久化) 切换恢复点; 协议头扩展 20/28/12B; 从库 caught-up 判定 | AutoSwitchHAService:83-141,199-209,493+; AutoSwitchHAConnection:56-72; DefaultMessageStore:386 |
| **DLedger 模式**: DLedgerCommitLog — 写路径走 Raft 多数派落盘替代主从推送; **DLedgerRoleChangeHandler** (RoleChangeHandler 回调 changeToMaster(SYNC_MASTER)/changeToSlave); 与 Controller 是"内嵌选举 vs 外置选举"演进对照 | DLedgerRoleChangeHandler:36,68-91; RequestCode:290 (1018) |
| **Controller 模式**: ControllerManager 双实现 (**JRaftController 默认 / DLedgerController**) + StateMachine apply 链 → ReplicasInfoManager (689); **electMaster**: 首选首个注册者/旧主存活拒绝/**designateElect 强制指定**/失败 MASTER_NOT_AVAILABLE; 策略 = syncStateSet 内 **maxOffset 降序+priority 升序**, 重试 **3** 次; **brokerId 分配 registerCheckCode 防伪** (1012/1013); **broker 心跳 1s (brokerHeartbeatInterval) + 10s 超时 (controllerHeartBeatTimeoutMills, 容 10 次)**; DefaultBrokerHeartbeatManager 2s 初/5s 扫描 (扫描≠心跳); 超时 → **makeFenced** (setIsolated+runningFlags 双层); **fenced 生命周期**: 启动 true→RUNNING 解封→超时再 true; **选举触发双轨** (5s 扫 + broker 主动) | ReplicasInfoManager:193-274,290-325; DefaultBrokerHeartbeatManager:59-90; ControllerManager:184-195; ReplicasManager:205,878-880; BrokerConfig:196,348 |
| **broker 侧 ReplicasManager + 切换链**: 状态机 + **epoch 递增守卫** (newMasterEpoch > masterEpoch 才生效); changeToMaster (追平 handleSlaveSynchronize → haService.changeToMaster → brokerId=0 + SYNC_MASTER → dataVersion.nextVersion(epoch) → 重注册); changeToSlave 对称; **NotifyService epoch 单调覆盖** (cancel 旧 future); 触发=心跳超时 (namesrv+controller 双判) → 选举 → 通知 → 追平/截断 → 重注册 → 路由感知; **三层防脑裂** (epoch/stateVersion/syncStateSet) | ReplicasManager:229-300,378-420; ControllerManager:309-348 |

**时空溯源**: 3.x 经典主从 (NIO acceptor + 双线程连接 + 组提交) → 4.x DLedger (Raft 接入) → 5.0 Controller (独立元数据 Raft + fenced + syncStateSet) → 5.x AutoSwitch (epoch 文件 + confirmOffset + RocksDB) + ReplicasManager 状态机 + electionPriority

**深审 (15 处, 5 轮 REVIEW)**: 表述精确化 3 (**"主推从拉"语义: 从库控制、主库推** / **组提交等待语义三档**: ackNums<=1 退化等主推水位/经典 ack 计数/AutoSwitch syncStateSet 全副本 / offset 强校验=对齐检测); 语义标注 4 (**多连接重复计数缺陷** — GroupTransferService:127 TODO 自认 / AutoSwitch RocksDB 依赖 / **冷启动边界** — 主库过期删除后新从库无法补同步 / DLedger 演进); 数字修正 1 (**controller 心跳 1s+10s 超时** — 非 2s/5s 扫描); 补锚 5 (首连 1GB 段起点/NotifyService epoch 覆盖/选举触发双轨/**designateElect**/**registerCheckCode**); 推理验证 21 项全过 (双水位数学/三档收敛/1GB 对齐/强校验闭环/epoch 截断/选举策略/fenced 生命周期/冷启动边界等)

**负面空间**: 经典模式不自动故障转移 (人工介入)/不跨机房同步/不消息级复制确认 (水位级)/不从库读均衡/不 RPO=0 (异步复制窗口)/N 副本写放大 (Raft)/无 gossip (静态分组)

### RM-13 Proxy+安全 — gRPC 协议层与双协议翻译

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **拓扑双模式**: ProxyMode (LOCAL 内嵌 BrokerController — 同进程直写, 生命周期耦合 / CLUSTER 独立网关 — 内嵌客户端远程转发); **三端口**: grpcServerPort=**8081** / remotingListenPort=**8080** / metrics **5557**; 会话/上下文过期 60s/30s; grpc 线程池 16+2N/队列 100000/**入站 130MB**/消息 4MB | ProxyStartup:180-213; ProxyConfig:89,101,226,240 |
| **gRPC 层**: GrpcMessagingApplication (MessagingServiceImplBase v2 全服务) + **RequestPipeline pipe 反转** (声明 Authz→Authn→ContextInit, 执行 ContextInit→Authn→Authz) + authConfig 驱动 (null 跳过认证) + 四线程池分流 (producer/consumer/clientManager/transaction) 满则 TOO_MANY_REQUESTS | GrpcMessagingApplication:145-175; RequestPipeline:28-33 |
| **双协议翻译**: **协商在 8080 remoting server** (MultiProtocolRemotingServer + enableRemotingLocalProxyGrpc): 'PRI ' magic 判定 HTTP/2 → **本地转发 LOCAL_HOST:8081** (单端口暴露方案, PRI 前缀碰撞风险代码自认 L49-50); 否则 **remoting 恒真兜底**; **gRPC 8081 = 独立 io.grpc NettyServerBuilder** (双端口各自 TLS: 8080 HandshakeHandler / 8081 ProxyAndTlsProtocolNegotiator); RemotingProtocolServer 请求码分发 (SEND/PULL/LITE_PULL/POP/HEART_BEAT/END_TRANSACTION → activity 独立线程池); RemotingConverter 翻译 — **非透明字节转发** | MultiProtocolRemotingServer:80-88; RemotingProtocolHandler:47-59; Http2ProtocolProxyHandler:49-50,84-90,114; GrpcServerBuilder:49-53; RemotingProtocolServer:191-217 |
| **统一消息面**: MessagingProcessor (send/pop/ack/pull/offset/endTx/query) 双协议收敛点; **LocalMessageService 直调 broker processor** (同进程零网络); **ClusterMessageService 远程转发 + CLUSTER 双段认证** (客户端→proxy 签名 + proxy→broker 内嵌 SessionCredentials, createForClusterMode L126); proxy 代选队列 (MessageQueueSelector 314) | DefaultMessagingProcessor:110-136,158-230; LocalMessageService:80-115 |
| **认证一句话**: PLAN "SignAuthentication" 类**不存在** — 实为 **HmacSHA1 签名 (acl AclSigner:30 DEFAULT_ALGORITHM) + 资源授权两段式**; DefaultAuthenticationHandler calSignature 比对 (L64-66); 结果 username 写入 **AUTHORIZATION_AK header 透传**; Stateful/Stateless 双策略; **AuthMigrator acl→auth 迁移**; broker 侧同款管线 (RM-5) | AuthenticationPipeline:47-76; DefaultAuthenticationHandler:64-66; AclSigner:28-39; AuthMigrator |
| **客户端面**: 5.x gRPC 客户端直连 8081; 重试 attempts=**3** + backoff ×**2** + 长轮询批量 **32** + mqClientNum=**6**; **proxy 无状态** (消费状态在 broker) → 水平扩展; 流控 (429) → 客户端退避重试闭环 | ProxyConfig:127-139; GrpcMessagingApplication:161 |

**时空溯源**: 5.0 proxy 初版 (gRPC v2 + remoting 兼容 + 双模式) → 5.1+ auth 模块独立 (chain + 双策略) → 5.x AuthMigrator 迁移 + http2proxy + POP 语义统一

**深审 (15 处, 5 轮 REVIEW)**: 认知修正 1 (**SignAuthentication 类不存在 — HmacSHA1 签名+资源授权两段式**); 表述精确化 2 (**"转发链"实为语义收敛** / **协商位置: 8080 MultiProtocolRemotingServer + enableRemotingLocalProxyGrpc, gRPC 8081 独立 io.grpc**); 补锚 8 (pipe 反转/authConfig 驱动/认证透传 header/恒真兜底/会话过期 60/30s/CLUSTER 双段认证/LOCAL 生命周期耦合/**HTTP/2 本地转发 LOCAL_HOST:8081**); 语义标注 1 (**PRI 4 字节前缀碰撞风险代码自认**); 推理验证 21 项全过 (pipe 反转推导/双协议收敛/HmacSHA1 对称/无状态网关/双段认证/流控闭环/PRI 判定/本地转发闭环等)

**负面空间**: 不透明代理 (翻译非字节转发)/不 proxy 集群状态 (LOCAL 挂一起挂)/不 gRPC 深度流控 (130MB 粗粒度)/不默认加密 (TLS 显式配置)/不默认认证 (authConfig 驱动)/不替代 namesrv

---

## §二 方法论执行报告 (13 域实证, V14 收官版)

### 1. 域级怀疑审计修正汇总 (执行计划 10 域 → 13 域)

| 修正 | 证据 | 落点 |
|:--|:--|:--|
| **域清单 10 → 13** (09 审计 v1): +remoting 协议层 (28217 行定义特征) / +消费面拆 2 (consumer 14212 行远超单域; "PullMessageService 属 LitePull" 概念错误 — 实为 Push 内部长轮询) / +Proxy (20375 行 gRPC) | 顶层模块扫描 + 行数穷举 | ROCKETMQ-PLAN.md v1 |
| **定级修正 16 → 13** (v3, 用户标准: 面试+业务接触面): 安全面 (auth 7232) 并入 RM-13 / Controller (6347) 并入 RM-12 / TieredStore (7378) 🟡 C 按需简略 | 定义特征级 ≠ 必须展开 (04 方案决策) | PLAN v3/v3.1 |
| **FilterServer 已废弃** (RM-6): 执行计划 RM-4 "FilterServer" 表述过时 — 仅 BrokerOuterAPI FilterServerList 残留, 5.x broker 内嵌过滤 | grep 实证 | RM-6 速查 |
| **延迟消息 5.x 位置**: store/schedule → **broker/schedule**; 18 级可配置非硬编码 | ScheduleMessageService 模块位置 | PLAN v1 修正 |
| **"18 级" 精确化**: messageDelayLevel 字符串可配置 (s/m/h/d 单位表), 越界钳制 | MessageStoreConfig:224 | RM-4 |
| **hashSlotSize=4 语义**: 桶内槽常数 (非配置), maxHashSlotNum 默认 500 万 | IndexFile.java:32 | RM-3 |
| **MachineRoom 选择器=null 桩** (RM-10): select() 直接返回 null — 执行计划"机房间选择器"需修正 | SelectMessageQueueByMachineRoom:29-31 | RM-10 速查 |
| **顺序消息起点无特殊分支** (RM-10): "顺序从 0 读"旧说法对 5.3.1 不成立 — computePullFromWhere 无 isOrder 判断 | RebalancePushImpl:166-248 | RM-10 速查 |
| **"三表"实为六表** (RM-11): PLAN "RouteInfoManager 三表" 是协议结构 (TopicRouteData 返回面); 存储面六表 — +filterServerTable (遗留) +topicQueueMappingInfoTable (5.x 静态 topic) | RouteInfoManager:72-77 | RM-11 速查 |
| **"主推从拉"语义** (RM-12): 连接与节奏控制 (报 offset) 在从库, 数据流动是主库主动推 — "拉取模式"是笼统表述 | DefaultHAClient:251-270 + DefaultHAConnection:274-384 | RM-12 速查 |
| **"SignAuthentication" 类不存在** (RM-13): PLAN 双认证表述过时 — 实为 **HmacSHA1 签名 (acl AclSigner) + 资源授权 (auth 模块) 两段式**; "转发链"实为语义收敛 (双协议→统一消息面) | AclSigner:30 + DefaultAuthenticationHandler:64-66 | RM-13 速查 |

### 2. 发现问题类型统计 (深审+多次 REVIEW, 累计 197 处)

| 类型 | 数量 | 代表 |
|:--:|:--:|:--|
| **认知修正** | 11 | rpc/ 层=RemotingClient 抽象非 gRPC (RM-1) / 空拉取立即重拉非 1s (RM-8) / Ext=配置开启非溢出 (RM-3) / getMessageAsync 同步包装 (RM-3) / FilterServer 废弃 (RM-6) / 双层重试 (RM-7) / **MachineRoom 选择器=null 桩 (RM-10)** / **顺序起点无 isOrder 分支 (RM-10)** / **PLAN"三表"实为六表 (RM-11)** / **"主推从拉"语义 (RM-12)** / **SignAuthentication 类不存在 — HmacSHA1+授权两段 (RM-13)** |
| **机制缺口** | 4 | 双组提交 (刷盘+复制独立水位, RM-2) / 投递 at-least-once 无去重 (RM-4) / **免疫时间双实现不一致 (RM-10)** / **checkTimes 免疫期递增→长免疫未查即弃 (RM-10)** |
| 数字修正 | 5 | 46 注册 = 20×2+6 / configManager 6+1 / 48→46 / 7→6 管理器 / 18→可配置 |
| 归位修正 | 2 | 磁盘保护在 store 非 broker (RM-2) / 写前改道在 HookUtils (RM-4) |
| 表述精确化 | 21+ | markProtocolType 高 8 位类型 / suspended 双路径 / 恢复双路径 / 10s 强刷=leastPages 降 0 / oneway 双端语义 / **fast=高频通道非纯发送 (RM-5)** / resetIndex 防同 broker / **三校验防伪造防串不防重复 (RM-10)** / **45s 门禁默认关+失效双路径 (RM-11)** / **offset 强校验=对齐检测 (RM-12)** / **组提交等待语义三档 (RM-12)** |
| 补充锚点 | 60+ | 每域 4-7 处 (默认值/边界/交叉面); RM-12: 首连 1GB 段起点/NotifyService epoch 覆盖/选举触发双轨/designateElect/registerCheckCode |
| 语义标注 | 19+ | evaluate 异常→被滤 / ASYNC 故障度量失真 / REAL_* 残留 / **FLUSH_* 强制回滚 (RM-10)** / **escape 退避 XOR 非幂 (RM-10)** / END 双超时 (RM-10) / **END-回查竞态双投递 (RM-10)** / **minId 通知在写锁内 (RM-11)** / 批注销满兜底 (RM-11) / **时间戳非 volatile 可见性 (RM-11)** / **WIPE/ADD 一次性权限 (RM-11)** / **stateVersion 冲突静默 (RM-11)** / **AutoSwitch RocksDB 依赖 (RM-12)** / **多连接重复计数缺陷 (RM-12)** / **冷启动边界 (RM-12)** / **PRI magic 前缀碰撞风险 (RM-13)** |

### 3. 行号验证数: 累计 ~521 处 (每域 35-45 处穷举 grep)

### 4. 方法论铁律 (V14 收官版, 13 域实证强化)

1. **数字必须穷举** — 46 注册 (非 48)/197 发现/160 码/6 算法/7 档映射 — 全部 grep/python 提取
2. **执行计划是待验证假设** — 10→13 域 (执行计划错误率 30%); 4.x 认知对 5.3.1 源码大量过时 (FilterServer/位置/18 级)
3. **用户标准驱动定级** — 面试频度×业务接触面 + 技术同源度 + 展开 ROI (v3.1 固化三标准)
4. **认知修正优先于写作** — 每域 1-2 个旧认知被源码推翻 (rpc 层/空拉取/Ext/双层重试)
5. **前向依赖标注** — RM-11 等未交付域仅"交叉"引用不展开
6. **REVIEW 追加** — 每域 4 轮 REVIEW (深审+二次+三次+四次), 推理验证每轮 6-8 项
7. **harness 取舍记录** — Java 生态官方测试覆盖充足, 🔴 域 harness 延迟 (记录于 §零注)
8. **HANDOFF 版本化** — 每域完成后 V+N 升级 (V1→V14)

---

## §三 高频坑汇总 (跨域 55 条)

### RM-1 (8)
1. **头长高字节是序列化类型** — 非"高 24 位头长"; 头长实际低 24 位 (16MB 上限)
2. **flag 只有 2 位** — RPC_TYPE/ONEWAY; suspended 是独立字段
3. **Oneway 是双端无响应** — 服务端 writeResponse 直接返回
4. **每处理器独立线程池** — 拉取慢不拖垮心跳
5. **二进制序列化头无 suspended** — JSON 路径才有
6. **超时分级关闭** — left>100ms 或 >timeout/4 才关; 取通道耗时计入预算
7. **rpc/ 层不是 gRPC** — RemotingClient 高层抽象; gRPC 在 proxy 模块; 翻译层 RemotingProtocolHandler
8. **namesrv 多地址轮询** — shuffle + index 递增

### RM-2 (9)
9. **写锁默认自旋** — isUseReentrantLockWhenPutMessage 默认 false
10. **双缓冲三阶段别混** — append→commit (仅池启用)→flush
11. **组提交是双链表 swap** — 写侧锁读侧无锁; flushedWhere 水位
12. **warm = 写 0 + mlock** — 预热是逐页写触发缺页 + 锁页
13. **同步刷盘 5s 兜底** — syncFlushTimeout=5000
14. **1GB 段取模对齐** — createOffset = startOffset - startOffset%1GB
15. **同步复制是双组提交** — 刷盘+复制独立水位, ackNums=inSyncReplicas
16. **磁盘保护在 store 模块** — warning→diskFull 拒写 / cleanForcibly→强制清理
17. **"10s 强刷" = leastPages 降 0** — 每 10s 阈值归零全刷

### RM-3 (8)
18. **恢复期分发不是全跳过** — 正常不重放 (CQ 已持久), 异常重放重建
19. **ConsumeQueueExt 是配置开启** — enableConsumeQueueExt, 非 tagsCode 溢出
20. **CRC32 段 = 属性校验和** — enabledAppendPropCRC 4B
21. **消息 18 段字段序** — PHYSICALOFFSET 先占位后回填; V6 标志 8/20B
22. **20B 单元 + 40B 头 + 20B 索引项** — CQ/IndexHeader/IndexItem 位宽
23. **CQ 文件 5.7MB** — 300000×20B; Index 槽表 20MB
24. **getMessageAsync 不是异步** — completedFuture; 长轮询在 broker PullRequestHoldService
25. **恢复先 CQ 后 CommitLog** — CQ 定界 → CommitLog 截断

### RM-4 (7)
26. **延迟消息到期时间存 CQ tagsCode 槽** — storeTimestamp+delay
27. **延迟投递是轮询** — 1s 首启/100ms 重查/10s 队尾; 精度 ±级
28. **等级即队列** — 18 级 = 18 队列 = 18 线程; 可配置但重启生效
29. **写前改道在 HookUtils** — transformDelayLevelMessage 三步
30. **延迟投递 at-least-once** — updateOffset 窗口重复; 时钟回拨提前投递; 批量不支持
31. **消费重试也走延迟** — delayLevel=3+reconsumeTimes
32. **correctDeliverTimestamp 防永久等待** — 超远到期时间立即投递

### RM-5 (5)
33. **46 注册不是 48** — 20 码×双服务 + 6 码单注册 (POP 族)
34. **protectBroker = 慢消费者自动禁用** — fallBehind 超阈值 → disableConsume
35. **namesrv 注册周期钳制 [10s,60s]** — shouldStartTime 防启动风暴
36. **fast 端口不是纯发送** — 20 码含心跳/查询/ACK/END_TRANSACTION
37. **事务服务是 SPI + 认证管线** — ServiceProvider.loadClass; Authorization/Authentication

### RM-6 (5)
38. **FilterServer 已废弃** — 执行计划表述过时
39. **布隆无假阴性** — 未命中=确定不匹配; f=20%/n=64 → 3 哈希 216 位
40. **isMsgInLive = 订阅注册后** — msgStoreTime > bornTime
41. **布隆存在的原因是省解析** — 粗筛通过才 decodeProperties
42. **精筛异常也是滤掉** — 激进一致; NOW()=当前毫秒; codeSet 无界

### RM-7 (4)
43. **重试是双层的** — sendDefaultImpl (SYNC 1+2) + MQClientAPIImpl (ASYNC 回调 2); ONEWAY 无重试
44. **ASYNC 故障度量失真** — 发起耗时非完成耗时; 压缩体发送后 prevBody 恢复
45. **批量累积 4 维分组** — topic/mq/tag/waitStoreMsgOK; 延迟 30s 上限内吞吐换时延

### RM-8 (5)
46. **"Push" 是伪推送** — 客户端长轮询 (broker 挂 15s/等 30s)
47. **重试上限 16 → DLQ** — pullBatchSize=32 拉取 vs batchMaxSize=1 消费 (两层)
48. **进度双实现** — 集群存 broker (共享), 广播存本地文件; 线程池 20 可调
49. **空拉取是立即重拉** — NO_NEW_MSG → 立即; 1s 是暂停消费者场景
50. **OFFSET_ILLEGAL 冻结修复** — freeze→persist→removeProcessQueue→rebalanceImmediately

### RM-9 (5)
51. **再平衡 20s/1s 自适应** — 心跳变更感知链
52. **广播无分配** — 全队列全量; 集群双排序 (确定性前提); broker 分配失败回退客户端
53. **AVG 余数处理** — 前 mod 个消费者多分 1
54. **有序锁在 broker** — LOCK_BATCH_MQ 批量锁
55. **起点默认尾部** — maxOffset; 重试 topic 从 0

### RM-10 (7)
56. **顺序≠全局序** — 同队列内严格有序; hash 取模定队列
57. **顺序 SYNC 不重试** — sendSelectImpl 单次发送, 失败即抛; ASYNC 重试同 broker (topicPublishInfo=null)
58. **MachineRoom 选择器是桩** — select() 返回 null
59. **半消息改道** — TRAN_MSG=true → HALF topic queueId=0 + REAL_TOPIC/QID 备份; 事务不支持延迟
60. **END 是 oneway** — 失败不回滚本地已提交业务; FLUSH_* 状态强制 ROLLBACK
61. **回查 = 30s 对账** — 6s 免疫期 + 15 次弃置 (checkTimes 免疫期也递增); at-least-once
62. **免疫时间双实现不一致** — 回查侧不钳制 vs END 侧钳制 ≥6s; 从库代主退避 2^cnt 是 XOR 非幂

### RM-11 (6)
63. **路由是六表不是三表** — 三表是协议结构; +filterServerTable (遗留) +topicQueueMappingInfoTable (5.x)
64. **注册≠心跳** — 103 带配置 (crc32+版本分支); 904 仅时间戳; 322 兼心跳; 注册也 upsert
65. **心跳超时 2min + 5s 扫描** — 容 2-12 次心跳丢失; 无锁遍历弱一致
66. **注销三入口 → 批注销队列** — 显式/通道事件/超时扫描; 满→显式报错, 事件静默, 5s 扫描兜底
67. **45s 就绪门禁默认关** — needWaitForService=false; 门禁失效双路径 (时间短路+命中 disable)
68. **acting master 是完整链** — 注册擦权→注销擦权→查询伪装→minId 通知 (通知在写锁内 oneway); 默认双 false

### RM-12 (6)
69. **主推从拉精确语义** — 从库主动连接+报 offset 控制节奏; 主库推数据; "拉取模式"是笼统说法
70. **三水位** — masterPutWhere > push2SlaveMaxOffset (CAS) > slaveAckOffset (volatile); 组提交等确认水位; 256MB 落后出 in sync
71. **首连从 1GB 段起点** — slaveRequestOffset==0 特殊路径; 落后靠报自身 offset 续推补差
72. **offset 强校验=对齐检测** — 主推 offset != 从库 maxPhyOffset 即断 (防错乱非防落后)
73. **epoch 文件裁决切换** — (epoch,startOffset) 区间; 升主截断未确认 + confirmOffset 恢复; RocksDB 辅助
74. **三层防脑裂** — broker epoch 守卫 + namesrv stateVersion 仲裁 + 选举 syncStateSet 内; fenced 双层 (setIsolated+runningFlags)

### RM-13 (6)
75. **proxy 双模式** — LOCAL 内嵌 broker (同进程直写, 挂一起挂) / CLUSTER 独立网关 (内嵌客户端转发)
76. **三端口** — gRPC 8081 / remoting 8080 / metrics 5557; 入站 130MB vs remoting 16MB
77. **管线 pipe 反转** — 声明 Authz→Authn→ContextInit, 执行 ContextInit→Authn→Authz
78. **remoting 恒真兜底** — 非 HTTP/2 首包走 remoting 解码; 翻译非透明转发
79. **SignAuthentication 类不存在** — 实为 HmacSHA1 签名 (AclSigner) + 资源授权两段式
80. **CLUSTER 双段认证** — 客户端→proxy 签名 + proxy→broker 内嵌 SessionCredentials; 认证默认关 (authConfig 驱动)

---

## §四 收官 — 13/13 全部完成, 无剩余域

> ✅ **阶段 4.1 RocketMQ 13 域全量交付 (2026-08-14)**。执行序 RM-1→RM-13 全部闭环。
> 可选后续 (用户需求驱动): TieredStore (🟡 C 简略面, RM-3 一句话) 深挖 / 🔴 域 harness 补建 (布隆数学/AVG/组提交/双水位) / Obsidian 双链 vault 转换 (STAGE3 §八 全局待办)。

---

## §五 完成检查单 (收官确认)

- [x] 13/13 域全部完成: 大纲 974 行 / 闭环 78 / questions 260 / 行号验证 ~521 / 发现 197
- [x] 全部域 Pass 0-3 + 六层深审 + 时空溯源 + 4-5 轮 REVIEW
- [x] 速查全部入 §一 (13 域完整速查库)
- [x] §零/§二/§三 统计全量同步 (V14 自审核对)
- [ ] Obsidian 知识图谱转换 (全局待办, 13 域大纲转双链 vault)

---

## §六 文件路径

```
analysis/source-analysis/rocketmq/
├── ROCKETMQ-PLAN.md        ← 13 域规划 (v1 审计 10→13 + v2 客户端缺口 + v3 定级 16→13 + v3.1 决策理由三标准)
├── HANDOFF-ROCKETMQ.md     ← 本文 V14 (超详细全量交接, 唯一入口, 13/13 收官)
├── outlines/
│   ├── rm1-remoting/   rm2-store/   rm3-commitlog/   rm4-delay/   rm5-broker/
│   ├── rm6-filter/     rm7-producer/  rm8-push/       rm9-rebalance/   rm10-order-tx/
│   ├── rm11-namesrv/   rm12-ha/        rm13-proxy/   (每域 11 文件, 13 域全量)
源码: /data/workspace/source-code/code/spring/rocketmq/  (RocketMQ 5.3.1, 19 Maven 模块)
上级: ../HANDOFF-STAGE3.md (阶段3 总入口 — 阶段 4.1 状态 13/13 收官注记)
后续: 阶段 4.2 Kafka (12 域, KRaft 时代 — 执行计划"Kafka 依赖 ZK"排序基于过时认知, 开工前 09 审计)
```
