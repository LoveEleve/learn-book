# 闭环笔记 q6: 5.x 新面 — OP 批量 + 事务指标 + SPI + 从库代主

## 假设
5.x 事务面新增: OP 写入批量、事务指标持久化、SPI 可扩展、从库代主逃逸。

## 验证过程
- **OP 批量** (5.x):
  - deletePrepareMessage (TransactionalMessageServiceImpl:596-631): 半消息 queueOffset 字符串 (带 "," 尾分隔) 入 per-queueId MessageQueueOpContext 的 contextQueue; totalSize 超 **transactionOpMsgMaxSize=4096B** → wakeup 立即批量写; offer 100ms 超时兜底单条写
  - TransactionalOpBatchService (65 行): ServiceThread, 周期 transactionOpBatchInterval=**3000ms**; onWaitEnd → batchSendOpMessage (L698-752): 逐 context (totalSize>0 且 达间隔/满 4096) 攒入 sendMap → writeOp 批量写 OP topic; 返回下次 wakeup 时间
- **事务指标** (5.x): TransactionMetrics (半消息计数持久化, broker 路径 JSON) + TransactionMetricsFlushService (**3s** 周期 persist, transactionMetricFlushInterval) + BrokerMetricsManager commit/rollback 计数与 finish 延迟 (EndTransactionProcessor:151-159)
- **SPI 扩展**: BrokerController.initialTransaction (L989-1006) — TransactionalMessageService 与 AbstractTransactionalMessageCheckListener 均 ServiceLoader 加载, 缺省回退默认实现; 测试资源 META-INF/service 实证可替换
- **从库代主** (5.x): enableSlaveActingMaster=false 默认 + enableRemoteEscape; EscapeBridge.putMessage: 有主 → 主 store 直写; 无主 → 远程转发 (半消息经 buildTransactionalMessageFromHalfMessage 还原真实 topic 后发) (EscapeBridge.java:95-116)
- **废弃面**: TransactionCheckListener / checkThreadPoolMinSize / checkRequestHoldMax @Deprecated (TransactionMQProducer:96-138) — 推荐 setExecutorService 自定义

## 代码类型
Implementation (批量 + 指标 + SPI)

## 跨域关联
- RM-12 (HA): enableSlaveActingMaster / EscapeBridge / minBrokerIdInGroup (5.x 故障转移面)
- RM-5 (Broker): 定时任务面 (TransactionMetricsFlushService 注册)

## 结论
5.x 事务消息 = 3.x 骨架 + OP 批量 (降写放大) + 指标 (可观测) + SPI (可替换) + 从库代主 (HA 协同)。接口面: 4.x TransactionCheckListener → TransactionListener。
源码位置: TransactionalMessageServiceImpl.java:596-752; TransactionalOpBatchService.java:46-64; BrokerController.java:989-1006; EscapeBridge.java:95-116
