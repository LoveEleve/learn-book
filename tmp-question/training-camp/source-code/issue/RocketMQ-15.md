# RocketMQ-15. Controller、选主和角色切换怎样决定 Broker 谁能继续写

> 场景：上一篇已经把 `DLedgerCommitLog` 讲清了：RocketMQ 可以把消息追加交给 DLedger，用 Leader/Follower 日志复制和提交结果替代传统 Master/Slave 的 offset 等待。但走到这里，读者通常会马上产生另一个问题：既然 DLedger 已经有 Leader，为什么 RocketMQ 还要单独做 Controller？到底是谁在决定某个 Broker 还能不能继续对外写？
>
> 本篇只回答一个问题：**Controller、选主和角色切换怎样决定 Broker 谁能继续写。** 本篇聚焦控制面：Controller 怎样维护副本元数据、怎样选主、怎样通知 Broker 落角色；不回头重讲 DLedger 的数据复制细节，也不展开 JRaftController 的另一套内部实现。

## 先把真正的困惑摆出来：为什么有了 DLedger Leader 还不够

很多人第一次看到 RocketMQ 5.x 的这组结构时，最自然的直觉就是：既然 DLedger 已经能选出 Leader，那么这台 Leader Broker 不就应该天然继续当 Master 对外写吗？再单独放一个 Controller，似乎只是把事情变复杂了。

这个直觉的问题在于，它把三种不同层次的“主”混成了一个词：

- **DLedger leader**：回答的是“谁在当前日志组里负责接受 append 并推进共识日志”；
- **Controller leader**：回答的是“哪台控制器节点当前有资格裁决 broker 集合的元数据和选主结果”；
- **Broker master**：回答的是“哪台业务 Broker 当前可以作为这个 broker-set 的对外写入者并暴露给 NameServer/客户端”。

这三者相关，但不是一回事。

如果只靠某个 Broker 自己根据本地 DLedger 状态对外宣布“我是 Master”，主链会先在哪坏掉？会坏在**外部世界没有统一裁决者**这里。因为：

- 新副本加入时，谁给它 brokerId？
- 老 Master 心跳丢失后，谁来裁定是否需要重选？
- 同一个 broker-set 里，哪台存活副本数据更完整、epoch 更高、优先级更合适？
- 选完以后，谁通知所有相关 Broker 切角色并重新注册 NameServer？

这些问题都不属于“单条日志怎样复制”，而属于“整个 broker-set 的角色元数据和控制面状态机怎样收口”。这正是 Controller 的职责。

所以这一篇要先把总图立住：

```text
Broker 启动
  → ReplicasManager 对接 Controller
    → 申请 brokerId / 注册副本元数据
      → 首次注册后 Broker 可主动 brokerElect
      → 运行期周期心跳上报 epoch / offset / 存活状态
        → Controller 检测 master 失活时也会触发 electMaster
          → 先尝试从 syncStateSet 选，必要时退化到 allReplicaBrokers
            → 通知 Broker 落地角色切换
              → 重新注册 NameServer
                → 客户端最终看到谁还能继续写
```

*关键设计（斜体）：* *DLedger 负责把日志复制对，Controller 负责把“谁可以继续代表这组副本对外写”裁决对；RocketMQ 真正的可写身份不是单个 Broker 自证出来的，而是 Controller 裁决 + Broker 落角色之后才成立。*[模式: 数据面共识 + 控制面裁决]

## 第一层：Controller 真正维护的不是消息，而是 broker-set 的角色元数据

要理解 Controller 为什么存在，第一步不是看选主算法，而是先看它到底在维护什么。

`ControllerManager.initialize()` 会根据配置选择 `DLedgerController` 或 `JRaftController`。默认的 DLedgerController 路线下，Controller 自己也是一个基于 DLedger 的控制面服务，但它承载的 entry 不是业务消息，而是 broker 集的元数据事件。

从暴露的处理器就能看出这一点。Controller 侧注册的请求处理包括：

- `CONTROLLER_GET_NEXT_BROKER_ID`
- `CONTROLLER_APPLY_BROKER_ID`
- `CONTROLLER_REGISTER_BROKER`
- `BROKER_HEARTBEAT`
- `CONTROLLER_ELECT_MASTER`
- `CONTROLLER_ALTER_SYNC_STATE_SET`
- `CONTROLLER_GET_REPLICA_INFO`

这些请求没有一个是在传递业务消息体。它们回答的是整套角色管理问题：

- 新副本是谁；
- 属于哪个 broker-set；
- 当前谁是 master；
- `syncStateSet` 是什么；
- 某个副本还活着吗；
- 现在要不要重选主。

这说明 Controller 不应被理解成“Broker 上方再放一个心跳转发器”，而应该被理解成：**broker-set 元数据的统一裁决平面**。

如果没有这层平面，主链会先在哪碎掉？会碎在“每个 Broker 只能看到自己的局部事实，没人能把所有副本的 brokerId、epoch、maxOffset、活性状态和 syncStateSet 放在一起裁决”这里。

所以从结构上看，RocketMQ 5.x 已经分成两张图：

```text
数据面：DLedgerCommitLog / AutoSwitchHAService / 副本数据复制
控制面：Controller / ReplicasManager / 选主 / 注册 / 角色通知
```

没有控制面，数据面就只能证明“日志是谁在复制”；它还证明不了“谁现在应该被外界当成唯一写入者”。

## 第二层：Broker 不是天然带着身份加入集群，而是先向 Controller 申请并注册自己

Controller 的存在还有一个特别基础、但经常被忽略的价值：Broker 进入集群时，并不是天生就知道自己应该以什么身份出现。

Broker 侧 `ReplicasManager.start()` 会先做几件事：

1. 同步 controller 地址；
2. 扫描可用 controller 节点；
3. 启动基础服务；
4. 同步 controller 元数据；
5. 注册自己；
6. 进入正常运行态。

注册流程本身又不是一步，而是一个小状态机：

```text
INITIAL
  → FIRST_TIME_SYNC_CONTROLLER_METADATA_DONE
    → REGISTER_TO_CONTROLLER_DONE
      → RUNNING
```

其中最关键的一段是 brokerId 申请与注册：

```text
1. 向 Controller 请求 nextBrokerId
2. 本地创建 temp metadata
3. 向 Controller apply brokerId
4. 创建正式 metadata 文件
5. registerBrokerToController
```

这套流程看起来比“直接起个 Broker 就加入集群”麻烦很多，但它补的就是身份一致性。因为对于同一个 broker-set，Controller 必须知道：

- 这个副本在集合里是谁；
- 它是不是已经被分配过 brokerId；
- 它的本地 metadata 是否与控制面一致；
- 它现在注册成功以后，当前 master 是谁。

如果省掉这套注册链，主链会先在哪坏？会先坏在“新副本看起来活着，但控制面根本无法确认它在这个 broker-set 里的合法身份”这里。到了那时，心跳、选主、角色通知和 syncStateSet 调整都无从谈起。

所以 Broker 加入集群不是“连上 Controller 就完了”，而是先完成一轮身份确认，才能进入后续的主从裁决世界。

## 第三层：Controller 通过心跳拿到副本活性与数据新鲜度视图

副本身份建立以后，下一步才轮到心跳。

Broker 侧 `ReplicasManager.sendHeartbeatToController()` 会周期性向可用的 controller 地址上报：

- clusterName / brokerName / brokerId
- brokerAddr
- lastEpoch
- `getMaxPhyOffset()`
- `getConfirmOffset()`
- 心跳超时
- 选举优先级

这里有一个非常关键的认知点：心跳不是只在告诉 Controller“我还活着”。它同时还在告诉 Controller：**我现在手里有多新的数据、处于哪个 epoch、在选主时大概应该处于什么位置。**

这就是为什么后面 `DefaultElectPolicy` 能按 `(epoch, maxOffset, electionPriority)` 排序。没有这些心跳数据，Controller 最多只能知道某个副本还在线，却无法进一步判断：

- 它是不是和当前 master 足够接近；
- 它是不是比别的副本更适合接主；
- 它是不是只是“活着”，但数据很旧。

如果心跳只上报存活，不上报 epoch/offset，主链会先在哪失真？会失真在选主阶段：Controller 只能在一堆“活着的副本”里盲选，而不是在一堆“既活着、又尽可能新的副本”里做有依据的裁决。

所以心跳在这里不是保活 ping，而是控制面持续更新候选者质量视图的输入流。

## 第四层：真正触发重选主的，不是某个 Broker 自发让位，而是 Controller 检测“主已经不活了”

只要控制面掌握了心跳和副本元数据，下一步自然就是：什么时候开始重选主？

`ControllerManager` 在检测到 broker 失活时，会进入 `onBrokerInactive()`。这里不是“谁掉了都马上重选”，而是要先查当前失活的是不是这个 broker-set 的 master：

```text
broker inactive event
  → getReplicaInfo(brokerName)
    → 判断失活 brokerId 是否等于 masterBrokerId
      → 只有当前 master 失活时才触发 electMaster
```

这一步很重要，因为它说明 Controller 的目标不是只要有副本下线就重组世界，而是**先维护当前 master 视图是否仍然有效**。

如果不是 master 掉了，只需要更新活性视图；如果 master 掉了，才进入 `triggerElectMaster()` 这条主链。

如果省掉“先判断掉的是不是 master”这一步，主链会先在哪变得过度敏感？会在任何普通 follower 抖动时都触发整组 broker-set 的主切换，从而让系统进入无意义的频繁重选。

所以 Controller 不是“监控一变化就立刻切主”，而是在维护“当前唯一写入者是否仍然可信”的稳定视图。

但这里还要补一个容易漏掉的入口：**选主不只会在 Controller 检测到 master 失活后触发。** Broker 在 `ReplicasManager.startBasicService()` 完成首次注册后，如果当前还没有明确 master，也会主动调用 `brokerElect()` 向 Controller 发起一次“请给我这组 broker 的当前主视图”的请求。也就是说，RocketMQ 的选主入口至少有两类：

- Controller 侧的被动入口：检测到 master 失活后触发 `electMaster`；
- Broker 侧的主动入口：首次注册完成后主动尝试 `brokerElect()`。

如果把选主只写成 Controller 的单向动作，主链会先在哪失真？会让读者误以为 Broker 启动时只能被动等 Controller 推结果，而看不见它也会在注册完成后主动拉起主视图建立过程。

## 第五层：DefaultElectPolicy 真正解决的是“多个活副本里谁最适合接主”

进入 `electMaster()` 以后，真正困难的问题才出现：即使已经确定要重选，新主也不能随便挑一个在线副本。

`DefaultElectPolicy` 的策略并不神秘，但很关键。它的顺序可以压成：

```text
1. 先在 syncStateSet 里找
2. 不行再在 allReplicaBrokers 里找
3. 先过滤掉不活跃副本
4. 若旧 master 仍有效，优先保留旧 master
5. 若指定 preferBrokerId 且合法，优先选它
6. 否则按 (epoch, maxOffset, electionPriority) 排序选最优者
```

这里至少有三层含义。

### 第一层：先尝试 syncStateSet，再必要时退化到所有副本

这表示 RocketMQ 默认希望新的 master 尽量从当前同步副本集合里选，而不是一开始就把所有副本一视同仁。因为 syncStateSet 本来就更接近“当前已被认定为足够跟上主线的一组副本”。

但这里必须把边界说严：这不是“新主一定只能从 syncStateSet 里产生”。`DefaultElectPolicy` 的真实策略是**先试 syncStateSet，选不出来再退化到 allReplicaBrokers**。也就是说，在某些场景下，尤其是允许更激进恢复或当前同步集里没有合格候选者时，控制面可以接受从更大的副本集合里挑出一个次优但仍可工作的候选者。

### 第二层：不是只看活不活，还要看数据新不新

排序器先比 epoch，再比 maxOffset，再看 electionPriority。这说明副本只是“在线”远远不够；一个 epoch 更高、maxOffset 更接近当前主线的副本，更有资格接主。

### 第三层：priority 不是万能第一条件，而是在 epoch/offset 相近时才进一步裁决

这避免了“运维手动配了高优先级，就能让一个明显更旧的副本强行接主”的风险。RocketMQ 先守数据新鲜度，再谈人为优先级。

如果选主只看 electionPriority，主链会先在哪坏掉？会坏在“一个配置上优先级更高、但数据更落后的副本也可能被抬成主”这里。反过来，如果只看 maxOffset 又不看 epoch，也可能把处于旧 epoch 的副本错误扶正。

所以 `DefaultElectPolicy` 的真正价值不是“写个排序器”，而是把“谁最有资格代表当前这组副本继续写”这件事，落成一套明确的候选优先级。

## 第六层：选出 master 结果以后，真正难的不是“知道答案”，而是把答案落到 Broker 身上

选主完成并不等于事情结束。Controller 知道了谁是新的 master，只说明裁决已经形成；而业务系统真正关心的是：**这台 Broker 什么时候真的能继续对外写？**

这一步由 Broker 侧 `ReplicasManager` 收结果并落角色。

无论是 Broker 自己触发 `brokerElect()`，还是 Controller 通过 `notifyBrokerRoleChanged()` 把结果推给 Broker，最终都会汇聚到：

- `changeToMaster(...)`
- `changeToSlave(...)`

这两条链不是简单改个字段。以 `changeToMaster()` 为例，它会做：

1. 更新 `masterEpoch`；
2. 更新或切换 `syncStateSet`；
3. 处理 slave synchronize 定时任务；
4. 通知 `AutoSwitchHAService.changeToMaster(...)`；
5. 把 `brokerId` 设成 Master；
6. 把 `MessageStoreConfig.brokerRole` 设成 `SYNC_MASTER`；
7. 打开特定服务能力；
8. 记录新的 `masterAddress/masterBrokerId`；
9. 启动 syncStateSet 检查；
10. 最后重新注册 NameServer。

而 `changeToSlave()` 也会反向完成：

- 停止 master 侧的 syncStateSet 检查；
- 改成 `BrokerRole.SLAVE`；
- 关闭 master 侧特定服务；
- 更新 `masterAddress/masterBrokerId`；
- 启动或切换 slave synchronise；
- 通知 HAService 切到 slave；
- 重新注册 NameServer。

这说明控制面和服务面之间还有最后一座桥：**Controller 负责给出正确答案，ReplicasManager 负责把这个答案变成 Broker 的真实运行状态。**

## 第七层：重新注册 NameServer，才把新的主视图真正暴露给外界

到这里还有最后一个容易被低估的步骤：注册 NameServer。

无论切成 Master 还是 Slave，`ReplicasManager.registerBrokerWhenRoleChange()` 都会在角色变更后重新调用 `registerBrokerAll(...)`。这一步之所以必须存在，是因为客户端最终看到的不是 Controller 内存里的裁决结果，而是 NameServer 暴露出来的路由与主视图。

如果选主已经完成，但 Broker 没有重新注册 NameServer，会先在哪失败？会先失败在**控制面已经知道新主是谁，而客户端路由还可能继续命中旧角色视图**这里。那样 Controller 和客户端世界会发生短暂甚至持续的视图分裂。

所以真正的“谁还能继续写”必须至少跨过三层：

```text
Controller 选出 master
  → Broker 本地切角色
    → NameServer 更新路由视图
      → Producer/Consumer 感知新的主从格局
```

这也是为什么本篇不能把 Controller 写成一个完全独立的小服务说明书。它最终还是要回到 RocketMQ 原有的 Broker / NameServer / Client 主链世界里，才能解释清楚外部可见的结果。

## 第八层：Controller leader、DLedger leader、Broker master 三种“主”必须严格分开

走到这里，最应该收紧的一件事就是术语。

### 1. Controller leader

Controller 集群内部也可能有 leader。它回答的是：**谁有资格裁决控制面事件。** 必须说清的是：只有 Controller leader 才应该对 broker-set 的 master 做最终裁决；如果某个 Controller 节点不是 leader，它就不应该承担这个最终决策。

### 2. DLedger leader

DLedger leader 回答的是：**谁在某个共识日志组里负责接受 append。** 这是数据面视角，不天然等于对外业务写主身份。

### 3. Broker master

Broker master 回答的是：**这个 broker-set 里当前哪台 Broker 应该被外部客户端当成主要写入者。** 这是业务路由与服务身份视角。

把这三者压成同一个“主节点”，主链会先在哪失真？会在你解释故障切换时彻底混乱：

- 你会分不清 Controller 自己是不是 leader；
- 你会把数据复制 leader 错当成业务写主；
- 你会看不见 Broker 本地角色切换和 NameServer 注册这条必要落地链。

所以本篇最重要的结构纪律，就是始终记住：

```text
Controller leader ≠ DLedger leader ≠ Broker master
```

它们之间有依赖，但不是自动同构的三个名字。

## 收网：真正决定 Broker 谁能继续写的，是控制面裁决落成服务身份

如果把整篇压成一句话，RocketMQ 5.x 里真正决定某个 Broker 能不能继续写的，不是它自己宣称“我是 DLedger leader”，而是 Controller 按心跳和副本元数据裁决出新的 master，再由 Broker 侧 `ReplicasManager`/`AutoSwitchHAService` 把这个裁决落成真实角色，并重新注册到 NameServer，最终让客户端看到新的主视图。

```text
Broker 注册与心跳
  → Controller 维护副本元数据
    → master 失活触发 electMaster
      → DefaultElectPolicy 选出候选者
        → 角色变化通知 Broker
          → changeToMaster / changeToSlave
            → registerBrokerAll
              → 外界最终知道谁还能继续写
```

到这里，主线只发生了五件事。

第一，Controller 维护的是 broker-set 的角色元数据，不是业务消息本身。

第二，Broker 进入集群前必须先申请 brokerId、注册自己并同步当前 master 视图。

第三，选主不是“谁活着就选谁”，而是先尝试在 `syncStateSet` 中按 `(epoch, maxOffset, electionPriority)` 选出更合格的候选者，必要时再退化到 `allReplicaBrokers`。

第四，选主结果只有落到 `changeToMaster` / `changeToSlave` 并切换本地服务状态以后，才成为真实的 Broker 角色。

第五，重新注册 NameServer 之后，新的角色视图才真正对 Producer/Consumer 生效。

**本篇的一句话困惑**：既然 DLedger 已经能选 Leader，为什么 RocketMQ 还要单独做 Controller？

**本篇的一句话顿悟**：因为 DLedger 只回答“日志由谁来复制与提交”，Controller 才回答“哪台 Broker 该被整个系统承认为新的写主”；真正的可写身份来自 Controller 裁决、Broker 落角色和 NameServer 重新暴露三步闭环，而不是单个 Broker 的本地自证。

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“DLedger leader 就等于 Broker 已经能继续写。”** 数据面 leader 不自动等于业务写主。
2. **“Controller 只是转发心跳的服务。”** 它维护 brokerId、master、syncStateSet 和选主裁决元数据。
3. **“只要某个副本活着，就可以直接接主。”** 选主还要比较 epoch、maxOffset 和 priority。
4. **“Controller 选完 master 就结束了。”** Broker 还要切换本地角色并重新注册 NameServer。
5. **“Controller leader、DLedger leader、Broker master 都是一个主。”** 三者是不同层级的角色。
6. **“syncStateSet 就是客户端看到的全部副本集合。”** 它只是控制面和 HA 世界中的一个同步副本子集。

### 关键证据清单

- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:102`：初始化默认 `DLedgerController` 与选举策略。
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:146`：broker 失活后触发选主。
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:211`：角色变化通知 broker。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/AdminBrokerProcessor.java:3045`：Broker 接收 `NOTIFY_BROKER_ROLE_CHANGED` 并落到 `ReplicasManager.changeBrokerRole(...)`。
- `controller/src/main/java/org/apache/rocketmq/controller/impl/DLedgerController.java:187`：`electMaster()` 通过控制面事件调度进行裁决。
- `controller/src/main/java/org/apache/rocketmq/controller/elect/impl/DefaultElectPolicy.java:38`：按 `(epoch, maxOffset, electionPriority)` 比较候选者。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:137`：Broker 启动后进入 controller 同步/注册状态机。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:225`：按新的 master 结果切换本地角色。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:375`：Broker 主动向 Controller 触发选主。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:401`：心跳向 Controller 上报 epoch/offset/confirmOffset。
- `controller/src/test/java/org/apache/rocketmq/controller/ControllerManagerTest.java:184`：注册两个 broker、选主与 master 切换测试。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦默认 `DLedgerController` 路线下的控制面；`JRaftController` 只作为另一种实现边界，不展开内部细节。
- 本文重点是 broker-set 级控制面裁决，不重讲上一篇的 `DLedgerCommitLog` 数据复制与 Future 提交细节。
- 本文聚焦 broker 角色、注册、心跳、选主与 NameServer 路由落地；更完整的故障恢复链放到后续总串联篇。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-2` 的 Broker 宿主能力、`RocketMQ-3` 的 NameServer 路由、`RocketMQ-13` 的传统 HA 主从确认、`RocketMQ-14` 的 DLedger 数据面提交边界。
- 后续桥接：下一篇不再单独拆 HA/Controller，而会在 `RocketMQ-18` 里把主切换后的恢复、可见性和重复投递边界统一收网。