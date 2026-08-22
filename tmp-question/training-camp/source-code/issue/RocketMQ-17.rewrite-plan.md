# RocketMQ-17 重写规划

> 题目：Broker 注册到 NameServer 为什么不是“一次心跳”——路由发布全流程
> 状态：补骨架篇。承接 RocketMQ-16，把 Broker 侧如何把 Topic/Broker/集群路由信息发布进 NameServer 讲成独立主链。

## 1. 读者困惑
- Broker 注册到 NameServer 到底注册了什么？
- 为什么不是简单“发个地址过去”就完了？
- TopicConfig、QueueData、BrokerData 怎么串起来？
- registerBrokerAll 和周期上报的关系是什么？

## 2. 一句话顿悟
**Broker 注册到 NameServer 不是“一次报活心跳”，而是把 Broker 自身、Topic 队列配置、集群归属、主从地址等路由元数据整体发布出去；NameServer 再把它们折叠进 RouteInfoManager，供 Producer 查询 TopicRouteData。**

## 3. 失败方案推演
- 把注册误解成单纯存活上报
- 只看 Broker 地址，不看 Topic 队列配置
- 只看 NameServer 侧 map，不看 Broker 上报入口

## 4. 章节问题
- registerBrokerAll 在什么时候触发？
- Broker 注册包里带了哪些信息？
- NameServer 收到后如何落入 RouteInfoManager？
- 为什么 Producer 查 topic 时能拿到 QueueData/BrokerData？

## 5. 至少要排除的误解
- Broker 注册 = 心跳
- NameServer 自己推导 Topic 路由
- 注册只发生在启动时
- QueueData 与 BrokerData 没关系

## 6. 关键证据清单
- `broker/.../BrokerController`
- `broker/.../BrokerOuterAPI`
- `namesrv/.../DefaultRequestProcessor`
- `namesrv/.../RouteInfoManager`

## 7. 版本与实现边界
- RocketMQ 5.x / 4.x 通用主链
- 本篇聚焦注册主链，不展开 Producer 本地缓存刷新（RocketMQ-24）

## 8. 字数预算
- 6000~9000 字