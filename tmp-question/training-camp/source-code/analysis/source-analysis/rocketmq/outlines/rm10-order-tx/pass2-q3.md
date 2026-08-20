# 闭环笔记 q3: 半消息写路径 — TRAN_MSG 改道

## 假设
事务消息先以"半消息"形态落盘 (消费者不可见), 真实 topic 信息备份在属性里。

## 验证过程
- **客户端标记**: sendMessageInTransaction (DefaultMQProducerImpl:1418-1498) — `PROPERTY_TRANSACTION_PREPARED="true"` (TRAN_MSG, L1434) + PROPERTY_PRODUCER_GROUP (L1435); **清 DelayTimeLevel (L1427-1429)** — 事务消息不支持延迟
- **broker 判定**: SendMessageProcessor:304-318 — oriProps 取 TRAN_MSG → `Boolean.parseBoolean(traFlag)` 且非 (reconsumeTimes>0 && delayLevel>0) [4.6.1 兼容: 重试消息不当 prepare] → **rejectTransactionMessage=true → NO_PERMISSION 拒收** (L308-314) → 走事务面
- **改道** (TransactionalMessageBridge.parseHalfMessageInner:219-233):
  1. UNIQ_KEY 复制为 `__transactionId__` 属性 (L220-223)
  2. **REAL_TOPIC + REAL_QID 备份** (L224-226)
  3. sysFlag `resetTransactionValue(..., TRANSACTION_NOT_TYPE)` (L227-228) — 清事务位
  4. **topic → RMQ_SYS_TRANS_HALF_TOPIC + queueId=0** (L229-230)
  5. propertiesString 重编码
- **半 topic 就绪**: TopicConfigManager:195-215 — HALF/OP topic 各 **1 读 1 写队列**; TopicValidator:62-64 — 三个事务 topic 禁客户端发送
- **存储**: 半消息走普通 putMessage → CommitLog (PreparedTransactionOffset 段在 RM-3 已见); 返回 SEND_OK 才执行本地事务
- **响应**: SendMessageResponseHeader.setTransactionId = MessageClientIDSetter.getUniqID (SendMessageProcessor:485)

## 代码类型
Implementation (改道 + 属性备份)

## 跨域关联
- RM-3 (存储): PreparedTransactionOffset 18 段第 14 段 / 消息编码
- RM-4 (延迟): 延迟属性清空交互 (事务+延迟互斥)
- RM-5 (Broker): initialTransaction SPI (ServiceProvider, BrokerController:989-1006)

## 结论
半消息 = 真实消息"换皮"落盘到 HALF topic (1 队列) + REAL_* 属性留还原现场; TRAN_MSG 属性是触发改道的开关。
源码位置: SendMessageProcessor.java:304-318; TransactionalMessageBridge.java:219-233; TopicConfigManager.java:195-215
