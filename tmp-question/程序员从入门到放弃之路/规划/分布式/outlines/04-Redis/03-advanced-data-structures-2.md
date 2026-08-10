# 你不用Kafka — 用Redis Stream也能做消息队列, 而且还能精确消费

> Cluster A: 5 KPs | 依赖: 域4-02(基础数据结构) | 读者基线: 了解MQ, 听说过Kafka/RocketMQ, 用过Redis基本类型

---

### 1. Stream — Redis里的"小型Kafka", 有消息ID有消费组有ACK
  你需要在缓存更新的同时异步通知下游 — 不想引入Kafka太重, Stream能否顶上?
  - Stream结构: Radix Tree(压缩前缀树)+listpack — 消息按ID排序存listpack, 索引用RadixTree快速定位 — 每条消息有唯一递增ID(时间戳-序列号格式: 1621345678000-0) (B1 Ch2 §2.6)
  - 消费者组: XGROUP CREATE→多个消费者共享一组 — 组内每个消费者读不同消息(负载均衡)→XACK确认消费→XPENDING查看未确认(重回队列) [工程: 消费者组 = Kafka的分区组内消费模型 — 但Stream没有分区, 用消息ID范围分配]
  - 几个关键限制: 消息不会被删(到期后XDEL或MAXLEN截断→listpack截断非逐条删)/无DLQ死信队列/无顺序重排 — 不能替代专业MQ (B1 Ch2 §2.6)
  - 关键设计: Stream不是"Redis做了MQ" — 是"轻量场景(日志/通知/数据同步)不需要Kafka的完整功能时, 一组命令就能搞定"

### 2. GeoHash — 把二维的地球坐标变成一维, 再放进Sorted Set
  你打开点评app搜"附近餐厅" — 距离计算、排序、过滤都用ZSET做到了。
  - GeoHash编码原理: 经纬度→二分区间→二进制→Base32字符串 — 字符串越长越精确(6位=±0.6km, 7位=±38m) — 前缀相同=距离近 (B1 Ch2 §2.7) [理论: GeoHash = Z-order curve — 将2D空间递归分成4个象限, 再用二进制编码, 相邻坐标编码相近]
  - GEOADD实现: 坐标→GeoHash→用ZADD存(GEOHASH值做score, 成员名做member) — GEORADIUS: ZRANGEBYSCORE+ Haversine公式精确距离计算 (B1 Ch2 §2.7)
  - GEORADIUS边界处理: GeoHash边界两侧很近但hash前缀不同→搜索时扩大周围8个格子的范围 — 9格覆盖, 精度可靠 (B1 Ch2 §2.7)
  - 关键设计: Geo不自己造数据结构, 用的是ZSET的能力 — 重用在Redis里就是"换一套命令名+同样的引擎" — 代码量小, 但能力强

### 3. Bitmap和HyperLogLog — 一个精确、一个估算, 都省内存
  你喜欢做用户签到系统 — 用户ID从0到1亿 — HashMap行吗? Bitmap: 10MB。HyperLogLog: 页面的UV — 去重器=HashMap? 12KB。
  - Bitmap: 字符串是byte数组=SDS的buf → 按bit操作(SETBIT/GETBIT/BITCOUNT) — 1亿用户=12.5MB → 签到10亿次也是12.5MB (B1 Ch2 §2.8) [案例: 签到系统 — `SETBIT sign:20260809 uid 1` → BITCOUNT算出日活 → BITOP AND计算连续签到]
  - Bitmap的BITCOUNT: SWAR算法 — 按字节组加速, 不是逐位数 — 12.5MB数十秒→BITCOUNT几十毫秒 (B1 Ch2 §2.8)
  - HyperLogLog: 基于位数组+哈希 — PFADD→用6bit寄存器存"最长前导0的位数"→调和平均→标准误0.81% — 1亿UV只要12KB (B1 Ch2 §2.9) [理论: HyperLogLog的数学直觉 — 抛硬币先出现几个正面是最直接的随机量, 页面的哈希值的0位数是统一分布, 最长0位数的期望是log2(N)]
  - 稀疏→稠密升级: 元素少用稀疏寄存器(zeros count=大部分0)→元素多自动升级稠密 — 类似intset升级但自动 (B1 Ch2 §2.9)
  - 关键设计: Bitmap是精确(每个bit=一个id)而HyperLogLog是估算 — 不同的容忍度, 不同的内存消耗 — 把"精度换空间"做透了

### 4. Bloom Filter — "没有"的确定性, "有"的可能性
  查询用户是否买过商品 — 如果不存在就不用去数据库 — 布隆过滤器说"不存在"那就不用再找了。
  - 布隆过滤器结构: M位位数组+K个哈希函数 — ADD: 对键使用K个哈希→K个位置=1 → EXISTS: K个位置全1? → "可能存在"(误判) / 有一个0 → "一定不存在"(确定) (B1 Ch2 §2.10)
  - 误判率: f = (1 - e^(-kn/m))^k — m越大误判越低, k=(m/n)*ln2时最优 — 用RedisBloom模块: BF.RESERVE 1亿数据 0.001误判 (B1 Ch2 §2.10) [理论: 布隆过滤器不存数据本身只存"存在痕迹"→无法删除(除非计数)+无法枚举(没有遍历)]
  - 关键设计: 缓存穿透预防 — 用布隆过滤器确保key一定在或可能在一定不在 — "一定不在"直接返回空, "可能在"再查DB — 防住了查询不存在数据的穿透

### 5. 收束 — 高级数据结构把"想复杂的事变简单"
  - Stream=MQ(消费组), Geo=ZSET+GeoHash(附近的人), Bitmap=位数组(签到), HyperLogLog=概率统计(UV), Bloom=位数组+哈希(穿透)
  - 这些不是造新轮子, 是用已存在的数据结构做巧妙的复用 — 每个高级结构背后都是一个经典算法 + Redis的工程优化
  - 域3讲过缓存穿透(布隆过滤器) — 域4给你看布隆过滤器的源码实现 — 从"怎么处理"到"为什么能处理"

---

### 核心悬念
**"数据结构讲完了 — 但Redis一宕机, 内存里所有数据都没了 — 怎么保证不丢?"**

→ 引出 持久化: RDB快照的写时复制魔法 + AOF的三种fsync策略 — 拿回你的数据 (04-persistence-replication)
