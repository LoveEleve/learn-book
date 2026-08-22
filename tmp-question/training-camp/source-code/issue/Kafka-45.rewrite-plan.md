# Kafka-45 重写规划

> 题目：Broker 怎么感知自己在集群里的身份——BrokerLifecycleManager、注册与心跳主链

## 1. 读者困惑
- Broker 为什么本地启动完成还不算真正可服务？
- 注册、心跳、caught up 分别代表什么？
- Broker 怎么知道 controller 接纳了自己？

## 2. 一句话顿悟
**Broker 通过 BrokerLifecycleManager 向 controller 注册并持续发送心跳；只有收到 controller 侧的 `isCaughtUp=true`，它才真正从启动态收敛到 RUNNING。**

## 3. 失败方案推演
- 只靠本地状态判断可用，会在 controller 尚未接纳时对外服务
- 把注册看成一次性动作，忽略重注册

## 4. 章节问题
- startup 为什么先建 lifecycle manager？
- sendBrokerRegistration 发送什么？
- heartbeat 为什么不只是保活？

## 5. 至少要排除的误解
- 进程起来就等于加入集群
- 注册成功就一定可服务
- 心跳只是保活

## 6. 关键证据清单
- `core/src/main/scala/kafka/server/BrokerServer.scala:224`
- `core/src/main/scala/kafka/server/BrokerServer.scala:532`
- `core/src/main/scala/kafka/server/BrokerServer.scala:547`
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:213`
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:351`
- `core/src/main/scala/kafka/server/BrokerLifecycleManager.scala:360`

## 7. 版本与实现边界
- Kafka v4.x KRaft
- 只抓 broker 集群感知，不展开 controller 内部状态机

## 8. 字数预算
- 5000~8000 字