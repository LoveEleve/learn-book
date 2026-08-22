# Kafka-44 重写规划

> 题目：限流架构怎么串起来——QuotaFactory、ClientQuotaManager 与请求节流主链

## 1. 读者困惑
- Kafka 限流到底在哪里生效？
- throttleTimeMs 是谁算出来的？
- 普通请求、复制流量、控制面是不是一套配额？

## 2. 一句话顿悟
**Kafka 在 broker 启动时由 QuotaFactory 统一装配多类 quota manager；普通请求经 RequestHandlerHelper 记账并计算 throttleTimeMs，再以延迟响应的方式节流；复制和控制面走各自 quota manager。**

## 3. 失败方案推演
- 只在 handler 里零散判断 throttle，无法统一记账
- 把复制流量与普通请求混在一套配额里

## 4. 章节问题
- QuotaFactory 为什么先装配？
- RequestHandlerHelper 为什么是统一入口？
- ReplicationQuotaManager 为什么独立？

## 5. 至少要排除的误解
- 限流就是拒绝请求
- 只有一种 request quota
- quota 不能动态更新

## 6. 关键证据清单
- `core/src/main/scala/kafka/server/BrokerServer.scala:200`
- `core/src/main/scala/kafka/server/RequestHandlerHelper.scala:34`
- `core/src/main/scala/kafka/server/RequestHandlerHelper.scala:111`
- `core/src/main/scala/kafka/server/ReplicaManager.scala:2629`

## 7. 版本与实现边界
- Kafka v4.x
- 只抓装配与节流主链，不展开采样窗口细节

## 8. 字数预算
- 5000~8000 字