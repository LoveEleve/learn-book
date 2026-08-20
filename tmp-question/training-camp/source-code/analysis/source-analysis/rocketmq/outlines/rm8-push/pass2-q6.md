# 闭环笔记 q6: 5.x POP 消费模式 + 测试面

## 假设
POP = 5.x 新消费协议 (拉取→可见性窗口→ACK); 测试覆盖消费面。

## 验证过程
- **POP 模式** (5.x, ConsumeMessagePopConcurrentlyService 484 + PopOrderlyService 407 + **PopProcessQueue 84**):
  - **PopProcessQueue**: 可见性窗口语义 (消息 pop 后不可见 → 消费 ACK 确认 / 超时重投) — **ack()** (L248-254)
  - 对照: 传统 Push = pull + offset; POP = pop + ack (类似 Kafka consumer 语义)
  - broker 侧 PopMessageProcessor + PopLongPollingService (RM-5 已见 pop 三服务)
- **测试面** (client consumer 测试 5345 行): DefaultMQPullConsumerTest / **rebalance 6 测试** (AllocateMessageQueueAveragely/AveragelyByCircle/MachineRoom/ByConfig/ConsitentHash + Nearby — RM-9 交叉) / store (ControllableOffset)
- **消费服务三选**: Concurrently (默认) / Orderly / Pop (5.x)

## 代码类型
Interface (消费协议演进)

## 跨域关联
- RM-9 (Rebalance): 6 分配算法测试
- RM-5 (Broker): pop 处理器

## 结论
5.x POP = pop+ack 消费协议 (可见性窗口, 对照 Kafka); 测试 5345 行含 6 种分配算法 (RM-9 素材)。
源码位置: ConsumeMessagePopConcurrentlyService.java:53-484; PopProcessQueue.java; client/src/test/consumer 5345 行
