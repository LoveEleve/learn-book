# RocketMQ-18. 消息系统真正难的不是“能发出去”，而是失败以后怎样恢复 —— RocketMQ 故障恢复总串联

> 场景：前面已经把消息怎样落到 CommitLog、怎样派生到 ConsumeQueue、传统 Master/Slave 怎样做副本确认、DLedger 怎样提供提交边界、Controller 怎样裁决 Broker 谁能继续写都讲清了。走到这里，读者最容易产生的错觉是：只要发送主链和选主主链讲通了，系统可靠性差不多就明白了。
>
> 本篇只回答一个问题：**RocketMQ 在失败以后，到底怎样重新承认哪些消息还算系统真相。** 本篇不再拆单个类做说明书，而是把 `CommitLog -> confirm/committed 边界 -> ConsumeQueue 截断 -> Reput 追平 -> 角色恢复 -> 路由再暴露` 这条总恢复链收成一个闭环；事务消息的恢复细节留给后文。

## 先把真正的困惑摆出来：恢复为什么比发送更难

正常路径下，消息系统很容易被理解成一条前进链：Producer 发，Broker 收，CommitLog 落，Consumer 读。只要每一段路径都能跑通，好像可靠性已经成立。

真正困难的是失败以后。

因为一旦 Broker 异常退出、主从切换、CommitLog 尾部出现半消息、ConsumeQueue 超前、Controller 把新主裁出来，系统就不能再只问“这条消息以前有没有出现过”，而必须重新回答一件更尖锐的事：**此刻系统到底还承认哪一段日志为真相。**

这里至少有几种不同但很容易混在一起的事实：

- Producer 曾经收到过成功；
- 某段字节曾经落到过磁盘；
- 某个副本曾经复制到过某个 offset；
- ConsumeQueue 曾经把某条消息索引出来；
- 当前 Broker 重启后仍决定继续承认这条消息；
- 当前 Consumer 还能重新看到它。

这些事实不是一回事。RocketMQ 的恢复逻辑真正困难的地方就在于：系统必须在故障后重新排序这些事实，决定谁还能继续被承认，谁只能被截断、回滚或重新派生。

如果恢复时只是简单地“把服务拉起来就继续跑”，主链会先在哪失败？会先失败在：**历史上出现过的某些消息和索引，可能已经不再处于当前系统可承认的一致边界内。** 这时如果仍把它们当成真相继续对外暴露，Consumer、路由和主从角色都会建立在一段并不被当前系统正式背书的数据上。

所以恢复的第一原则不是“恢复得快”，而是“先重新承认一段一致的日志真相”。

```text
Broker 重启 / 主切换 / 异常退出
  → 恢复可承认的 CommitLog 物理边界
    → 校正 confirm / committed / process 边界
      → 截断超前 ConsumeQueue
        → Reput 从正确位置追平
          → 角色与路由重新稳定
            → Producer / Consumer 再次看到新的系统真相
```

*关键设计（斜体）：* *RocketMQ 恢复时真正要做的，不是把“以前所有出现过的数据”都拿回来，而是先决定“现在还承认哪一段日志为真相”，再让索引、角色和路由围绕这段真相重新长出来。*[模式: 真相重建优先于服务重启]

## 第一层：恢复入口先把问题拆成三层，而不是一锅炖

`DefaultMessageStore.recover(lastExitOK)` 是总恢复入口。但这里必须先把时序说严：**`recover()` 负责的是把存储世界恢复到一个可继续启动的基础边界，真正把 Reput 追平、等待 `dispatchBehindBytes()==0` 并再次校正消费视图的动作，发生在后续 `start()` 阶段的 `doRecheckReputOffsetFromCq()` 里。**

也就是说，恢复总链至少分成两段：

1. `recover()`：恢复 ConsumeQueue、CommitLog 和基础 offset/TopicQueue 视图；
2. `start()`：设置 Reput 起点、等待派生消息追平、再次收束消费位点视图。

先看 `recover()` 本身，它没有把恢复写成一个大而全的“全部重来”，而是先按三层拆开：

1. 恢复 ConsumeQueue；
2. 根据上一步得到的 `maxPhyOffsetOfConsumeQueue` 恢复 CommitLog；
3. 恢复 TopicQueueTable 等消费位点/索引视图。

```text
recover(lastExitOK)
  → recoverConsumeQueue()
  → commitLog.recoverNormally() / recoverAbnormally()
  → recoverTopicQueueTable()

start()
  → reputFromOffset = commitLog.getConfirmOffset()
  → doRecheckReputOffsetFromCq()
  → dispatchBehindBytes() 归零后再收束 TopicQueueTable
```

为什么顺序要这样排？因为恢复不是只看日志文件自己长什么样，还要对照消费索引已经走到了哪里。`maxPhyOffsetOfConsumeQueue` 会成为后续判断“逻辑索引有没有跑到物理日志前面去”的重要参照。

这一步最值得强调的是：RocketMQ 恢复时并不相信单一来源。

- 只看 CommitLog：你不知道逻辑索引有没有超前或缺口；
- 只看 ConsumeQueue：你不知道底层物理日志是否仍完整可承认；
- 只看 TopicQueueTable：你更看不出消息实体是否还存在。

所以总恢复入口一上来就把问题拆成“物理日志、逻辑索引、位点视图”三层，后面所有恢复动作都围绕这三层对齐展开。

如果把恢复写成“先加载 CommitLog 文件就行”，主链会先在哪理解错？会看不见 ConsumeQueue 与物理日志之间那条最重要的校验关系，最后误把“磁盘有数据”当成“消费视图一定还能安全继续用”。

## 第二层：正常退出和异常退出，恢复问题根本不是同一种问题

RocketMQ 在恢复阶段第一个大分叉就是 `lastExitOK`。

- 正常退出：系统倾向于认为大部分持久化状态是一致收束过的，恢复重点是找到最后一段有效日志边界并校准位置；
- 异常退出：系统必须假设尾部可能存在半消息、脏尾巴、未完成 dispatch 或未正确落稳的逻辑索引。

这就是为什么 `CommitLog` 会分成：

- `recoverNormally(maxPhyOffsetOfConsumeQueue)`
- `recoverAbnormally(maxPhyOffsetOfConsumeQueue)`

这两条链虽然最后都在找“还承认哪一段日志”，但做法不同。

正常恢复从后面几个 mapped file 开始扫，目标是快速重新定位最后有效消息边界，并重新校正 flush/commit/confirm 这些物理位置；这里的 `doDispatch=false`，它不是在承担一轮“重新把消息派发回消费世界”的主任务。异常恢复则更保守，会从匹配恢复条件的文件往后逐段扫描，`doDispatch=true`，并在 controller/duplication 模式下继续用 `confirmOffset` 过滤哪些消息有资格再次进入 dispatch。

所以两者真正的区别不是“一个快一点，一个慢一点”，而是：

- 正常恢复更偏**边界重新定位与物理位置收束**；
- 异常恢复更偏**在不信任尾部世界的前提下，重新决定哪些消息还能被 dispatch 回逻辑视图**。

如果把正常和异常恢复写成“只是入口名字不一样”，主链会先在哪失真？会错过最关键的差别：**异常恢复不是简单重放，它是在怀疑最后一截世界是否还可信；正常恢复也不是缩小版回放，而更像一轮物理边界的重新收束。**

所以恢复的第二原则是：退出方式决定你对尾部状态的默认信任程度。

## 第三层：传统 CommitLog 恢复先回答“最后还承认哪一条完整消息”

传统 CommitLog 的恢复链里，最关键的不是文件数，也不是从第几个文件开始扫，而是：**最后一条仍被系统承认的完整消息边界在哪里。**

正常恢复里会维护 `lastValidMsgPhyOffset`；异常恢复里则会同时关注：

- `lastValidMsgPhyOffset`：最后一条完整物理消息；
- `lastConfirmValidMsgPhyOffset`：在 controller/duplication 语义下，最后一条还处于确认边界内的消息。

这两个量一出现，就已经说明 RocketMQ 在恢复时并不是只看“有没有完整消息”，还要继续问：

- 这条消息是不是完整；
- 在当前模式下，它是不是也处于系统仍承认的确认边界内。

异常恢复里的逻辑尤其说明这个层次：

```text
消息格式完整
  ≠
消息仍处于当前 confirm 边界内
```

如果在 controller 模式下只要消息格式完整就直接 dispatch，主链会先在哪失败？会失败在某些消息虽然物理上完整存在，但已经超出当前 `confirmOffset` 所代表的可承认边界。继续把它们派生到 ConsumeQueue，相当于把一段系统自己尚未确认的历史重新暴露给消费者。

所以传统 CommitLog 恢复真正先做的，是重建“最后完整且仍可承认的物理日志边界”。

## 第四层：confirmOffset 不是可有可无的指标，而是恢复时的真相闸门

很多人第一次接触 RocketMQ 恢复时，容易把 `confirmOffset` 当作一个运行时指标，觉得它主要对同步复制或 controller 模式有用，恢复时应该还是文件扫描说了算。

实际上，`confirmOffset` 在恢复里扮演的角色更接近一个**真相闸门**。

在传统非 controller 模式下，恢复完成后系统会把确认边界重新收敛到 `lastValidMsgPhyOffset`；而在 controller/duplication 语义下，如果当前 `confirmOffset` 明显小于最小物理位置或大于扫描出的有效处理位置，还会被重新修正到合法区间内。

这意味着 RocketMQ 的恢复并不默认认为“历史 confirmOffset 一定正确”，而是要结合当前可扫描到的物理现实重新校准它。这里的核心思想非常重要：

```text
确认边界不是历史遗留值
而是恢复后重新被系统承认的真相边界
```

如果不重校 `confirmOffset`，主链会先在哪坏？

- 过小：系统会无意义地放弃一段其实仍然完整且可承认的日志；
- 过大：系统会错误承认一段超出当前有效处理位置的消息。

所以恢复时不是“先扫描、后随便记个数”，而是“扫描出来的有效现实，反过来纠正确认边界”。

## 第五层：一旦 ConsumeQueue 跑到了物理日志前面，就必须裁掉逻辑世界

恢复链中最具有“失败推进感”的一个动作，就是：

```text
truncateDirtyLogicFiles(processOffset)
```

它表达的是一个很严厉但非常必要的原则：**如果逻辑索引已经超前于当前仍被承认的物理日志边界，必须先砍逻辑世界。**

无论是传统 CommitLog 还是 DLedger 恢复，都有这类判断：如果 `maxPhyOffsetOfConsumeQueue >= processOffset` 或相应的有效物理位置，就要截断脏 ConsumeQueue。

为什么恢复时首先牺牲逻辑索引，而不是反过来强行补物理日志？因为 RocketMQ 的真相层始终是 CommitLog。ConsumeQueue 只是消费索引桥，它的合法性来自底层统一日志。一旦底层真相边界缩回，逻辑索引就不能继续假装自己还有效。

如果不做这一步，主链会先在哪失败？会先失败在 Consumer 世界：

- ConsumeQueue 仍指向某个物理 offset；
- 但底层系统已经不再承认那段 CommitLog；
- Consumer 看见“有消息可读”，系统真相却说“这段历史已经失效”。

这就是最典型的“逻辑世界跑到真相前面去了”。RocketMQ 选择的恢复策略非常明确：宁可截断逻辑索引，也不让消费世界建立在一段未被真相层背书的数据上。

## 第六层：DLedger 恢复真正补的，是“提交边界下的物理真相”

到了 DLedger 模式，恢复逻辑会再多一层：**当前可承认的物理边界，不再只看传统 CommitLog 文件，还要看 DLedger 自己恢复出来的有效日志范围。**

`DLedgerCommitLog.recoverNormally()` / `recoverAbnormally()` 会：

- 加载 DLedger 文件；
- 恢复 DLedger store；
- 记录 `dividedCommitlogOffset`，处理旧 CommitLog 与 DLedger 新日志的分界；
- 根据正常/异常路径拿到 `maxPhyOffset` 或 `processOffset`；
- 如果 ConsumeQueue 已超前，就截断脏逻辑文件。

这里最容易写错的一点，是把 DLedger 恢复简单说成“看 `committedPos` 就行”。实际上，正文必须保留更细的边界：

- 正常恢复可能直接利用 DLedger 文件的最大写入位置；
- 异常恢复会边扫边推进 `processOffset`；
- 如果当前还没有 DLedger 文件，就不会走“纯 DLedger 文件恢复”这条路，而是先退回 `super.recoverNormally()` / `super.recoverAbnormally()` 去恢复旧 CommitLog，再通过 `setRecoverPosition()` 建立 mixed commitlog/DLedger 的分界；
- 混合 CommitLog / DLedger 场景因此不是边角情况，而是 RocketMQ 兼容旧日志向新日志过渡时的正式恢复分支。

共同点不是“永远只有一个 `committedPos` 决定一切”，而是：**系统最终会用 DLedger 恢复出来的有效物理边界，去裁决当前还能承认哪一段日志。**

如果把 DLedger 恢复写成“只要 leader 还在就没事”，主链会先在哪理解错？会忽略混合日志、异常退出、消费索引超前和恢复分界这些现实问题。那样读者会误以为 DLedger 只要负责选主，恢复就自然正确，但源码显然没有这么乐观。

所以 DLedger 补的不是“自动没故障”，而是“在共识日志语义下，重新确定可承认的物理真相边界”。

## 第七层：Reput 追平以后，消费视图才算真正恢复回来

即使 CommitLog 与 ConsumeQueue 的边界都已经校正好，恢复也还没有真正结束。因为 RocketMQ 还要让消费视图重新追平。

`DefaultMessageStore.start()` 会先把 Reput 的起始位置设到 `commitLog.getConfirmOffset()`，随后还会执行 `doRecheckReputOffsetFromCq()`。这条逻辑的核心不是“后台再跑一遍线程”，而是明确做两件事：

1. 根据 ConsumeQueue 实际最大物理位置重新估算更合理的 Reput 起点；
2. 等 `dispatchBehindBytes()` 归零，确保落后的派生消息已经重新补发到消费视图里。

```text
确认/恢复边界已确定
  → 设置 reputFromOffset
    → 补 dispatch behind 消息
      → dispatchBehindBytes() 归零
        → recoverTopicQueueTable()
```

为什么要强调 `dispatchBehindBytes()`？因为它不是一个可有可无的监控指标，而是恢复闭环的重要完成信号。只要它还大于 0，就说明还有一段已被系统承认的物理消息，尚未完全翻译回 ConsumeQueue / IndexFile 世界。

如果恢复后立即对外开放消费，而不等 Reput 追平，主链会先在哪失败？会失败在“系统已经承认真相，但消费视图还落在旧边界上”这里。Producer 可能继续发、Controller 可能已经切主、Broker 也可能已经活了，但 Consumer 看到的世界仍然滞后。

所以恢复不是“日志边界一校正就完”，还必须等消费视图重新跟上真相层。

## 第八层：主切换恢复的真正终点，是角色稳定以后重新把真相暴露给外界

前面几层讲的 mostly 是存储世界内部如何重新承认真相。到了 Controller / AutoSwitch / ReplicasManager 世界，还要再走最后一段：**谁把恢复后的新真相重新暴露给客户端。**

主切换时，`AutoSwitchHAService.changeToMaster()` 会：

- 计算并更新新的 `confirmOffset`；
- 处理 epoch；
- 等待 `dispatchBehindBytes() == 0`；
- `recoverTopicQueueTable()`；
- 再把状态机版本推进到新 epoch。

而 Broker 侧 `ReplicasManager.changeToMaster()` / `changeToSlave()` 还会：

- 更新 `masterEpoch` / `syncStateSet`；
- 切换 `BrokerRole`；
- 调整 HAService；
- 开关特定服务；
- 最终重新 `registerBrokerAll(...)` 到 NameServer。

这说明恢复链的真正终点不是“本地文件修好了”，而是：

```text
存储边界恢复
  → ConsumeQueue/Reput 追平
    → 角色切换稳定
      → NameServer 路由重新注册
        → Producer / Consumer 看到新的真相视图
```

如果少掉最后这一步，主链会先在哪失败？会失败在外部世界：Broker 自己也许已经恢复完成，但 Producer 仍然可能沿旧路由发往旧主，Consumer 仍然可能按旧视图理解当前主从格局。也就是说，系统内部真相已经变了，外部世界却还没被同步更新。

所以恢复的最后一公里，不是存储问题，而是“把恢复后的新真相再暴露给整个系统”。

## 第九层：Producer 的成功、磁盘上的存在、恢复后的承认，是三件必须拆开的事

走到这里，最该收紧的一件事就是语义。

### 1. Producer 曾经收到成功

这回答的是：某次发送请求在当时的确认策略和时间窗口里得到了成功反馈。

### 2. 某段字节曾经落到磁盘

这回答的是：历史上某份物理日志文件里确实出现过某段消息数据。

### 3. 恢复后系统仍承认这条消息为真相

这回答的是：在当前恢复完成、角色稳定、索引重建之后，系统最终仍把它纳入了可对外暴露的一致边界。

这三者绝对不能被压成一个“消息成功了所以还在”。

因为：

- `FLUSH_SLAVE_TIMEOUT` 说明某次确认窗口没等到；
- DLedger 的 `WAIT_QUORUM_ACK_TIMEOUT` 说明 quorum 边界没满足；
- 异常恢复时，某条历史上曾落盘的消息也可能不再位于最终承认边界之内；
- 逻辑索引超前时，即便 ConsumeQueue 曾经出现过条目，也会被裁掉。

如果不拆开这三层，故障恢复就会被误写成一个非常危险的结论：只要曾经出现过、曾经成功过、曾经能读到，恢复后就理应继续存在。RocketMQ 的源码显然不是这么工作的。

它真正做的是：**恢复后只重新承认那段仍被当前系统一致性边界背书的历史。**

## 收网：RocketMQ 恢复的核心不是重启服务，而是重建系统承认的真相边界

如果把整篇压成一句话，RocketMQ 的故障恢复，本质上不是把 Broker 拉起来继续跑，而是先重新确定当前还承认哪一段 CommitLog 为系统真相，再据此修正 `confirmOffset` / `processOffset` / `committedPos` 边界，截断超前的 ConsumeQueue，等待 Reput 追平，最后在新的 Broker 角色和 NameServer 视图下把这段真相重新暴露给客户端。

```text
失败发生
  → 恢复 CommitLog 有效边界
    → 校正 confirm / committedPos / process 边界
      → truncateDirtyLogicFiles()
        → Reput 追平，dispatchBehindBytes() 归零
          → 主角色稳定
            → NameServer 路由重新注册
              → 外界重新看到新的系统真相
```

到这里，主线只发生了五件事。

第一，恢复总入口先把物理日志、逻辑索引和位点视图拆开处理，而不是“一键全恢复”。

第二，正常退出和异常退出不是一回事；异常恢复真正要怀疑的是尾部世界还能不能继续被承认。

第三，物理真相层一旦缩回，逻辑索引必须先让路，`truncateDirtyLogicFiles()` 不是补丁，而是恢复纪律。

第四，`dispatchBehindBytes()` 归零之前，消费视图还没有真正追上恢复后的真相层。

第五，恢复的最后终点不是文件修好，而是角色和路由一起稳定下来，让 Producer/Consumer 再次看到一致的外部世界。

**本篇的一句话困惑**：消息都已经发过、写过、复制过了，RocketMQ 为什么恢复时还要重新扫描、截断和追平？

**本篇的一句话顿悟**：因为 RocketMQ 恢复时真正要回答的不是“历史上发生过什么”，而是“现在系统还承认哪一段日志为真相”；只有先把这条真相边界重新钉死，索引、角色和路由世界才有资格继续长出来。

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“服务拉起来就等于恢复完成。”** RocketMQ 还要重建可承认的日志边界、索引和角色视图。
2. **“磁盘上存在过的消息恢复后都应该继续存在。”** 只有仍处于当前承认真相边界内的消息才会被保留。
3. **“CommitLog 恢复完就等于 Consumer 一定能立刻读到。”** Reput 还要追平，`dispatchBehindBytes()` 归零之前消费视图可能仍落后。
4. **“ConsumeQueue 既然是索引，就可以独立于 CommitLog 继续存在。”** 一旦物理真相缩回，超前的逻辑索引必须截断。
5. **“主切换恢复只是 Broker 改个角色字段。”** 还要处理 HAService、epoch、Reput、NameServer 路由再暴露。
6. **“Producer 曾收到成功，就等于故障恢复后系统一定继续承认这条消息。”** 发送成功语义和恢复后最终承认真相不是同一时刻。

### 关键证据清单

- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:1890`：总恢复入口先恢复 CQ、再恢复 CommitLog、再恢复位点表。
- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:436`：`doRecheckReputOffsetFromCq()` 用 ConsumeQueue 最大物理位置重校 Reput 起点并等待 dispatch 追平。
- `store/src/main/java/org/apache/rocketmq/store/CommitLog.java:321`：正常恢复重新定位最后有效消息并校正 confirmOffset。
- `store/src/main/java/org/apache/rocketmq/store/CommitLog.java:695`：异常恢复按有效消息/确认边界决定 dispatch 与截断。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:293`：DLedger 正常/异常恢复按有效物理边界裁掉超前 ConsumeQueue。
- `store/src/main/java/org/apache/rocketmq/store/ha/autoswitch/AutoSwitchHAService.java:143`：切主后等待 `dispatchBehindBytes()==0`，再恢复 TopicQueueTable。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:237`：Broker 切成 Master 时同步 syncStateSet、HAService 与角色服务。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:321`：角色变化后重新注册 NameServer。
- `store/src/test/java/org/apache/rocketmq/store/dledger/MixCommitlogTest.java:35`：普通 CommitLog 与 DLedger 混合恢复、落后 ConsumeQueue 与分界恢复测试。
- `store/src/test/java/org/apache/rocketmq/store/dledger/DLedgerCommitlogTest.java:120`：DLedger 正常/异常恢复与 `dispatchBehindBytes()==0` 证据。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦普通消息存储、复制、控制面角色与消费索引恢复，不展开事务消息的半消息回查与二阶段恢复。
- 本文区分 `confirmOffset`、`committedPos`、`processOffset`、`dispatchBehindBytes()` 等不同边界，不把它们压成一个“恢复 offset”。
- 本文把 NameServer 路由再暴露也纳入恢复闭环，因为外部视图稳定是恢复完成的一部分。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-4` 的 CommitLog 真相层、`RocketMQ-5/6` 的 Reput/ConsumeQueue 派生链、`RocketMQ-13` 的传统 HA 确认边界、`RocketMQ-14` 的 DLedger 提交边界、`RocketMQ-15` 的 Controller/角色切换控制面。
- 后续桥接：下一批转入 `RocketMQ-19~21` 的事务消息恢复世界，继续回答“业务成功以后还能不能补偿回来”的更高层一致性问题。