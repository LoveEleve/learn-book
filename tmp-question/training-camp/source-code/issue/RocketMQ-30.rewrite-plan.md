# RocketMQ-30 重写规划

> 题目：Proxy 为什么不是“转发层”——Proxy 架构与宿主职责总览
> 状态：5.x 特色骨架篇。对应 `cqfy` 中 Proxy 启动、Netty/gRPC、代理收发等主题，把 Proxy 放回 RocketMQ 全局主链。

## 1. 读者困惑
- RocketMQ 5.x 为什么要引入 Proxy？
- Proxy 只是做一次网络转发吗？
- Proxy 和 Broker、NameServer、客户端之间的职责边界是什么？
- 为什么 Proxy 既要管生产者连接，也要管消费者 / Pop / 路由查询？

## 2. 一句话顿悟
**RocketMQ 5.x 的 Proxy 不是“多了一个中转层”这么简单，而是把客户端接入、协议适配、路由查询、Broker 代理访问、消费模型（尤其 Pop）等职责收敛到一个独立宿主里，让 Broker 更聚焦存储与核心消息处理。**

## 3. 失败方案推演
- 把 Proxy 理解成纯转发，忽略它的运行时状态管理
- 只看网络入口，不看它与路由、Broker 代理、消费模型的耦合
- 把 Proxy 和 Broker 宿主边界混成一层

## 4. 章节问题
- Proxy 为什么出现？
- 它的宿主职责有哪些？
- 它和 Broker/NameServer/客户端的主链怎么接？
- 为什么 Pop/Ack/ck 等 5.x 特色功能会强依赖 Proxy？

## 5. 至少要排除的误解
- Proxy 只是四层转发
- 引入 Proxy 后 Broker 就不再参与客户端语义
- Proxy 只服务生产者
- Proxy 与 Pop 无关

## 6. 关键证据清单
- `proxy/.../ProxyStartup`
- `proxy/.../ProxyController`
- `proxy/.../service` and grpc/netty server entry classes
- `client/broker/namesrv interaction classes as needed`

## 7. 版本与实现边界
- RocketMQ 5.x 为主
- 本篇是 Proxy 总览，不展开 Pop 细节（留给 RocketMQ-31/32）

## 8. 字数预算
- 6000~9000 字