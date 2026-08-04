# 08-REQ：Redis 基础设施增强（Redisson 视角，v3，Redisson 源码验证）

> v3 更新（Explore-14）：REQ-001 实现障碍标注 + REQ-N02 修正（Redisson 已有 keyspace 监听）+ D04 影响面确认。~~D09~~ 已验证为错误结论并移除。
>
> 需求分三类：
> 1. **已实现需求**（REQ-001~006，6 项）——microsphere 参考实现
> 2. **待修复**（REQ-D01~D08，8 项）——bug 修复（源码确认）
> 3. **全新发散**（REQ-N01~N05，5 项）——REQ-N02 已验证修正
>
> **基准环境**：Java 17+，Redisson 3.x，对照源码 `/data/workspace/source-code/code/spring/redisson/`
>
> **⚠️ 重要修正**：
> - REQ-N02: Redisson 已有 `RMap.addListener(MapPutListener/MapRemoveListener)`（keyspace 驱动，跨实例）——REQ 原声称"Redisson 自己要发消息"低估了现有能力
> - REQ-001: `RedissonClient.getCommandExecutor()` API 位置错误——在 `Redisson` 实现类，且外部包装 executor 不影响已创建对象
> - D09(新): ValueHolder 双向缓存因 `RawValue(byte[])` identity equals 永久失效

---

## 项目定位

**从 Redisson 的角度看，生产环境的 Redis 基础设施需要扩展什么？**

Redisson 已经是成熟的分布式数据网格——提供了 `RLock`/`RMap`/`RQueue`/`RTopic` 等高级数据结构，连接管理基于 Netty，集群模式完整支持。但它有 3 个生产缺口：(1) **没有命令级拦截 SPI**——无法在每条 Redis 命令执行前后插入自定义逻辑；(2) **没有跨实例的逻辑复制**——无法让一个实例的写操作在另一个实例重放；(3) **分布式对象缺少操作级监控**——RLock 被谁持有、RMap 写入频率如何，不可见。

microsphere-redis 的现有代码（基于 SDR `RedisConnection` 代理）覆盖了部分需求——但它的拦截点只在 SDR 层，Redisson 的直接调用不被拦截。本文从 Redisson 视角重新规划需求——无论你用的是 `redisson.getMap(key).put("k","v")` 还是 `redisson.getLock(key).lock()`，都能被统一拦截、监控、复制。

**源码信息**：现有实现参考 `/data/workspace/java-training-camp/cloud-native-code/stage-4/microsphere-redis/`，但本文需求不绑定其实现路径。

---

## 一、命令级拦截与监控

### REQ-001：Redisson 命令的透明拦截 SPI

**问题**：你想在每条 Redis 命令执行前后做点事——记录慢查询、审计写入操作、缓存失效广播。Redisson 没有命令级拦截 SPI。`RTopic` 只能做发布-订阅，不是命令级回调；`NodeListener` 是连接级事件，看不到具体命令。

**产出**：
- `RedisCommandInterceptor` SPI：`beforeExecute(RedissonMethod context)` / `afterExecute(context, result)` / `onError(context, exception)`——三个回调覆盖命令生命周期
- `RedisMethod` 上下文：`getCommand()`（如 `SET/GET/HSET`）、`getKey()`、`getArgs()`、`getCostMs()`、`isWrite()`
- 拦截点：包装 Redisson 的 `CommandExecutor`（`RedissonClient.getCommandExecutor()`）——所有命令经 `CommandExecutor.writeAsync/readAsync` 发出时触发回调
- `EventPublishingRedisCommandInterceptor`：默认拦截器——写命令成功时发布 `RedisCommandEvent`
- SPI 发现：`ServiceLoader` 加载所有 `RedisCommandInterceptor` 实现，`@Ordered` 排序执行

**状态**：[待实现]（现有 SDR 代理方案作为参考，但已确认其范围限定缺陷——见 D01）

---

### REQ-002：Redis 命令事件发布与序列化

**产出**：
- `RedisCommandEvent`：命令事件——`getCommand/getKey/getArgs/getApplicationName/getTimestamp`
- `RedisCommandEventType`：`WRITE` / `READ` / `SCRIPT`
- `RedisCommandEventPublisher`：异步发布（非线程阻塞 Redis 执行线程）→ Spring `ApplicationEvent` 或直接回调
- 事件序列化：支持 JSON/Protobuf，**不用 JDK 序列化**（安全风险）

**状态**：[待实现]

---

## 二、跨实例 Redis 逻辑复制

### REQ-003：Kafka 异步复制 Redis 写操作

**问题**：多机房架构中，机房 A 删了缓存，机房 B 需要同步失效。Redisson 没有跨实例的命令重放机制——你需要自己写 Kafka 生产者/消费者。需要一个可配置的逻辑复制管道：生产者端过滤 domain → 异步发 Kafka → 消费者端反序列化 → 重放命令到目标 Redisson 实例。

**产出**：
- `RedisCommandReplicator`：核心复制引擎——`produce(RedisCommandEvent)` → 序列化 → Kafka → `consume(topic, event)` → 反序列化 → `replay(targetRedisson, event)`
- `DomainFilter`：按 `domain` 过滤——每个 domain 映射到一组 Redis key 前缀，只复制匹配的命令
- `ReplicatorConfig`：`broker/topic/domains/consumerGroup` 配置模型
- `RedisCommandPartitioner`：按 key 分区保证同 key 有序
- 消费者：`auto.offset.reset=earliest` + batch ack + 异常时 `seek()` 重试（不丢消息）

**状态**：[待实现]（现有 Kafka replicator 实现作为参考，但需修 CONSUMER 缺陷——见 D02）

---

## 三、分布式对象操作监控

### REQ-004：Redisson 分布式对象的操作级指标

**问题**：RLock 获得了多少次？平均持锁时间？RMap 的写入频率是多少？——Redisson 没有任何面向操作的指标。运维想知道"分布式锁竞争率"、"RMap 热点 key"、"RQueue 消费 lag"——当前全部不可见。

**产出**：
- `RedissonObjectMetrics`：按对象类型（LOCK/MAP/QUEUE/SET/TOPIC）聚合指标
  - `RLock`：`acquire_count` / `acquire_time_avg` / `contention_rate`（attempts / success）
  - `RMap`：`put_count` / `get_count` / `size_bytes_estimate`
  - `RQueue`：`offer_count` / `poll_count` / `current_size`
- Micrometer Counter + Gauge + Timer 导出，Redisson 的 `CommandExecutor` 拦截点采集

**状态**：[待实现]

---

## 四、连接与健康管理

### REQ-005：Redisson 连接池监控与健康检查

**产出**：
- `RedissonHealthIndicator`：`PING` 各节点 + 延迟 + 角色（master/slave）——UP/DOWN 状态
- `RedissonPoolMetrics`：连接池 active/idle/pending/waiters 指标 → Micrometer
- `RedissonHealthEvent`：节点 UP/DOWN/ROLE_CHANGE 事件发布

**状态**：[待实现]

---

## 五、配置管理

### REQ-006：Redisson 配置的热更新与审计

**产出**：
- `RedissonConfigListener`：监听 `EnvironmentChangeEvent` → 解析 Redisson 配置前缀 → 对比新旧值 → 发布 `RedissonConfigChangedEvent`
- `RedissonConfigAuditor`：记录配置变更历史（old→new + timestamp + 来源）
- 热更新的能力范围文档化：哪些变更有副作用（如连接池大小）需重启提示

**状态**：[待实现]

---

## 六、现有实现的缺陷修正（待实现）

> 以下缺陷基于 microsphere-redis 现有 SDR 代理实现的源码审查。即使切换到 Redisson 路径，其中部分设计缺陷的教训仍然适用。

### REQ-D01：拦截范围限定——仅 SDR，Redisson 不可见

**问题**：现有实现通过 `RedisConnectionFactoryProxyBeanPostProcessor` 代理 SDR 的 `RedisConnection`，Redisson 的直接调用不走 SDR → 完全不可见。这导致拦截系统只能覆盖一半流量。

**方案**：增加 Redisson 的 `CommandExecutor` 包装路径，与 SDR 代理并行。

---

### REQ-D02：Kafka 消费者无幂等/乱序/重试保障

**问题**：`consumeRecord` catch Throwable 仅 warn + batch ack 仍提交 → 消息丢失。

**方案**：异常时 `consumer.seek(partition, offset)` 重新消费；或持久化 offset 到 Redis，重启恢复。

---

### REQ-D03：事件序列化单字节长度前缀 >255 截断

**问题**：`outputStream.write(bytesLength)` 单字节——大 value 写命令序列化损坏。

**方案**：改用变长编码（VarInt）。

---

### REQ-D04：单参数命令 write 误判

**问题**：`RedisMethodContext.initParameters()` 参数数≤1 时 write 被强制 false——`del/exists` 等写命令被误判为读。

**方案**：元数据驱动的 write 判断，不靠参数数量推断。

---

### REQ-D05：ThreadLocal 永远不清理

**问题**：`ValueHolder`（对象↔bytes 双向缓存）主代码从不 clear()。

**方案**：命令生命周期结束时 finally 块 clear + 非 Web 场景（定时任务/自定义线程池）加 close hook。

---

### REQ-D06：等值事件 equals/hashCode 契约违反

**问题**：`RedisCommandEvent.equals` 深比较含 byte[] args，`hashCode` 浅 hash——等值事件 hash 不同，HashMap 中查找失败。

**方案**：`hashCode` 改用 `Arrays.deepHashCode(args)`。

---

### REQ-D07：异常传播被 NPE 覆盖

**问题**：`InterceptingRedisConnectionInvocationHandler.invoke()` 中 `throw e.getCause()`——非 `InvocationTargetException` 时 `getCause()=null` → `throw null` = NPE。

**方案**：区分异常类型，非 InvocationTargetException 时直接 throw e。

---

### REQ-D08：元数据强依赖静态 YAML

**问题**：`spring-data-redis-metadata.yaml`（9825 行一次性生成）缺失/损坏→启动失败；SDR 升级→YAML 过时。

**方案**：运行时反射推断（方法名前缀启发式 30+ 规则）+ 静态 YAML 作为优化加速路径。

---

> ~~D09: ValueHolder 双向缓存永久失效~~ —— **已验证为错误结论**。Java 17 `record RawValue(byte[] data)` 自动生成的 `equals()` 使用 `Arrays.equals(byte[], byte[])` 内容比较，非 identity。ValueHolder 缓存失效另有根源，当前不确认真实 bug。（2026-08-04 Explorer-14 结论被记录级实现纠正）

---

## 七、发散需求

### REQ-N01：Redisson RLock 的分布式锁竞争监控与告警

**生产痛点**：线上某个业务的 RLock 竞争率突然飙升——所有线程都在等锁但不知道。当前完全看不到 RLock 的获取/等待/超时数据。

**产出**：`RLockMetrics` —— 拦截 `RLock.tryLock()` → 记录 `attempts/success/timeouts/avgWaitMs` → Meter Gauge `redisson_lock_contention_rate`。超过阈值（默认 20%）时发布 `LockContentionEvent`，可触发告警。

**状态**：[待实现]

---

### REQ-N02：RMap 的变更广播（跨实例缓存同步）

**生产痛点**：多实例部署时，实例 A 更新了 `RMap.put("config", newVal)`，实例 B 的本地缓存（或业务缓存）不会自动感知。Redisson 有 `RTopic` 可以自己发消息，但需要统一的变更事件抽象。

**产出**：`RMapChangeListener` —— 拦截 `RMap.put/remove/clear` → 自动发布 `RMapChangeEvent(mapName, key, oldValue, newValue)` → 通过 `RTopic` 广播给所有实例 → 监听器消费做本地动作。

**状态**：[待实现]

---

### REQ-N03：Redisson 命令级慢查询日志与 Pinpoint 追踪

**生产痛点**：想定位 "哪个 key 的 GET 操作平均耗时超过 5ms"——Redisson 没有慢查询日志。运维只能从 Redis Server `SLOWLOG` 看，但无法关联到应用端的调用方。

**产出**：`RedissonSlowCommandLogger` —— 从命令拦截点记录 >threshold 的命令（command/key/args/costMs/callerStack）→ 结构化日志 / Micrometer Timer。

**状态**：[待实现]

---

### REQ-N04：Redisson 多数据源动态路由

**生产痛点**：多租户/读写分离需要根据业务上下文选择 Redisson 实例——租户 A 的 RLock 发到 Redis-A，租户 B 的发到 Redis-B。Redisson 配置层的 `readMode`（MASTER/SLAVE/MASTER_SLAVE）只能做连接级的读操作路由——无法做"这个 key 的写入路由到实例 A"这样的程序化路由。

**产出**：`RedissonRouter` —— 配置 `writeClient`/`readClient` → 拦截命令 → `isWrite()` 时路由到 `writeClient`，否则到 `readClient`。支持按 key 前缀路由（多租户）。

**状态**：[待实现]

---

### REQ-N05：Redisson 命令代理的 Micrometer 统一指标

**生产痛点**：不同团队用不同方式监控 Redis——有人看 `SLOWLOG`、有人 grep 日志、有人用 APM。每个项目都要重新写一遍监控代码。

**产出**：`RedissonCommandMetrics` —— 基于命令拦截 SPI（REQ-001）→ 为每条命令注册 `Timer("redis.command.duration", "command", keyPrefix)` + `Counter("redis.command.count", "command", "result")`——Micrometer 标准命名，Prometheus 直接可用。

**状态**：[待实现]

---

## 八、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-02 | v2 重写——从 Redisson 视角重构全部需求，突破 SDR 范围限定 |
