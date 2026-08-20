# R-21 下 — kvstore 分片与时间桶: 键空间的物理实现

> 前置: [[R-21-上]] (键空间操作面) + [[R-3-Dict]] (分片单元) | 引出: [[R-22-过期机制]] (ebExpire 消费) + [[R-2-事件驱动]] | 对照: [[R-3-Dict]] (dictScan 弱语义在分片上的延伸) + [[R-15-cluster]] (16384 槽, 仅引用 cluster.h 常量, 域详述在 R-15)
> 🔴 A | 4 KP | [模式: 分片寻址+游标复用+时间分桶+阈值异步释放]
> Pass 2 闭环: q4(分片寻址) q5(扫描/随机/BIT) q6(ebuckets+HFE) q7(惰性删除)

**读者处境**: 单机 Redis 的键空间为什么也是"分片"的?SCAN 游标怎么在一个 dict 数组上保持"不重不漏"的弱语义?为什么 7.x 要造一个 ebuckets 时间桶管过期?大 key 删除为什么会卡 — lazyfree 怎么救?这篇拆键空间的物理实现: kvstore 分片、游标编码、Fenwick 随机、ebuckets 时间桶、异步释放。

### 1. kvstore — 一个"dict 数组"的键空间

场景: 为什么键空间是 kvstore 而非单个 dict?
源码路径:
- 结构 (kvstore.c:30-45): dicts[] 数组 + num_dicts_bits + **Fenwick 树 dict_size_index** (累积键数) + rehashing list + key_count/bucket_count 计数
- **kvstoreCreate** (L230): num_dicts_bits → `1<<bits` 个 dict (bits=0 时 1 个 — 单机退化为普通 dict); **上限 16 bits** (L233: 游标要省 48 位)
- flags (kvstore.h:14-15): KVSTORE_ALLOCATE_DICTS_ON_DEMAND (按需建 dict) + KVSTORE_FREE_EMPTY_DICTS (空 dict 回收)
- **三创建路径** (同一 slot_count_bits 逻辑): initServer (server.c:2674-2675) / initTempDb (db.c:569-570, 无盘复制) / emptyDbAsync (lazyfree.c:210-211) — 均为 cluster 时 14bit+FREE, 否则 0bit+按需
- **寻址**: getKeySlot (db.c:210) → keyHashSlot (cluster.h:43-56: `crc16(key) & 0x3FFF` = 16384 槽; `{...}` hash tag 只哈希括号内) → didx = slot
- **rehash 记账** (L185-215): 回调注入 — rehashingStarted/Completed 维护 rehashing list + bucket_count
关键设计 (q4): 分片目的 = **cluster 按槽定位** (迁移/FLUSH 按槽); 单机 bits=0 退化为 dict 但 API 统一; 每 dict 独立渐进 rehash, kvstore 用 list 统一调度。[模式: 数组化分片 + 按需分配]
数据流: 键 → crc16 → slot → didx → 对应 dict → dict API。

### 2. kvstoreScan — 48+14 位游标的跨 dict 迭代

场景: SCAN 游标怎么在 16384 个 dict 上跳?
源码路径:
- **游标编码** (kvstore.c:102-117): 高 48 位 = 当前 dict 内 dictScan 游标; 低 num_dicts_bits 位 = dict 索引 — 一次迭代两信息
- **kvstoreScan** (L361-403): 拆 didx → dictScan (R-3 反转游标) → 该 dict 扫完 (游标回 0) → **kvstoreGetNextNonEmptyDictIndex 跳下一个非空 dict** (L531-538: 累积计数+1 定位) → 重组游标返回
- **FAIR 随机** (L431-434): randomULong % kvstoreSize → **kvstoreFindDictIndexByKeyIndex** (L500-523: Fenwick 树 O(log n) 二分定位含第 N 个键的 dict) — 每 dict 被选概率 ∝ 元素数 (RANDOMKEY 无偏)
- 迭代器: kvstoreIterator (L555-617, 跨 dict 安全迭代 + 空 dict 回收) / kvstoreDictIterator (L683-722, 单 dict)
- onlydidx 优化 (L372-382): SCAN MATCH 聚簇槽 (KEYS 的 pslot)
- skip_cb (L386): 主动过期时跳过不可扫 dict
关键设计 (q5): 游标复用 dictScan 的 64 位, 分 48+bits 两段 — **不新增扫描语义** (每 dict 内仍是弱语义: 不丢旧键/重复允许/插入可漏); BIT 树把"选桶/跳桶"降到 O(log n)。[模式: 位域复用 + 树索引]
数据流: SCAN 游标 → 拆段 → dictScan → 跳非空 dict → 合并段 → 返回。

### 3. 增量 rehash 调度 — databasesCron 消费

场景: 空闲服务器上双表 rehash 谁来推进?
源码路径:
- **kvstoreIncrementallyRehash** (kvstore.c:642-661): rehashing list 队首循环 — 每 dict dictRehashMicroseconds (阈值预算内), **时间上限 threshold_us 一到即退**
- **kvstoreTryResizeDicts** (L621-633): resize_cursor 轮转 — 先缩容后扩容, 每轮 limit 个 dict
- 消费端 (server.c:1054-1101, R-20): databasesCron → 每 tick: 16 dict/DB 检查 (CRON_DICTS_PER_DB=16) + 1000us 预算增量 rehash (INCREMENTAL_REHASHING_THRESHOLD_US=1000, server.h:105,128) — **子进程存在时跳过** (防 CoW)
- cluster 扩容跳过 (L2022-2024: dbExpandSkipSlot — 只扩本节点槽)
关键设计 (q5): 空闲收敛 = **cron 预算制** (每 tick 1ms 上限, 多 DB 轮转); 与 R-3 的访问时联动迁移互补 (两路推进)。[模式: 预算式收敛]
数据流: cron tick → databasesCron → 预算 → 队列 dict 逐个推进 → 时间到退出。

### 4. ebuckets — 时间桶: 到期项的批量管理

场景: 过期字段 (HFE) 怎么高效"到点删一批"?
源码路径:
- **三级结构** (ebuckets.h:24-47): ebuckets (list 或 rax) → bucket (时间区间) → segment (≤16 项链表) → item (内嵌 **ExpireMeta** 48bit 到期时间 + 位标志)
- **ExpireMeta** (ebuckets.h:161-211): expireTimeLo 32 + expireTimeHi 16 = 48bit 毫秒时间戳; 位域 = lastInSegment/firstItemBucket/lastItemBucket/trash (4 单标志) + numItems:5 (仅段首维护) + userData:3; **指针低 1 位标记 list/rax** (itemsAddrAreOdd 技巧, 与 R-3 keys_are_odd 同族)
- **list→rax 升级** (L528-548, L62-63): ≤16 项 (EB_LIST_MAX_ITEMS=16) 单向链表 → 满则转 rax (bucketKey 6B 大端序键, EB_KEY_SIZE=6)
- **segment 分裂** (L286-330): 满 16 项 (EB_SEG_MAX_ITEMS=16) 尝试劈半; 全同 TTL 则**扩展段链** (ebSegAddExtended L201)
- **ebAdd** (L1424-1453): 时间戳 → EB_BUCKET_KEY (>>EB_BUCKET_KEY_PRECISION=0) → list 或 rax 插入
- **ebExpire** (L1464-1549): 从最小桶起逐段删到 `bucketKey >= nowKey` 停 (L1508); updateList 重插更新 TTL 项; 全空 → rax 释放回 NULL
- **ebExpireDryRun** (L1561-1648): 只数不删 (预算预判; 消费端仅 hash 本地 hfe t_hash.c:1307); ebGetNextTimeToExpire (L1663): 下次最小到期 (消费端仅 hash 本地 hfe t_hash.c:1992 — 全局 hexpires 不依赖它, 主动过期走 expires_cursor 遍历, R-22)
关键设计 (q6): 时间桶 = **按到期时间分组, 删一个桶 = 删一批** (摊销 O(1)); 精度分级 48bit/PRECISION=0 (TBD 10 — 当前 msec 精确); 分段聚合避免 rax 每项一个叶子 (每段 1 个 rax 叶子 vs 每项 1 个, 省 40B/项)。[模式: 时间分桶摊销]
数据流: 字段设 TTL → ebAdd → 桶内 segment → ebExpire 批量删 → 空桶回收。

### 5. HFE 注册与 lazyfree — 全局早到表 + 异步释放

场景: hash 字段级过期怎么全局调度?大 key 删除怎么不卡?
源码路径:
- **两级 ebuckets** (t_hash.c:110-130): db->hexpires = 全局表 (hashExpireBucketsType, 键=hash 对象, TTL=最早字段); 每 hash 内部 hfe (hashFieldExpireBucketsType, itemsAddrAreOdd=1) — **全局表只挂"最早到期字段的 hash"**, 到时由 hash 本地 ebExpire 处理其余
- 注册/摘除: **hashTypeAddToExpires (t_hash.c:2040-2060) 是全局注册唯一入口** — 常规路径: 字段带 TTL 写入/编码转换时内联 ebAdd (hashTypeConvert t_hash.c:1624-1662) + hashTypeSet 更新 (L1229-1231); 特殊路径: RENAME/MOVE/COPY (db.c:1446,1529,1643) / cluster 迁移 (cluster.c:248); hashTypeRemoveFromExpires (db.c:282 覆盖旧值时)
- **freeObjAsync** (lazyfree.c:184-196): lazyfreeGetFreeEffort (L129-174: 按编码估工作量 — list=节点数, dict=元素数, stream=宏节点) → **> LAZYFREE_THRESHOLD=64 且 refcount=1** → bio 异步; 否则同步
- **emptyDbAsync** (L201-215): 换表法 — 新建空 kvstore/ebuckets 顶替, 旧表交 bio (lazyfreeFreeDatabase L23-41: kvstoreRelease ×2 + ebDestroy + jemalloc purge)
- 三态删除入口 (db.c:372-425): dbSyncDelete/dbAsyncDelete/dbDelete (配置驱动)
关键设计 (q7): HFE 全局表 = **"最早到期者"代理** (一层薄索引, 其余字段留在 hash 本地); lazyfree = **"工作量阈值 + 换表"** 双保险 — 单点删除看工作量, 全库清空用指针交换。[模式: 代理索引 + 阈值异步]
数据流: 删除大 key → effort > 64 → bio 队列 → 后台 decrRefCount; FLUSHDB ASYNC → 换新表 → 旧表后台释放。

### 负面空间 — 键空间实现刻意不做的事

- **不做跨 dict 原子操作**: 多键命令逐个访问 (单线程保证整体一致)
- **不做 dict 数组动态伸缩**: num_dicts_bits 创建时定死 (cluster 恒 14), 不随槽位变化
- **不做时间桶精度提升**: EB_BUCKET_KEY_PRECISION=0 显式 TBD (注释 "modify to 10"), 主动过期面是近似 (惰性面精确)
- **不做 ebRemove 合并**: TODO 注释明示 segment 碎片不合并
- **不做无界异步**: lazyfree 队列 FIFO, 无优先级/无限流 (bio 线程数固定)

→ 引出: 主动过期如何消费 ebExpire/ebExpireDryRun?→ [[R-22-过期机制]]
