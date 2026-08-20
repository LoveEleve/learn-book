# RM-10 顺序+事务消息 — 队列绑定与半消息回查

> 前置: [[RM-7-发送]] (sendKernelImpl/重试) + [[RM-8-消费]] (有序消费锁) + [[RM-9-再平衡]] (队列锁/起点) + [[RM-3-存储]] (PreparedTransactionOffset) + [[RM-5-Broker]] (initialTransaction SPI) | 引出: [[RM-12-HA]] (从库代主)
> 🟡 B | 6 KP | [模式: 队列选择 + 两阶段回查 + op 消息对账]
> Pass 2 闭环: q1(顺序发送) q2(顺序消费协同) q3(半消息写路径) q4(本地事务+END) q5(回查) q6(5.x 新面)

**读者处境**: 怎么让同一订单的消息按顺序被消费? 消息发了业务失败了怎么撤回? 这篇拆顺序消息 (选择器绑定队列) 和事务消息 (半消息 + 本地事务 + 回查对账)。

### 1. 顺序消息 — 选择器绑定队列

场景: 同一订单的多条消息怎么保证到同一队列?
源码路径:
- **MessageQueueSelector 接口** (client/producer): select(mqs, msg, arg) 自定义选队列; 三个内置实现 (selector/ 3 文件)
- **SelectMessageQueueByHash**: `arg.hashCode() % mqs.size()`, 负数 Math.abs (L28-31) — **业务键 hash → 固定队列**
- **SelectMessageQueueByRandom**: 随机 (非顺序用途); **SelectMessageQueueByMachineRoom: 返回 null — 桩实现 (L29-31)** ⚠
- **sendSelectImpl** (DefaultMQProducerImpl:1314-1353): 选队列一次 → **单次 sendKernelImpl — SYNC 无重试** (失败即抛, 保序不换队列); **ASYNC 回调层重试发同 broker** (topicPublishInfo=null → onExceptionImpl 里 retryBrokerName=brokerName, MQClientAPIImpl:778-782)
- **invokeMessageQueueSelector** (L679-713): 选队列超时计入预算; namespace 剥离后选择再回包
关键设计 (q1): **"全局序"是伪命题 — 顺序 = 同队列内严格有序**; 顺序保证 = 选择器确定性 + 消费端锁双端。[模式: 队列选择]

### 2. 顺序消费协同 — 队列锁与有序消费

场景: 多个消费者怎么保证同一队列不被并发消费?
源码路径:
- **消费端**: ConsumeMessageOrderlyService (MessageQueueLock 进程内队列锁, RM-8 已讲) + 锁失败 10ms 重试
- **broker 侧**: LOCK_BATCH_MQ 批量锁 (RM-9 已讲) — **跨客户端互斥**: 再平衡时 `isOrder && !lock(mq)` 立即加锁 (RebalanceImpl:521)
- **起点**: computePullFromWhereWithException **无 isOrder 特殊分支 (5.3.1)** — 顺序 topic 与普通同起点逻辑 (默认尾部); 旧版"顺序从 0 读"说法过时
关键设计 (q2): **顺序 = 单队列单消费者串行**; 锁在 broker (跨客户端) + 客户端双重。[模式: 分布式锁]

### 3. 半消息写路径 — TRAN_MSG 改道

场景: 事务消息落盘时和普通消息有什么不同?
源码路径:
- **客户端标记**: sendMessageInTransaction (DefaultMQProducerImpl:1418-1498) → PROPERTY_TRANSACTION_PREPARED="true" (TRAN_MSG, L1434) + PROPERTY_PRODUCER_GROUP; **忽略 DelayTimeLevel**
- **broker 判定**: SendMessageProcessor:304-318 — TRAN_MSG=true 且非 (重试+延迟) (4.6.1 兼容) → rejectTransactionMessage 配置拒收 (NO_PERMISSION) 或走事务面
- **parseHalfMessageInner** (TransactionalMessageBridge:219-233): UNIQ_KEY→`__transactionId__` 属性 + **REAL_TOPIC/REAL_QID 备份** + sysFlag 清为 TRANSACTION_NOT_TYPE + **topic 改写 RMQ_SYS_TRANS_HALF_TOPIC + queueId=0**
- **半 topic 1 队列**: TopicConfigManager:195-215 (HALF/OP 均 1 读 1 写); 系统 topic 禁客户端发送 (TopicValidator:62-64)
- **存储面**: 半消息走普通 putMessage → CommitLog (PreparedTransactionOffset 段, RM-3); **投递成功返回后才执行本地事务**
关键设计 (q3): **半消息 = 真实消息换皮** (REAL_* 属性还原现场); 失败面: 半消息写失败 → 本地事务不执行。[模式: 两阶段]

### 4. 本地事务 + END_TRANSACTION — 状态回传

场景: 本地事务执行完怎么让 broker 提交/回滚?
源码路径:
- **本地事务**: executeLocalTransaction (L1454-1459) — 用户代码同库事务; 返回 COMMIT/ROLLBACK/UNKNOW; 异常 → localException 记录
- **endTransaction** (L1508-1548): 解析 offsetMsgId → END_TRANSACTION (37) **oneway 发送** (超时 sendMsgTimeout, 失败只 log — **不回滚本地已提交业务!**); **FLUSH_DISK_TIMEOUT/FLUSH_SLAVE_TIMEOUT/SLAVE_NOT_AVAILABLE → 强制 ROLLBACK** (L1475-1479, 半消息可能已落盘, 弃置语义安全)
- **EndTransactionProcessor** (349 行): SLAVE 拒收 (SLAVE_NOT_AVAILABLE, L64-68) → **checkPrepareMessage 三校验** (producerGroup/tranStateTableOffset=queueOffset/commitLogOffset 逐项比对, L220-248) → commit: **endMessageTransaction 还原真实 topic/qid + tagsCode 重算 + 清 REAL_* 属性** (L250-273) → sendFinalMessage 再落盘 (PUT_OK/FLUSH_DISK_TIMEOUT/FLUSH_SLAVE_TIMEOUT/SLAVE_NOT_AVAILABLE 均算 SUCCESS, L285-290) → **deletePrepareMessage 写 OP 消息** (标记对账) / rollback: 直接 deletePrepareMessage
- **OP 消息**: RMQ_SYS_TRANS_OP_HALF_TOPIC + tag="d" + body=半消息 queueOffset 列表 (TransactionalMessageUtil: REMOVE_TAG="d"/OFFSET_SEPARATOR=",") — **回查对账的依据**
- **rejectCommitOrRollback** (L199-213): 非回查请求超过自定义免疫时间 → ILLEGAL_OPERATION (604) 拒绝 — 防过期事务污染; **免疫时间双实现不一致** ⚠ (回查侧 ServiceImpl:356-366 不钳制 vs END 侧 Util:78-92 钳制 ≥6s — 同一属性两路径行为不同)
关键设计 (q4): **END 是 oneway — 发完即走**; 客户端不重试 (本地事务已提交, 靠回查兜底); **三校验防伪造/串消息, 不防同 offset 重复 commit** (EndTransactionProcessor 全程无 op 查询); **END 与回查竞态窗口: 双 commit → 同业务消息重复投递** — at-least-once 实证, 防护=客户端单次 END+业务幂等。[模式: 状态回传]

### 5. 回查 — 30s 周期对账

场景: END_TRANSACTION 丢了/超时了怎么办?
源码路径:
- **调度**: TransactionalMessageCheckService (ServiceThread, 30s 周期 transactionCheckInterval) → check(timeout=6s, checkMax=15)
- **check 主循环** (TransactionalMessageServiceImpl:161-354): 逐半队列 → 读 OP 队列 (OP_MSG_PULL_NUMS=32) 建 **removeMap** (已提交/回滚的 halfOffset → 跳过) → 逐条:
  - **needDiscard**: TRANSACTION_CHECK_TIMES ≥ 15 → TCMT topic 丢弃 (DefaultTransactionalMessageCheckListener:43-61, TRANS_CHECK_MAX_TIME_TOPIC 1 队列); **checkTimes 免疫期也递增** (needDiscard 先于免疫检查 L263 vs L269) — 遭遇 15 次后第 16 次弃置; **长免疫期 (自定义 >480s) 消息可能未查即弃** ⚠ 边缘
  - **needSkip**: born > fileReservedTime(72h) → 跳过
  - **免疫期**: 半消息 born 至今 < checkImmunityTime (**回查侧: 自定义 CHECK_IMMUNITY_TIME_IN_SECONDS×1000, 未设/解析失败=6s, 不钳制** ServiceImpl:356-366) → 不查 (L275-293); 免疫重写携带 TRAN_PREPARED_QUEUE_OFFSET 供下轮对账
  - **isNeedCheck 三条件** (L294-298): 无 OP 且超免疫 / OP 最后一条 born-startTime>6s / **时钟回拨 (born 未来时间 ≤ -1)** → 检查
  - **putBackHalfMsgQueue**: 半消息重写回半队列 (新 offset) — **mutate msgExt (queueOffset/commitLogOffset/msgId 改新值)** → **listener.resolveHalfMsg → sendCheckMessage → CHECK_TRANSACTION_STATE (39) 发客户端** (应答带新 offset → op 标记新 copy → 下轮对账跳过 — **重写链单条单轮闭环, 不放大重复**; bornTimestamp 保留原值 → 重写 copy 免疫期立即失效, 重写即查; 重写失败 → continue 不推进, 同 offset 重试至 60s 上限)
- **客户端应答** (DefaultMQProducerImpl:361-449): checkExecutor 线程池 (默认 1 线程, checkRequestHoldMax=2000, **AbortPolicy — 满则应答丢弃, 下轮再查**) → checkLocalTransaction → endTransactionOneway (fromTransactionCheck=true, **固定超时 3000** — 主路径 sendMsgTimeout 双超时对照); **broker 侧 resolveHalfMsg 是 CallerRunsPolicy 背压 (2-5 线程/2000 队列)** — 双端策略不对称: broker 必达, 客户端尽力
- **进度**: 每队列处理上限 MAX_PROCESS_TIME_LIMIT=60s; 队列消费进度半消息侧 updateConsumeOffset 推进; **半队列膨胀** ⚠ (重写链多副本 + 弃置前最多 15 轮滞留 — 长滞留事务消耗 HALF topic 1 队列容量)
- **5.x 从库代主**: enableSlaveActingMaster → 半消息经 EscapeBridge 转发 (escapeMessage, 10 次重试 `100*(2^escapeFailCnt)`ms — **2^cnt 是 Java 异或非幂, 退避序列 300,0,100,600,... 非指数**, L234-261)
关键设计 (q5): **回查 = 定时对账 (30s) + 免疫期 (6s) 双保护**; 状态=半消息+OP 消息双流推断; **至少一次 (at-least-once)**: 客户端杀进程 → 半消息滞留 → 无限回查直到 15 次弃置。[模式: 定时对账]

### 6. 5.x 新面 — OP 批量 + 事务指标

- **OP 批量**: deletePrepareMessage 攒 deleteContext 队列 (per queueId) → TransactionalOpBatchService (3s 周期) 批量写 OP (maxSize=4096B); 满 4096 立即触发
- **事务指标**: TransactionMetrics (持久化) + TransactionMetricsFlushService (3s 落盘) + BrokerMetricsManager (commit/rollback 计数与延迟, EndTransactionProcessor:151-159)
- **SPI 扩展**: TransactionalMessageService / AbstractTransactionalMessageCheckListener 均 ServiceLoader (BrokerController:989-1006); 4.x TransactionCheckListener 已废弃 → TransactionListener

### 负面空间 — 顺序+事务刻意不做的事

- **不做全局序**: 顺序仅同队列内; "严格全局有序"需单队列 (吞吐受限)
- **不做选择器重试**: SYNC 顺序发送失败即抛 — 不换队列重发 (会乱序); 保序优先于可用性
- **不做协议级 2PC**: 半消息+回查是"最大努力"最终一致, 非分布式事务原子提交
- **不做本地事务补偿**: 客户端 END oneway 丢失 → 靠回查; 本地已提交不回滚 (业务需自备幂等)
- **不做回查去重**: at-least-once — 三校验防伪造不防重复; END 与回查竞态可双投递; 业务需自备幂等 (uniqKey)
- **不做幂等存储**: 半消息重复投递由业务 uniqKey 幂等
- **MachineRoom 选择器是桩**: select() 返回 null (占位实现)

→ 引出: 从库怎么接替主库? → [[RM-12-HA]]
