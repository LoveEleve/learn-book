# RocketMQ-16 重写规划

> 题目：NameServer 为什么不是“路由表缓存”——路由元数据宿主主链
> 状态：补骨架篇。对应 `hz` 中 NameServer 架构设计主题，用来把 RocketMQ 主链里一直缺的“NameServer 到底是什么、存什么、怎么服务路由发现”补成独立骨架篇。

## 1. 读者困惑
- NameServer 为什么不是“ZK 那样的强一致注册中心”？
- Producer / Broker 都会跟 NameServer 交互，但它到底保存什么？
- NameServer 为什么可以无状态、近似对等部署？
- 它和 Broker / TopicRouteData / QueueData / BrokerData 的关系是什么？

## 2. 一句话顿悟
**NameServer 的核心不是“做强一致控制”，而是作为 RocketMQ 的路由元数据宿主：Broker 把 Topic/Broker/集群信息注册进去，Producer 再按主题去查路由。它追求的是简化、对等、可快速恢复，而不是像协调系统那样做复杂一致性裁决。**

## 3. 失败方案推演
- 把 NameServer 理解成 ZooKeeper/Etcd，误判它的设计目标
- 只知道 Producer 会查路由，但不知道路由数据结构从哪来
- 把 Broker 注册和 Producer 查询拆开看，丢失主链

## 4. 章节问题
- NameServer 存的到底是什么？
- 为什么它可以多节点近似对等而不做强一致？
- TopicRouteData / QueueData / BrokerData 分别代表什么？
- NameServer 在发送主链中扮演什么角色？

## 5. 至少要排除的误解
- NameServer 是强一致注册中心
- NameServer 负责消息转发
- Producer 每次发送都实时远程查 NameServer
- NameServer 只保存 broker 地址字符串

## 6. 关键证据清单
- `namesrv/.../NamesrvController`
- `namesrv/.../RouteInfoManager`
- `remoting/.../RemotingCommand`
- `client/.../MQClientInstance`

## 7. 版本与实现边界
- RocketMQ 5.x / 4.x 通用主链
- 本篇只讲 NameServer 架构与路由宿主职责，不展开 Broker 注册细节（留给 RocketMQ-17）

## 8. 字数预算
- 6000~9000 字