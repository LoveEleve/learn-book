# RocketMQ-32. Ack、ck、revive 为什么必须同时存在——Pop 确认与恢复闭环

> 场景：前一篇已经讲清，Pop 真正改变的不是“怎么拿消息”，而是“拿到消息以后，服务端如何继续持有一段消费确认语义”。但如果继续追问，就会出现一个更尖锐的问题：既然已经有 `ack`，为什么还要 `ck`、invisibility、`revive`、死信这些看上去更复杂的东西？本篇把这条闭环彻底拆开：Pop 为什么不能只靠一个 ack 回执就成立。

## 先把真正的困惑摆出来：为什么 Pop 不能简单变成“拿消息 + 成功后 ack”

如果只追求“客户端告诉服务端消费成功了”，那 Pop 看起来似乎只需要做两件事：

1. Broker 把消息交给客户端；
2. 客户端消费完以后发一个 ack。

但一旦把失败场景加进来，这个模型马上就不够：

- 如果客户端拿走消息后崩了，谁知道这批消息还没处理完？
- 如果一段时间都没 ack，Broker 怎么判断是“还在处理”还是“已经丢了”？
- 如果 invisibility 到期了，谁来把消息重新放回可投递状态？
- 如果重试多次还是失败，什么时候该进死信？

这说明 Pop 真正要解决的不是“成功了怎么回执”，而是：**从拿走消息开始，到成功确认、超时恢复、最终转死信，这一整段中间态如何被服务端托管。**

*关键设计（斜体）：* *Pop 闭环之所以必须同时有 ack、ck/checkpoint、invisibility 和 revive，是因为服务端必须同时回答四个问题：谁拿走了消息、这段持有状态持续多久、成功时如何显式结束、失败/超时时如何恢复。ack 只解决“成功结束”这一问；ck 与 invisibility 负责定义中间态；revive 则负责把超时未完成状态重新拉回可投递世界。*[模式: Pop 获取消息 + 服务端持有中间态 + ack 成功结束 + revive 超时恢复]

## 第一层：`ack` 解决的是“成功结束”，不是“整个生命周期管理”

`AckMessageProcessor` 的意义很直接：客户端处理完成后，显式告诉服务端这批消息对应的持有状态可以结束。

所以 ack 的真实职责不是普通“我收到啦”回执，而是：

- 关闭一段由 Pop 建立的消息持有状态；
- 告诉服务端这批消息不需要再进入 revive / 重投递路径；
- 让这次 Pop 消费在确认语义上真正闭环。

但 ack 只能解决“成功完成”的那条支路。一旦客户端没有及时 ack，问题就立刻落到别的机制头上。

## 第二层：为什么必须有 `ck` / checkpoint

如果服务端要持有“这批消息已经被某客户端取走，但尚未确认完成”的中间态，它至少要记住：

- 哪些消息被拿走了；
- 属于哪个消费组/客户端上下文；
- 什么时候开始进入 invisibility；
- 这段不可见窗口持续多久。

这就是 `ck` / checkpoint 语义存在的原因。它不是多造一个术语，而是服务端托管中间态所需的**检查点落点**：这类状态不是纯脑内逻辑，而会以服务端可追踪、可恢复的消息/状态记录形式存在，供后续 invisibility 判断与 revive 恢复继续使用。

也可以换句话说：

- ack 是“成功后关闭状态”；
- ck 是“在成功之前，先把这段待确认状态安顿下来”。

没有这一步，服务端根本无从判断一条未 ack 的消息到底还处于什么生命周期位置。

## 第三层：invisibility 不是客户端超时，而是服务端维护的一段不可见窗口

Pop 的一个核心区别是：消息被取走后，并不会立刻重新对其他消费者可见。

这不是客户端自己本地记一个超时，而是服务端在消费模型里明确维护一段 **invisible time**：

- 在这段时间内，消息不应被重复投递；
- 这段时间如果客户端成功 ack，状态关闭；
- 如果快到期但客户端还在处理，可能需要通过 `ChangeInvisibleTimeProcessor` 延长；
- 如果最终超时未完成，就进入 revive 判断。

所以 invisibility 是 Pop 消费语义的核心边界，它把“被拿走但尚未完成”变成了一个服务端可感知、可调节、可恢复的窗口。

## 第四层：为什么 `ChangeInvisibleTime` 是闭环里的调节阀

真实消费并不总能在第一次 invisibility 窗口内完成。

这时就出现了一个关键中间动作：**不是马上 ack，但也不能让这条消息立刻过期重投。**

`ChangeInvisibleTimeProcessor` 之类的入口，解决的正是这个问题。但它并不是脱离上下文、凭空修改一个超时值，而是建立在前面已经存在的 receipt / ck / invisibility 状态之上：

- 延长某次 Pop 持有状态的 invisible time；
- 让服务端继续维持“这批消息暂不可见”；
- 给客户端留出继续处理的窗口。

这说明 Pop 闭环不是只有“成功 ack / 失败超时”两个硬状态，中间还存在“**我还没完成，但请别现在就把消息放回去**”的调节动作。

## 第五层：为什么 `revive` 不是定时重试线程，而是闭环的下半场

当 invisibility 最终超时，且客户端没有 ack，也没有把 invisible time 合理延长时，服务端就必须决定这批消息怎么办。

如果没有 revive，这批消息会永久卡在“曾被取走”的中间态。

所以 `PopReviveService` 一类机制的职责不是简单“做点重试”，而是：

- 扫描超时未完成的 Pop 中间态；
- 判断消息该重新投递、重新进入可消费路径，还是继续其他恢复流程；
- 达到阈值后转死信。

所以 revive 是 Pop 闭环真正的下半场：**它把失败或失联状态重新拉回到可投递或终结状态。**

## 第六层：死信不是额外附送功能，而是“多次无法闭环”的终点

一条消息如果：

- 被 Pop 拿走；
- 多次未成功 ack；
- revive 后仍然继续失败；

那就不能无限在主消费路径里兜圈子。

这时死信队列的意义才出现：它不是顺便加上的补丁，而是 Pop 闭环最终必须具备的终点。

否则 RocketMQ 就只能在“重投”与“继续超时”之间无限摆动。

## 第七层：把闭环压缩成四问，就能看出每个组件各补了哪一个洞

Pop 闭环可以压成四个连续问题：

### 1) 谁拿走了消息？
- `PopMessageProcessor` 建立这次取消息行为；
- 同时形成服务端需要跟踪的中间态。

### 2) 这段中间态如何被标记与持有？
- `ck` / checkpoint 语义落地；
- invisibility 窗口被建立。

### 3) 成功时如何显式结束？
- `AckMessageProcessor` 关闭这段持有状态。

### 4) 失败/超时后如何恢复？
- `ChangeInvisibleTime` 调整窗口；
- `PopReviveService` 扫描并恢复；
- 多次失败后进入死信。

只要把这四问连起来看，ack、ck、revive 为什么必须同时存在就一目了然。

## 收网：Pop 闭环不是“有个 ack 就够了”，而是完整的中间态管理

把整篇压成一句话：RocketMQ 5.x 的 Pop 要真正闭环，不能只靠 `ack`；因为一旦服务端决定在消息被取走后继续持有中间态，就必须同时具备 checkpoint/ck 来标记这段状态、invisibility 来定义其有效窗口、`ChangeInvisibleTime` 来调节窗口、`PopReviveService` 来处理超时恢复，并在必要时把多次失败消息送入死信。ack 只是成功结束这条支路，整个模型真正难的是**如何托管并清理“未完成”状态**。

```text
Pop 取消息
  → 服务端建立持有中间态（ck / invisibility）
    → ack 成功结束
      或
    → ChangeInvisibleTime 延长窗口
      或
    → 超时未 ack
        → PopReviveService 恢复
          → 重投递 / 死信
```

**本篇的一句话困惑**：为什么 Pop 不能只靠一个 ack 回执，而必须再引入 ck、invisibility、revive、死信这些机制？

**本篇的一句话顿悟**：因为 Pop 真正难的不是“成功后怎么确认”，而是“被拿走但尚未完成”的中间态如何被服务端托管、延长、超时恢复并最终收口；ack、ck、invisibility、revive 分别对应这条闭环里的不同缺口。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“ack 就足够了。”** ack 只能关闭成功分支，不能管理未完成中间态。
2. **“ck 只是普通时间戳。”** 它是 Pop 服务端检查点语义的落点。
3. **“invisible time 是客户端本地超时概念。”** 它是服务端维护的不可见窗口。
4. **“revive 只是定时重试线程。”** 它是超时未完成状态回到可投递世界的关键恢复机制。
5. **“死信是额外附送功能。”** 它是多次无法闭环后的必要终点。

### 关键证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/processor/PopMessageProcessor.java`：Pop 取消息主入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/AckMessageProcessor.java`：ack 主入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/ChangeInvisibleTimeProcessor.java`：invisibility / checkpoint 调整入口。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PopLongPollingService.java`：Pop 等待与唤醒机制。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/PopReviveService.java`：超时恢复主链。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/PopBufferMergeService.java`：Pop 检查点/状态合并相关组件。

### 版本与实现边界

- 本文以 RocketMQ `5.x` 为主。
- 本篇聚焦 Pop 确认与恢复闭环，不重复 Proxy 总览与 Pull 对照。
- 本篇以主链解释为主，不展开每个内部状态字段与存储细节。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-31`（Pop 总览）、`RocketMQ-30`（Proxy 总览）。
- 后续桥接：可继续补 `Proxy + Pop` 全链路串联，或进入 `4.x vs 5.x` 架构对照。