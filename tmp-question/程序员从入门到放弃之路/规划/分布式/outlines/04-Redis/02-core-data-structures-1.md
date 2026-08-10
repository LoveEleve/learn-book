# 你用ZSET做游戏排行榜 — 但Redis不是排序更不是随机, Skiplist凭什么更快?

> Cluster A: 5 KPs | 依赖: 域4-01(架构/线程模型) | 读者基线: 理解Redis五种基本数据结构, 好奇底层实现

---

### 1. String不是String — SDS的巧妙不是你一眼能看出来的
  你`SET counter 100`, Redis存的是字符串"100"不是整数 — 但SDS做了很多C字符串没做的事。
  - SDS结构: len(已用长度)+alloc(分配长度)+flags(类型)+buf(字节数组) — 不是C的char*, O(1)取长度, 二进制安全(不用\0, buf可以存任意字节) (B1 Ch2 §2.1)
  - 空间预分配: len<1MB→分配len+len+1(翻倍), len≥1MB→分配len+1MB+1(多1MB) — 减少频繁内存分配, 以空间换时间 (B1 Ch2 §2.1)
  - 惰性释放: trim/setrange缩短字符串不立即缩内存 — 用free记录可用空间 — 避免了反复malloc/free [理论: malloc/free的代价 — jemalloc的size classes能回收大于8KB的, 小于8KB的留在进程里]
  - 关键设计: SDS知道自己的长度 — strlen()是O(1)不是O(N) — 一个API调用节省了千倍运算

### 2. List不是LinkedList — ziplist/quicklist/listpack 三代言
  你`LPUSH queue message`, 链表头插入O(1) — 但Redis底层不是简单链表。
  - ziplist(一代): 一块连续内存, 每个entry=前一个长度(1或5字节)+编码+数据 — 省内存(无指针开销), 但插入删除可能触发"连锁更新"(所有entry都要更新prevlen) (B1 Ch2 §2.2)
  - quicklist(二代): ziplist切片+linkedlist — 每个node是一个ziplist, node之间用链表连接 — 兼顾内存紧凑和更新效率 (B1 Ch2 §2.2)
  - listpack(三代): ziplist替代 — 不再存prevlen, 它记录当前entry的总字节数 — 消除连锁更新, 从根源上解决问题 [工程: 升级路径: linkedlist→ziplist(内存问题)→quicklist(折中)→listpack(根本解决), 数据结构的4次反省]
  - 关键设计: ziplist→listpack的演进是一个教训 — 省内存的设计必须同时考虑修改成本, 微优化可能导致连锁性成本

### 3. Set/hash背后的intset和dict — 字面上简单, 实现上精妙
  Set和Hash用起来简单 — `SADD users 1 2 3` 和 `HSET cart apple 5` — 但底层各自有选择。
  - intset: 整数集合 — encoding(int16/int32/int64) + length + contents数组 — 所有整数连续存储, 无指针, 二分查找O(logN) — 升级不可逆(int16→int32不再回退) (B1 Ch2 §2.3)
  - hash dict: 两个哈希表ht[0]+ht[1] — 默认用ht[0], 扩容/收缩时触发rehash — 渐进式rehash: 每次操作搬1~N个bucket到ht[1], bucket数决定完成时间 — 负载因子>1就expand, <0.1就shrink (B1 Ch2 §2.4) [理论: 渐进式rehash = 把O(N)停服务分摊到O(1/N)每操作, 使主线程始终可用]
  - dict的哈希函数: siphash — 不像普通dic的幂等哈希(siphash有随机种子)→防HashDos攻击(相同键值的碰撞攻击) — 每个进程的哈希值不同 (B1 Ch2 §2.4)
  - 关键设计: intset为什么编码升级不可逆? — 升级简单(C copy+重写), 降级麻烦——而且元素一旦多就不会再"少回来"

### 4. Sorted Set — 跳表+字典, 两者一起干一件事
  `ZADD leaderboard 1000 playerA` — 按分数排序, 按成员查分数, 两个方向都要快。
  - skiplist: 多层索引 — 层数是随机产生的(概率1/4生成下一层) — lookUp/insert/delete全O(logN) — 不像红黑树需要旋转, 实现简单 (B1 Ch2 §2.5)
  - 为什么skiplist不单独用: 按name查分数需要O(logN)遍历 — 太慢 — 所以再加一个dict做O(1)成员→分数映射 (B1 Ch2 §2.5) [工程: skiplist+dict = 空间换时间 — 双份内存换ZSCORE O(1)]
  - skiplist的随机层数: 概率1/4 → 期望层数=1/(1-0.25)=1.33层 → 内存≈1.33个指针/节点 — 红黑树=3-4个指针(left/right/parent+color位) — 期望1.33个指针的skiplist内存更省 (B1 Ch2 §2.5)
  - 关键设计: Sorted Set引入listpack编码 — 元素少时用listpack紧凑存储+二分查找 — 元素多了切换到skiplist+dict — 用小对象编码偷性能

### 5. 收束 — 每种数据结构都是"场景选择", 不是"教科书选择"
  - SDS不用C字符串(要长度), ziplist不用链表(要内存), skiplist不用红黑树(要简单) — 每个选择都有理由
  - 规模自适应编码: intset/ziplist/listpack → 小对象紧凑, 大对象升级 — 不是写死的, 是根据数据量长出来的
  - 同一个Hash: 5个field用listpack(紧凑, 10ms读写), 5000个field用dict/链表(散列, 50ms读写) — 编码升级阈值在hash-max-ziplist-entries=512

---

### 核心悬念
**"5种基本结构都讲完了 — 但Stream的消费组和Ack怎么实现? GeoHash怎么把二维坐标变成一维数?"**

→ 引出 高级数据结构: Stream/Geo/Bitmap/HyperLogLog/BloomFilter — 每个解决一个特定难题 (03-advanced-data-structures-2)
