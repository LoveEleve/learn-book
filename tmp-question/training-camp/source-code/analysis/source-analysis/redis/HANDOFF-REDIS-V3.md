# Redis 源码分析 — 超详细交接文档 V3 (阶段 3.5, 28/33 完成)

> **⚠ 本文已被 V4 取代**: 全量交接请用 **`HANDOFF-REDIS-V4.md`** (28 域核心知识全固化 + 剩余 5 域详案, 945 行, 新会话零回溯)。本文 V3 保留作历史。

> **日期**: 2026-08-13 | Redis 7.4.2 (src/ 175 文件) | 本 V3 为**自包含全量交接** (固化全部 28 域核心知识, 新会话无需回溯历史对话)
> **入口关系**: 阶段3 总入口 `../HANDOFF-STAGE3.md` | 域规划 `REDIS-PLAN.md` (33 域 v2, §〇 怀疑审计表) | 本文替代 V2 作为 Redis 分域唯一入口
> **给新 AI**: 读 §零 状态总览 → §四 (R-17 详案) 开工; 每域完成后回填 §一 (该域速查) + 更新 §零/§四 + HANDOFF-STAGE3。方法论铁律见 §二。

---

## §零 状态总览 (2026-08-13, 28/33)

### 完成状态表

| 域 | 目录 | 类型 | 大纲行数 | 闭环 | questions | 行号验证 | harness | 二次 REVIEW |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| R-33 zmalloc | r33-zmalloc | 🔴 | 85 | 8 | 20 | — | 6/6+6/6 | — |
| R-4 SDS | r4-sds | 🔴 | 104 | 8 | 20 | 40 处 | 16/16 | — |
| R-3 Dict | r3-dict | 🔴 | 101 | 8 | 20 | 33 处 | 16/16 | ✓ (4 处) |
| R-19 listpack | r19-listpack | 🔴 | 94 | 8 | 20 | 34 处 | 16/16 | — |
| R-7 intset | r7-intset | 🟡 | 78 | 6 | 20 | 26 处 | 无 (方案 B) | — |
| R-6 skiplist+ZSet | r6-zset | 🔴 | 99 | 8 | 20 | 32 处 | 8/8 | — |
| R-5 quicklist | r5-quicklist | 🔴 | 96 | 8 | 20 | 28 处 | 11/11 | — |
| R-1 redisObject | r1-object | 🔴 | 94 | 8 | 20 | 28 处 | 14/14 | — |
| R-20 server 骨架 | r20-server | 🔴 | 53+56 | 8 | 20 | 35 处 | 5/5 | — |
| R-21 db 键空间 | r21-db | 🔴 | 77+80 | 8 | 21 | ~80 处 | 44/44 | ✓ (10 处) |
| R-22 过期机制 | r22-expire | 🟡 | 95 | 6 | 21 | 96 处 | 无 | ✓ (5 处) |
| R-23 内存淘汰 | r23-evict | 🟡 | 99 | 6 | 21 | 130 处 | 无 | ✓ (2 处) |
| R-2 事件驱动 | r2-events | 🔴 | 79+60 | 8 | 21 | 100 处 | 23/23 | ✓ (4 处) |
| R-28 networking | r28-networking | 🔴 | 69+70 | 8 | 21 | 116 处 | 23/23 | ✓ (2 处) |
| R-24 t_string | r24-string | 🟡 | 85 | 6 | 20 | 117 处 | 无 | ✓ (2 处) |
| R-25 t_hash | r25-hash | 🔴 | 65+66 | 8 | 21 | 129 处 | 30/30 | ✓ (3 处) |
| R-26 t_list+blocked | r26-list | 🔴 | 61+66 | 8 | 21 | 117 处 | 16/16 | ✓ (2 处) |
| R-27 t_set | r27-set | 🟡 | 86 | 6 | 21 | 82 处 | 无 | ✓ (1 处) |
| R-11 Bitmap | r11-bitmap | 🟡 | 83 | 6 | 21 | 56 处 | 无 | ✓ (3 处) |
| R-12 HLL | r12-hll | 🟡 | 88 | 6 | 20 | ~40 处 | 无 | ✓ (2 处) |
| R-13 GEO | r13-geo | 🟡 | 86 | 6 | 20 | ~40 处 | 无 | ✓ (2+4 处) |
| R-29 pubsub+notify | r29-pubsub | 🟡 | 87 | 6 | 20 | ~60 处 | 无 | ✓ (2+5 处) |
| R-16 事务 | r16-multi | 🟡 | 86 | 6 | 20 | ~30 处 | 无 | ✓ (1+3 处) |
| R-8 RDB+AOF | r8-persistence | 🔴 | 88+91 | 12 | 40 | ~50 处 | 156 | ✓ (1+3 处) |
| R-9 复制 | r9-replication | 🔴 | 92+88 | 12 | 40 | ~50 处 | 36 | ✓ (1 处) |

> 注: "大纲行数" = outline.md 单文件行数 (拆 2 篇域为两文件行数和), 2026-08-13 实测 (二次 REVIEW 修改后已同步)。

### 执行序 (已完成 28, 剩余 5)

```
✅ R-33 → R-4 → R-3 → R-19 → R-7 → R-6 → R-5 → R-1 → R-20 → R-21 → R-22 → R-23 → R-2 → R-28 → R-24 → R-25 → R-26 → R-27 → R-11 → R-12 → R-13 → R-29 → R-16 → R-8 → R-9 → R-10 → R-14 → R-15
➡️  R-17 (客户端缓存) → R-18 (碎片) → R-30 (Lua+Functions) → R-32 (ACL) → R-31 (module)
```

### 分层进展

- **基础层 (9)**: zmalloc/SDS/dict/listpack/intset/zset/quicklist/object/server 骨架 ✅
- **数据库层 (3)**: 键空间/过期/淘汰 ✅
- **事件与网络 (2)**: 事件驱动/协议 ✅
- **命令层 (4)**: t_string/t_hash/t_list/t_set ✅
- **高级数据结构 (3)**: Bitmap + HLL + GEO ✅ — 剩余: Stream
- **高级数据结构 (4)**: Bitmap + HLL + GEO + Stream ✅ — 全部完成
- **持久化/复制/HA (2)**: 持久化 + 复制 + Sentinel + Cluster ✅ — 全部完成

### 交付物统计

- 大纲 28 域 / 39 篇 (11 个大域拆 2 篇: +R-15) / 总计 9867 行
- harness 18 个, 全部 gcc+ASan 验证通过 (合计 530 断言)
- 行号穷举验证累计 ~1785 处
- 深审+二次 REVIEW 发现问题累计 **113 处** (行号偏差 40+/编造 3/表述 25+/前向依赖违规 3/覆盖缺口 7/机制洞察 19/认知修正 3)

---

## §一 28 域核心知识速查 (全量固化)

> 每域格式: 核心机制表 / 时空溯源 / 深审发现 / 负面空间 / harness 迭代。行号均为 Redis 7.4.2 实测。

### R-33 zmalloc — 内存分配层

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 分配器多路: tcmalloc/jemalloc/macOS/libc 编译期选择, 宏重映射 | zmalloc.h:29-79; zmalloc.c:54-68 |
| **双路径记账**: HAVE_MALLOC_SIZE → 0 前缀 (usable 实时) / 否则 8B 前缀存请求大小 — harness 实证: 10B 请求记 16 vs 18 | zmalloc.c:37-45,93-111 |
| OOM 分层: zmalloc → handler → serverPanic; ztry 家族 → NULL 降级 (rdb/t_stream/module) | zmalloc.c:124-134; server.c:6712-6717 |
| **usable 优化**: zmalloc_usable → sds 初始 alloc 免费膨胀 (harness: alloc=15 > 请求 3) | zmalloc.c:138-147; sds.c:90-105 |
| extend_to_usable: alloc_size 属性骗编译器, 解 _FORTIFY_SOURCE SIGABRT | zmalloc.h:110-124 (注释段实为 125-146) |
| 原子统计: used_memory redisAtomic (bio 线程实证) | zmalloc.c:70-73; atomicvar.h:62-92 |
| jemalloc 绑定: with_flags/no_tcache (R-18 消费)/arena (Lua)/HAVE_DEFRAG | zmalloc.c:149-213 |

**时空溯源**: 2009 初版 → 旧 libc 前缀路径 → 6.x defrag → 7.x try 家族 → gcc-12 extend_to_usable

**深审**: 表述错误 1 (jemalloc used_memory 更高 — 实为记账语义不同, 非高低); 行号 1 (extend_to_usable 125-146); 完整性 2 (zcalloc_num 溢出检查 L249-259 / zmalloc_get_memory_size 兜底)

**负面空间**: 不做 GC/内存池/大小分级/压缩/全 OOM 兜底

### R-4 SDS — 字符串

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 指针即对象: sds=char*, packed 头部内嵌, s[-1]=flags | sds.h:20-79 |
| **五级头部**: 31/255/65535/2^32; sdshdr5 仅 1B 静态串专用 (从不作 struct), 空串/扩容跳 8 | sds.c:40-52,87,241-244 |
| **扩容三路**: avail 够原地 / 同型 realloc / 升级 malloc+memcpy+free ("can't use realloc") | sds.c:217-268 |
| **伪降型缩容**: use_realloc = 同型\|\|(降型>8) — 保留旧头只缩 alloc, s[-1] 不更新 (harness 实证) | sds.c:327-343 |
| 预分配: greedy <1MB→2× / ≥1MB→+1MB; NonGreedy=querybuf 按需 | sds.c:232-237; networking.c:2401,2698 |
| usable 接力: alloc=分配器实际; sdsResize je_nallocx 预查询 | sds.c:93-105 |
| **零拷贝**: sdsIncrLen (read 直写+递增, 负增量去 CRLF) | sds.c:385-440 |

**时空溯源**: 2006 初版 (Redis 最老) → 5 类型定型 → 3.2+ sdsResize → usable/SDS_NOINIT

**深审**: 编造 1 (sdshash 哈希缓存不存在 — dict 键哈希是 siphash 对内容); 精确化 1 (q4 readlen 16KB); 通过 2

**负面空间**: 不做惰性拷贝/内存池/自动压缩/编码检测/线程安全

### R-3 Dict — 哈希表

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **双表渐进**: ht_table[2]+rehashidx 逐桶; 空桶 n×10 上限; 完成=旧表释放+提升 | dict.h:96-102; dict.c:312-414 |
| 触发阈值: 1:1 扩 / <1:8 缩; AVOID: 4:1/1:32; **COW 三态** (fork→FORBID/子进程→AVOID/正常 ENABLE) | dict.c:1492-1550; server.c:640-652 |
| 联动迁移: 查找时目标桶未迁且非空 → _dictBucketRehash; 双表查找 | dict.c:736-770 |
| **2 幂掩码**: 扩重算 hash / **缩容 idx&新掩码免重算** | dict.c:320-327 |
| **单指针 entry**: 低 3 位编码; **sds 恒奇** (奇数头+malloc 对齐) | dict.c:128-171 |
| **dictScan 反向游标**: 弱语义 — 迭代前键不丢/重复允许/插入可漏 | dict.c:1369-1470 |
| SipHash: 16B 随机 seed; 2014 HashDoS 后引入 | dict.c:92-113; server.c:6985-6987 |

**时空溯源**: 早期双桶 → 2014 SipHash → 2013 dictScan 反转 → 7.x ht_size_exp/no_value/storedKey/COW 三态

**深审**: 语义修正 1 (**"不丢不重"错 — 实为弱语义**, harness 实证); 机制补强 1 (sds 恒奇闭环); 完整性 1 (pauserehash 冻结面); 表述 1 (桶空才直存); 二次 REVIEW: 前向依赖 1 (R-15 违拓扑 — 后全域修复此类)

**负面空间**: 不做完美哈希/并发安全/主动缩容/迭代器快照/内存池

### R-19 listpack — 紧凑列表

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 布局: 6B 头 + entry* + 0xFF EOF; entry=[编码+数据][backlen] | listpack.c:22-27,869-872 |
| **无级联 (核心卖点)**: backlen 存自身长度+固定 5B 空间; **ziplist 才有级联** (prevlen 1B/5B) | listpack.c:452-482; ziplist.c:55-69 |
| **编码族 9 种**: 整数 7/13/16/24/32/64BIT + 字符串 6/12/32BIT; 前缀 0/10/110/1110/1111 | listpack.c:30-82 |
| 整数嗅探: lpEncodeGetType→lpStringToInt64 (**L317 实测**, 非 660+) | listpack.c:154-179,317 |
| **lpInsert 三合一**: 删=替换零长; 扩先 realloc 后 memmove / 缩先 memmove 后 realloc (harness 实证); UINT32_MAX+1GB 安全线 | listpack.c:821-968 |
| 批量: lpBatchInsert 单次 realloc+memmove | listpack.c:993-1080 |
| 惰性计数: numele 超 65535 → 全扫回填; lpValidateIntegrity | listpack.c:27,505-521 |
| 消费阈值: hash 512/64B, zset 128/64B (config.c:3215-3223) | config.c:3215-3223 |

**时空溯源**: 2017 antirez 独立项目 → 7.x 全面替代 ziplist; lpStringToInt64 移植自 string2ll

**深审**: 认知修正 1 (**REDIS-PLAN"级联更新"错 — listpack 无级联**); harness 2 (缩容顺序); 行号 2 (L317/L29); 二次 REVIEW: 表述 1 (读者处境量级)

**负面空间**: 不做 O(1) 随机访问/级联安全/原地更新/压缩/嵌套

### R-7 intset — 整数集合

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| 编码分级: 2/4/8 字节宽; 值域 ±32767/±2^31/其他 | intset.c:41-53 |
| **升级**: intsetUpgradeAndAdd — 新值必在极值 (负数头插/正数尾插免二分); **从后往前搬防覆盖** | intset.c:159-182,214-216 |
| 二分查找 O(log n) + 首尾快速路径; MoveTail memmove | intset.c:117-156 |
| **升而不降**: 删除只缩元素数不缩编码 | intset.c:236-253 |
| 字节序: memrevXXifbe 统一小端 | intset.c:56-95 |
| 消费: set-max-intset-entries=512; setTypeAdd 双条件 | config.c:3216; t_set.c:26,42 |

**时空溯源**: 2009-2012 Pieter Noordhuis → 7.x intsetValidateIntegrity

**深审**: 行号 1 (struct intset 在 intset.h:35-39 非 13-17); 严谨性 1 (升而不降成本标注推断)

**负面空间**: 不做降级/字符串/插入 O(1)/压缩/缩容优化

### R-6 skiplist+ZSet — 有序集合

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **双结构**: zset{dict: member→score, zsl: 有序} 双写 — 单线程天然一致 | server.h:1357-1360; t_zset.c:1425-1530 |
| **层级概率 P=0.25**: 期望 1.33 层 (harness 实测 1.334); MAXLEVEL 32 = log_4(2^64) | t_zset.c:126-132; server.h:514-515 |
| **span 距离索引**: 排名 O(log n); 插入 rank 差 O(1)/层 | server.h:1345-1348; t_zset.c:171-183 |
| 复合排序: (score, ele) 全序 — 同分 sdscmp 字典序 | t_zset.c:147-150 |
| zslInsert: update[]+rank[] 双数组; 新层 span=length | t_zset.c:137-214 |
| **zslNthInRange 统一首/末/偏移** (7.x 替代 First/LastInRange — 已移除!); ZSKIPLIST_MAX_SEARCH=10 | t_zset.c:317-410; server.h:516 |
| 转换: listpack→skiplist **dictExpand 预扩** (L1292) + 创建路径预扩 (L1248) | t_zset.c:1265-1300 |

**时空溯源**: 早期无 span (排名 O(n)) → span 引入 → 7.x zslNthInRange

**深审**: 认知修正 1 (zslFirstInRange 已移除); harness 1 (层级实测); 行号 2 (NaN L1435 / dictExpand L1292); 精确化 1 (MAX_SEARCH=10)

**负面空间**: 不用红黑树/AVL; 不做降级/原地更新/P=0.5/跨 key 排序

### R-5 quicklist — 分页链表

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **分页链表**: quicklistNode (位域 count/encoding/container/recompress) + quicklist (fill/compress/bookmark_count) | quicklist.h:47-59,101-112 |
| **双容器**: PACKED (listpack) / **PLAIN (大元素裸节点)** — isLargeElement → PlainNode | quicklist.c:508-519,571-603 |
| **fill 双语义**: 正=元素数/负=字节数 (2^k 映射, **默认 -2=8KB**) | quicklist.c:462-482; config.c:3152 |
| **压缩三条件**: 两端 compress 深度外 + ≥48B (MIN_COMPRESS_BYTES) + 收益 ≥8B + lzf 成功; 默认 compress=0 | quicklist.c:78,83,214-252 |
| **recompress 延迟重压**: 读时解压+标记, 批量后统一重压 | quicklist.c:260-290 |
| 分裂: 复制整包 + lpDeleteRange 双侧裁剪 | quicklist.c:971-1004 |
| 三路路由: PLAIN/就地追加/新节点 (+相邻合并); SIZE_SAFETY_LIMIT=8192 | quicklist.c:521-603 |
| **list 当前仅 quicklist 一种编码** (7.x 无其他) | t_list.c:52 |

**时空溯源**: 3.2 前 linkedlist/ziplist 二选一 → 2014 Matt Stancliff → 2015+ LZF → 7.x listpack

**深审**: harness 1 (分裂测试对象错误); 精确化 2 (SIZE_SAFETY_LIMIT=8192 / Bookmark L79-87); 完整性 1 (list 唯一编码)

**负面空间**: 不做单节点无限增长/全量压缩/压缩保证/O(1) 随机访问/默认 bookmarks

### R-1 redisObject — 对象系统

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **16B 外壳**: type:4+encoding:4+lru:24+refcount+ptr | server.h:896,901-911 |
| **EMBSTR 同 chunk**: 16+3+44+1=64B 恰好 jemalloc 64B 桶; 不可变 (追加转 RAW) | object.c:71-107 |
| **INT 零分配**: ptr 存值; 共享整数 (<10000); maxmemory 禁共享 | object.c:128-140,627-635 |
| **优化链**: tryObjectEncodingEx — refcount>1 跳过 → INT (≤20 字符) → 共享/EMBSTR (≤44B) | object.c:607-683 |
| 引用计数三态: 1→分派释放 / >1→-- / 特殊值不碰 | object.c:56-60,349-377 |
| 共享池: shared.integers[10000] + 响应串族 | server.c:1847+,1992-1995 |
| lru 24bit 双用途: LRU 时钟/LFU 频率+时间 | object.c:685-715 |

**时空溯源**: 2010 初版 → 2013 EMBSTR (44B=64B arena) → LRU_BITS 22→24 → LFU

**深审**: harness 3 (测试逻辑/计数混计/EMBSTR 误 free); 行号 2 (共享 L636-643 / EMBSTR L657); 机制 1 (共享双路径); 二次 REVIEW: 精确化 1 (TYPE 在 db.c:1336)

**负面空间**: 不做可变 EMBSTR/共享修改/解码缓存/对象池/用户类型

### R-20 server 骨架 — 启动与周期引擎 (大域 2 篇)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **main 管线**: OOM handler → 哈希 seed → **哨兵先于配置** → 配置 → initServer → aeMain | server.c:6917-7256 |
| initServer 矩阵: 信号/事件循环/键空间 kvstore/共享池/cron 注册 | server.c:2591-2772 |
| **配置宏 DSL**: createIntConfig 族五合一; CONFIG SET 失败回滚 restoreBackupConfig | config.c:2244+,760-780 |
| **serverCron 时间分级**: 每 tick/100ms/1s/5s; 返回 1000/hz (**L1538 实测**) | server.c:1273-1540 |
| **hz 自适应**: 每 tick 重置 config_hz (**L1283**) → clients/hz>200 → ×2 上限 500 (harness: 10000 clients→80) | server.c:1282-1295 |
| 命令表: commands.def (11235 行/122 命令) → 双字典注册 (rename 免疫) | commands.c/def; server.c:3075-3095 |
| beforeSleep: AOF flush/待写/阻塞键; **cron=定时 vs beforeSleep=事件驱动** | server.c:1637-1800 |

**时空溯源**: 2009 骨架定型 → 配置族重构 → hz 自适应 → 7.x kvstore 键空间

**深审**: harness 1 (hz 回落模拟错 — 每 tick 重置语义); 行号 2 (L1283/L1538)

**负面空间**: 不做并行初始化/配置热加载/优雅降级/实时调度/多线程 cron

### R-21 db 键空间 — 键空间宿主 (大域 2 篇)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **lookupKey 副作用链**: 查找+过期拦截+LRU/LFU+统计+keymiss 通知; 五标志独立开关; slot 缓存免 CRC16 | db.c:75-127,217-219 |
| **惰性过期三态**: VALID/EXPIRED/DELETED; 从库只报不删; CLIENT_MASTER 豁免; FORCE/AVOID/PAUSE | db.c:1974-2017 (keyIsExpired L1928) |
| **三表一致**: dbAddInternal (sdsdup+信号)/dbSetValue (lru 继承+HFE 摘除)/dbGenericDelete (先 expires 后 keys) | db.c:180-195,256-289,372-425 |
| **expires 零拷贝**: 键复用主 dict sds + 整数联合值; dbExpiresDictType 双 destructor=NULL | db.c:1846-1863; server.c:501-508 |
| **kvstore 分片**: dict 数组 bits≤16; cluster 14bit=16384 槽 (crc16&0x3FFF+{tag}); 三创建路径 | kvstore.c:230-268; cluster.h:8-9,43-60; server.c:2667-2675 |
| **扫描/随机**: 游标 48+bits 复用弱语义; Fenwick 树 O(log n) 选桶/跳桶 (FAIR) | kvstore.c:102-117,361-403,431-434,500-538 |
| 增量 rehash: rehashing list + 1000us/tick + 16 dict/DB; 子进程跳过防 CoW | kvstore.c:621-661; server.h:105,128 |
| **ebuckets 时间桶**: list(≤16)→rax(6B)→segment(≤16); ExpireMeta 48bit; 指针 LSB 判 list/rax; 批量过期摊销 | ebuckets.c:62-63,1424-1549; ebuckets.h:142,161-211 |
| **HFE 两级**: db->hexpires 全局代理 (早到字段) + hash 私有 hfe | t_hash.c:110-130,2073-2094 |
| lazyfree: effort>64 且 refcount=1 → bio 异步; FLUSHDB ASYNC 换表法 | lazyfree.c:129-215 |
| 命令面: SCAN 四步/FAIR+maxtries=100/SWAPDB 指针互换/getKeys 三代 | db.c:1049-1319,336-369,1712-1755,2133-2442 |

**时空溯源**: 2009 初版 → 2011 kvstore (cluster 槽) → 7.0 全面接管 → 2024 ebuckets (HFE)

**深审**: 行号 2 (kvstoreCreate L2667-2675 / hashTypeDbActiveExpire L2073); **harness 实证: expires 共享键删除必须不释放 (destructor=NULL)** — ASan use-after-free 复现; 表述 1 (TYPE 过滤 Step3); **二次 REVIEW 10 处** (前向依赖 R-15/覆盖缺口 2/编造 ebGetNextTimeToExpire 消费端/位域/区间/注册路径)

**负面空间**: 不做自动过期扫描/键级锁/独立键对象/物理删除确认/事务隔离; 不做 dict 数组伸缩/时间桶精度提升 (PRECISION=0 TBD)/ebRemove 合并

### R-22 过期机制 — 主动回收与命令面

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **SLOW/FAST 双循环**: SLOW 25% CPU 预算 (25ms@hz=10); FAST 1000us + 双条件拒跑 (上次未超时且 stale 低/2ms 冷却); **均需 iAmMaster** | expire.c:92-96,218-231,247-252; server.c:1059,1687-1690 |
| effort 缩放: 1-10 → 20 键+5/档, 1000us+250, 25%+2, 10%-1 | expire.c:191-200 |
| **采样驱动**: 20 键×20 桶双限; expires_cursor 持久游标; repeat=过期比例>10%; 填充率<1% 跳过 | expire.c:313-348,127-137 |
| 统计: avg_ttl 指数滑动 (pow(0.98) 16 项表); stale_perc 5%/95% = FAST 信号 | expire.c:24,353-407 |
| **HFE 配额**: 10000/hz → 1000 字段/次; 积压>100 万 ×32 封顶 | expire.c:98,144-185 |
| **可写从库记账**: slaveKeysWithExpire 位图 dict (DB>63 折衷); 停止: 3 连拒/64 循环 1ms/表空 | expire.c:410-560 |
| **EXPIRE 族**: 四命令归一; NX/XX/GT/LT (无 TTL 视为无限); checkAlreadyExpired → 删+重写 DEL/UNLINK (**三种格式**); 正常重写 PEXPIREAT | expire.c:562-750 |
| TTL 三值: -2/-1/剩余; 四舍五入 (ttl+500)/1000; commandTimeSnapshot 冻结 (#1525) | expire.c:773-838; server.c:221 |

**时空溯源**: 2009 → 3.2 可写从库 → 4.0 effort → 5.x 游标 → 2024 HFE

**深审**: 表述 2 (超时传递方向/avg_ttl 变量遮蔽); 行号验证 96 处; **二次 REVIEW 5 处** (FAST iAmMaster 条件/notify 缺口/传播三格式/TOUCH 返回计数/溢出守卫表述)

**负面空间**: 不做实时时钟同步/精确到期扫描/从库主动删主库键/DB>63 记账

### R-23 内存淘汰 — maxmemory 八策略

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **三态驱动**: EVICT_OK/RUNNING/FAIL; FAIL+**CMD_DENYOOM 标志** → rejectCommand oomerr (is_denyoom_command L3958); RUNNING 由 aeTimeProc 续清 | server.c:4036-4059; evict.c:520 |
| **overhead 剔除**: AOF buf + repl 超出 backlog 部分不计 (防 DEL 反馈环) | evict.c:310-353 |
| **跨 DB 采样池**: EVPOOL_SIZE=16 + 5 采样/DB; idle 三打分 (LRU 空闲ms/LFU 255-计数/TTL ULLONG_MAX-TTL); cached sds 255B 复用; 幽灵键跳过 | evict.c:33-43,125-225,568-630 |
| **LRU 近似**: 24bit×1000ms 时钟 (~194 天回绕); hz≥1 缓存 | evict.c:52-100; server.h:896-898 |
| **LFU**: 16bit 分钟 + 8bit 对数计数 (p=1/(base×factor+1), INIT_VAL=5); 惰性衰减 | evict.c:230-308; server.h:3537 |
| **八策略位域**: 高 8 位 id + 低 3 位标志; ALLKEYS→keys 否则 expires; 三路径 (池/随机/拒绝) | server.h:556-569; evict.c:538,564-566 |
| **tenacity 执行**: ≤10 线性 50us×t / <100 几何 500×1.15^(t-10) / =100 无限; delta 实测; 每 16 键三检查 | evict.c:479-494,692-720 |

**时空溯源**: 2009 → 3.0 LFU → 4.0 lazyfree → 7.0 tenacity+三态+kvstore 适配

**深审**: 行号 1 (rejectCommand L4050/pre_command_oom_state L4059); 行号验证 130 处; **二次 REVIEW 2 处** (CMD_DENYOOM 非"只读"/统计面 stat_evictedkeys)

**负面空间**: 不做全局精确 LRU/逐键评估/预算动态调优/大对象优先/淘汰排序保证

### R-2 事件驱动+IO 多线程 — 事件循环 (大域 2 篇)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **事件表**: fd 直接索引双数组 O(1); mask 合并; 删 WRITABLE 连带删 BARRIER | ae.h:20-27,78-90; ae.c:143-183 |
| **睡眠编排**: 三等待 (DONT_WAIT/睡到最早时间事件 O(N)/无限); beforesleep→poll→aftersleep 三明治 | ae.c:245-258,342-389 |
| **分派顺序**: 读先写后; **AE_BARRIER 逆序** (AOF fsync 后回复); 同 proc 去重; 回调可改事件表 | ae.c:391-443 |
| **时间事件**: 无序链表 O(N) (权衡注释); 惰性删除; **双重防重入** (maxId/refcount); 周期重排 | ae.c:200-325 |
| **多路复用**: 编译期特化 (evport→epoll→kqueue→select); **ERR/HUP 双触发**; ADD/MOD/DEL | ae.c:29-43; ae_epoll.c:18-118 |
| **io threads 生命周期**: 1-128 默认 1 IMMUTABLE; **互斥锁即启停栅栏**; 惰性停 pending<num×2; cache-line 对齐 | networking.c:4215-4384 |
| **写扇出扇入**: 分发 % num; **从库强制 list[0]** (共享 repl 缓冲); pending 归零唯一通信 | networking.c:4393-4484 |
| **读扇出扇入**: postpone 五条件; 线程只读+parse; **IO 可并行执行仍单线程** | networking.c:2662,4491-4560 |

**时空溯源**: 2006 Jim Tcl → 2009 epoll → 6.0 io threads → 6.2+ do-reads/对齐

**深审**: 行号 4 (L4295/4347/4354/4518); harness 2 轮 (顺序日志强化/maxId 时间设定); 行号验证 100 处; **二次 REVIEW 4 处** (FAST iAmMaster 条件/notify/evictionTimeProc 连接/installClientWriteHandler L4474)

**负面空间**: 不做事件优先级/定时器红黑树/跨线程事件投递/ET 模式/命令并行/线程动态扩缩

### R-28 networking — 协议与客户端 IO (大域 2 篇)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **读路径**: readlen 四决策 (16KB/大参数精确读/MASTER 回升/avail); NonGreedy vs Greedy; querybuf 上限 (1GB/未认证 1MB) | networking.c:2655-2764 |
| **解析循环**: qb_pos 滑窗; 四提前退出; reqtype 首字节; io 线程 PENDING_COMMAND; trim (普通/master repl_applied) | networking.c:2559-2653 |
| **RESP 三行状态机**: * 计数 → argv 起步 1024 2× 增长 → $ 长度 (512MB/未认证 16KB) → data; **大参数零拷贝** (预对齐+整包借用) | networking.c:2292-2452 |
| **协议安全**: 未认证三级限 (10 参数/16KB/1MB); 协议错误即断连 | networking.c:103-111,2252-2291 |
| **输出双缓冲**: 静态 16KB 优先, **链表出现后静态退休**; 节点 ≥16KB 尾续写; 超限断连 | networking.c:278-375 |
| **写路径**: writev 批量 (静态+链表拼 iov, ≤64KB/轮); **从库 replBufBlock 共享零复制**; sentlen 部分写 | networking.c:1844-2062 |
| **生命周期**: createClient 双态 + 读事件注册; freeClient 释放链 (master 缓存/从库杀 RDB/async 队列) | networking.c:112-211,1578-1810 |

**时空溯源**: 2009 → 双缓冲 → writev → 6.0 共享缓冲/io threads → 7.x 零拷贝/未认证限流

**深审**: harness 3 轮 (长度硬编码 SEGV/分片数据/零拷贝构造); 行号验证 116 处; **二次 REVIEW 2 处** (R-9 前向依赖/"六拒"实为五拒+挂队列)

**负面空间**: 不做流式解析/协议协商/未认证队列/零拷贝常态/错误重试/输出压缩/背压调度/从库私有缓冲

### R-24 t_string — 字符串命令

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **SET 标志矩阵**: 9 位域 (NX/XX/EX/PX/KEEPTTL/GET/EXAT/PXAT/PERSIST); 解析期互斥 (互斥时报 syntaxerr); 命令族分域 | t_string.c:49-58,188-270 |
| **SET 传播归一**: expire → 重写 SET...PXAT 绝对毫秒; GET 剥离; GETSET 重写 SET | t_string.c:63-129,410-419 |
| **INCR 原地优化**: 溢出双符号检查; **四条件原地** (refcount==1+INT+非共享+LONG → ptr 直改零分配); DECRBY LLONG_MIN 特判; INCRBYFLOAT (NaN/Inf 拒 + 重写 SET KEEPTTL) | t_string.c:580-674 |
| 范围命令: checkStringLength (512MB+uint64 溢出检测); SETRANGE **sdsgrowzero 零填充** (非 sdsnewlen); GETRANGE INT 栈 buf+负索引 | t_string.c:19-31,421-520 |
| **GETEX 读改写三路径**: 已过期删 (DEL/UNLINK)/设置 (PEXPIREAT)/移除 (PERSIST) | t_string.c:340-408 |
| **LCS 三层防护**: UINT32_MAX-1 → 表乘法双重检查 → 512MB → ztrymalloc | t_string.c:716-929 |

**时空溯源**: 2009 → 2.6 SETNX/GETRANGE → 4.0 SET 选项 → 6.0 LCS/GETEX → 6.2+ 统一解析器

**深审**: 行号验证 117 处; **二次 REVIEW 2 处** (syntaxerr 非静默拒/零填充归 sdsgrowzero)

**负面空间**: 不做字符串截断/INCR 浮点落库/GETRANGE 惰性/SET 返回值缓存/LCS 优化变体

### R-25 t_hash — 哈希命令与 HFE (大域 2 篇)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **编码三态**: LISTPACK (两元素组)/LISTPACK_EX (三元素组+TTL)/HT (hfield 键); 阈值 512/64; 升级单向 | t_hash.c:74-130; config.c:3215,3221 |
| **hashTypeSet 三分支**: lpReplace/lpAppend; KEEP_TTL/TAKE_VALUE 标志; dictUseStoredKeyApi (R-3) | t_hash.c:855-977 |
| **转换**: 三触发 (字段数+预扩/单值/lpSafeToAdd); **HFE 三阶段迁移** (全局摘→trash=1→私有重建→全局重注) | t_hash.c:594-623,1553-1679 |
| **GETF 惰性链**: 四态含 **EXPIRED_HASH** (读字段把 hash 读没了 — 级联删键); 从库只报; HDEL 传播+hexpired 通知 | t_hash.c:32-35,711-779 |
| **条件 TTL**: 三阶段框架 (Init/SetEx/Done 批量聚合); GT/LT/NX/XX (无 TTL 视为无限 — R-22 同语义); 已过期删字段 | t_hash.c:979-1238 |
| **HFE 命令族**: hexpire 批量/httl 三值/hpersist; hfield mstr 奇数地址 (itemsAddrAreOdd=1) | t_hash.c:2837-3294 |
| **两级注册**: 全局 db->hexpires (早到代理) + 私有 hfe; AddToExpires 唯一入口; R-22 消费 (10000/秒) | t_hash.c:115-130,1996-2094 |

**时空溯源**: 2009 → 2.6 HT → 7.0 listpack+storedKey → 7.4 HFE (LISTPACK_EX/trash 状态机)

**深审**: harness 3 轮 (Field 未初始化 BUS/EXPIRED vs EXPIRED_HASH 断言/SetEx 构造冲突); 行号验证 129 处; **二次 REVIEW 3 处** (GETF 行号/HFE 常量 L197-200/hrandfield 加权 L1789)

**负面空间**: 不做编码降级/字段排序/HGETALL 分页/字段级精确扫描/HFE 降级/HEXPIRE 单位混用

### R-26 t_list+blocked — 列表与阻塞框架 (大域 2 篇)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **list 双编码**: listpack → quicklist (GROWING/SHRINKING 分派 + beforeConvertCB); list-max-listpack-size=-2 | t_list.c:21-127; config.c:3152 |
| **🔴 唤醒只需 dbAdd 路径**: t_list.c 零 signalKeyAsReady 调用 — **空键不存在不变量** (pop 空即删键 L748-750) → 阻塞者等待键必然不存在 → push 走 db.c:192; stream 例外 (t_stream.c:2083 显式) | t_list.c:464-492; db.c:192 |
| pop: 单/COUNT 范围; 空键删键+signalDeletedKeyAsReady; mpop 多键+传播重写 | t_list.c:736-845 |
| **阻塞状态机**: btype 10 种; blockClient (CLIENT_BLOCKED L75+by_type 计数 L78+timeout 表 L79); master 不可阻; 超时 null 回复 | server.h:398-407; blocked.c:54-210,700-708 |
| **blockForKeys 双向注册**: bstate.keys ↔ blocking_keys **互链 (list node 作 value → O(1) 解链)**; unblock_on_nokey 引用计数; PENDING_COMMAND | blocked.c:359-410,507-540 |
| **就绪队列三级快检**: 类型可阻塞/无该类型阻塞者 (by_type O(1))/键无等待者; **ready_keys dict 防重** (脚本多 push 一次唤醒) | blocked.c:430-494 |
| **消费**: 防递归+**新列表交换** (BLMOVE 连环唤醒)+FIFO+**类型匹配防误醒**+PENDING_COMMAND 重执行 | blocked.c:306-350,553-670 |

**时空溯源**: 2009 BLPOP → 2.8 双向注册 → 3.2 quicklist → 6.0 LMOVE/类型扩展

**深审**: **机制洞察: 唤醒只需 dbAdd** (grep 实证 t_list.c 无 signalKeyAsReady); harness 5 轮 (栈对象误 free BUS/泄漏 3 轮/消费 FIFO 语义); 行号验证 117 处; **二次 REVIEW 2 处** (lmove L1151/blmoveGenericCommand 先试后阻样板 L1265-1280/blockClient 行号偏移)

**负面空间**: 不做双向索引/消息确认/阻塞优先级/多键部分唤醒/超时精确性/跨 DB 阻塞

### R-27 t_set — 集合命令

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **🔴 三编码 (非 PLAN 双编码)**: intset (≤512 整数)/**listpack (≤128/64B — 7.x 新增)**/HT (预扩); set-max-listpack-entries=128 ≠ hash 512; intset 1<<30 上限 | t_set.c:25-54; config.c:3216-3218 |
| **🔴 双向转换 (Redis 唯一支持降级)**: intset 非整数 → listpack 中间态或 HT; **HT→intset** (sinterstore 全整数 L1392) — 对照 hash/list 单向 | t_set.c:57-88,169-202,1392 |
| 写入: **dictFindPositionForInsert 预定位** (一次哈希); lpAppendInteger 整数直插 | t_set.c:104-208 |
| **集合运算**: sinter 空集短路+**最小集 qsort** (O(min×others)); SINTERCARD 基数+LIMIT | t_set.c:1229-1460 |
| **随机**: **SPOP COUNT 仅正数** (getPositiveLong L742, 负值报错); count>=size 全删+DEL 重写; MUL=5 双策略; **SRANDMEMBER 才有正负** (正=uniq 不重复/负=可重复) | t_set.c:407-457,737-809,998-1202 |

**时空溯源**: 2009 双编码 → 2.8 COUNT/最小集 → 7.x listpack+降级+预定位

**深审**: 机制发现 1 (三编码+双向); 行号验证 82 处; **二次 REVIEW 1 处 (重要)**: "SPOP COUNT 正负" 完全说反 — SPOP 仅正数, 正负语义属 SRANDMEMBER 且方向相反

**负面空间**: 不做有序集合/成员级 TTL/集合引用计数/流式运算/SPOP 保序

### R-11 Bitmap — 位操作

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **redisPopcount**: 256 查表 + **SWAR 28 字节批** (0x55555555→0x33333333→0x0F0F0F0F×0x01010101 三阶段); 对齐前缀+尾查表; 上限 512MB | bitops.c:19-71 |
| **redisBitpos**: 字对齐跳过 (找 0 跳全 1/找 1 跳全 0 — O(段数)); 尾字大端组装+MSB 扫描; **特殊值: 全 0 找 1 → -1 / 找 0 恒成功 (零填充假设)** | bitops.c:80-165 |
| **BITFIELD**: iN/uN ≤64; 掩码存取+符号扩展; **溢出三模式** (WRAP 截断/SAT 钳制/FAIL); 64 位特判 (L268/L305 防 UB) | bitops.c:188-360,1032-1262 |
| **SETBIT/GETBIT**: MSB 优先 (byte>>3 L535 / bit=7-(off&7) L537); **dirty 三条件** (值相同零写); 扩容零填充 (sdsnewlen L473/sdsgrowzero L486); 返回旧值 | bitops.c:460-491,511-585 |
| **BITOP**: 四运算+NOT 单键 (L611-612); **maxlen 语义** (短键零填充); **字宽批量** (L672+: 每轮 4×ulong — 非零字节短路!) | bitops.c:586-774 |
| **BITCOUNT**: start/end+BIT/BYTE 单位; **首尾掩码** (声明 L782, 免子串拷贝); popcount L852 | bitops.c:775-1031 |

**时空溯源**: 2009 查表 → 2.6 BITOP → 2.8 BITPOS → 3.2 BITFIELD → 4.0+ 范围单位

**深审**: 行号偏移 1 (位定位 L535-537); 行号验证 56 处; **二次 REVIEW 3 处**: **编造 "AND 零字节短路"** (实为字宽批量 L672+ 无短路)/UINT64_MAX L268 与 INT64_MAX L305 偏移/扩容精确化 (sdsgrowzero L486)

**负面空间**: 不做位压缩 (RLE, 对照 R-12 稀疏)/跨键视图/64+ 位宽/自动紧凑/原子多字段

### R-12 HyperLogLog — 概率基数估计

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **哈希分域**: MurmurHash64A (字节序无关改造) **固定 seed 0xadc83b19 无随机化** (对照 R-3 SipHash); 低 14 位定寄存器 + 高 50 位前导零计数 (1..51) | hyperloglog.c:376-459 |
| **稠密 6bit 打包**: 16384×6bit=12288B+16B 头=12304B; LSB-first 物理位序; sds 尾零免费末位越界字节; 直方图 16 寄存器/轮×1024 全展开 | L318-340,499-554 |
| **稀疏三 opcode**: ZERO (1B, 1-64)/XZERO (2B, 1-16384)/VAL (1B, 值 1-32×长 1-4); **空 HLL=18B**; 更新 A/B/C/D 四分支+分裂 seq[5]+相邻合并; 纯位置游程 | L344-367,634-886 |
| **升稠不降稀**: 两触发 (count>32 / 超 hll_sparse_max_bytes=3000); 单向对照 R-7 | L642,822; config.c:3224 |
| **⚠ Ertl 估计器 (认知修正)**: 非经典 Σ2^-M 调和平均 — sigma/tau (arXiv:1702.01284), z=m·tau((m-h[51])/m)+递推折叠+m·sigma(h[0]/m), E=llroundl(alpha_inf·m²/z); alpha_inf=1/(2·ln2); 0.8125%=1.04/√16384 | L962-991,1030-1038,1432 |
| **基数缓存**: card[8] LE + card[7] MSB 有效位; PFCOUNT 命中零计算; **只读命令改值** (commands.def 唯一 CMD_READONLY\|CMD_MAY_REPLICATE) + 回写传播 | L161-171,1211-1304 |
| **多键 RAW 归并**: 栈上 16400B max 数组 + HLL_RAW 内部编码; PFMERGE use_dense 跟随输入; 校验头浅检+使用深检 | L1059-1099,1220-1246,1307-1378 |

**时空溯源**: 2014 (2.8.9) 初版三命令 → 稀疏表示 → 4.0 Ertl 估计器 (2017 论文) → 7.x hll-sparse-max-bytes 动态化

**深审**: **认知修正 1 (Ertl 估计器替代经典调和平均 — 交接文档 §四 表述过时)**; 精确化 2 (12KB 仅稠密, 稀疏最小 18B / opcode 字节 1/2/1); 强断言实证 1 (唯一 READONLY+MAY_REPLICATE 组合); 缓存一致性闭环 1 (promote 复制旧头→三处命令层失活全覆盖); 行号 ~40 处穷举 (HLL_REGISTERS L176 预验正确); **二次 REVIEW 2 处**

**负面空间**: 不做精确计数 (0.81% 近似, 对照 R-11)/稠密降级/哈希随机化/删除减法/寄存器扩容/独立类型 (RAW 字符串+magic 头)

### R-13 GEO — 空间索引 (52bit 编码 + ZSet 载体)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **52bit 编码**: GEO_STEP_MAX=26; 范围 ±180/±85.05112878 (EPSG:900913 墨卡托); 归一→定点×2^26→位交错; **lat 偶数位/lon 奇数位 (与标准 geohash 相反, 自洽)** | geohash.h:46-52; geohash.c:121-151,52-77 |
| **GEOADD = ZADD 包装**: 编码→score→replaceClientCommandVector+zaddCommand; NX/XX/CH 透传; 零新类型 | geo.c:445-503 |
| **半径三步**: EstimateStepsByRadius (对折 MERCATOR_MAX=20037726.37 + 高纬修正 + clamp 1..26) → BoundingBox (cos 纬线修正 + 南半球反转) → 9 盒扫描 (decrease_step 复核 + GZERO 剔除 + 大半径去重) | geohash_helper.c:62-83,98-116,121-211; geo.c:365-421 |
| **两段式过滤**: 前缀范围 [align(h), align(h+1)) 粗筛 (listpack zzlFirstInRange t_zset.c:920 / skiplist zslNthInRange) → haversine 精筛 (v==0 同经度捷径; 矩形先纬后经); **qsort 全量 vs pqsort 部分**; COUNT 强制 ASC | geo.c:261-323,740-754; geohash_helper.c:224-280 |
| **解码面**: 固定 step=26 解码取格中心; GEOPOS/GEODIST 直出; **GEOHASH 重编码标准 -90/90** (内部 -85/85 差异) + 11 字符 base32 (55bit 槽>52bit → 第 11 字符恒 '0') | geo.c:92-95,878-1005 |
| **命令族**: georadiusGeneric 五 flags (COORDS/MEMBER/NOSTORE/GEOSEARCH/GEOSEARCHSTORE); GEOSEARCH 6.2 FROMMEMBER\|FROMLONLAT+BYRADIUS\|BYBOX; GEORADIUS 族 deprecated; STORE 手工建 zset (zslInsert+dictAdd→setKey) | geo.c:509-513,523-712,803-872; commands.def:11019-11028 |

**时空溯源**: 2013-2014 yinqiwen ardb 项目 → 2014 Matt Stancliff 移植 (geohash_helper 为 C++→C 转换) → 3.2.0 首版六命令 → 3.2.10 只读变体 → 6.2.0 GEOSEARCH 上位 (GEORADIUS deprecated)

**深审**: 预判 3 项全验证 (52bit L46 / ZADD 包装 / haversine); **机制洞察 1 (奇偶位与标准反向)**; 精确化 1 (第 11 字符恒 '0'); 行号 ~40 处穷举; **二次 REVIEW 2 处 + 三次深度 REVIEW 4 处** (COUNT-ANY 条件/负面空间"精筛不补漏"/move_x 命名轴澄清/base32 推断标注; 推理验证 6 项全通过)

**负面空间**: 不做投影转换 (球面精度靠 cos/haversine 缓解)/动态降精度 (score 恒 52bit)/跨 key 索引/距离排序缓存/地理单元删除/9 盒覆盖保证 (精筛兜底)

### R-29 Pub/Sub + Keyspace 通知 — 发布订阅与事件总线

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **双订阅面**: client 三 dict (channels/patterns/shard, 初始化 networking.c:188-190) ↔ server 三镜像 (kvstore pubsub_channels / dict pubsub_patterns / kvstore shard 按 slot); **pubsubtype 抽象** (4 函数指针参数化全局/分片, 7.0 shard 零重复) | server.h:1222-1224,1989-1994; pubsub.c:14-75 |
| **订阅/退订**: dictFindPositionForInsert 预定位; 双向注册+双引用 (incrRefCount×2); **空频道即删** (防 millions of channels 滥用 L294-299); 批量退订安全迭代器+零订阅回 null; freeClient 自动退订 | pubsub.c:238-311,340-448; networking.c:1548-1550 |
| **分发两路**: 频道 kvstore O(1) 直发 + 模式 stringmatchlen glob 全量扫 (shard 跳过模式 L477-480); receivers=客户端数; **传播双语义**: 非 cluster → forceCommandPropagation PROPAGATE_REPL 复制从库 (从库二次分发), cluster → gossip | pubsub.c:453-508,590-608; util.c:193 |
| **双协议格式**: RESP2 mbulkhdr[3/4] / RESP3 addReplyPushLen + CLIENT_PUSHING (1ULL<<46); 9 个共享协议串; **RESP2 订阅白名单** (仅 ping/订阅族/quit/reset, RESP3 无限制 L4112-4125); ping 订阅态 ["pong",msg] | pubsub.c:86-182; server.c:1932-1940,4112-4125,4596 |
| **Keyspace 通知**: 15 类位掩码 (K/E/g/$/l/s/h/z/x/e/t/m/d/n+LOADED); **NOTIFY_ALL 仅 10 类不含 K/E/m** (L656 注释); module 旁路绕过配置; **双频道 __keyspace@db:key (msg=事件名) / __keyevent@db:event (msg=key)**; 默认全关 (server.c:2081) | server.h:641-656; notify.c:19-44,83-124 |
| **命令面+资源**: 订阅族 2.0.0/PUBSUB 2.8.0/shard 族 7.0.0; PUBSUB 5 子命令 (SHARDNUMSUB 按 slot); **输出缓冲 pubsub 类 {32MB,8MB,60s}** 慢消费者保护; pubsubMemOverhead 三 dict | commands.def; pubsub.c:611-748; config.c:152 |

**时空溯源**: 2009 初版 (pubsub.c 版权) → 2.0 六命令 → 2.8 PUBSUB+keyspace 通知 → 3.x 传播语义定型 → 6.0 RESP3 push → 7.0 shard pubsub+kvstore 分片

**深审**: 预判 4 项全验证 (双镜像/消费链 15 文件/慢消费者/从库语义); **机制洞察 2 (pubsubtype 抽象 / PUBLISH 传播双语义)**; 精确化 1 (**A 掩码 10 类非全量**); 行号 ~60 处穷举; **二次 REVIEW 2 处 + 三次深度 REVIEW 5 处** (A 掩码漏 n/LOADED、映射 15 字符、receivers 投递数、nocase=0、kvstore bits=0; 推理验证 8 项全通过)

**负面空间**: 不做消息持久化/积压 (对照 R-10 Stream 有 PEL)/确认重投/订阅读者组/模式订阅计数 (NUMSUB 不含)/订阅持久化/DB 命名空间频道

### R-16 事务 — MULTI/EXEC/WATCH (命令队列 + CAS 观察者)

**核心机制**

| 机制 | 锚点 |
|:--|:--|
| **命令队列**: multiState (cmd_flags/cmd_inv_flags 双累计, server.h:1005-1016) + multiCmd; **argv 所有权转移零拷贝** (入队后 c->argv=NULL); 预分配 2 + 倍增; **DIRTY 冻结入队** (注定失败不浪费内存) | multi.c:39-75; server.h:998-1016 |
| **入队错误延迟爆发**: rejectCommand → flagTransaction (DIRTY_EXEC); **CMD_NO_MULTI 仅 4 命令** (psync/save/shutdown/sync); EXEC 被拒 → execCommandAbort | server.c:3757-3785,3979-3981; multi.c:86-89 |
| **EXEC 双失败路径**: DIRTY_EXEC → EXECABORT 错误 / 仅 DIRTY_CAS → **nullarray** ("technically not an error"); 执行: DENY_BLOCKING + in_exec + **ACL 复查** (入队后变更) + AOF 客户端 CMD_CALL_NONE + **mstate 回写** (命令可改 argv) | multi.c:127-235,115-125 |
| **⚠ 传播修正**: EXEC **逐命令独立传播** (无包裹); MULTI...EXEC 包裹仅 also_propagate 批量 (numops>1, 脚本/模块), CMD_TOUCHES_ARBITRARY_KEYS 例外, dbid=-1 不传 SELECT | server.c:3395-3427 |
| **WATCH 嵌入式节点**: watchedKey 内嵌 listNode + node.value 指回客户端列表 — **双列表 O(1) 增删** (免 listSearchKey/dictFind); touch → DIRTY_CAS + **立即退订** (省内存); **expired:1 位域** (WATCH 时已过期语义) | multi.c:246-310,359-398 |
| **失效传播**: signalModifiedKey = touchWatchedKey + trackingInvalidateKey 双失效 (db.c:620-623); FLUSHDB/SWAPDB/SELECT → touchAllWatchedKeysInDb (**迭代中不可退订 UAF 防护**) | multi.c:407-450; db.c:626-637,1721-1722,1767 |
| 命令面: MULTI/EXEC 1.2.0 / DISCARD 2.0.0 / WATCH/UNWATCH 2.2.0; WATCH in MULTI 拒; UNWATCH 清 DIRTY_CAS (CAS 可逆); multiStateMemOverhead 三构成 | commands.def; multi.c:77-108,452-482 |

**时空溯源**: 1.2.0 MULTI/EXEC → 2.0 DISCARD → 2.2 WATCH (CAS) → 7.x 嵌入式 watchedKey 重构 + isWatchedKeyExpired (HFE 时代)

**深审**: **认知修正 1 (传播语义: 逐条非包裹, 包裹仅 also_propagate 批量)**; 机制洞察 3 (argv 零拷贝转移/嵌入式节点 O(1)/UAF 防护取舍); 行号 ~30 处穷举 (CMD_NO_MULTI 4 命令 grep 实证); **二次 REVIEW 1 处记录 + 三次深度 REVIEW 3 处** (expired 键重建 break 边界语义存疑/两种 EXECABORT 来源/CMD_TOUCHES_ARBITRARY_KEYS 2 命令; 推理验证 8 项全通过)

**负面空间**: 不做回滚 (执行期错误不撤销)/隔离 (WATCH 是唯一并发防护, 乐观锁)/嵌套事务/MVCC/EXEC 内中断恢复/多 DB WATCH

### R-8 RDB+AOF — 持久化 (大域拆 2 篇: R-8a RDB / R-8b AOF)

**核心机制 (R-8a RDB)**

| 机制 | 锚点 |
|:--|:--|
| **长度四档** 6bit(1B)/14bit(2B)/32bit(5B)/64bit(9B) + **整数编码** INT8/16/32 (2/3/5B); ENCVAL 长度兼编码类型前缀 | rdb.c:151-238,258-280; rdb.h:36-49 |
| **rio 流抽象**: buffer/file/conn 三实现 (函数表注入); autosync 增量写分批 (4MB); crc64 checksum 钩子 | rio.c:62-200,436,448; server.h:170 |
| **26 类型分派**: (type,encoding) 双键 → RDB_TYPE (类型号 0-7∪9-25 无 4/8); listpack/intset 整包直存; stream PEL/消费组深度序列化 | rdb.c:671-716,829+; rdb.h:54-83 |
| **fork COW 快照**: bgsave fork 子进程写共享内存; temp-pid.rdb → **rename 原子替换** + fsyncFileDir; autoSync+reclaimFilePageCache | rdb.c:1522-1670 |
| **文件布局**: magic "REDIS0012" (RDB_VERSION=12) + AUX (%-INFO/repl-*) + FUNCTIONS + SELECTDB/RESIZEDB/SLOT_INFO + EOF + CRC64 (可关); 无盘复制 $EOF:mark 包裹 | rdb.c:1452-1520; rdb.h:20 |
| **加载容错**: opcode 分派 + 版本门 1..12 + **v9 字节序修复** (仅 ≥v9, 大端老文件兼容) + rdbReportError 三分支 (RESTORE 报错/rdbCheckMode/加载即终) | rdb.c:46-87,3328-3712 |

**核心机制 (R-8b AOF)**

| 机制 | 锚点 |
|:--|:--|
| **三策略** always/everysec(默认)/no; 写前缓冲 aof_buf + beforeSleep flush (回复前落盘) | config.c:77-79,3134; aof.c:1340-1344 |
| **flush 细节**: 空缓冲补偿 fsync / everysec 延期 ≤2s (aof_delayed_fsync) / **短写 ftruncate 截断修复** | aof.c:1045-1183 |
| **feed 序列化**: SELECT 注入 (aof_selected_db 缓存) + RESP 重写; AOF 与复制同源传播 | aof.c:1308-1347 |
| **重写**: 命令级重建 (SET/RPUSH 变参 **64/批**/SADD/ZADD/HSET/XADD) + PEXPIREAT + **混合持久化 RDB preamble** (4.0) + temp→rename | aof.c:2249-2418; server.h:112 |
| **重写生命周期**: fork + 父进程切新 INCR + **⚠ 7.0 文件级分离 (无 diff 管道)** + 完成双 rename (temp BASE→新 BASE + 临时 INCR→新 INCR) + 旧标 HISTORY + manifest 提正 | aof.c:128-214,2453,2565+ |
| **加载**: 假客户端重放 (CLIENT_ID_AOF+DENY_BLOCKING) 借 processCommand 全链; RDB preamble 检测; **截断容忍 AOF_TRUNCATED** | aof.c:1355-1437,1637 |

**时空溯源**: 2009 RDB → 1.2 AOF → 2.2 bio → 3.2 变参批量 → **4.0 混合持久化** → **5.0 v9 字节序修复** → **7.0 Multi-Part manifest** → 7.4 HFE 类型

**深审**: 行号修正 1 (**RDB_VERSION=12** 非 11); 常量 2 (AUTOSYNC 4MB/批量 64); 机制洞察 2 (v9 兼容策略/fork 三流并行); **harness 156 断言** (四档长度/整数/double 含 NaN/时间戳 v8v9/序列化/批量 79 条, ASan clean); **二次 REVIEW 1 处 + 三次深度 REVIEW 3 处** (**认知修正: 重写 diff 管道 7.0 移除改文件级分离**/REDIS0011 漏改/AOF_TRUNCATED 仅最后文件容忍; 推理验证 8 项全通过)

**负面空间**: RDB 无增量/并发/在线校验; AOF 无命令压缩/对象级增量/fsync 保证/加载校验和

### R-9 复制 — 主从同步 (大域拆 2 篇: R-9a 全量 / R-9b 增量)

**核心机制 (R-9a 全量同步)**

| 机制 | 锚点 |
|:--|:--|
| **握手状态机**: CONNECTING→PING→AUTH→REPLCONF listening-port/ip-address/**capa eof psync2**→PSYNC; 无 auth 跳步; 老主库忽略未知选项 | replication.c:2608-2809 |
| **PSYNC 裁决**: replid 双 ID (replid2 仅到 second_replid_offset) + **backlog 范围 [offset, offset+histlen]**; "?" 强制全量 | L718-814 |
| **FULLRESYNC 延迟应答**: +FULLRESYNC replid offset 延迟到 RDB 就绪 (offset=RDB 生成时刻) — 全量+增量无缝衔接 | L689,808-813,1016-1039 |
| **RDB 双模式传输**: 磁盘 sendBulkToSlave 流式 / **无盘 rdbPipeReadHandler 管道扇出** (多从库共享一次 bgsave); RDB-FILTER-ONLY functions 过滤 | L834,1385,1487,1229-1255 |
| **从库接收**: 临时文件 temp-unixtime-pid.rdb (O_EXCL) / **diskless 空库交换** (replicationAttachToNewMaster); 加载后原子切换 | L1853,1802,1841,2863-2877 |
| **首 ACK 门控**: RDB 完成 → 从库 REPLCONF ACK → 主库才开命令流 (repl_start_cmd_stream_on_ack) | L1201-1212,1275,1308 |

**核心机制 (R-9b 增量同步)**

| 机制 | 锚点 |
|:--|:--|
| **replBacklog 共享块链**: repl_buffer_blocks + refcount (backlog+每从库各一引用) + blocks_index rax (每 64 块索引); 块大小自适应 max(size/16, 16KB) | L102-162,346-353; server.h:482-486 |
| **feedReplicationBuffer**: 尾部追加/新块/**三方引用传播** + 新块时增量裁剪 (64 块/次) + 慢从库缓冲限制 | L315-413 |
| **裁剪 refcount 守恒**: 首块 refcount==1 (仅 backlog) 才裁 + 至少留 1 块 + 裁后不超限则停 + **新 head 引用转移** (harness 实证) | L242-295 |
| **部分重同步**: +CONTINUE [新 replid] + addReplyReplicationBacklog (从请求 offset 续发); PSYNC offset = cached_master->reploff+1 | L598,781-790,2451-2453,2528-2574 |
| **ACK 心跳**: 每秒 REPLCONF ACK offset [fack aof-offset] + GETACK (WAIT 触发); 超时断线检测; **WAIT 命令** 副本确认 | L1184-1218,3254,3487,3521 |
| **PSYNC2 与断线恢复**: replid2 代际演进 (shiftReplicationId); cached_master 缓存 → resurrect 复活; 断线后 backlog 引用保持 | L1679-1715,3292-3386 |

**时空溯源**: 2009 SYNC → 2.8 PSYNC+backlog → 3.0 无盘 → 4.0 PSYNC2 → 6.2 WAIT → 7.0 共享块链+RDB-ONLY → 7.4 WAITAOF+fack

**深审**: 机制洞察 2 (**refcount 守恒裁剪** / FULLRESYNC 延迟应答 offset 语义); 常量 3 (backlog 默认 1MB/裁剪 64/索引 64); **harness 36 断言 3 轮迭代** (块边界/backlog 自引用/引用转移实证); 行号 ~50 处穷举; **二次 1 处 + 三次 3 处 + 四次 4 处 REVIEW** (**PSYNC 半开区间/rax 定位续传/磁盘共享 bgsave copyReplicaOutputBuffer/$前导/WAIT 阻塞拉取**; 推理验证 14 项全通过)

**负面空间**: 全量无增量 RDB/断点续传/独立 bgsave; 增量无无限保留 (1MB 超限全量)/命令级确认/多代 ID/从库间直同步

### R-10 Stream+rax — 消息队列 (大域拆 2 篇: R-10a rax / R-10b Stream)

**核心机制 (R-10a rax)**

| 机制 | 锚点 |
|:--|:--|
| **节点结构**: 1B 头位域 (iskey/isnull/iscompr/size) + 边字符区 + 子指针区; **压缩节点单子指针 vs 非压缩按位置索引** (children[j]↔edge[j]) | rax.h:35-44; rax.c:129-171,233-299 |
| **raxLowWalk**: 单趟下行 — 压缩逐字符/非压缩位置匹配; **j 每循环重置** (仅停中间保留 splitpos); parentlink+splitpos 双返回 | rax.c:436-477 |
| **ALGO 1 分裂**: 3a j==0 (splitnode 替换+iskey 继承) / 3b j>0 (trimmed 前缀+splitnode); postfix=后缀或原子子; **5 情形图解注释** (L529-595) | rax.c:596-780 |
| **ALGO 2 前缀键**: i==len 触发; **postfix=原节点余下+新键数据, trimmed=前缀+继承原键**; 键可在压缩节点 (L384-394) | rax.c:759-806 |
| **raxFind**: 完全匹配 (含压缩 splitpos==0) 且 iskey | rax.c:895-904 |
| **迭代器**: raxSeek (>=/</=) + 中序 Next/Prev (前缀优先) + SAFE 迭代中修改 | rax.c:1517-1792 |

**核心机制 (R-10b Stream)**

| 机制 | 锚点 |
|:--|:--|
| **双层存储**: rax (128bit BE ID→listpack) + listpack 叶子 (**stream-node-max-entries=100**, config.c:3207); master entry 主条目字段压缩 | stream.h:16-24; t_stream.c:475-520,457-459 |
| **ID 语义**: ms-seq 128bit; streamNextID (ms 前进用新 ms+seq0, 否则 last+1 — **时钟回退保护**); 指定 ID 严格递增 (EDOM) | t_stream.c:78-129,417-441 |
| **XADD**: ID 生成 → 尾 listpack 追加/新 rax 节点 → **MAXLEN/MINID 联动裁剪** (近似 limit=100×node_max_entries) | t_stream.c:408-520,862-873 |
| **消费组**: streamCG (last_id+entries_read+pel rax+consumers rax); **PEL 双层共享 NACK** (delivery_time/count/consumer) | stream.h:55-97 |
| **确认闭环**: XREADGROUP→PEL 入 / XACK→双 PEL 出 / XCLAIM→delivery_count++ 幂等重投 / NOACK | t_stream.c:2734,2834,3134 |
| **命令面**: XTRIM (MAXLEN/MINID) / XSETID / **XDEL 墓碑删除** (max_deleted_entry_id, 迭代跳过) / XINFO; 阻塞走 R-26 框架 (t_stream.c:2083) | t_stream.c:2147-3862 |

**时空溯源**: 2017 rax (antirez) → 5.0 Stream 发布 (XADD/XREAD/XGROUP) → 6.2 stream-node-max-entries+XAUTOCLAIM → 7.0 entries_read 修正 → 7.4 稳定

**深审**: **harness 5 轮迭代实证** (根节点类型/压缩必有子/位置索引/ALGO1 3a/ALGO2 条件 — 每轮修正源码误解) + **真实 rax.c 编译对照** (raxShow 结构一致); 机制洞察 2 (**键可在压缩节点** / ALGO2 键归属); 32 断言 ASan clean; 行号 ~40 处穷举; **二次 1 处 + 三次 4 处 REVIEW** (**raxNode 4B 头非 1B**/XREADGROUP=xreadCommand 统一实现/streamReplyWithRange L1670/raxSeek ^$/常量; 推理验证 8 项全通过)

**负面空间**: rax 无平衡/压缩率保证/并发; stream 无超时清理 (PEL 永久)/消费者自动删除/ID 复用/跨实例协调

### R-14 Sentinel — 高可用监控与故障转移 (大域拆 2 篇: R-14a 监控 / R-14b 故障转移)

**核心机制 (R-14a 监控)**

| 机制 | 锚点 |
|:--|:--|
| **三角结构**: sentinelRedisInstance 统一 (master→slaves/sentinels 双字典); **双链接** (命令 cc + pubsub pc 分离); 递归处理 | sentinel.c:5394-5416; sentinel.h |
| **周期命令**: PING 1s (L62) + INFO 10s→1s 动态 (L64) + PUBLISH hello; 断线重连+回调 | sentinel.c:3095,2377,2747 |
| **SDOWN 三条件**: elapsed>down_after (默认 30s, L68) / 主变从超 down_after+2×info / 主重启特殊期; **+sdown/-sdown 事件** | sentinel.c:4516-4582 |
| **ODOWN 弱 quorum**: 自己(1)+他哨兵 MASTER_DOWN 票 ≥ quorum (L4595-4606); **注释 "weak quorum"** (L4584-4589); is-master-down-by-addr 投票 | sentinel.c:4590-4623,4670 |
| **hello 自动发现**: __sentinel__:hello pub/sub (myid/epoch/主地址) → 哨兵互知+配置收敛 | sentinel.c:2996,2838 |
| **TILT 保护**: delta<0 或 >2s (L69) → 只收不判; 恢复 30s (PING×30, L70) | sentinel.c:5437-5447,5368-5372 |

**核心机制 (R-14b 故障转移)**

| 机制 | 锚点 |
|:--|:--|
| **领导者选举**: failover_epoch=++current_epoch; **每 epoch 每哨兵一票** (leader_epoch<req_epoch, L4739); +elected-leader 才继续; 非领导等 min(10s,timeout) 中止 | sentinel.c:4728-4756,4927-4938,5087-5118 |
| **确定性选主**: 候选 5 过滤 (非 down/5×ping/3×info/断连上限/priority>0) → **priority 小 → offset 大 → runid 小** | sentinel.c:4981-5052,5013-5039 |
| **7 态状态机**: WAIT_START→SELECT→SLAVEOF NO ONE→WAIT_PROMOTION→RECONF→DETECT_END→UPDATE_CONFIG; 每态超时中止 | sentinel.c:5087-5338 |
| **并行重配**: parallel_syncs 限流 (每轮最多 N 个从库 SLAVEOF 新主); RECONF_SENT→INPROG→DONE; 超时强制 | sentinel.c:5239-5298,4859 |
| **配置收敛**: DETECT_END→UPDATE_CONFIG→主切换; hello 广播 epoch 收敛; 配置自写落盘 | sentinel.c:5176-5235,5299,2261 |
| **保护**: abort 各态超时; **2×failover_timeout 冷却** (L4959); SENTINEL FAILOVER 强制; priority=0 不选 | sentinel.c:5339,4958-4975,3844 |

**时空溯源**: 2012 Sentinel 雏形 → 2.8 正式 (SDOWN/ODOWN/自动转移) → 3.0 hello 自动发现 → 3.2 三维选主+parallel_syncs → 4.0 REPLICAOF → 6.2 SENTINEL CONFIG

**深审**: 机制洞察 2 (**ODOWN 弱 quorum** / failover_start_time 随机化 MAX_DESYNC); 常量 8 (PING 1s/INFO 10s/publish 2s/down_after 30s/tilt 2s+30s/election 10s); **harness 18 断言 2 轮迭代** (get_leader 平票语义); 行号 ~35 处穷举; **二次 1 处 + 三次 4 处 REVIEW** (**DETECT_END 非状态枚举**/胜者双条件 绝对多数+quorum/sentinel.h 不存在/偶数哨兵; 推理验证 7 项全通过)

**负面空间**: 监控无实时推送/强一致判活/数据全同步; 转移无数据补偿/多领导并行/跨主转移/自动缩容

### R-15 Cluster — 分布式分片 (大域拆 2 篇: R-15a 分片路由 / R-15b 集群协议)

**核心机制 (R-15a 分片)**

| 机制 | 锚点 |
|:--|:--|
| **槽散列**: 16384 槽 = 1<<14; **crc16 & 0x3FFF** (低 14 位); crc16 = XMODEM 多项式 0x1021 查表; harness 实证 "foo"→12182/"bar"→5061 | cluster.h:8-10,43-62; crc16.c:82-88 |
| **{tag}**: 无 { / 无 } / 空 {} → 全键哈希; 有 → **只哈希 {} 之间** (取第一个对); 多键同槽前提 | cluster.h:40-42,46-61 |
| **七种重定向** (cluster.h:16-23): NONE/CROSS_SLOT(-CROSSSLOT)/UNSTABLE(-TRYAGAIN)/ASK/MOVED/DOWN_STATE/DOWN_UNBOUND; **MOVED=槽已迁 (客户端更新路由) vs ASK=迁移中 (一次性)** | cluster.c:1179-1205 |
| **槽迁移**: migrating_slots_to / importing_slots_from 双数组; SETSLOT MIGRATING/IMPORTING/NODE 三阶段; 迁移期源节点 ASK 重定向 | cluster_legacy.c:617-619; server.h |
| **阻塞救出**: clusterRedirectBlockedClientIfNeeded — 槽迁走解阻+MOVED (R-26 交叉); **READONLY 豁免** 从库副本槽 | cluster.c:1218-1278 |
| **命令面**: CLUSTER SLOTS/SHARDS(7.0)/SETSLOT/FAILOVER/ADDSLOTS | cluster.c:816,1371; cluster_legacy.c:5711 |

**核心机制 (R-15b 集群协议)**

| 机制 | 锚点 |
|:--|:--|
| **集群总线**: **port+10000** (CLUSTER_PORT_INCR, cluster_legacy.h:17); clusterAcceptHandler 连接; 消息 PING/PONG/FAIL/UPDATE | cluster_legacy.c:1232,2699 |
| **Gossip**: 每周期 PING 带随机节点子集; clusterProcessGossipSection 处理 — 未知节点自动发现/已知更新/PFAIL 报告 | cluster_legacy.c:2088,4634 |
| **PFAIL/FAIL 两阶段**: PFAIL=本地超时; **FAIL=markNodeAsFailingIfNeeded (L1883): 报告 ≥ size/2+1 + 自己主 +1, 无多数则清除** (对照哨兵 SDOWN/ODOWN) | cluster_legacy.c:1883-1900,5044 |
| **configEpoch**: 槽位图版本; clusterUpdateSlotsConfigWith 大者胜; 冲突递增 (HandleConfigEpochCollision); bump 无共识 (7.0) | cluster_legacy.c:1765,2321 |
| **从库提升**: clusterHandleSlaveFailover — 主 FAIL → 请求投票 (一 epoch 一票) → 多数 → 提升+SLAVEOF NO ONE (对照 R-14 选举) | cluster_legacy.c:3206,4153 |
| **可用性**: clusterUpdateState 三层 — **CLUSTER_WRITABLE_DELAY=2s 重启写保护** (L5042) / **cluster-require-full-coverage 可配置** (默认 1, config.c:3069) / **少数派分区保护: reachable < size/2+1 → FAIL** (L5095-5104, 脑裂拒写); CLUSTERDOWN 写拒绝 | cluster_legacy.c:5044-5130; cluster.c:1234 |

**时空溯源**: 2011 设计 → **3.0 发布** (16384/CRC16/MOVED/gossip) → 3.2 槽迁移完善 → 5.0 提升优化 → 6.0 客户端缓存交叉 → **7.0 cluster_legacy.c 拆分 + SHARDS/bump 无共识**

**深审**: 机制洞察 1 (**MOVED vs ASK**); 常量 3 (PORT+10000/SLOTS 16384/CLUSTER_OK); **harness 20 断言一次通过** (crc16 0x31C3/{tag} 三边界/槽位对照/重定向判定); 行号 ~40 处穷举; **二次 1 处 + 三次 4 处 REVIEW** (**FAIL=size/2+1 精确判定**/**少数派分区保护**/WRITABLE_DELAY 2s/require_full_coverage 配置/CLUSTER_PORT_INCR 位置; 推理验证 8 项全通过)

**负面空间**: 分片无自动重分片/跨槽多键/槽级复制; 协议无强一致/跨节点事务/多活写/脑裂防写

---

## §二 方法论执行报告 (28 域实证)

### 1. harness 13 个迭代历史 (全部抓到真实问题)

| harness | 断言 | 抓到的问题 | 轮次 |
|:--:|:--:|:--|:--:|
| r33 zmalloc | 6/6+6/6 | 记账双路径 16 vs 18 (机制实证) | — |
| r4 sds | 16/16 | 伪降型缩容 s[-1] 不更新 | — |
| r3 dict | 16/16 | SCAN 弱语义 (扩容后插入桶漏键合法) | 4 |
| r19 listpack | 16/16 | 缩容顺序 (先 memmove 后 realloc) | 2 |
| r6 zset | 8/8 | 层级期望 1.334 实测 | — |
| r5 quicklist | 11/11 | 分裂测试对象错误 | 1 |
| r1 object | 14/14 | 共享边界 "5"<10000 / EMBSTR 误 free | 3 |
| r20 server | 5/5 | hz 每 tick 回落 (非累积) | 1 |
| r21 db | 44/44 | **expires 共享键删除必须不释放** (ASan UAF) | 3 |
| r2 events | 23/23 | maxId 防重入测试时间设定 / 顺序日志强化 | 2 |
| r28 networking | 23/23 | RESP 长度硬编码 SEGV / 分片数据 / 零拷贝构造 | 3 |
| r25 hash | 30/30 | Field 未初始化 free (BUS) / EXPIRED vs EXPIRED_HASH | 3 |
| r26 blocked | 16/16 | 栈对象误 free (BUS) / 消费 FIFO 语义 (一个元素一个客户端) | 5 |

### 2. 发现问题类型统计 (深审+二次 REVIEW, 累计 87 处)

| 类型 | 数量 | 代表 |
|:--:|:--:|:--|
| 行号偏差/偏移 | 40+ | lpEncodeGetType L317/hashTypeDbActiveExpire L2073/kvstoreCreate L2667-2675/rejectCommand L4050 |
| **编造** | 3 | sdshash 缓存不存在 / ebGetNextTimeToExpire 调度依据 / AND 零字节短路 |
| 语义/表述错误 | 25+ | SPOP COUNT 正负说反 / "六拒"实为五拒 / "不丢不重"弱语义 / 传播三格式 |
| 前向依赖违规 | 3 | R-15 (R-21)/R-16 (R-21)/R-9 (R-28) 前置声明未来域 |
| 覆盖缺口 | 7 | expireSlaveKeys/getKeys-cluster/FAST iAmMaster/notify/统计面/beforesleep 内容 |
| 机制洞察 | 4 | 唤醒只需 dbAdd / set 三编码+双向 / HFE 三阶段迁移 / 字宽批量 |
| harness 缺陷 | 20+ | 长度硬编码/断言构造/清理泄漏/测试对象 |

### 3. 行号验证数: 累计 ~1400 处 (每域 40-130 处穷举 grep)

### 4. 方法论铁律 (V3 版, 28 域实证强化)

1. **行号必 grep 原文件** — 40+ 处偏差实证; "附近"/"L908+"/区间猜测全被二次 REVIEW 抓出
2. **机制描述必须 harness 实证** — 13 个 harness 全部迭代修正过
3. **认知修正优先于写作** — 级联更新/不丢不重/zslFirstInRange/唤醒路径 4 个旧认知被源码推翻
4. **前向依赖必查拓扑** — 3 次违规 (R-15/R-16/R-9), 前置声明只允许已讲域
5. **"六拒/N 种"类计数必须穷举** — 六拒→五拒+挂队列; 双编码→三编码
6. **推断必须标注** — 每域 3 处以上 (动机/量级/命名)
7. **C 单体适配** — 依赖方向用函数调用+回调注册点; MCP trace 对回调抓取有限, grep 兜底
8. **大域必拆** — 6 域拆 2 篇 (R-20/R-21/R-2/R-28/R-25/R-26); 每篇独立大纲
9. **每域二次 REVIEW 必走 07 五维度轮换** — 零发现=没细查; R-27 的 SPOP 语义错误只有逐句对源码才暴露
10. **更新双文档** — 每域: 本文 §零/§一/§四 + HANDOFF-STAGE3

---

## §三 高频坑汇总 (跨域 40+ 条, 28 域实证)

### 行号/引用类
1. 行号必 grep — 40+ 处偏差 (lpEncodeGetType 317/NaN 1435/dictExpand 1292/hz 1283/返回 1538/extend_to_usable 125-146/intset.h 35-39/SIZE_SAFETY_LIMIT 8192/键空间 2667-2675/hashTypeDbActiveExpire 2073/rejectCommand 4050/blockClient 75-79/setbit 535-537)
2. "附近"/"L908+"/区间猜测 = 未验证 — 一律 grep 精确定位
3. 函数定义行 vs 函数体关键行要区分 (多行语句偏移是区间覆盖陷阱)
4. 注释行号 ≠ 代码行号 (注释偏移 1-2 行常见)

### 语义/机制类
5. **SCAN 弱语义**: 不丢旧键 (保证)/重复允许/插入可漏 — 别写"不丢不重"
6. **listpack 无级联** (backlen 自指+固定 5B) — ziplist 才有
7. **zslFirstInRange/zslLastInRange 已移除** — zslNthInRange 统一
8. **set 三编码非双编码** — intset/listpack/HT; listpack 阈值 128 ≠ hash 512
9. **set 支持编码降级** (Redis 唯一) — sinterstore 全整数转回 intset
10. **SPOP COUNT 仅正数** — 正负语义属 SRANDMEMBER (正=uniq/负=可重复)
11. **"六拒"实为五拒+挂队列** (prepareClientToWrite)
12. **传播面三格式**: PEXPIREAT (未过期)/DEL|UNLINK (已过期, 依 lazyfree_lazy_expire)
13. **EXPIRE 已过期不是静默拒绝** — checkAlreadyExpired → 删+DEL 重写; SET 标志互斥报 syntaxerr
14. **FAST 过期循环需 iAmMaster** (server.c:1687-1690)
15. **淘汰拒绝按 CMD_DENYOOM 标志** 非"只读命令" — DEL/EXPIRE 清理类可执行
16. **avg_ttl 公式变量遮蔽**: `db->avg_ttl = avg_ttl + (db->avg_ttl - avg_ttl)*factor` — 右值第一个是局部采样均值
17. **GETF_EXPIRED_HASH**: 读一个过期字段可能把整个 hash 读没了
18. **唤醒只需 dbAdd 路径** (list): 空键不存在不变量 — t_list.c 零 signalKeyAsReady
19. **ebGetNextTimeToExpire 无全局消费端** — 主动过期走 expires_cursor
20. **hashTypeAddToExpires 唯一入口在 t_hash.c:2040** — db.c 只是 RENAME/MOVE/COPY 特例
21. **AND 零字节短路不存在** — BITOP 是字宽批量 (每轮 4×ulong)
22. **sdsnewlen(NULL) 不填零** — 零填充归 sdsgrowzero
23. **NOTOUCH/NOEXPIRE 等五标志** 是副作用开关矩阵 (lookupKey)
24. **expires 表 destructor=NULL 是删除安全的物理依据** (harness ASan 实证)

### 数字/常量类
25. 编码家族数字: EMBSTR 44=16+3+44+1 (64B)/set 512/hash 512+64/zset 128+64/list -2=8KB/共享整数 10000/EVPOOL 16/MAX_SAMPLES 5
26. 14bit=16384 槽 (crc16&0x3FFF+{tag}); kvstore bits≤16 (游标留 48 位)
27. ebuckets: SEG_MAX=16/LIST_MAX=16/KEY_SIZE=6/PRECISION=0 (TBD)/48bit TTL
28. HFE 配额: 10000/hz + 100 万×32 封顶; CRON_DICTS=16/1000us
29. LAZYFREE_THRESHOLD=64 (按分配数非字节)
30. proto 常量: IOBUF 16KB/REPLY_CHUNK 16KB/INLINE 64KB/MBULK_BIG_ARG 32KB/NET_MAX_WRITES 64KB
31. tenacity: ≤10 线性 50us×t/<100 几何 1.15^(t-10)/=100 无限; 默认 10
32. LFU: INIT_VAL=5/log_factor=10/decay_time=1; LRU 24bit×1000ms (~194 天回绕)
33. intset 1<<30 上限; SPOP_MOVE_STRATEGY_MUL=5; ZSKIPLIST_MAX_SEARCH=10

### 结构/关系类
34. 共享对象不可变 (OBJ_SHARED_REFCOUNT) — maxmemory 禁共享整数 (私有 LRU)
35. 7.x 重构改变机制面: kvstore 分片/ziplist 退役/listpack 无级联/HFE
36. **编码单向 vs 双向**: hash/list 单向升级; set 双向 (唯一降级)
37. 空集/空 list 不存在不变量 — 删空即删键 (R-21 三表一致)
38. io threads: IO 可并行执行仍单线程; 从库强制主线程 (共享 repl 缓冲)
39. 传播归一哲学: 绝对时间戳 (PEXPIREAT/PXAT) — 主从时钟无关
40. 配置阈值单一来源: config.c:3215-3223 (list/hash/set/zset 编码阈值)

### R-12 新增
41. **hllCount 不是经典调和平均** — 7.4.2 是 Ertl sigma/tau 估计器 (arXiv:1702.01284), 经典 Σ2^-M 是 Flajolet 2010 原版; 面试讲错公式即翻车
42. **"HLL 12KB" 只对稠密成立** — 稀疏最小 18B (空 HLL = XZERO:16384); 稠密 = 16B 头 + 12288B
43. **PFCOUNT 是只读命令但会改值+传播** — commands.def 唯一 CMD_READONLY\|CMD_MAY_REPLICATE 组合; 缓存回写需 dbUnshare + dirty++
44. **HLL 哈希固定 seed 0xadc83b19 无随机化** — 对照 dict SipHash 随机 seed; 跨实例确定性是主从/合并一致的前提

### R-13 新增
45. **GEO 内部奇偶位与标准 geohash 相反** (lat 偶数位) — 标准 lon 偶数位; 内部自洽, GEOHASH 命令输出时重编码标准
46. **GEO 纬度范围 ±85.05112878 非 ±90** — EPSG:900913 墨卡托极限 (极地不可编码)
47. **GEOHASH 11 字符 = 55bit 槽 > 52bit → 第 11 字符恒 '0'** (低 2 bit 不入字符)
48. **GEOADD 就是 ZADD**: score=52bit 编码 — GEO 键用 ZRANGE/ZSCORE 完全可查 (不是独立类型)

### R-29 新增
49. **notify-keyspace-events 的 "A" 只含 10 类** — 不含 K/E/m/n; K 与 E 是频道视角开关, 需单独加
50. **RESP2 订阅态命令白名单** — 只能 ping/订阅族/quit/reset; RESP3 无限制; 别给 RESP2 订阅客户端发 GET
51. **PUBLISH 非 cluster 会复制到从库** (PROPAGATE_REPL) — 从库订阅者也能收到; cluster 走 gossip 不复制
52. **空频道即删** — 最后订阅者退订频道表删除 (防滥用); 订阅几百万频道的成本是持久的, 退订即回收

### R-16 新增
53. **EXEC 传播是逐条命令流, 不包 MULTI** — MULTI...EXEC 包裹仅 also_propagate 批量场景 (numops>1); 面试讲"EXEC 包成 MULTI 传播"会被源码打脸
54. **WATCH 后 UNWATCH 可恢复** — 清 DIRTY_CAS 即复活; 但 touch 后已自动退订, 需重新 WATCH
55. **MULTI 内禁 4 命令** (psync/save/shutdown/sync, CMD_NO_MULTI); EXEC 期才做 ACL 复查
56. **CAS 失败回 nil 数组, 队列错误回 EXECABORT** — 两个失败语义别混 (nil 非错误)

### R-8 新增
57. **RDB_VERSION=12** — magic "REDIS0012"; RDB 类型号 0-7∪9-25 **无 4/8**; 面试说 "REDIS0011" 会被源码打脸
58. **AOF everysec 有 2s 延期预算** — fsync 忙时推迟写, 超 2s 强制 (aof_delayed_fsync 计数)
59. **混合持久化 = RDB preamble + AOF 尾** — aof-use-rdb-preamble 开时重写文件前半是二进制 RDB
60. **重写不丢数据靠文件级分离**: fork 子进程写 temp BASE + 父进程切新 INCR (7.0 起无 diff 管道); manifest 管理 BASE/INCR/HISTORY

### R-9 新增
61. **PSYNC offset 是 +1 语义** (reploff+1) — 面试说"从库最后处理的 offset"不精确
62. **backlog 裁剪 refcount==1 才裁** — 慢从库引用旧块时 backlog 天然扩大 (注释 "accept partial resync as much as possible")
63. **FULLRESYNC 延迟到 RDB 就绪** — offset 是 RDB 生成时刻; 立即回复的 offset 会断链
64. **断线重连是否全量 = backlog 范围判定** — 1MB 默认, 断线超 1MB 写入量必全量

### R-10 新增
65. **rax 非压缩节点 children 按位置索引** (children[j]↔edge[j]) — 面试写"按字符值索引"是错; harness 实证
66. **键可以挂在压缩节点上** — ALGO 2 插入前缀键后, 新键数据在 postfix, 原键在原子子 ("ANNI"→"BALE"(新)→[](原))
67. **XDEL 是墓碑删除** — 标记 deleted, 不物理移除 (Stream 唯一; 其他类型物理删)
68. **XADD ID 时钟回退保护** — ms 回退用 last 递增; 指定 ID ≤ last 报 EDOM

### R-14 新增
69. **SDOWN 是本地判断, ODOWN 才是共识** — "weak quorum" (自票+他票 ≥ quorum), 无强一致保证 (源码注释实证)
70. **每 epoch 每哨兵一票** (leader_epoch < req_epoch) — 投过票的 epoch 不重复投; 面试讲"每哨兵可反复投票"是错
71. **选主是确定性排序** — priority 小 → offset 大 → runid 小; 所有哨兵算出同一结果
72. **转移冷却 2×failover_timeout** — ODOWN 后立即重试会被拒; SENTINEL FAILOVER 可强制

### R-15 新增
73. **MOVED 和 ASK 不是一回事** — MOVED=槽已迁移 (客户端永久更新路由); ASK=迁移中导入 (一次性, 需 ASKING); 面试混用即错
74. **{tag} 只哈希 {} 之间** — "foo{bar}baz" 和 "{bar}" 同槽; 无 } / 空 {} 退化为全键哈希
75. **集群总线 port+10000** — 客户端端口之外的独立拓扑; 面试说"客户端连 10000"是错 (那是总线)
76. **16384 = 2^14, crc16 & 0x3FFF** — 槽是 CRC 低 14 位; "foo"→12182 是已知对照

---

## §四 下一步 R-17 (客户端缓存) 详案 + 剩余域路线图

### R-15 (Cluster) — ✅ 已交付 (2026-08-13, 速查见 §一)

**交付摘要**: 12 闭环全完成 (R-15a: q1 槽散列/q2 tag/q3 重定向/q4 槽迁移/q5 阻塞救出/q6 命令面; R-15b: q1 总线/q2 gossip/q3 故障检测/q4 configEpoch/q5 从库提升/q6 状态); 两篇大纲 87+91 行; **harness 20 断言一次通过** (crc16 已知值/槽位对照/{tag} 边界/重定向判定); 行号 ~40 处穷举; 二次 REVIEW 1 处记录。
**⚠ 域级怀疑审计结果**: 预判 6 项全验证 (16384/CRC16/{tag}/MOVED-ASK/槽迁移/gossip); **机制洞察: MOVED vs ASK 语义** (harness 实证); 常量 CLUSTER_PORT_INCR=10000 (详 §一 R-15)。

### R-17 (客户端缓存) 详案

**源码**: `src/tracking.c (648)` + `src/blocked.c` 部分 + `src/networking.c` 部分 — 服务端协助的客户端缓存

**方案**: 🟡 B (Pass 0-3 + 时空溯源, 无 harness)。

**已知连接点**:
- R-21 (已交付): **signalModifiedKey = touchWatchedKey + trackingInvalidateKey 双失效** (db.c:620-623) — 本域挖 trackingInvalidateKey
- R-16 (已交付): WATCH 同源失效路径对照
- R-28 (已交付): RESP3 push 消息 (CLIENT_PUSHING/CLIENT_TRACKING) — 已见协议面
- R-29 (已交付): 对照 pubsub 通知
- R-15 (已交付): 集群下重定向失效面

**关键机制面** (Pass 1 起点): **CLIENT TRACKING ON/OFF** (RESP2: 重连失效; RESP3: 原生 push) / tracking 表 (key→client 集合, 内存上限 maxmemory 淘汰) / **失效消息 invalidate** (push) / BCAST 前缀模式 / OPTIN/OPTOUT 模式 / **noloop** / 重连恢复 (id 与失效重放) / 多客户端共享 / 集群节点内失效

**流程提醒**: 🟡 B → 六层深审必须真找问题 (tracking 表结构/失效消息格式/内存上限/BCAST 前缀穷举) → 无 harness → 全量回归 → 更新本文 §零/§一/§四 + HANDOFF-STAGE3。

### 剩余 5 域路线图

| 序 | 域 | 文件 (行) | 方案 | 预判 |
|:--:|:--|:--|:--:|:--|
| 29 | R-17 客户端缓存 | tracking.c (648) | 🟡 B | RESP3 invalidate |
| 30 | R-18 碎片 | defrag.c (1270) | 🟡 B | jemalloc hint |
| 31 | R-30 Lua+Functions | eval+script+functions | 🟡 B | 原子性/脚本缓存 |
| 32 | R-32 ACL | acl.c (3241) | 🟡 B | 用户/权限 |
| 33 | R-31 module | module.c (14002) | 🟡 B | 巨型单文件, 可最后/按需 |

---

## §五 完成检查单 (下次会话开始前)

- [ ] 读本文 §零 (28/33 状态) + §四 (R-17 详案) — **无需回溯任何历史对话**
- [ ] 开工 R-17 前 15 分钟: 对 tracking 表结构/失效消息格式/内存上限/BCAST 前缀/OPTIN-OPTOUT 做域级怀疑审计 (数字穷举)
- [ ] 每域完成: Pass 0-3 + 六层深审 + 时空溯源 + harness (🔴) + 更新本文 §零/§一/§四 + HANDOFF-STAGE3
- [ ] 每域二次 REVIEW (07 五维度轮换) 记录真实问题, 零发现=不合格
- [ ] 每域完成时把该域速查从 §四 详案区移入 §一 (保持 §一 是完整速查库)

## §六 文件路径

```
analysis/source-analysis/redis/          ← 本文所在
├── REDIS-PLAN.md                        ← 33 域规划 + §〇 怀疑审计表 (18→33 v2)
├── HANDOFF-REDIS.md                     ← V2 (历史, 被本文取代为唯一入口)
├── HANDOFF-REDIS-V3.md                  ← 本文 (超详细全量交接)
├── knowledge-planning/                  ← 早期 9 域规划速查 (后续域直接走 outlines/ 全管线)
├── outlines/                            ← 28 域 (11 个大域拆 2 篇, 共 39 篇大纲, 9867 行)
│   ├── r33-zmalloc/ r4-sds/ r3-dict/ r19-listpack/ r7-intset/
│   ├── r6-zset/ r5-quicklist/ r1-object/
│   ├── r20-server/ (2篇) r21-db/ (2篇) r22-expire/ r23-evict/
│   ├── r2-events/ (2篇) r28-networking/ (2篇)
│   ├── r24-string/ r25-hash/ (2篇) r26-list/ (2篇) r27-set/ r11-bitmap/ r12-hll/ r13-geo/ r29-pubsub/ r16-multi/ r8-persistence/ (2篇) r9-replication/ (2篇) r10-stream/ (2篇) r14-sentinel/ (2篇) r15-cluster/ (2篇)
└── harness/                             ← 18 个, 全部 gcc+ASan (r33/r4/r3/r19/r6/r5/r1/r20/r21/r2/r28/r25/r26/r8/r9/r10/r14/r15)

源码: /data/workspace/source-code/code/spring/redis/  (Redis 7.4.2, src/ 175 文件)
MCP 索引: data-workspace-source-code-code-spring-redis (31593 节点, ready)
上级: ../HANDOFF-STAGE3.md (阶段3 总入口, Redis 状态 28/33)
后续: 阶段3.6 Redisson (7 域) / 阶段3.7 ES (11 域) — 开工前 09 域重审
```
