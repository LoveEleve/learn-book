# Redis 源码分析 — 交接文档 v2 超详细版 (阶段3.5)

> **日期**: 2026-08-13 | 阶段3.5 Redis (7.4.2, src/ 175 文件)
> **⚠️ 本文已被 V3 取代**: 全量交接请用 **`HANDOFF-REDIS-V3.md`** (19 域核心知识全固化/方法论执行报告/40+ 高频坑/R-12 详案, 618 行, 新会话零回溯)。本文保留作历史增量记录。
> **⚠️ 总入口**: 阶段3 总交接见 `../HANDOFF-STAGE3.md` (M/MP/Redis 全状态)。
> **规划权威**: `REDIS-PLAN.md` (33 域 v2, 含 §〇 09 怀疑审计表 — 执行计划 18→33, 拓扑重排, v2 补 zmalloc/functions)。
> **方法论权威**: `talk-method/source-code-analysis/methodology/zh/` (01-09; **09 对既有规划保持怀疑必读** — 反模式 7/8: 行号限制废弃/合规范≠讲清楚)。
> **给新 AI**: 阅读顺序 §零~§二 → §十二 (R-12 详案) → 开工。严格一个域一个域, 问题驱动, 禁止批量写。每域走 v5 全管线 (Pass 0-3 + 六层深审 + 时空溯源 + harness [仅 🔴] + 更新本文 §零/§十二 + HANDOFF-STAGE3 §零)。

---

## §零 当前状态速查 (2026-08-13, 19/33 完成)

| 域 | 目录 | 类型 | 行数 | 锚点 | 闭环 | questions | 状态 |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|
| R-33 zmalloc | outlines/r33-zmalloc | 🔴 | 85 | 23 | 8 | 20 | ✅ (harness 双变体 6/6+6/6, ASan 零越界) |
| R-4 SDS | outlines/r4-sds | 🔴 | 104 | 28 | 8 | 20 | ✅ (harness 16/16) |
| R-3 Dict | outlines/r3-dict | 🔴 | 101 | 23 | 8 | 20 | ✅ (harness 16/16) |
| R-19 listpack | outlines/r19-listpack | 🔴 | 94 | 18 | 8 | 20 | ✅ (harness 16/16) |
| R-7 intset | outlines/r7-intset | 🟡 | 78 | 14 | 6 | 20 | ✅ (方案 B, 无 harness) |
| R-6 skiplist+ZSet | outlines/r6-zset | 🔴 | 99 | 16 | 8 | 20 | ✅ (harness 8/8) |
| R-5 quicklist | outlines/r5-quicklist | 🔴 | 96 | 21 | 8 | 20 | ✅ (harness 11/11) |
| R-1 redisObject | outlines/r1-object | 🔴 | 94 | 18 | 8 | 20 | ✅ (harness 14/14) |
| R-20 server 骨架 | outlines/r20-server | 🔴 | 53+56 (2篇) | 3+6 | 8 | 20 | ✅ (大域拆 2 篇 + harness 5/5) |
| R-21 db 键空间 | outlines/r21-db | 🔴 | 77+80 (2篇) | 5+4 | 8 | 21 | ✅ (大域拆 2 篇 + harness 44/44 + 二次 REVIEW 10 处修正) |
| R-22 过期机制 | outlines/r22-expire | 🟡 | 95 | 30+ | 6 | 21 | ✅ (方案 B, 无 harness, 96 行号验证 + 二次 REVIEW 5 处修正) |
| R-23 内存淘汰 | outlines/r23-evict | 🟡 | 98 | 35+ | 6 | 21 | ✅ (方案 B, 无 harness, 130 行号验证 + 二次 REVIEW 2 处修正) |
| R-2 事件驱动 | outlines/r2-events | 🔴 | 70+54 (2篇) | 5+3 | 8 | 21 | ✅ (大域拆 2 篇 + harness 23/23 + 二次 REVIEW 4 处修正) |
| R-28 networking | outlines/r28-networking | 🔴 | 74+63 (2篇) | 4+4 | 8 | 21 | ✅ (大域拆 2 篇 + harness 23/23 + 二次 REVIEW 2 处修正) |
| R-24 t_string | outlines/r24-string | 🟡 | 83 | 30+ | 6 | 20 | ✅ (方案 B, 无 harness, 117 行号验证 + 二次 REVIEW 2 处修正) |
| R-25 t_hash | outlines/r25-hash | 🔴 | 73+69 (2篇) | 4+4 | 8 | 21 | ✅ (大域拆 2 篇 + harness 30/30 + 二次 REVIEW 3 处修正) |
| R-26 t_list+blocked | outlines/r26-list | 🔴 | 70+66 (2篇) | 4+4 | 8 | 21 | ✅ (大域拆 2 篇 + harness 16/16 + 二次 REVIEW 2 处修正) |
| R-27 t_set | outlines/r27-set | 🟡 | 86 | 30+ | 6 | 21 | ✅ (方案 B, 无 harness, 82 行号验证 + 二次 REVIEW 1 处修正) |
| R-11 Bitmap | outlines/r11-bitmap | 🟡 | 83 | 30+ | 6 | 21 | ✅ (方案 B, 无 harness, 56 行号验证 + 二次 REVIEW 3 处修正) |

**执行序**: R-33 ✅ → R-4 ✅ → R-3 ✅ → R-19 ✅ → R-7 ✅ → R-6 ✅ → R-5 ✅ → R-1 ✅ → R-20 ✅ → R-21 ✅ → R-22 ✅ → R-23 ✅ → R-2 ✅ → R-28 ✅ → R-24 ✅ → R-25 ✅ → R-26 ✅ → R-27 ✅ → R-11 ✅ → **R-12 → R-13 → R-29 → R-16 → R-8 → R-9 → R-10 → R-14 → R-15 → R-17 → R-18 → R-30 → R-32 → R-31**

**基础层 + 网络层 + 命令层 + 高级数据结构 19 域已闭环** — 剩余 14 域全为高级特性。

---

## §一 方法论铁律 (本阶段已实战验证)

1. **09 怀疑审计必走**: 每域开工前对 PLAN 断言做域级复查 (数字/行号/默认值穷举 grep)。REDIS-PLAN §〇 已示范 (18→33)。
2. **行号必须 grep 原文件**: 本阶段 9 域已抓 10+ 处行号偏差 (lpEncodeGetType L317 非 660+/NaN L1435 非 1442/dictExpand L1292/hz 重置 L1283 非 1282/返回 L1538 等) — 凡引用行号必 sed 验证。
3. **机制描述必须 harness 实证**: 🔴 域强制 harness (R-33/R-4/R-3/R-19/R-6/R-5/R-1/R-20/R-21/R-2/R-28/R-25/R-26 共 13 个)。harness 每次抓到真实问题: 记账双路径 16 vs 18 (R-33)/伪降型 (R-4)/SCAN 弱语义 (R-3)/缩容顺序 (R-19)/层级期望 1.334 (R-6)/分裂目标 (R-5)/共享边界 "5"<10000 (R-1)/hz 每 tick 回落 (R-20)/expires 共享键删除安全 (R-21)/maxId 防重入 (R-2)/RESP 分片到达 (R-28)/Field 未初始化 (R-25)/栈对象误 free (R-26)。
4. **认知修正先行**: 本阶段重大修正 — REDIS-PLAN"级联更新"(listpack 实为无级联)/zslFirstInRange 已移除 (zslNthInRange 统一)/"不丢不重"实为 SCAN 弱语义。**机制描述遇矛盾先查源码再下笔**。
5. **六层深审必须真找问题**: 每域二次 REVIEW 记录 2-6 处 (行号/编造/语义/推断标注)。零发现 = 没细查。
6. **推断必须标注**: 代码事实 vs 机制推断分开 (如 intset"升而不降"的成本分析标注推断)。
7. **C 单体代码适配**: 无模块 import — 依赖方向用函数调用方向 + server.h 全局态分析; MCP (codebase-memory, 31593 节点) trace 对回调注册 (函数指针) 抓取有限, grep 兜底。
8. **大域拆分**: ≥6 闭环拆 2-5 篇 (R-20 拆 2 篇: 01-boot/02-cron-commands; R-21 拆 2 篇: 01-keyspace-operations/02-kvstore-ebuckets-lazyfree)。每篇独立大纲文件。
9. **🟡 域 (R-7)**: 方案 B, 无 harness, 仍走 Pass 0-3 + 深审。

---

## §二 Redis 域重审历史 (09 完整故事, 必读)

**执行计划原始断言 (18 域)**: redisObject/事件驱动/dict/SDS/quicklist/zset/intset/RDB+AOF/复制/Stream/bitmap/HLL/GEO/Sentinel/Cluster/事务/客户端缓存/碎片。

**09 重审发现 (2026-08-13)**:
1. **域清单严重不完整**: 顶层 src/ 全量扫描 (175 文件) → 遗漏 14 个定义特征级机制: server 骨架 (7256)/db 键空间 (2818)/networking 协议 (4653)/t_string/t_hash/t_list/t_set/expire/evict/pubsub/Lua 脚本/listpack (3150)/ACL/module (14002)。
2. **v2 再补 2 处**: zmalloc (1056, 内存层!) + Functions 系统 (functions+function_lua)。
3. **数字断言 8 项全接受**: EMBSTR 44B/1:1-4:1-3.125%/1MB/0.25/16384/52bit 逐项 grep ✅。
4. **拓扑重排**: R-2 事件驱动 2→12 位 (反拓扑, initServer 注册后才 aeMain); R-1 object 1→7 位 (依赖全部编码); R-33 zmalloc 新增第 1 位。
5. **依赖方向方法适配**: C 单体无 import — 改函数调用 + 回调注册点 (main→aeMain L7251/initServer→aeCreateTimeEvent L2757) 验证。

**最终**: 18 → **33 域** (17🔴+15🟡 修正为 18🔴+15🟡, 覆盖率 183%, 域清单错误率 83%)。完整审计表见 REDIS-PLAN.md §〇。

---

## §三~§十一 已交付 10 域机制速查

## §三 R-33 已交付内容速查 (zmalloc 内存层)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **分配器多路**: tcmalloc/jemalloc/macOS/libc 编译期选择; 宏重映射 malloc→je_malloc | zmalloc.h:29-79; zmalloc.c:54-68 |
| **双路径记账**: HAVE_MALLOC_SIZE → 0 前缀 (usable 实时查询, 记账=分配器实际); 否则 8B 前缀存请求大小 — harness 实证: 10B 请求记 16 vs 18 | zmalloc.c:37-45,93-111 |
| **OOM 分层**: zmalloc 失败→handler→serverPanic 崩溃 (main 装配 redisOutOfMemoryHandler); ztry 家族→NULL 降级 (rdb 加载/t_stream/module) | zmalloc.c:124-134; server.c:6712-6717,6970; rdb.c:389 |
| **usable 优化**: zmalloc_usable 返回分配器实际大小 → SDS sdsnew 初始 alloc=usable 免费膨胀 (harness: alloc=15 > 请求 3) | zmalloc.c:138-147; sds.c:90-105 |
| **extend_to_usable**: alloc_size 属性传导骗编译器, 解 _FORTIFY_SOURCE SIGABRT (systemd PR#25688/gcc 96503) | zmalloc.h:110-124; zmalloc.c:84-89 |
| **原子统计**: used_memory redisAtomic (C11=_Atomic) — bio 后台线程分配实证 (bio.c:192) | zmalloc.c:70-73; atomicvar.h:62-92 |
| **统计面**: RSS (/proc/self/stat 多平台, fallback used_memory) / private_dirty (smaps→fork CoW 报告 childinfo.c:70) / mallctl allocator_info (INFO memory) | zmalloc.c:496-646,700+; childinfo.c:60-80 |
| **jemalloc 绑定**: with_flags (mallocx) / no_tcache (MALLOCX_TCACHE_NONE, R-18 defrag 唯一消费) / arena 级 (Lua VM) / HAVE_DEFRAG=定制 FRAG_HINT | zmalloc.c:149-213; zmalloc.h:76-78 |

### R-33 时空溯源 (仓库单提交快照, 代码内演进痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez 初版) | zmalloc 基础 + used_memory 记账 |
| 旧 libc 时代 | PREFIX_SIZE 前缀路径 (无 usable_size 平台兜底, 保留至今) |
| 6.x | defrag 引入 with_flags/no_tcache (R-18) |
| 7.x | try 家族 (maxmemory 重构); muzzy 统计 (RELEASENOTES #12996) |
| gcc-12 时代 | extend_to_usable (_FORTIFY_SOURCE SIGABRT 修复) |

### R-33 深审记录 (六层深审抓到 1 处真实问题)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述错误** | 节 2 "jemalloc 下 used_memory 更高" — harness 实证 10B 请求: usable 记 16 / 前缀记 18 — **无绝对高低, 记账语义不同** (分配器视角 vs 请求者视角) | 大纲节 2 + KP q1 修正 |

### R-33 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 2 | 行号偏差 | extend_to_usable 注释区间 110-124 实为 **125-146** (声明 L146/完整注释段 L125-146) | 大纲节 4+闭环 q5+KP 3 处修正 |
| 3 | **完整性补充** | zcalloc_num 乘法溢出检查 (L249-259, 安全面: num×size 防 wrap) 未写 | 大纲节 1 补全 |
| 4 | **完整性补充** | zmalloc_get_memory_size (server.c:2637 → INFO total_system_memory) + mallctl 不可用兜底 (server.c:1249-1250 allocator_allocated=zmalloc_used) | 大纲节 5 补全 |
| 5 | 通过项 | io_threads 分配证据确认: IOThreadMain (L4248) → readQueryFromClient → processInputBuffer 解析链会分配 — q3 论证补强; lua_arena (server.h:2028) ✅; 行号 27/29 验证 ✅ | 记录 |

### R-33 负面空间 (07 维度5)

不做垃圾回收 (对称 free) / 不做内存池 (分配直接走系统分配器, jemalloc 内部有 tcache) / 不做对象大小分级 / 不做内存压缩 (上层数据结构的事) / 不兜底所有 OOM (try 只覆盖特定路径)。

---

## §四 R-4 已交付内容速查 (SDS)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **指针即对象**: sds=char*, packed 头部内嵌 buf 前 (s[-1]=flags), inline 访问族 O(1) | sds.h:20-79 |
| **五级头部**: sdsReqType 阈值 31/255/65535/2^32; **sdshdr5 仅 1B (5bit len) 静态串专用** (从不作 struct), 空串/扩容强制跳 8, would_regrow 禁降 5 | sds.c:40-52,87,241-244,316-318; sds.h:22-27 |
| **扩容三路**: avail 够→原地 / 同型→realloc (L248) / 升级→malloc+memcpy+free ("can't use realloc" L253-254) | sds.c:217-268 |
| **伪降型缩容**: use_realloc = 同型\|\|(降型>8) — 保留旧头只缩 alloc, s[-1] 不更新 (L327-343 推演+harness 实证) | sds.c:327-343 |
| **预分配**: greedy <1MB→2× / ≥1MB→+1MB (摊还 O(log n) realloc); NonGreedy=querybuf 读取按需防恶意膨胀 | sds.c:232-237,277-279; networking.c:2401,2698 |
| **usable 接力**: 创建/扩容 alloc=分配器实际 (免费膨胀 L93-105); sdsResize je_nallocx 预查询免无谓 realloc | sds.c:93-105,332-338 |
| **双标准**: 总 \0 结尾 (printf 兼容) + len 为准 (二进制安全, 中间 \0 合法) | sds.c:73-80,142,463-472 |
| **零拷贝**: sdsIncrLen (MakeRoomFor→read 直写→递增, 断言守卫; 负增量去 CRLF) — querybuf 消费 | sds.c:385-440; networking.c:2431,2727 |
| **复用三件**: sdsclear (aof_buf 每轮复用 aof.c:1216) / SDS_NOINIT 免 memset / 导出分配器 (Lua 宿主) | sds.c:200-203,97-98; sds.h:256-258 |

### R-4 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2006 (antirez) | SDSLib 初版 (Redis 最老组件) |
| 早期 | 5 类型分级定型; sdshdr5 退化为"仅标志" (注释 "never used") |
| 3.2+ | sdsResize/NonGreedy (缩容重构 + 非贪心追加) |
| 后期 | usable 联动 (R-33 演进) + SDS_NOINIT 免 memset + 导出分配器 (Lua 集成) |

### R-4 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 通过项 | 全部锚点行号 grep 验证 (40 处); harness 16/16 + ASan 零越界; 伪降型源码推演 (L327-343: s[-1] 不更新/oldhdrlen 计算) 与 harness 互证; would_regrow=0 允许降到 5 (object.c:601) 已如实记录 | 记录 |
| 2 | harness 简化标注 | sdsResize 简化禁止降到 5 (真实 would_regrow=0 可降) | harness 注释补注 |

### R-4 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | **编造** | 引出桥 "sds 的哈希缓存 (sdshash)" — **不存在** (grep 零命中); dict 键哈希 = siphash 对 sds 内容计算 (dict.c:105-113) | 大纲引出+KP+HANDOFF §三 3 处修正 |
| 4 | 精确化 | q4 readlen = PROTO_IOBUF_LEN 16KB (server.h:164), 部分场景 remaining/MASTER 扩容 (networking.c:2667-2687) | 闭环 q4 补全 |
| 5 | 通过项 | 行号 40/40 ✅; sdscpylen 覆盖语义确认 (负面空间"不做惰性拷贝"成立, sds.c:496-503); 版权 2006-Present ✅ | 记录 |

### R-4 负面空间 (07 维度5)

不做惰性拷贝 (sdscpy 直接覆盖, 无 COW) / 不做内存池 (释放块不缓存, 分配走 R-33) / 不做自动压缩 (sdsclear 保留 alloc, 紧凑需显式 sdsResize) / 不做编码检测 (纯字节) / 不做线程安全 (无锁, 主线程单写)。

---

## §五 R-3 已交付内容速查 (Dict)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **双表渐进**: ht_table[2]+rehashidx 逐桶迁移; 空桶扫描 n*10 上限; 完成=旧表释放+新表提升 | dict.h:96-102; dict.c:312-414 |
| **触发阈值**: 1:1 扩 / <1:8 缩 (ENABLE); AVOID: 4:1 扩/1:32 缩; **COW 三态**: fork→FORBID / 子进程→AVOID / 正常 ENABLE (updateDictResizePolicy) | dict.c:1492-1550; server.c:640-652 |
| **联动迁移**: 查找时目标桶未迁且非空→_dictBucketRehash (缓存友好); 双表查找 | dict.c:736-770,472-489 |
| **2 幂掩码**: 扩容重算 hash / **缩容 idx&新掩码免重算** (低 j 位不变) | dict.c:320-327 |
| **单指针 entry**: 低 3 位编码 key/normal/no-value; **sds 恒奇** (奇数头 1/3/5/9/17B+malloc 对齐) | dict.c:128-171; server.c:474-475 |
| **dictScan 反向游标**: rev+1+rev; **弱语义: 迭代前键不丢/重复允许/中途插入可漏** (SCAN 文档) | dict.c:1369-1470 |
| **dictType 定制**: hash/cmp/dup/free 注入 + 7.x no_value/keys_are_odd/storedKey API; 联合值整数免分配 | dict.h:32-92; dict.c:850+ |
| **SipHash**: 16B 随机 seed (main getRandomBytes); 2014 HashDoS 后引入; nocase 变体 (命令表) | dict.c:92-113; server.c:6985-6987 |

### R-3 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 早期 | 双桶渐进 rehash 定型 (头部注释); DICT_HT_INITIAL_EXP=2 |
| 2014 | **SipHash 切换** (HashDoS 攻击潮) |
| 2013 | dictScan 反转游标 (antirez) |
| 7.x | ht_size_exp 指数化重构; **no_value/keys_are_odd 单指针 entry**; storedKey API; rehashingCompleted/onDictRelease 回调; COW 三态 (fork 保护) |

### R-3 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **语义修正 (harness 实证)** | dictScan "不丢不重" 表述错误 — 真实 SCAN 弱语义: **迭代前键不丢 (保证)/重复允许/中途插入可漏** (harness: 扩容后插入桶 16 的键未返回 — 合法) | 大纲节 6+闭环 q6+KP+pass1 同步修正 |
| 2 | **机制补强 (harness 实证)** | sds 恒奇机制闭环: sdshdr 奇数大小 (1/3/5/9/17B) + malloc 偶数对齐 → sds 指针恒奇 — keys_are_odd 成立依据 | 闭环 q5 补全 |
| 3 | harness 迭代 | 缺 "不在 rehash 返回 0" 守卫 (真实 L389) / 迁移中新增应插新表 ht[1] (真实 dictAddRaw) / 双表查找 (真实 L758-770) / rev 64 位全宽 — 4 处修正后 16/16 PASS | harness 修正 |

### R-3 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **完整性缺口** | pauserehash 冻结面未覆盖 — 两阶段删除 (dictTwoPhaseUnlinkFree L815,833: 定位→暂停→删→恢复) + 安全迭代器生命周期冻结 (L1034) + 非安全迭代器 fingerprint 检测 (L950-1010) | 大纲节 1+KP q1 补全 |
| 5 | **表述不精确** | 节 5 "key 直接入桶" — 实为**桶空才直存** (无 next 可链), 桶非空退回 createEntryNoValue (dict.c:328-349) | 大纲节 5+闭环 q5+KP 修正 |
| 6 | 开篇误导 | "内存快满时哈希表不扩容" — AVOID 实为子进程 (COW) 场景 | 读者处境改为 "RDB 保存时" |
| 7 | 通过项 | 行号 33/33 ✅; AVOID 4:1 条件逐字确认 (L1509-1513: `ht_used >= 4*size`); server.c:6985-6987 getRandomBytes ✅ | 记录 |

### R-3 负面空间 (07 维度5)

不做完美哈希 (链式冲突, 负载因子控制) / 不做并发安全 (无锁, 单线程) / 不主动缩容 (删除路径触发) / 不做迭代器快照 (fingerprint 防误用, 非快照语义) / 不做内存池 (entry 走 zmalloc)。

---

## §六 R-19 已交付内容速查 (listpack)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **布局**: 6B 头 (4B total+2B numele) + entry* + 0xFF EOF; entry=[编码+数据][backlen 自身长度 1-5B] | listpack.c:22-27,869-872 |
| **无级联 (核心卖点)**: backlen 存自身长度+固定 5B 空间 — 插入/替换只改头部, 后驱零影响; **对照 ziplist prevlen 存前驱+1B/5B 可变→级联** (ziplist.c:55-69); REDIS-PLAN "级联更新"表述错误已修正 | listpack.c:452-482; ziplist.c:55-69 |
| **编码族 9 种**: 整数 7/13/16/24/32/64BIT (2-10B) + 字符串 6/12/32BIT (1-5B 头); 前缀分层 0/10/110/1110/1111 无歧义解码 | listpack.c:30-82,434-446 |
| **整数嗅探**: lpEncodeGetType→lpStringToInt64 (string2ll 移植, 严格语义: 无空格/无前导零) — "123"→7BIT_INT 省 50%+ | listpack.c:154-179,317,850-860 |
| **lpInsert 三合一**: 删除=替换零长 (where 强制 REPLACE); LP_AFTER 转 BEFORE; **扩先 realloc 后 memmove / 缩先 memmove 后 realloc** (harness 迭代实证); UINT32_MAX 上限+1GB 安全线 | listpack.c:821-968,122-128 |
| **双向遍历**: lpPrev O(1) (前驱 backlen 紧贴当前 entry 前 → 解码跳回); lpSkip 前进 | listpack.c:452-482 |
| **批量**: lpBatchInsert 单次 realloc+memmove (O(N) vs O(N²)); 栈 3 元素缓冲 | listpack.c:993-1080 |
| **惰性计数+完整性**: numele 超 65535 (UNKNOWN) 全扫描+回填; lpValidateIntegrity 头部一致性+deep 逐元素 (RDB 加载防御) | listpack.c:27,505-521,1541-1553 |
| **消费阈值**: hash ≤512 字段/值≤64B, zset ≤128/64B (config.c:3215-3223, 旧名 ziplist 兼容); 超阈值单向转 dict; stream 消息 | config.c:3215-3223; t_hash.c:487,605,893 |

### R-19 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2017 (antirez) | listpack 独立项目 (github.com/antirez/listpack) — 设计目标: 替代 ziplist, 消除级联更新 |
| 7.x | ziplist 全面退役 → listpack 全切换 (hash/zset/list/stream); 配置别名 hash-max-ziplist-entries 兼容 |
| 遗留 | #if 0 调试块 (强制新指针抓 bug); lpStringToInt64 移植自 2011 utils.c string2ll |

### R-19 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | REDIS-PLAN "级联更新"表述错误 — listpack **无级联** (backlen 存自身+固定 5B); ziplist 才有级联 (prevlen 1B/5B) | pass1/KP/大纲全量修正 |
| 2 | harness 迭代 | 缩容顺序错误 (真实: 先 memmove 后 realloc L911-914) / 缩容 realloc 后 dst 未更新 (真实 L913) — 2 处修正后 16/16 PASS | harness 修正 |
| 3 | 通过项 | 消费阈值 config.c:3215-3223 (512/128/64B) 验证; numele 回填 L520; ziplist 级联对照 L55-69 | 记录 |

### R-19 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | 行号偏差 | lpEncodeGetType 实际在 **L317** (大纲/闭环/KP/HANDOFF 4 处写 660+) | 全量修正 |
| 5 | 行号偏差 | LP_MAX_BACKLEN_SIZE 实际在 **L29** (闭环 q1 写 L37) | 修正 |
| 6 | 表述 | 读者处境 "100 字段只占几十字节" 夸张 — 实为几百字节~KB 量级 | 改为 "几 KB" |
| 7 | 通过项 | 行号 34/34 验证 ✅; backlen"自指"语义再确认 (lpSkip L453-455 用自身长度跳过 — 后驱无感知 = 无级联的根本); 前缀分层 0/10/110/1110/1111 逐字确认 | 记录 |

### R-19 负面空间 (07 维度5)

不做 O(1) 随机访问 (线性扫描, 小规模专用) / 不做级联安全 (根本无级联) / 不做原地更新 (变长=整体搬移) / 不做压缩 (无 LZF, 对照 quicklist R-5) / 不存嵌套结构 (只字符串/整数)。

---

## §七 R-7 已交付内容速查 (intset)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **编码分级**: encoding=元素字节宽 2/4/8; 值域判定 ±32767→16/±2^31→32/其他→64 | intset.c:41-53 |
| **升级机制**: 值域超界→intsetUpgradeAndAdd (新值必在极值: 负数头插/正数尾插, 免二分); **从后往前搬防覆盖** (新宽度); O(n) 单调升级 | intset.c:159-182,214-216 |
| **二分查找**: O(log n) + 首尾快速路径 (值>max 插尾/<min 插头); MoveTail memmove 按宽度 | intset.c:117-156,184-203 |
| **升而不降**: 删除只缩元素数不缩编码 (降级需全量值域验证+可能回弹); 与 listpack→dict 单向同哲学 | intset.c:236-253 |
| **字节序**: memcpy+memrevXXifbe 统一小端 (RDB 可移植+免未对齐); 头部 intrev32ifbe | intset.c:56-95 |
| **完整性**: encoding 合法/大小精确一致 (count×宽度==blob)/非空; deep 严格递增无重复 — RDB 防御 | intset.c:302-343 |
| **消费**: set-max-intset-entries=512 (CONFIG 可调); setTypeAdd 双条件 (整数性+≤512); 超限单向转 dict | config.c:3216; t_set.c:26,42 |

### R-7 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2009-2012 (Pieter Noordhuis) | intset 初版 (版权头) — 560 行稳定至今 |
| 7.x | intsetValidateIntegrity 新增 (RDB 加载防御, 与 listpack 同族) |

### R-7 深审记录

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 通过项 | 全部锚点行号 grep 验证; 升级"极值特权"数学依据 (新值超值域→必在极值); 升而不降成本分析 (推断标注); 与 R-19 单向转换同哲学对照 | 记录 |

### R-7 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 2 | 行号偏差 | struct intset 定义在 **intset.h:35-39** (大纲/闭环误写 13-17, 那是版权注释区) | pass1+大纲 2 处修正 |
| 3 | 严谨性 | 节 4 "升而不降" 成本分析未标注推断性 (代码无降级路径是事实, 为何不做是推断) | 大纲节 4 补标注 |
| 4 | 通过项 | 行号 26/26 验证 ✅; 升级"从后往前搬"防覆盖机制再确认 (新宽度位置与旧数据重叠 → 反向搬安全); prepend=value<0 边界 (0 永不触发升级) | 记录 |

### R-7 负面空间 (07 维度5)

不做降级 (升而不降) / 不存字符串 (非整数→dict) / 不做 O(1) 插入 (memmove O(n), 小规模专用) / 不做压缩 / 不做删除缩容优化。

---

## §八 R-6 已交付内容速查 (skiplist+ZSet)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **双结构**: zset{dict: member→score O(1), zsl: 有序 O(log n)}; 写路径双写 (zslInsert+dictAdd / zslDelete+dictDelete) — 单线程天然一致 | server.h:1357-1360; t_zset.c:1425-1530,1589-1600 |
| **层级概率**: P=0.25 几何分布 (threshold=P*RAND_MAX), 期望 1.33 层 (省指针); MAXLEVEL 32 = log_4(2^64) 理论上限 | t_zset.c:126-132; server.h:514-515 |
| **span 距离索引**: 每层存跨越节点数 → 排名=路径 span 累加 O(log n); 插入 rank 差 O(1)/层维护 | server.h:1345-1348; t_zset.c:171-183,508-545 |
| **复合排序**: (score, ele) 全序 — 同分 sdscmp 字典序; 命令确定性 | t_zset.c:147-150 |
| **zslInsert**: update[]+rank[] 双数组 (一次遍历双信息); 新层 span=length; 删除 level 惰性收缩 | t_zset.c:137-192,196-214 |
| **范围查询**: IsInRange 首尾 O(1) 判空; **zslNthInRange 统一首/末/偏移 (7.x 替代 First/LastInRange)**; ZSKIPLIST_MAX_SEARCH 小偏移优化 | t_zset.c:317-410,3345-3451 |
| **命令面**: zsetAdd 双编码透明 + NX/XX/GT/LT/INCR + NaN 全局守卫; score 变更先删后插 | t_zset.c:1425-1530 |
| **转换**: listpack→skiplist: **dictExpand 预扩免 rehash** + 逐元素双写; 单向 (≤128/64B 阈值) | t_zset.c:1265-1300; config.c:3219 |

### R-6 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 早期 | zset 双结构定型; skiplist 无 span (排名 O(n)) |
| span 引入 | ZRANK/ZREVRANK 提速至 O(log n) |
| 7.x | zslNthInRange 统一首/末/偏移 (替代 FirstInRange/LastInRange); zslGetElementByRankFromNode 排名差定位; listpack 替代 ziplist 小规模编码 |

### R-6 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | pass1 写 zslFirstInRange/zslLastInRange (L345+) — **7.x 已移除**, 由 zslNthInRange (n=0/-1/offset) 统一 (t_zset.c:336) | pass1 修正 |
| 2 | harness 实证 | P=0.25 层级分布实测 1.334 (理论 1.333); 同分字典序 (banana 3/cherry 4); span 排名; 范围判空/开区间 — 8/8 PASS | harness |
| 3 | 通过项 | 锚点行号全 grep 验证; 双写一致性 (单线程无锁) 分析; 转换预扩 (dictExpand cap) 语义 | 记录 |

### R-6 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | 行号偏差 | NaN 校验在 **L1435-1436** (大纲/闭环写 L1442-1445 — 那是 listpack 分支) | 大纲节 7+闭环 q7 修正 |
| 5 | 行号偏差 | dictExpand 转换预扩在 **L1292** (写 L1283-1285); **创建路径预扩 L1248 (zsetTypeCreate size_hint) 遗漏** — 两条预扩路径 | 大纲节 8+闭环 q8+KP 修正 |
| 6 | 精确化 | ZSKIPLIST_MAX_SEARCH 值 **=10** (server.h:516) 未给出 | 大纲节 6 补全 |
| 7 | 通过项 | 行号 32/32 验证 ✅; zslGetRank 的 `<=0` vs zslInsert 的 `<0` 复合比较语义确认 (GetRank 前进后每层检查命中, 正确累计排名); P=0.25 期望 1.33 数学 (几何分布 E=1/(1-P)); MAXLEVEL 32=log_4(2^64) | 记录 |

### R-6 负面空间 (07 维度5)

不用红黑树/AVL (实现复杂+范围遍历需中序 — 工程选择) / 不做 skiplist 降级 (转换单向) / 不做原地更新 (score 变更先删后插) / 不做 P=0.5 (内存敏感省指针) / 不做跨 key 排序结构 (ZUNION 用临时结构)。

---

## §九 R-5 已交付内容速查 (quicklist)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **分页链表**: quicklistNode (位域 count:16/encoding:2/container:2/recompress 等) + quicklist (fill:16/compress:16/bookmark_count:4) | quicklist.h:47-59,101-112 |
| **双容器**: PACKED (listpack) / **PLAIN (大元素裸节点)** — isLargeElement (超 fill 字节限) → __quicklistInsertPlainNode | quicklist.c:508-519,571-603 |
| **fill 双语义**: 正=元素数 / 负=字节数 (optimization_level {4K..64K} 2^k 映射, **默认 -2=8KB**) | quicklist.c:462-482; config.c:3152 |
| **压缩三条件**: 两端 compress 深度外 + ≥48B (MIN_COMPRESS_BYTES) + 收益 ≥8B (MIN_COMPRESS_IMPROVE) 且 lzf 成功; 默认 compress=0 | quicklist.c:78,83,214-252,307-345; config.c:3174 |
| **recompress 延迟重压**: 读时解压+标记, 批量后统一重压; 头尾永不压缩/重压 | quicklist.c:260-290,311-312,380-390 |
| **分裂**: 复制整包 + lpDeleteRange 双侧裁剪 (extent -1=到结尾) — 3×O(n) 换极简 | quicklist.c:971-1004 |
| **三路路由**: PLAIN / 就地追加 / 新节点 (+相邻合并); SIZE_SAFETY_LIMIT 防单节点爆 | quicklist.c:521-603 |
| **迭代器**: 自动解压 (LZF 透明) + 双向 + resetIterator 变更失效 | quicklist.h:114+; quicklist.c:720+ |
| **演进**: 2014 quicklist 替代 linkedlist+ziplist 混合; 7.x 容器→listpack; bookmarks 超大列表锚点 (零默认开销, ≤16) | quicklist.h:70-112 |

### R-5 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 3.2 前 | linkedlist (大) 或整体 ziplist (小) — 二选一无中间态 |
| 2014 (Matt Stancliff) | quicklist 引入 (双向链表+每节点打包) |
| 2015+ | per-node LZF 压缩 + 深度策略 |
| 7.x | 容器 ziplist → listpack; #if 0 显式深度分支被通用迭代替代 |

### R-5 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | [4] 分裂测试对象错误 (head 是 count=1 新节点, 分裂条件 count<2 提前返回) — 改为遍历找可分裂节点 | harness 修正, 11/11 PASS |
| 2 | 通过项 | 锚点行号全 grep 验证; MIN_COMPRESS_BYTES=48/MIN_COMPRESS_IMPROVE=8 常量; optimization_level 表; isLargeElement 判定 | 记录 |

### R-5 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | 精确化 | SIZE_SAFETY_LIMIT 值 **=8192** (L69) 未给出 | 大纲节 6 补全 |
| 4 | 行号精确化 | quicklistBookmark 定义在 **L79-87** (写 70-112 含注释区) | 大纲节 8+KP 修正 |
| 5 | 完整性 | **list 当前仅 quicklist 一种编码** (t_list.c:52) 未明示 — 7.x 无其他 list 编码 | 大纲节 8 补全 |
| 6 | 通过项 | 行号 28/28 验证 ✅; 分裂 after/offset 语义 (L985-990: extent -1=到结尾) 再确认; listTypeTryConversionRaw (t_list.c:110) 转换面确认 | 记录 |

### R-5 负面空间 (07 维度5)

不做单节点无限增长 (fill+SIZE_SAFETY_LIMIT 双上限) / 不做全量压缩 (只压中间区) / 不做压缩保证 (收益不足放弃) / 不做 O(1) 随机访问 (LINDEX O(n), 对照 R-6 跳表) / 不做 bookmarks 默认 (超大列表专属)。

---

## §十 R-1 已交付内容速查 (redisObject)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **16B 外壳**: type:4+encoding:4+lru:24+refcount+ptr; OBJ_STATIC/SHARED_REFCOUNT 特殊值 | server.h:896,901-911 |
| **EMBSTR 同 chunk**: robj+sdshdr8+buf 一次分配; **16+3+44+1=64B 恰好 jemalloc 64B 桶** (L99-101); 不可变 (追加转 RAW) | object.c:71-107 |
| **INT 零分配**: ptr 存值; 共享整数 (<10000) 优先; **maxmemory 禁共享** (NO_SHARED_INTEGERS, 私有 LRU 需要 L627-635) | object.c:128-140,627-635 |
| **优化链**: tryObjectEncodingEx — refcount>1 跳过 → INT (≤20 字符 string2l) → 共享/EMBSTR (≤44B); 写入时 O(1) | object.c:607-683 |
| **引用计数三态**: 1→分派释放 (freeXxxObject) / >1→-- / 特殊值不碰; makeObjectShared | object.c:56-60,349-377 |
| **共享池**: shared.integers[10000] (INT+makeObjectShared, server.c:1992-1995) + 响应串族 (ok/wrongtypeerr/oomerr...) L1847+ | server.c:1847+,1992-1995; server.h:108 |
| **解码/LRU**: getDecodedObject 按需临时 (INT→ll2string); 比较直接比值免解码; lru 24bit 双用途 (LRU 时钟/LFU 频率+时间); 共享无 lru | object.c:32-43,685-715 |
| **可观测**: objectCommand ENCODING/REFCOUNT/IDLETIME/FREQ | object.c:1442+ |

### R-1 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2010 初版 | robj 三字段 + refcount |
| 2013+ | EMBSTR 引入 (44B=64B arena 设计) |
| 演进 | LRU_BITS 22→24; LFU 模式 (8bit 频率+16bit 时间) |
| 长期稳定 | OBJ_SHARED_INTEGERS=10000; 响应串族持续扩充 |

### R-1 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | [4a] 测试逻辑错 ("12345"≥10000 不共享, 改 "5"); [5] freed_count 混计数 (拆 ptr_freed/obj_freed); freeStringObject 对 EMBSTR 误 free 内部指针 — 3 处修正后 14/14 PASS | harness 修正 |
| 2 | 通过项 | 锚点行号全 grep 验证; 64B 数学 (16+3+44+1) harness 实证; shared.integers 创建 (server.c:1992-1995) 确认; NO_SHARED_INTEGERS (server.h:559) | 记录 |

### R-1 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | 行号偏差 | 优化链共享条件在 **L636-643** (写 627-635); EMBSTR 尝试在 **L657** (写 654+) | 大纲节 3/4 + 闭环 q3/q4 + KP 修正 |
| 4 | 机制补充 | 共享条件**双路径**未写全: 创建路径 (createStringObjectFromLongLongWithOptions L159) + 优化链 (L638) | 大纲节 3 补全 |
| 5 | 精确化 | TYPE 命令实现在 **db.c:1336** (非 object.c) — OBJECT 族与 TYPE 分离 | 大纲节 8 补全 |
| 6 | 通过项 | 行号 28/28 验证 ✅; harness 14/14 (共享边界 "5"<10000 实证); 16+3+44+1=64 数学再确认 | 记录 |

### R-1 负面空间 (07 维度5)

不做可变 EMBSTR (同 chunk 不可变) / 不做共享对象修改 (OBJ_SHARED_REFCOUNT 只读) / 不做解码缓存 (INT 字符串形态临时生成) / 不做对象池复用 (外壳不回收) / 不做用户自定义类型 (仅内置 7 类+Module)。

---

## §十一 R-20 已交付内容速查 (server 骨架, 大域 2 篇)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **main 管线** (篇 1): 依赖序 — OOM handler L6970 → 哈希 seed L6985 → **哨兵先于配置** L7006-7012 → 配置 → initServer → aeMain L7251 | server.c:6917-7256 |
| **initServer 矩阵** (篇 1): 信号/事件循环 L2657 / **键空间 kvstore 分片 L2665-2680 (cluster 14bit)** / list 族 / cron 注册 L2757 | server.c:2591-2772 |
| **配置宏 DSL** (篇 1): createIntConfig 族 L2244+ (类型/范围/默认/验证/apply 五合一; 53 Bool+41 Int+36 String+20 Enum+13 SizeT+9 Special); **CONFIG SET 失败回滚 restoreBackupConfig L760-780** | config.c:2244+,760-780 |
| **生产面** (篇 1): systemd READY L7230 / CPU 亲和 L7250 / OOM score L7251 / **watchdog 卡死检测 L1281** (cron 缺席→SIGALRM→栈 dump) | server.c:1281,2232,6783,7230-7251 |
| **serverCron 时间分级** (篇 2): 每 tick (clientsCron/databasesCron/updateDictResizePolicy) / 100ms (复制/模块/抽样) / 1s (receiveChildInfo/tracking) / 5s (日志); **返回 1000/hz L1538** | server.c:1273-1540 |
| **hz 自适应** (篇 2): **每 tick 重置 config_hz → clients/hz > 200 (MAX_CLIENTS_PER_CLOCK_TICK) → hz×2**; 上限 500 (harness 实证: 10000 clients→80, 回落 10) | server.c:1282-1295; server.h:100-103 |
| **命令表** (篇 2): commands.def (11235 行/122 命令, 生成式) → populateCommandTable **双字典注册 (commands/orig_commands, rename 免疫)** L3075-3095 + sentinel 过滤/ACL 分类 L3032+ | commands.c/def; server.c:3032-3095 |
| **beforeSleep** (篇 2): 每轮 AOF flush/客户端待写/阻塞键 L1637+; **cron=定时 vs beforeSleep=事件驱动双面** | server.c:1637-1800,2772 |

### R-20 时空溯源 (代码内痕迹)

| 时期 | 机制演变 |
|:--|:--|
| 2009 | main/initServer 骨架定型 (稳定至今) |
| 演进 | 配置族重构 (createXxxConfig 统一宏); hz 自适应 (dynamic_hz); 命令表 JSON 生成 |
| 7.x | **键空间 dict → kvstore 分片** (cluster 14bit); hexpires (ebuckets) |

### R-20 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | [5] hz 回落模拟错 — 真实语义是**每 tick 先重置 config_hz** (L1283) 再上调 | harness 修正, 5/5 PASS (回落实证) |
| 2 | 通过项 | 锚点行号全 grep 验证; 命令表 122 命令 (commands.def 计数); 配置族统计 (53/41/36/20/13/9); CONFIG SET 回滚机制确认 | 记录 |

### R-20 二次深度 REVIEW (2026-08-13, 07 五维度 + 六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | 行号偏差 | hz 重置在 **L1283** (写 L1282); 返回 1000/hz 在 **L1538** (写 L1537) | 全量修正 (大纲/KP/HANDOFF/harness 注释) |
| 4 | 行号精确化 | 哨兵块在 **L7006-7012** (写 7003-7012 含 exec_argv) | 修正 |
| 5 | 通过项 | 行号 35/35 验证 ✅; hz 重置语义 (每 tick L1283) + 返回 L1538 再确认; READY L7233/7239; hashseed L6985-6986 | 记录 |

### R-20 负面空间 (07 维度5)

不做并行初始化 (全串行) / 不做配置热加载 (CONFIG SET 才动态) / 不做优雅降级 (初始化失败即退出) / 不做实时调度 (尽力而为) / 不做多线程 cron (单线程串行)。

---


---

## §十二 下一步 — R-12 (HyperLogLog) 详案

**源码**: `src/hyperloglog.c (1597)` — 稀疏/稠密/16384 寄存器

**方案**: 🟡 B (Pass 0-3 + 时空溯源, 无 harness)。

**已知连接点** (直接引用, 无需导航):
- R-11 (已交付): **精确 vs 近似计数对照** — popcount (精确) vs HLL (概率); 字符串承载 (HLL 是特殊编码的字符串)
- R-1 (已交付): 字符串编码 + OBJ_ENCODING_RAW 特殊用途 (HLL 用 RAW 字符串 + magic 头)
- R-24 (已交付): 字符串命令面 (HLL 复用字符串存储)

**关键机制面** (Pass 1 起点): hllAdd (寄存器更新: 前导零计数 max) / hllCount (调和平均: alpha_m × m² / Σ 2^-M) / **16384 寄存器 (2^14) × 6bit = 12KB** (HLL_REGISTERS=16384, HLL_P=14) / 稀疏编码 (opcode 0-3 字节) / 稠密编码 (6bit 打包) / 稀疏→稠密转换 / 0.81% 误差 (1.04/√m) / PFADD/PFCOUNT/PFMERGE / magic "HYLL" + 64bit 计数缓存

**流程提醒**: 🟡 B → 六层深审必须真找问题 (寄存器数字/稀疏 opcode/误差公式穷举) → 无 harness → 全量回归 → 更新本文 §零 + HANDOFF-STAGE3 §零。

---

## §十二·附 R-11 (Bitmap) 已交付速查 (2026-08-13)

**源码**: bitops.c (1269) → 🟡 B, 单篇 (outline.md 83 行) + 6 闭环, 行号验证 56 处

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **redisPopcount**: 256 查表 (L23) + **SWAR 28 字节批** (L33-66: 0x55555555 每 2 位 → 0x33333333 每 4 位 → 0x0F0F0F0F×0x01010101 累加); 对齐前缀 (L26-29) + 尾查表 (L68-69); 上限 512MB | bitops.c:19-71 |
| **redisBitpos**: 字对齐跳过 (L98-121, 找 0 跳全 1 字/找 1 跳全 0 字); 尾字大端组装 (L130-138) + MSB 扫描 (L151-159); **特殊值: 全 0 找 1 → -1 (L145) / 找 0 恒成功 (零填充假设)** | bitops.c:80-165 |
| **BITFIELD 位宽整数**: iN/uN ≤64 (L429-459); set/getUnsigned/Signed 掩码+符号扩展 (L188-265); **溢出三模式** (WRAP 截断低 bits L295-300 / SAT 钳制 L281,288 / FAIL); 64 位特判 (UINT64_MAX L269/INT64_MAX L306 防移位 UB) | bitops.c:188-360,1032-1262 |
| **SETBIT/GETBIT**: MSB 优先定位 (byte>>3 L535 / bit=7-(off&7) L537); **dirty 三条件** (L540: 新键/扩容/值变化); 扩容零填充 (lookupStringForBitCommand L460); 返回旧值 (L555) | bitops.c:393-491,511-585 |
| **BITOP**: AND/OR/XOR/NOT (L598-605) + **NOT 单键限制** (L611-612); **maxlen 语义** (maxlen L592/minlen L594: 短键零填充); AND 零字节短路; 传播原样 | bitops.c:586-774 |
| **BITCOUNT/BITPOS**: start/end + BIT/BYTE 单位 (L783-786); **首尾掩码** (声明 L782, 免子串拷贝) + 整字节 popcount (L852) + 调整 (L860); 负索引 R-24 同款 | bitops.c:775-1031 |

### R-11 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | SETBIT/GETBIT/BITCOUNT (查表) |
| 2.6 | BITOP |
| 2.8 | BITPOS (字对齐跳过) |
| 3.2 | BITFIELD (位宽整数 + overflow) |
| 4.0+ | BITCOUNT 范围单位 |

### R-11 深审记录 (六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **行号偏移** | setbitCommand 位定位实测 **L535-537** (初稿 L531-534 是 dirty 声明区); BITOP 操作解析 **L598-605**/NOT **L611-612**/maxlen **L592**; BITCOUNT 掩码 **L782**/popcount **L852**/调整 **L860** | 大纲节 4/5/6 修正 |
| 2 | 通过项 | 行号穷举 56 处验证 (23 个区间覆盖无偏差); SWAR 掩码/bitpos 特殊值/溢出三模式/64 位特判/MSB 位序/dirty/maxlen/BIT-BYTE 全验证 | 记录 |
| 3 | 推断标注 | SWAR 提速量级 / BITFIELD 动机 / dirty 占比 — 3 处显式标注 | 记录 |

### R-11 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **编造** | 节 5 "AND 遇 0 字节提前" — **不存在短路**! 实测 AND/OR/XOR 是**字宽批量运算** (L672+: 每轮 4×sizeof(ulong), lp+4) 无零检查提前退出 | 大纲节 5/关键设计 + pass2-q5/questions 全量修正 |
| 5 | **行号偏移** | 溢出特判: UINT64_MAX **L268** (写 269) / INT64_MAX **L305** (写 306) | 大纲节 3 修正 |
| 6 | 精确化 | 节 4 扩容: 新键 sdsnewlen (L473-474) / 已存在 dbUnshare (L484) + sdsgrowzero (L486) | 大纲节 4 补精确行号 |
| 7 | 通过项 | 前置依赖拓扑合规 (R-24/R-1/R-4) ✅; 锚点 ~30 (标准 ≥4) ✅; 负面空间/开篇 ✅; 其余 ~40 句逐句对源码一致 ✅ | 记录 |

### R-11 负面空间 (07 维度5)

不做位压缩 (RLE, 对照 R-12 稀疏) / 不做跨键视图 (BITOP 物化) / 不做 64+ 位宽 / 不做自动紧凑 / 不做原子多字段。

---

## §十二·附 R-27 (t_set) 已交付速查 (2026-08-13)

**源码**: t_set.c (1658) → 🟡 B, 单篇 (outline.md 86 行) + 6 闭环, 行号验证 82 处

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **三编码**: intset (INT 且 ≤512, L26-27) / **listpack (≤128/64B, L28-29 — 7.x 新增!)** / HT (dictExpand 预扩 L33-35); set-max-listpack-entries=128 ≠ hash 512 (config.c:3217); intset 1<<30 上限 (L52) | t_set.c:25-54; config.c:3216-3218 |
| **双向转换 (Redis 唯一)**: intset 非整数 → listpack 中间态 (L181-194, lpShrinkToFit) 或 HT (L196-200); **HT/listpack → intset 降级** (maybeConvertToIntset L66-88, **sinterstore 全整数结果 L1392**) — 与 hash/list 单向形成对比 | t_set.c:57-88,169-202,1392 |
| **写入**: dictFindPositionForInsert 预定位插入 (L124-128, 一次哈希); lpAppendInteger 整数直插 (L148); sds 复用免复制 (L122) | t_set.c:104-208 |
| **集合运算**: sinter 空集短路 (L1275-1300) + 最小集 qsort 策略 (L1229,1300+); SINTERCARD 基数+LIMIT (L1424); sunion/sdiff 迭代 (L1460) | t_set.c:1229-1460 |
| **随机命令**: SPOP COUNT 双语义 (L750-760: 正可重/负不重); 大 set 采样/全排双策略 (L840+, count vs size/10); 弹空删键 (L900+, "空集不存在" R-26 同款); SRANDMEMBER 同构不删 (L998) | t_set.c:407-457,739-1203 |
| **命令面**: SADD size_hint 预判创建 (L592) + 预转换 (L596); SREM/SMOVE 空集删键 (L620-624,665-669); src==dst 短路 (L654) | t_set.c:583-738 |

### R-27 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | intset+HT 双编码初版 |
| 2.8+ | SPOP/SRANDMEMBER COUNT; sinter 最小集 |
| 6.0+ | SINTERCARD/SMISMEMBER |
| 7.x | **listpack 编码加入**; **HT→intset 降级**; dict 预定位插入 |

### R-27 深审记录 (六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **机制发现** | PLAN 写 "intset↔dict 双编码" — 实为**三编码** (intset/listpack/HT, set-max-listpack-entries=128 config.c:3217); **双向转换** (HT→intset 降级 L1392) 是 Redis 唯一支持降级的容器 | pass1/大纲/闭环全量按三编码+双向重写 |
| 2 | 通过项 | 行号穷举 82 处全验证 (12 个区间覆盖无偏差); 数字: 512/128/64/1<<30 全 ✅; 预定位插入/lpShrinkToFit/空集短路/最小集/COUNT 双语义/采样阈值 全验证 | 记录 |
| 3 | 推断标注 | listpack 128 动机 / 降级动机 / sinterstore 低频 — 3 处显式标注 | 记录 |

### R-27 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **表述错误 (重要)** | 节 4 "SPOP COUNT 正负" — **完全错误**: SPOP COUNT **仅正数** (L742 getPositiveLong); count>=size → 返回全部+删键+DEL/UNLINK 重写 (L773-787); 双策略是 **SPOP_MOVE_STRATEGY_MUL=5** (L737) 非 size/10; **正负语义属于 SRANDMEMBER** (L1001-1016: 正=uniq 不重复/负=可重复) 方向与初稿相反 | 大纲节 4 + pass2-q4 全量重写 |
| 5 | 通过项 | 前置依赖拓扑合规 (R-7/R-19/R-3/R-21) ✅; 锚点 ~32 (标准 ≥4) ✅; 负面空间/开篇 ✅; 其余 ~40 句逐句对源码一致 (含 sinter 最小集 qsort L1300+/sinterstore 降级 L1392/SPOP 传播 SREM 批 L786-789) ✅ | 记录 |

### R-27 负面空间 (07 维度5)

不做有序集合 (ZSet R-6) / 不做成员级 TTL (对照 HFE R-25) / 不做集合引用计数 (SINTERSTORE 独立拷贝) / 不做流式集合运算 / 不做 SPOP 保序。

---

## §十二·附 R-26 (t_list+blocked) 已交付速查 (2026-08-13)

**源码**: t_list.c (1364) + blocked.c (746) → 🔴 A, 拆 2 篇 (01-list-commands.md 70 行 + 02-blocking-framework.md 66 行) + harness 16/16 (gcc+ASan)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **list 双编码**: listpack (小) / quicklist (大, -2=8KB config.c:3152); GROWING 只影响 listpack (L116-117) / SHRINKING 只影响 quicklist (L122-123) + beforeConvertCB 回调 | t_list.c:21-127 |
| **pushGenericCommand**: XX 不存在返回 0 (L470-471); 创建 createListListpackObject + dbAdd; 转换预判 (L477); **唤醒只需 dbAdd 路径** — 空键不存在不变量 (pop 空即删键 L748-750) → 阻塞者等待键必然不存在 → db.c:192 signalKeyAsReady; stream 例外 (t_stream.c:2083 显式) | t_list.c:464-492; db.c:192 |
| **popGenericCommand**: 单元素/COUNT 范围 (rangelen=min(count,llen)); 空键删键 + signalDeletedKeyAsReady (L751, XREADGROUP); mpop 多键 + 传播重写 [LR]POP COUNT | t_list.c:736-845 |
| **阻塞状态机**: btype 10 种 (server.h:398-407); blockClient (CLIENT_BLOCKED + **blocked_clients_by_type 计数** L79 + addClientToTimeoutTable L80); master 复制流不可阻 (L69-74); 超时 null 回复 (L700-708) | blocked.c:54-210 |
| **blockForKeys 双向注册**: client→key (bstate.keys) + db→client (blocking_keys 的 list); **双向节点关联** (list node 作 value, O(1) 解链 L389); unblock_on_nokey 引用计数 (L393-401); PENDING_COMMAND (L408) | blocked.c:359-410,507-540 |
| **就绪队列三级快检**: 类型可阻塞 (L451-455) / 无该类型阻塞者 (L456-463, by_type 计数 O(1)) / 键无等待者 (L465-474); **ready_keys dict 防重** (L476-486, 脚本多 push 一次唤醒) | blocked.c:430-494 |
| **消费与重处理**: 防递归 (L310-313) + **新列表交换** (L322-348, BLMOVE 连环唤醒) + FIFO (L563) + **类型匹配防误醒** (L578-580) + PENDING_COMMAND 重执行 (L648-668) | blocked.c:306-350,553-670 |

### R-26 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | LPUSH/LPOP + BLPOP 初版 |
| 2.8 | 阻塞框架重构 (blockForKeys 双向注册 + ready_keys 队列) |
| 3.2 | quicklist 引入 (R-5) |
| 6.0 | LMOVE/BLMOVE; 阻塞类型扩展 (WAIT/WAITAOF) |
| 7.x | list-max-listpack-size 别名; LIST_CONV_SHRINKING + lazyfree 联动 |

### R-26 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **机制洞察** | **唤醒只需 dbAdd 路径**: t_list.c 无 signalKeyAsReady 调用 (grep 证实) — 空键不存在不变量 (pop 空即删键) → 阻塞者等待键必然不存在 → push 走 dbAddInternal (db.c:192); stream 例外 (t_stream.c:2083) | pass2-q2 重写 (初稿误判"需继续验证") |
| 2 | harness 迭代 | [1] 误 free 栈上 Client → ASan BUS; 泄漏清理 3 轮 (bstate_keys/队列重置); [4] 消费语义修正 (**一个 push 元素只唤醒一个客户端** FIFO); [5] 类型覆盖测试改用 OBJ_STRING 信号 (快检 1 拦截) | harness 5 轮迭代, 16/16 PASS |
| 3 | 通过项 | 行号穷举 117 处全验证 (15 个区间覆盖无偏差); 数字: 10 种 btype/by_type 数组/list-max-listpack-size=-2 全 ✅; 双向注册/三级快检/防重/换列表/类型匹配/重处理/超时 全验证 | 记录 |
| 4 | 推断标注 | 空键不变量命名 / stream 例外动机 / FIFO 公平性 — 3 处显式标注 | 记录 |

### R-26 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 5 | **行号猜测** | 01 节 4 "lmoveCommand (L908+)" — 实测 **L1151**; blmoveGenericCommand **L1265-1280** 是"先试后阻"显式样板 (key==NULL → blockForKeys L1274 / 常规 L1279) | 修正 + 补样板 |
| 6 | **行号偏移** | blockClient 四行全偏一位: CLIENT_BLOCKED **L75** / btype **L76** / blocked_clients++ **L77** / by_type **L78** / addClientToTimeoutTable **L79** | 大纲 02 + pass2-q5 全量修正 |
| 7 | 通过项 | 前置依赖拓扑合规 (R-5/R-19/R-21/R-20) ✅; 锚点 25/22 (标准 ≥8) ✅; 负面空间/开篇 ✅; 其余 ~45 句逐句对源码一致 ✅ | 记录 |

### R-26 负面空间 (07 维度5)

不做双向索引 (LINDEX O(n)) / 不做消息确认 (弹走即消费, 对照 R-10) / 不做阻塞优先级 (FIFO 严格公平) / 不做多键部分唤醒 (任一触发全醒) / 不做超时精确性 (cron 粒度) / 不做跨 DB 阻塞。

---

## §十三 Redis 高频坑 (跨域 12 条, 本阶段实证)

**源码**: t_hash.c (3418) → 🔴 A, 拆 2 篇 (01-command-encoding.md 73 行 + 02-hfe-field-expire.md 69 行) + harness 30/30 (gcc+ASan)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **编码三态**: LISTPACK (两元素组) / LISTPACK_EX (三元素组+TTL) / HT (hfield 键); 阈值 512 字段/64 值 (config.c:3215,3221 旧名 ziplist 兼容); 升级单向 (HT→X panic L1675) | t_hash.c:74-130,1541-1550 |
| **hashTypeSet 三分支**: LISTPACK (lpFind/lpReplace/lpAppend + 超 512 转 HT); LISTPACK_EX (三元素组 + KEEP_TTL 保留/清 TTL); HT (hfieldNew + dictUseStoredKeyApi R-3 + TAKE_VALUE 零拷贝) | t_hash.c:855-977 |
| **转换**: 三触发 (批量字段数超 512 + dictExpand 预扩 L607 / 单值超 64 / lpSafeToAdd 总量 R-19 1GB); Listpack→EX (lpInsertInteger 扩三元素组 L1568); **ListpackEx→HT HFE 三阶段迁移** (全局摘 L1624-1625 → trash=1 标记 L1634 → 私有 hfe 重建 L1652-1653 → 全局重注 L1661-1662) | t_hash.c:594-623,1553-1679 |
| **GETF 惰性链**: 四态 OK/NOT_FOUND/EXPIRED/**EXPIRED_HASH**; 角色语义 (CLIENT_MASTER 有效/从库只报); 主库删字段+propagateHashFieldDeletion (HDEL)+notify "hexpired"+**空 hash 级联删键** (L770-775) | t_hash.c:711-779 |
| **条件 TTL**: 三阶段框架 (Init L1114/SetEx L1068/Done L1192 批量聚合); SetExpiryHT 矩阵 (无 TTL 视为无限: XX|GT 拒; GT/LT/NX 条件; checkAlreadyExpired 删字段 L1044-1051) | t_hash.c:979-1238 |
| **HFE 命令族**: hexpire 批量 (key+N 字段+条件) / httl 三值 (-2/-1/剩余) / hpersist; 传播归一 (同 R-22); hfield mstr 奇数地址 (itemsAddrAreOdd=1) | t_hash.c:2837-3294 |
| **两级注册**: 全局 db->hexpires (早到字段代理, itemsAddrAreOdd=0) + 私有 hfe (字段级, =1); AddToExpires 唯一入口 (L2040) / RemoveFromExpires 返回 minExpire (RENAME/MOVE/COPY 续接); R-22 主动消费 (hashTypeDbActiveExpire L2073) | t_hash.c:115-130,1996-2094 |

### R-25 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | HSET/HGET 初版 (ziplist 小编码) |
| 2.6 | HT 编码 + 阈值转换 |
| 7.0 | ziplist→listpack + storedKey (hfield 作 dict 键) |
| 7.4 | HFE 字段过期 (LISTPACK_EX/私有 hfe/hexpire 族/trash 状态机) |
| 演进 | HMSET 弃用 (回复差异保留) |

### R-25 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | [1] 新 Field value 未初始化即 free → ASan BUS; [3] 单字段 hash 删后应返回 EXPIRED_HASH 非 EXPIRED (断言错); [3][4] setFieldExpiry 构造已过期字段与自身删字段逻辑冲突; 补 hashFree 清理 | harness 3 轮迭代, 30/30 PASS |
| 2 | 通过项 | 行号穷举 129 处全验证 (8 个区间覆盖无偏差); 数字: 512/64/HASH_LP_NO_TTL/EB_EXPIRE_TIME_INVALID/GETF 四态/HFE_LAZY 三标志 全 ✅; 三编码分支/转换三触发+三阶段迁移/GETF 惰性链/条件矩阵/两级注册/skipExpiredFields 全验证 | 记录 |
| 3 | 推断标注 | 百万字段字节数对比 / HFE 版本号 (7.4 从 2024 版权推断) / HT 转换必然性 — 3 处显式标注 | 记录 |

### R-25 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **行号缺失** | 02 节 1 GETF 枚举未给行号 — 实测 **t_hash.c:32-35** | 补 L32-35 |
| 5 | **行号猜测** | pass2-q6 写 HFE 条件枚举 "L980-983 附近" — 实测 **L197-200** (HFE_NX=1<<0/XX/GT/LT) | 修正 |
| 6 | **行号缺失** | 01 节 4 大 hash 加权未给行号 — 实测 hashTypeRandomElement **L1789** (dictGetFairRandomKey R-3) | 补精确行号 |
| 7 | 通过项 | 前置依赖拓扑合规 (R-19/R-3/R-21/R-22/R-24) ✅; 锚点 28/24 (标准 ≥8) ✅; 负面空间/开篇 ✅; 其余 ~50 句逐句对源码一致 ✅ | 记录 |

### R-25 负面空间 (07 维度5)

不做编码降级 (单向) / 不做字段级排序 (迭代序=编码内部序) / 不做 HGETALL 分页 (HSCAN 显式) / 不做字段级精确扫描 (全局代理批量) / 不做 HFE 降级 / 不做 HEXPIRE 单位混用。

---

## §十二·附 R-25 (t_hash) 已交付速查 (2026-08-13)

**源码**: t_hash.c (3418) → 🔴 A, 拆 2 篇 (01-command-encoding.md 73 行 + 02-hfe-field-expire.md 69 行) + harness 30/30 (gcc+ASan)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **SET 标志矩阵**: 9 位域 (NX/XX/EX/PX/KEEPTTL/GET/EXAT/PXAT/PERSIST); 解析期互斥 (NX×XX/KEEPTTL×TTL 族/同类 TTL); 命令族分域 (SET vs GETEX); setkey_flags 组装 → setKey 四态 | t_string.c:49-58,188-270 |
| **SET 传播归一**: expire → 重写 SET...PXAT 绝对毫秒 (主从时钟无关); GET 剥离 (执行面语义不传播); GETSET 重写 SET | t_string.c:63-129,410-419 |
| **INCR 原地优化**: 溢出双检查 (符号对齐); **四条件原地** (refcount==1 && INT && 非共享池 && LONG 范围 → ptr 直改零分配); DECRBY LLONG_MIN 特判; INCRBYFLOAT (long double + NaN/Inf 拒 + 重写 SET KEEPTTL 浮点精度不传播) | t_string.c:580-674 |
| **范围命令**: checkStringLength (512MB + uint64 加法溢出检测); SETRANGE (sdsgrowzero 零填充 + dbUnshare 共享保护); GETRANGE (INT 栈 buf 免分配解码 + 负索引归一) | t_string.c:19-31,421-520 |
| **GETEX 读改写三路径**: 已过期删 (重写 DEL/UNLINK) / setExpire (重写 PEXPIREAT) / PERSIST (重写 PERSIST); GETDEL 读+删; "never propagated as is" | t_string.c:340-408 |
| **LCS 三层防护**: UINT32_MAX-1 长度限 → 表乘法双重检查 → 512MB 内存限 → ztrymalloc 降级; DP O(n×m) | t_string.c:716-929 |

### R-24 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | get/set/incr/append 初版 |
| 2.6 | SETNX/SETEX/GETSET/SETRANGE/GETRANGE |
| 4.0 | SET NX/XX/EX/PX + tryObjectEncoding 链 |
| 6.0 | LCS + GETEX/GETDEL |
| 6.2+ | SET GET/KEEPTTL + 统一解析器 (9 标志) |

### R-24 深审记录 (六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | 通过项 | 行号穷举 117 处全验证 (5 个区间覆盖无偏差); 数字: 9 标志/512MB/10000/UINT32_MAX/双符号溢出 全 ✅; 解析期互斥/INCR 四条件/传播归一/GETEX 三路径/LCS 三层防护 全验证 | 记录 |
| 2 | 推断标注 | INCR 高频占比 / GETRANGE 复制取舍 / INT 省内存量级 — 3 处显式标注 | 记录 |

### R-24 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 3 | **表述不精确** | 节 1 "互斥时后出现拒绝" — 实为落入 else 报 **syntaxerr** (L265-266), 非静默拒绝 | 大纲节 1 修正 |
| 4 | **表述不精确** | 节 4 "sdsnewlen 预填零" — sdsnewlen(NULL,...) 只分配不填零; 零填充由 **sdsgrowzero (L471)** 完成 | 大纲节 4 + pass2-q4 修正 |
| 5 | 通过项 | 前置依赖拓扑合规 (R-1/R-21/R-4/R-28 均序号 < 15) ✅; 锚点 ~30 (标准 ≥4) ✅; 负面空间/开篇 ✅; 其余 ~40 句逐句对源码一致 ✅ | 记录 |

### R-24 负面空间 (07 维度5)

不做字符串截断 (全量拒绝) / 不做 INCR 浮点落库 (long double→十进制串) / 不做 GETRANGE 惰性 (总是复制) / 不做 SET 返回值缓存 / 不做 LCS 优化变体 (纯 DP+防护)。

---

## §十二·附 R-28 (networking 协议) 已交付速查 (2026-08-13)

**源码**: networking.c (4653) → 🔴 A, 拆 2 篇 (01-protocol-parse.md 74 行 + 02-client-io.md 63 行) + harness 23/23 (gcc+ASan)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **读路径**: readlen 四决策 (16KB 基准 server.h:164 / 大参数读剩余 L2677-2682 / MASTER 回升 16KB #9100 / avail 多读); NonGreedy (L2698) vs Greedy (L2703) 双分配; querybuf 上限 (默认 1GB config.c:3226, 未认证 1MB L2745) | networking.c:2655-2764 |
| **解析循环**: qb_pos 滑窗; 四提前退出 (BLOCKED/PENDING_COMMAND/master+忙脚本/CLOSE); reqtype 首字节判定 (*→MULTIBULK); io 线程标记 PENDING_COMMAND 解析执行分离; trim (普通 qb_pos / master repl_applied) | networking.c:2559-2653,2606-2610,2635-2644 |
| **RESP 三行状态机**: * 计数 (INT_MAX 限/未认证 >10 拒) → argv 起步 min(1024) 2× 增长 → $ 长度 (proto_max_bulk_len 512MB config.c:3206 / 未认证 >16KB 拒) → data; **大参数零拷贝** (预对齐 sdsrange L2396-2398 + qb_pos==0 整包借用 createObject L2424-2435) | networking.c:2292-2452 |
| **协议安全面**: 未认证三级限 (10 参数/16KB bulk/1MB querybuf); 全局上限 (512MB/1GB/64KB INLINE); 协议错误一律断开 (setProtocolError L2252) | networking.c:103-111,2252-2291,2323-2378,2739-2755 |
| **输出双缓冲**: prepareClientToWrite 五拒 (SCRIPT/CLOSE_ASAP/REPLY_OFF/MASTER/无 conn); 静态 buf 16KB 优先, **链表出现后静态退休** (L328); clientReplyBlock 节点 ≥16KB 尾节点续写 (L349-375); 超限断连 | networking.c:278-375 |
| **写路径**: writev 批量 (静态+链表拼 iov, ≤64KB/轮 server.h:106); sentlen 部分写跟踪; **从库 replBufBlock 共享零复制** (L1919-1942); 批量直写 handleClientsWithPendingWrites (L2062) | networking.c:1844-2062 |
| **生命周期**: createClient 双态 (查询/回复) + 读事件注册 (L123); freeClient 释放链 (querybuf→阻塞→watch→pubsub→reply→repl 引用→argv); master 缓存 replicationCacheMaster (L1616-1623); 从库杀 RDB 子进程 (L1687-1694); async 队列 | networking.c:112-211,1578-1810 |

### R-28 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | networking.c 初版 — RESP 解析/静态输出缓冲 |
| 早期 | INLINE 命令 (telnet 兼容) |
| 3.x/4.0 | 输出双缓冲 + writev 批量 + clientReplyBlock |
| 6.0 | 共享 repl 缓冲 (R-9) + io threads postpone |
| 7.x | 大参数零拷贝 (32KB) + 未认证分级限流 + reqres |

### R-28 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | harness 迭代 | [1][2] 长度硬编码错误 (30/16 vs 实际 33/19) → ASan SEGV; [2] 分片测试缺第 3 参数; [4] 零拷贝构造未模拟预对齐状态 (重写为 qb_pos==0 整包前提验证); 补 clientFree 清理 | harness 3 轮迭代, 23/23 PASS |
| 2 | 通过项 | 行号穷举 116 处全验证 (7 个区间覆盖无偏差); 数字: 16KB/16KB/64KB/32KB/64KB/512MB/1GB/1024/2×/INT_MAX/10/16384/1MB 全 ✅; 五拒条件/双缓冲切换/大参数零拷贝四前提/从库 replBufBlock/master repl_applied trim 全验证 | 记录 |
| 3 | 推断标注 | 静态 buf "大多数回复零分配" / 32KB 阈值权衡 / 16384 与 16KB 相关性 — 3 处显式标注 | 记录 |

### R-28 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **前向依赖违规** | 02 前置声明 [[R-9-replication]] — R-9 执行序第 25 位未讲 (与 R-21 的 R-15 同类) | 移到对照位, 标注 "仅引用 replBufBlock 概念" |
| 5 | **表述错误** | 02 节 2 "prepareClientToWrite 六拒" — 实为 **5 拒 + 1 挂队列** (第 6 分支是排队非拒绝) | 大纲/闭环/速查/REVIEW 全量 "六拒"→"五拒" |
| 6 | 通过项 | 前置依赖拓扑合规 (R-2/R-4/R-1) ✅; 锚点 28/24 (标准 ≥8) ✅; 负面空间/开篇 ✅; 其余 ~45 句逐句对源码一致 (含 acceptCommonHandler maxclients L1340) ✅ | 记录 |

### R-28 负面空间 (07 维度5)

不做流式解析 (整条命令才执行) / 不做协议协商 (HELLO 显式切换) / 不做未认证队列 (直接限流) / 不做零拷贝常态 (仅 ≥32KB) / 不做错误重试 (协议错误即断连) / 不做输出压缩 / 不做背压调度 (超限断连) / 不做从库私有缓冲 (共享 replBufBlock)。

---

## §十二·附 R-2 (事件驱动+IO 多线程) 已交付速查 (2026-08-13)

**源码**: ae.c (493) + ae.h (115) + ae_epoll.c (118) + networking.c io threads 段 → 🔴 A, 拆 2 篇 (01-event-loop.md 70 行 + 02-io-threads.md 54 行) + harness 23/23 (gcc+ASan)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **事件表**: fd 直接索引双数组 (events/fired) O(1) 注册/删除; mask 合并; maxfd 维护; 删 WRITABLE 连带删 BARRIER | ae.h:20-27,51-56,78-90; ae.c:143-183 |
| **睡眠编排**: 三等待 (DONT_WAIT 立即 / 睡到最早时间事件 O(N) / 无限); beforesleep → aeApiPoll → aftersleep 三明治; DONT_WAIT 可被 beforesleep 侧写 | ae.c:245-258,342-389; networking.c:4178-4211 (processEventsWhileBlocked 4 次喂事件) |
| **分派顺序**: 读先写后 (可立即应答); AE_BARRIER 逆序 (AOF fsync 后统一回复); 同 proc 去重; 回调内可改事件表 (mask 复查+指针刷新) | ae.c:391-443 |
| **时间事件**: 无序单链表 O(N) 找最早 (注释明示权衡); 惰性删除 (AE_DELETED_EVENT_ID); 双重防重入 (maxId 防迭代中新事件 / refcount 防递归释放); 回调返回 ms 周期重排 | ae.c:200-325 |
| **多路复用抽象**: 编译期特化三后端 (evport→epoll→kqueue→select); epoll: EPOLLIN/OUT 映射 + **ERR/HUP 双触发** + ADD/MOD/DEL 语义; epoll_create(1024) 仅内核提示 | ae.c:29-43; ae_epoll.c:18-118 |
| **io threads 生命周期**: io-threads 1-128 默认 1 IMMUTABLE (config.c:3149); 互斥锁即启停栅栏 (创建即锁→start 解锁→stop 上锁); 自旋等 pending 为主; 惰性停 pending < num×2; threads_pending cache-line 对齐防伪共享 | networking.c:4215-4384 |
| **写扇出扇入**: 分发 item_id % num; **从库客户端强制 list[0]** (共享 repl 缓冲); 主线程也干 list[0]; 等全部 pending 归零 → op=IDLE → 装写处理器 | networking.c:4393-4484 |
| **读扇出扇入**: postponeClientRead 五条件 (active && do_reads && !ProcessingEventsWhileBlocked && !(MASTER|SLAVE|BLOCKED) && op==IDLE); 线程内 readQueryFromClient 读+parse; **IO 可并行执行仍单线程** | networking.c:2662,4491-4560 |

### R-2 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2006 | ae 初版 (Jim Tcl 解释器移植) |
| 2009 | epoll 后端 |
| 演进 | aeResizeSetSize / AE_BARRIER / 单调时钟 |
| 6.0 | io threads (扇出扇入, 128 上限) |
| 6.2+ | io-threads-do-reads 可选默认关; cache-line 对齐; #6988 修复 (阻塞中禁用读线程) |

### R-2 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **行号偏差** | networking.c io threads 4 处: initThreadedIO **L4295** / startThreadedIO **L4347** / stopThreadedIO **L4354** / handleClientsWithPendingReadsUsingThreads **L4518** | 大纲 02 + pass1/pass2 全量修正 |
| 2 | harness 迭代 | [5] maxId 测试时间设定错误 (selfRegistering 未到期即断言) — 修 when 后 23/23; [3][4] 用顺序日志强化 (读先写后/BARRIER 逆序真实验证) | harness 修正 |
| 3 | 通过项 | 行号穷举 100 处全验证; 数字: 128/1/1-128/0/num×2/100 万次/三后端链 全 ✅; 互斥锁启停/从库强制 list[0]/postpone 五条件/EPOLLERR-HUP 双触发 全验证 | 记录 |
| 4 | 推断标注 | epoll 生产占比 / io-threads 默认 1 动机 / cache-line 引入版本 — 3 处显式标注 | 记录 |

### R-2 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 5 | **行号偏差** | processEventsWhileBlocked 实测 **L4169** (写 4178-4211); installClientWriteHandler 调用 **L4474** (写 L4471) | 01/02 节 + pass2 修正 |
| 6 | **覆盖缺口** | 01 节 2 beforesleep 未给内容 (AOF flush/FAST 过期/客户端写连接); 01 节 4 时间事件只提 serverCron (evictionTimeProc R-23 同挂) | 两处补连接 |
| 7 | 精确化 | initThreadedIO 锁互斥锁: L4319 是 init, **lock 在 L4320** | pass2-q6 修正 |
| 8 | 通过项 | 前置依赖拓扑合规 (R-20/R-33) ✅; 锚点 25/20 (标准 ≥8) ✅; 负面空间/开篇 ✅; 其余 ~40 句逐句对源码一致 ✅ | 记录 |

### R-2 负面空间 (07 维度5)

不做事件优先级 (fired 顺序 = 内核顺序) / 不做定时器红黑树 (O(N) 权衡注释) / 不做跨线程事件投递 (单线程执行模型) / 不做 epoll ET 模式 (全程 LT) / 不做命令并行执行 (io threads 只 IO+parse) / 不做线程动态扩缩 (1-128 启动定, 仅启停两态)。

---

## §十二·附 R-23 (内存淘汰) 已交付速查 (2026-08-13)

**源码**: evict.c (761) → 🟡 B, 单篇 (outline.md 98 行) + 6 闭环, 行号验证 130 处

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **触发与三态**: processCommand 前置 (server.c:4036) → performEvictions (evict.c:520); EVICT_OK/RUNNING/FAIL — FAIL+denyoom → rejectCommand oomerr (server.c:4049-4052); pre_command_oom_state 命令内共享 (L4059); isSafeToPerformEvictions 四跳过 (yielding/loading/从库 ignore/PAUSE) | evict.c:463-476,515-541; server.c:4036-4059 |
| **overhead 剔除**: AOF buf + repl 超出 backlog 部分不计入 (防 DEL 反馈环 — "越删 DEL 越大") | evict.c:310-353,396-398 |
| **跨 DB 采样池**: EVPOOL_SIZE=16 全局 + maxmemory_samples=5/DB (1-64 config.c:3163); FAIR 选槽+dictGetSomeKeys; idle 三打分 (LRU 空闲ms / LFU 255-计数 / TTL ULLONG_MAX-TTL); 有序插入+cached sds 255B 复用; 幽灵键选键时跳过 | evict.c:33-43,125-225,568-630 |
| **LRU 近似**: 24bit×1000ms 时钟 (满量程 ~194 天), 回绕符号处理; hz≥1 时 server.lruclock 缓存免系统调用 | evict.c:52-100; server.h:896-898 |
| **LFU**: 24bit = 16bit 分钟 LDT + 8bit LOG_C; 概率对数递增 p=1/(base×factor+1) (LFU_INIT_VAL=5 起跳, 255 饱和); 每 lfu_decay_time 分钟惰性衰减 | evict.c:230-308; server.h:3537; config.c:3159-3160 |
| **八策略位域**: 高 8 位 id + 低 3 位标志 (LRU/LFU/ALLKEYS); ALLKEYS→keys 表否则 expires 表; 三条路径 (池排序/随机/拒绝); TTL 免值对象 | server.h:556-569; evict.c:538,564-566,577-581,635-637 |
| **执行控制**: tenacity 三级时间上限 (≤10 线性 50us×t / <100 几何 500×1.15^(t-10) / =100 无限, 默认 10=500us); delta 实测法; 每 16 键: 从库 flush/lazyfree 重查/超时→aeTimeProc 续清; EVICT_FAIL 等 bio ≤1000us | evict.c:479-494,556-726,728-745 |
| **淘汰联动**: dbGenericDelete(lazyfree_lazy_eviction) → notify "evicted" → propagateDeletion → tracking invalidation | evict.c:674-688 |

### R-23 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | maxmemory + LRU 近似采样 |
| 3.0 | LFU 引入 (24bit 双用途) |
| 4.0 | lazyfree 联动 (lazyfree_lazy_eviction + EVICT_FAIL 等 bio) |
| 7.0 | tenacity 重构 (三态 + aeTimeProc 续清); kvstore 分片适配 (FAIR 选槽) |
| 演进 | 池从局部 DB → 跨 DB 全局池 (注释 L571) |

### R-23 深审记录 (六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **行号偏差** | rejectCommand 实测 server.c:4050 (初稿写 L4046-4049), pre_command_oom_state L4059 (初稿写 L4050) | 大纲节 1 + pass2-q1 修正 |
| 2 | 通过项 | 行号穷举 130 处全验证; 数字: 16/255/24/1000/5/1-64/10/0-100/5 全 ✅; tenacity 复算 (500us 默认, 1.15^89≈2.5e5→125s≈2min 与注释一致); 八策略位域全验证 | 记录 |
| 3 | 推断标注 | LFU ~1M 访问/100 计数 (经验值); 194 天/45.5 天回绕 (推导) — temporal-trace.md 显式标注 | 记录 |

### R-23 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 4 | **表述错误** | 大纲节 1 "只读命令放行" — 实测拒绝条件是 **CMD_DENYOOM 标志** (server.c:3958: `is_denyoom_command = cmd_flags & CMD_DENYOOM`); DEL/EXPIRE 等清理类写命令无 DENYOOM 可执行 | 大纲节 1/数据流 + pass2-q1/pass1-notes 修正 |
| 5 | **覆盖缺口** | 大纲节 6 未提统计面: stat_evictedkeys (L682) / stat_last_eviction_exceeded_time (L750-759) — INFO 可观测断链 | 大纲节 6 补统计面 |
| 6 | 通过项 | 前置依赖拓扑合规 (R-21/R-1/R-22/R-33 均序号 < 12) ✅; 锚点 ~40 (标准 ≥4) ✅; 负面空间/开篇 ✅; 其余 ~40 句逐句对源码一致 ✅ | 记录 |

### R-23 负面空间 (07 维度5)

不做全局精确 LRU (采样+池近似, 常数内存) / 不做逐键评估 (仅候选池) / 不做淘汰预算动态调优 (tenacity 静态) / 不做大对象优先 (只看 idle/频率) / 不做淘汰排序保证 (allkeys 忽略 TTL)。

---

## §十二·附 R-22 (过期机制) 已交付速查 (2026-08-13)

**源码**: expire.c (838) → 🟡 B, 单篇 (outline.md 95 行) + 6 闭环, 行号验证 96 处

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **SLOW/FAST 双循环**: SLOW 每 hz tick (databasesCron server.c:1059), 25% CPU 预算 (25ms@hz=10); FAST 每事件循环 (beforeSleep server.c:1689), 1000us 预算, 双条件拒跑 (上次未超时且 stale 低 / 2ms 冷却) | expire.c:92-96,218-231,247-252 |
| **effort 缩放**: active_expire_effort 1-10 (config.c:3177) → 20 键 +5/档, 1000us+250, 25%+2, 10%-1 | expire.c:191-200 |
| **采样驱动**: 每 DB 20 键 (KEYS_PER_LOOP) + 桶上限 num×20; expires_cursor 持久游标跨调用续扫; repeat = 过期比例 >10% 才重扫; 填充率 <1% 跳过 | expire.c:313-348,127-137; server.h:980 |
| **统计反馈**: avg_ttl 指数滑动 (pow(0.98) 16 项常数表, 循环→闭式推导注释); stale_perc 5%/95% 滑动 = FAST 触发信号 | expire.c:24,353-382,399-407 |
| **HFE 配额**: maxToExpire=10000/hz (1000 字段/次@hz=10); 累积 >100 万 (EXPIRED_FIELDS_TH) → ×1-32 放大; 每 DB 先 HFE 后键过期 | expire.c:98,144-185,288; t_hash.c:2073 |
| **可写从库记账**: slaveKeysWithExpire (键→64bit DB 位图, DB>63 折衷); rememberSlaveKeyWithExpire (db.c:1860 触发); 回收停止: 连续 3 不可删 / 64 循环 >1ms / 表空; FLUSHALL 清表 | expire.c:410-560; db.c:542 |
| **EXPIRE 族**: 四命令归一 (basetime+unit); 溢出双守卫; NX/XX/GT/LT (无 TTL 视为无限: GT 失败/LT 通过); checkAlreadyExpired (when<=now && !loading && !masterhost) → 直接删+重写 DEL/UNLINK; 正常路径统一重写 PEXPIREAT 毫秒戳 | expire.c:562-570,635-750 |
| **查询面**: TTL -2/-1/剩余三值; 秒级四舍五入 (ttl+500)/1000; EXPIRETIME 绝对戳; PERSIST/TOUCH; commandTimeSnapshot 命令期冻结 (脚本一致 #1525) | expire.c:773-838; server.c:221-232 |

### R-22 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 | expire.c 初版 — activeExpireCycle 骨架 + 命令族 |
| 3.2 | 可写从库过期记账 (注释 L430: 3.2 前泄漏) |
| 4.0 | effort 配置 (active_expire_effort 1-10) |
| 5.x | expires_cursor 每 DB 持久游标 |
| 7.0 | kvstoreScan 适配 (R-21 连接) |
| 2024 | HFE 字段级过期 (10000/秒配额 + 序列放大) |
| 演进 | avg_ttl 循环 → pow(0.98) 常数表闭式 (L366-377 推导注释) |

### R-22 深审记录 (六层深审)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **表述偏差** | "超时传递 → 下轮全 DB 扫" — 实为 static timelimit_exit 是**上次**标记, 影响**本次** dbs_per_call (L240-241) | 大纲节 1 修正 |
| 2 | **变量遮蔽误导** | avg_ttl 公式 `db->avg_ttl = avg_ttl + (db->avg_ttl - avg_ttl)*factor` — 代码变量遮蔽 (两个 avg_ttl 同名), 笔记原样引用易误读 | 大纲节 2 改语义式 + pass2-q2 加遮蔽注释 |
| 3 | 通过项 | 行号穷举 96 处全验证 (4 个区间覆盖无偏差); 数字: 20/1000/25/10/16/1-10/100万/32 全 ✅; SLOW 预算公式 25ms@hz=10 复算 ✅; 消费链 (server.c:1059/1061/1689) ✅; commandTimeSnapshot 冻结语义 (server.c:221, #1525) ✅ | 记录 |
| 4 | 推断标注 | HFE ×32 封顶动机 / pow 表 16 项上限 / FAST 零开销动机 — 3 处显式标注 temporal-trace.md | 记录 |

### R-22 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 5 | **覆盖缺口** | FAST 调用点漏 iAmMaster + active_expire_enabled 双条件 (server.c:1687-1690 实测) — 从库不跑 FAST 未交代 | 大纲节 1 + pass2-q1 修正 |
| 6 | **覆盖缺口** | 节 5 未提 keyspace 通知 ("expire"/"del", expire.c:726,746) — 通知横切面断链 | 大纲节 5 补 notify |
| 7 | **表述错误** | "传播面只认 PEXPIREAT/DEL 两种" — 实测已过期重写 **DEL 或 UNLINK** (L723: lazyfree_lazy_expire ? unlink : del) — 三种格式 | 节 5 标题/场景/关键设计修正 |
| 8 | **表述错误** | 节 6 场景 "TOUCH 为什么不返回值" — TOUCH 返回触摸计数 (L837) | 场景句修正 |
| 9 | 精确化 | "溢出守卫 ×1000 前后双检查" 含糊 — 实为 ×1000 前预检 (L652-653) + basetime 加法检查 (L659-662) | 精确化 |
| 10 | 通过项 | 前置依赖拓扑合规 (R-21/R-20 已讲) ✅; 锚点 ~48 (标准 ≥4) ✅; 负面空间/开篇 ✅; 其余 ~40 句逐句对源码一致 ✅ | 记录 |

### R-22 负面空间 (07 维度5)

不做实时时钟同步 (传播用绝对时间戳) / 不做精确到期扫描 (抽样+预算, 惰性面保证正确性) / 不做从库主动删主库键 (只等 DEL, 除可写自产键) / 不做 DB>63 从库记账 (位图折衷)。

---

## §十二·附 R-21 (db 键空间) 已交付速查 (2026-08-13)

**源码**: db.c (2818) + kvstore.c (1060) + ebuckets.c (2440) + lazyfree.c (274) ≈ 6600 行 → 🔴 A, 拆 2 篇 (01-keyspace-operations.md 77 行 + 02-kvstore-ebuckets-lazyfree.md 80 行) + harness 44/44 (gcc+ASan)

### 核心机制速查

| 机制 | 关键行号锚点 |
|:--|:--|
| **lookupKey 副作用链**: 查找+过期拦截+LRU/LFU+统计+keymiss 通知; 五标志独立开关; slot 缓存免 CRC16 | db.c:75-127,138-173,217-219 |
| **惰性过期三态**: VALID/EXPIRED/DELETED; 从库只报不删 (等 master DEL); CLIENT_MASTER 豁免; FORCE/AVOID/PAUSE | db.c:29-33,1974-2017; keyIsExpired L1928 (commandTimeSnapshot 虚拟时间) |
| **三表一致**: dbAddInternal (键 sdsdup+信号)/dbSetValue (旧值 lru 继承+HFE 摘除)/dbGenericDelete (先 expires 后 keys); 删除顺序约定 | db.c:180-195,256-289,372-425 |
| **expires 零拷贝**: 键复用主 dict sds (指针共享) + 整数联合值 (免分配); dbExpiresDictType 双 destructor=NULL | db.c:1846-1863; server.c:501-508; dict.c:849 |
| **kvstore 分片**: dict 数组, bits=log2(dicts) ≤16 (游标留 48 位); cluster 14bit=16384 槽 (crc16&0x3FFF+{tag}); 三创建路径 | kvstore.c:230-268; cluster.h:8-9,43-60; server.c:2667-2675, db.c:569, lazyfree.c:210 |
| **扫描/随机**: 游标 48+bits 复用 dictScan 弱语义; Fenwick 树 O(log n) 选桶/跳桶 (FAIR 概率∝元素数) | kvstore.c:102-117,361-403,431-434,500-538 |
| **增量 rehash 预算**: rehashing list + 1000us/tick + 16 dict/DB 轮转; 子进程跳过防 CoW | kvstore.c:621-661; server.c:1054-1101; server.h:105,128 |
| **ebuckets 时间桶**: list(≤16)→rax(6B 键)→segment(≤16); ExpireMeta 48bit TTL 内嵌; 指针 LSB 判 list/rax; 批量过期摊销 O(1)/桶 | ebuckets.c:62-63,146-165,528-548,1424-1549; ebuckets.h:142,161-211 |
| **HFE 两级**: db->hexpires 全局表 (早到字段代理) + hash 内 hfe; 消费 hashTypeDbActiveExpire (R-22 接) | t_hash.c:110-130,2073-2094; expire.c:98,159 |
| **lazyfree**: effort 按分配数估, >64 且 refcount=1 → bio 异步; FLUSHDB ASYNC 换表法 | lazyfree.c:129-215 |
| **命令面**: SCAN 四步 (COUNT×10 双限+Step3 过滤) / RANDOMKEY FAIR+maxtries=100 / SWAPDB 指针互换 (watch/blocked 不换) / getKeys 三代 (key-spec→回调→legacy) | db.c:1049-1319,336-369,1712-1755,2133-2442 |

### R-21 时空溯源

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | db.c 初版 — lookupKey/dbAdd/expire 家族 |
| 2011 | kvstore 引入 (原 dictarray) — 初期只服务 cluster 槽 |
| 5.x/6.x | lazyfree 独立 (freeObjAsync/换表法); UNLINK |
| 7.0 | kvstore 全面接管 redisDb.keys (单机 bits=0 退化); TwoPhaseUnlink |
| 7.x | key-spec 声明式 getKeys (commands.def 生成) |
| 2024 | ebuckets 时间桶 + HFE 字段过期 (PRECISION=0 显式 TBD) |

### R-21 深审记录 (六层深审 + harness 实证)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **行号偏差** | 交接文档 "kvstoreCreate L2665-2680" — 实测 **L2667-2675** | 大纲 02 + 本文 §十二 修正 |
| 2 | **行号偏差** | hashTypeDbActiveExpire 在 **t_hash.c:2073** (初稿写 2080) | pass2-q6 修正 |
| 3 | **机制实证 (harness)** | expires 删除共享键必须不释放 (destructor=NULL) — harness 复现 ASan use-after-free 后修复 | harness 迭代 (dictDelete free_key 标志) |
| 4 | harness 缺陷 | [9] 断言错 (k3 TTL 仍在); [10] 桶扫描漏最后桶 (i<=mask) | harness 修正 |
| 5 | 表述修正 | "TYPE 过滤 7.8 起" 无版本依据 — 实为 Step3 复查 (L1290-1296) | pass2-q8 修正 |
| 6 | 通过项 | 锚点行号 ~80 处全 grep 验证 ✅; 数字穷举: 14bit/16384/16/16/0/64/10000 全部 ✅ | 记录 |

### R-21 二次深度 REVIEW (2026-08-13, 07 五维度 + 内容深度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **前向依赖违规** | 02 前置声明 [[R-15-cluster]] (第 25 位未讲) — 07 维度3: 依赖必须序号 < 当前域 | 移对照位, 标注 "仅引用 cluster.h 常量" |
| 8 | 前向引用 | 01 对照 [[R-16-transaction]] 同属未来域 | 标注 "仅信号语义" |
| 9 | **覆盖缺口** | 从库主动过期面 (expireSlaveKeys) 未提及 — 读者会误以为从库永不删过期键 | 01 节 2 补注 (R-22 详述) |
| 10 | **覆盖缺口** | getKeys 的 cluster 消费面 (多键槽一致检查) 未提及 | 01 节 5 补 "消费面" |
| 11 | **编造** | "ebGetNextTimeToExpire = serverCron 调度依据" — 消费端穷举仅 t_hash.c:1992 (hash 本地), 全局表无消费端 | 02 节 4 修正 + ebExpireDryRun 消费端标注 |
| 12 | **表述错误** | "ExpireMeta 5 位标志" — 位域实为 4 单标志 + numItems:5 + userData:3 | 02 节 4 精确化 |
| 13 | **行号区间** | getKeys "L2133-2284" 不含 legacy range (L2379-2421) | 改 L2133-2442 |
| 14 | **表述误导** | "hashTypeAddToExpires (db.c:1446 等)" — db.c 仅 RENAME/MOVE/COPY 特例, 常规在 t_hash.c (L1624-1662/L1229-1231) | 02 节 5 改 t_hash.c:2040 唯一入口 + 路径分解 |
| 15 | 边界 | "PERSIST 语义" 未注实现位置 (PERSIST 命令在 expire.c) | 01 节 4 加注 (R-22 边界) |
| 16 | 通过项 | 02 前置 R-21-上 承接 ✅; 锚点密度 45/42 (标准 ≥8) ✅; 其余 ~40 句机制描述逐句对源码一致 ✅ | 记录 |

### R-21 负面空间 (07 维度5)

不做自动过期扫描 (R-22 主动面) / 不做键级锁 (单线程) / 不做独立键对象 (键值分离) / 不做 dict 数组动态伸缩 (bits 定死) / 不做时间桶精度提升 (PRECISION=0 TBD) / 不做 ebRemove 合并 (TODO 注释) / 不做无界异步 (lazyfree FIFO)。

---

## §十三 Redis 高频坑 (跨域 12 条, 本阶段实证)

1. **行号引用必 grep** — 18 处偏差实证 (lpEncodeGetType 317/NaN 1435/dictExpand 1292/hz 1283/返回 1538/extend_to_usable 125-146/intset.h 35-39/SIZE_SAFETY_LIMIT/键空间创建 2667-2675/hashTypeDbActiveExpire 2073 等)
2. **7.x 重构改变机制面** — zslFirstInRange 已移除/ziplist 已退役/listpack 无级联/键空间 kvstore 分片 — 引用旧知识前 grep 现版
3. **认知修正优先于写作** — "级联更新"/"不丢不重"均为旧认知, harness/源码实证后修正
4. **编码家族的数字** — EMBSTR 44=16+3+44+1 (64B arena)/set 512/hash 512+64B/zset 128+64B/list -2=8KB/共享整数 10000 — 每个数字独立穷举
5. **共享对象语义** — OBJ_SHARED_REFCOUNT 不可变/incr-decr 不碰/maxmemory 禁共享整数 (LRU 私有)
6. **SCAN 弱语义** — 不丢旧键 (保证)/重复允许/中途插入可漏 — 别写"不丢不重"
7. **C 单体工具适配** — MCP trace 对回调注册抓取有限, 函数指针依赖 grep 兜底
8. **harness 每次抓真问题** — 13 个 harness 全部迭代修正过 (构造顺序/缩容顺序/伪降型/共享边界/hz 回落/expires 共享键删除安全/maxId 测试时间设定/RESP 长度硬编码+分片数据/Field 未初始化 free/栈对象误 free)
9. **大域必须拆分** — R-20 (7256 行) 拆 2 篇 ✅; R-21 (~6300 行) 拆 2 篇 ✅; R-28 networking (4653)/R-9 replication (4231) 同样预判
10. **推断与事实分离** — 代码无 X 是事实, "为什么无 X"是推断 — 标注
11. **配置阈值跨域引用** — config.c:3152-3223 是全部编码阈值的单一来源 (list/hash/set/zset)
12. **更新双文档** — 每域完成: 本文 §零/§十二 + HANDOFF-STAGE3 §零 (进度/域数/REVIEW 汇总)

---

## §十四 完成检查单 (下次会话开始前)

- [ ] 已读本文 §零~§二 + §十二 (R-12 详案)
- [ ] 已读 REDIS-PLAN.md §〇 (怀疑审计表) + 09 方法论 (反模式 7/8)
- [ ] 开工 R-28 前 15 分钟: 对寄存器数字/稀疏 opcode/误差公式做域级怀疑审计 (数字穷举)
- [ ] 每域完成: Pass 0-3 + 六层深审 + 时空溯源 + harness (🔴) + 更新本文 §零/§十二 + STAGE3 §零
- [ ] 每域二次 REVIEW (07 五维度) 记录真实问题, 零发现=不合格

---

## §十五 文件路径

```
analysis/source-analysis/redis/            ← 本文所在
├── REDIS-PLAN.md                          ← 33 域规划 + §〇 怀疑审计表 (18→33 v2)
├── HANDOFF-REDIS.md                       ← 本文 (分域唯一入口, 自包含)
├── knowledge-planning/                    ← r33-zmalloc/r4-sds/r3-dict/r19-listpack/r7-intset/r6-zset/r5-quicklist/r1-object/r20-server
├── outlines/
│   ├── r33-zmalloc/  r4-sds/  r3-dict/  r19-listpack/  r7-intset/
│   ├── r6-zset/  r5-quicklist/  r1-object/
│   ├── r20-server/  (01-boot.md + 02-cron-commands.md)  ← 大域 2 篇
│   ├── r21-db/  (01-keyspace-operations.md + 02-kvstore-ebuckets-lazyfree.md)  ← 大域 2 篇
│   ├── r22-expire/  (outline.md 单篇)
│   ├── r23-evict/  (outline.md 单篇)
├── r2-events/  (01-event-loop.md + 02-io-threads.md)  ← 大域 2 篇
│   └── r28-networking/  (01-protocol-parse.md + 02-client-io.md)  ← 大域 2 篇
└── harness/                               ← 10 个 (r33/r4/r3/r19/r6/r5/r1/r20/r21/r2), 全部 gcc+ASan 验证 (10/10 编译运行通过)

源码: /data/workspace/source-code/code/spring/redis/  (Redis 7.4.2, src/ 175 文件)
MCP 索引: data-workspace-source-code-code-spring-redis (31593 节点, ready)

上级: ../HANDOFF-STAGE3.md (阶段3 总入口, Redis 状态 19/33)
后续: 阶段3.6 Redisson (7 域) / 阶段3.7 ES (11 域) — 开工前 09 域重审
```

---

## §十五·附 交接文档 REVIEW 记录 (2026-08-13, 07 五维度)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **状态表数字失真** | §零 状态表 5 处与实际不符: R-4 (82→104 行/23→28 锚), R-19 (20→18 锚), R-6 (20→16 锚), R-5 (85→96 行/20→21 锚), R-1 (85→94 行/20→18 锚); **R-20 的 85+75/20+18 是交付时预估非实测 (实为 53+56/3+6)** | 逐域 wc+grep 实测修正 |
| 2 | **表述错误** | §十五 "javac→gcc+ASan" — javac 是 Java 编译器 (从 MP 阶段误复制), Redis 是 C | 改为 "gcc+ASan" |
| 3 | 数字精确化 | §十三 偏差计数 "10+" → 实测 **16 处** | 修正 |
| 4 | 归属错误 | §一 铁律 3 的 R-33 "构造顺序" 是深审发现非 harness; harness 抓到的是记账双路径 16 vs 18 | 修正归属 |
| 5 | 通过项 | §二 重审历史数字 (18→33/183%/83%/8 项断言) 与 REDIS-PLAN §〇 一致 ✅; §十六 跨阶段状态 ✅; 执行序 ✅; R-21 连接点行号 (L2667-2675/db.c:1974) ✅ | 记录 |

---

## §十六 完成状态总览 (供 STAGE3 同步)

| 阶段 | 仓库 | 状态 |
|:--|:--|:--|
| 3.1 HikariCP | 13 域 | ✅ 100% (历史) |
| 3.2 Druid | 9 域 | ✅ 100% (历史) |
| 3.3 MyBatis+MP | 7+9 域 | ✅ 100% (三件套收官) |
| **3.5 Redis** | **33 域** | 🔵 **19/33** (R-33/R-4/R-3/R-19/R-7/R-6/R-5/R-1/R-20/R-21/R-22/R-23/R-2/R-28/R-24/R-25/R-26/R-27/R-11) |
| 3.6 Redisson | 7 域 | ⏳ |
| 3.7 ES | 11 域 | ⏳ |
