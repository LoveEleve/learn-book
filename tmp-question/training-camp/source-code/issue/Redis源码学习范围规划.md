# Redis 源码学习范围规划

> **版本**: 7.4.2 | **仓库**: `/data/workspace/source-code/code/spring/redis/`
> **语言**: C (115 .c + 68 .h) | **日期**: 2026-08-04
> **对照**: 《Redis 高手心法》5 章 47 节 — 全覆盖核心数据结构/高可用/高级特性
> **R1**: grep 验证所有函数名/字段名/结构体/算法公式/行数（修复 24 处错误）

---

## 一、仓库概况

Redis 7.4.2 — 高性能内存 KV 数据库，C 语言实现，单线程事件驱动 + IO 多线程（6.0+）。

**源码模块全量审计**：

| 模块 | 行数 | 职责 | 对照书节 | 纳入 |
|:---:|:---:|---|---|:---:|
| `server.c/h` | 7256 | 主事件循环、命令表、启动初始化 | 1.2, 4.3 | ✅ |
| `networking.c` | 4653 | 客户端连接、RESP 协议解析 | 1.2 | ✅ |
| `ae.c/h` + `ae_epoll.c` | ~600 | epoll/kqueue/select 事件驱动 | 2.11, 4.3 | ✅ |
| `sds.c/h` | 1473 | SDS 字符串 | 2.1 | ✅ |
| `dict.c/h` | 2056 | 渐进式 rehash 哈希表 | 2.3, 2.4 | ✅ |
| `object.c` | 1677 | redisObject 创建/编码决策 | 2.x | ✅ |
| `quicklist.c` | 3334 | List 实现(双向链表+listpack/ziplist) | 2.2 | ✅ |
| `ziplist.c` | 2665 | 压缩列表(紧凑内存布局, 已渐弃用) | 2.2 | ✅ |
| `listpack.c` | 3150 | listpack(替代 ziplist, 无连锁更新) | 2.2 | ✅ |
| `t_list.c` | 1364 | List 命令(LPUSH/RPOP/LRANGE) | 2.2 | ✅ |
| `intset.c` | 200+ | 整数集合(Set/Hash 小规模编码) | 2.3 | ✅ |
| `t_set.c` | 1658 | Set 命令(SADD/SINTER/SDIFF) | 2.3 | ✅ |
| `t_hash.c` | 3418 | Hash 命令(HSET/HGETALL) | 2.4 | ✅ |
| `t_zset.c` | 4513 | ZSet(skiplist+dict 双结构) | 2.5 | ✅ |
| `t_string.c` | 930 | String 命令(SET/GET/INCR) | 2.1 | ✅ |
| `rax.c/h` | 1933 | Radix Tree(基数树) — Stream 核心 | 2.6 | ✅ **新** |
| `t_stream.c` | 4055 | Stream 命令(XADD/XREAD/XGROUP) | 2.6 | ✅ **新** |
| `bitops.c` | 1269 | Bitmap 命令(SETBIT/BITCOUNT/BITOP) | 2.8 | ✅ **新** |
| `hyperloglog.c` | 1597 | HyperLogLog(PFADD/PFCOUNT) | 2.9 | ✅ **新** |
| `geo.c` + `geohash*.c` | 1584 | GEO 命令(GEOADD/GEORADIUS) | 2.7 | ✅ **新** |
| `rdb.c` | 4070 | RDB 快照持久化(SAVE/BGSAVE) | 3.1 | ✅ |
| `aof.c` | 2754 | AOF 追加日志(append+rewrite) | 3.1 | ✅ |
| `replication.c` | 4231 | 主从复制(PSYNC 部分同步) | 3.2 | ✅ |
| `sentinel.c` | 5463 | Sentinel 哨兵(监控/故障转移) | 3.3 | ✅ **新** |
| `cluster.c` | 1445 | Cluster 集群(分片/槽位/重定向) | 3.4 | ✅ **新** |
| `expire.c` | 838 | key 过期(惰性+定期删除) | 4.2 | ✅ |
| `evict.c` | 761 | 8 种内存淘汰策略(LRU/LFU) | 4.2 | ✅ |
| `lazyfree.c` | 274 | UNLINK 异步释放 | 4.2 | ✅ |
| `bio.c` | 439 | 后台 IO 线程 | 4.2 | ✅ |
| `multi.c` | 482 | MULTI/EXEC/WATCH 事务 | 4.1 | ✅ |
| `pubsub.c` | 748 | PUBLISH/SUBSCRIBE | 4.4 | ✅ |
| `blocked.c` | 746 | BLPOP/BRPOP 阻塞操作 | 4.4 | ✅ |
| `tracking.c` | 648 | 客户端缓存(RESP3 推送失效) | 4.5 | ✅ **新** |
| `defrag.c` | 1270 | 内存碎片整理 | 4.7 | ✅ **新** |
| `script_lua.c` `eval.c` `functions.c` | 3000+ | Lua 脚本 | — | 淘汰 |
| `module.c` | 5000+ | Redis Module API(扩展) | — | 淘汰 |
| 其他(cli/benchmark/check/tls) | ~5000 | 客户端/测试/网络层工具 | — | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（10 个—数据结构 + 事件 + 持久化 + 复制）

| 编号 | 域 | 核心文件 | 对照书节 | 说明 |
|:---:|---|---|:---:|---|
| R-1 | **redisObject 类型系统** | object.c, server.h | 2.x | `struct redisObject`(`server.h` L882-894): `type:4bit`(STRING/LIST/SET/ZSET/HASH) + `encoding:4bit`(RAW/INT/HT/ZIPMAP已废弃/LINKEDLIST已废弃/ZIPLIST已废弃/INTSET/SKIPLIST/EMBSTR/QUICKLIST/STREAM/LISTPACK/LISTPACK_EX) + `lru:24bit`(LRU时钟/LFU 8bit freq+16bit access) + `refcount` + `*ptr`。EMBSTR 阈值 44B。共享整数 `shared.integers[OBJ_SHARED_INTEGERS=10000]`。type/encoding 分离——type 对外, encoding 对内(内存优化) |
| R-2 | **事件驱动—ae+IO多线程** | ae.c, ae_epoll.c, networking.c | 2.11, 4.3, 4.6 | `aeEventLoop`(`ae.h`): `int setsize` + `aeFileEvent *events`(动态分配) + `aeFiredEvent *fired` + `timeEventHead` + `beforesleep`/`aftersleep`。4 种多路复用(epoll/kqueue/evport/select)。`serverCron`(每 100ms, hz=10)。**IO 多线程**(6.0+, `networking.c`): `io_threads_op` 分 READ/WRITE 两阶段——`beforesleep` → IO 线程并行读/写 → 主线程单线程执行命令(保证无锁) |
| R-3 | **Dict—渐进式 rehash** | dict.c, dict.h | 2.3, 2.4 | `ht_size_exp[2]`双表 + `rehashidx`(-1=未rehash)。SipHash(`siphash(key,len,seed)`, 非 MurmurHash)。渐进式 rehash: 每次操作 `dictRehash(d,1)` 迁移1桶。扩容: `dict_can_resize==ENABLE`且负载>1; 强制扩容: 负载>`dict_force_resize_ratio(=4)`. 缩容: 负载<1/32(3.1%) |
| R-4 | **SDS** | sds.c, sds.h | 2.1 | 5种 header(`sdshdr5`仅flags+buf[], 无独立len/alloc; `sdshdr8/16/32/64`有`len/alloc/flags/buf[]`)。二进制安全(`sdscatlen()`)。预分配(`_sdsMakeRoomFor`): <1MB→`2*newlen`, ≥1MB→`newlen+1MB`。sdshdr5永不用于expand(追加自动升级) |
| R-5 | **quicklist—List** | quicklist.c, ziplist.c, listpack.c, t_list.c | 2.2 | `quicklistNode`: `prev/next` + `entry`(listpack/ziplist指针) + `sz` + `count:16` + `encoding:2`(RAW=1/LZF=2) + `container:2`(PLAIN=1/PACKED=2)。**ziplist**: `<zlbytes><zltail><zllen><entry>...<zlend>`, 连锁更新(prevlen 1B→5B)。**listpack**: 替代 ziplist——每 entry 存自身长度不理 prevlen→无连锁更新。**LZF 压缩**: 中间节点(`fill>0`)可 LZF 压缩 |
| R-6 | **skiplist+dict—ZSet** | t_zset.c, server.h | 2.5 | `zskiplistNode`: `ele(sds)+score(double)+backward+level[]{forward,span}`。`zslRandomLevel()`: `while(random()<ZSKIPLIST_P*RAND_MAX) level++`, P=0.25, 最大32层。`zset`: `dict*dict + zskiplist*zsl`, 两者共享同一 ele 指针避免重复。span 实现 O(logN) 排名(ZRANK) |
| R-7 | **intset—Set 小规模编码** | intset.c, t_set.c | 2.3 | `intset`: 有序整数数组——`encoding(uint16/uint32/uint64) + length + contents[]`。元素数少且全整数时 Set 使用 intset, 超过 `set-max-intset-entries=512` 或出现非整数值→升级为 HT(dict)。二分查找 O(logN) |
| R-8 | **持久化—RDB+AOF** | rdb.c, aof.c | 3.1 | **RDB**: `SAVE`(阻塞)→`rdbSave()`, `BGSAVE`(fork子进程COW)→`rdbSaveBackground()`, CRC64校验, `tmp-{pid}.rdb`→`rename`原子替换。**AOF**: `flushAppendOnlyFile()`→`write()+fsync()`(`always/everysec/no`)。**AOF Rewrite**: `rewriteAppendOnlyFileBackground()`(fork)→子进程`rewriteAppendOnlyFileRio()`, 父进程 pipe 收增量→`rename`替换。**Multi-part AOF**(7.0+): base+incremental+manifest |
| R-9 | **主从复制—PSYNC** | replication.c(4231行) | 3.2 | **全量**: SLAVEOF→BGSAVE→`replicationSetupSlaveForFullResync()`→`rdbLoad()`。**PSYNC部分同步**(7.x 新 `replBacklog`): `ref_repl_buf_node`(replBufBlock链表) + `blocks_index`(rax树索引快速定位offset) + `histlen`+`offset`。`repl-backlog-size`默认1MB。`replid`+`replid2`支持链式复制 |
| R-10 | **Stream—rax 树** | rax.c(1933行), t_stream.c(4055行) | 2.6 | **Radix Tree(rax)**: 压缩前缀树——节点含 `iskey/isnull/iscompr/size/data[]{child, key}`。**Stream 消息**: `stream{rax*rax(消息树)+length+last_id+cgroups}`。**消费者组**: `streamCG{last_id+pel(pending entries list)+consumers(rax)}`。XADD→追加到 rax 树。XREAD→按 ID 范围遍历。XREADGROUP→消费者组协调+ACK+PEL机制 |

### 🟡 扩展域（8 个—其余数据类型/高可用/高级特性）

| 编号 | 域 | 核心文件 | 对照书节 | 说明 |
|:---:|---|---|:---:|---|
| R-11 | **Bitmap** | bitops.c(1269行) | 2.8 | SETBIT/GETBIT/BITCOUNT/BITOP——基于 SDS 字符串的位操作。BITCOUNT 使用 SWAR(每字节查表256项+每32bit variable-precision)。BITOP AND/OR/XOR/NOT 按字节运算。亿级用户签到: 1亿位≈12MB |
| R-12 | **HyperLogLog** | hyperloglog.c(1597行) | 2.9 | PFADD/PFCOUNT/PFMERGE——基数估计算法。**稀疏矩阵**(HLL_SPARSE): ZERO/XZERO/VAL 3种 opcode。**稠密矩阵**(HLL_DENSE): 16384×6bit 寄存器。标准误差 0.81%。内存占用固定 12KB |
| R-13 | **GEO** | geo.c(1005行), geohash*.c(579行) | 2.7 | GEOADD→`geohashEncode()`(52bit编码经度纬度交错)→存为 ZSet(score=geohash)。GEORADIUS→`geohashGetAreasByRadiusWGS84()`计算9宫格→`membersOfAllNeighbors()`→Haversine公式计算精确距离。GEOSEARCH(6.2+): 替代 GEORADIUS, 支持圆形/矩形搜索 |
| R-14 | **Sentinel** | sentinel.c(5463行) | 3.3 | `sentinelHandleRedisInstance()`—主循环, 每秒对所有 monitoring master/slave/sentinel 执行。**SDOWN**(主观下线): PING 超时 `sentinel down-after-milliseconds`。**ODOWN**(客观下线): `SENTINEL is-master-down-by-addr`协议→quorum 个 sentinel 确认→故障转移。**故障转移**: 选最优 slave(`slave-priority`+`slave_repl_offset`+runid)→SLAVEOF NO ONE→通知其他 slave→更新配置 |
| R-15 | **Cluster** | cluster.c(1445行) | 3.4 | 16384 slot(CRC16(key) % 16384)。**MOVED**: key 不在本节点→返回新节点地址。**ASK**: slot 迁移中(IMPORTING/MIGRATING)→临时重定向。**Gossip 协议**: 节点间 PING/PONG/MEET/FAIL 消息→`clusterCron()` 10次/秒维护集群拓扑。**故障转移**: `clusterHandleSlaveFailover()`→PFAIL 超时→投票选举→当选→接管 slot |
| R-16 | **事务** | multi.c(482行) | 4.1 | MULTI→命令入队(client.mstate.commands)→EXEC→顺序执行(DISCARD 取消)。WATCH→`watched_keys`(dict key→client list)→EXEC 前检查 key 是否被修改→是则返回 nil(乐观锁)。**ACID**: 原子性(EXEC 中命令全部执行或都不执行), 无隔离性(EXEC 中命令看到中间状态), 持久性取决于 AOF/RDB 配置 |
| R-17 | **客户端缓存(tracking)** | tracking.c(648行) | 4.5 | RESP3 协议新特性——`CLIENT TRACKING on`→服务端记录 client 读取的 key→key 被修改时→`invalidate`消息推送→client 失效本地缓存。**BCAST 模式**: `PREFIX foo:*` 按前缀广播。**OPTIN/OPTOUT**: 精细控制哪些 key 参与 tracking。**redirect**: 失效消息转发到另一 client(减少连接数) |
| R-18 | **内存碎片整理** | defrag.c(1270行) | 4.7 | `activeDefragCycle()`—`serverCron` 中按 `hz` 频率执行。利用 Jemalloc `je_get_defrag_hint()` 获取碎片率→超出阈值(默认>1.1)→遍历 `db->expires` dict→`defragKey()`→`defragDictBucketCallback()`→移动 dict entry→`je_vsnprintf()` 重新分配后合并→分片整理 |

---

## 三、淘汰清单（仅限纯工具/测试/module/脚本）

| 模块 | 理由 |
|:---:|---|
| `redis-cli.c` `redis-benchmark.c` `redis-check-*` | 命令行工具 |
| `debug.c` `memtest.c` | 调试/测试 |
| `module.c` | Redis Module API(C扩展框架, 非内核) |
| `script_lua.c` `eval.c` `function_lua.c` `functions.c` | Lua 脚本引擎 |
| `tls.c` `anet.c` `connection.c` `syncio.c` | 网络传输层 |
| `latency.c` `slowlog.c` `monotonic.c` | 监控/时间 |

---

## 四、统计

| 类别 | 数量 | 对照书节 |
|:---:|:---:|---|
| 🔴 核心域 | **10** | 数据结构(SDS/List/Set/Hash/ZSet/Stream)+事件+持久化+复制 |
| 🟡 扩展域 | **8** | Bitmap/HLL/GEO/Sentinel/Cluster/事务/Tracking/碎片整理 |
| **总域** | **18** | 覆盖全书第 2/3/4 章 100%, 第 5 章为运维实战 |

## 五、关键架构关系

```
事件循环: aeMain → beforesleep(IO线程并行读写+过期/淘汰/过期)
  → processCommand() → call(c) → propagate(AOF+slaves)

数据结构: redisObject{type,encoding,lru,refcount,ptr}
  STRING: ptr→sds(5header)   BITMAP: SETBIT→sds位操作
  LIST: ptr→quicklist(quicklistNode→listpack/ziplist+LZF)
  SET: encoding=INTSET→intset; encoding=HT→dict
  HASH: encoding=LISTPACK→listpack; encoding=HT→dict
  ZSET: ptr→zset{dict+skiplist}   GEO: ZSet存储(geohash=score)
  STREAM: ptr→stream{rax树+cgroups+consumers}

持久化: serverCron(100ms/10Hz) → flushAppendOnlyFile() (AOF fsync)
  → activeExpireCycle()(过期扫描) → replicationCron()(每秒心跳)

高可用: Master←→Slave(PSYNC/全量) → Sentinel(监控+故障转移)
  → Cluster(16384 slot/Gossip/故障转移)
```

## 六、阅读顺序

```
R-1 redisObject → R-4 SDS → R-3 Dict → R-5 quicklist → R-6 skiplist+ZSet
→ R-7 intset → R-10 rax+Stream → R-11 Bitmap → R-12 HLL → R-13 GEO
→ R-2 事件循环+IO多线程 → R-8 持久化 → R-9 复制
→ R-14 Sentinel → R-15 Cluster
→ R-16 事务 → R-17 客户端缓存 → R-18 内存碎片
```
