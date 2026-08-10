# 商品详情页, 你用Redis改了个价格然后刷新 — 为什么页面还显示旧价格?

> Cluster B: 15 KPs | 依赖: 03-distributed-theory-architecture, 域1-一致性模型 | 读者基线: 用过Redis, 知道缓存基本概念

---

### 1. 三层缓存架构 — 你的请求到数据到底经过了几个"缓存层"?
  用户打开商品详情页 — CDN→Nginx本地→应用本地→Redis→MySQL, 5层缓存, 每一层都是一致性风险
  - B3 Ch7 §3: 缓存分层 — CDN(边缘缓存, 用户最近) / Nginx本地缓存(proxy_cache, 热点页面) / 应用本地缓存(Caffeine/Guava, JVM内) / 分布式缓存(Redis/Memcached, 跨JVM共享) / 数据库(最慢最可靠)
  - B4 Ch6 §7.1-7.2: 本地缓存 — ConcurrentHashMap(缺乏过期, 内存无界)/Guava Cache(过期策略+弱引用+LRU), 牺牲一致性换亚微秒延迟 (B4 Ch6 §7.2)
  - 多级缓存同步: 应用本地缓存(Guava)→Redis→MySQL, 更新穿透→先删Redis→等本地缓存过期→最终一致 (B3 Ch7 §5)
  - 关键设计: 为什么不像CPU Cache那样做缓存一致性协议? — 分布式缓存节点太多+网络开销太大, 宁愿接受最终一致也不值得维持全局序

### 2. Redis的独特性 — 为什么不是Memcached?
  Redis不只是个KV Store — 它的数据结构+持久化+集群让它成了微服务基础设施
  - B1 Ch4 §3.4: Redis核心 — 数据结构(String/Hash/List/Set/ZSet/HyperLogLog/Bitmap/GEO) + 持久化(RDB全量快照+AOF增量命令, RDB-AOF混合) + 集群(主从→Sentinel→Cluster, Gossip协议)
  - Sentinel故障转移: 主观下线(SDOWN, 单Sentinel判断)→客观下线(ODOWN, 多数Sentinel确认)→选主(根据slave-priority+复制偏移量)→failover (B4 Ch3 §7.4)
  - Codis代理方案: Proxy层(ZK做元数据路由)+Group(每个Group=一主一从)+Dashboard, 1024个slot → hash(key)%1024定位Group (B4 Ch3 §7.5)
  - 关键设计: Redis为什么快? — 单线程(避免锁)+内存操作+epoll多路复用+数据结构优化(ziplist/intset小数据用小结构)

### 3. 缓存更新 — Cache Aside/Read-Through/Write-Behind哪个更适合你的场景?
  用户更新商品价格→更新MySQL→删Redis→下一个请求读MySQL→写Redis — 这是最常见的模式, 有什么问题?
  - B3 Ch8 §3: 三大模式 — Cache Aside(应用控制, 读缓存Miss→读DB→写缓存, 写DB→删缓存), Read/Write Through(缓存层对应用透明, 缓存自己负责读写DB), Write Behind(先写缓存→异步批量写DB, 性能最高但一致性最差)
  - Cache Aside陷阱: 先删缓存再写DB→并发读到旧数据写缓存→脏数据 → 延时双删(写DB→等N ms→再删一次缓存) 或 先写DB再删缓存(并发可能短暂脏, 但最终对) (B3 Ch9 §3)
  - 延时双删的"延时"怎么定? — 统计从写DB到读请求可能写缓存的最长时间, 取p99 + buffer, 典型值200-500ms (B3 Ch9 §3.2)
  - 关键设计: Cache Aside模式的本质=应用层承担一致性责任 — 缓存层不做保证, 应用开发者决定何时操作缓存

### 4. 缓存一致性 — binlog异步方案为什么比延时双删更可靠?
  延时双删的假设(延时够长)在生产环境常会被长事务/慢请求打破
  - B3 Ch9 §4: binlog异步方案 — Canal(阿里开源)伪装MySQL Slave→接收binlog→解析→发MQ→消费者删除对应缓存 → 基于数据库权威数据源(master), 不依赖估算延时
  - 流程详解: INSERT/UPDATE/DELETE→MySQL写binlog→Canal解析ROW→发现order表变更→MQ(order_cache_invalidate topic)→消费者执行DEL order:123→缓存失效 (B3 Ch9 §4.2-4.4)
  - B3 Ch9 §5: 自动过期+失败补偿 — 即使binlog方案也可能失败(Canal宕机/MQ丢失) → Redis TTL兜底(1h后自动过期) + 定时对账(对比DB和Redis数据)
  - 关键设计: 缓存一致性最高保证=最终一致+TTL兜底+对账修复 — 三层防御, 承认"100%强一致在缓存层不可能"

### 5. 缓存三剑客 — 雪崩/穿透/热点, 各怎么防?
  你Redis容量是100GB, 缓存了200万商品 — 一个热点商品1秒被刷100万次
  - B3 Ch8 §4: 缓存雪崩 — 大量缓存同时过期→全部打DB → 过期时间加随机(0~N分钟)→多级缓存(Guava+Redis, Guava TTL短+本地内存)
  - B3 Ch8 §5: 缓存穿透 — 大量查询不存在的数据(如"id=-1")→每次穿透→打DB → 缓存空值(TTL短,5min)+布隆过滤器(可能存在→DB, 一定不存在→直接返回, 省DB查询) [案例: Google Bigtable用布隆过滤器检查RowKey是否存在, 减少不必要的磁盘SSTable读取]
  - B3 Ch8 §6: 热点数据 — 多个Key到同一个Redis节点→单节点被打爆 → 前置缓存(应用本地Guava 1min TTL, 挡住99%请求)+热点散列(key_1, key_2, ..., key_N副本分散到不同节点) (B3 Ch8 §6)
  - 关键设计: 缓存三剑客的防御层次 — 穿透(布隆过滤器)→热点(本地缓存+散列)→雪崩(TTL随机+多级), 不是一把梭

### 6. 收束 — 回到商品详情页的价格更新
  - 你改MySQL→删Redis→Canal异步再删→TTL过期兜底, 最终一致的时间窗口=p99延迟+异步补偿延迟+TTL
  - 5层缓存×多级一致性=根本没有全局强一致 — 接受这个现实, 用TTL+对账+最终一致打底
  - 缓存不是性能优化, 是架构决策 — 引入缓存就意味着接受最终一致性

---

### 核心悬念
**"缓存帮你扛住了100万QPS读, 但秒杀时的下单请求(BURST写)怎么处理 — 数据库每秒只能写5000条?"**

→ 引出 消息驱动: MQ的削峰填谷+异步解耦+事件驱动架构 (05-message-driven)
