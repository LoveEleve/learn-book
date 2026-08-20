# Curator — 知识网络化规划 (C-1~C-8, 09 怀疑审计后 v1)

> **日期**: 2026-08-15 | **依据**: issue/源码分析执行计划.md 阶段4.5 (5 域) + 09 对既有规划保持怀疑 全量重审
> **源码**: `/data/workspace/source-code/code/spring/curator` (**Apache Curator 5.8.0**, pom.xml:34 实证; 主模块 curator-client 43 文件 + curator-framework 185 文件 + curator-recipes 107 文件 + curator-x-discovery 32 文件; curator-x-async 116 文件经设计决策测试**排除**)
> **定位**: 阶段 4.5 — 消息与事务第七环 **ZK 客户端库与分布式协调配方 (框架层 + recipes)**
> **知识网络**: 与 ZooKeeper (4.3, 底层协议/会话/watch 机制) + Kafka (4.2, 协调对照) + Seata (4.4, 事务对照) + SofaJRaft (4.6, Raft 对照) 互联; 为 Spring Cloud Zookeeper (阶段5) 提供连接面

---

## 〇、09 怀疑审计表 (Curator, 2026-08-15) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 5 个** (C-1~C-5) | 顶层包扫描 (curator-client 43 + framework 185 + recipes 107 + x-discovery 32 + x-async 116) | 未覆盖大包: **shared (8)+atomic (11) 版本 CAS 族**、**nodes (5) 跨会话保活**、**x-discovery (32) 服务发现**、**x-async (116)**、framework/api/transaction (15)、schema (7) | **修正** ⚠ 新增 C-6 (shared+atomic)、C-7 (nodes)、C-8 (x-discovery); x-async 排除 (见下); transaction/schema 并入 C-1 |
| C-1 "RetryPolicy(**5 种**)" | ls retry/ 穷举 | **6 种具体策略** (ExponentialBackoffRetry/RetryForever/RetryNTimes/RetryUntilElapsed/RetryOneTime/BoundedExponentialBackoffRetry) + 抽象基类 SleepingRetry | **修正** ⚠ 6 种 |
| C-1 "ConnectionState" | ConnectionState.java 枚举 | **5 态** (CONNECTED/SUSPENDED/RECONNECTED/LOST/READ_ONLY, 带 isConnected 抽象方法, framework/state/ConnectionState.java:29-94) | **接受+补充** ✅ |
| C-2 "LeaderLatch(临时顺序节点)" | LeaderLatch.java (683) | 手写 EPHEMERAL_SEQUENTIAL + 前驱 watch + getData 校验 ephemeralOwner; LeaderSelector 复用 **InterProcessMutex** (LeaderSelector.java:60-64 注释实证) | **接受+补充** ✅ (selector 复用锁 — 拓扑 C-3 先于 C-2) |
| C-3 "InterProcessMutex(可重入)" | InterProcessMutex.java (221) + LockInternals.java (288) | 可重入经线程本地锁计数 + 顺序节点排序 + 前驱 watch (internalLockLoop L223-271); "可重入"语义需 C-3 穷举 (LockInternals 无 lock 计数 — 在 InterProcessMutex 内) | **接受+待验证** ✅ |
| C-4 "DistributedQueue/DistributedBarrier" | queue/ (19 文件) + barriers/ (2 文件) | DistributedQueue (656) 顺序节点+ChildrenCache 版本驱动消费循环; 另有 Priority/Id/Delay/SimpleDistributedQueue 四变体 + QueueSharder (257) 未在规划内 | **接受+补充** ✅ (四变体并入 C-4) |
| C-5 "TreeCache/PathChildrenCache/NodeCache" | cache/ (36 文件) | **旧三件套全部 @Deprecated** — 新一代 **CuratorCache** (基于 ZK 3.6+ PERSISTENT_RECURSIVE watcher + cversion 差分, CuratorCacheImpl.java:82-87) 未在规划内 | **修正** ⚠ 三件套+CuratorCache 一并纳入 |
| C-5 "PathChildrenCache STRONG/ENDPOINT 模式" (面试常识) | PathChildrenCache.java:282-302 枚举 | **5.8.0 仅 3 模式** (NORMAL/BUILD_INITIAL_CACHE/POST_INITIALIZED_EVENT); STRONG/ENDPOINT 为 1.x Netflix 时期历史枚举, 2.0 起移除 | **修正** ⚠ (依赖被修正, 不写入大纲) |
| C-4 "DistributedBarrier" (plan 提及) | DistributedBarrier.java (126) | 单持久节点"发令枪" + 存在性轮询; DistributedDoubleBarrier (293) 双屏障成员计数 + ready 节点 | **接受** ✅ |
| 版本 | pom.xml:34 | **5.8.0** (执行计划无版本声明) | **补充** ✅ |
| Curator→ZK 依赖 (STAGE3 注记) | maven 坐标 | curator-framework 依赖 zookeeper 3.9.x; 阶段4.3 ZK 已 9/9 收官 → 拓扑满足 | **接受** ✅ |
| x-async 模块 (116 文件, 未规划) | 设计决策测试 (00 §3) | typed/ 33 文件全为 50-90 行机械样板 (TypedZPath0-10/TypedModelSpec0-9); AsyncCuratorFrameworkDsl 为同步 API 的 CompletionStage 镜像 — **薄封装, 无框架级新决策**; 面试/生产双低 | **排除** ✂️ |
| client/utils (17 文件) | 设计决策测试 | PathUtils/ZKPaths/ThreadUtils/EnsurePath 等纯工具类 | **排除** ✂️ (EnsurePath 等并入 C-1 上下文引用) |
| client/drivers (4 文件) | 设计决策测试 | TracerDriver 观测面, 无独立算法决策 | **并入 C-1** ✅ |

**覆盖率报告**: 既有规划 5 域 → 重审后 **8 域** (160%, +3 解释: shared/atomic 与 nodes 通过设计决策测试 — 版本 CAS 与跨会话保活均为定义特征级分布式原语; x-discovery 32 文件承载服务注册/发现决策)。修正 **4 处** (RetryPolicy 5→6/PathChildrenCache 模式/CuratorCache 新增/域清单 5→8)。

---

## 一、入口点与主线

`CuratorFrameworkFactory.newClient() (CuratorFrameworkFactory.java:90-111) → Builder.build() (:186-188) → CuratorFrameworkImpl (881) → CuratorZookeeperClient (425) → ConnectionState (293) → ZooKeeper` — 启动主线: `start() (:284-329) → ConnectionStateManager.start (必须先于 client) → client.start → backgroundOperationsLoop (单线程 DelayQueue, :786-805)` — 操作主线: `create()/delete()/getData() 等 fluent builder → BackgroundOperation → RetryLoop (可重试) → ZK` — 配方主线: `InterProcessMutex (C-3) / LeaderLatch·LeaderSelector (C-2) / DistributedQueue (C-4) / TreeCache·CuratorCache (C-5) / SharedValue·DistributedAtomicValue (C-6) / PersistentNode (C-7) / ServiceDiscovery (C-8)`。

## 二、域清单 (8 域: 4🔴 + 4🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| C-1 | **CuratorFramework 核心+客户端层** | curator-client (CuratorZookeeperClient 425/ConnectionState 293/RetryLoopImpl) + curator-framework (CuratorFrameworkFactory 675/CuratorFrameworkImpl 881/ConnectionStateManager 329/NamespaceImpl 93) + api/ (88) + transaction (15) + schema (7) | Builder→start 生命周期/连接状态机 5 态/RetryLoop 重试 6 策略/namespace 门面/后台操作队列/fluent API 面/事务 | 🔴 A |
| C-2 | **Leader 选举** | recipes/leader/ LeaderLatch (683) + LeaderSelector (558) + Participant | 临时顺序节点+前驱 watch 被动选举 / 锁复用回调式选举 / 连接状态联动 | 🔴 A |
| C-3 | **分布式锁** | recipes/locks/ InterProcessMutex (221) + LockInternals (288) + StandardLockInternalsDriver (93) + ReadWriteLock + Semaphore (18 文件) | 顺序节点排队 / 前驱 watch 通知链 / 可重入计数 / 锁升级 / 读写锁 / 信号量 | 🔴 A |
| C-4 | **队列与屏障** | recipes/queue/ DistributedQueue (656) + QueueBuilder (256) + SimpleDistributedQueue (243) + 3 变体 + QueueSharder (257) + barriers/ (2 文件) | 顺序节点队列 / ChildrenCache 版本驱动消费 / 锁安全模式 / 优先级·ID·延迟队列 / 单双屏障 | 🟡 B |
| C-5 | **缓存与监听** | recipes/cache/ TreeCache (816) + PathChildrenCache (788) + NodeCache (310) + CuratorCacheImpl (261) + watch/PersistentWatcher (159) (36 文件) | 三件套 mzxid/CAS 一致性 / CuratorCache 持久 watcher+cversion 差分 / 事件语义 / INITIALIZED 三种实现 | 🔴 A |
| C-6 | **共享状态与原子量** | recipes/shared/ SharedValue (291) + SharedCount (169) + recipes/atomic/ DistributedAtomicValue (297) + DistributedAtomicLong (211) + CachedAtomicLong (19 文件) | zxid 单调性 / 版本 CAS / 乐观重试→锁升级 / 分块缓存 | 🟡 B |
| C-7 | **持久节点与组成员** | recipes/nodes/ PersistentNode (509) + PersistentTtlNode (257) + GroupMember (143) (5 文件) | 三触发跨会话重建 / CONTAINER+TTL 心跳 / 组成员视图 | 🟡 B |
| C-8 | **服务发现** | curator-x-discovery/ ServiceDiscoveryImpl (1488 总行) + ServiceProviderImpl + ServiceCacheImpl + DownInstanceManager + 5 种 ProviderStrategy (32 文件) | 注册/发现/缓存/实例策略/URI 模板 | 🟡 B |

## 三、执行顺序 (拓扑: 框架层 → 锁 → 选举 → 队列 → 缓存 → 原子 → 节点 → 发现)

**C-1 → C-3 → C-2 → C-4 → C-5 → C-6 → C-7 → C-8**

> 拓扑理由: 框架核心 (C-1, 一切配方的地基) → 锁 (C-3, 顺序节点排队算法, 被选举/原子量复用) → 选举 (C-2, LeaderSelector 复用 InterProcessMutex) → 队列 (C-4, children watcher 模式) → 缓存 (C-5, watcher+存储一致性) → 共享状态/原子量 (C-6, PromotedToLock 升级用锁) → 持久节点 (C-7, GroupMember 依赖 CuratorCache) → 服务发现 (C-8, ServiceCache 基于缓存, 收束应用面)。

## 四、知识网络图

```
← 复用: ZooKeeper (4.3 会话/watch/顺序节点语义) + Seata (4.4 分布式事务对照)
→ 引出: SofaJRaft (4.6) + 阶段 5 Spring Cloud Zookeeper (服务发现消费 C-8) + Dubbo/微服务注册中心面
```

## 五、完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (5/8, +3 说明)
- [x] 09 审计: 修正 4 处 (RetryPolicy 5→6/PathChildrenCache 模式/CuratorCache 新增/域数), 排除 2 处 (x-async/utils)
- [x] 数字断言穷举 (6 retry/5 连接态/36 cache 文件/19 queue 文件等实证)
- [x] **C-1 ✅ 2026-08-15** (harness 28/28: 默认值/生命周期/重试/namespace/保护模式/后台回调/断连重连/事务/错误策略)
- [x] **C-3 ✅ 2026-08-15** (harness 22/22: 互斥/可重入/FIFO 实证/超时清理/会话崩溃释放/读写锁/信号量/MultiLock)
- [x] **C-2 ✅ 2026-08-15** (harness 15/15: 唯一性/事件驱动接替/await 阻塞/participants/任期/autoRequeue)
- [x] **C-4 ✅ 2026-08-15** (harness 18/18: FIFO/消费回调/flushPuts/优先级/延迟/IdQueue/SimpleQueue/双屏障)
- [x] **C-5 ✅ 2026-08-15** (harness 19/19: NodeCache/PathChildrenCache 三事件/三种启动模式/INITIALIZED/TreeCache 级联/CuratorCache/SINGLE_NODE/桥接)
- [x] **C-6 ✅ 2026-08-15** (harness 25/25: 版本 CAS 互斥/并发累加 50/compareAndSet/initialize/号段/SharedCount)
- [x] **C-7 ✅ 2026-08-15** (harness 12/12: 创建/自动重建/setData/close 删除/TTL 父子/组成员 — 发现 extendedTypesEnabled 部署前提)
- [x] **C-8 ✅ 2026-08-15** (harness 8/8: 注册/缓存/轮询/更新/降级熔断/注销)
- [x] **阶段 4.5 全量收官 8/8** (2026-08-15): 48 节大纲 + 185 问/深审 110 项核对/harness 8 个 **147/147 断言全 PASS** + HANDOFF-CURATOR.md v1 (REVIEW 修复 4 锚点 + 锚点密度补强)
