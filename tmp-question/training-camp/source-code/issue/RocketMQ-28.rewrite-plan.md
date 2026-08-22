# RocketMQ-28 重写规划

> 题目：RocketMQ 存储为什么不是只有 CommitLog——存储架构总览
> 状态：骨架补深篇。对应 `hz` 的存储架构设计主题，承接 RocketMQ-4/5/11/18，把 CommitLog、ConsumeQueue、IndexFile、刷盘、恢复、定时与事务相关存储边界拉成一张总图。

## 1. 读者困惑
- 为什么 RocketMQ 不能只靠 CommitLog 就完成存储？
- CommitLog、ConsumeQueue、IndexFile 各自解决什么问题？
- 刷盘、恢复、延迟消息、事务消息为什么都会回到存储层？
- 存储架构里哪些是“消息正文”，哪些是“索引/派生结构”？

## 2. 一句话顿悟
**RocketMQ 的存储不是“一个 CommitLog 加点文件”这么简单，而是以 CommitLog 为唯一追加主链，以 ConsumeQueue 为消费索引桥，以 IndexFile 为键查询入口，再叠加刷盘、恢复、延迟、事务等附属存储机制。读写性能、可恢复性和查询能力，都是这套分层存储结构共同提供的。**

## 3. 失败方案推演
- 把 ConsumeQueue 误解成第二份消息日志
- 把 IndexFile 误解成主存储
- 把延迟/事务看成业务逻辑，不看它们对存储结构的依赖

## 4. 章节问题
- CommitLog 为什么是唯一主落点？
- ConsumeQueue 为什么是消费索引桥？
- IndexFile 解决的是什么查询问题？
- 刷盘/恢复/延迟/事务为什么都要回到存储架构总图里看？

## 5. 至少要排除的误解
- RocketMQ 存储 = CommitLog
- ConsumeQueue 是另一份消息副本
- IndexFile 决定消费顺序
- 延迟消息和事务消息与存储无关

## 6. 关键证据清单
- `store/.../CommitLog`
- `store/.../ConsumeQueue`
- `store/.../DefaultMessageStore`
- `store/.../index/IndexFile`
- `store/.../schedule/ScheduleMessageService`
- `store/.../ha` or transaction-related store paths as relevant

## 7. 版本与实现边界
- RocketMQ 5.x / 4.x 通用主链
- 本篇是存储总览，不替代 RocketMQ-4/5/11/18 的细节篇

## 8. 字数预算
- 7000~10000 字