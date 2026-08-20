# HANDOFF — Curator 源码分析交接文档 (8/8 域收官)

> **日期**: 2026-08-15 | **版本**: Apache Curator 5.8.0 (pom.xml:34 实证)
> **给新 AI**: 本文是 Curator 阶段的**唯一入口**。所有域已交付 (KP + 大纲 + 提问 + 深审 + 时空溯源 + harness)。
> **源码**: `/data/workspace/source-code/code/spring/curator` | **规划**: CURATOR-PLAN.md (09 审计 v1)

---

## §零 状态速查

| 域 | 级别 | 方案 | 大纲锚点数 | harness | 状态 |
|:--:|:--:|:--:|:--:|:--:|:--:|
| C-1 CuratorFramework 核心+客户端层 | 🔴 | A | 12 节/12 KP | **28/28** | ✅ |
| C-2 Leader 选举 | 🔴 | A | 3 节/3 KP | **15/15** | ✅ |
| C-3 分布式锁 | 🔴 | A | 8 节/8 KP | **22/22** | ✅ |
| C-4 队列与屏障 | 🟡 | B | 7 节/7 KP | **18/18** | ✅ |
| C-5 缓存与监听 | 🔴 | A | 5 节/5 KP | **19/19** | ✅ |
| C-6 共享状态与原子量 | 🟡 | B | 5 节/5 KP | **25/25** | ✅ |
| C-7 持久节点与组成员 | 🟡 | B | 3 节/3 KP | **12/12** | ✅ |
| C-8 服务发现 | 🟡 | B | 5 节/5 KP + 23 问 | **8/8** | ✅ |
| **合计** | 4🔴+4🟡 | | 48 节 | **147/147** | **8/8 收官** |

**环境**: Java 21 + Maven; harness 用 curator-test 5.9.0 TestingServer 内嵌真 ZK 3.9.5 (全部本地 m2 依赖, 离线可跑)。

---

## §一 09 怀疑审计结论 (详细见 CURATOR-PLAN.md)

- **域清单 5 → 8** (+C-6 shared/atomic 版本 CAS 族 +C-7 nodes 跨会话保活 +C-8 x-discovery 服务发现)
- **x-async (116 文件) 排除**: typed/ 33 文件全为 50-90 行样板, 同步 API 机械镜像, 无框架级新决策
- **RetryPolicy "5 种" → 6 种** (Exponential/RetryForever/RetryNTimes/RetryOneTime/RetryUntilElapsed/BoundedExponential)
- **PathChildrenCache "STRONG/ENDPOINT" 是 1.x 历史枚举** — 5.8.0 仅 3 模式 (NORMAL/BUILD_INITIAL_CACHE/POST_INITIALIZED_EVENT)
- **旧三件套 (NodeCache/PathChildrenCache/TreeCache) 全部 @Deprecated** — 新 API CuratorCache (ZK 3.6+ 持久 watcher + cversion 差分)
- Curator 5.8.0 依赖 ZK 3.9.x; 阶段4.3 ZK 9/9 收官 → 拓扑满足

---

## §二 拓扑与依赖

**C-1 → C-3 → C-2 → C-4 → C-5 → C-6 → C-7 → C-8**

- C-2 依赖 C-3: LeaderSelector **复用 InterProcessMutex** (LeaderSelector.java:60-64)
- C-6 依赖 C-3: PromotedToLock 锁升级 (DistributedAtomicValue.java:221-250)
- C-7/C-8 依赖 C-5: GroupMember/ServiceCache 用 CuratorCache (GroupMember.java:68, ServiceProviderImpl.java:77)
- C-4 依赖 C-2: QueueSharder 用 LeaderLatch 唯一扩容 (QueueSharder.java:119)

---

## §三 关键机制速查 (每域核心)

### C-1 框架核心
| 机制 | 锚点 |
|---|---|
| Builder 23 字段/默认值 (session 60000/connection 15000) | CuratorFrameworkFactory.java:155-179, 60-63 |
| start() 五步顺序 (状态分发器先于连接) | CuratorFrameworkImpl.java:284-329 (L291 注释) |
| 后台 DelayQueue + 断连睡 1s 重排 + 重连唤醒 | CuratorFrameworkImpl.java:786-863 (CURATOR-52) |
| 会话过期注入 (percent/Testable) | ConnectionStateManager.java:281-313 (CURATOR-405/525/561) |
| 6 种重试策略/公式 baseSleep×random(1<<(n+1)) | ExponentialBackoffRetry.java:66-73 |
| namespace 懒创建/物理-逻辑路径 | NamespaceImpl.java:54-88 |
| 保护模式 _c_+GUID+_ | ProtectedUtils.java:53-101 |
| 事务 multi 原子 | CuratorMultiTransactionImpl.java:109-115 |

### C-3 分布式锁 (算法核心)
| 机制 | 锚点 |
|---|---|
| 可重入本地计数 (threadData lockCount) | InterProcessMutex.java:197-220, 132-144 |
| 顺序节点 + ourIndex<maxLeases 判定 | StandardLockInternalsDriver.java:33-42 |
| 前驱 watch (getData 非 exists 防泄漏) + wait/notify | LockInternals.java:223-271 (L242-243) |
| 读写锁等长前缀 __READ__/__WRIT__ | InterProcessReadWriteLock.java:60-62, 134-159 |
| 读→写升级不可能 (位置判定) | Javadoc L43-54 |
| 信号量 lease 租约 + 动态上限 | InterProcessSemaphoreV2.java:113-133, 287-290 |
| MultiLock 逆序回滚 | InterProcessMultiLock.java:104-112 |

### C-2 选举
| 机制 | 锚点 |
|---|---|
| latch: EPHEMERAL_SEQUENTIAL + ephemeralOwner 二次确认 | LeaderLatch.java:516-521, 556-573 |
| latch: watch 前驱 + await wait/notify | LeaderLatch.java:575-601, 307-316 |
| selector: 锁复用 + takeLeadership 任期 | LeaderSelector.java:70-77, 422-467 |
| CancelLeadershipException → cancelElection | LeaderSelector.java:546-556 |

### C-4 队列与屏障
| 机制 | 锚点 |
|---|---|
| PERSISTENT_SEQUENTIAL + runLoop 版本驱动 | DistributedQueue.java:359, 457-490 |
| ChildrenCache 一次性 watcher 重挂 + version | ChildrenCache.java:47-54, 134-145 |
| 先删后消费 / 锁安全 + 事务 requeue | DistributedQueue.java:578-605, 607-651 |
| 优先级/延迟/ID 名字编码 | DistributedPriorityQueue.java:186-192 / DistributedDelayQueue.java:75-83, 212-214 |
| 单屏障/双屏障 (ready 节点) | DistributedBarrier.java:101-125 / DistributedDoubleBarrier.java:260-292 |

### C-5 缓存
| 机制 | 锚点 |
|---|---|
| NodeCache 探活再取数 | NodeCache.java:250-274 |
| PathChildrenCache 双 watcher + 差集 + mzxid | PathChildrenCache.java:96-117, 660-703 |
| TreeCache TreeNode 双链路 + DEAD 级联 | TreeCache.java:227, 301-333 |
| CuratorCache 持久 watcher + cversion 差分 | CuratorCacheImpl.java:82-87, 172-198 |
| 三种 INITIALIZED 实现 | PathChildrenCache:705-744 / TreeCache:456-460 / OutstandingOps:25-49 |

### C-6 原子量
| 机制 | 锚点 |
|---|---|
| SharedValue zxid 单调 + 版本 CAS | SharedValue.java:177-214 |
| 乐观重试 tryOnce (withVersion) | DistributedAtomicValue.java:252-296 |
| PromotedToLock 锁升级 | DistributedAtomicValue.java:221-250 |
| CachedAtomicLong 号段 | CachedAtomicLong.java:48-70 |
| IllegalTrySetVersionException (version=-1) | SharedValue.java:186-188 |

### C-7 持久节点
| 机制 | 锚点 |
|---|---|
| 三触发重建 (watcher/重连/回调 NONODE) | PersistentNode.java:86-113, 130-137 |
| close 防孤儿 guaranteed | PersistentNode.java:222-237, 386-395 |
| CONTAINER+TTL 心跳 (ttlMs/2) | PersistentTtlNode.java:162-201 |
| GroupMember 自己强制入列 | GroupMember.java:116-132 |

### C-8 服务发现
| 机制 | 锚点 |
|---|---|
| 注册 = 临时节点 + reRegister | ServiceDiscoveryImpl.java:141-148, 178-190 |
| ServiceCache 基于 CuratorCache | ServiceProviderImpl.java:77 |
| 过滤链 + 策略 + noteError 熔断 | ServiceProviderImpl.java:79-82, 125-132; DownInstanceManager.java:46-60 |

---

## §四 harness 实证发现 (写作素材, 全部真 ZK 实证)

1. **isConnected() 是事件驱动最终一致** — 杀服务器后标志不立即翻转 (C-1)
2. **公平性 = ZK 创建序**: 5 线程同刻抢锁顺序 [0,3,1,2,4] 非线程 id 序; 错开启动后严格 FIFO (C-3)
3. **选举接替延迟 <3s 全链路事件驱动** (C-2)
4. **先删后消费的速度**: consumeMessage 之前节点已删 (C-4)
5. **启动时 TreeCache/CuratorCache 对根节点发 NODE_ADDED/CREATED**; 快速 create+setData 会吞 CHILD_UPDATED (C-5)
6. **TTL 节点要求 ZK 服务端 `-Dzookeeper.extendedTypesEnabled=true`** — 否则创建失败, 部署前提 (C-7)
7. **DownInstanceManager 阈值语义**: 错误数 ≥ errorThreshold 且须同一实例才排除 (C-8)
8. 会话崩溃释放锁 <8s (ephemeral 回收) (C-3)

---

## §五 遗留与待办

- [ ] 大纲→正式文章写作 (另行启动, 与 Z-8 Recipes 对照着写)
- [x] 深审缺陷档案回填 (C-1~C-8 无新增缺陷类别; 反模式 7/8 已遵守 — 无行号硬压缩, 内容优先)
- [x] **REVIEW 修复 2026-08-15**: ① 锚点 4 处修正 (ConnectionState 双文件歧义→framework/state 全路径 / InterProcessMutex L60→L42 / InterProcessSemaphoreV2 L287-290→L327-332 / acquire L233→L241) ② 锚点密度补强 (C-2 25/C-3 19/C-4 32/C-5 33/C-7 10/C-8 12, 全达 🔴≥8/🟡≥4) ③ C-8 提问文件补齐 (23 问) ④ 数字自洽 (深审 97→110, 问数 185 穷举复核)
- [ ] 阶段4.6 SofaJRaft (下一仓库, 5 域)
- [ ] Obsidian 知识图谱 (全局待办)

## §六 文件路径

```
analysis/source-analysis/curator/
├── CURATOR-PLAN.md (09 审计表 + 拓扑)
├── knowledge-planning/ (c1~c8 共 8 个 KP)
├── outlines/
│   ├── c1-curatorframework/ (outline 12 节 + completeness 25 问 + review-notes 15 项 + temporal-trace)
│   ├── c2-leader/ (3 节 + 23 问 + 14 项)
│   ├── c3-locks/ (8 节 + 25 问 + 17 项 + temporal-trace)
│   ├── c4-queue/ (7 节 + 25 问 + 14 项)
│   ├── c5-cache/ (5 节 + 24 问 + 17 项)
│   ├── c6-atomic/ (5 节 + 22 问 + 11 项)
│   ├── c7-nodes/ (3 节 + 18 问 + 11 项)
│   └── c8-discovery/ (5 节 + 23 问 + 11 项)
└── harness/ (c1~c8, 每域 HarnessCx.java + run 脚本, 真 ZK 内嵌, 147/147 断言)
```

**运行**: `cd harness/cN-xxx && ./runCN.sh` (需 /data/.m2 本地仓库; C-7 需 extendedTypesEnabled 已内置)。

## §七 完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (5→8 域 +2 排除)
- [x] 09 审计 4 修正全部落 PLAN
- [x] 8/8 域交付: KP + 大纲 + completeness-questions (8/8 全齐, 共 185 问) + 六层深审 (110 项核对) + 时空溯源 (C-1/C-3)
- [x] harness 8 个全部真 ZK 实证 **147/147 全 PASS** (REVIEW 后重跑确认)
- [x] 每域负面空间 3-6 条
- [ ] 大纲文章写作 (下一阶段)
