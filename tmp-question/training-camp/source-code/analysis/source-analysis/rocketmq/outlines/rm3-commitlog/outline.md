# RM-3 CommitLog+ConsumeQueue+IndexFile — 消息主链路与双索引

> 前置: [[RM-2-存储底层]] (MappedFile/刷盘) + [[RM-1-remoting]] (协议) | 引出: [[RM-8-消费]] [[RM-9-再平衡]] [[RM-16]] | 对照: [[R-8-rdb-aof]] (Redis 持久化) + Kafka (阶段4.2, log 文件对照)
> 🔴 A | 6 KP | [模式: 二进制消息格式 + 写后分发 + 逻辑队列 + 哈希索引]
> Pass 2 闭环: q1(编码) q2(分发) q3(CQ) q4(IndexFile) q5(读面+恢复) q6(5.x 多实现)

**读者处境**: 消息在 CommitLog 里长什么样? 按 topic/queue 怎么找? 按 key 怎么找? 重启怎么对齐? 这篇拆主链路: 18 段消息格式、写后分发链、20B 逻辑队列、哈希索引。

### 1. 消息编码 — 18 段字段序

场景: 一条消息落盘前的字节布局?
源码路径:
- **18 段字段** (MessageExtEncoder.encode L200-278): TOTALSIZE/MAGICCODE/BODYCRC/QUEUEID/FLAG/QUEUEOFFSET/PHYSICALOFFSET(回填)/SYSFLAG/BORNTIMESTAMP/BORNHOST/STORETIMESTAMP/STOREHOST/RECONSUMETIMES/PreparedTxOffset/BODY/TOPIC/PROPERTIES/**属性 CRC32 (4B, enabledAppendPropCRC 配置, L57)**
- **calMsgLength** (L60-83): 定长头 + 变长段; **V6 标志** (BORNHOST_V6_FLAG → 8B/20B IPv6)
- **双上限**: maxMessageSize=**4MB** (MessageStoreConfig:168)/maxMessageBodySize (MESSAGE_ILLEGAL); **属性超限** (propertiesLength > Short.MAX → PROPERTIES_SIZE_EXCEEDED); PHYSICALOFFSET **回填在 doAppend** (L2010 区域)
- **版本**: MESSAGE_VERSION_V2 (topic 长 short); **Batch** (encode L282: 批量打包); **encodeWithoutProperties** (5.x 多分派)
关键设计 (q1): **物理偏移写后回填** (先占位后定位); 长度预算精确 (mmap 缓冲预分配)。[模式: 二进制消息格式]

### 2. 写后分发 — dispatcherList 链

场景: 消息写进 CommitLog 后, CQ/索引怎么同步?
源码路径:
- **三链** (DefaultMessageStore L266-272): BuildConsumeQueue + BuildIndex (+ 5.x Compaction)
- **触发** (CommitLog L338-352): doAppend 后 → onCommitLogDispatch (**doDispatch && !isFileEnd**); **恢复期双路径**: 正常退出 recoverNormally **不重放分发** (L337 "normal recover doesn't require dispatching" — CQ 已持久) / **异常退出 recoverAbnormally 重放分发** (重建 CQ/Index, L695)
- **DispatchRequest**: 写后中间契约 (topic/queueId/offset/size/tagsCode/bitMap/时间)
- **CQ 可写限流**: putMessagePositionInfoWrapper 30 次重试 + isCQWriteable
- **一致性** (L418 注释): 恢复期末端对齐 "eliminating the dispatch inconsistency"
关键设计 (q2): **同步分发链** (写后即建索引 — 读一致性); 恢复期跳过分发 (靠截断对齐)。[模式: 写后分发]

### 3. ConsumeQueue — 20B 逻辑队列

场景: 百万 topic 怎么不扫 CommitLog?
源码路径:
- **单元** (ConsumeQueue:59): **20B** = commitLogOffset(8) + size(4) + tagsCode(8); **tagsCode 生成**: tag 字符串 → 哈希 (8B, RM-6 filter 交叉)
- **Ext 扩展** (L200-230, **enableConsumeQueueExt 配置开启时** — 位图过滤场景, 非溢出): tagsCode 高位标记转 ConsumeQueueExt (48MB, CqExtUnit: filterBitMap/msgStoreTime); **tagsCode = tags.hashCode()** (Java 哈希, MessageExtBrokerInner:43-47); **Batch CQ 单元 46B** (BatchConsumeQueue.CQ_STORE_UNIT_SIZE=46, 非 20)
- **文件**: 300000×20 ≈ 5.7MB (MessageStoreConfig:112); 独立刷盘 1s
- **5.x 多实现**: ConsumeQueueStoreInterface — 默认/Batch/Sparse/**RocksDB** (q6)
关键设计 (q3): **20B 固定单元 = O(1) 定位逻辑偏移**; Ext 面把"可变数据"外置, 保持单元固定。[模式: 逻辑队列]

### 4. IndexFile — 哈希槽索引

场景: 按 key 查消息怎么快?
源码路径:
- **布局** (IndexFile:58): 40B 头 + **500 万×4B 槽表** + 20B×N 索引项
- **IndexHeader 40B** (IndexHeader:37): BeginTs/EndTs/BeginPhy/EndPhy/SlotCount/IndexCount
- **索引项 20B**: keyHash(4) + phyOffset(8) + timeDiff(4) + **prevIndex(4) 冲突链**
- **key**: topic#uniqKey + **多 keys 逐个建索引** (split KEY_SEPARATOR, IndexService:235-246); **TRANSACTION_ROLLBACK 消息跳过索引** (L219-224); **滚动**: putKey full → 新文件 (L255 "is full, trying to create another one")
关键设计 (q4): **哈希槽 + 链式索引项** (对照 Redis dict); timeDiff 相对时间 (4B 够用)。[模式: 哈希索引]

### 5. 读面与恢复 — 双索引消费 + 对齐截断

场景: 消费怎么读? 重启怎么对齐?
源码路径:
- **读** (getMessage DefaultMessageStore:793): 守卫 (shutdown/readable) → CQ 定位 → CommitLog mmap 切片 → **双限** (maxMsgNums/maxTotalMsgSize); **getMessageAsync = completedFuture 同步包装** (L987-990 — **非异步**! 长轮询挂起在 broker PullRequestHoldService, RM-8 交叉)
- **恢复** (DefaultMessageStore.recover L1890-1902): **CQ 先恢复** (getMaxPhyOffsetInConsumeQueue) → **CommitLog recoverNormally(maxPhyOffset) 对齐截断** — 顺序: CQ 定界 → CommitLog 对齐; ConsumeQueue.recover 逐单元校验 → maxPhysicOffset 推进 → 尾文件 truncateDirtyFiles (半写单元丢弃)
关键设计 (q5): **读面 = CQ 索引导航 + mmap 零拷贝**; 恢复 = 单元校验截断 (崩溃安全)。[模式: 导航+切片]

### 6. 5.x 多实现与测试 — queue/ 抽象

场景: 5.x 消费队列有什么新形态?
源码路径:
- **queue/ 包** (17 文件): ConsumeQueueStoreInterface — 默认 (20B) / **Batch** (批量索引) / **Sparse** / **RocksDB** (后端)
- **Batch 面**: BatchConsumeQueue + BatchOffsetIndex + MultiDispatchUtils (批量消息消费索引)
- **测试**: 7 专项 (ConsumeQueue/Batch/Sparse/Store/RocksDB/IndexFile/Ext)
- **Compaction** (5.1): 清理分发 (TieredStore 协同)
关键设计 (q6): **接口化 = 存储后端可替换** (mmap → RocksDB); 批量消费队列 = 批量消息的配套索引。[模式: 多实现抽象]

### 负面空间 — 主链路刻意不做的事

- **不做按消息删除**: 物理删除靠文件过期 (ConsumeQueue 同步截断), 无单条墓碑
- **不做 key 索引全覆盖**: IndexFile 仅 uniqKey 精确查询, 无二级索引/范围
- **不做 CQ 压缩**: 20B 固定单元无压缩 (对照 Sparse 是 5.x 尝试)
- **不做读时过滤下推**: 过滤在拉取侧 (RM-6 filter 域)
- **不做多主写**: 单 CommitLog 写 (对照 Kafka partition 多写)

→ 引出: 消费端怎么用这套索引? → [[RM-8-消费]]
