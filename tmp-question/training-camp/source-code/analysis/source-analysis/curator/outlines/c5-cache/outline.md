# C-5 缓存与监听 — 三代 watcher 演进: 从一节点一监听器到 O(1) 持久 watch

> 前置: [[C-1-CuratorFramework]] (watcher/后台操作) + [[Z-6-Watcher]] (一次性 watcher) + [[Z-7-ClientAPI]] (ZK 3.6 持久 watcher) | 引出: [[C-7-持久节点与组成员]] (GroupMember 用 CuratorCache) + [[C-8-服务发现]] (ServiceCache) | 对照: Redisson RLocalCachedMap + ZooKeeper 官方 WatchManager
> 🔴 A | 5 KP | [模式: watcher 重挂 + 版本判定 + 哨兵]
> Pass 2 闭环: q1(NodeCache) q2(PathChildrenCache) q3(TreeCache) q4(CuratorCache)

**读者处境**: 服务注册中心要缓存 10 万节点的状态, 每个节点一个 watcher 会怎样? 断连重连后缓存怎么重新同步? 为什么 Curator 官方把三件套标记废弃?

### 1. NodeCache — 一个 watcher 搞定单节点

场景: 只需监听"配置节点"的变化
源码路径:
- 单 watcher 收任何事件 → reset() (NodeCache.java:86-96); 仅 STARTED+已连接执行 (NodeCache.java:226-234)
- **processBackgroundResult 状态机** (NodeCache.java:250-274): GET_DATA+OK → setNewData; EXISTS+NONODE → setNewData(null); EXISTS+OK → 转 getData (先探活再取数)
- setNewData: **Objects.equal 全量比较** (path+stat+data) 不等才通知 (NodeCache.java:280-300); 监听器同步调用
- isConnected 门控: 断连期间丢弃事件, 重连后主动 reset (L68-84)
关键设计 (q1): **单节点无独立事件线程** — 回调在 ZK 事件线程同步执行, 顺序天然一致; "变了"是最粗粒度信号, 无事件类型。 [模式: 探活再取数]

### 2. PathChildrenCache — 双 watcher 分工 + 差集刷新

场景: 监听"某服务下所有实例"的增删
源码路径:
- **childrenWatcher → RefreshOperation 整体重拉** (PathChildrenCache.java:96-101); **dataWatcher → NodeDeleted remove / NodeDataChanged 定点 getData** (PathChildrenCache.java:103-117) — 列表变化重拉, 数据变化定点
- StartMode 三模式 (PathChildrenCache.java:282-302): NORMAL / **BUILD_INITIAL_CACHE (前台阻塞构建)** / POST_INITIALIZED_EVENT
- **operationsQuantizer 去重** (PathChildrenCache.java:82, 746-768): Set<Operation> equals/hashCode — 高频事件合并
- processChildren 差集 (PathChildrenCache.java:660-681): removed = 旧 - 新; 新节点才 getData
- **applyNewData mzxid 判定** (PathChildrenCache.java:682-703): 无 previous → CHILD_ADDED; mzxid 不同 → CHILD_UPDATED
- **INITIALIZED**: initialSet + NULL_CHILD_DATA 哨兵 (PathChildrenCache.java:92), == 引用比较判未就绪 (L732-744), getAndSet(null) 只发一次 (L713-730)
- 重连 FORCE_GET_DATA_AND_STAT 全量重拉 (PathChildrenCache.java:630-658)
关键设计 (q2): **事件合并防风暴** — ZK 一次性 watcher 每变更必触发, 高频场景队列会爆炸; 差集只对增量 getData, 减少往返; mzxid 是"变化证据" (Z-3 stat 交叉)。 [模式: 双 watcher + 差集]

### 3. TreeCache — 一节点一双链路 + DEAD 哨兵

场景: 缓存整棵配置树 (上千节点)
源码路径:
- **TreeNode implements Watcher, BackgroundCallback** (TreeCache.java:227) — 同一节点既收事件又收数据结果, 天然配对无空窗
- refresh 受 maxDepth 与 selector 约束 (TreeCache.java:240-248); maybeWatch 双模式 (TreeCache.java:278-285)
- **mzxid 乱序保护** (TreeCache.java:424-429): 旧更新不覆盖新数据; 非根 dead→live 禁止 (L430-434)
- **DEAD 哨兵 + 级联删除** (TreeCache.java:301-333): 删除即整棵子树出缓存
- INITIALIZED = outstandingOps 计数归零 (TreeCache.java:456-460); LOST 重置 / RECONNECTED 全树刷新 (TreeCache.java:766-788)
- @Deprecated → CuratorCache (TreeCache.java:75-77)
关键设计 (q3): **事件与结果一条链路** — 避免 getChildren 后再 getData 的空窗; mzxid 单调性对抗后台响应乱序; DEAD 防"迟到响应复活已删节点"。代价: 每节点一个 watcher — 大树下 watcher 数量 = 节点数。 [模式: 双链路 + 哨兵]

### 4. CuratorCache — 持久 watcher + cversion 差分 (O(1) watcher)

场景: 10 万节点树, 还按节点挂 watcher 吗?
源码路径:
- **PERSISTENT_RECURSIVE 持久 watcher** (CuratorCacheImpl.java:82-87; PersistentWatcher.java:147-152) — 一次注册自动续期, watcher 数量 O(1)
- **事件语义差异** (CuratorCacheImpl.java:151-170): 持久 watcher **不产生 NodeChildrenChanged** → 靠 **cversion 差分**: stat.cversion 变化 → getChildren + 逐子 nodeChanged (CuratorCacheImpl.java:172-198)
- **version 判定** (CuratorCacheImpl.java:231-245): storage.put 返回 previous, version 不同 → NODE_CHANGED; 无 previous → NODE_CREATED (用 version 而非 mzxid)
- Options: SINGLE_NODE_CACHE 替代 NodeCache / COMPRESSED_DATA / DO_NOT_CLEAR_ON_CLOSE (CuratorCache.java:54-71)
- 无独立事件线程: client.runSafe (CuratorCacheImpl.java:247-251); INITIALIZED 用 OutstandingOps (CuratorCacheImpl.java:63-64, OutstandingOps.java:25-49)
- **bridge**: ZK<3.6 自动回退 TreeCache (CompatibleCuratorCacheBridge.java:38-60; CuratorCacheBridgeBuilderImpl.java:58-68)
关键设计 (q4): **持久 watcher 是 ZK 3.6 的协议级礼物** — 服务器端维护递归订阅, 客户端不再重挂; cversion 差分替代 children 事件; 事件判定从 mzxid 换成 version — 3 个旧类各自实现的一致性逻辑收敛为一个。 [模式: 持久订阅 + stat 差分]

### 5. 一致性哲学与事件映射 — 尽力而为的快照

场景: 缓存永远同步吗?
源码路径:
- 四个类共享免责声明 "not possible to stay transactionally in sync" (TreeCache.java:71-73 等) — 缓存是尽力而为快照
- 事件映射: NODE_ADDED→NODE_CREATED / NODE_UPDATED→NODE_CHANGED / NODE_REMOVED→NODE_DELETED (CompatibleCuratorCacheBridge.java:105-127)
- CuratorCacheListener 无连接状态事件 — 连接状态由框架层处理 (对照旧三件套 7 类事件含 CONNECTION_*)
关键设计 (q4): **分区期间的洞** — 断连时事件丢失, 重连靠全量重建补; 所以"写缓存数据必须带版本号"是官方建议 (CuratorCache Javadoc)。 [模式: 尽力而为一致性]

## 代码类型
Architecture (缓存/观察)

## 负面空间 — Curator 缓存刻意不做的事

- **不做强一致性**: 缓存是本地快照, 官方明示事务性同步不可能
- **不做事件重放**: 分区期间事件可能漏, 无补偿重放
- **不做失效广播**: 多客户端本地缓存无相互通知 (对照 Redisson RLocalCachedMap 的 invalidation topic)
- **不做持久化**: 内存缓存, 重启即失
- **不做查询引擎**: 仅路径查找/前缀遍历, 无表达式过滤
- **不做服务端过滤**: selector 是客户端逻辑, 不节省服务端事件流量

→ 引出: 共享状态配方怎么在缓存之上做 CAS? → C-6 共享状态与原子量
