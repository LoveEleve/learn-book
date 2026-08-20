# RM-4 延迟消息 — 等级队列 + 定时投递

> 前置: [[RM-3-commitlog]] (CQ/tagsCode) + [[RM-2-存储底层]] | 引出: [[RM-8-消费]] [[RM-10-事务]] | 对照: Kafka 延迟队列 (阶段4.2) 
> 🟡 B | 6 KP | [模式: 等级队列 + 定时扫描 + 消息还原]
> Pass 2 闭环: q1(等级表) q2(写路径) q3(投递循环) q4(还原) q5(投递模式+持久化) q6(配置测试)

**读者处境**: 延迟 10 分钟的消息怎么实现? 为什么存 SCHEDULE_TOPIC_XXXX? 到期时间存哪? 这篇拆延迟消息: 18 级等级表、topic 重写、TimerTask 扫描、消息还原。

### 1. 延迟等级表 — 可配置 18 级

场景: "延迟 30 分钟"怎么表示?
源码路径:
- **配置** (MessageStoreConfig:224): `messageDelayLevel = "1s 5s 10s 30s 1m ... 1h 2h"` — **18 级字符串可配置** (非硬编码)
- **解析** (parseDelayLevel L299-325): 空格分割 + **单位表 s/m/h/d** → delayLevelTable (ConcurrentSkipListMap) + maxDelayLevel
- **越界钳制** (CommitLog L541-544): 客户端 level > max → 钳到 max
- **等级↔队列**: level-1 = queueId (一一对应)
关键设计 (q1): **等级即队列** — 18 个延迟队列, 每级独立进度; 配置化让部署可调。[模式: 等级队列]

### 2. 写路径 — topic 重写 + 到期标记

场景: 客户端发"延迟 1m"的消息, broker 存哪?
源码路径:
- **写前改道** (HookUtils.transformDelayLevelMessage L226-236): 客户端 DELAY_TIME_LEVEL 属性 → **钳制** → **备份 REAL_TOPIC/REAL_QUEUE_ID** → topic 改 SCHEDULE_TOPIC_XXXX + queueId=level-1 (发送钩子链); **消费重试另走延迟**: 重试消息 delayLevel = 3+reconsumeTimes (AbstractSendMessageProcessor L209-212)
- **到期标记** (CommitLog.doAppend L536-552): 分发时 **tagsCode = computeDeliverTimestamp(level, storeTimestamp) = 落盘时间 + 延迟** (L548) — **到期绝对时间存 CQ 的 tagsCode 槽**
- **兜底** (L125-130): 表缺失 → +1000ms
关键设计 (q2): **tagsCode 槽复用** — 普通消息存 tag 哈希, 延迟消息存到期时间 (由 topic 区分语义); 零额外存储。[模式: 槽复用]

### 3. 投递主循环 — 每级 TimerTask

场景: 到期消息怎么被捞出来?
源码路径:
- **启动** (start L134-156): **maxDelayLevel 线程池 (每级一线程)** + 每级 schedule(TimerTask, **1s 首启**); load 前置 + offset 续扫
- **扫描** (executeOnTimeUp L401-485): CQ 迭代 → tagsCode (到期时间) vs now:
  - **未到期** → 100ms 后重查 (offset 续扫, L447-452)
  - **到期** → 取消息 → messageTimeUp 还原 → 投递
  - **失败** → 100ms 重排; 队尾 → 10s 周期重排
- **correctDeliverTimestamp** (L387-393): deliverTimestamp > now+levelDelay → **now (防永久等待** — 到期时间异常超远; **代价: 时钟回拨时消息提前投递** — 推导: now 回拨 → 条件恒真 → 立即投递); **Ext 丢失兜底** (L434-440): tagsCode 无效 → 重算; **batch 断言** (L446): 延迟队列不支持批量消息 (getBatchNum==1)
- **TRANS_HALF 校验** (L460-464): 真实 topic 是事务半消息 → discard
关键设计 (q3): **三级延迟节奏** (1s 首启/100ms 未到期/10s 队尾) — 精度与开销平衡。[模式: 定时扫描]

### 4. 消息还原 — messageTimeUp

场景: 到期后消息怎么回到真实 topic?
源码路径:
- **还原** (L340-380): 复制消息体 → **清延迟属性** (DELAY_TIME_LEVEL/TIMER_DELIVER_MS/TIMER_DELAY_SEC, L371-374) → **topic = REAL_TOPIC / queueId = REAL_QUEUE_ID** (L376-380); **REAL_* 属性残留** (还原后 properties 仍含, 无害但消费者可见 — 标注)
- **tagsCode 重算** (L346-348): 按还原后 tag 重算哈希 (真实 topic 的 CQ 语义)
- **waitStoreMsgOK=false** (L369): 投递不要求同步刷盘 (异步容忍)
关键设计 (q4): **属性往返** — REAL_* 属性在延迟期间保存原目标, 到期还原; 消费者对延迟透明。[模式: 消息还原]

### 5. 投递模式与持久化 — sync/async + offset JSON

场景: 投递失败怎么办? 进度存哪?
源码路径:
- **syncDeliver** (L494): 等待 putMessage 结果 → PUT_OK → updateOffset
- **asyncDeliver** (5.x, L508-527): deliverPendingTable 队列 + **流控 (maxPendingLimit=2000)** + Blocked 语义; **完成回调 HandlePutResultTask** (每级轮询: SUCCESS→updateOffset/RUNNING→重排/**FAILED→失败面**, L550-570+) — 异步面
- **offset 持久化**: DelayOffsetSerializeWrapper JSON + scheduleAtFixedRate(**初始 10s** + flushDelayOffsetInterval) + 关闭时; load 恢复; **loadWhenSyncDelayOffset** (DLedger 专用, RM-12)
- **版本计数**: DelayOffsetUpdateVersionStep (5.x 状态机)
关键设计 (q5): **offset 是投递进度锚点** — 崩溃后从持久化 offset 续扫; 5.x 异步投递解耦等待。[模式: 进度持久化]

### 6. 配置与测试

- 配置: messageDelayLevel / enableScheduleAsyncDeliver=false 默认 / maxPendingLimit=2000 / flushDelayOffsetInterval=10s
- 常量: 1s/100ms/10s/5s/10ms (L43-49)
- 测试: testLoad / testCorrectDelayOffset / testDeliverDelayedMessageTimerTask (level=3 → queueId=2)
- 系统 topic 受保护 (用户不可发 SCHEDULE_TOPIC_XXXX)

### 负面空间 — 延迟消息刻意不做的事

- **不做秒级以下延迟**: 等级粒度 (1s 起), 无任意延迟 (对照 Timer 消息 5.x)
- **不做精度保证**: 投递有 ±100ms/10s 级误差 (轮询而非精确调度)
- **不做堆积告警**: 延迟积压靠外部监控 (offset 表可见)
- **不做等级热增**: messageDelayLevel 需重启生效
- **不做事务延迟混合**: TRANS_HALF 消息直接 discard (不支持)
- **不做投递去重**: 投递成功→updateOffset 间崩溃 → 重启续扫 → **至少一次语义 (可能重复投递)**; 时钟回拨可致提前投递

→ 引出: 事务消息怎么管理半消息? → [[RM-10-事务]]
