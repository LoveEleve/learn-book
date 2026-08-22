# Kafka-45. Broker 怎么感知自己在集群里的身份——BrokerLifecycleManager、注册与心跳主链

> 场景：Broker 启动后并不是“自己觉得启动了就算加入集群”。它必须向 controller 注册、持续发心跳、等待 controller 认定 caught up，才能真正进入 RUNNING。本篇补上 Broker 集群感知细节，解释 broker 是怎么知道“自己已经被集群接受”的。

## 先把真正的困惑摆出来：Broker 为什么不能只靠本地状态认为自己可用

因为在 KRaft 下，集群视角由 controller 持有。broker 本地把 SocketServer、LogManager 都启动了，只代表“本地组件好了”；但它是否被 controller 接纳、是否有有效 epoch、是否完成初始 catch-up，必须通过注册与心跳链路得到远端确认。

*关键设计（斜体）：* *Broker 用 `BrokerLifecycleManager` 统一处理注册、心跳、状态迁移与 controller 回包；只有当 controller 通过心跳响应承认它已 caught up，broker 才从启动态进入真正可服务的 RUNNING。*[模式: 远端注册确认 + 持续心跳 + 状态机收敛]

## 第一层：`BrokerServer.startup()` 创建并启动 `BrokerLifecycleManager`

在 `BrokerServer.startup()` 里，broker 会创建：

- `BrokerLifecycleManager`
- `BrokerRegistrationTracker`

这表明 broker 是否加入集群，不是由 `BrokerServer` 直接硬编码，而是专门交给 lifecycle manager 管理。

## 第二层：启动后先发注册，再等初始超时

`BrokerLifecycleManager.start()` 会：

1. 安排 `initialRegistrationTimeout` 延迟事件；
2. 调用 `sendBrokerRegistration()`；
3. 等待 controller 回 `BrokerRegistrationResponse`。

如果在 `initialRegistrationTimeoutMs` 内迟迟没完成首次注册，就会走超时失败路径。

所以 broker 启动后的第一个关键动作不是接业务流量，而是**先向 controller 说明“我是谁、监听哪些 endpoint、支持哪些 feature”**。

## 第三层：注册成功后不是结束，还要持续 `heartbeat`

注册成功后，`BrokerLifecycleManager` 还会继续发送 `BrokerHeartbeatRequest`。controller 在 `ControllerApis.handleBrokerHeartbeat` 中处理，并返回：

- 是否 fenced
- 是否 shouldShutDown
- 是否 isCaughtUp

其中 `isCaughtUp` 最关键：它告诉 broker，controller 是否已经认为你的元数据状态追平到了可以服务的程度。

## 第四层：为什么 `caught up` 只是第一道门槛，而不是最后一步

`BrokerServer` 里有明确注释：controller 会通过 heartbeat response 里的 `isCaughtUp=true` 告诉 broker，它已被标记为 caught up；`BrokerLifecycleManager` 负责跟踪这个状态。更细一点看状态机源码：

- 在 `STARTING` 状态收到 `isCaughtUp=true` 时，broker 是从 `STARTING → RECOVERY`；
- 在 `RECOVERY` 状态下，只有当 heartbeat 再表明 `!isFenced` 时，broker 才从 `RECOVERY → RUNNING`。

也就是说：

```text
本地 startup 完成
  ≠ 集群已接纳
注册成功
  ≠ 一定可服务
heartbeat 收到 isCaughtUp=true
  → STARTING → RECOVERY
heartbeat 再确认 !isFenced
  → RECOVERY → RUNNING
```

这就是 Broker 的“集群感知”核心：不是看本地对象是否 new 完，而是看 controller 是否先承认你已 caught up，再进一步 unfence 你进入 RUNNING。

## 第五层：为什么还要 `BrokerRegistrationTracker`

`BrokerRegistrationTracker` 作为 metadata publisher 被加入 `metadataPublishers`，并持有 `() => lifecycleManager.resendBrokerRegistration()` 这个回调。

这至少说明 broker 注册不是完全“一次性动作”：当 tracker 识别到需要重发注册的场景时，它可以触发 `resendBrokerRegistration()`。但本篇不把所有触发条件展开成细则，因为真正的触发判定还要继续沿 tracker 的实现深挖。

## 收网：Broker 的集群身份来自 controller 的确认，不来自自我感觉

把整篇压成一句话：Broker 启动后由 `BrokerLifecycleManager` 先向 controller 发注册请求，再持续发心跳；controller 通过注册回包与 heartbeat 回包确认 broker 的 epoch、fenced 状态与 `isCaughtUp`，broker 先在 `isCaughtUp=true` 时从 `STARTING` 进入 `RECOVERY`，再在后续 heartbeat 确认 `!isFenced` 后进入 `RUNNING`；`BrokerRegistrationTracker` 则提供了重发注册的钩子。

```text
BrokerServer.startup
  → BrokerLifecycleManager.start
    → initialRegistrationTimeout + sendBrokerRegistration
      → controller registration reply
        → 周期 heartbeat
          → fenced / shutdown / isCaughtUp
            → isCaughtUp=true
              → STARTING → RECOVERY
                → !isFenced
                  → RUNNING

metadata update
  → BrokerRegistrationTracker
    → resendBrokerRegistration
```

**本篇的一句话困惑**：Broker 怎么知道自己已经真正加入 Kafka 集群并可服务了？

**本篇的一句话顿悟**：Broker 不靠本地组件启动完成来判断可用，而是靠 BrokerLifecycleManager 与 controller 的注册/心跳往返，先在 `isCaughtUp=true` 时进入 `RECOVERY`，再在 controller unfence 后进入 `RUNNING`。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Broker 进程起来就等于加入集群。”** 还要通过 controller 注册与心跳确认。
2. **“收到 `isCaughtUp=true` 就直接 RUNNING。”** 实际是先 `STARTING → RECOVERY`，再在 `!isFenced` 后进入 `RUNNING`。
3. **“Broker 状态全靠本地自己判断。”** 集群视角由 controller 决定。
4. **“注册只发一次，后面绝不会重发。”** `BrokerRegistrationTracker` 至少提供了触发重发注册的钩子。
5. **“心跳只是保活。”** 它同时传递 fenced、shutdown、caught up 等关键状态。

### 关键证据清单

- `core/src/main/scala/kafka/server/BrokerServer.scala:224`：创建 `BrokerLifecycleManager`。
- `core/src/main/scala/kafka/server/BrokerServer.scala:532`：创建 `BrokerRegistrationTracker`。
- `core/src/main/scala/kafka/server/BrokerServer.scala:547`：`isCaughtUp` 注释。
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:213`：start。
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:351`：`initialRegistrationTimeout`。
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:360`：`sendBrokerRegistration()`。
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:507`：`isCaughtUp` 驱动 `STARTING → RECOVERY`。
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:518`：`!isFenced` 驱动 `RECOVERY → RUNNING`。
- `core/src/main/scala/kafka/server/ControllerApis.scala:609`：处理 `BrokerHeartbeatRequest`。
- `core/src/main/scala/kafka/server/ControllerApis.scala:656`：处理 `BrokerRegistrationRequest`。

### 版本与实现边界

- 本文以 Kafka `v4.x`、KRaft 为基线。
- 本篇聚焦 broker 如何感知集群接纳，不展开 controller 内部状态机细节。
- 不把 `BrokerLifecycleManager` 与 KRaft Raft 状态机混为一谈。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-39`（启动流程）、`Kafka-27`（MetadataLoader / Publisher）、`Kafka-32`（Raft 总览）。
- 后续桥接：可再补 `消息丢失排查` 与 `容量评估` 两篇，明确标成“源码 + 运维桥接篇”。