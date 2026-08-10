# 全公司都在用Redis做分布式锁 — 结果SET NX PX忘了设过期, 永锁了

> Cluster C: 4 KPs | 依赖: 域4-05(集群/哨兵/故障转移), 域1-01(CAP一致性), 域3(并发编程) | 读者基线: 在代码里写过Redis分布式锁, 听说过Redlock, 不理解争议

---

### 1. 分布式锁的五层进化 — SET NX PX → Redlock, 每一层解决的是一层问题
  你写了4行Redis锁代码: `SET lock:order NX PX 10000 + 执行业务 + Lua删除` — 但锁超时了, 锁已过期, 你还在执行业务。
  - 第一层(原始): SET NX + EXPIRE — 两个命令不原子, 可能在SET后宕机(死锁) → 已经不能用 (B1 Ch5 §5.7)
  - 第二层(单值): SET lock:order unique_value NX PX 10000 — 原子锁, 但有: 锁过期后不能续期, 业务超时锁被其他人获取 (B1 Ch5 §5.7)
  - 第三层(自动续期): Redisson看门狗(watch dog) — 默认30秒锁, 每10秒自动续期 → SET NX PX不是一次性 — 应用还在就一直在 (B1 Ch5 §5.7)
  - 第四层(可重入): 同一线程多次获取同一锁不阻塞 — Hash存储, key=锁, field=线程, value=count — 防自己的操作block自己 (B1 Ch5 §5.7)
  - 第五层(多Redis): Redlock — CP模型 — N个独立Redis(Master不是slave) — 客户端必须在多数达成+有效期内完成锁 — 少数Redis宕不影响互斥 [案例: 5个Redis实例→1个宕→在4个里获得3个锁, 10秒过期→另一个请求只能在另一个实例拿锁→最多1个拿到多数→互斥保证了]

### 2. Redlock争议 — Martin Kleppmann说"不安全", Antirez答"你不懂"
  你面试说Redlock是分布式锁最佳方案 — 面试官掏出Martin的博客:"这个锁可以被GC暂停破坏!"
  - Martin的时钟跳跃攻击: 锁在某Redis过期(因NTP时钟跳跃提前过期)→其他客户端以为拿到了给其他人的锁→多客户端同时获取—破坏互斥 (B1 Ch5 §5.7) [理论: Martin争辩: Redlock基于时序 — 假设Redis时钟单调+进程不暂停 — 分布式系统里这两个假设不可靠 — GC暂停数十秒、时钟跳跃、网络分区都能破坏互斥]
  - Antirez的防守: 时钟跳跃可以通过`SWAP`方式(不同实例分步检测, 边界检查)+fencing token(单调递增锁token, 存储系统拒绝老token的写) (B1 Ch5 §5.7) [工程: fencing token = 写数据库时带上lock token, 数据库拒绝token小的写 — 锁可能超时但数据库层面保证了互斥]
  - 结论: "Fencing token让Redlock安全"是共识 — 单独Redlock在不加fencing的存储面前是有风险的
  - 关键设计: 如果你不需要完美互斥(缓存刷新/定时任务单点执行), 单Redis锁够用 — 如果需要强互斥(对账/金额写), 用DB行锁或ZooKeeper顺序临时节点+watch — 不是所有场景都需要Redlock

### 3. 生产配置与性能排查 — 不是改个配置就行, 你还要知道怎么看
  你改完maxmemory-policy, 线上还是报OOM — 你以为是配置没生效, 实际上是别的key占内存。
  - 王者必配: maxmemory(上限)+maxmemory-policy(淘汰)+cluster-require-full-coverage(集群完整性)+slowlog(慢命令)+lazyfree-lazy-eviction/expire(惰性释放) (B1 Ch5 §5.4)
  - 基线测量: redis-benchmark -t set,get -c 50 -n 1000000 — 建立这台机器的QPS上限 — 线上监控<50%上限时正常, >80%要扩容 (B1 Ch5 §5.1)
  - slowlog: slowlog-log-slower-than 10000(10ms)→记录执行>10ms的命令→slowlog get 100分析 — KEYS */SORT等慢命令出现马上重构 [案例: slowlog显示HGETALL user:session:* 300ms→session大object→切hash分表→50ms每个, 并行批处理1ms总]
  - 排查工具: MEMORY USAGE key(单key内存)/--bigkeys(找大key)/latency doctor(检测延迟源)/INFO commandstats(命令频率统计) (B1 Ch5 §5.1)

### 4. 内存优化 — 不是买更多机器, 是把浪费的找回来
  你存储了用户信息, repeated字段存在所有key里 — 公共前缀不要存在value里。
  - 小数据集编码: hash-max-ziplist-entries 512→小于512 field的Hash用listpack不是dict — 小对象存为紧凑编码, 内存省70% (B1 Ch5 §5.3)
  - 对象共享池: 0-9999的整数Redis预分配→INCR一个=0-9999的整数时用已分配对象 — 不每次malloc (B1 Ch5 §5.3)
  - bit操作替代多个bool: 用户配置10个bool 开关→用BIT位表示=16字节→不用10个单独的key (B1 Ch5 §5.3) [工程: 内存节省实例 — 1000万个用户+每个10个bool key=1000万×10 bytes每个key=~100MB → bit存储=每个用户2字节+1个key overhead=~4MB — 节省25倍]
  - 关键设计: Redis的CPU是够用的(N秒处理N万请求) — 瓶颈在内存 — 内存优化把空间变成有价值的, 不是浪费在key的overhead上

### 5. 收束 — Redis不是"一招套一场景", 是"每种配置选对场景"
  - 锁: 不同场景用不同层次的锁 — 定时任务(单Redis, 自动续期), 金额对账(DB锁或ZK), 高频缓存同步(SET NX PX够用)
  - 配置: 不是把maxmemory加高就省心 — 淘汰策略不对, 加再多内存也撑不过
  - 排查: slowlog+-bigkeys+Frequent INFO → 数据驱动优化, 不是"猜着改"
  - 域4的全景: 结构(存得巧)→高可用(不挂)→内存(不撑)→特性(善用)→实战(稳跑) — 五层金字塔的顶峰是"用得好"

---

### 核心悬念
**"Redis把数据和一致性问题都解决了 — 但分布式系统不只是数据: 你A服务想要调B服务的某个方法, B在哪个机器上? 网络不通怎么办? 怎么序列化参数和返回值?"**

→ 引出 域5 Dubbo: RPC通信原理 + 服务注册发现 + 负载均衡 + 服务治理 — 从"数据层分布式"进到"服务层分布式" (域5-01)
