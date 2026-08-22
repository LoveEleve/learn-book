# RocketMQ-27 重写规划

> 题目：Topic 为什么天然分片——Queue 数据分片与路由选择主链
> 状态：骨架补深篇。对应 `hz` 中 Topic 数据分片机制主题，承接 RocketMQ-3、16、17，把 TopicRouteData、QueueData、MessageQueue 以及 Producer 选队列逻辑串成独立主链。

## 1. 读者困惑
- RocketMQ 为什么不是“一个 Topic 一个日志”？
- Topic 下为什么会有多个 queue？
- Producer 最终是怎么从 TopicRouteData 选中某个 MessageQueue 的？
- 顺序消息和普通消息在选 queue 时有什么差异？

## 2. 一句话顿悟
**RocketMQ 天然把 Topic 拆成多个逻辑 queue；NameServer 返回的不是“topic 对应哪个 broker”这种单点答案，而是 TopicRouteData → QueueData/BrokerData 这份分片路由视图。Producer 再把它展开成 MessageQueue 列表，按普通发送、故障规避或顺序选择器挑一个 queue 发。**

## 3. 失败方案推演
- 把 Topic 理解成单日志，无法理解并行发送与消费
- 只看 Broker 地址，不看 queue 级分片
- 把顺序消息选队列和普通轮询混成一套逻辑

## 4. 章节问题
- QueueData 在 Topic 分片里代表什么？
- MessageQueue 是怎么从 TopicRouteData 派生出来的？
- Producer 普通发送如何选队列？
- 顺序消息为什么要求同 key 落同 queue？

## 5. 至少要排除的误解
- Topic = 单一物理日志
- Producer 只需要知道 broker 地址
- 顺序消息是 broker 侧自动全局排序
- 队列分片和路由缓存无关

## 6. 关键证据清单
- `remoting/.../TopicRouteData`
- `remoting/.../QueueData`
- `common/.../message/MessageQueue`
- `client/.../TopicPublishInfo`
- `client/.../producer/DefaultMQProducerImpl`

## 7. 版本与实现边界
- RocketMQ 5.x / 4.x 通用主链
- 本篇聚焦 Topic 分片与 Producer 选队列，不展开故障规避细节（RocketMQ-22）

## 8. 字数预算
- 6000~9000 字