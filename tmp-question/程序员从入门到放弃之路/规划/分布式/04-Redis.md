# 04 — Redis — 知识点规划

> 方法论: knowledge-planning/methodology/v5 | 教学叙事5+1要素 | 跨层标注(`[理论:/工程:/案例:]`)
> N=1本 | N≥20 KPs→一域一卷

---

## 贡献书籍

| # | 书名 | 角色 | 特点 |
|---|------|------|------|
| B1 | Redis高手心法 | 🔴唯一源 | 5章: 入门→核心数据结构→高可用→高级技能→出师实战 |

---

## 01 提取 — 逐书映射

### `[01 #1/1]` B1 — Redis高手心法

| Original Chapter | Inferred Knowledge Point | Confidence |
|-----------------|------------------------|------------|
| Ch1 §1.1 | Redis能做什么(缓存/消息队列/排行榜/计数器/分布式锁/会话管理)/源码编译/目录结构 | 🔴核心 |
| Ch1 §1.2 | 整体架构: 数据存储原理(全局dict→redisObject→编码)/一条命令执行全过程(networking→解析→查找→执行→响应) | 🔴核心 |
| Ch2 §2.1 | SDS实现: len+free+柔性数组/空间预分配/惰性释放/二进制安全/分布式ID生成器实战 | 🔴核心 |
| Ch2 §2.2 | Lists演进: linkedlist→ziplist→quicklist→listpack/消息队列实战 | 🔴核心 |
| Ch2 §2.3 | Sets实现: intset编码升级(int16→int32→int64)/共同好友实战 | 🔴核心 |
| Ch2 §2.4 | Hash: dict结构(渐进式rehash迭代器)/listpack编码/购物车实战 | 🔴核心 |
| Ch2 §2.5 | Sorted Set: skiplist(多层索引/O(logN))+dict(成员→分数O(1))/listpack/游戏排行榜实战 | 🔴核心 |
| Ch2 §2.6 | Stream: Radix Tree存储/消费者组/ACK/消息ID格式/轻量级MQ实战 | 🔴核心 |
| Ch2 §2.7 | Geo: GeoHash编码(2D→1D)+ZSET存储+GEORADIUS实现/附近的人 | 🔴核心 |
| Ch2 §2.8 | Bitmap: SDS位数组+SETBIT/GETBIT/BITCOUNT/亿级用户签到实战 | 🔴核心 |
| Ch2 §2.9 | HyperLogLog: 稀疏矩阵→稠密矩阵/调和平均数/标准误0.81%/海量UV统计实战 | 🔴核心 |
| Ch2 §2.10 | Bloom Filter: 位数组+多哈希函数/误判率计算/缓存穿透预防实战 | 🔴核心 |
| Ch2 §2.11 | Redis高性能原因: 纯内存操作/IO多路复用(epoll)/单线程避免锁竞争/高效数据结构/全局散列表O(1) | 🔴核心 |
| Ch3 §3.1 | RDB快照: SAVE/BGSAVE/COW(fork子进程共享页表)/写时复制触发→大量写入时内存翻倍风险 | 🔴核心 |
| Ch3 §3.1 | AOF持久化: 命令追加→AOF缓冲区→always/everysec/no三种fsync策略→AOF重写(分析进程内存直接生成最小命令集) | 🔴核心 |
| Ch3 §3.2 | 主从复制: 全量同步(RDB+replication buffer)+增量同步(复制积压缓冲区/repl_backlog)+replication ID | 🔴核心 |
| Ch3 §3.3 | 哨兵集群: 主观下线(PING超时)→客观下线(quorum投票)→Leader选举(Raft简化)→故障转移(选新主/通知从/更新客户端) | 🔴核心 |
| Ch3 §3.4 | Redis集群: 16384 hash slots/一致性哈希替代/gossip协议(Meet/Ping/Pong/Fail)/MOVED重定向/ASK重定向(迁移中)/cluster bus | 🔴核心 |
| Ch4 §4.1 | Redis事务: MULTI→EXEC(原子执行)/WATCH(乐观锁, CAS)/ACID分析(原子性=入队错误vs执行错误不同行为/无隔离性/耐久性取决于fsync策略) | 🔴核心 |
| Ch4 §4.2 | 内存淘汰策略: noeviction/allkeys-lru/volatile-lru/allkeys-lfu/volatile-lfu/allkeys-random/volatile-random/volatile-ttl — 8种/LRU近似算法(24bit时钟)/LFU对数计数器 | 🔴核心 |
| Ch4 §4.2 | 过期删除策略: 惰性删除(访问时检查)+定期删除(serverCron循环随机抽样)/主从复制中从节点等待主节点del命令 | 🔴核心 |
| Ch4 §4.3 | 事件驱动: Reactor模式→文件事件(aeFileEvent/readable/writable/accept)→时间事件(aeTimeEvent/serverCron每100ms)→aeProcessEvents循环 | 🔴核心 |
| Ch4 §4.4 | 发布订阅: PubSub(频道SUBSCRIBE/PUBLISH/PSUBSCRIBE模式匹配)/pubsub_channels dict+pubsub_patterns list→不适合消息可靠性(无ack/无持久化/断开消息丢失) | 🔴核心 |
| Ch4 §4.5 | 客户端缓存: tracking模式(客户端开启→修改invalidate消息)/广播模式(BCAST前缀过滤)/重定向模式(REDIRECT转发到其他客户端) | 🔴核心 |
| Ch4 §4.6 | IO多线程: 从单线程→IO多线程演进(读/写用线程池处理网络IO→命令执行仍单线程)/io-threads-do-reads配置→TLS场景收益大 | 🔴核心 |
| Ch4 §4.7 | 内存碎片: 成因(jemalloc分配固定size classes→大量释放不归还OS)→activedefrag自动碎片整理(lazy freeing/碎片阈值)→重启替代方案 | 🔴核心 |
| Ch5 §5.1 | 性能排查: 基线测量(redis-benchmark 100万QPS参考)→慢命令监控(slowlog-log-slower-than)→latency monitor→大key扫描(--bigkeys) | 🔴核心 |
| Ch5 §5.2 | 使用规范: key命名规范/禁用keys */慎用hgetall→hscan/禁用大value/pipeline批量/连接池(连接数=客户端数×连接数) | 🔴核心 |
| Ch5 §5.3 | 内存优化: 小数据集编码(ZIPLIST/LISTPACK阈值)/对象共享池(0-9999整数)/bit操作替代多个bool字段/Hash小对象优化/32位Redis减指针 | 🔴核心 |
| Ch5 §5.4 | 生产配置: RDB(save)+AOF(appendfsync)+maxmemory+cluster-require-full-coverage+slowlog+lazyfree-lazy-eviction/expire+CPU绑定(taskset/protected-mode) | 🔴核心 |
| Ch5 §5.5 | 缓存穿透: 布隆过滤器(判断一定不存在)/缓存空值(短TTL)/IP限流 + 缓存击穿: 互斥锁(SET NX防重复加载)+逻辑过期(异步刷新)+SingleFlight合并 | 🔴核心 |
| Ch5 §5.5 | 缓存雪崩: 随机TTL(expire+random(0,300))/多级缓存Caffeine降级/流量控制(限流熔断) | 🔴核心 |
| Ch5 §5.6 | 缓存一致性: Cache-Aside(先写DB后删缓存)/延迟双删/基于Canal Binlog异步更新/写穿(Write-Through)/写回(Write-Behind) | 🔴核心 |
| Ch5 §5.7 | 分布式锁: SET NX PX(Lua脚本原子释放)/可重入锁(Redisson/Hash value为计数器)/RedLock(多实例多数同意→争议: Martin Kleppmann时钟跳跃/GC暂停破坏互斥) | 🔴核心 |

---

## 02 聚合 — 逐Cluster聚合

### Cluster A: 核心数据结构 (3篇 ~24 KPs)

| KP | 源 | 归属 |
|----|----|------|
| Redis整体架构: 全局dict→redisObject→编码→存储原理 | B1 Ch1 §1.2 | 01 |
| 一条命令执行过程: networking→解析→查找→执行→响应 | B1 Ch1 §1.2 | 01 |
| 单线程高性能原因: 纯内存/IO多路复用(epoll)/单线程避锁/高效数据结构/全局散列表 | B1 Ch2 §2.11 | 01 |
| IO多线程模型演进: 单线程→IO多线程(读写线程池/命令执行仍单线程) | B1 Ch4 §4.6 | 01 |
| SDS实现: len+free+柔性数组/空间预分配/惰性释放/二进制安全/分布式ID生成器 | B1 Ch2 §2.1 | 02 |
| Lists演进: linkedlist→ziplist→quicklist→listpack/消息队列实战 | B1 Ch2 §2.2 | 02 |
| Sets实现: intset编码升级(int16→int32→int64)/共同好友 | B1 Ch2 §2.3 | 02 |
| Hash: dict(渐进式rehash/迭代器)+listpack/购物车 | B1 Ch2 §2.4 | 02 |
| Sorted Set: skiplist(多层索引O(logN))+dict(O(1)查分)+listpack/排行榜 | B1 Ch2 §2.5 | 02 |
| Stream: Radix Tree/消费者组/ACK/消息ID格式/轻量级MQ | B1 Ch2 §2.6 | 03 |
| Geo: GeoHash编码(2D→1D)+ZSET+GEORADIUS/附近的人 | B1 Ch2 §2.7 | 03 |
| Bitmap: SDS位数组+SETBIT/GETBIT/BITCOUNT/亿级签到 | B1 Ch2 §2.8 | 03 |
| HyperLogLog: 稀疏→稠密矩阵/调和平均/0.81%误差/UV统计 | B1 Ch2 §2.9 | 03 |
| Bloom Filter: 位数组+哈希函数/误判率/k值计算/缓存穿透预防 | B1 Ch2 §2.10 | 03 |

### Cluster B: 高可用与持久化 (2篇 ~14 KPs)

| KP | 源 | 归属 |
|----|----|------|
| RDB快照: SAVE/BGSAVE/COW(fork页表共享+写时复制) | B1 Ch3 §3.1 | 04 |
| AOF持久化: 命令追加→缓冲区→fsync策略(always/everysec/no)→AOF重写 | B1 Ch3 §3.1 | 04 |
| RDB+AOF混合持久化: 全量快照+增量AOF=恢复快+数据少丢 | B1 Ch3 §3.1 | 04 |
| 主从复制: 全量同步(RDB+replication buffer)+增量同步(repl_backlog)+replication ID | B1 Ch3 §3.2 | 04 |
| 哨兵集群: 主观下线(PING超时)→客观下线(quorum投票)→Leader选举(Raft简化)→故障转移 | B1 Ch3 §3.3 | 05 |
| Redis集群: 16384槽/一致性哈希/gossip(Meet/Ping/Pong/Fail)/MOVED+ASK重定向 | B1 Ch3 §3.4 | 05 |
| 集群扩容缩容: 槽迁移(MIGRATE命令)/数据迁移中读写(MOVED→ASK) | B1 Ch3 §3.4 | 05 |

### Cluster C: 高级特性与生产实战 (4篇 ~27 KPs)

| KP | 源 | 归属 |
|----|----|------|
| 内存淘汰策略: 8种(LRU/LFU/random/ttl/noeviction)/LRU近似(24bit时钟)/LFU对数计数器 | B1 Ch4 §4.2 | 06 |
| 过期删除策略: 惰性删除+定期删除(serverCron)/从节点等主del命令 | B1 Ch4 §4.2 | 06 |
| 事件驱动: 文件事件+时间事件(serverCron)/aeProcessEvents主循环 | B1 Ch4 §4.3 | 06 |
| 内存碎片: jemalloc Size Classes成因/activedefrag自动整理/阈值配置 | B1 Ch4 §4.7 | 06 |
| Redis事务: MULTI/EXEC/WATCH(乐观锁)/入队错误回滚? 执行错误继续! /ACID分析 | B1 Ch4 §4.1 | 07 |
| 发布订阅: 频道+模式匹配/persistence=无/ACK=无/断连=丢 → vs Stream对比 | B1 Ch4 §4.4 | 07 |
| 客户端缓存: tracking模式(invalidate)/广播BCAST/重定向REDIRECT | B1 Ch4 §4.5 | 07 |
| 使用规范: key命名/禁用keys */pipeline批量/大value/连接池 | B1 Ch5 §5.2 | 07 |
| 缓存穿透/击穿/雪崩: 布隆过滤器+互斥锁+随机TTL+多级降级 | B1 Ch5 §5.5 | 08 |
| 缓存一致性方案: Cache-Aside/延迟双删/Canal Binlog异步/Write-Through/Write-Behind | B1 Ch5 §5.6 | 08 |
| Redis分布式锁: SET NX PX+Lua释放/可重入(Redisson)/Redlock+争议 | B1 Ch5 §5.7 | 09 |
| 生产配置: RDB+AOF参数/maxmemory/cluster/slowlog/lazyfree/CPU绑定 | B1 Ch5 §5.4 | 09 |
| 性能排查: redis-benchmark/slowlog/latency monitor/大key扫描(--bigkeys) | B1 Ch5 §5.1 | 09 |
| 内存优化: 小数据集编码/对象共享池(0-9999)/bit操作/Hash小对象/32位Redis | B1 Ch5 §5.3 | 09 |

---

## 03 深度分类 — 按教学叙事组织

### 叙事线: 数据结构→高可用→高级特性→生产实战

| 教学阶段 | 核心问题 | 对应篇文章 | 关键叙事 |
|---------|---------|-----------|---------|
| 阶段1: 认识Redis | Redis为什么这么快? 命令怎么执行的? | 01 | 从一组SET GET→深入内核→理解架构 |
| 阶段2: 数据结构 | String/List/Set/Hash/ZSet底层是什么? | 02 | 5种基础结构→每种底层编码→为什么这样设计 |
| 阶段3: 数据结构进阶 | Stream/Geo/Bitmap/布隆 怎么用的? | 03 | 高级结构→生产场景→每种解决什么难题 |
| 阶段4: 高可用 | 你的数据丢了怎么办? 服务器挂了怎么办? | 04-05 | RDB+AOF→主从→哨兵→集群→容错金字塔 |
| 阶段5: 高级特性 | 事务真的能用吗? PubSub靠谱吗? 内存怎么管? | 06-07 | 内存管理→事件驱动→事务→PubSub→规范 |
| 阶段6: 生产实战 | 线上Redis踩过哪些坑? 怎么用稳它? | 08-09 | 缓存三剑客→一致性→分布式锁→配置→性能 |

---

## 04 方案选择 — 3 Cluster / 9篇

### Cluster A: 核心数据结构 (3篇)
1. **01-redis-architecture-thread-model** — 架构+命令+单线程高性能+IO多线程演进
2. **02-core-data-structures-1** — 5种基础数据结构: String/List/Set/Hash/SortedSet
3. **03-advanced-data-structures-2** — 高级数据结构: Stream/Geo/Bitmap/HyperLogLog/Bloom

### Cluster B: 高可用与持久化 (2篇)
4. **04-persistence-replication** — RDB/AOF+/AOF半持久化+主从复制
5. **05-sentinel-cluster** — 哨兵集群+Redis集群(16384槽/gossip/重定向)

### Cluster C: 高级特性与生产实战 (4篇)
6. **06-memory-management-events** — 内存淘汰(8种)+过期删除+事件驱动+内存碎片
7. **07-transaction-pubsub-cache** — 事务+发布订阅+客户端缓存+使用规范
8. **08-cache-penetration-consistency** — 缓存穿透/击穿/雪崩+缓存一致性方案
9. **09-distributed-lock-production** — 分布式锁+生产配置+性能排查+内存优化+域5桥

---

## 05 域间桥

| 前向引用(域4→前面域) | 桥接点 |
|---------------------|--------|
| 域1-01(CAP) | 哨兵/集群一致性 = CP, 主从复制 = AP → Raft简化满足P |
| 域2-04(分布式缓存架构) | 域2已讲Redis部署架构→域4深入原理 |
| 域3-05(缓存优化) | 域3从应用层讲缓存策略→域4从Redis角度讲实现原理 |

| 后向引用(域4→域5/6) | 桥接点 |
|---------------------|--------|
| 域4-09(分布式锁) → 域5(Dubbo) | Redis解决了数据和一致性问题 → 服务间RPC调用? |
| Stream(轻量MQ) → 域6(RocketMQ) | Redis Stream vs 专业MQ(可靠性/持久化/顺序)对比 |

---

## 06 教学叙事要素覆盖

| 要素 | 覆盖 |
|------|------|
| 场景句 | 每节以生产异常/面试场景开场 |
| 书源路径 | B1 ChX §X.X 全部映射 |
| 关键设计 | 每条含"为什么这样设计" |
| 跨层标注 | [理论:][工程:][案例:] ≥1/篇 |
| 收束+悬念+引出 | 01-07引下一篇, 08引09, 09引域5 |
