# Kafka-47 重写规划

> 题目：容量该怎么算——从 batch、segment、flush、fetch 到 quota 的估算路径
> 状态：源码 + 运维桥接篇。把吞吐、磁盘、分区数、批量、复制与限流这些常见容量问题，沿 Kafka 的关键实现边界串成一条估算主链。

## 1. 读者困惑
- Kafka 容量评估到底该从 producer、broker 还是 consumer 开始算？
- 为什么同样 TPS，批量大小不同，broker 压力差这么多？
- segment、flush、fetch、quota 分别影响什么？
- 分区数为什么会改变 CPU / 内存 / 磁盘放大？

## 2. 一句话顿悟
**Kafka 容量不是“每秒消息数 × 单条大小”这么简单，而是要沿 producer 批量聚合、broker append/segment、复制链路、consumer fetch、quota 限流五层边界一起估：批越大，每条协议与索引开销越低；分区越多，索引、活跃 segment、请求扇出与元数据开销越高；副本数和 acks 决定复制放大；fetch / quota 决定读写端的回压上限。**

## 3. 失败方案推演
- 只按 TPS × bytes/s 估磁盘，不看副本放大与保留时间
- 只看磁盘容量，不看分区数导致的活跃 segment / index / page cache 压力
- 只看 producer 发送量，不看 follower 复制流量
- 只看 broker 写入，不看 consumer fetch 与 quota 回压

## 4. 章节问题
- batch 为什么会改变容量与吞吐拐点？
- 分区数为什么不仅影响并发，还影响内存与索引负担？
- segment / flush / retention 在容量里分别代表什么？
- follower replication 为什么会放大带宽与磁盘 IO？
- quota 为什么是容量保护阀，而不是“性能差才开的东西”？

## 5. 至少要排除的误解
- 容量评估只看磁盘总量
- 分区越多越好
- 批量只影响 producer，不影响 broker 容量
- 副本数只增加可靠性，不增加容量消耗
- quota 只是限客户端，不影响整体容量边界

## 6. 关键证据清单
- `clients/.../RecordAccumulator`
- `storage/.../LogSegment` / `OffsetIndex` / `TimeIndex`
- `core/.../ReplicaManager`
- `core/.../DelayedFetch`
- `core/.../RequestHandlerHelper` / `QuotaFactory`

## 7. 版本与实现边界
- Kafka v4.x
- 源码 + 运维桥接篇：给估算路径，不伪装成容量压测报告

## 8. 字数预算
- 7000~10000 字