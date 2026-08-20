# Z-6 Watcher — 双向注册与一次性触发

> 前置: [[Z-3-DataTree]] (触发面交叉) + [[Z-7-ClientAPI]] (投递面交叉) | 引出: [[Z-7-ClientAPI]] | 对照: Redis 发布订阅 + ES watcher
> 🔴 A | 8 KP | [模式: 双向索引 + 一次性触发 + 双实现]
> Pass 2 闭环: q1(双实现) q2(双向注册) q3(触发链) q4(WatcherMode)

**读者处境**: watch 存哪? 触发一次后还有吗? 会话关闭怎么清理? 这篇拆 WatchManager 双实现: 标准 (HashMap 双向) vs Optimized (位图压缩) + 一次性触发 + WatcherMode 四模式。

### 1. 双实现 — WatchManager vs WatchManagerOptimized

场景: watch 管理器两种实现差在哪?
源码路径:
- **WatchManager** (376): **watchTable (path → Set<Watcher>)** + **watch2Paths (watcher → Map<path, WatchStats>)** (L50-52) — synchronized 全方法锁
- **WatchManagerOptimized** (412): **pathWatches (ConcurrentHashMap<String, BitHashSet>)** + **watcherBitIdMap (BitMap<Watcher> 位图)** (L61-64) — **watcher 位图压缩** (每个 watcher 一个 bit id); **RWLock** (addWatch readLock / removeWatcher writeLock — 防 dead watch 竞态注释 L79-80); **IDeadWatcherListener + deadWatchers 懒清理** (triggerWatch 时删 — 注释 L200-201 避免锁竞争); **⚠ 无 sessionWatches 分桶** (五次 REVIEW 修正执行计划断言 — 会话清理靠 deadWatchers 懒清理, 非 session 分桶)
- **工厂选择**: WatchManagerFactory (watchManagerClassName / isWatchManagerOptimized 配置) — DataTree 构造 (Z-3: dataWatches/childWatches)
关键设计 (q1): **位图压缩 = 优化核心** (watcher 集合位表示 + contains O(1)); 懒清理换锁竞争。[模式: 双实现]

### 2. 双向注册 — watchTable + watch2Paths

场景: watch 怎么登记?
源码路径:
- **addWatch** (WatchManager:75-110): 死 watcher 忽略 → watchTable HashSet(**4 初始, 4th 翻倍** — 注释 L83-85) → watch2Paths (WatchStats 模式合并) + recursiveWatchQty 计数
- **removeWatcher(watcher)** (L113-132): watch2Paths 移除 → **watchTable 反向清理** (空路径删除) — 双向一致性
- **removeWatcher(path, watcher)** (Optimized:130): 单路径移除 (writeLock)
- **isDeadWatcher**: cnxn 已关闭的 watcher 忽略 (L76-78) — 防死连接污染
关键设计 (q2): **双向索引**: path→watchers (触发查询) + watcher→paths (清理查询); 死 watcher 守卫。[模式: 双向索引]

### 3. 触发链 — PathParentIterator + 一次性移除

场景: 数据变化怎么触发?
源码路径:
- **triggerWatch** (WatchManager:140-199): **PathParentIterator 双模式** (五次 REVIEW): **forAll (递归父路径) / forPathOnly (仅路径 — 配置开关** L370-374) → 逐 watcher:
  - **STANDARD: 触发即移除** (L161-167: removeMode + iterator.remove)
  - **PERSISTENT_RECURSIVE: 父路径保持** (L168-170)
  - watchers 集合去重 (同 watcher 多路径只触发一次)
- **WatcherOrBitSet suppress** (L185-187): 已处理 watcher 跳过 (Z-3 deleteNode 双触发用)
- **分发**: ServerWatcher.process(e, acl) / Watcher.process(e) (L188-192) → **NIOServerCnxn.process → checkACL (READ 权限过滤 — NoAuth 丢弃!) → NOTIFICATION_XID + sendResponse ("notification")** (Z-7, L710-736)
- **metrics**: NODE_CREATED_WATCHER 等计数 (L195-199)
- **DataTree 触发点** (Z-3): createNode 双 watch + deleteNode 三 watch (L519-521,621-624)
关键设计 (q3): **一次性语义 = 触发即移除**; 递归模式父路径保持; suppress 防重复。[模式: 触发移除]

### 4. WatcherMode — 四模式 + 会话清理

场景: watch 有几种模式?
源码路径:
- **WatcherMode 枚举** (WatcherMode.java:23-26): **STANDARD (一次性) / PERSISTENT (持久) / PERSISTENT_RECURSIVE (持久递归)** + DEFAULT=STANDARD
- **WatchStats**: 路径级模式状态 (addMode/removeMode 合并)
- **会话关闭清理**: 标准 removeWatcher(watcher) 全清; **Optimized 懒清理** (deadWatchers → triggerWatch 时批量删) — 会话/连接关闭高效清理 (注释 L47-48)
- **WatchManagerFactory**: 工厂创建 (watchManagerClassName 可插拔)
关键设计 (q4): **模式 = 触发语义参数化**; 会话关闭清理双路径 (立即全清 vs 懒批量)。[模式: 触发语义]

### 负面空间 — Watcher 刻意不做的事

- **不做持久事件历史**: watch 只存注册状态, 事件不排队 (错过即丢 — 客户端需重查)
- **不做超时**: watch 无 TTL (会话过期才清)
- **不做跨节点同步**: watch 状态在本地 (leader 触发广播给所有 replica — 通过会话)
- **不做 watch 级 ACL 细分**: 权限在触发时传 (ServerWatcher.process(e, acl))
- **不做递归默认**: PERSISTENT_RECURSIVE 需显式 (addWatch mode)
- **不做事件排序保证**: 触发顺序依赖处理器链顺序

→ 引出: 事件怎么到客户端? → [[Z-7-ClientAPI]]
