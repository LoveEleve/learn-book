# R-3 Dict — 哈希表: 双表渐进迁移 + 2 幂掩码 + 访问感知调度

> 前置: [[R-33-zmalloc]] (分配) + [[R-4-SDS]] (sds 键/恒奇指针) | 引出: [[R-19-listpack]] (另一条存储路线) | 对照: [[R-21-db]] (键空间宿主)
> 🔴 A | 8 KP | [模式: 双表渐进迁移+阈值双轨+访问感知调度+指针编码压缩+反转游标]
> Pass 2 闭环: q1(渐进) q2(阈值) q3(联动) q4(掩码) q5(单指针) q6(扫描) q7(定制) q8(SipHash)

**读者处境**: Redis 里所有键值都存在一张哈希表 — 为什么它要两张表?插入时为什么"偶尔卡一下"?RDB 保存时哈希表为什么"不扩容了"?SCAN 遍历时哈希表正在搬家, 为什么不会漏掉迭代前存在的键?这篇拆 Redis 哈希表: 渐进式迁移、阈值双轨、2 的幂掩码, 以及"把 key 直接当指针存"的内存魔法。

### 1. 双表渐进 — 搬家不阻塞

场景: 插入 1000 万键时为什么要"边用边搬家"?为什么不一次性重建?
源码路径:
- `dict.h:96-102` — `ht_table[2]` 双表 + `rehashidx` 游标 (-1 = 不在迁移) + `ht_used[2]` 计数
- `dict.c:385-414` (dictRehash) — n 步迁移 (每步一个桶) + **空桶扫描上限 n*10** (注释: "otherwise the amount of work it does would be unbound and the function may block for a long time")
- `dict.c:362-374` (dictCheckRehashingCompleted) — ht[0] 清空 → 释放旧表 → **ht[1] 提升为 ht[0]** → rehashidx=-1
- `dict.c:448` (_dictRehashStep) — 每次查找/添加顺带 1 步
- **迁移暂停面 (pauserehash)**: `dict.c:815,833` — 两阶段删除 (dictTwoPhaseUnlinkFree: 定位→暂停→删除→恢复) 冻结迁移防 entry 丢失; `dict.c:1034` — **安全迭代器生命周期内 dictPauseRehashing** (rehash 完全冻结防迭代丢键); 非安全迭代器用 fingerprint 检测误用 (dict.c:950-1010)
关键设计: 增量迁移状态机 (q1): 双表并存 + 游标逐桶推进 — 迁移摊到每次操作 + 周期任务, 单次延迟有界 (空桶上限); 迁移期间查找/添加/删除双表兼容; **迭代/两阶段操作期间暂停迁移**。[模式: 双表渐进 + 有界步进 + 暂停面]
数据流: 触发扩容 → 分配 ht[1] → rehashidx=0 → 操作联动+serverCron 逐桶迁移 → ht[0] 空 → 提升切换。

### 2. 阈值与 COW — 什么时候搬家

场景: 为什么有子进程时哈希表"不到 4:1 不扩容"?
源码路径:
- `dict.c:1492-1515` — 扩容触发: **1:1 比例** (used == buckets); 空表→4 桶起步
- `dict.c:1533-1550` — 缩容触发: **<1:8** (ENABLE); 初始 4 桶不缩
- `dict.c:41-42` — 全局三态 + `dict_force_resize_ratio=4` + `HASHTABLE_MIN_FILL=8`
- `server.c:640-652` (updateDictResizePolicy) — **fork 子进程 → FORBID (全冻结)**; RDB/AOF/复制子进程活跃 → **AVOID (4:1 才扩 / 1:32 才缩)**; 正常 → ENABLE
关键设计: 阈值双轨 (q2): 迁移会触发 COW 页复制 — 子进程在读旧页时扩容 = 大量页复制, 所以 AVOID 把阈值放宽到"必须" (4:1/1:32); fork 中彻底冻结。**阈值是内存效率与 COW 成本的权衡**。[模式: 阈值双轨 + COW 联动]
数据流: 子进程启动 (RDB 保存) → AVOID → 哈希表只在高比例才动 → 子进程退出 → ENABLE 恢复。

### 3. 访问感知调度 — 查找顺便搬家

场景: 迁移怎么"蹭"命令执行?热门桶为什么先搬?
源码路径:
- `dict.c:736-757` (dictFind) — rehash 中: **目标桶未迁移且非空 → `_dictBucketRehash(d, idx)` 直接迁移它** (注释: "being more CPU cache friendly"); 否则 `_dictRehashStep` 游标步进
- `dict.c:472-489` (_dictBucketRehash) — 单桶迁移 + 完成检测 + AVOID 阈值检查
- `dict.c:758-770` — 双表查找: 已迁移区 (idx < rehashidx) 跳过 ht[0], 表 1 用新掩码
关键设计: 访问感知 (q3): 马上要读的桶先迁过来 — 后续命令大概率命中同一区域 (缓存友好); 冷桶交给 serverCron 限时兜底。查找代价 O(1)+偶发迁移。[模式: 访问模式调度]
数据流: GET key → 哈希 → 桶未迁且非空 → 迁移该桶 → 双表查找 → 返回。

### 4. 2 幂掩码 — 缩容免重算哈希

场景: 扩容要重新计算每个键的哈希, 缩容为什么不用?
源码路径:
- `dict.c:320-327` (rehashEntriesInBucketAtIndex) — **扩容**: `h = dictHashKey(key) & 新掩码` (重算); **缩容**: `h = idx & 新掩码` — 注释: "The tables sizes are powers of two, so we simply mask the bucket index in the larger table"
- 数学依据: 表 0 大小 2^k, 表 1 大小 2^j (j<k); 原索引 = h 的低 k 位, 新索引 = 低 j 位 = **idx & (2^j-1)** — 低 j 位不变
关键设计: 2 幂数学 (q4): 表大小恒为 2 的幂 → 掩码寻址替代取模, **缩容迁移纯位运算零哈希计算**; 扩容仍需重算 (掩码变大)。这是"2 幂表"设计的数学回报。[模式: 2 幂表 + 掩码复用]
数据流: 缩容 → 桶内每元素: 新索引 = 旧索引 & 新掩码 → 链入新表。

### 5. 单指针 entry — 把 key 当指针存

场景: set 的哈希表为什么"没有 value"?key 为什么能直接当 entry?
源码路径:
- `dict.c:128-171` — **低 3 位指针编码三态**: `entryIsKey` (de & 1 — **key 指针直存, 无 entry 分配!**) / ENTRY_PTR_NORMAL (完整 entry) / ENTRY_PTR_NO_VALUE (key+next 无 value)
- `dict.c:152-159` — encodeMaskedPtr/decodeMaskedPtr: 指针 OR/AND 位标记 (前提: 被编码指针低位为 0)
- **sds 恒奇机制** (q5 闭环): sdsnewlen `s = sh + hdrlen` — malloc 16 对齐 (偶) + **sdshdr 恒奇数大小 (1/3/5/9/17B)** → sds 指针恒奇 → `&1` 即可识别
- `server.c:474-475` — 键空间 dict: `no_value=0, keys_are_odd=1` (有值但键恒奇); set 内部 dict: `no_value=1` (server.c:634)
- **链的维护 (dict.c:328-349)**: **仅目标桶为空时 key 直存** (无 next 字段可链); 桶非空时退回 `createEntryNoValue(key, next)` 建带 next 的轻量 entry — 直存是"桶空特权"
关键设计: 指针位编码压缩 (q5): 无值字典的 key 在桶空时直接存桶 (省 16-24B/元素 entry); 指针低 3 位闲置位复用为类型标记 — 与 sds 奇数指针设计合谋。[模式: 指针位压缩 + 桶空特权]
数据流: SADD → set 内部 dict → key (sds 奇数指针) 直接入桶 → SISMEMBER 用 &1 识别后比较。

### 6. dictScan 反向游标 — 搬家不丢键

场景: SCAN 遍历中哈希表正好在扩容, 怎么保证不丢键?重复怎么办?
源码路径:
- `dict.c:1369-1470` (dictScan/dictScanDefrag) — 非 rehash: 桶遍历 + `v |= ~m0; v = rev(v); v++; v = rev(v)` — **反向二进制游标**
- rehash 中: 小表桶 + **大表展开区** (do-while `v & (m0 ^ m1)`) — 扩容后 1 桶变 2/4 桶, 全部访问
- **精确语义 (SCAN 文档)**: 迭代开始前存在的键**不丢** (保证); **重复允许** (rehash 中键可能在双表都被扫到); **中途插入的键可能漏** (游标已过的区域不回溯 — harness 实证: 扩容后插入桶 16 的键未被返回, 合法)
关键设计: 反转游标 (q6): 教科书级位运算技巧 — 遍历顺序与 2 幂扩展同构; 但 SCAN 语义是"弱保证": 不丢旧键、容忍重复、新键不保证。[模式: 反转游标遍历 + 弱一致性]
数据流: SCAN cursor → rev+1+rev → 遍历桶 (双表展开区) → 返回新游标 → 循环至 0。

### 7. dictType 定制 — 一个内核, 无数用法

场景: 键空间/过期表/命令表/set 内部 — 为什么用同一个哈希表?
源码路径:
- `dict.h:32-92` — dictType: hashFunction (必选) / keyCompare / keyDup/valDup / keyDestructor/valDestructor / expandAllowed + 7.x: no_value/keys_are_odd/storedKey API/rehashingCompleted/onDictRelease/dictMetadataBytes
- `dict.h:94-111` — dictEntry 联合值: v{val, s64, u64, double} — **整数键值直接存, 免指针分配**
- `server.c:467-480` — dbDictType: `dictSdsHash` (键哈希) + keys_are_odd=1; 命令表用 `dictSdsCaseHash` (大小写不敏感)
关键设计: 策略注入 (q7): 哈希/比较/复制/释放全函数指针化 — 键空间 (sds→robj)、过期表、命令表同构异制; 联合值让整数值免分配。[模式: 策略注入 + 联合值]
数据流: 键空间: sds 键哈希 → 桶 → entry (key=sds, val=robj*)。

### 8. SipHash — 防碰撞的密钥化哈希

场景: 为什么哈希函数要带"密钥"?2014 年发生了什么?
源码路径:
- `dict.c:92-113` — `dictGenHashFunction → siphash(key, len, seed)` + `dictGenCaseHashFunction → siphash_nocase` — **16 字节随机 seed 作密钥**
- `server.c:6985-6987` — main 启动: `getRandomBytes(hashseed, 16)` → dictSetHashFunctionSeed
- 历史: 2014 年 HashDoS 攻击潮 (Java/PHP/Node.js 哈希表被碰撞攻击打崩) — Redis 同年切换到 SipHash
关键设计: 密钥化哈希 (q8): 哈希结果依赖启动时随机密钥 — 攻击者不知道 seed 就无法构造碰撞集; 代价是每次哈希稍慢 (密码学级), 换来输入不可预测性。[模式: 密钥化安全哈希]
数据流: 键 → siphash(键, len, 随机seed) → 不可预测的桶分布。

### 负面空间 — Dict 刻意不做的事

- **不做完美哈希**: 链式冲突解决 (桶内链表), 不追求 O(1) 最坏 (负载因子控制平均)
- **不做并发安全**: 无锁 (Redis 单线程; defrag/io 线程不并发改 dict)
- **不做删除后立即缩容**: 缩容只在删除路径的 ShrinkIfNeeded 触发, 不主动压缩
- **不做迭代器快照**: 非安全迭代器有 fingerprint 防误用 (修改即报错), 非快照语义
- **不做内存池**: entry 分配走 zmalloc, 不缓存空闲 entry

→ 引出: listpack 是另一条存储路线 — 紧凑数组 vs 哈希链的取舍 → [[R-19-listpack]]
