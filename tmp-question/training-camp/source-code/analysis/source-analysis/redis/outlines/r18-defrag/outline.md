# R-18 内存碎片 — 在线整理 (jemalloc 感知搬移 + 渐进扫描)

> 前置: [[R-33-zmalloc]] (mallocx 家族/HAVE_DEFRAG) + [[R-3-dict]] (反向游标/单指针 entry/storedKey) + [[R-21-db]] (kvstore 四阶段) + [[R-2-events]] (serverCron/whileBlockedCron) + [[R-1-object]] (EMBSTR/refcount) | 引出: [[R-31-module]] (搬移回调 API) | 对照: [[R-22-expire]] (同款 timelimit 预算) + [[R-23-evict]] (内存治理另一面)
> 🟡 B | 6 KP | [模式: 分配器感知搬移 + 渐进扫描 + 延迟任务 + 插值调度]
> Pass 2 闭环: q1(搬移原语) q2(渐进扫描) q3(类型分派) q4(大键延后) q5(调度) q6(配置/统计/模块)

**读者处境**: Redis 跑久了 used_memory 不高但 RSS 很高 — 重启才能降? 为什么碎片只在小对象上? "不值得搬"由谁说了算? 搬一个指针为什么牵一发动全身? 这篇拆在线碎片整理: jemalloc 感知判定、no_tcache 搬移、渐进扫描、大键延后、插值调度。

### 1. 搬移原语 — jemalloc 告诉你"这指针值得搬"

场景: 为什么 Redis 知道哪些分配是碎片?
源码路径:
- **判定 API** (defrag.c:30-32): `je_get_defrag_hint(ptr)` — jemalloc 为 Redis 加的补丁方法
- **jemalloc 实现** (jemalloc_internal_inlines_c.h:341-400): 仅**小分配** (slab) 判定; **跳过 slabcur** (L362, 当前写入 slab 搬走无意义); 统计 bin 全部 shard 的 non-full slabs 平均利用率; **判定式** (L389): `(nregs-free_in_slab)×curslabs <= curregs + curregs/8` — 本 slab 利用率 ≤ 平均 + **12.5% 防停滞权重**
- **搬移流程** (defrag.c:39-55): hint=1 → `zmalloc_usable_size` → **`zmalloc_no_tcache`** (mallocx MALLOCX_TCACHE_NONE, zmalloc.c:199-213) → memcpy → `zfree_no_tcache` → hits++; hint=0 → misses++
- **no_tcache 动机** (L46-48): "so that we don't get back the same pointers we try to free" — thread cache 会立刻复用刚释放的块
- **指针家族** (L62-127): activeDefragSds (分配内偏移保留) / activeDefragStringObEx (**expected_refcount 检查** — 共享对象不搬; **EMBSTR 特例**: sds 内嵌 robj 分配, 搬后 `ret->ptr = ret + ofs` 重算; **INT 编码值在 ptr 无分配可搬跳过**, 其余未知编码 serverPanic 兜底 L122-124)
- **大分配永不搬**: hint=0 (不产生小 bin 碎片)
关键设计 (q1): **分配器感知 = 零成本过滤** — Redis 不做自己的碎片判定, 把"利用率低于平均的 slab 里的对象"交给 jemalloc 逐指针裁决; 搬移=新分配-拷贝-释放, 指针必须全局替换。[模式: 感知搬移]

### 2. 渐进扫描 — dictScan 的"可搬移"变体

场景: 千万键的 dict 怎么不卡地扫完?
源码路径:
- **dictScanDefrag** (dict.c:1385-1470): 反向游标 + rehash 双表扩张桶 = **R-3 dictScan 同款算法** (差异仅在桶处理); **dictPauseRehashing** (L1398) / **dictResumeRehashing** (L1472) 配对 — 扫描期冻结 rehash (搬移后按哈希查表安全)
- **dictDefragBucket 三态** (dict.c:1217-1252) — 消费 R-3 单指针 entry 编码:
  - entryIsKey (storedKey): key 即 entry → defragKey 返回值替换桶头 (L1232-1235)
  - entryIsNoValue: 解包 → defragalloc 搬 → 重编码 (L1236-1241)
  - entryIsNormal: 搬 entry 本身 + key/val 就地替换 (L1242-1249)
  - 沿 next-ref 链式推进 (L1251)
- **defragfns 三回调** (dict.h:135-142): defragAlloc (entry 本身) / defragKey / defragVal — 参数化 val_type 5 档 (defrag.c:295-310: NO_VAL/SDS/STROB/VOID_PTR/LUA_SCRIPT)
- **kvstore 面**: kvstoreDictScanDefrag 跨 slot + **kvstoreDictLUTDefrag** (kvstore.c:778-790, LUT 数组逐个 dict 结构搬移 + **rehashing 链表节点 value 同步** L785-788 — 增量 rehash 链是"引用持有者", 搬 dict 后必须更新, R-21 交叉)
关键设计 (q2): **游标续扫 + 桶内就地替换** — 搬移不打断扫描 (next-ref 已缓存); PauseRehashing 保证搬移后按哈希查表安全。[模式: 渐进扫描]

### 3. 类型分派 — 一个键的全部指针

场景: 搬一个键名, 为什么 expires 表也要动?
源码路径:
- **defragKey** (defrag.c:729-822): 键名 → robj → 值 三步搬移
- **键名同步** (L737-752): keys 表 + **expires 表** (L740-747 — 搬后旧指针已释放, 不能用字符串比较, 改 `FindEntryByPtrAndHash` 哈希+指针定位); HASH → hashTypeUpdateKeyRef (HFE/listpackEx 键引用)
- **robj 与 HFE** (L754-758): hashTypeGetMinExpire 有效 → **ebDefragItem** (时间桶持有 robj 引用, 必须同步 — R-21); 否则直接搬
- **type×encoding 矩阵** (L768-821): 紧凑编码 (listpack/intset) 整块搬 ob->ptr; quicklist → 节点+entry 双搬 (L357-381); skiplist → zset 结构/zsl/header/元素四层 (L498-525); listpackEX 双搬 (L804-809); **stream 递归 6 层** (L694-713: s → rax entry → cgroups → consumers → pel → nack, nack 搬移须 raxInsert 回 pel 断言旧指针 L653-665); module → moduleDefragValue
- **zset 双结构一致** (L246-256): dict 键 sds 搬 → zsl 节点 ele 同步 → zslDefrag 返回 score 引用 → dict val 更新
- **pubsub 共享频道** (L870-902): refcount 断言 = clients+1 (L878); 搬后客户端侧表同步 (L886-894)
- **defragOtherGlobals** (L906-915): eval scripts / module globals / pubsub LUT — "one small allocation can hold a full allocator run" (L907)
关键设计 (q3): **搬移的隐式契约 = 引用全局同步** — 每次搬移必须找到所有持有旧指针的位置 (共享 sds 键/时间桶/双结构/共享频道); serverPanic 兜底未知类型。[模式: 跨引用同步]

### 4. 大键延后 — 防延迟尖峰

场景: 100 万字段的 hash 一次扫完会卡 100ms?
源码路径:
- **动机** (L383-385): "prevent latency spikes when handling large items"
- **登记** (L386-389): `sdsdup(key)` → db->defrag_later (拷贝键名 — 主键名可能随后被搬)
- **阈值** (L492/513/532/546/703): `> active_defrag_max_scan_fields` (默认 1000) — list/zset/hash/set/stream 五处
- **三种续扫技术**:
  - list → `quicklistBookmark "_AD"` (L392-433; bookmark 记录断点, 128 节点/时限; 失败 → 从头)
  - set/zset/hash → dictScanDefrag 游标 (L446-484)
  - stream → **static last[16] 存最后 ID** + raxSeek(">", last) (L567-616; rax 节点 node_cb 搬移 + entry 搬)
- **主循环衔接** (L1185-1210): 每桶扫描前先清积压; 桶扫完列表非空 → **延后扫描插队优先** (L1199-1204, defrag_later_item_in_progress=1 不推 slot, 清完才前进)
- **每键时限** (L990-992): 16 迭代 / 512 重分配 / 64 字段 三条件
关键设计 (q4): **游标化大键 = 把"一次性大任务"切成小片**; bookmark/static-last 是单线程下的廉价断点 (无需锁)。[模式: 延迟任务队列]

### 5. 调度 — 插值 effort 与 cron 预算

场景: 碎片 30% 时花多少 CPU? 什么时候停?
源码路径:
- **碎片率** (L841-867): `frag_pct = frag_smallbins_bytes/allocated×100` — **小 bin 浪费占比** (注释: 大 bin 多时 rss 比例虚高 L854-857); **Lua arena 排除** (L845-852, Lua 分配不可搬)
- **双门槛 AND** (L1020-1023): pct ≥ lower(10) **且** bytes ≥ ignore(100MB) 才启动 — "任一不足不启动"
- **effort 插值** (L1027-1034): INTERPOLATE(frag, lower→upper, min→max) 线性映射 [10%,100%] → [1%,25%] + LIMIT 钳制
- **只升不降** (L1039-1047): 扫描中途 frag 下降不降 effort ("should not lower the aggressiveness"); **例外: CONFIG SET 时无条件采用新值 (允许下降)** — 条件 = `cpu_pct > running OR configuration_changed`
- **cycle 主循环** (L1053-1252): fork 子进程 → **直接 return** (L1089, "will just do damage" — COW); timelimit = `1M×running/hz/100` (25%@100hz = 2.5ms, 对照 activeExpireCycle L1108); **四阶段** (L1172-1179): keys → expires → pubsub×2; **expires 阶段仅计数** (scanCallbackCountScanned — 共享 sds 键引用已在 keys 阶段同步, 避免双搬); **16 迭代/512 重分配/64 键** 检时限 (L1224-1227); defragOtherGlobals 须同周期完成 (L1222-1223); 完成 → running=0 + 立即重决策 (L1144-1147)
- **双调用点** (server.c:1066 + 1586): serverCron 常规 + **whileBlockedCron 循环补齐** (L1578-1583: "if activeDefragCycle needs to utilize 25% cpu, it will utilize 2.5ms, so we need to call it multiple times") — 阻塞命令期间不浪费预算
关键设计 (q5): **碎片度 → CPU 预算的线性映射 + 单调执行**; fork 暂停防 COW 放大 (搬移=写页); 阻塞期补齐保证"预算守恒"。[模式: 插值调度]

### 6. 命令面 — 7 配置 + 模块搬移 API

场景: 怎么开? 怎么调? 模块键怎么办?
源码路径:
- **7 配置** (config.c): activedefrag (L3080, 默认 no, **非 jemalloc 编译拒绝** L2323-2337) / cycle-min 1 / cycle-max 25 / threshold-lower 10 / threshold-upper 100 / max-scan-fields 1000 / **ignore-bytes 100MB** (L3220) — 全 MODIFIABLE 热调; cycle/threshold-upper 挂 configuration_changed 立即生效 (L1100-1103)
- **INFO**: active_defrag_running (测试断言 effort) / hits / misses / **key_hits / key_misses (键级统计 — 键内任何指针搬移即命中, defragScanCallback L826-833)** / scanned / total+current_time (server.c:5746,5852-5853,5896-5900)
- **latency monitor**: "active-defrag-cycle" 采样 (L1242)
- **排障命令面**: `debug mallctl arenas.page` (判页大小, 测试 L53) / MEMORY DOCTOR 报告碎片不治疗
- **模块 API** (module.c): RM_RegisterDefragFunc (L13449) / moduleDefragValue 立即搬 (L13580, 返 0 → defragLater) / moduleLateDefrag 延后 (L13553) / moduleDefragGlobals — 测试 tests/modules/defragtest.c
- **测试地图** (memefficiency.tcl L39-800): 8 场景 — 主 dict (700000 键 + **digest 数据不变校验** + RDB save) / AOF loading (while-blocked-cron) / eval scripts / big keys / pubsub / HFE / big list / edge case
关键设计 (q6): **全配置热调 + 模块注册制** — 模块键的搬移逻辑由模块自己声明 (Redis 无法通用搬); digest 校验是"搬移无数据损坏"的黄金测试。[模式: 扩展 API]

### 负面空间 — 在线整理刻意不做的事

- **不做大分配整理**: je_get_defrag_hint 只判小 slab; 大对象不搬 (不产生小碎片)
- **不做 rss 驱动**: 判定用 small-bins 浪费占比, 纯 rss 高 (如页表/元数据) 不触发
- **不做重启替代**: 极严重碎片仍建议重启 (4.0 前唯一办法, redis.conf L2240-2241)
- **不做 arena 调优**: 不干预 jemalloc arena 数量/迁移 (对照 jemalloc-bg-thread 仅线程)
- **不做实时搬移保证**: 搬移成功率依赖 hint 判定 (slab 利用率); MEMORY DOCTOR 只报告不治疗
- **不做跨 fork 搬移**: 子进程期间完全暂停 (COW 保护优先)
- **不做数据压缩**: 只搬位置不缩数据 (对照 R-4/R-19 编码层)

→ 引出: 模块怎么实现自己的搬移? → [[R-31-module]]
