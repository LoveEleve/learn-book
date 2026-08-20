# RocketMQ-2 重写规划

> 题目：Broker 为什么不是“开个端口就行”——Broker 启动主链与宿主能力
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成 RocketMQ 主链里“Broker 作为宿主”专题，而不是 BrokerController 初始化步骤清单

## 1. 读者困惑

- Broker 为什么不是一个“开个 Netty 端口收消息”的轻量节点？
- `BrokerController.initialize()` 做的事情为什么决定后面 Producer 发送、Pull、长轮询、事务、延迟、HA 能不能成立？
- Broker 启动阶段到底在装配哪些能力，它们为什么必须在请求到来前先准备好？
- 为什么这一篇应该在 Producer/CommitLog 细节之前写？

## 2. 一句话顿悟

**RocketMQ 的 Broker 并不是转发站，而是消息系统的宿主：`BrokerStartup` 与 `BrokerController.initialize()/start()` 做的不是“把进程跑起来”这么简单，而是提前把请求处理器、消息存储、长轮询、事务检查、定时任务、HA 和对外注册这些能力装配好，让后续所有消息主链专题都有地方落脚。**

## 3. 总图

```text
BrokerStartup
  → BrokerController.initialize()
    → 配置/存储/线程池/Processor/长轮询/事务/调度/HA 装配
      → BrokerController.start()
        → 对外服务 + 对 NameServer 注册 + 后台任务运行
          → Producer / Pull / 事务 / 延迟 / HA 都在这个宿主内继续展开
```

## 4. 关键边界

- 本篇只讲 Broker 为什么是“宿主能力容器”，不细抠 CommitLog 刷盘、NameServer 路由缓存、事务消息回查、DLedger 协议细节。
- 重点回答“为什么这些能力必须在启动时先装好”，而不是逐个讲模块实现。
- 不把 Broker 启动写成 Spring Boot 风格 auto-config 清单，也不写成 Netty Server 生命周期流水账。
- Producer 发送 / Pull / 延迟 / 事务 / HA 在本篇只作为挂载能力出现，后文再拆专题。

## 5. 本轮重写主线

1. 用“Broker 不是开个端口收包就行”开场。
2. 否定：Broker 只是转发站、Producer/Consumer 主链可以不依赖 Broker 宿主能力、初始化顺序只是样板代码。
3. 先讲 BrokerStartup -> BrokerController 为什么是总入口。
4. 再讲 initialize() 装配的几类能力：存储、处理器、长轮询、事务、调度、HA、注册。
5. 最后讲 start() 为什么把“已经装好能力”变成真正可服务节点。
6. 收网时明确：后续 Producer/CommitLog/Pull/事务/HA 篇都是在这篇宿主上继续拆。