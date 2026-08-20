# RM-7 Producer 发送 — 三模式 + 故障感知负载均衡

> 前置: [[RM-1-remoting]] (invoke 三模式) + [[RM-11-路由]] (namesrv) | 引出: [[RM-10-事务]] [[RM-8-消费]] | 对照: Kafka Producer (分区选择/重试)
> 🔴 A | 6 KP | [模式: 门面 + 重试编排 + 故障策略 + 轮询均衡]
> Pass 2 闭环: q1(门面三模式) q2(重试编排) q3(故障策略) q4(路由选择) q5(kernel+累积) q6(选择器+异步)

**读者处境**: producer.send() 背后几步? 发失败重试几次? 慢 broker 怎么避开? 这篇拆 Producer: 三模式、重试编排、7 档故障冷却、轮询均衡。

### 1. 门面与三模式

场景: send 族怎么分派?
源码路径:
- **门面** (DefaultMQProducer:1442): send → sendDefaultImpl (SYNC) / send+Callback (ASYNC, **独立重试 retryTimesWhenSendAsyncFailed=2**) / sendOneway (ONEWAY 无重试); **消息体压缩面**: 异步发送时 body 压缩+cloneMessage 恢复 (L1008-1035)
- **配置**: retryTimesWhenSendFailed=2 / retryAnotherBrokerWhenNotStoreOK=false / **retryResponseCodes 默认 8 码** (TOPIC_NOT_EXIST/SERVICE_NOT_AVAILABLE/SYSTEM_ERROR/SYSTEM_BUSY/NO_PERMISSION/NO_BUYER_ID/NOT_IN_CURRENT_UNIT/GO_AWAY, CopyOnWriteArraySet)
- **hook**: sendMessageHookList (观测扩展)
关键设计 (q1): **门面薄封装** — 全部逻辑在 impl; 重试码白名单控制"什么错值得重试"。[模式: 门面]

### 2. 发送主链 — 重试编排

场景: 发失败怎么办?
源码路径:
- **timesTotal** (L760): **SYNC 1+2 次 (sendDefaultImpl 层)**; **ASYNC 的重试在 MQClientAPIImpl 回调层** (sendMessageAsync 内 onException → times<retryTimesWhenSendAsyncFailed=2 → **换 broker 递归重发** L674-787); **ONEWAY 无重试**; ASYNC 的 updateFaultItem 度量=发送发起耗时 (异步本质)
- **重试循环** (L762-849): lastBroker 传递 → **resetIndex** (重轮询) → timeout 扣减 → kernel
- **异常三分类** (L794-840):
  - MQClientException → 故障更新 (非隔离) + continue
  - **RemotingException → 隔离 (10s)** 或探测模式置不可达
  - **MQBrokerException → 不可达** + retryResponseCodes 判定 (可重试 continue / 否则抛)
  - InterruptedException → 抛
- **SEND_OK 检查**: retryAnotherBrokerWhenNotStoreOK → 换 broker
关键设计 (q2): **异常分类决定故障维度与重试策略** (网络错=隔离, broker 错=仅不可达+白名单重试); 超时预算逐次扣减。[模式: 重试编排]

### 3. 故障策略 — 7 档冷却

场景: 慢 broker 多久不用?
源码路径:
- **7 档映射** (MQFaultStrategy:29-30): latencyMax {50..15000}ms → notAvailableDuration {0..30000}ms — **越慢冷却越久**
- **isolation** (L174): 发送失败/网络异常 → **固定 10s**
- **双维度** (5.x): 延迟冷却 + **reachableFlag 可达性** (ServiceDetector 探测恢复)
- **三级选择** (L145-170): availableFilter → reachableFilter → 兜底 (逐级放宽)
关键设计 (q3): **延迟→冷却分段映射** = 自适应避开慢节点; 三级降级保可用性。[模式: 故障冷却]

### 4. 队列选择与路由

场景: 队列怎么挑?
源码路径:
- **路由** (tryToFindTopicPublishInfo L883): namesrv 发现 → 缓存 → TopicPublishInfo
- **轮询** (selectOneMessageQueue): `sendWhichQueue.incrementAndGet() % size` — **线程独立递增**
- **resetIndex** (L766): 重试重置轮询索引 (配合 lastBrokerName 排除已试 broker — 防重试打回同 broker); brokersSent 记录
关键设计 (q4): **递增取模 = 无锁轮询均衡**; 线程独立防竞争。[模式: 轮询均衡]

### 5. kernel 与 5.x 批量累积

- **sendKernelImpl** (L900): 头构造 (group/topic/queue/bornHost/uniqId) → hook 前置 → invoke 三模式 → 响应解析; **重试属性面**: 重试时设置 RETRY 相关属性 (与消费重试 RM-8 共用语义)
- **ProduceAccumulator** (5.x): **批量累积** (延迟 1ms-30s / 字节 1B-2MB 双条件); **AggregateKey 四维分组** (topic/mq/tag/waitStoreMsgOK, L287-312); sync/async 双批表
关键设计 (q5): **累积 = 小消息吞吐优化** (批量协议 SEND_BATCH)。[模式: 批量累积]

### 6. 选择器与异步面

- **选择器三实现**: hash (顺序消息, RM-10) / 机房间 / 随机
- **异步回调**: SendCallback + **callbackExecutor 可注入** (setCallbackExecutor → remoting 层)
- **扩展**: TransactionMQProducer (RM-10) / Request-Reply (5.x)

### 负面空间 — Producer 刻意不做的事

- **不做同步无限重试**: 有限次 (2) + 白名单 — 防雪崩
- **不做严格负载均衡**: 轮询近似均匀, 无权重 (对照 Kafka sticky partitioner)
- **不做发送事务保证**: 半消息由 RM-10 回查兜底
- **不做端到端确认**: SEND_OK 是存储确认, 非消费确认 (对照 Kafka acks)
- **不做压缩内建**: 消息压缩由业务层 (对照 Kafka 内建)

→ 引出: 事务消息怎么保证一致性? → [[RM-10-事务]]
