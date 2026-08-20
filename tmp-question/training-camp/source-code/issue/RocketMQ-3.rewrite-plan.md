# RocketMQ-3 重写规划

> 题目：Producer send 以后，到底是谁决定发往哪个 Broker —— 路由发现与发送主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“Producer 为什么不是拿着 topic 就能直接发消息”的主链专题，而不是 Producer API 或 MQFaultStrategy 说明书

## 1. 读者困惑

- Producer 为什么手里明明有 topic，却还不知道该把消息发给哪个 Broker？
- NameServer 在这篇里到底扮演什么角色：是每次发送都实时裁决，还是路由来源？
- `TopicPublishInfo` 为什么不是一个普通缓存对象，而是 Producer 发送前的本地路由视图？
- `selectOneMessageQueue()` 与 `MQFaultStrategy` 为什么不能被看成“随便挑个队列”的小优化？
- `SYNC / ASYNC / ONEWAY` 为什么不是三条完全不同的发送主链，而是已完成路由决策之后的三种收口方式？
- 为什么这篇必须先于 CommitLog：消息还没决定发往哪个 Broker 时，根本无从谈 Broker 侧落盘？

## 2. 一句话顿悟

**RocketMQ 的发送主链真正先解决的不是“怎么把字节发出去”，而是“当前这条消息应该发往哪个 Broker/Queue、失败时是否重选、重试时是否避开故障节点”；`TopicPublishInfo` 提供本地路由视图，`selectOneMessageQueue()` 与 `MQFaultStrategy` 共同完成本地目标决策，`sendKernelImpl()` / `MQClientAPIImpl` 只是把这次决策真正变成网络发送。**

## 3. 总图

```text
业务 send(topic, msg)
  → 本地是否已有 TopicPublishInfo 路由视图
    → 不够时才去更新 NameServer 路由
      → TopicPublishInfo 持有可写队列视图
        → selectOneMessageQueue() + MQFaultStrategy
          → 选出当前 Broker / Queue
            → sendKernelImpl()
              → MQClientAPIImpl 远程调用 Broker
                → SYNC / ASYNC / ONEWAY 三种收口方式
```

## 4. 关键边界

- 本篇只讲“Producer 如何决定发给谁并把请求送出去”，不进入 CommitLog 落盘、Consumer 拉取、事务消息、顺序消息和 HA 细节。
- NameServer 在本篇只讲“路由来源”和“路由更新触发点”，不展开其服务端内部注册表数据结构与 Broker 注册实现。
- `TopicPublishInfo` 是发送前的本地路由视图，不应写成“每次都远程获取的新鲜路由”。
- `MQFaultStrategy` 在本篇视为发送主链的一部分，不是后置优化细节；它回答的是“静态知道队列分布后，为什么仍然不能无脑轮询”。
- `SYNC / ASYNC / ONEWAY` 在本篇只讲它们如何改变发送收口方式，不写成三套完全独立的主链。

## 5. 必须打透的失败方案

1. Producer 手里有 topic，直接发给任意一个 Broker 就行。
2. 每次发送都实时去 NameServer 远程问一次路由，最准确。
3. 拿到队列列表后，简单 round-robin 就足够，失败 broker 以后再说。
4. `MQFaultStrategy` 只是性能优化，不影响发送主链正确性。
5. `SYNC / ASYNC / ONEWAY` 是三条彼此独立的发送主链。
6. 还没讲完路由发现和队列选择，就可以直接进入 CommitLog 落盘。

## 6. 本轮重写主线

1. 用“Producer 手里明明有 topic，为什么仍然不知道发给谁”开场。
2. 先否定“每次都远程问路”和“静态轮询所有队列都一样”这两个直觉。
3. 第一层写 `TopicPublishInfo`：本地路由视图怎样建立、何时更新。
4. 第二层写 `selectOneMessageQueue()`：为什么有了视图还不等于有了目标。
5. 第三层写 `MQFaultStrategy`：失败规避为什么是发送主链的一部分。
6. 第四层写 `sendKernelImpl()` / `MQClientAPIImpl`：真正把本地决策落成网络发送。
7. 最后再点 `SYNC / ASYNC / ONEWAY`：它们是发送收口模型，不是三条完全独立主链。
8. 收网时明确：下一篇才进入 CommitLog，回答消息真正写到哪里。
