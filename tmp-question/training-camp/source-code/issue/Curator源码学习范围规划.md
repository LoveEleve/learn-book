# Curator 源码学习范围规划

> **版本**: v5.8.0
> **仓库**: `/data/workspace/source-code/code/spring/curator/`
> **规模**: client(43) + framework(185) + recipes(107) = 335 个核心源文件
> **日期**: 2026-08-03

---

## 一、仓库概况

Apache Curator 是 ZooKeeper 的高级客户端框架，解决 ZK 原生 API 的三大痛点：**连接重连处理**、**分布式配方（Recipes）**、**异步操作**。核心三层：`curator-client`(底层 ZK 连接管理) → `curator-framework`(CuratorFramework 高级 API) → `curator-recipes`(分布式锁/选主/队列/计数器等配方)。

**核心模块**：

| 模块 | 文件数 | 职责 | 状态 |
|---|---|---|---|
| `curator-client/` | 43 | 底层：ZooKeeper 连接、重试策略、Watcher 管理 | ✅ |
| `curator-framework/` | 185 | 核心：CuratorFramework、ConnectionState、Namespace、Schema | ✅ |
| `curator-recipes/` | 107 | 配方：LeaderLatch/LeaderSelector/SharedLock/Queue/Barrier 等 | ✅ |
| `curator-x-async/` `curator-x-discovery/` | 116/32 | 异步DSL/服务发现 | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| C-1 | **CuratorFramework + 连接管理** | CuratorFramework, CuratorFrameworkFactory, ConnectionState, RetryPolicy | **Builder 模式**：`CuratorFrameworkFactory.builder().connectString().retryPolicy(ExponentialBackoffRetry).build().start()` → 后台线程自动重连——`ConnectionStateManager` 管理 `CONNECTED/RECONNECTED/LOST/SUSPENDED/READ_ONLY` 状态转换；**RetryPolicy**：`ExponentialBackoffRetry`(指数退避)、`RetryNTimes`、`RetryOneTime`、`BoundedExponentialBackoffRetry`、`RetryForever`——5 种策略；**Namespace**：隔离——`usingNamespace("myapp")` → 所有路径自动加 `/myapp` 前缀 |
| C-2 | **Leader 选举** | LeaderLatch, LeaderSelector, LeaderLatchListener | **LeaderLatch**：分布式锁风格——`start()` 创建临时顺序节点→最小序号成为 Leader→`await()` 阻塞等待→`notLeader`/`isLeader` 回调；**LeaderSelector**：轮转风格——`takeLeadership(client)` 抢到 Leader 执行业务→释放后重新选举→适合"谁拿到谁干活"的场景；**区别**：Latch 一次选举→直到主动 close；Selector 每次释放后重新选举 |
| C-3 | **分布式锁** | InterProcessMutex, InterProcessReadWriteLock, InterProcessSemaphoreV2 | **InterProcessMutex**（可重入互斥锁）：`acquire()` → 创建临时顺序节点 `/locks/lock-/0000000001` → 检查是否最小序号→是最小获得锁→不是则 Watcher 监听前一个节点→前一个释放时唤醒→`release()` 删除节点；**InterProcessReadWriteLock**：读写锁——读锁并发、写锁互斥——通过 `__READ__`/`__WRIT__` 前缀区分；**InterProcessSemaphoreV2**：信号量——`setMaxLeases(n)` 控制并发数 |

### 🟡 扩展域（2 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| C-4 | **分布式队列与屏障** | DistributedQueue, DistributedPriorityQueue, DistributedBarrier, DistributedDoubleBarrier | `DistributedQueue`：ZK 顺序节点实现 FIFO 队列；`DistributedBarrier`：设置 barrier 节点→所有参与者 await 阻塞→`removeBarrier()` 唤醒；`DistributedDoubleBarrier`：`enter()` N 个参与者到齐→`leave()` N 个参与者都离开才继续 |
| C-5 | **Curator 事件监听** | TreeCache, PathChildrenCache, NodeCache | **TreeCache**：递归监听整个子树——`start()` → Watch 所有变化(child add/update/remove)→`TreeCacheListener` 回调；**PathChildrenCache**：监听一级子节点变化；**NodeCache**：监听单节点变化——都有 `start()` 初始化同步 |

---

## 三、淘汰清单

| 模块 | 理由 |
|---|---|
| `curator-x-async/` (116) | 异步DSL——高级用法 |
| `curator-x-discovery/` (32) | ZK服务发现——用 Nacos 替代 |
| `curator-examples/` `curator-test/` | 示例/测试 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 3 |
| 🟡 扩展域 | 2 |
| **总域** | **5** |

---

## 五、学习顺序建议

```
C-1 CuratorFramework 连接管理（理解 Builder+重连+Namespace）
  → C-2 Leader 选举（理解 Latch vs Selector 两种模式）
    → C-3 分布式锁（理解临时顺序节点+Watcher 前节点）
      → C-4/C-5 按需
```

以上规划完成，共 **3🔴+2🟡=5 域**。
