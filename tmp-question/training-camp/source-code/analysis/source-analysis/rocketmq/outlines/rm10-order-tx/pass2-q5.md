# 闭环笔记 q5: 回查 — 30s 周期对账

## 假设
broker 定时扫半队列, 对已决断 (OP) 的跳过, 未决断且过免疫期的发回查给客户端。

## 验证过程
- **调度**: TransactionalMessageCheckService (ServiceThread, 62 行) — 周期 = transactionCheckInterval **30s** (BrokerConfig:275); onWaitEnd → check(transactionTimeOut=6s, transactionCheckMax=15)
- **check 主循环** (TransactionalMessageServiceImpl:161-354):
  - 逐半队列 (fetchMessageQueues, 半 topic 1 队列) → **OP 队列对账**: fillOpRemoveMap (L379-439) 读 OP (OP_MSG_PULL_NUMS=**32**) 建 removeMap (halfOffset→opOffset) — 已决断的半消息跳过 (L206-213)
  - **needDiscard** (L108-121): TRANSACTION_CHECK_TIMES ≥ **15** → listener.resolveDiscardMsg → **TRANS_CHECK_MAX_TIME_TOPIC** (DefaultTransactionalMessageCheckListener:43-61, TCMT_QUEUE_NUMS=1)
  - **needSkip** (L123-133): born > fileReservedTime(**72h**)×3600×1000 → 跳过
  - **免疫期** (L275-293): born 至今 < checkImmunityTime = max(自定义 CHECK_IMMUNITY_TIME_IN_SECONDS×1000, **6s**) → 不查; 带 TRAN_PREPARED_QUEUE_OFFSET 的免疫消息重写回半队列 (checkPrepareQueueOffset L449-473)
  - **isNeedCheck 三条件** (L294-298): ①无 OP 且超免疫 ②OP 最后一条 born-startTime > 6s ③**born 未来 (时钟回拨) valueOfCurrentMinusBorn ≤ -1** → 检查
  - **putBackHalfMsgQueue** (L135-159): 半消息重写回半队列 (新 offset/新 msgId) — 保证后续轮次还能再查 → listener.resolveHalfMsg
  - **sendCheckMessage** (AbstractTransactionalMessageCheckListener:51-70): CHECK_TRANSACTION_STATE (**39**) + CheckTransactionStateRequestHeader (commitLogOffset/tranStateTableOffset/uniqKey/transactionId) → 经 ProducerManager 取该 group 可用通道 → broker2Client 发送; 无通道 → warn 放弃
  - 处理上限: 每队列 **MAX_PROCESS_TIME_LIMIT=60s** 退出 (L202-205); 半/OP 队列 offset 推进 (L333-339)
  - 无 OP 时 SLEEP_WHILE_NO_OP=1000ms (L319)
- **客户端应答** (DefaultMQProducerImpl:361-449): checkExecutor (默认 ThreadPoolExecutor **1-1 线程**, checkRequestHoldMax=**2000** 队列; 可注入自定义, initTransactionEnv L205-218) → checkLocalTransaction (L381) → processTransactionState → **endTransactionOneway (fromTransactionCheck=true, 超时 3000)** (L440-441)
- **5.x 从库代主**: enableSlaveActingMaster + minBrokerIdInGroup==自身 + SLAVE 角色 → 半消息直接 **escapeMessage 转发主库** (renew + EscapeBridge, L234-261; 失败 10 次重试 100×(2^cnt)ms 退避)

## 代码类型
Implementation (定时对账 + 状态机)

## 跨域关联
- RM-1 (协议): CHECK_TRANSACTION_STATE=39; oneway 回传
- RM-5 (Broker): ProducerManager 通道 / initialTransaction SPI / getMinBrokerIdInGroup
- RM-12 (HA): enableSlaveActingMaster 从库代主 (EscapeBridge 交叉)

## 结论
回查 = 30s 周期对账: OP 跳过已决断, 免疫期 6s 保护新消息, 三条件判定检查, 半消息重写保证可再查, 15 次上限弃置。at-least-once: 无 ACK, 客户端失联消息滞留直至弃置。
源码位置: TransactionalMessageCheckService.java:42-60; TransactionalMessageServiceImpl.java:161-354,379-439,449-473; AbstractTransactionalMessageCheckListener.java:51-70
