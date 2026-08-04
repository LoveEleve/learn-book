# Redis 源码学习范围规划

> **版本**: 7.4.2
> **仓库**: `/data/workspace/source-code/code/spring/redis/`
> **语言**: C (115 .c + 68 .h 文件)
> **日期**: 2026-08-04
> **聚焦**: 内存数据结构(sds/dict/quicklist/ziplist/skiplist) + 事件驱动(ae) + 持久化(RDB/AOF) + 复制 + RedisObject 类型系统

---

## 一、仓库概况

Redis 7.4.2 — 高性能内存 KV 数据库。核心语言是 C，**单线程事件驱动 + IO 多线程**（6.0+）。架构核心：`redisObject` 类型系统 + `ae` 事件循环 + 丰富的数据结构编码优化。

**源码模块**：

| 模块 | 文件 | 行数 | 职责 | 纳入 |
|:---:|:---:|:---:|---|---|
| `server.c/h` | 1 | 7256 | 主事件循环、命令表、启动初始化 | ✅ 核心 |
| `ae.c/h` + `ae_epoll.c` | 3 | ~600 | epoll/kqueue/select 事件驱动框架 | ✅ 核心 |
| `networking.c` | 1 | 4653 | 客户端连接、RESP 协议解析 | ✅ 核心 |
| `dict.c/h` | 2 | 2056 | 渐进式 rehash 哈希表 | ✅ 核心 |
| `sds.c/h` | 2 | 1473 | Simple Dynamic String——二进制安全字符串库 | ✅ |
| `object.c` | 1 | 1677 | redisObject 创建/释放/编码决策 | ✅ 核心 |
| `quicklist.c` | 1 | 3334 | quicklist(双向链表+ziplist/listpack) | ✅ |
| `ziplist.c` | 1 | 2665 | 压缩列表——紧凑内存布局 | ✅ |
| `listpack.c` | 1 | 3150 | listpack——替代 ziplist | ✅ |
| `t_string.c` | 1 | 930 | String 命令(SET/GET/INCR/APPEND) | ✅ |
| `t_hash.c` | 1 | 3418 | Hash 命令(HSET/HGET/HGETALL) | ✅ |
| `t_list.c` | 1 | 1364 | List 命令(LPUSH/RPOP/LRANGE) | ✅ |
| `t_set.c` | 1 | 1658 | Set 命令(SADD/SMEMBERS/SINTER) | ✅ |
| `t_zset.c` | 1 | 4513 | Sorted Set(skiplist+dict), ZRANK/ZRANGE | ✅ |
| `t_stream.c` | 1 | 2000 | Stream(rax树), XADD/XREAD/XGROUP | 🟡 |
| `rdb.c` | 1 | 4070 | RDB 快照持久化——BGSAVE/SAVE | ✅ 核心 |
| `aof.c` | 1 | 2754 | AOF 追加日志——appendfsync(AOF+Rewrite) | ✅ 核心 |
| `replication.c` | 1 | 4231 | 主从复制——PSYNC(partial sync) | ✅ 核心 |
| `expire.c` | 1 | 838 | 惰性删除+定期删除 | ✅ |
| `evict.c` | 1 | 761 | LRU/LFU 内存淘汰(8 种策略) | ✅ |
| `lazyfree.c` | 1 | 274 | UNLINK/FLUSHALL ASYNC——异步释放 | 🟡 |
| `bio.c` | 1 | 439 | 后台 IO 线程(close fd/AOF fsync/lazy free) | 🟡 |
| `multi.c` | 1 | 482 | MULTI/EXEC/WATCH 事务 | 🟡 |
| `pubsub.c` | 1 | 748 | PUBLISH/SUBSCRIBE/PSUBSCRIBE | 🟡 |
| `blocked.c` | 1 | 746 | BLPOP/BRPOP 阻塞操作 | 🟡 |
| `script_lua.c` `eval.c` | 2 | 2000 | Lua 脚本(EVAL/EVALSHA) | 淘汰 |
| `cluster.c` `cluster_legacy.c` | 2 | ~3000 | 集群模式 | 淘汰 |
| `sentinel.c` | 1 | 5463 | Sentinel 哨兵 | 淘汰 |
| `module.c` | 1 | 5000 | Redis Module API | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（8 个）

| 编号 | 域 | 核心文件 | 说明 |
|:---:|---|---|---|
| R-1 | **redisObject 类型系统** | object.c, server.h | **`struct redisObject`**(16B)：`type:4bit`(STRING/LIST/SET/ZSET/HASH) + `encoding:4bit`(RAW/INT/EMBSTR/ZIPLIST/QUICKLIST/SKIPLIST/HT/LISTPACK) + `lru:24bit`(LRU时钟/LFU 8bit freq+16bit access) + `refcount`(引用计数) + `*ptr`(实际数据指针)。**编码决策**：`createStringObject()` — 长度≤44B→EMBSTR(单次malloc), >44B→RAW(两次malloc); 整数→INT encoding(共享0-9999)。**共享对象**：0-9999 整数 `shared.integers[]` 数组全局共享。**type/encoding 分离**：type 对外(用户视角), encoding 对内(内存优化)——LIST 可升级为 HASHTABLE(HGETALL) 或降级为 ZIPLIST(小规模) |
| R-2 | **事件驱动—ae 框架** | ae.c, ae_epoll.c, networking.c | **`aeEventLoop`**(源码验证)：`int setsize`(最大fd数) + `aeFileEvent *events`(动态分配,非固定数组) + `aeFiredEvent *fired` + `timeEventHead`(时间事件链表) + `beforesleep`/`aftersleep`(前后睡眠回调) + `void *apidata`(epoll/kqueue 特定数据)。**支持 4 种多路复用**：epoll(Linux)/kqueue(BSD)/evport(Solaris)/select(通用)。**文件事件**：`aeCreateFileEvent(fd, mask, handler)` → `aeApiAddEvent(eventLoop, fd, mask)` → epoll_ctl。**`aeProcessEvents`**：`aeApiPoll(eventLoop, tvp)` → 遍历 `fired[j]` → `fe->rfileProc/fefileProc`。**时间事件**：`aeCreateTimeEvent(milliseconds, handler)` → `serverCron`(每 100ms, hz=10, server.hz 可配)。**`aeMain`** 主循环：`while(!stop){ aeProcessEvents(flags) }` → `beforesleep` 处理未 flush 的客户端响应 → IO 线程(6.0+) → epoll_wait |
| R-3 | **Dict—渐进式 rehash 哈希表** | dict.c, dict.h | **`struct dict`**：`ht_size_exp[2]`(双表, ht[0]当前, ht[1]rehash目标) + `rehashidx`(-1=未rehash, >=0=rehash进行中) + `iterators`(安全迭代器计数)。**SipHash 哈希算法**(源码验证：`dictGenHashFunction` → `siphash(key,len,seed)`，非 MurmurHash)。**渐进式 rehash**：rehash 过程中每次查找/插入同时 `dictRehash(d, 1)` 迁移 1 个桶 → 避免一次性阻塞。**rehash 触发**(源码验证 `dictExpandIfNeeded`)：`dict_can_resize==ENABLE` 且 `used >= size`(负载>1)→扩容；`dict_can_resize!=FORBID`(正在BGSAVE)且 `used >= dict_force_resize_ratio(=4)*size`(负载>4)→强制扩容。**缩容**(`dictShrinkIfNeeded`)：`size > DICT_HT_INITIAL_SIZE` 且 `used * HASHTABLE_MIN_FILL(8) * dict_force_resize_ratio(4) < size`(负载<1/32≈3.1%)。**dictEntry**：`key + union{v:void*/u64/s64/d} + next`(链表)。**迭代器**：safe(允许操作dict) vs unsafe(不允许) |
| R-4 | **SDS—Simple Dynamic String** | sds.c, sds.h | **5 种 header**(源码验证)：`sdshdr5`(仅 flags+buf[], 无独立 len/alloc——长度存在 flags 高 5 位) + `sdshdr8/16/32/64`(`len, alloc, flags, buf[]`)。存储: `s[-1] & SDS_TYPE_MASK` 判断类型。**二进制安全**：存 length 而非依赖 \0, `sdscatlen()` 可存 \\0 字节。**惰性空间释放**：`sdstrim()/sdsclear()` 不释放内存(`alloc` 保留), `sdsRemoveFreeSpace()` 主动缩容。**预分配策略**(源码验证)：`_sdsMakeRoomFor()` → `greedy=1` 时 <1MB→`newlen *= 2`; ≥1MB→`newlen += SDS_MAX_PREALLOC(1MB)`。sdshdr5 永不用于 expand(追加时自动升级到 sdshdr8) |
| R-5 | **Quicklist—双向链表+listpack** | quicklist.c, ziplist.c, listpack.c | **`struct quicklist`**(源码验证)：`head/tail` + `count`(总元素数) + `len`(节点数) + `fill:QL_FILL_BITS`(填充因子) + `compress:QL_COMP_BITS`(压缩深度)。**节点 `quicklistNode`**(源码验证)：`prev/next` + `entry`(listpack/ziplist 数据指针) + `sz`(字节数) + `count:16`(元素数) + `encoding:2`(RAW=1/LZF=2) + `container:2`(PLAIN=1/PACKED=2) + `recompress/attempted_compress/dont_compress`(压缩状态标志)。**ziplist(listpack 替代)**：紧凑布局——`<zlbytes><zltail><zllen><entry>...<zlend>` → entry=`<prevlen><encoding><data>`——**连锁更新**：插入/删除导致 prevlen 从 1B→5B, 连锁触发后续 entry 扩容(概率极低)。**listpack**：替代 ziplist(解决连锁更新)→每个 entry 存自身长度不存 prevlen→**无连锁更新**。**LZF 压缩**：中间节点(`fill>0`)可 LZF 压缩降低内存 |
| R-6 | **Skiplist—跳表(ZSet)** | t_zset.c, server.h(zskiplist) | **`struct zskiplist`**：`header + tail + length + level`(最大层级)。**节点 `zskiplistNode`**：`ele(sds) + score(double) + backward` + `level[]{forward, span}`(层级数组)。**层级随机化**(源码验证)：`zslRandomLevel()` → `while(random() < ZSKIPLIST_P*RAND_MAX) level++`(P=0.25, 期望 1.33层, 最大 ZSKIPLIST_MAXLEVEL=32)。**插入 `zslInsert(zsl, score, ele)`**：每层找前驱 update[i] → 随机生成 level → 更新 forward/backward/span → O(log N)。**范围查询 `zslGetElementByRank()`**：利用 span 累加→**O(log N)** 定位排名(类比 Redis ZRANK)。**ZSet 双结构**：zskiplist(for 范围查询 ZRANGE/ZREVRANGE) + dict(for 分值查询 ZSCORE)——两者共享同一 ele 指针避免重复 |
| R-7 | **持久化—RDB + AOF** | rdb.c, aof.c | **RDB(R2 4070行)**：`SAVE`(阻塞) → `rdbSave()` 遍历所有DB→每个key→`rdbSaveObjectType()`→根据 encoding 序列化→CRC64校验→`tmp-{pid}.rdb`→`rename`原子替换。`BGSAVE`(fork子进程, COW共享内存)→`rdbSaveBackground()`→子进程 exec。**AOF(R2 2754行)**：每条写命令追加到 `appendonly.aof`→`flushAppendOnlyFile()`→`write()`+`fsync()`(`appendfsync always/everysec/no`)。**AOF Rewrite**：`rewriteAppendOnlyFileBackground()`(fork子进程)→子进程 `rewriteAppendOnlyFileRio()` 遍历DB生成最小AOF(如 `RPUSH list "a" "b" "c"` 替代多条 RPUSH)→`aofReadDiffFromParent()` 增量管道→`rename`替换。**Multi-part AOF(7.0+)**：base AOF + incremental AOF→manifest 文件管理多个 AOF 文件 |
| R-8 | **主从复制—PSYNC** | replication.c(4231行) | **全量同步**：SLAVEOF → Master `BGSAVE` fork → `sendBulkToSlave()` 发送 RDB → Slave `rdbLoad()` 载入 → Master 发送积压缓冲区命令。**PSYNC 部分同步**(断线重连)：Master 维护 `repl_backlog`(环形缓冲区, 默认 1MB) + `replid`(复制ID) + `repl_offset`。Slave 断线重连 → `PSYNC replid repl_offset` → Master offset 在 backlog 中→发送增量命令; 否则→全量同步。**ReplBacklog 环形缓冲**：`char* buf[repl-backlog-size]` + `repl_backlog_idx`(写位置) + `repl_backlog_histlen`(有效数据长度)。**replid2**：从旧主切换到当前主时保存旧 replid——支持链式复制(级联)和故障转移 |

### 🟡 扩展域（3 个）

| 编号 | 域 | 核心文件 | 说明 |
|:---:|---|---|---|
| R-9 | **内存淘汰—LRU/LFU** | evict.c, expire.c | 8 种策略：noeviction/volatile-lru/allkeys-lru/volatile-lfu/allkeys-lfu/volatile-random/allkeys-random/volatile-ttl。**近似 LRU**：`redisObject.lru` 存全局时钟 `server.lruclock`(100ms 精度)→`evictionPoolPopulate()` 随机采样 N 个 key→选 idle 最大淘汰(非精确LRU, 省内存)。**LFU**：lru 拆为 8bit-counter+16bit-minute→counter 对数增长(`counter *= (1-1/(counter*lfu_log_factor+1))`)+衰减(`counter >>= LFU_DECR_PERIOD`) |
| R-10 | **Pub/Sub + 阻塞操作** | pubsub.c, blocked.c | **Pub/Sub**：`pubsub_channels`(dict channel→client list)→PUBLISH 遍历 client 列表推送。**Pattern**：`pubsub_patterns`(list)→PSUBSCRIBE `pattern`→PUBLISH 遍历 pattern 匹配→`stringmatchlen()`. **阻塞**：BLPOP `blocking_keys`(dict key→client list)→key 有数据→弹出返回; 无数据→`blockClient()`→`client.bpop.timeout`→timeout 或被 `signalKeyAsReady()` 唤醒 |
| R-11 | **Lazy Free + BIO** | lazyfree.c, bio.c | **Lazy Free**(UNLINK/FLUSHALL ASYNC)：`lazyfreeGetFreeEffort()` 估算释放代价(元素数+size)→> LAZYFREE_THRESHOLD(64)→`bioCreateLazyFreeJob()` 提交后台 BIO 线程→BIO 线程执行 `decrRefCount()`(引用计数归0→真正 free)。**BIO 三线程**：CLOSE_FILE(关闭fd)/AOF_FSYNC(fsync)/LAZY_FREE(异步释放)。**惰性删除 expire key**：`dbAsyncDelete()`→引用计数>1(共享对象)→后台 lazy free; =1→同步删除(快) |

---

## 三、淘汰清单

| 模块 | 理由 |
|:---:|---|
| `cluster.c` `cluster_legacy.c` | 集群模式——C 实现, 非单机核心 |
| `sentinel.c` | Sentinel 哨兵——运维 |
| `module.c` | Module API——扩展 |
| `script_lua.c` `eval.c` `function_lua.c` `functions.c` | Lua 脚本——C 实现, 上层功能 |
| `redis-cli.c` `redis-benchmark.c` `redis-check-*` | 客户端工具 |
| `debug.c` `memtest.c` `monotonic.c` | 调试/测试 |
| `geo.c` `geohash*.c` `hyperloglog.c` | 地理位置/HLL——特殊算法 |
| `bitops.c` | 位操作——特殊命令 |
| `defrag.c` | 内存碎片整理 |
| `latency.c` `slowlog.c` | 监控 |
| `tls.c` `connection.c` `anet.c` | 网络传输层 |

---

## 四、统计

| 类别 | 数量 |
|:---:|:---:|
| 🔴 核心域 | 8 |
| 🟡 扩展域 | 3 |
| **总域** | **11** |

## 五、关键架构关系

```
客户端请求 → networking.c(readQueryFromClient) → RESP协议解析(querybuf)
  → processCommand() → lookupCommand(c->argv[0]) → c->cmd->proc(c)
  → call(c) → propagate(c, PROPAGATE_AOF|REPL) → replicationFeedSlaves()

数据流: redisObject{type,encoding,lru,refcount,ptr}
  STRING: ptr→sds
  LIST:   ptr→quicklist(quicklistNode→ziplist/listpack)
  HASH:   encoding=HT→ptr→dict; encoding=LISTPACK→ptr→listpack
  SET:    encoding=HT→ptr→dict; encoding=LISTPACK→ptr→listpack
  ZSET:   ptr→zset{dict+skiplist}

持久化: serverCron(100ms) → flushAppendOnlyFile() (AOF fsync节奏)
  → activeExpireCycle() (过期扫描) → replicationCron() (每秒)

事件循环: aeMain → beforesleep → IO线程(io_threads_op=READ)
  → aeProcessEvents(timeout) → epoll_wait → 处理文件事件+时间事件
```

## 六、阅读顺序

```
R-1 redisObject → R-4 SDS → R-3 Dict → R-5 Quicklist → R-6 Skiplist
→ R-2 事件循环 → R-8 复制 → R-7 持久化 → R-9 淘汰 → R-10 发布订阅 → R-11 LazyFree
```
