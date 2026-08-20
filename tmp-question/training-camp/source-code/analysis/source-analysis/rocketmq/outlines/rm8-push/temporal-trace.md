# RM-8 消费-Push — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | PushConsumer 骨架: PullMessageService + ProcessQueue (TreeMap 缓冲) + 长轮询 (broker 挂 15s) + 并发消费 |
| 4.x | **ConsumeMessageService 化** (Concurrently/Orderly 双实现 + 队列锁); offsetStore 抽象 (集群/广播); 流控面 (span/阈值) |
| **5.0** | **POP 消费模式** (ConsumeMessagePopConcurrently/Orderly + PopProcessQueue ack 语义 — pop+ack 协议, 对照 Kafka); **MessageRequest 队列重构** (PullMessageService 泛化) |
| 5.x | ControllableOffset (可控偏移测试面); 服务探测交叉 |

## 痕迹证据

- PullMessageService.java:33: messageRequestQueue (5.x 重构痕迹)
- DefaultMQPushConsumerImpl.java:101-115: 延迟分级 + 长轮询参数
- DefaultMQPushConsumer.java:176-217: 缓存阈值 (1000 条/100MiB/span 2000)
- ProcessQueue.java:46-53: TreeMap 双缓冲
- ConsumeMessageOrderlyService.java:63: messageQueueLock
- PopProcessQueue.java: ack() (5.x)
- client/src/test/consumer: 5345 行 (rebalance 6 算法)

## 推断标注

- "3.x 骨架" — RocketMQ 公知版本线 (标注)
- "4.x 服务化/offsetStore/流控" — 特性年代推断 (标注)
- "5.0 POP/MessageRequest" — 与 proxy 同代推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
