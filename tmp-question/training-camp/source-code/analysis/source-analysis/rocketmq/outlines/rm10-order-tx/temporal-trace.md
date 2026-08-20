# RM-10 顺序+事务消息 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | 事务消息骨架: 半消息 (RMQ_SYS_TRANS_HALF_TOPIC) + OP 消息 (RMQ_SYS_TRANS_OP_HALF_TOPIC) + TransactionalMessageCheckService 回查 + TransactionCheckListener; 顺序消息: MessageQueueSelector + hash 选择器 + 有序锁 (LOCK_BATCH_MQ) |
| 4.6.1 | **TransactionListener 新 API** (executeLocalTransaction/checkLocalTransaction) 替代 TransactionCheckListener; TRAN_MSG 兼容分支注释实证 (SendMessageProcessor:307 "For client under version 4.6.1"); 自定义免疫期 CHECK_IMMUNITY_TIME_IN_SECONDS |
| 5.0 | 事务服务 **SPI 化** (ServiceProvider.loadClass); 旧 API @Deprecated (TransactionMQProducer:96-138 "will be removed in the version 5.0.0"); checkRequestHoldMax 保留默认 2000 |
| 5.x | **OP 批量** (TransactionalOpBatchService 3s/4096B); **事务指标** (TransactionMetrics + 3s FlushService); **从库代主逃逸** (enableSlaveActingMaster + EscapeBridge, 半消息还原转发); TransactionOpMsgMaxSize/BatchInterval 配置 |

## 痕迹证据

- TransactionMQProducer.java:96-138: @Deprecated + "will be removed in 5.0.0" (版本锚)
- SendMessageProcessor.java:307: "For client under version 4.6.1" 注释 (重试消息不当 prepare)
- BrokerController.java:989-1006: ServiceProvider.loadClass 双 SPI (5.0 扩展面)
- TopicValidator.java:28-31: 三系统 topic 常量; :62-64 禁发送集合
- TransactionalMessageUtil.java: REMOVE_TAG="d" / OFFSET_SEPARATOR="," (3.x 兼容格式)
- RequestCode.java:58,61: END_TRANSACTION=37 / CHECK_TRANSACTION_STATE=39
- BrokerConfig.java:263-284: 事务六参数 (6s/15/30s/4096/3s/3s)

## 推断标注

- "3.x 骨架" — RocketMQ 公知版本线 (标注); 半/OP topic 命名 3.x 一致 (高置信)
- "4.6.1 新 API" — 源码注释实证
- "5.0 SPI" — ServiceProvider 模式 + @Deprecated 文案实证
- "5.x OP 批量/指标/代主" — 配置项与类为 5.x 引入推断 (TransactionOpMsgMaxSize 等); 未做 git 考古, 已逐条标注
