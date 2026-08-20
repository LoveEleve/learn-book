# RocketMQ-4 重写规划

> 题目：CommitLog 为什么是消息真正的落点 —— 顺序追加写主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“RocketMQ 为什么必须先有统一顺序日志落点”的主链专题，而不是 CommitLog/MappedFileQueue/FlushManager 的实现清单

## 1. 读者困惑

- Producer 已经把消息发到 Broker 了，为什么还不能直接给 Consumer，而必须先写 CommitLog？
- CommitLog 为什么是“消息真正的落点”，而不是 Broker 里一个普通存储细节？
- `MappedFileQueue`、`appendMessage`、`FlushManager` 在主链里分别补哪一层缺口？
- 为什么后面的 ConsumeQueue、IndexFile、HA、事务消息都要建立在 CommitLog 之上？

## 2. 一句话顿悟

**RocketMQ 先把消息写进 CommitLog，不是为了多做一步，而是为了先建立一条统一、顺序、可刷盘、可复制、可重放的消息真相层；`MappedFileQueue` 负责把顺序日志落到分段文件上，`appendMessage` 负责把单条消息编进当前日志尾部，`FlushManager` 决定“写进内存页缓存”和“真正持久化”之间怎样收口。**

## 3. 总图

```text
Broker 收到发送请求
  → DefaultMessageStore / CommitLog.putMessage()
    → MappedFileQueue 找到当前写入段
      → appendMessage() 顺序追加
        → 返回写入结果
          → FlushManager 决定刷盘策略
            → 后续 Reput / ConsumeQueue / IndexFile / HA / 事务消息继续消费这条统一日志
```

## 4. 关键边界

- 本篇只讲 CommitLog 为何是统一真相层，以及顺序追加写主链；不展开 ConsumeQueue / IndexFile / HA / 事务半消息细节。
- 重点回答“为什么消息必须先进入统一日志”，而不是把它写成文件布局说明书。
- 刷盘策略在本篇只讲“为什么必须区分追加成功与真正持久化”，不展开所有实现参数细节。
- `ReputMessageService`、ConsumeQueue、IndexFile 在本篇只作为后续消费者角色出场，不抢下一篇主题。

## 5. 本轮重写主线

1. 用“消息到了 Broker，为什么还要先写 CommitLog”开场。
2. 否定：Broker 收到消息就算成功、CommitLog 只是普通文件、Consumer 可以直接读发送请求结果。
3. 先讲 CommitLog 作为统一顺序真相层的存在理由。
4. 再讲 `MappedFileQueue -> appendMessage -> FlushManager` 这条顺序追加写主链。
5. 最后讲为什么后续所有专题都依赖这条统一日志。
6. 收网时明确：下一篇再进入 ConsumeQueue，回答为什么仅有 CommitLog 仍不适合直接消费。
