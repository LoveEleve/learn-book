# 阶段 4（消息与事务）详细交接文档

> 交接对象：下一个 AI
> 交接日期：2026-08-20
> 适用范围：`源码分析执行计划.md` 中的**阶段 4：消息与事务**，当前重点是 `RocketMQ` 正文执行与阶段 4 规划收口
> 当前源码基线：
> - RocketMQ：`/data/workspace/source-code/code/spring/rocketmq/`
> - 阶段 4 规划目录：`/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/`

---

## 1. 当前阶段判断

这次工作已经不再是“先扫仓库、列域清单”的阶段，而是已经进入：

1. **阶段 4 总体结构已经方法论化重构完成**
2. `RocketMQ` 已经从仓库级规划进入**正文级执行**
3. 其他仓库（Kafka / ZooKeeper / Seata / Curator / SofaJRaft）目前还停留在**深度规划完成、未进入正文**的状态

要特别注意区分两种状态：

### A. 已完成到“正文执行级”的对象

- `RocketMQ`
  - 已做仓库级范围规划 review
  - 已做“正文入口级重排”
  - 已把 10 域展开成 `24~28` 篇正文顺序
  - 已经实际产出前 `12` 篇正文中的一部分（详见后文）

### B. 仍停留在“阶段规划级”的对象

- `Kafka`
- `ZooKeeper`
- `Seata`
- `Curator`
- `SofaJRaft`

这些仓库已经做过一到两轮方法论级 review，并且都已回写到 `源码分析执行计划.md`，但**还没有开始逐篇正文**。

---

## 2. 一定先读什么

按顺序阅读：

1. 本文档：
   - `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/HANDOFF-MQ-STAGE4-DETAILED.md`
2. 阶段 4 总计划：
   - `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/源码分析执行计划.md`
3. RocketMQ 专项范围规划：
   - `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ源码学习范围规划.md`
4. 方法论文档：
   - `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/源码范围规划复盘方法论.md`
5. 如果要继续正文级执行，先读已经写出来的 `RocketMQ-1 ~ RocketMQ-12`

---

## 3. 阶段 4 的当前方法论骨架

已经正式补进 `源码分析执行计划.md` 的 4 条主线是：

1. **消息主链**
   - 生产 -> 路由 -> 存储 -> 投递 -> 消费 -> 重试/顺序/延迟/过滤
   - 代表仓库：`RocketMQ`、`Kafka`
2. **一致性主链**
   - 选主 -> 广播/复制 -> 提交 -> apply -> 快照/恢复 -> 成员变更
   - 代表仓库：`ZooKeeper`、`SofaJRaft`
3. **分布式事务主链**
   - 全局入口 -> 上下文传播 -> RM 执行 -> undo/锁/phase2 -> TC 协调 -> 恢复/存储
   - 代表仓库：`Seata`
4. **ZK 上层应用链**
   - 裸原语 -> recipes -> 工程化封装
   - 代表仓库：`ZooKeeper`、`Curator`

当前阶段 4 不再按“仓库平推”来理解，而是按以上主线推进。

---

## 4. RocketMQ 当前状态

### 4.1 RocketMQ 已完成的规划层工作

已经完成并正式回写到文档的有：

- `RocketMQ` 的 10 域骨架重排
- `RocketMQ` 的“正文执行顺序（24~28 篇建议版）”
- 第一篇不再从 `Broker启动` 开始，而是先写：
  - `RQ-1 一条消息怎样从 Producer 走到 Consumer —— RocketMQ 主链总图`

这些都已经落到：
- `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/源码分析执行计划.md`
- `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ源码学习范围规划.md`

### 4.2 RocketMQ 已完成的正文

当前已经写出的 RocketMQ 正文有：

1. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-1.md`
   - 一条消息怎样从 Producer 走到 Consumer —— 主链总图
2. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-2.md`
   - Broker 为什么不是“开个端口就行”——Broker 启动主链与宿主能力
3. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-3.md`
   - Producer send 以后，到底是谁决定发往哪个 Broker —— 路由发现与发送主链
4. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-4.md`
   - CommitLog 为什么是消息真正的落点 —— 顺序追加写主链
5. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-5.md`
   - ConsumeQueue 为什么不是“另一份日志”，而是消费索引桥
6. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-6.md`
   - Consumer 为什么不是“读到消息就算完”——拉取、ProcessQueue 与消费推进主链
7. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-7.md`
   - Push、LitePull、Rebalance 为什么不是三套平行模型
8. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-8.md`
   - 顺序消息为什么只能保证“分区内有序”
9. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-9.md`
   - 顺序消费失败以后，为什么不能简单重试
10. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-10.md`
   - 延迟消息为什么不是 Broker 睡一会再发
11. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-11.md`
   - delay level、专用 Topic 和定时扫描怎样把消息重新投递出来
12. `/data/workspace/source-code/book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-12.md`
   - 消息过滤为什么不只是“消费者收到以后自己丢掉”

### 4.3 RocketMQ 已完成的 plan 文件

对应已写正文，目前已有这些 `rewrite-plan`：

- `RocketMQ-1.rewrite-plan.md`
- `RocketMQ-2.rewrite-plan.md`
- `RocketMQ-3.rewrite-plan.md`
- `RocketMQ-4.rewrite-plan.md`
- `RocketMQ-5.rewrite-plan.md`
- `RocketMQ-6.rewrite-plan.md`
- `RocketMQ-7.rewrite-plan.md`
- `RocketMQ-8.rewrite-plan.md`
- `RocketMQ-9.rewrite-plan.md`
- `RocketMQ-10.rewrite-plan.md`
- `RocketMQ-11.rewrite-plan.md`
- `RocketMQ-12.rewrite-plan.md`

### 4.4 RocketMQ 正文执行的真实方法

这里已经形成了一套稳定执行方式，后续 AI 不要再改回“先写成稿再说”或“每一步都停下来问”两种低效方式。

当前采用的是：

- **单篇闭环执行**
- 但单篇内部仍然完整走：
  - `rewrite-plan -> deep review -> 正文成稿 -> review findings -> 深修`
- 只是这些步骤在一次连续推进里完成，不再拆成多轮对话

### 4.5 RocketMQ 当前最重要的写作风格判断

这一组正文已经逐渐形成自己的写法，后续必须保持一致：

1. **不要上来就写组件说明书**
   - 先从“这一步如果没有，会先在哪失败”切入
2. **主链推进优先**
   - 尽量写成：没有这一层，主链会先坏在哪
3. **每篇只回答一个核心困惑**
   - 例如：
     - CommitLog 解决的是统一真相层
     - ConsumeQueue 解决的是消费索引桥
     - Rebalance 解决的是责任队列归属
4. **不要让后文主题越界提前吞掉**
   - 比如：
     - `RQ-4` 不抢 `ConsumeQueue`
     - `RQ-6` 不吞 `Push/LitePull/Rebalance`
     - `RQ-10` 不吞 `RQ-11`
5. **大量使用“失败推进链”来立住结构**
   - 不是空泛解释，而是：
     - 没这一层，会先在哪一层失败

---

## 5. RocketMQ 下一步该怎么继续

### 5.1 不是回头大修前 12 篇

当前前 12 篇已经完成一轮“plan -> 写作 -> 深修”。它们不是完美定稿，但已经不该回头推倒重写。

正确姿势是：

- 继续往后写
- 等一批主链和变体专题都齐了，再统一做二轮 consistency review

### 5.2 下一篇建议直接继续

下一篇应该继续写：

- `RocketMQ-13：单机 CommitLog 为什么还不等于可靠消息 —— Master/Slave 复制主链`

为什么不是回头修旧文：

- 现在消息主链和变体主线已经有了
- 接下来最自然的是进入“一致性/可靠性”层
- 而 `Master/Slave` 正是这一层的第一篇入口

### 5.3 RocketMQ 后续推荐顺序

建议继续按当前执行顺序推进：

13. `RocketMQ-13：单机 CommitLog 为什么还不等于可靠消息 —— Master/Slave 复制主链`
14. `RocketMQ-14：DLedgerCommitLog 为什么把 RocketMQ 拉进了 Raft 世界`
15. `RocketMQ-15：Controller、选主和角色切换怎样决定 Broker 谁能继续写`
16. `RocketMQ-16：消息系统真正难的不是“能发出去”，而是失败以后怎样恢复 —— RocketMQ 故障恢复总串联`
17. `RocketMQ-17：事务消息为什么不能直接发正式消息 —— 半消息主链`
18. `RocketMQ-18：Producer 本地事务执行完了，Broker 为什么还要回查`
19. `RocketMQ-19：Broker 二阶段 commit/rollback 怎样把事务消息真正落稳`

后面再按情况决定是否进入 `RQ-22~28` 的补深篇。

---

## 6. 其他仓库当前状态

### Kafka

已完成：
- 12 域重排
- 子专题展开
- 预计篇数：`28~34`

状态：**规划完成，未开始正文**

### ZooKeeper

已完成：
- 9 域重排
- 子专题展开
- 预计篇数：`15~18`

状态：**规划完成，未开始正文**

### Seata

已完成：
- 从 13 域重构为 16 主域
- 子专题展开
- 预计篇数：`26~32`

状态：**规划完成，未开始正文**

### Curator

已完成：
- 从 5 域重构为 6 主域
- 子专题展开
- 预计篇数：`10~14`

状态：**规划完成，未开始正文**

### SofaJRaft

已完成：
- 从 5 域重构为 7 主域
- 子专题展开
- 预计篇数：`12~16`

状态：**规划完成，未开始正文**

---

## 7. 阶段 4 当前最重要的阶段结论

### 已经完成的方法论工作

- 阶段 4 不再是“仓库并排列表”，而是已经重构成 4 条主线：
  1. 消息主链
  2. 一致性主链
  3. 分布式事务主链
  4. ZK 上层应用链
- 这层总结构已经正式落进：
  - `源码分析执行计划.md`

### 当前唯一进入正文执行的仓库

- `RocketMQ`

### 当前最应该继续的事

- 继续顺着 RocketMQ 正文往后推进
- 不要突然切去 Kafka / ZooKeeper / Seata 写正文
- 等 RocketMQ 第一轮写得更完整之后，再考虑是否切到 Kafka

---

## 8. 下一任 AI 的正确接手方式

### 如果继续 RocketMQ

必须严格按：

1. 先读本文档
2. 读 `RocketMQ源码学习范围规划.md`
3. 读已写出的 `RocketMQ-1 ~ RocketMQ-12`
4. 从 `RocketMQ-13` 继续
5. 单篇内部继续执行：
   - `rewrite-plan -> deep review -> 正文 -> deep fix`
   - 但要一次性闭环推进，不要拆成三轮对话

### 不要做的事

- 不要回到“先讲类，再讲方法”的源码说明书路线
- 不要把 `RocketMQ-13` 之前已经写过的篇再从零推翻
- 不要跳去 Kafka 或 Seata 直接开正文
- 不要把阶段 4 再理解成“按仓库顺序平推”
- 不要丢掉“失败推进链”这个已经形成的写作骨架

---

## 9. 一句话交接总结

当前阶段 4 的方法论重构已经完成，RocketMQ 已正式进入正文执行，并完成前 12 篇；下一任 AI 最正确的接手方式，不是重做规划，也不是切新仓库，而是直接从 `RocketMQ-13` 开始，沿着已经建立好的消息主链 -> 变体专题 -> 一致性/可靠性 -> 事务压轴顺序继续往下写。