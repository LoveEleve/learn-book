# C-5 缓存与监听 — 知识规划 (KP)

> 域级: 🔴 A | 模块: curator-recipes/.../recipes/cache/ (36 文件) + recipes/watch/PersistentWatcher (159): TreeCache 816 / PathChildrenCache 788 / NodeCache 310 / CuratorCacheImpl 261 / CuratorCache 146 / CuratorCacheListener 74 / StandardCuratorCacheStorage 66 / ChildData 121 / TreeCacheEvent 153 / PathChildrenCacheEvent 133 / OutstandingOps 49 / PersistentWatcher 159 / CompatibleCuratorCacheBridge 128 / CuratorCacheBridgeBuilderImpl 69
> 日期: 2026-08-15 | 版本: 5.8.0

## 一、机制提取 (逐源)

### M1 NodeCache: 单节点缓存 (310)
- 单 watcher: 任何事件 → reset() (L86-96); 仅 STARTED+已连接执行 (L226-234)
- **processBackgroundResult 状态机** (L250-274): GET_DATA+OK → setNewData; EXISTS+NONODE → setNewData(null); EXISTS+OK → 转 getData (带 watcher)
- setNewData: CAS 换 data, **Objects.equal 不等才通知** (L280-300); 监听器**同步调用** (无独立事件线程)
- isConnected 门控: 断连丢弃 watcher 回调, 重连 compareAndSet(false,true) 才 reset (L68-84)
- start(buildInitial)/rebuild (L150-201)

### M2 PathChildrenCache: 单层子节点缓存 (788)
- **双 watcher**: childrenWatcher → RefreshOperation(STANDARD) 重拉 (L96-101); dataWatcher → NodeDeleted→remove / NodeDataChanged→GetDataOperation (L103-117)
- **StartMode 3 模式** (L282-302): NORMAL (后台刷新) / BUILD_INITIAL_CACHE (前台 rebuild 阻塞) / POST_INITIALIZED_EVENT (初始就绪后发 INITIALIZED)
- **operationsQuantizer** (L82): Set<Operation> equals/hashCode 去重; offerOperation add 成功才提交 (L746-768)
- refresh: getChildren().usingWatcher(childrenWatcher).inBackground (L520-523); NONODE → ensureContainers.reset + NO_NODE_EXCEPTION 模式重试 (L503-515)
- **processChildren 差集** (L660-681): removed = currentData - 新children → remove; 新节点 (FORCE 或不在缓存) → getDataAndStat
- **applyNewData mzxid 判定** (L682-703): 无 previous → CHILD_ADDED; mzxid 不同 → CHILD_UPDATED; NONODE → remove
- **INITIALIZED 机制** (L705-744): initialSet + NULL_CHILD_DATA 哨兵 (L92); hasUninitialized 用 == 引用比较 (L732-744); getAndSet(null) 保证只发一次 (L713-730)
- 重连: FORCE_GET_DATA_AND_STAT 全量刷新 (L630-658, L673)
- getCurrentData 按路径排序 TreeSet (L420-422); clearDataBytes 内存优化 (L442-465)

### M3 TreeCache: 全树缓存 (816)
- **TreeNode implements Watcher, BackgroundCallback** (L227) — 一节点双链路; AtomicReferenceFieldUpdater CAS 更新 (L220-225)
- refresh: depth < maxDepth && selector.traverseChildren → data+children 双刷新 (L240-248)
- **mzxid 乱序保护** (L424-429): 新 mzxid ≤ 旧 → 丢弃; 非根 dead→live 禁止 (L430-434)
- **DEAD 哨兵 + 级联删除** (L301-333): wasDeleted 递归子节点; 根节点转 checkExists 监视复活
- **INITIALIZED = outstandingOps 归零** (L456-460); LOST → isInitialized=false (L766-769); RECONNECTED → 全树 wasReconnected 刷新 (L780-788)
- maybeWatch: disableZkWatches → 只 inBackground (L278-285)
- @Deprecated → CuratorCache 替代 (L75-77)

### M4 CuratorCache: 新一代统一缓存 (261+146)
- **基于 ZK 3.6+ PERSISTENT_RECURSIVE 持久 watcher** (CuratorCacheImpl.java:82-87; PersistentWatcher.java:147-152 AddWatchMode) — watcher 数量 O(1)
- Options: SINGLE_NODE_CACHE / COMPRESSED_DATA / DO_NOT_CLEAR_ON_CLOSE (CuratorCache.java:54-71)
- **事件语义** (L151-170): 持久 watcher 无 NodeChildrenChanged → **cversion 差分** (L172-198): stat.cversion 不同 → getChildren + 逐子 nodeChanged
- **version 判定** (L231-245): storage.put 返回 previous → version 不同 → NODE_CHANGED; 无 previous → NODE_CREATED; 删除 → NODE_DELETED (用 version 而非 mzxid)
- INITIALIZED: OutstandingOps 计数 (L63-64, L25-49)
- 无独立事件线程: callListeners 经 client.runSafe (L247-251)
- **bridge 兼容层**: CompatibleCuratorCacheBridge 内嵌 TreeCache; ZK<3.6 回退 (L38-60, 105-127)
- CuratorCacheListener: 3 事件 + initialized (L32-62); 无连接状态事件 (对照旧三件套 7 类)

### M5 支持面
- ChildData: path/stat/data 不可变; compareTo 仅 path (L42-52); equals 全量 (L56-85)
- StandardCuratorCacheStorage: ConcurrentHashMap + cacheBytes 开关 (L27-66)
- CuratorCacheListenerBuilderImpl: forCreates/forChanges/.../afterInitialized 门控 (L38-117)
- PathChildrenCacheMode/NodeCacheListener/TreeCacheListener 等 wrapper 桥接 (CompatibleCuratorCacheBridge L105-127)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M4 CuratorCache 持久 watcher+cversion | P1 | 新一代核心; 代际差异面试点 |
| M2 双 watcher + 差集 + mzxid | P1 | 旧三件套中最常用 |
| M3 TreeNode 双链路 + DEAD | P1 | 全树一致性 |
| M1 NodeCache 简单模型 | P2 | 入门对照 |
| M5 bridge/事件映射 | P2 | 兼容面 |

## 三、负面空间

- **不做强一致**: 四个类共享 "not possible to stay transactionally in sync" 免责 (TreeCache.java:71-73 等) — 写数据必须带版本
- **不做事件重放/去重保证**: 分区期间事件可能漏 (CuratorCache Javadoc L36-48)
- **不做缓存持久化**: 内存缓存, 重启重建
- **不做 SQL 式查询**: 只有 path 查找/find 前缀匹配
- **不做多客户端缓存一致性**: 本地缓存无失效广播 (对照 Redisson 发布订阅缓存)
