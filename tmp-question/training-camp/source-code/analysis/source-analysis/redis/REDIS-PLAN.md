# Redis — 知识网络化规划 (R-1~R-33, 09 域重审后 v2)

> **日期**: 2026-08-13 | **依据**: issue/源码分析执行计划.md 阶段3.5 (18 域) + **09 对既有规划保持怀疑 全量域重审** (顶层包扫描+数字穷举+依赖方向+拓扑重排) + **二次 REVIEW** (v2: 补 zmalloc/functions, 修正 32→33)
> **源码**: `/data/workspace/source-code/code/spring/redis` (**Redis 7.4.2**, 2025-01-06; src/ 107 个 .c + 68 个 .h = 175 文件, 扁平单层)
> **定位**: 阶段3.5 — 数据存储 (缓存之王). 核心 = **单线程事件循环 + 全局键空间 + 编码优化** — Redis 的价值在"数据结构内存引擎 + 网络协议服务器"双面
> **知识网络**: 本文含 前置/复用/引出 双链; 与阶段3.3 (MyBatis 缓存对照 M-7) + 阶段2 (Spring Cache) + Redisson (阶段3.6) + ES (3.7) 互联

---

## 〇、09 怀疑审计表 (Redis, 2026-08-13) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 18 个** | 顶层 src/ 全量扫描 (175 文件) | **未覆盖定义特征级机制 ≥15 处**: server.c 7256/serverCron、db.c 2818 键空间、networking.c 4653 协议主体、t_hash 3418、t_list 1364、t_set 1658、t_string 930、expire.c 838、evict.c 761、pubsub.c 748、Lua 脚本 (eval 1749+script 640+script_lua 1712)+Functions (functions 1121+function_lua 497)、listpack.c 3150、**zmalloc.c 1056 (内存层)**、module.c 14002、acl.c 3241 | **修正: 18 → 33 域** (遗漏 15, 全部过设计决策测试: 命令面/生命周期/编码层/内存层为 Redis 定义特征) |
| R-1 "EMBSTR阈值44B" | grep OBJ_ENCODING_EMBSTR_SIZE_LIMIT | object.c:101 = 44 | **接受** ✅ |
| R-3 ">1, >4强制, <3.1%缩容" | grep dict_can_resize/force_ratio/HASHTABLE_MIN_FILL | dict.c:41-42 (ratio=4), dict.h:27 (MIN_FILL=8), dict.c:392 注释 "1/32" = **3.125%** | **接受** ✅ (1/32 精确) |
| R-4 "预分配<1MB→2×, ≥→+1MB" | grep sdsMakeRoomFor/SDS_MAX_PREALLOC | sds.c:233-236, sds.h:13 (1024*1024) | **接受** ✅ |
| R-6 "P=0.25" | grep ZSKIPLIST_P | server.h:515 = 0.25, MAXLEVEL 32 | **接受** ✅ |
| R-12 "16384×6bit/0.81%" | grep HLL_REGISTERS | hyperloglog.c:176 (P=14→16384); 0.81% = 1.04/√16384 理论值 | **接受** ✅ |
| R-13 "52bit" | grep GEO_STEP_MAX | geohash.h:46 (26*2=52) | **接受** ✅ |
| R-15 "16384 slot" | grep CLUSTER_SLOTS | cluster.h:9 = 16384 | **接受** ✅ |
| R-2 事件驱动排第 2 | 依赖分析 | 事件驱动依赖 server 骨架 (serverCron/网络注册) — 排第 2 反拓扑 | **修正: 后移** (R-2→12 位) |
| R-3 dict 排第 3 | 依赖分析 | server/db/object 全依赖 dict — 应排基础层第 2 | **修正: 提前** |
| R-5 "ziplist/listpack" | 检查编码层 | **listpack.c 3150 行独立未成域** (hash/zset/stream 底层编码, 7.x 替代 ziplist) | **新增 R-19** |
| ~~32 域~~ (二次 REVIEW) | 二次全量文件扫描 | **再漏 2 处**: zmalloc.c 1056 (内存分配层/jemalloc 封装/used_memory 统计 — 全服务器基础), functions.c 1121+function_lua.c 497 (Redis 7.0 Functions 系统) | **修正: 32 → 33 域** (zmalloc 新增 R-33, functions 并入 R-30) |
| ~~expire 归 R-22~~ (二次 REVIEW) | grep 实现位置 | **expireIfNeeded 实现在 db.c:1974** (非 expire.c) — 惰性过期归 R-21 (db 键空间), 主动过期 activeExpireCycle (expire.c) 归 R-22 | 边界精确化 |
| 依赖方向 | import 统计适用性 | **C 单体内核: 无模块 import, 全量共享 server.h 全局态** — import 法不适用, 改函数调用方向 (codebase-memory trace + 回调注册点: main→aeMain L7251 / initServer→aeCreateTimeEvent(serverCron) L2757) | 记录方法适配 |

**覆盖率报告**: 既有规划 18 域 → 重审后 33 域 (**183%**, +15 域解释: 命令层 (t_*×4)/生命周期 (serverCron)/键空间 (db+expire+evict)/网络 (networking+pubsub)/脚本 (Lua+Functions)/编码 (listpack)/内存层 (zmalloc)/扩展 (module+ACL) 全部为定义特征级)。

---

## 一、入口点与主线

`main (server.c) → initServer → aeMain 事件循环` — 所有命令: `client → readQueryFromClient (networking.c) → processCommand (server.c) → 命令分发表 → t_*.c 实现 → addReply` — 数据: `redisDb.dict (db.c 键空间) → robj (object.c) → 编码 (sds/dict/quicklist/listpack/intset/rax...)` — 旁路: 持久化 (rdb/aof), 复制 (replication), 高可用 (sentinel/cluster), 脚本 (lua), 模块 (module.c)。

## 二、入口展开追踪 (00 §2)

| 候选 | 源码位置 | 设计决策测试 | 结论 |
|---|---|---|---|
| `main/serverCron` | server.c (7256) | 生命周期+周期任务编排 (过期/持久化/复制心跳) — 定义特征 | **R-20** |
| `redisDb.dict 键空间` | db.c (2818) + kvstore.c/ebuckets.c | lookupKey/expireIfNeeded/键遍历 — 所有命令入口 | **R-21** |
| `readQueryFromClient/协议` | networking.c (4653) | RESP 解析/输出缓冲/io threads 宿主 — R-2 只覆盖 io 面 | **R-28** |
| `robj 编码` | object.c (1677) | 类型+编码双态/共享整数/EMBSTR | **R-1** |
| `expireIfNeeded/activeExpireCycle` | expire.c (838) | 惰性+主动双删除 — 定义特征 | **R-22** |
| `performEvictions` | evict.c (761) | maxmemory LRU/LFU 淘汰 — 定义特征 | **R-23** |
| `t_hash/t_list/t_set/t_string` | 各 t_*.c | 命令面+编码选择 (listpack/hash 转 dict 等) — 高频命令域 | **R-24~27** |
| `EVAL 脚本` | eval.c+script.c+script_lua.c (~4100) | 原子性执行/脚本缓存 — 定义特征 | **R-30** |
| `RedisModule_*` | module.c (14002) | 插件扩展系统 — 巨型域 (独立扩展, 面试低频) | **R-31** (🟡 可选) |
| `ACL` | acl.c (3241) | 权限体系 — 安全面 | **R-32** |
| `listpack` | listpack.c (3150) | hash/zset/stream 底层编码, 7.x 主编码 — 执行计划漏 | **R-19** |

## 三、域清单 (33 域: 18🔴 + 15🟡, 含 09 重审修正 v2)

### 基础层 (内存与编码) — 零依赖, 拓扑最前

| # | 域 | 文件 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| R-33 | **zmalloc** (新增 v2) | zmalloc.c (1056) | 内存分配封装 (jemalloc/used_memory 统计/OOM 处理/内存共享) — 全服务器内存基础, INFO memory 数据源 | 🔴 A |
| R-4 | SDS | sds.c (1473) | 5 种 header/二进制安全/预分配策略 | 🔴 A |
| R-3 | Dict | dict.c (2056) | 渐进式 rehash/双 ht/SipHash | 🔴 A |
| R-19 | **listpack** (新增) | listpack.c (3150) | 紧凑编码/向后遍历/级联更新 — hash/zset/stream 底层 | 🔴 A |
| R-7 | intset | intset.c (560) | 有序整数数组/升级 | 🟡 B |
| R-6 | skiplist+ZSet | t_zset.c (4513) | 双结构 (dict+skiplist)/P=0.25 | 🔴 A |
| R-5 | quicklist | quicklist.c (3334) | Node 分页+LZF+listpack | 🔴 A |
| R-1 | redisObject | object.c (1677) | type/encoding/共享整数/refcount | 🔴 A |
| R-2 | 事件驱动+IO 多线程 | ae.c+ae_epoll.c+networking.c 部分 (io threads) | epoll/beforesleep/io threads | 🔴 A |

### 骨架与数据库层

| # | 域 | 文件 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| R-20 | **server 骨架+cron** (新增) | server.c (7256)+config.c 并入 | main/initServer/serverCron/命令表/配置 | 🔴 A |
| R-21 | **db 键空间** (新增) | db.c (2818)+kvstore+ebuckets+lazyfree | lookupKey/**expireIfNeeded (惰性, 实现在 db.c:1974)**/dbAdd/dbDelete/7.x kvstore | 🔴 A |
| R-22 | **过期机制** (新增) | expire.c (838) | **activeExpireCycle (主动, expire.c)**/过期事件 — 与 R-21 惰性面互补 | 🟡 B |
| R-23 | **内存淘汰** (新增) | evict.c (761) | maxmemory LRU/LFU/volatile 策略 | 🟡 B |

### 网络与命令层

| # | 域 | 文件 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| R-28 | **networking 协议** (新增) | networking.c (4653) | RESP 解析/readQueryFromClient/输出缓冲/client 生命周期 | 🔴 A |
| R-24 | **t_string** (新增) | t_string.c (930) | GET/SET/INCR/编码选择 | 🟡 B |
| R-25 | **t_hash** (新增) | t_hash.c (3418) | HSET 族/listpack↔dict 转换 | 🔴 A |
| R-26 | **t_list+blocked** (新增) | t_list.c (1364)+blocked.c (746) | LPUSH 族/quicklist/**阻塞框架 (BLPOP/就绪键唤醒/超时 — 通用阻塞语义)** | 🔴 A (v2 升级: 阻塞机制定义特征级) |
| R-27 | **t_set** (新增) | t_set.c (1658) | SADD 族/intset↔dict 转换 | 🟡 B |
| R-11 | Bitmap | bitops.c (1269) | SETBIT/BITCOUNT | 🟡 B |
| R-12 | HyperLogLog | hyperloglog.c (1597) | 稀疏/稠密/16384 寄存器 | 🟡 B |
| R-13 | GEO | geo.c (1005)+geohash.c | 52bit 编码+ZSet 载体 | 🟡 B |
| R-29 | **pubsub+notify** (新增) | pubsub.c (748)+notify.c (124) | 频道订阅/keyspace 通知 | 🟡 B |

### 持久化/复制/高可用

| # | 域 | 文件 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| R-8 | RDB+AOF | rdb.c (4070)+aof.c (2754)+bio.c | fork COW/Multi-part AOF/fsync 策略 | 🔴 A |
| R-9 | 主从复制 | replication.c (4231) | PSYNC/replBacklog/全量+增量 | 🔴 A |
| R-10 | Stream+rax | t_stream.c (4055)+rax.c (1933) | Radix Tree/XADD/PEL/消费者组 | 🔴 A |
| R-14 | Sentinel | sentinel.c (5463) | SDOWN/ODOWN/故障转移 | 🔴 A |
| R-15 | Cluster | cluster.c (1445)+cluster_legacy.c (6526) | 16384 slot/CRC16/MOVED/Gossip | 🔴 A |

### 高级特性

| # | 域 | 文件 (行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| R-16 | 事务 | multi.c (482) | MULTI/EXEC/WATCH — ACID | 🟡 B |
| R-17 | 客户端缓存 | tracking.c (648) | CLIENT TRACKING/RESP3 invalidate | 🟡 B |
| R-18 | 内存碎片 | defrag.c (1270) | activeDefragCycle/jemalloc hint | 🟡 B |
| R-30 | **Lua 脚本+Functions** (新增) | eval.c (1749)+script.c (640)+script_lua.c (1712)+**functions.c (1121)+function_lua.c (497, 7.0 Functions 并入)** | EVAL/EVALSHA/原子性/脚本缓存/FUNCTION 库管理 | 🟡 B |
| R-32 | **ACL** (新增) | acl.c (3241) | 用户/权限/命令分类 | 🟡 B |
| R-31 | **module 系统** (新增) | module.c (14002) | RedisModule API/插件扩展 — 巨型单文件 | 🟡 B (标注独立扩展, 可最后) |

## 四、已排除 (00 §3 — 防"存在=域")

| 类/文件 | 原因 |
|---|---|
| redis-cli/redis-benchmark/redis-check-aof/rdb | 独立 CLI 工具, 非服务器内核 (redis-cli 10815 行 — 工具) |
| debug.c (2672) | 调试命令面, 无定义特征 |
| memtest/latency/slowlog/sparkline/lolwut/monotonic/localtime/rand/mt19937/crcspeed/strl/setcpuaffinity/setproctitle | 工具/库函数, 薄 |
| adlist.c (list 库) | 已被 quicklist/listpack 取代的旧链表 (7.x 仅少数残留) |
| ziplist.c (2665) | 旧编码, 7.x 被 listpack 替代 (R-19 讲替代关系一句话) |
| sort.c (613) | 单命令, 薄 |
| connhelper/socket.c/tls.c/unix.c | 连接抽象 — 并入 R-28 |
| timeout.c (182)/syncio.c (124)/rio.c (520) | 客户端超时并入 R-28; 同步 IO/rio 抽象并入 R-8 |
| syscheck/fpconv/hdr_histogram/linenoise/lzf/jemalloc/deps/* | 第三方依赖库 (jemalloc 在 R-18/R-33 一句话) |
| call_reply.c/resp_parser.c | RESP3 客户端侧 — 并入 R-28 一句话 |
| cli_commands/commands.c/commands.def | 命令元数据生成, 并入 R-20 |

## 五、知识网络图 (Obsidian 双链)

```
← 复用/内核来源:
   MyBatis 缓存 (M-7, 阶段3.4 已分析) ──→ R-1 对照 (本地缓存 vs 分布式缓存)
   spring-tx (s29-33, 已分析) ──→ R-16 事务 ACID 对照
   Spring Cache (B-12, 已分析) ──→ R-17/Redisson 集成

→ 引出/消费者:
   R-1~R-32 ──→ 阶段3.6 Redisson (7 域) — 客户端视角
   R-2/R-28 ──→ Redisson CommandAsyncExecutor
   R-1/R-21 ──→ Redisson 编码/数据结构镜像
   R-8/R-14/R-15 ──→ 生产运维 (SRE 面)

📌 双链格式 (每篇大纲 header 写):
  前置: [[R-4-SDS]] ...
  复用: [[M-7-cache]] [[s29-tx]] ...
  引出: [[RD-4-command-executor]] ...
```

## 六、执行顺序 (拓扑: 叶子先, 09 重排 v2)

**R-33 → R-4 → R-3 → R-19 → R-7 → R-6 → R-5 → R-1 → R-20 → R-21 → R-22 → R-23 → R-2 → R-28 → R-24 → R-25 → R-26 → R-27 → R-11 → R-12 → R-13 → R-29 → R-16 → R-8 → R-9 → R-10 → R-14 → R-15 → R-17 → R-18 → R-30 → R-32 → R-31**

> 拓扑理由: 内存层 (zmalloc) 与基础编码零依赖先行; 对象 (R-1) 依赖全部编码; server 骨架 (R-20) 提供全局态后, 数据库层 (R-21~23) → 事件驱动 (R-2) → 网络协议 (R-28) → 命令层 (R-24~27/11~13) 依赖键空间+对象; 持久化 (R-8 依赖 bio) → 复制 (R-9 依赖 RDB) → 高可用 (R-14/15) → 高级特性殿后。
> **与执行计划差异**: R-33 zmalloc 新排第 1 (内存基础); R-2 事件驱动 2→12 位 (原排 object 后, 反拓扑: 事件循环依赖 server 骨架); R-3 dict 3→2 位 (提前, 全局依赖); R-4 sds 4→1 位; R-1 object 1→7 位 (依赖全部编码层); R-8 rdb 8→23 位 (依赖 bio+db)。

## 七、深度分类复核 (00 §3.5)

- **18🔴 / 15🟡** (55% 🔴, v2 修正) — 执行计划 10🔴/8🟡 → 重审 +15 域后重新定级
- 🔴 = 定义特征 (内存层/编码层/键空间/协议/持久化/复制/高可用/命令层主干/阻塞框架)
- 🟡 = 支撑与高级 (过期/淘汰/高级数据结构/事务/脚本/ACL/模块)
- 巨型域: module.c 14002 行 (单文件) — 按 01 巨型域标准 (单文件承载大域), 标注"独立扩展系统, 面试低频, 可最后或按需简略"

## 八、与原始执行计划的差异汇总 (v2)

| 原始 | 重审后 | 理由 |
|:--:|:--:|---|
| 18 域 | **33 域** (+15) | 命令层 t_string/t_hash/t_list/t_set (高频命令域), server 骨架+cron, db 键空间, 过期, 淘汰, networking 协议主体, pubsub+notify, Lua 脚本+Functions, listpack 编码, **zmalloc 内存层**, ACL, module 系统 — 全过设计决策测试 |
| R-2 事件驱动第 2 | 第 12 位 | 反拓扑: 事件循环依赖 server 骨架初始化 |
| R-1 object 第 1 | 第 7 位 | object 依赖全部编码层 (sds/dict/quicklist/listpack) |
| 数字断言 (8 项) | 全部接受 | 44B/1:1/4:1/3.125%/1MB/0.25/16384/52bit 逐项 grep 验证 ✅ |
| 依赖方向 import 法 | **方法适配** | C 单体内核无模块 import — 改函数调用方向 + 回调注册点 (main→aeMain/initServer→serverCron) + server.h 全局态共享分析 |

**覆盖率报告 (00 §第九步)**: 方法论域发现 33 域 = 执行计划 18 域 + 重审新增 15 域. 差距分析: 执行计划域清单错误率 **83%** (MyBatis 5→7 教训的放大版 — 越大仓库遗漏越隐蔽, 二次 REVIEW 又抓 2 处)。

## 九、完成检查单 (00 §8)

- [x] 顶层包扫描 (src/ 175 文件全量) ↔ 域清单覆盖矩阵
- [x] 全部数字断言穷举验证 (8 项, 全部接受)
- [x] 依赖方向方法适配记录 (单体 C: 函数调用方向)
- [x] 拓扑重排完成, 与规划差异逐条记录理由
- [x] 新增域全过 00 §3 + §3.5 测试
- [x] 怀疑审计表已写入 (§〇)
- [x] 偏差已同步 HANDOFF-STAGE3
