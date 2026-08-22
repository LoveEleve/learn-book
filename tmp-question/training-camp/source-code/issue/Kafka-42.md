# Kafka-42. CheckPoint 机制——recovery-point 与 log-start-offset 两类检查点的恢复边界

> 场景：broker 重启时，Kafka 既不想把每个分区从头“傻重放”一遍，也不能完全不记住上次已经 flush 到哪里、日志可读下界又在哪里。于是它用两类 checkpoint 文件把这些边界保存到磁盘：`recovery-point-offset-checkpoint` 与 `log-start-offset-checkpoint`。本篇不把它们讲成“简单起点”，而是讲清楚它们在恢复流程里分别限制什么。

## 先把真正的困惑摆出来：checkpoint 到底是在记录“从哪里开始恢复”吗

最容易写错的一句话就是：

> recovery-point-offset-checkpoint 记录恢复起点，重启时从这个 offset 之后开始回放。

这句话太粗，会把恢复流程讲窄。真实语义更接近：**它记录每个 log 已经安全 flush / 恢复到哪里的边界，broker 启动时会先读这份边界，再结合 clean shutdown 与实际日志段决定是否需要恢复、恢复多少。**

*关键设计（斜体）：* *Kafka 用 `recovery-point-offset-checkpoint` 记录每个分区上次已持久化到的恢复边界，用 `log-start-offset-checkpoint` 记录每个分区当前保留下来的可读下界；启动时 `LogManager` 先读这两份 checkpoint，再把它们交给 `loadLog(...)` 恢复具体分区。checkpoint 约束的是“恢复边界”和“日志下界”，不是一句话能概括的“统一 replay 起点”。*[模式: 启动前读取 checkpoint + 按分区恢复边界 + clean/unclean 分流]

## 第一层：`recovery-point-offset-checkpoint` 记录的是“恢复边界”

`recovery-point-offset-checkpoint` 按 `TopicPartition -> offset` 保存每个 log 的 recovery point。

它的核心作用不是单纯告诉你“从这里往后扫”，而是让 `LogManager` 在重启装载日志时知道：**这个 log 上次已经安全 flush / recover 到哪里，哪些尾部数据可能还需要检查或恢复。**

所以它更像“恢复安全边界”，而不是“唯一 replay 起点”。

## 第二层：`log-start-offset-checkpoint` 记录的是“可读下界”

`log-start-offset-checkpoint` 同样是按分区保存 offset，但语义完全不同：它记录的是当前 log 的 `logStartOffset`。

这回答的是另一个问题：**这个分区当前最早还能读到哪里。**

如果 retention、删除、截断已经把前面的 segment 清掉了，重启时 broker 不能再假装那些更早 offset 还存在，于是必须从 checkpoint 恢复这个下界。

## 第三层：启动时 `LogManager` 先读 checkpoint，再决定恢复策略

`LogManager.loadLogs()` 会先尝试读取：

- `recovery-point-offset-checkpoint`
- `log-start-offset-checkpoint`

如果读取失败，源码分别采用更保守的回退：

- recovery point 读失败时，回退为空 map，相当于恢复边界按 0 处理；
- log start offset 读失败时，回退为空 map，相当于后续按首个 segment 的 base offset 兜底。

随后 `loadLog(...)` 会拿着这两份 map、再结合该目录是否有 clean shutdown 文件，决定每个 log 是否跳过恢复、还是执行恢复。

## 第四层：clean shutdown 与 unclean shutdown 会改变恢复量

`LogManager` 启动时先看 `CleanShutdownFile`：

- 如果存在 clean shutdown 文件，说明上次是干净关闭，可以跳过大部分恢复；
- 如果没有，就把该目录记为 unclean log dir，并进入恢复路径。

这一步很关键，因为 checkpoint 不是单独决定恢复逻辑的；它必须和“上次是不是干净退出”一起看。

```text
读取 checkpoint
  + 读取 clean shutdown 标记
    → 决定是否 recover logs
      → loadLog(...)
```

## 第五层：checkpoint 的写入不是只发生在关闭时

`flushDirtyLogs()` 会周期性遍历当前 logs，只要某个 log 超过 `flushMs` 且有未刷盘数据，就执行 `log.flush(false)`。

与此对应，checkpoint 也不是只在 broker 退出时才有意义；它是 LogManager 持续维护恢复边界与日志下界的一部分。否则一旦 crash，恢复只能从更保守的位置重新检查。

## 收网：checkpoint 记录的是恢复与可读边界，不是单一 replay 起点

把整篇压成一句话：Kafka 用 `recovery-point-offset-checkpoint` 保存每个分区上次已安全 flush / recover 到哪里的边界，用 `log-start-offset-checkpoint` 保存每个分区当前可读日志下界；broker 启动时 `LogManager` 先读取这两类 checkpoint，再结合 clean shutdown 文件把它们交给 `loadLog(...)`，由具体 log 决定是否需要恢复以及恢复多少。

```text
broker 启动
  → LogManager.loadLogs()
    → 读 recovery-point-offset-checkpoint
    → 读 log-start-offset-checkpoint
    → 读 clean shutdown file
      → loadLog(...)
        → 决定跳过恢复 / 执行恢复
```

**本篇的一句话困惑**：checkpoint 到底是在记录什么，为什么不能简单理解成“恢复从这里开始”？

**本篇的一句话顿悟**：recovery-point 记录的是每个 log 的恢复安全边界，log-start-offset 记录的是日志可读下界；它们和 clean shutdown 一起约束 `loadLog(...)` 的恢复行为，而不是一句“从某个 offset 开始 replay”能讲清的。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“recovery-point-offset-checkpoint 就是统一 replay 起点。”** 它更准确是每个 log 的恢复边界。
2. **“log-start-offset-checkpoint 和 recovery point 是一回事。”** 前者是可读下界，后者是恢复安全边界。
3. **“checkpoint 一读完就能决定全部恢复逻辑。”** 还要结合 clean shutdown 与实际日志段状态。
4. **“checkpoint 只在正常关闭时才重要。”** crash recovery 同样依赖它们缩小恢复范围。
5. **“checkpoint 读失败就无法启动。”** 源码有保守回退路径。

### 关键证据清单

- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogManager.java:38`：`RECOVERY_POINT_CHECKPOINT_FILE`。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogManager.java:39`：`LOG_START_OFFSET_CHECKPOINT_FILE`。
- `core/src/main/scala/kafka/log/LogManager.scala:441`：读取 recovery point checkpoint。
- `core/src/main/scala/kafka/log/LogManager.scala:450`：读取 log start offset checkpoint。
- `core/src/main/scala/kafka/log/LogManager.scala:471`：clean shutdown 时跳过恢复。
- `core/src/main/scala/kafka/log/LogManager.scala:475`：unclean shutdown 时进入恢复。
- `core/src/main/scala/kafka/log/LogManager.scala:486`：`loadLog(...)` 接收两类 checkpoint map。
- `core/src/main/scala/kafka/log/LogManager.scala:1475`：周期性 `flushDirtyLogs()`。

### 版本与实现边界

- 本文以 Kafka `v4.x` 为基线。
- 本篇聚焦 checkpoint 与启动恢复边界，不展开 `UnifiedLog` 内部每一步恢复算法。
- 不把 recovery point 说成“绝对 replay 起点”。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-17/18/19`（log/索引/producer state）。
- 后续桥接：可继续补“消息丢失排查”时把 checkpoint、flush、HW 串起来。