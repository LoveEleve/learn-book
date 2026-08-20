# R-21 上 — 键空间操作: 从 lookupKey 到三表一致

> 前置: [[R-20-下]] (键空间创建 server.c:2674-2675) + [[R-3-Dict]] (dict 分片版) + [[R-1-object]] (值载体/引用计数) | 引出: [[R-21-下]] (kvstore/ebuckets/lazyfree 实现) + [[R-22-过期机制]] | 对照: [[R-16-transaction]] (watch 信号, 仅信号语义, 域详述在 R-16)
> 🔴 A | 4 KP | [模式: 查找路径副作用链+三表一致性+键共享零拷贝]
> Pass 2 闭环: q1(lookupKey+惰性过期) q2(增删改三件套) q3(expires 表) q8(命令面+getKeys)

**读者处境**: 一条 GET 命令背后, 键空间查找都做了什么?为什么从 master 复制来的过期键在从库"看得到却删不得"?expires 表为什么几乎不占额外内存?SET 覆盖旧值、DEL 删除、FLUSHDB 清库, 怎么保证 keys/expires/hexpires 三张表永远一致?这篇拆键空间的操作面: 查找、增删改、过期表、命令面。

### 1. lookupKey — 一条查找的完整副作用链

场景: 查一个键, 背后发生什么?
源码路径:
- `db.c:75` (lookupKey): dbFind (L2064: kvstoreDictFind + getKeySlot) → 命中后 **expireIfNeeded 拦截** (L94) → LRU/LFU 更新 (L107-113, LOOKUP_NOTOUCH 跳过; hasActiveChildProcess 时跳过防 CoW) → hits/misses 统计 + keymiss 通知 (L115-123)
- 标志矩阵 (L59-68): NOTOUCH/NONOTIFY/NOSTATS/WRITE/NOEXPIRE — 每个副作用可单独关闭
- 查找三入口: lookupKeyRead (L145) / lookupKeyWrite (L159, 强制删过期) / OrReply 变体 (L163-173, 失败直接回错误)
- **slot 缓存优化** (L217-219): current_client->slot + CLIENT_EXECUTING_COMMAND → 免 CRC16
关键设计 (q1): 查找不是"读字典" — 是**过期判定 + 访问时间 + 统计 + 通知**的副作用链, 标志位逐项可关。[模式: 副作用链]
数据流: 命令 → lookupKey → dbFind → 过期拦截 → 访问记录 → 值返回。

### 2. expireIfNeeded — 惰性过期三态

场景: 过期的键什么时候被删?从库为什么不删?
源码路径:
- `db.c:1974` (expireIfNeeded): **keyIsExpired** (L1928: getExpire + commandTimeSnapshot 虚拟时间 — 脚本内时间一致) → 三态返回
- 三态 (L29-33): **KEY_VALID (未过期) / KEY_EXPIRED (逻辑过期但没删) / KEY_DELETED (删了)**
- 从库语义 (L1991-1994): masterhost 存在 → CLIENT_MASTER 永不过期; 无 FORCE 标志 → 只报 KEY_EXPIRED 不删 (等 master 的 DEL); 从库端主动清理走 expireSlaveKeys (R-22 详述)
- 强制路径: LOOKUP_WRITE + 非只读从库 → FORCE 删除 (L90-91); AVOID_DELETE 只查不删 (L1998-1999); PAUSE_ACTION_EXPIRE 暂停 (L2004)
- 删除本体: deleteExpiredKeyAndPropagate (L1877: 删 + notify "expired" + propagateDeletion L1908 合成 DEL 给 AOF/从库)
- **静态键转换** (L2006-2010): robj 是栈上静态的 (SCAN 迭代) → 先转堆上再删
关键设计 (q1): 惰性删除 = **"读到才删"**, 从库一致性的关键: 过期判定与删除行为分离 (逻辑判定三态 vs 物理删除由角色决定)。[模式: 判定与执行分离]
数据流: 读键 → keyIsExpired → 主库: 删 + 传播 DEL / 从库: 报过期等 master。

### 3. 增删改三件套 — 三表一致性

场景: SET/DEL 怎么保证 keys/expires/hexpires 三张表不打架?
源码路径:
- **dbAddInternal** (db.c:180): kvstoreDictAddRaw(keys) → 键 sdsdup (L189) → initObjectLRUOrLFU → 值入表 → signalKeyAsReady (阻塞键唤醒, L192) + NOTIFY_NEW
- **dbSetValue** (L256): 覆盖 — 旧值 lru 继承 (L262) → overwrite 标志: module unlink 通知 + signalDeletedKeyAsReady (XREADGROUP) → **HFE: 旧值 hash 从 db->hexpires 摘除** (L281-282) → 旧值释放 (lazyfree_lazy_server_del ? 异步 : 同步)
- **dbGenericDelete** (L372): TwoPhaseUnlinkFind (暂停 rehash 两阶段删, R-3) → 通知 → **先删 expires 表** (L401, 键共享所以安全) → UnlinkFree → async ? freeObjAsync
- setKey (L310): 高层统一入口 — 四态路由 (ADD/UPDATE/已存在/不存在) + KEEPTTL + WATCH 通知 (L329)
- dbAddRDBLoad (L235): RDB 加载专用 (键不 dup, 无通知)
关键设计 (q2): 三表一致性 = **每张表独立的 kvstore + 删除顺序约定** (先 expires 后 keys, 键共享 sds 保证安全); hexpires 是嵌入式 (每个 hash 自带的 ExpireMeta 头, 只摘除注册)。[模式: 多表顺序化]
数据流: SET → setKey → dbAdd/dbSetValue → 信号 → expires 处理 (KEEPTTL) → WATCH/TRACKING 通知。

### 4. expires 表 — 零拷贝键 + 整数值

场景: 100 万带 TTL 的键, expires 表额外占多少内存?
源码路径:
- `db.c:1846` (setExpire): **键复用主 dict 的 sds** (L1853: kvstoreDictAddRaw(db->expires, slot, dictGetKey(kde)) — 零拷贝!) → dictSetSignedIntegerVal (整数联合值, dict.c:849 — 免 val 分配)
- dbExpiresDictType (server.c:501-508): **key destructor = NULL + val destructor = NULL** — 键由主 dict 持有, 值在 entry 联合体内
- getExpire (L1867) / removeExpire (L1838: kvstoreDictDelete) / PERSIST 语义 (PERSIST 命令面在 expire.c, R-22 边界)
- writable_slave 记账 (L1860-1862: rememberSlaveKeyWithExpire)
关键设计 (q3): expires 表是"键共享 + 值内嵌"的**零额外分配表** — 一个 TTL 只占一个 dictEntry + 8B 整数。[模式: 结构共享]
数据流: EXPIRE → setExpire → expires 表 → 惰性/主动过期读它 → 删除时先清它。

### 5. 键空间命令面 — 遍历/删除/互换

场景: KEYS/SCAN/DEL/FLUSHDB/SWAPDB 怎么协作?
源码路径:
- **SCAN 四步** (scanGenericCommand db.c:1049): 选项解析 (COUNT/MATCH/TYPE/NOVALUES) → 迭代 (ht: kvstoreScan/dictScan; 小编码: listpack 一次性返回) → **过期键过滤** (L1280-1301, expireIfNeeded + TYPE 过滤) → 游标+批量回复
- 集合型遍历: KEYS (L864: 模式匹配 + pslot 优化 L876-885) / RANDOMKEY (dbRandomKey L336: **FAIR 随机** + maxtries=100 + allvolatile 兜底 L351-360)
- 删除族: DEL/UNLINK (delGenericCommand L796: 先 expireIfNeeded 再删)
- **SWAPDB** (dbSwapDatabases L1712: 换 keys/expires/hexpires 指针 — 客户端不受影响; watch/blocked 表不换)
- **FLUSHDB/FLUSHALL** (L730-793: SYNC/ASYNC 三态 + blocking_async 后台执行 + jemalloc purge) / emptyDbStructure (L471: HFE DS 先毁)
- **getKeys 声明式** (L2133-2442): key-spec (INDEX/KEYWORD 起 + RANGE/KEYNUM 找, L2133) vs 旧 getkeys_proc 回调 (L2434) vs legacy range (L2379) — 三代提取 (7.x key-spec 生成式); 消费面: cluster 多键命令槽一致检查 + 命令级键存取标志 (RO/RW/OW)
关键设计 (q8): 命令面 = **统一副作用入口** (所有写命令过 signalModifiedKey L621 → WATCH/TRACKING); SCAN 在分片下走 kvstoreScan 跨 dict 游标 (篇 2)。[模式: 统一信号出口]
数据流: 命令 → 副作用链 → 信号 (WATCH/TRACKING/阻塞键/通知) → 回复。

### 负面空间 — 键空间刻意不做的事

- **不做自动过期扫描**: 惰性删除只在被访问时触发 (主动删除归 R-22 activeExpireCycle)
- **不做键级锁**: 单线程事件循环保证原子性 (多线程只做 IO, 不碰键空间)
- **不做独立键对象**: 键与值是分离的 (dict key=sds + value=robj), 无"key 对象"概念
- **不做物理删除确认**: DELETE 的异步面 (lazyfree) 释放时机不保证
- **不做事务隔离**: watch 信号只是失败标记 (R-16 详述), 无 MVCC

→ 引出: 分片怎么寻址?扫描游标怎么跨 dict?时间桶怎么过期?→ [[R-21-下]]
