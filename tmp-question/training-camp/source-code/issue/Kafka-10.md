# Kafka-10. 为什么 Kafka 不立刻返回，而要把请求挂起来——Purgatory、DelayedProduce、DelayedFetch 与 TimingWheel 主链

> 场景：Kafka-2 里 Producer 看见的是 `acks=all` 带来的等待，Kafka-4 里 Consumer 看见的是 `fetch.min.bytes` / `maxWaitMs` 带来的 long poll。看起来一个是在等副本确认，一个是在等数据凑够字节数，似乎是两回事。但在 broker 里，这两种“等一会儿再返回”的需求最终会汇合到同一套机制：Purgatory。

## 先把真正的困惑摆出来：为什么 Kafka 不能“等一会儿再返回”？

先看两个最常见的等待场景。

### 场景一：`acks=all`

producer 写入 leader 以后，如果要求 `acks=all`，broker 不能立刻告诉客户端“成功”。它必须等 ISR 里的副本都追上这条消息，才算真正满足确认条件。

### 场景二：`fetch.min.bytes`

consumer 发来 fetch 请求时，如果当前数据很少，Broker 也不能立刻返回一个空响应。它要尽量等到数据累计到 `min.bytes`，或者等到 `maxWaitMs` 超时，再把现有数据返回给客户端。

一个很直觉的写法是：请求线程里 while 循环，不断检查条件，条件满足就返回；不满足就 sleep 10ms 再查一次。

这条路看起来简单，实际上几乎是 Kafka 最不可能接受的写法，因为它会把请求处理线程直接卡住：

- `acks=all` 可能要等多轮 follower fetch；
- fetch long poll 可能要等几百毫秒；
- 同时几千个请求都在 sleep/poll，线程和 CPU 都会被空耗。

```text
请求到达
  → while (!条件满足) {
       sleep(10ms)
       再查一次
     }
  → 返回
```

这等价于让网络线程 / IO 线程替请求“站岗”。Kafka 的设计目标恰恰相反：**线程只在真正能推进状态时工作，不能在“等”这件事上白白烧掉。**

所以 Kafka 要解决的不是“怎么 sleep 更优雅”，而是：**怎么把“等待条件满足”从请求线程里摘出来，既能在条件一满足时立刻醒，又能在永远等不到时按超时兜底。**

*关键设计（斜体）：* *Kafka 把“等待条件满足”抽象成 DelayedOperation：先立即尝试完成；如果条件还不够，就按 key 挂到 DelayedOperationPurgatory 的 watcher 列表，同时放进 TimingWheel 管理超时。相关状态一变化，就用 `checkAndComplete` 事件驱动重试；如果一直等不到，就由定时器强制完成。*[模式: 先试一次 + 事件唤醒 + 时间兜底]

## 第一层：`DelayedOperation` 把“只完成一次”变成了统一抽象

Kafka 没有为 DelayedProduce、DelayedFetch 各自造一套“等待框架”，而是先抽出了一个统一父类：`DelayedOperation`。

它同时扮演两种角色：

- **条件等待对象**：定义 `tryComplete()`，判断“现在能不能完成”；
- **定时任务**：继承 `TimerTask`，到点后会触发 `run()`。

这两个角色被一个非常关键的语义绑在一起：**不管是条件满足提前完成，还是超时到了被迫完成，`onComplete()` 都只能被调用一次。**

`forceComplete()` 就是这条语义的核心实现：

```text
若还没 completed
  → 加锁
    → 再检查一次
      → 标记 completed=true
        → cancel timeout
          → onComplete()
```

为什么 Kafka 这么强调“只完成一次”？因为一个延迟请求可能被多个线程同时看到：

- 一个线程正因为 ISR 追上而尝试唤醒它；
- 另一个线程正因为超时而触发它；
- 第三个线程可能正处理另外一个分区 key 的变化，也试图唤醒它。

如果没有 `forceComplete()` 的 once-only 语义，同一个请求就可能返回两次，或者一边超时一边又带着成功结果返回。Kafka 用一个 `completed` 标志 + 锁，把这件事彻底封死。

所以 DelayedOperation 的本质不是“带超时的 future”，而是“**一个允许被多方竞争完成、但最终只成功一次的条件任务**”。

## 第二层：Kafka 不是先挂起来再判断，而是“先试一次，不行再挂表”

Purgatory 最聪明的地方，不是把请求塞进列表，而是 `tryCompleteElseWatch()` 这条顺序。

它不是：

```text
先加入等待队列
  → 以后有人来检查
```

而是：

```text
先 tryComplete()
  → 如果已经满足，直接完成
  → 如果不满足，挂到所有 watch keys
    → 再 tryComplete() 一次
      → 还不满足，才真正进入等待态
```

这个顺序解决了一个微妙但致命的问题：**“在我挂表之前，条件刚好满足了怎么办？”**

例如：

1. produce 请求刚到，条件还没满足；
2. 线程准备把它挂到 watcher list；
3. 就在这时，某个 follower 追上了，条件实际上已经满足；
4. 如果先挂表、但挂完不再检查一次，就可能漏掉这次变化，白等到超时。

Kafka 的做法是：在 operation 的锁内，**先把它挂到所有 watch key 上，再做第二次 `tryComplete()`**。关键不只是“多试一次”，而是“挂表 + 第二次检查”这两步在同一把 operation 锁里完成。这样才能保证：**一旦操作真正进入等待态，它已经不可能错过之后任何一次相关事件。**

这就是 `safeTryCompleteOrElse` 背后的真正设计动机：不是单纯多试一次，而是把“持锁挂表后再检查，因此不会漏唤醒”变成一个原子语义。

## 第三层：watcher list 负责“谁的变化能唤醒我”

Purgatory 不是一个普通队列，因为一个延迟请求不是单纯“等时间到”，而是“等某些 key 上的状态变化”。

所以 `DelayedOperationPurgatory` 有一组 watcher lists。每个延迟请求会按若干个 `DelayedOperationKey` 挂进去。

### produce 的 key

DelayedProduce 一般按 topic-partition 挂 watcher。因为它是否满足，取决于这些分区对应的 ISR/HW 是否推进到 `requiredOffset`。

### fetch 的 key

DelayedFetch 也是按 topic-partition 挂 watcher。因为某个分区有新数据、leader 变化、epoch 分叉、log segment 切换时，都可能触发这个 key 上的一次**再检查**。

要注意，这里不是说 purgatory 能直接分辨“究竟是哪一种条件满足了”。它只知道：**这个 key 相关的状态变了，值得把等待中的 fetch 再跑一遍 `tryComplete()`。** 是否真的完成，仍然要在 `DelayedFetch.tryComplete()` 里重新判断。于是“谁来唤醒我”这件事，就从“我在一个队列里等”变成“**只要相关 key 发生变化，就到对应 watcher list 上把我再检查一遍。**”

`checkAndComplete(key)` 做的就是这件事：拿到这个 key 的 watchers，遍历里面的操作，对每一个调用 `tryCompleteWatched()`，尽可能把已经满足的请求立刻完成。

这里再压一句边界：**watcher list 只负责把“这个 key 上有变化，值得再试一次”这件事通知给操作；它本身并不直接判定请求已经满足。** 真正的完成条件，仍然只在各自的 `tryComplete()` 里统一判断。

这一步的重要性在于：Kafka 等待的不是“某个时间点”，而是“某类状态变化”。Purgatory 的 watcher list 把“状态变化”和“等待中的请求”建立了直接索引。

## 第四层：只靠 watcher 不够，因为有些条件可能永远不满足

如果 Purgatory 只有 watcher list，没有 timeout timer，会出现另一个问题：某个请求可能永远等不到条件。

- produce：ISR 一直没追上；
- fetch：数据一直凑不到 `min.bytes`；
- 甚至某个 topic-partition 后续再也没有任何状态变化。

这时如果没有时间兜底，请求就会永久留在 watcher list 里，既不返回，也不清理，最后变成资源泄漏。

所以 Kafka 给每个 DelayedOperation 又接了一条 timeout 路：如果操作在 delayMs 内一直没完成，就由 timer 线程触发 `run()`，而 `run()` 会调用：

```text
if (forceComplete())
  onExpiration()
```

注意这里的顺序：

- 先 `forceComplete()`，保证“只完成一次”；
- 只有第一次超时线程真的抢到完成权，才会执行 `onExpiration()`。

这意味着超时不是“另一种完成逻辑”，而是**另一条抢占完成权的路径**。一旦操作已经因为 watcher 事件提前完成，timeout 就会在 `forceComplete()` 那里失败，什么都不会重复执行。

## 第五层：TimingWheel 让大量超时任务不会拖垮 broker

有了 timeout 之后，还剩一个工程问题：如果 broker 上同时挂着几万、几十万个 delayed requests，怎么管理它们的超时？

用优先队列当然能做，但插入/删除都是 `O(log N)`，请求量一大成本就会持续放大。Kafka 选择的是**分层时间轮（Hierarchical Timing Wheel）**。

`TimingWheel` 的核心思想是：

- 最底层 wheel 用最细粒度时间片；
- 一个 wheel 只覆盖有限时间窗口；
- 超出窗口的任务往上层 overflow wheel 放；
- 高层 bucket 到期时，再把任务重新分配回更细粒度的 wheel。

这样，定时任务的插入/删除成本可以压到接近 `O(1)` / `O(m)`（m 为 wheel 层数，通常很小），非常适合 Kafka 这种“请求量大、很多请求会在超时前提前完成”的场景。

更重要的是，时间轮只负责**到点兜底**，而不是承担主唤醒路径。大多数 DelayedOperation 其实不会等到 timer 到期，而是被 watcher 事件提前唤醒。这让 Kafka 把时间轮的压力大幅降下来。

还要补一个实现边界：`TimingWheel` 本体并不直接承诺线程安全，线程安全约束由 `SystemTimer` 和外层调用方式承担。也就是说，时间轮是 Kafka 的 timeout 核心数据结构，但它不是一个“任何线程都能随便并发操作”的万能容器。

## 第六层：`DelayedProduce` 在等什么——不是等时间，而是等 enough replicas

`DelayedProduce.tryComplete()` 的主线很直白：遍历每个分区的 produce status，看它的 `requiredOffset` 是否已经被 enough replicas 追上。

它逐个分区检查三类情况：

- **Case A**：本 broker 已经不再拥有该分区（或分区出错）
- **Case B**：本 broker 不再是这个分区的 leader
- **Case C**：本 broker 还是 leader，就调用 `partition.checkEnoughReplicasReachOffset(requiredOffset)`

只要分区满足了“已经 enough replicas 确认”或“我已经不该再等这个分区”，就把这个分区的 `acksPending` 置为 false。等全部分区都不再 pending，就 `forceComplete()`。

这解释了 `acks=all` 在 broker 侧的真实形态：**producer 不是在等某个固定延时，而是在等“每个相关分区都跨过 requiredOffset 的副本确认条件”。** 时间只是兜底，真正驱动完成的是 ISR/HW 的状态变化。

## 第七层：`DelayedFetch` 在等什么——不是等时间，而是等 minBytes / epoch / leader 变化

`DelayedFetch.tryComplete()` 比 DelayedProduce 更复杂，因为 fetch 的满足条件更多样。

它会遍历每个分区，检查：

- broker 是否还领导这个分区（否则立刻返回）
- 请求 epoch 是否已被 fenced（否则立刻返回）
- fetch offset 是否已经落到旧 segment（否则立刻返回，避免白等）
- 是否发现 diverging epoch（否则立刻返回，让 follower 先去截断）
- 当前可读字节累积是否已经达到 `minBytes`

所以 fetch 的 long poll 不是“等到 maxWaitMs 再统一返回”，而是“**先过 leader/epoch/segment 这些边界条件，再看 minBytes 是否达标**”：

- 有数据够了 → 立即返回；
- leader 变了 / epoch fenced 了 → 立即返回；
- diverging epoch 发现了 → 立即返回，驱动截断；
- 什么都没发生 → 到 `maxWaitMs` 超时返回。

这正是 Kafka-4 里 consumer 看起来像“长轮询”的原因：它不是线程睡在那里，而是一个 DelayedFetch 正挂在 purgatory 里，先等 leader/epoch/segment 这些边界条件稳定，再用 `minBytes` 作为最后的数据量门槛。

## 第八层：Purgatory 不是“等时间”，而是“等条件”；时间只是兜底

把 DelayedProduce 和 DelayedFetch 放在一起看，会发现它们共享的是同一个骨架：

```text
tryComplete()
  → 满足：forceComplete → onComplete
  → 不满足：watch key + timeout timer
    → key 变化：checkAndComplete 再试
    → 到期：run() → onExpiration
```

它们真正的差别只在 `tryComplete()` 条件：

- produce 等 enough replicas / leader 语义
- fetch 先等 leader/epoch/segment 边界，再用 minBytes 作为数据量门槛

这就是 Kafka Purgatory 最值得记住的地方：**Kafka 不在等待“时间过去”，而是在等待“条件发生”。时间从来不是主角，只是条件一直不发生时的兜底出口。**

如果反过来把 timer 当主路径、事件当补充，主链会先在哪失败？你会把大量本可提前返回的请求都拖到 timeout 才醒，延迟抖动立刻上升，broker 也会堆积更多无意义的等待任务。

把这一层的两条触发路径再压成一句话：**watcher 负责“条件变化的再检查”，timer 负责“条件一直不发生的兜底”。** 一条请求挂进 purgatory 后，任何一个相关 key 上的状态变化都可能提前唤醒它，而 timeout 只负责在没有任何事件能推进它时兜底返回。

## 收网：Kafka 不是把请求排队睡一会儿，而是把条件等待做成统一框架

把整篇压成一句话：Kafka 用 `DelayedOperation` 把“条件满足即可返回、条件一直不满足也必须超时”抽成统一框架；请求先 `tryComplete`，不满足就通过 `tryCompleteElseWatch` 挂进 `DelayedOperationPurgatory` 的 watcher list，同时进入 TimingWheel 管理超时；相关 key 一变化就 `checkAndComplete` 事件驱动重试，实在等不到再由超时线程 `forceComplete + onExpiration` 兜底。`DelayedProduce` 与 `DelayedFetch` 只是这套骨架上的两个条件实现。

```text
请求到达
  → DelayedOperation.tryComplete
    → 满足：完成
    → 不满足：watch keys + timer
      → key 变化：checkAndComplete
      → timeout：forceComplete + onExpiration
```

到这里，主线只发生了八件事。

第一，Kafka 不能让请求线程自己 while/sleep 等条件。

第二，`DelayedOperation` 把“多方竞争完成但最终只完成一次”变成统一抽象。

第三，`tryCompleteElseWatch` 通过“先试一次、挂表、再试一次”避免漏唤醒。

第四，watcher list 决定“谁的状态变化能唤醒我”。

第五，timer 只负责兜底超时，不负责主要唤醒。

第六，TimingWheel 让大量超时任务的成本可控。

第七，`DelayedProduce` 在等 enough replicas reach offset。

第八，`DelayedFetch` 在等 minBytes / epoch / leader 条件，long poll 只是这套机制的一个外观。

**本篇的一句话困惑**：Kafka 为什么不让 produce/fetch 请求线程直接等一会儿再返回？

**本篇的一句话顿悟**：因为 Kafka 等的不是时间，而是条件；Purgatory 用 watcher list 负责事件唤醒、用 TimingWheel 负责超时兜底，把 `acks=all` 和 long poll 统一进了一套 once-only 完成框架。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Purgatory 就是一个延迟队列。”** 它同时有 watcher lists 与 timeout timer 两套触发机制。
2. **“请求一进 purgatory 就只能等超时。”** 多数请求会被事件驱动提前唤醒。
3. **“`acks=all` 与 long poll 是两套完全不同实现。”** 它们只是 `DelayedOperation` 的两个子类。
4. **“TimingWheel 是为了更久地睡眠。”** 它的核心是大量超时任务下的低成本管理。
5. **“条件满足时一定会在下一轮轮询才返回。”** `checkAndComplete` 会在相关 key 变化时立即重试。

### 关键证据清单

- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperation.java:24`：DelayedOperation 抽象与 once-only 完成语义。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperation.java:60`：`forceComplete()`。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperationPurgatory.java:122`：`tryCompleteElseWatch()`。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperationPurgatory.java:184`：`checkAndComplete()`。
- `server-common/src/main/java/org/apache/kafka/server/purgatory/DelayedOperationPurgatory.java:409`：超时 reaper。
- `server-common/src/main/java/org/apache/kafka/server/util/timer/TimingWheel.java:22`：分层时间轮设计与复杂度。
- `core/src/main/scala/kafka/server/DelayedProduce.scala:57`：DelayedProduce。
- `core/src/main/scala/kafka/server/DelayedProduce.scala:89`：produce 满足条件检查。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:50`：DelayedFetch。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:77`：fetch 满足条件检查。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 purgatory 框架、DelayedProduce/DelayedFetch、timer/watcher 双触发；ISR/HW 本体语义留在 Kafka-9。
- 本篇把 TimingWheel 当作 timeout 兜底机制，不回头展开 Kafka 网络线程与请求调度。
- 不把 watcher list 与 timeout queue 写成同一层结构：前者处理事件，后者处理时间。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-2`（Producer `acks=all` 语义）、`Kafka-4`（Consumer long poll）、`Kafka-9`（ISR/HW/epoch 条件）。
- 后续桥接：下一篇可进入 Kafka 事务 / 幂等主线，把“为什么 Producer 还要等事务标记、为什么 read_committed 还要过滤未提交数据”接到这里的条件等待机制上。