# Z-3 DataTree — 内存状态树与事务应用

> 前置: [[Z-2-原子广播]] (COMMIT 落点) + [[Z-6-Watcher]] (触发面交叉) | 引出: [[Z-4-Processor链]] (写路径入口) | 对照: Redis 数据结构 + ES 状态树 + RM-3 (RocketMQ 索引)
> 🔴 A | 8 KP | [模式: 树结构 + 事务应用 + 快照序列化]
> Pass 2 闭环: q1(树结构) q2(写操作) q3(事务应用) q4(快照面)

**读者处境**: 数据存在哪? 创建/删除怎么校验? 快照怎么序列化? 这篇拆 DataTree: NodeHashMap + DataNode + 父节点锁 + 事务分发 + digest 校验 + 序列化。

### 1. 树结构 — NodeHashMap + DataNode + 分类集合

场景: znode 树怎么组织?
源码路径:
- **NodeHashMap** (DataTree:105): **HashMap 扁平路径 → 节点** (非真实树遍历 — "tree is the source of truth" 注释 L102-104); **NodeHashMapImpl + digestCalculator** (preChange/postChange 计算 **tree digest** — 3.5+ 校验机制, DIGEST_LOG_LIMIT=1024/DIGEST_LOG_INTERVAL=128, L169-173; **digest 历史**: digestLog + DigestWatcher + 快照加载校验 L177-188); **根节点双 key**: `nodes.put("", root)` + `putWithoutDigest("/", root)` (L288-289 — 写路径 parentName="" 查根, 序列化 "/")
- **DataNode** (DataNode.java:40-88): data (volatile) + **StatPersisted stat** (czxid/mzxid/pzxid/cversion/version/ephemeralOwner/dataLength) + children (Set) + acl (引用计数 id)
- **分类集合**: **ephemerals (sessionId → paths, ConcurrentHashMap)** / containers / ttls — 会话关闭清扫面 (Z-5 交叉); **ReferenceCountedACLCache** (ACL 引用计数); **单线程写模型** (五次 REVIEW): Final 顺序应用 (killSession 注释 L1122-1127) — 分类集合写免锁, 读并发 (CHM)
- **系统节点**: /zookeeper (proc/quota/config 子树) + PathTrie (quota 路径前缀索引)
- **统计**: nodeDataSize (AtomicLong 缓存) + approximateDataSize (逐节点同步遍历)
关键设计 (q1): **扁平 HashMap + 父节点 synchronized 锁** (写路径锁父节点); digest 双钩子 (pre/postChange) 增量校验。[模式: 树存储]

### 2. 写操作 — createNode/deleteNode/setData 校验链

场景: 创建/删除/改数据怎么校验?
源码路径:
- **createNode** (DataTree:433-522): 路径拆分 → **synchronized(parent) 父节点锁** → **ACL 先入缓存** (fuzzy snapshot race 注释 L446-457) → 已存在 NodeExistsException → **preChange/postChange (digest)** → **parent.cversion/pzxid 递增** (replay 保护: 仅当 parentCVersion > 现有, L470-478) → parent.addChild + nodes.put → **nodeDataSize.addAndGet** → **ephemeral 分类** (CONTAINER/TTL/ephemerals) → **quota 两段式: 检查在 Prep (checkQuota 超限拒绝) / 树层只计数 (updateQuotaStat L375)** → **双 watch 触发** (dataWatches NodeCreated + childWatches NodeChildrenChanged)
- **deleteNode** (L533-625): parent.removeChild + **pzxid 保护 (zxid > pzxid 才更新 — 防 CreateTxn 覆盖, L548-553)** → nodes.remove + **aclCache.removeUsage** → 分类清理 → quota 递减 → **三 watch 触发** (NodeDeleted ×2 + NodeChildrenChanged); **读路径挂 watch** (五次 REVIEW): getChildren 在 synchronized(n) 内 childWatches.addWatch (L722-744)
- **setData** (L627+): version 校验 (乐观锁 — 版本不符 BadVersionException) → stat 更新 (mzxid/mtime/version++) → watch 触发
- **setACL** (L757+): 同 version 校验 + ACL 缓存引用管理
关键设计 (q2): **写 = 父锁 + digest + 分类维护 + watch 触发四步**; version 乐观锁; cversion/pzxid 单调保护。[模式: 校验链]

### 3. 事务应用 — processTxn 分发

场景: 广播提交的事务怎么落树?
源码路径:
- **processTxn 三层** (L845-855): 带 digest / 不带 / isSubTxn
- **OpCode 分发** (L865-960): create/create2/createTTL/createContainer → createNode; delete/deleteContainer → deleteNode; reconfig/setData → setData; setACL; **multi → 子事务循环 (isSubTxn=true, L1028) — 原子性靠 PrepRequestProcessor 预校验 (Z-4 交叉, 树层无回滚)**; ErrorTxn → 忽略
- **失败语义**: processTxn 抛异常 → 上层处理; 部分失败 (multi 原子性 — Z-4 交叉)
- **digest 校验**: TxnDigest 参数 (3.5+ 校验) — 快照/日志重放的一致性验证
关键设计 (q3): **事务 = 应用层分发**: 同一树 API 被 commit 回放与客户端写共享 (幂等性由 zxid 单调保证)。[模式: 事务分发]

### 4. 快照面 — serialize/deserialize + ZKDatabase

场景: 快照怎么读写?
源码路径:
- **serialize** (L1331+): **serializeAcls + serializeNodes (DFS)**, 路径 + 节点记录, **"/" 结束标记** (L1336-1339)
- **deserialize** (L1350-1388): ACL 缓存恢复 → nodes.clear + pTrie.clear → 逐节点: nodes.put + **parent.addChild 父链重建** (parent 缺失 → IOException) + **分类恢复** (containers/ttls/ephemerals) + aclCache.addUsage
- **ZKDatabase** (806): dataTree + **committedLog (ArrayDeque 最近提案缓存 — Z-2 DIFF 数据源)** + **snapLog (FileTxnSnapLog — Z-9)**; loadDataBase → **snapLog.restore** (快照+事务恢复)
关键设计 (q4): **快照 = 树序列化 + 父链/分类重建**; committedLog 桥接广播与存储。[模式: 序列化]

### 负面空间 — DataTree 刻意不做的事

- **不做真实树指针结构**: 扁平 HashMap (路径 key) — 无父子指针遍历, 靠字符串路径
- **不做节点级锁**: 写锁在父节点 (synchronized(parent)) — 兄弟并发创建串行
- **不做路径压缩**: 全路径字符串 (对照 ES 的 trie 压缩面 — PathTrie 仅 quota 用)
- **不做数据压缩**: znode data 原样存 (对照 Redis 压缩列表)
- **不做子节点排序索引**: children 是 HashSet (无序)
- **不做大节点拆分**: 单 znode 数据上限 1MB (ServerCnxn maxBuffer)

→ 引出: 写请求怎么进树? → [[Z-4-Processor链]]
