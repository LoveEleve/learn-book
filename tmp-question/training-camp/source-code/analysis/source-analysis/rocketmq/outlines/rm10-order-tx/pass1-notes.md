# RM-10 顺序+事务消息 — Pass 1 探索笔记

> 域: RM-10 顺序消息 + 事务消息 | 🟡 B 方案 | 2026-08-14
> 源码: client selector/ 3 文件 (107) + TransactionMQProducer (155) + DefaultMQProducerImpl 事务面 (1418-1548, 361-449) + SendMessageProcessor 事务分支 (304-363) + EndTransactionProcessor (349) + TransactionalMessageBridge (364) + TransactionalMessageServiceImpl (757) + TransactionalMessageCheckService (62) + 2 Listener + TransactionalOpBatchService (65) + TransactionMetrics 族 + EscapeBridge (5.x) | RocketMQ 5.3.1

## 调用图

```
顺序面 (client):
send(msg, selector, arg) → sendSelectImpl (L1314-1353): 选择器选队列一次
  → 单次 sendKernelImpl (SYNC 无重试!) / ASYNC: topicPublishInfo=null → 重试同 broker
  → 消费端: ConsumeMessageOrderlyService (RM-8) + broker LOCK_BATCH_MQ (RM-9) + RebalanceImpl:521 isOrder 立即锁

事务面 (client):
sendMessageInTransaction (L1418): TRAN_MSG=true + PGROUP 属性 + 清 DELAY
  → send() 半消息 → 本地事务 executeLocalTransaction (SEND_OK 才执行)
  → endTransaction (L1508): END_TRANSACTION(37) oneway
      → EndTransactionProcessor: SLAVE 拒 → 三校验 → commit: endMessageTransaction 还原+重投 / rollback: 删半消息
      → 均写 OP 消息 (tag=d, body=offset 列表)

回查面 (broker):
TransactionalMessageCheckService (30s 周期) → TransactionalMessageServiceImpl.check (757)
  → 半队列 + OP 队列对账 (removeMap) → 免疫期 6s → isNeedCheck 三条件
  → putBackHalfMsgQueue (重写回半队列) → resolveHalfMsg → CHECK_TRANSACTION_STATE(39) 发客户端
  → 客户端 checkExecutor (1 线程/2000 队列) → checkLocalTransaction → endTransactionOneway (fromTransactionCheck=true)

5.x 新面: TransactionalOpBatchService (OP 攒批 3s/4096B) + TransactionMetrics + EscapeBridge (从库代主)
```

## 基本元素分解

1. **顺序发送**: 选择器 (hash/random/机房间桩) + sendSelectImpl 单次发送
2. **顺序消费协同**: 队列锁 (RM-8/9 交叉) + 起点无特殊分支
3. **半消息写路径**: TRAN_MSG 判定 + parseHalfMessageInner 改道
4. **本地事务+END**: 状态回传 + EndTransactionProcessor 校验/还原/重投
5. **回查**: 30s 周期 + 免疫期 + 三条件 + 15 次弃置
6. **5.x 新面**: OP 批量 + 指标 + SPI + 从库代主

## 标记问题 (20 问)

1. 顺序消息怎么选队列? (hashCode % size)
2. 顺序发送失败重试吗? (SYNC 不重试?)
3. ASYNC 顺序重试换不换 broker? (topicPublishInfo=null?)
4. 机房间选择器实现? (返回 null?)
5. 半消息存哪? (HALF topic queueId=0)
6. REAL_TOPIC/REAL_QID 怎么备份还原?
7. TRAN_MSG 谁标记的? (客户端属性)
8. 本地事务什么时候执行? (SEND_OK 后)
9. END_TRANSACTION 是同步还是 oneway? (oneway)
10. 三校验查什么? (group/两个 offset)
11. commit 怎么还原真实消息? (endMessageTransaction)
12. OP 消息什么格式? (tag=d + body=offset 列表)
13. 回查周期和免疫期? (30s / 6s)
14. isNeedCheck 三条件? (无OP超免疫/OP旧/时钟回拨)
15. 回查次数上限? (15 → TCMT topic)
16. 半消息重写回队列为什么? (回查后可再查)
17. 客户端回查应答线程池? (checkExecutor 1 线程)
18. 从库代主怎么逃逸半消息? (EscapeBridge)
19. OP 批量服务参数? (3s / 4096B)
20. 事务消息支持延迟吗? (清 DelayTimeLevel)

## 时空溯源 (代码内痕迹)

- 3.x: 半消息+OP 消息+回查骨架 (RMQ_SYS_TRANS_* topic 系统级)
- 4.6.1: TransactionListener (executeLocalTransaction/checkLocalTransaction) 替代 TransactionCheckListener; TRAN_MSG 兼容分支注释 (SendMessageProcessor:307)
- 5.0: 事务 SPI 化 (ServiceProvider.loadClass, BrokerController:989-1006); TransactionCheckListener/checkThreadPool 废弃 (TransactionMQProducer:96-138)
- 5.x: TransactionalOpBatchService (OP 批量) + TransactionMetrics + enableSlaveActingMaster (EscapeBridge 逃逸)

## 大域拆分判断

RM-10 = 顺序 (client 选择器 + 消费锁协同) + 事务 (client 半消息 + broker 回查) 双主题; 单篇 🟡 B, 6 闭环; 消费锁细节归 RM-8/9 (本域交叉引用不展开)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "MessageQueueSelector/TransactionMQProducer/回查" | selector 3 文件 + TransactionMQProducer + TransactionalMessageCheckService 链 | **接受** ✅ |
| 数字: 选择器实现 | 3 个 (Hash/Random/MachineRoom); **MachineRoom 返回 null 桩** | **修正** ⚠ |
| 数字: 事务参数 | timeOut=6s / checkMax=15 / checkInterval=30s / opMaxSize=4096 / opBatchInterval=3s / metricFlush=3s | **补充** ✅ |
| 数字: 请求码 | END_TRANSACTION=37 / CHECK_TRANSACTION_STATE=39 | **补充** ✅ |
| 数字: 半/OP topic 队列 | 各 1 队列 (TopicConfigManager) | **补充** ✅ |
| 数字: sysFlag 事务位 | NOT=0 / PREPARED=0x1<<2 / COMMIT=0x2<<2 / ROLLBACK=0x3<<2 | **补充** ✅ |
| "顺序从 0 读" 旧说法 | computePullFromWhere 无 isOrder 分支 — 5.3.1 无特殊起点 | **认知修正** ⚠ |
