# Redis 源码学习范围规划复盘与修订路线图

> 复盘基线：Redis 7.4.2，源码 `/data/workspace/source-code/code/spring/redis/`。
> 本文不是正文大纲，而是按"先模块扫描，再按机制重组；补齐失败路径、测试证据、集成边界"的方法论，对现有 18 域规划做卷级复盘。

## 一、复盘结论

现有 18 域规划（10 核心 + 8 扩展）抓住了 Redis 的最高价值主题：

- 数据结构与编码（SDS / Dict / quicklist / skiplist / intset / rax）
- 事件驱动（ae + IO 多线程）
- 持久化（RDB + AOF）
- 复制（PSYNC）
- 高可用（Sentinel / Cluster）
- 高级特性（Bitmap / HLL / GEO / Stream）

从方法论看，本章还存在五个风险：

1. **这是 C 语言项目，不是 Java 项目**。之前四卷（HikariCP / Druid / MyBatis / MyBatis-Plus）都是 Java。Redis 卷的方法论需要适配：无 MCP 索引、无类/方法级跨文件图，只能用 grep/glob + 文件行号锚点。
2. **18 域一卷偏重**。之前的卷都是 8~11 篇。Redis 18 域若全做，正文规模是之前的两倍，必须明确"主干层先闭合、扩展层分批补"。
3. **事件驱动（R-2）被安排在中后段**。`R-2` 的 `ae.c` 只有 493 行，是十大数据结构之后最靠前的高价值域，但阅读顺序却放在 R-13 GEO 之后。它才是 Redis 性能主线的入口，应前置。
4. **淘汰清单里"网络传输层"整块被淘汰**，但 `networking.c`（4653 行，RESP 协议解析 + IO 多线程）是核心域 R-2 的一部分，不能与 `anet.c`/`connection.c` 一起淘汰。
5. **缺集成边界**。Redis 是服务器，无 Spring 集成。但对 Java 读者，`spring-boot-starter-data-redis` 与 Lettuce/Jedis 的关系第一卷未覆盖。这与方法论要求的"集成边界"不符，但可标记为暂缓候选而非必做。

## 二、已验证的仓库事实

| 项目 | 规划值 | 实际值 | 差异 |
|---|---|---:|---|
| 版本 | 7.4.2 | 7.4.2 | 一致 |
| `.c` 文件数 | 115 | 107 | 规划多计 8 |
| `.h` 文件数 | 68 | 68 | 一致 |
| `intset.c` 行数 | 200+ | 560 | 规划少计 |
| `ae.c` 行数 | ~600 | 493 | 规划多计 |
| `server.c` | 7256 | 7256 | 一致 |
| `networking.c` | 4653 | 4653 | 一致 |
| `sds.c` | 1473 | 1473 | 一致 |
| `dict.c` | 2056 | 2056 | 一致 |
| `quicklist.c` | 3334 | 3334 | 一致 |
| `ziplist.c` | 2665 | 2665 | 一致 |
| `listpack.c` | 3150 | 3150 | 一致 |
| `t_zset.c` | 4513 | 4513 | 一致 |
| `rax.c` | 1933 | 1933 | 一致 |
| `t_stream.c` | 4055 | 4055 | 一致 |
| `hyperloglog.c` | 1597 | 1597 | 一致 |
| `rdb.c` | 4070 | 4070 | 一致 |
| `aof.c` | 2754 | 2754 | 一致 |
| `replication.c` | 4231 | 4231 | 一致 |
| `sentinel.c` | 5463 | 5463 | 一致 |
| `cluster.c` | 1445 | 1445 | 一致 |

## 三、按机制重组后的建议结构

### A. 数据结构与编码层（R-1 ~ R-7、R-10）

原规划的数据结构核心域方向正确，建议不动，但微调阅读顺序：

#### R-1 redisObject 类型系统
**读者问题**：`type` 与 `encoding` 为什么分离？一个 `redisObject` 如何决定用哪种底层编码？

**入口**：`src/object.c`、`src/server.h:882`-`894`（`struct redisObject`）、`createStringObject`、`tryObjectEncoding`。

**状态核心**：`redisObject`（type:4bit / encoding:4bit / lru:24bit / refcount / ptr）、EMBSTR 阈值 44B、共享整数 `shared.integers[10000]`。

**失败路径**：编码选择边界（INT→EMBSTR→RAW 何时升级）、refcount 与共享对象交互。

**跨域桥**：R-4 SDS（EMBSTR/RAW 的实际载体）、R-3 Dict（HT 编码）、R-5 quicklist。

#### R-4 SDS
**读者问题**：Redis 为什么不直接用 C 字符串？

**入口**：`src/sds.c`、`src/sds.h`、`sdscatlen`、`_sdsMakeRoomFor`。

**状态核心**：5 种 header（sdshdr5/8/16/32/64）、`len`/`alloc`/`flags`/`buf[]`、二进制安全、预分配策略（<1MB 翻倍 / ≥1MB +1MB）、sdshdr5 追加时自动升级。

**失败路径**：扩容边界、buf 越界、sdshdr5 永不用于 expand。

**跨域桥**：R-1 redisObject（STRING/LIST/HASH/SET 的值载体）、R-8 AOF（命令序列化为 SDS）。

#### R-3 Dict 渐进式 rehash
**读者问题**：Redis 的哈希表如何在扩容时不影响线上服务？

**入口**：`src/dict.c`、`src/dict.h`、`dictAdd`、`dictRehash`、`_dictExpandIfNeeded`。

**状态核心**：`ht[2]` 双表 + `rehashidx`、SipHash、渐进式 rehash（每次 1 桶）、扩容/缩容阈值、`dict_can_resize`。

**失败路径**：rehash 中的增删改查双表操作、缩容触发边界、大 key 扩容时的阻塞。

**跨域桥**：R-1（HT 编码）、R-7（intset 升级为 HT）、R-6（zset 的 dict 侧）。

#### R-5 quicklist / List
**读者问题**：List 为什么从 ziplist 迁移到 listpack？

**入口**：`src/quicklist.c`、`src/listpack.c`、`src/ziplist.c`、`src/t_list.c`。

**状态核心**：`quicklistNode`（entry=ziplist/listpack、count、encoding RAW/LZF、container PLAIN/PACKED）、LRU/LFU 分页式 List 编码、ziplist 连锁更新缺陷、listpack 消除连锁更新、LZF 压缩中间节点。

**失败路径**：ziplist 连锁更新、节点分裂/合并、LZF 压缩阈值。

**跨域桥**：R-1（LIST type）、R-8（AOF 中 List 命令重写）、R-16 阻塞操作（BLPOP 依赖 List）。

#### R-6 skiplist + dict / ZSet
**读者问题**：ZSet 为什么同时用 skiplist 和 dict？

**入口**：`src/t_zset.c`、`src/server.h`（`zskiplistNode`/`zset`）。

**状态核心**：`zskiplistNode`（ele/score/backward/level[]{forward,span}）、`zslRandomLevel()`（P=0.25，最大 32 层）、`dict*` + `zskiplist*` 共享 ele、span 实现 O(logN) ZRANK。

**失败路径**：level 生成概率、score 相同时的 ele 字典序、删除时内存释放。

**跨域桥**：R-3 Dict、R-13 GEO（ZSet 存 geohash score）。

#### R-7 intset / Set
**读者问题**：Set 为什么在小整数集合下用定长数组？

**入口**：`src/intset.c`（560 行）、`src/t_set.c`。

**状态核心**：`intset`（encoding uint16/32/64 + length + contents[]）、有序 + 二分查找、升级（超 512 或出现非整数值 → HT）、`set-max-intset-entries`。

**失败路径**：升级时的全量重分配、降级条件（Redis 不降级）。

**跨域桥**：R-3 Dict（HT 编码）、R-1（SET type）。

#### R-10 Stream / rax
**读者问题**：Stream 如何做到按 ID 高效插入与范围查询？

**入口**：`src/rax.c`（1933 行）、`src/t_stream.c`（4055 行）。

**状态核心**：Radix Tree（压缩前缀树：iskey/isnull/iscompr/size/data[]）、`stream`（rax 消息树 + length + last_id + cgroups）、`streamCG`（last_id + pel + consumers）、消费者组 XREADGROUP + ACK + PEL。

**失败路径**：rax 分裂/压缩、消费者组 PEL 增长、last_id 推进时序。

**跨域桥**：R-6（部分命令语义）、R-16 阻塞读取（XREAD BLOCK 复用 blocked.c）。

### B. 事件驱动核心层（R-2）

#### R-2 事件驱动 + IO 多线程

**读者问题**：Redis 单线程为什么还能支撑 10 万级 QPS？6.0 之后 IO 多线程怎么不破坏无锁主线？

**入口**：`src/ae.c`（493 行）、`src/ae_epoll.c`、`src/networking.c`（4653 行）、`src/server.c`（`main`/`serverCron`/`processCommand`/`call`）。

**状态核心**：`aeEventLoop`（setsize/events/fired/timeEventHead/beforesleep/aftersleep）、多路复用统一接口（epoll/kqueue/select）、`serverCron`（100ms/hz=10）、IO 多线程（`io_threads_op` READ/WRITE 两阶段：beforesleep → IO 线程并行读写 → 主线程单线程执行命令）。

**失败路径**：IO 线程与主线程的边界、`beforesleep` 中读写阶段切换、命令执行期间禁止 IO 线程写。

**跨域桥**：R-8（AOF 写入时机在事件循环内）、R-9（replicationCron）、R-16（blocked.c）、R-17（tracking 失效推送）。

### C. 持久化与复制层（R-8、R-9）

#### R-8 持久化 RDB + AOF
**读者问题**：RDB 与 AOF 各自解决什么问题？7.0 的 Multi-part AOF 为什么重要？

**入口**：`src/rdb.c`（4070 行）、`src/aof.c`（2754 行）。

**状态核心**：RDB `SAVE`/`BGSAVE`（fork COW + CRC64 + rename 原子替换）、AOF `flushAppendOnlyFile()`（write + fsync always/everysec/no）、AOF Rewrite（fork + pipe 增量）、Multi-part AOF（base + incremental + manifest）。

**失败路径**：fork 期间阻塞、fsync 策略抖动、rewrite 期间的增量丢失风险。

**跨域桥**：R-2（serverCron 触发）、R-9（全量复制的 RDB 传输）。

#### R-9 主从复制 PSYNC
**读者问题**：主从如何只同步增量？replid 与 replid2 解决什么问题？

**入口**：`src/replication.c`（4231 行）。

**状态核心**：全量（SLAVEOF → BGSAVE → RDB 传输 → Load）、PSYNC 部分同步（`repl_backlog`：`ref_repl_buf_node` 链表 + `blocks_index` rax + histlen + offset）、`replid`+`replid2` 链式复制、repl-backlog-size 默认 1MB。

**失败路径**：backlog 被覆盖导致退化为全量、offset 不连续、主从切换后 replid 漂移。

**跨域桥**：R-8（RDB 传输）、R-2（replicationCron）。

### D. 高可用层（R-14、R-15）

#### R-14 Sentinel
**读者问题**：Sentinel 如何做主客观下线判断与故障转移？

**入口**：`src/sentinel.c`（5463 行）。

**状态核心**：`sentinelHandleRedisInstance()`、SDOWN（PING 超时）、ODOWN（quorum 确认）、选最优 slave（priority + offset + runid）、SLAVEOF NO ONE、配置改写。

**失败路径**：脑裂、quorum 不足、选主规则边界。

**跨域桥**：R-9（slave promotion 语义）、R-2（定时循环）。

#### R-15 Cluster
**读者问题**：Cluster 如何分片、重定向、维护拓扑与故障转移？

**入口**：`src/cluster.c`、`src/cluster_legacy.c`（6526 行）。

**状态核心**：16384 slot（CRC16 % 16384）、MOVED / ASK / IMPORTING / MIGRATING、Gossip（PING/PONG/MEET/FAIL）、`clusterCron` 10 次/秒、故障转移（PFAIL → 投票 → 接管 slot）。

**失败路径**：slot 迁移一致窗口、Gossip 消息放大、fail 状态收敛。

**跨域桥**：R-3（CRC16 依赖 dict？否，独立）、R-2（clusterCron）。

### E. 高级特性层（R-11、R-12、R-13、R-16、R-17、R-18）

原规划的 8 个扩展域方向正确，建议按批次并入主干之后的"机制补深层"：

#### R-11 Bitmap、R-12 HyperLogLog、R-13 GEO
三个"基于现有结构的算法编码"域，共同点是把内存当数组/寄存器/位图用。建议并成一篇"位与概率与空间编码专题"或保持三篇小文。

#### R-16 事务（MULTI/EXEC/WATCH）
**入口**：`src/multi.c`（482 行）。**状态核心**：命令入队、WATCH optimistic lock、EXEC。**失败路径**：WATCH 失效、EXEC 中途失败语义。

#### R-17 客户端缓存 tracking
**入口**：`src/tracking.c`（648 行）。RESP3 推送失效、BCAST/OPTIN/OPTOUT。

#### R-18 内存碎片整理 defrag
**入口**：`src/defrag.c`（1270 行）。`activeDefragCycle()` + `je_get_defrag_hint()`。

## 四、需要修订的旧判断

### 1. `.c` 文件数与 `intset.c`/`ae.c` 行数修正

- `.c` 文件：115 → **107**（规划多计 8）
- `intset.c`：200+ → **560**
- `ae.c`：~600 → **493**
- 其余核心文件行数全部一致。

### 2. 事件驱动 R-2 必须前置

原阅读顺序把 R-2 放在 R-13 GEO 之后。这是最大的结构问题——事件驱动是 Redis 性能主线的入口，应放到数据结构层之后、持久化之前：

```
R-1 redisObject → R-4 SDS → R-3 Dict → R-5 List → R-6 ZSet → R-7 Set
→ R-10 Stream → **R-2 事件驱动** → R-11 Bitmap/R-12 HLL/R-13 GEO
→ R-8 持久化 → R-9 复制 → R-14 Sentinel → R-15 Cluster
→ R-16 事务 → R-17 tracking → R-18 defrag
```

### 3. `networking.c` 不能整块淘汰

规划把"网络传输层"（`tls.c`/`anet.c`/`connection.c`）整块淘汰。但 `networking.c` 是核心域 R-2 的必需文件（IO 多线程在两阶段读写中直接操作它），必须保留为 R-2 的锚点来源。

### 4. 入门集成卷（可选）

Redis 是服务器，无 Spring 集成。但对 Java 读者角度，可把 `spring-boot-starter-data-redis` + Lettuce/Jedis 的连接管理作为**独立入门卷**（类似 MyBatis-Plus 依赖 MyBatis），标记为暂缓候选而非必做。

### F. 生产排障层（正式纳入，非候选）

以下生产排障域是面试重难点，正式纳入卷结构，标记为"排障层"：

#### R-19 RDB 持久化阻塞与 fork 陷阱
**入口**：`src/rdb.c:1636` `rdbSaveBackground()`（fork 子进程）、`rdbSave()`（SAVE 命令阻塞）。
**状态核心**：SAVE 全程阻塞（`rdb.c:1335` `rdbSave()` → `rdbSaveRio()` 序列化），BGSAVE fork 后 COW（Copy-On-Write）内存放大，`redisFork(CHILD_TYPE_RDB)` 在 `src/rdb.c:1646`、fork 耗时与实例内存正相关（`redisFork` 在 `src/` 中定义，`COW_DISABLE` 等配置）。
**失败路径**：fork 时内存超卖 → OOM killer、COW 峰值内存翻倍、`lastbgsave_time` 过长导致连续 fork 拒绝。
**跨域桥**：R-8（持久化主干）、R-2（serverCron 触发 BGSAVE）。

#### R-20 大 key 删除与 lazyfree 异步释放
**入口**：`src/lazyfree.c:13` `lazyfreeFreeObject()`、`lazyfree.c:23` `lazyfreeFreeDatabase()`、`src/db.c` `dbAsyncDelete()`、`src/server.c` `delCommand`/`unlinkCommand`、`src/evict.c` `lazyfree-lazy-eviction`。
**状态核心**：DEL 同步删除（`dictDelete()` O(N) 逐元素释放）vs UNLINK 异步删除（`lazyfreeFreeObject` 将释放任务交给 `bio` 后台线程）。`lazyfree_objects` 原子计数器追踪待释放对象数。
**失败路径**：大 key 的 `dictDelete()` 遍历全部元素阻塞主线程、lazyfree 累计太多对象来不及释放、FLUSHALL ASYNC 的 `emptyDbAsync()`。
**跨域桥**：R-3 Dict（dictDelete 的遍历逻辑）、R-4（SDS 释放链）、R-5~R-7（quicklist/skiplist/intset 的逐元素释放）。

#### R-21 复制 backlog 溢出与全量重同步风暴
**入口**：`src/replication.c:104`-`:121` `replBacklog` 初始化、`repl_backlog` 的 `ref_repl_buf_node` 链表 + `blocks_index`（rax 树索引）、`addReplyReplicationBacklog()`。
**状态核心**：`repl-backlog-size` 默认 1MB、backlog 被覆盖时 PSYNC 退化为全量（RDB 传输 + Load）。链式复制中 `replid2` 防止全量回退。
**失败路径**：backlog 太小 → 频繁全量重同步 → 主节点 fork + RDB 发送压力 → 线上毛刺，主从同时 SLAVEOF 形成复制风暴。
**跨域桥**：R-9（PSYNC 主干）、R-19（RDB fork 阻塞）。

#### R-22 AOF fsync 与磁盘抖动
**入口**：`src/aof.c` `flushAppendOnlyFile()`（`write()` + `fsync()` 的 always/everysec/no 三种策略）、`aof_background_fsync()`、`aof_fsync_offset`。
**状态核心**：`everysec` 模式下 `fsync` 后置，`bio` 线程执行 `aof_background_fsync()`。`no-appendfsync-on-rewrite` 配置避免 rewrite 期间 fsync 阻塞。
**失败路径**：`always` 策略下每个命令都 `fsync` → 磁盘 QPS 瓶颈、`everysec` 下 `fsync` 调度延迟 → 最多丢 1s 数据、rewrite 期间 `fsync` 竞争。
**跨域桥**：R-8（AOF 主干）、R-2（`beforesleep` 调用 `flushAppendOnlyFile` 的时机）。

#### R-23 过期 key 删除与阻塞边界
**入口**：`src/expire.c` `activeExpireCycle()`、`expire.c` 惰性删除（`expireIfNeeded()`）、`src/server.c` `serverCron` 调用 `activeExpireCycle`。
**状态核心**：惰性删除（访问时检查 `expireIfNeeded`）+ 定时删除（`activeExpireCycle` 在 `serverCron` 中每 100ms 执行，限定时间预算）。`ACTIVE_EXPIRE_CYCLE_LOOKUPS_PER_LOOP` 控制每次扫描的 key 数。
**失败路径**：大量 key 同时过期（`expireat` 同一秒）→ `activeExpireCycle` 一次扫不完 → 内存堆积，大 key 过期时 `expireIfNeeded` 同步删除阻塞主线程。
**跨域桥**：R-3 Dict（dictGetRandomKeys 随机采样）、R-20（lazyfree 可优化过期删除）。

#### R-24 慢命令阻塞与 IO 多线程边界
**入口**：`src/networking.c` `processCommand()`/`call()`/`IO 线程读写阶段`、`src/server.c` `processCommand`。
**状态核心**：`KEYS`（`dictScan` 全表扫描）、`SMEMBERS`（Set 全体返回）、`ZRANGE`（大 ZSet 范围查询）、`EVAL`（Lua 脚本执行期间不交出控制权）。IO 多线程只负责读写，不参与命令执行。
**失败路径**：`KEYS` 在百万级 key 上阻塞数秒、`EVAL` 死循环阻塞所有客户端、IO 线程读入大量命令后排队等待主线程执行。
**跨域桥**：R-2（IO 多线程边界）、R-3（dictScan 实现）、R-6（ZRANGE 的 skiplist 遍历）。

### G. 命令执行基础设施层（R-25、R-26）—— 用户补充的缺口

原规划遗漏了 Redis 的两大地基：**缓冲区体系** 和 **命令执行全流程**。这两块既是阅读任何数据结构/持久化域的前置背景，也是面试高频。现补入：

#### R-25 缓冲区体系：client 输入/输出缓冲、AOF 缓冲、复制缓冲

**读者问题**：Redis 主线程只有一个，为什么能同时处理大量客户端的读写？每个客户端、每类副本、AOF 各自持有哪些缓冲区？缓冲区什么时候会撑爆、会被怎么处理？

**入口与状态核心**：

1. **客户端输入缓冲 `querybuf`**：`src/networking.c:147` `c->querybuf = sdsempty()`（SDS 累积客户端查询）、`:148` `querybuf_peak`（峰值追踪）。`processInputBuffer()` 在 `networking.c:2559` 里解析 `querybuf` 中完整命令。`PROTO_INLINE_MAX_SIZE` / `PROTO_MAX_BULK_LEN` / `proto-max-bulk-len` 限制单条命令/批量大小。**客户端最大输入缓冲 `client-query-buffer-limit`**（`src/networking.c:2258` 附近 `PROTO_DUMP_LEN` 转储）。`pending_querybuf`（主从复制时从节点累积的未处理命令，`src/networking.c`）。

2. **客户端输出缓冲：`buf` + `reply` 双向**：`src/server.h:1166` `querybuf` 与 `:1185` `reply`（`list *reply`）是一对。**静态缓冲快速写 + 链式缓冲溢出写**：`networking.c:71` 附近 `clientReplyBlock`（`src/server.h:933`-`936`，`char buf[]` 定长 block）、`addReply*` 系列在 `src/networking.c:342` 附近先写 `buf`，溢出转 `reply` 列表。`c->bufpos`（`src/server.h:1147`）、`c->sentlen`（`:1188` 已发送字节）。

3. **客户端输出缓冲限制 `client-output-buffer-limit`**：对 **normal / replica / pubsub 三类客户端** 分别设 `obuf_hard_limit`（立刻断开）与 `obuf_soft_limit`（持续超过 N 秒才断开），`src/server.h:1195` `obuf_soft_limit_reached_time`。物理上由 `clientsCron` 周期检查。

4. **AOF 缓冲 `aof_buf`**：`src/server.h:1788` `sds aof_buf`（"AOF buffer, written before entering the event loop"）、`src/aof.c:1117` `nwritten = aofWrite(server.aof_fd, server.aof_buf, ...)`。命令传播后先进 `aof_buf`，`beforeSleep()` 调用 `flushAppendOnlyFile()` 落盘。`aof_fsync_offset`、`aof_pref` 控制 fsync。

5. **复制缓冲 `repl_buffer_blocks` / backlog**：`src/server.h:1894` `repl_buffer_mem`（复制缓冲总内存）、`:1895` `repl_buffer_blocks`（`list *` 复制缓冲块链表）。主节点把写命令同时传播到 `aof_buf` 和 replica 的 `repl_buffer_blocks`。backlog 是环形缓冲（`src/replication.c:104` 等）。`repl_backlog`（`server.h:1879`-`1881`）。两者有内存上限与释放时机（`defer_free_client`/`clientMemUsage`）。

6. **内存记账与阈值**：`client-memory-usage`、`maxmemory-clients`（客户端聚合内存限制）、`CLIENT KILL`、`setProtocolError`。

**失败路径**：`querybuf` 无限增长（恶意客户端不发完整命令）→ `client-query-buffer-limit` 断开；`reply` 链式缓冲膨胀（`MONITOR`/`PSUBSCRIBE` 高流失客户）→ `client-output-buffer-limit` 断开；`repl_buffer_blocks` 无限增长（从节点长期掉线不消费）→ 主节点内存爆掉；`aof_buf` 在 `always` 模式下每次命令后落盘导致吞吐瓶颈。

**跨域桥**：R-2（事件循环/IO 线程贯穿读写全程）、R-8（AOF 缓冲）、R-9/R-21（复制缓冲与 backlog）、R-16（MULTI 命令排队也涉及 querybuf）、R-24（慢命令与输出缓冲膨胀的叠加）。

#### R-26 命令执行全流程：从网络读到响应返回

**读者问题**：一次 `SET key value` 从雷达到 Redis 返回 `+OK`，中间到底穿过哪几道门？哪些步骤在主线程，哪些可以并行？哪个阶段是公认的瓶颈？

**入口（执行链）**：

```
readQueryFromClient()                     src/networking.c（IO 线程读，或主线程 read）
  -> processInputBuffer()                 src/networking.c:2559（从 querybuf 解析完整命令）
    -> processCommandAndResetClient()     src/networking.c:2501
      -> processCommand()                 src/server.c（命令合法性/ACL/状态检查、WATCH、CLIENT_MULTI 入队）
        -> lookupCommand()                src/server.c:3200（命令表查询）
        -> call(c, CMD_CALL_FULL)         src/server.c（执行核心：慢日志、统计、传播）
          -> command 具体实现（如 setCommand -> t_string.c）
        -> addReply(...)                  src/networking.c（写响应到 buf/reply）
      -> beforeSleep()                    src/server.c:1637（AOF flush、复制传播、（IO 写阶段））
  -> IO 写线程 / 主线程写 socket
```

**状态核心分段**：

1. **读阶段**：`readQueryFromClient` → `querybuf` 累积（`networking.c:2181` 附近 `find newline`、`processMultibulkBuffer` 解析 RESP）。IO 多线程开启时，读在 IO 线程并行执行，`io_threads_op` 分为 `IO_THREADS_OP_READ/WRITE`。
2. **解析阶段**：`processInputBuffer` 把 `querybuf` 中的一条条命令按 RESP 协议拆成 `c->argv`/`c->argc`。`processMultibulkBuffer`、`processInlineBuffer` 两种解析器。
3. **前置检查 `processCommand`**：命令是否已加锁（`CLIENT_MULTI` 入队）、`lookupCommand`（`server.c:3200`）、ACL 检查、`maxmemory` 检查（`performEvictions`）、key 过期判断（惰性删除在 `lookupKeyRead/Write` 时触发）、只读从节点禁写、`server.masterhost` 检查、`clusterMode` 槽归属、`WATCH` 被修改拒绝。
4. **执行 `call()`**：`c->cmd->proc(c)` 调具体命令实现；`call` 内做：命令耗时统计（`server.stat_commands`）、慢日志（`slowlogPushEntryIfNeeded`）、命令传播决策（`dirty` 计数、`propagate` 到 AOF/复制的条件）。
5. **传播做账**：命令执行后 `dirty` 非 0 的命令被 `propagate`（`server.c`/`replication.c`）→ `feedAppendOnlyFile`（`aof.c`）写入 `aof_buf` + 写入 `repl_buffer_blocks` 给从节点。
6. **写阶段**：`addReply` 写入 `c->buf`，不够写进 `reply` 链表；`beforeSleep` 中 `flushAppendOnlyFile` 落盘 AOF、复制块推送给从节点、IO 写线程并行写 socket。

**失败路径**：`querybuf` 被大量半命令撑爆、`processCommand` 在 `maxmemory` 淘汰/惰性删除时阻塞、`call` 执行 `KEYS`/`EVAL` 阻塞主线程、`beforeSleep` 里 AOF fsync 慢阻塞整个事件循环、`addReply` 溢出到 `reply` 链表后客户端消费太慢被输出缓冲限制断开。

**跨域桥**：R-2（事件循环是执行全流程的"时钟"）、R-25（缓冲区是执行全流程的两个端点）、R-16（MULTI/EXEC 在 `processCommand` 里入队）、R-19（调 `SAVE` 时 `call` 阻塞）、R-21（传播链经过复制缓冲）。

#### R-27 Lua 脚本：原子性、EVAL/EVALSHA、脚本缓存、超时与集群模式

**读者问题**：Lua 脚本在 Redis 里为什么是"原子"的？这个"原子"到底指什么？为什么 SCRIPT KILL 有时能成功、有时只能 SHUTDOWN NOSAVE？`redis.replicate_commands()` 解决了什么？**集群模式下脚本又有什么额外限制？为什么跨槽位的脚本执行不了？**

**入口**：
- `src/eval.c:545` `evalGenericCommand()`——EVAL/EVALSHA 的统一入口
- `src/script.c:170` `scriptPrepareForRun()`——脚本执行前的准备与检查
- `src/script.c:329` `scriptKill()`——脚本终止逻辑
- `src/script.c:292` `scriptResetRun()`——脚本执行后的清理
- `src/cluster.c:956` `getNodeByQuery()`——集群模式下所有命令（含脚本、MULTI/EXEC）的统一 key 槽位检查
- `src/cluster.c:1181` `clusterRedirectClient()`——CROSSSLOT / TRYAGAIN / CLUSTERDOWN / MOVED / ASK 错误码

**状态核心**：

（基础部分见上。这里重点补集群模式——原规划只写了单机视角，漏了集群限制）

6. **集群模式的核心限制：脚本内所有 key 必须落在同一个 hash slot**。`src/cluster.c:956` `getNodeByQuery()` 把所有执行入口（单命令、MULTI/EXEC、EVAL/FCALL）统一成"多命令状态"处理：对每个命令调 `getKeysFromCommand()`（`src/db.c:2434`）提取 key 数组，逐 key 用 `keyHashSlot()` 计算 slot，只要有两个 key 属于不同 slot，就报 `CROSSSLOT`（`cluster.c:1181`）。**脚本的 KEYS[] 数组就是被检查的 key 来源**——`EVAL` 第 3 个参数起的 `KEYS[1..numkeys]` 就是命令的 key 规格。

7. **跨 slot 脚本的报错**：`-CROSSSLOT Keys in request don't hash to the same slot`（`cluster.c:1181`）。如果脚本只访问固定前缀 key（如 `{user123}:cart`），必须用 **hash tag `{}`** 强制同一个 slot；`ACL` 与 `MOVED/ASK` 重定向对脚本同样生效——脚本访问的 slot 不在本节点，会收到 MOVED 重定向到正确节点。

8. **槽位迁移期间的 TRYAGAIN**：如果目标 slot 正在 `MIGRATING`/`IMPORTING`，`cluster.c:1181` 返回 `-TRYAGAIN Multiple keys request during rehashing of slot`——脚本跨多 key 在迁移窗口期无法安全执行。

9. **`SCRIPT_FLAG_NO_CLUSTER` 与 `no-cluster` 脚本标志**：`src/script.c:179-183`——如果脚本声明了 `no-cluster` 标志（Redis 7.0 Functions 里用 `#!no-cluster` 或 `SCRIPT FLAGS` 声明），且 `server.cluster_enabled` 为真，直接拒绝：`"Can not run script on cluster, 'no-cluster' flag is set."`。

10. **集群 + 函数的联动**：FCALL 同样受 `getNodeByQuery` 的 slot 检查约束。集群下函数库发布（`FUNCTION LOAD`）由 clusterbus 传播，但**函数传播到其他节点后是非阻塞的**（`cluster.c` 中 `replicate_to_slave`/`obey_client` 逻辑），主节点执行脚本后，命令由复制链同步——这回到 `replicate_commands()` 的逐命令复制，保证随机命令在主从间一致。

**失败路径**（集群版补入）：
- 脚本访问两个不同 slot 的 key → `CROSSSLOT` 拒绝执行
- 无 hash tag 的固定前缀 key 无法在集群分布式中定位 → 必须 `{hash_tag}`
- slot 迁移窗口期内脚本访问迁移中 key → `TRYAGAIN`，重试
- 脚本命中的 slot 不在本节点 → MOVED 重定向，客户端需跟随新节点重发（脚本天然无法在单次执行里跨节点）

**跨域桥**：R-2（事件循环是脚本不被中断的原因）、R-26（EVAL 是 `processCommand → call` 的一个特殊分支）、R-9（复制模式影响主从一致性）、R-8（AOF 重写时 Lua 脚本的传播方式）、R-24（死循环脚本属于最严重的慢命令阻塞）、R-15 Cluster（slot/重定向/迁移是集群脚本限制的地基）、R-26（`getKeysFromCommand` 也被普通命令 key 提取使用）。

### H. 经典混淆补深层（R-28 ~ R-32）

以下域是面试高频、且常因概念混淆被问错的主题。单独成组，与排障层互补。

#### R-28 内存淘汰策略：LRU/LFU 的近似实现与 8 种策略

**读者问题**：Redis 的 LRU 和 LFU 为什么不是教科书版？"过期删除"和"内存淘汰"到底有什么区别？为什么面试问"Redis 内存满了怎么办"不能用"加内存"糊弄？

**入口**：
- `src/evict.c:23`-`36`  淘汰候选池（`evictionPoolEntry`/`idle`）
- `src/evict.c:117`-`162` `performEvictions()` 的辅助函数——`MAXMEMORY_FLAG_LRU`/`MAXMEMORY_FLAG_LFU` 分支
- `src/evict.c:228`-`255` LFU 实现（`LFUDecrAndReturn`、`LFU_INIT_VAL`）
- `src/evict.c` `performEvictions()` 主入口
- `src/config.c` 配置 `maxmemory`/`maxmemory-policy`/`maxmemory-samples`
- `src/db.c` 非直连 `freeMemoryIfNeeded()` 在 `processCommand` 中被调用

**状态核心**：

1. **8 种淘汰策略**：`noeviction`、`allkeys-lru`、`volatile-lru`、`allkeys-lfu`、`volatile-lfu`、`allkeys-random`、`volatile-random`、`volatile-ttl`。`volatile-*` 只对有 `expire` 的 key 生效，`allkeys-*` 对所有 key 生效。`noeviction` 直接返回错误。

2. **近似 LRU（Approximated LRU）**：`evict.c:36` `evictionPoolEntry` 维护一个候选池（`idle` 字段记录空闲时间）。`performEvictions()` 通过 `dictGetSomeKeys` 随机采样（`maxmemory-samples` 默认 5），把最久未访问的候选加入池中，淘汰池中 idle 最大的 key。不是教科书版 LRU（维护全局双向链表），而是**采样+淘汰池**的近似实现，因为全局链表成本太高。

3. **LFU 对数计数与衰减**：`evict.c:228`-`255` LFU 的 `lru` 字段拆为 16 位访问时间 + 8 位频率计数器（`LFU_INIT_VAL` 初始值）。计数器是对数增长（`LFULogIncr` 使用概率递增），不是线性加 1。`LFUDecrAndReturn` 在 `evict.c:231` 附近实现频率衰减，count 随时间周期减少。这就是"热点 key 衰减"的源码实现。

4. **过期删除 vs 内存淘汰的混淆**：**
   - **过期删除**（R-23）：`expire.c` 管"TTL 到了删 key"，**是 key 级**的，主动（`activeExpireCycle`）+ 被动（`expireIfNeeded`）
   - **内存淘汰**（R-28）：`evict.c` 管"maxmemory 满了踢 key"，**是内存级**的，在 `processCommand` 里的 `performEvictions` 调用
   - 两者互斥不互斥？可以同时发生——一个 key 可能同时有 TTL 且被淘汰策略选中

**失败路径**：`maxmemory-policy` 设为 `noeviction` 但写命令不断 → 返回 `OOM` 错误、`volatile-*` 策略下所有 key 都没设 TTL → 退化为 `noeviction`、`maxmemory-samples` 太小 → 近似 LRU 选出的不是真正最久未访问的 key。

**跨域桥**：R-23（过期删除 —— 与 R-28 是"易混淆对"）、R-1（`lru:24bit` 的双重身份）、R-26（`processCommand` 中 `performEvictions` 的调用时机）、R-20（`lazyfree-lazy-eviction` 配置让淘汰走 lazyfree）。

#### R-29 键空间与 SCAN 迭代器

**读者问题**：Redis 的 key 存在哪里？`redisDb` 到底是什么？为什么 `KEYS` 会阻塞而 `SCAN` 不会？SCAN 的 cursor 为什么叫"永不停止"？

**入口**：
- `src/server.h:968` `typedef struct redisDb`（`dict *dict` 键空间、`dict *expires` 过期字典、`kvstore *blocking_keys` 阻塞键列表等）
- `src/db.c:50` `lookupKeyRead`/`lookupKeyWrite` 与 `...WithFlags()` 变体——所有命令的 key 查找入口
- `src/dict.c:1369` `dictScan()`——SCAN 的核心迭代器
- `src/db.c` `keysCommand`（KEYS 实现：`dictScan` 全表遍历）

**状态核心**：

1. **`redisDb` 结构**：`server.h:968` 的 `struct redisDb` 包含 `dict *dict`（键空间）、`dict *expires`（过期字典）、`kvstore *blocking_keys`、`kvstore *watched_keys` 等。每个数据库实例对应一个 `redisDb`，默认 16 个（`server.dbnum`）。

2. **`lookupKeyRead` / `lookupKeyWrite`**：`db.c:50` 是所有命令访问 key 的必经之路。`lookupKeyRead` 触发 `expireIfNeeded`（惰性删除），`lookupKeyWrite` 还会做 `WATCH` 记录。`lookupKeyReadWithFlags` 的 `flags` 控制是否更新 LRU/LFU 等。

3. **KEYS 为什么阻塞**：`keysCommand`（`db.c`）调用 `dictScan` 全表遍历全部 key，返回所有匹配的 key。在百万级 key 上时，`dictScan` 遍历所有 dict 桶，主线程在此期间无法处理其他命令。

4. **SCAN 为什么是"游标式"**：`dict.c:1369` `dictScan(dict *d, unsigned long *v, dictScanFunction *fn, ...)` 的 `v` 是游标。每一次 `SCAN` 返回一个新游标（0 表示遍历完成）。`dictScan` 使用**反向二进制迭代**（reversed bits）保证不遗漏任何 key，但**不保证不重复**（因为 rehash 期间一个 key 可能被遍历两次）。

**失败路径**：`KEYS` 在百万级 key 上阻塞数秒、`SCAN` 在 rehash 过程中可能返回重复 key 或漏掉 key（但设计上保证不遗漏）、`SCAN` COUNT 参数太小 → 遍历次数太多。

**跨域桥**：R-3 Dict（`dictScan` 是 dict 的迭代器，依赖 rehash 理解）、R-23（`expireIfNeeded` 在 `lookupKeyRead` 中触发）、R-26（`lookupKey` 是命令执行全流程的必经之路）、R-28（淘汰时 `dictGetSomeKeys` 也是 dict 遍历）。

#### R-30 阻塞命令：BLPOP/BRPOP/BLMOVE 的实现

**读者问题**：`BLPOP key timeout` 发出后，key 一直没数据，客户端是怎么"挂起"的？另一个客户端 `LPUSH` 时，挂起的客户端怎么被"唤醒"的？超时后怎么恢复？

**入口**：
- `src/blocked.c:359` `blockForKeys()`——注册阻塞客户端
- `src/blocked.c:327`-`352` `signalKeyAsReady()`——key 被写入时唤醒阻塞客户端
- `src/blocked.c` `serveClientsBlockedOnListKey()`——满足条件时从阻塞队列中弹出
- `src/blocked.c:56` `c->bstate.timeout`——超时设置
- `src/blocked.c` `processUnblockedClients()`——超时后恢复执行

**状态核心**：

1. **阻塞注册**：`blockForKeys()` 在 `blocked.c:359` 设置 `c->bstate.timeout`、`btype`、`keys` 列表，把客户端加入 `db->blocking_keys` 字典。客户端状态标记为 `CLIENT_BLOCKED`。

2. **唤醒机制**：`signalKeyAsReady()` 在 `blocked.c:327` 当一个 key 被写入时（如 `LPUSH` 后），调用 `signalKeyAsReady` 检查 `db->blocking_keys` 中是否有客户端在等这个 key，如果有，把客户端加到 `server.ready_keys` 中。`beforeSleep`（`server.c:1637`）中调用 `handleClientsBlockedOnKeys()` 处理这些 ready keys。

3. **超时**：`c->bstate.timeout` 在 `blockForKeys` 时设置。`processUnblockedClients()` 在事件循环中定期检查，超时的客户端自动解除阻塞，返回 `nil`。

**失败路径**：超时时间设为 0（永不超时）→ 客户端一直阻塞直到有数据或被强制断开、`BLPOP` 在 List 只有一个元素时被多个客户端同时阻塞 → 只有一个能被唤醒（`signalKeyAsReady` 只唤醒一个）。

**跨域桥**：R-5（List 是阻塞命令的主要操作对象）、R-2（`beforeSleep` 中 `handleClientsBlockedOnKeys`）、R-26（`processCommand` 中对阻塞命令的特殊处理）、R-10（Stream 的 XREAD BLOCK 也走 `blocked.c`）。

#### R-31 发布订阅：PUB/SUB 的频道组、广播与可靠性

**读者问题**：Redis 的 Pub/Sub 为什么"发了就忘"？和 Stream 的消费者组到底有什么区别？和 List 的 BLPOP 有什么区别？

**入口**：
- `src/pubsub.c` `subscribeCommand` / `publishCommand` / `unsubscribeCommand` / `psubscribeCommand` 等
- `src/pubsub.c:49` `channelList()`——频道列表
- `src/pubsub.c:58`-`65` Pub/Sub type（global + shard level）
- `src/server.h` `pubsub_channels`（`kvstore`）/ `pubsub_patterns`（`list`）

**状态核心**：

1. **频道订阅**：客户端订阅频道后，`pubsub_channels` 字典（`kvstore`）记录频道 → 客户端列表的映射。`pubsub_patterns` 列表记录模式 → 客户端列表。

2. **消息发布**：`PUBLISH channel msg` 在 `pubsub.c` 中遍历 `pubsub_channels[channel]` 的所有客户端，向每个客户端写 `*3\r\n$7\r\nmessage\r\n...` 消息。同时遍历 `pubsub_patterns` 匹配模式，向匹配的客户端发送 `*3\r\n$8\r\npmessage\r\n...`。

3. **为什么"发了就忘"**：Pub/Sub 没有持久化。消息不写入 `aof_buf`，不写入 `repl_buffer_blocks`，不写入 backlog。断连的客户端收不到订阅期间的消息。

4. **Pub/Sub vs Stream vs List 消费者**：Pub/Sub 是"广播+易失"，Stream 是"持久化+消费者组"，List (BLPOP) 是"独占+易失"。三者的持久性、消息确认、消费独占性都不同。

**失败路径**：`PUBLISH` 在高频下客户端消费太慢 → 输出缓冲 `client-output-buffer-limit` 撑爆断开、客户端断线期间消息丢失、`PSUBSCRIBE` 模式匹配在大规模频道的性能开销。

**跨域桥**：R-25（输出缓冲限制）、R-10（Stream 消费者组对比）、R-5（List BLPOP 对比）、R-26（`call` 中发布命令的传播路径）。

#### R-32 ACL 权限控制

**读者问题**：Redis 6.0 的 ACL 怎么实现用户隔离？`default` 用户有什么权限？`ACL CAT` 和 `acl_categories` 怎么工作？

**入口**：
- `src/acl.c` `ACLLogin()` / `authCommand()` / `ACLSetUser()` / `ACLCheckCommandPermissions()`
- `src/acl.c:75`-`81` `acl_categories` 标志位
- `src/server.c` `processCommand` 中的 ACL 检查点

**状态核心**：
1. `default` 用户默认有全部权限。`ACL SETUSER` 创建新用户并设置 `+@all` / `-@all` 等规则。
2. `acl_categories` 用位标志按命令类别（`@string`、`@list`、`@set`、`@keyspace` 等）做权限检查。
3. `ACL LOG` 记录失败尝试。

**失败路径**：ACL 权限配置错误导致服务不可用、`default` 用户被误删。

**跨域桥**：R-26（`processCommand` 中 ACL 检查点）。

#### 阅读位置

命令执行基础设施层建议放在**事件驱动 R-2 之后、数据结构之前**或**R-2 之后紧接**，因为它是"命令如何进入并离开 Redis"的骨架；缓冲区体系与命令执行全流程相互依存，需成对阅读。

排障层与基础设施层的位置：

```
数据结构层 → R-2 事件驱动 → R-25 缓冲区 + R-26 命令执行全流程 → 持久化/复制/高可用 → 排障层 → 高级特性层
```

## 六、建议的新阅读顺序

```text
R-1 redisObject
  -> R-4 SDS
    -> R-3 Dict
      -> R-5 List (quicklist)
        -> R-6 ZSet (skiplist+dict)
          -> R-7 Set (intset)
            -> R-10 Stream (rax)
              -> R-2 事件驱动 + IO 多线程
                -> R-25 缓冲区体系（querybuf / buf+reply / aof_buf / repl_buffer）
                  -> R-26 命令执行全流程（readQueryFromClient → addReply）
                    -> R-27 Lua 脚本原子性（EVAL/EVALSHA/SCRIPT KILL/SCRIPT_WRITE_DIRTY）
                      -> R-29 键空间与 SCAN（redisDb / lookupKey / KEYS vs SCAN）
                        -> R-28 内存淘汰策略（8种策略 / 近似LRU / LFU衰减 / 过期 vs 淘汰）
                          -> R-30 阻塞命令（BLPOP/BRPOP / blockForKeys / signalKeyAsReady）
                            -> R-31 发布订阅（PUB/SUB / 频道组 / 广播失效 / vs Stream）
                              -> R-32 ACL 权限控制（用户 / acl_categories / 权限检查）
                                -> R-11 Bitmap / R-12 HLL / R-13 GEO（扩展域，可分批）
                                  -> R-8 持久化 (RDB + AOF)
                                    -> R-9 复制 (PSYNC)
                                      -> R-14 Sentinel
                                        -> R-15 Cluster
                                          -> R-19 RDB 阻塞 / R-20 大 key lazyfree / R-21 复制 backlog
                                            -> R-22 AOF fsync / R-23 过期删除 / R-24 慢命令阻塞
                                              -> R-16 事务 / R-17 tracking / R-18 defrag
```

## 七、方法论适配说明

这是四卷中第一个 **C 语言项目**。方法论适配：

1. **锚点格式**：`src/xxx.c:行号`，Java 的"类.方法"改为"文件.函数"。
2. **无 MCP 索引**：只能用 grep/glob + 行号锚点，跨文件函数调用需手动 `grep`。
3. **测试证据**：Redis 用 `tests/`（Tcl 编写），测试证据从 `tests/unit/` 引用。
4. **四件套不变**：rewrite-plan → 正文 → note → review-notes 流程与语言无关。

## 八、方法论自检结果

- 已修正仓库事实（.c 文件数、intset/ae 行数）。
- 已把事件驱动 R-2 前置到数据结构之后。
- 已把 `networking.c` 从淘汰清单中拉回 R-2。
- 已明确 C 语言项目的方法论适配。
- 当前仍未完成：逐域源码锚点表、`tests/` 测试证据盘点、以及 Redis 全卷的正式正文四件套。