# RM-9 Rebalance+offset+LitePull — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | RebalanceService (20s 周期) + AVG 分配算法 + 有序锁 (LOCK_BATCH_MQ) + computePullFromWhere 起点 |
| 4.x | 算法族扩展 (ConsistentHash/MachineRoom/ByConfig); 广播模式; RebalancePushImpl 细化 |
| **5.0** | **broker 中心化分配** (queryAssignment: 策略名上报 + 3s×3 重试; topicClientRebalance/topicBrokerRebalance 双表); **RebalanceLitePullImpl** (LitePull 手动消费); **1s 快速收敛** (不平衡时) |
| 5.x | Nearby 算法 (机房间就近, 129 行); updateAndFreezeOffset (冻结修复面, RM-8 交叉) |

## 痕迹证据

- RebalanceService.java:22-40: 20s/1s 自适应 (系统属性)
- RebalanceImpl.java:63-64: QUERY_ASSIGNMENT_TIMEOUT=3s / TIMEOUT_CHECK_TIMES=3
- RebalanceImpl.java:67: 双表 (client/broker)
- AllocateMessageQueueAveragely.java: AVG 算法 (余数处理注释风格)
- RebalancePushImpl.java:166-230: 起点 5 模式
- AllocateMachineRoomNearby.java: 129 行 (5.x)
- DefaultLitePullConsumerImpl.java:105: subscribe/assign 互斥

## 推断标注

- "3.x 骨架" — RocketMQ 公知版本线 (标注)
- "4.x 算法族" — 特性年代推断 (标注)
- "5.0 broker 分配/LitePull/1s" — 与 5.x 同代推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
