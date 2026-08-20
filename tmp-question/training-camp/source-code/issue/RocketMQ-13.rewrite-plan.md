# RocketMQ-13 重写规划

> 题目：单机 CommitLog 为什么还不等于可靠消息 —— Master/Slave 复制主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把传统 Master/Slave HA 写成一条从本地追加、网络复制、Slave 物理日志追赶到生产确认的失败推进链，不提前吞并 DLedger、Controller 与自动选主。

## 1. 读者困惑

- Producer 看到 Master 本地 `CommitLog` 写成功，为什么还不能直接认为消息可靠？
- Master 和 Slave 到底复制什么，是复制 ConsumeQueue，还是复制 CommitLog 字节？
- Slave 怎样知道从哪里开始追，Master 又怎样知道 Slave 已经追到哪里？
- SYNC_MASTER 和 ASYNC_MASTER 的确认边界有什么不同？复制超时后，消息究竟处于什么状态？
- 传统 Master/Slave 为什么能提高消息存活能力，却不能自动回答“谁来接管继续写”？

## 2. 一句话顿悟

**CommitLog 的本地追加只建立了 Master 的单机事实；传统 HA 还要把同一段物理日志按 offset 传到 Slave，并由 Slave 回报已追加位置，Master 再依据复制确认和超时决定是否向 Producer 返回成功。它解决的是副本追赶与确认边界，不是自动选主。**

## 3. 五要素卡片

### 读者问题

单机 CommitLog 已经顺序写入文件，为什么还需要 Master/Slave？生产请求的“成功”到底由谁决定？

### 入口

- `DefaultMessageStore.start()` 初始化并启动 `HAService`
- Master 侧 `DefaultHAService.start()` 启动 `AcceptSocketService`
- Slave 侧 `DefaultHAService.init()` 创建 `DefaultHAClient`
- `CommitLog.asyncPutMessage()` 在本地追加后进入 `handleDiskFlushAndHA()`

### 状态核心

- Master 当前 CommitLog 物理写入位置
- `DefaultHAConnection.nextTransferFromWhere`
- `DefaultHAConnection.slaveAckOffset`
- Slave `DefaultHAClient.currentReportedOffset`
- `GroupCommitRequest.nextOffset`、ack 数量和截止时间
- Master 的连接数与同步副本判断

### 失败路径

- Slave 尚未连接：同步确认无法完成
- Slave 追赶落后超过阈值：副本被视为不同步
- 网络断开或 housekeeping 超时：连接关闭并从连接列表移除
- 复制等待超过 `slaveTimeout`：Producer 得到 `FLUSH_SLAVE_TIMEOUT`
- Slave 追加 offset 不连续：拒绝继续处理，连接失败
- Master 宕机：传统 HA 本身没有自动选主，后续由 Controller/DLedger 等机制解决

### 连接点

- 前文 `RocketMQ-4`：复制对象是 CommitLog，而不是 ConsumeQueue
- 前文 `RocketMQ-5/6`：Slave 追加日志后由 Reput 重新构建消费索引
- 后文 `RocketMQ-14/15`：DLedger、Controller 和选主会改变复制与角色切换模型
- 后文 `RocketMQ-16`：故障恢复会继续讨论可见性、重复投递和恢复边界

## 4. 总图

```text
Producer
  → Master CommitLog 本地追加
    → 刷盘路径与 HA 路径并行推进
      → Master 通过 HAConnection 发送 [物理 offset, 长度, 日志字节]
        → Slave HAClient 校验 offset 连续性
          → Slave appendToCommitLog()
            → Slave 回报当前最大物理 offset
              → Master GroupTransferService 检查 ack 数量/截止时间
                → Producer 得到 PUT_OK 或 FLUSH_SLAVE_TIMEOUT
```

## 5. 关键边界

- 本篇只讲传统 `DefaultHAService` / `DefaultHAConnection` / `DefaultHAClient` 主从复制。
- 不把“本地刷盘成功”“Slave CommitLog 追加成功”“Slave 消费索引已构建”“Producer 收到成功”混成同一个时刻。
- 不把传统 Master/Slave 写成 Raft；它没有在本篇内完成自动选主、任期、日志多数派提交和角色切换。
- 不展开 DLedger、Controller、JRaft 的实现，只在结尾说明它们为什么需要单独成篇。

## 6. 失败方案推演

1. **只要 Master 本地追加成功就返回成功**：直觉上延迟最低，但 Master 在副本形成前宕机时，Producer 已经拿到成功而系统可能没有可恢复副本。
2. **Master 发出多少就认为 Slave 拥有多少**：直觉上省掉回报协议，但网络写出不等于 Slave 校验和追加完成，无法支撑同步确认。
3. **收到数据后不检查物理 offset**：直觉上可以直接追加，但断线重连、重复发送或缺口会破坏 CommitLog 连续性。
4. **把 Slave 回报 offset 当成追加成功证明**：理想协议应如此，但 5.3.1 当前 `DefaultHAClient` 没有检查 `appendToCommitLog()` 返回值，正文必须把它作为失败边界写出。

## 7. 误解清单

- 本地 CommitLog 写成功不等于副本已形成。
- 传统 HA 复制 CommitLog，不是复制 ConsumeQueue。
- `SYNC_MASTER` 不代表默认每条消息都等待 Slave；默认 `inSyncReplicas=1`。
- `FLUSH_SLAVE_TIMEOUT` 是确认窗口超时，不是消息最终状态判决。
- 传统 Master/Slave 不负责自动选主。

## 8. 证据清单

- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAService.java:125`
- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAConnection.java:333`
- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAClient.java:189`
- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAClient.java:203`
- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:1248`
- `store/src/main/java/org/apache/rocketmq/store/ha/GroupTransferService.java:79`
- `store/src/main/java/org/apache/rocketmq/store/CommitLog.java:1297`
- `store/src/main/java/org/apache/rocketmq/store/config/MessageStoreConfig.java:322`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇只写传统 `DefaultHAService` 主从复制；DLedger、Controller、JRaft、AutoSwitch 留给后续篇目。
- 目标正文：8000~12000 字；核心拆解层分别覆盖复制对象、连接推进、offset 校验、Reput 派生、ack/超时与角色边界。

## 10. 本轮重写主线

1. 从“Master 本地写成功为什么仍不可靠”开场。
2. 先区分单机事实、本地刷盘、副本复制和 Producer 确认四个边界。
3. 解释 Master 如何监听 HA 端口、建立 `HAConnection`，Slave 如何由 `HAClient` 主动连接。
4. 沿数据包格式讲清物理 offset、body size 和 CommitLog 字节的传输。
5. 解释 Slave 的连续 offset 校验、追加 CommitLog、Reput 重建 ConsumeQueue 和 offset 回报，并保留追加失败未被检查的源码边界。
6. 解释 `GroupCommitRequest` 如何把复制确认接回 Producer，以及 ack/超时如何形成结果。
7. 收网：传统 HA 提高的是副本存活和确认可靠性，但不负责故障后的自动接管；下一篇再进入 DLedger。
