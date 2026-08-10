# 减少一次内存拷贝能让QPS翻倍 — 代码级优化的"四两拨千斤"

> Cluster C: 8 KPs | 依赖: 07-performance-methodology | 读者基线: 能读懂Java代码, 知道GC和序列化, 没做过系统性代码优化

---

### 1. 优化不是"写更快"而是"做更少" — 减少工作的策略
  你觉得优化就是选更快的库, 但更多时候是"根本不需要做这件事"。
  - 优化的三层金字塔: 减少工作(不序列化不需要的字段)→减少次数(批量替代逐条)→减少开销(Protobuf替代JSON) — 三层从根本性递减 (B4 Ch5 §5.1, B5 Ch8 §8.3)
  - 减少并发冲突: ThreadLocal(线程独享副本, 不用锁)→数据分片(每个线程处理自己的区间, 最后合并)→LongAdder(分段累加, 不竞争单个AtomicLong) (B4 Ch5 §5.4, B3 Ch4 §4.3)
  - LongAdder vs AtomicLong: AtomicLong(所有线程CAS一个value, 高竞争无限自旋)→LongAdder(Cell数组, 线程映射到槽, 只CAS自己的Cell, 读时sum汇总) [工程: LongAdder的Cell数组初始2个, 竞争检测到碰撞→扩容到4→8→..., sum()遍历所有Cell累加 — 高并发写远优于AtomicLong, 但sum非原子]
  - 关键设计: 不是所有场景LongAdder都更优 — 低竞争(1-2线程)AtomicLong直接CAS更快(LongAdder有Cell分配+sum开销), 高竞争(>4线程)LongAdder优势明显

### 2. 零拷贝 — 磁盘上的文件怎么"零次拷贝"发送到网络?
  你写了一个文件下载接口, 数据从磁盘→内核缓冲区→用户缓冲区→Socket缓冲区→网卡 — 4次拷贝? 其实可以只有2次。
  - 传统IO: disk→DMA到内核→CPU拷贝到用户→CPU拷贝到Socket→DMA到网卡 — 4次拷贝(2次DMA+2次CPU)+4次上下文切换 (B5 Ch8 §8.4, B3 Ch4 §4.2)
  - sendfile: disk→DMA到内核→CPU拷贝到Socket→DMA到网卡 — 3次拷贝(2DMA+1CPU)+2次上下文切换, 跳过用户态 (B5 Ch8 §8.4) [理论: sendfile的零拷贝本质是"零CPU拷贝" — DMA拷贝不算因为CPU不参与; 但用户态缓冲区被绕过, 所以无法对数据做修改]
  - mmap+write: mmap映射内核页到用户空间→write拷贝到Socket — 适合小文件且需要修改数据的场景
  - Kafka的零拷贝: Consumer拉消息→sendfile(socket, file) — 消息从PageCache直接DMA到网卡, 完全不进用户态 — 这是Kafka高吞吐的基石 (B5 Ch8 §8.4)

### 3. 对象池 — 频繁new对象导致GC频繁, 能不能复用?
  你监控到Young GC每2秒一次, 每次STW 50ms — 高峰期P99毛刺全来自GC暂停。
  - 对象池成本: new T() = 分配内存(指针碰撞/TLAB) + 构造方法 + GC回收 — 如果对象生命周期短且频繁创建→Young GC频繁 (B5 Ch8 §8.5, B4 Ch5 §5.3)
  - Netty Recycler: FastThreadLocal+Stack(每线程一个栈, pop回收对象)→WeakOrderQueue(跨线程归还) — 复用ByteBuf减少GC (B5 Ch8 §8.5)
  - 对象池陷阱: 池化增加代码复杂度(借+还)+内存占用(池中对象不被GC)+并发瓶颈(借还竞争) — 只在对象创建昂贵(连接/大缓冲区)时用池 (B5 Ch8 §8.5, B6 Ch2 §2.3)
  - 关键设计: 对象池 vs 逃逸分析 + 栈上分配 — JIT如果能证明对象不逃逸→栈上分配(随栈帧销毁自动回收) → 池化的价值下降; 但栈上分配有条件(对象小+不逃逸)

### 4. 批量与异步 — 1000次单条插入 vs 1次批量插入, 差100倍
  你插入1000条日志, 一条条insert RT=3秒, 改批量后20ms — 为什么差这么多?
  - 网络RTT: 1000次insert=1000次网络往返(每次0.5ms=500ms网络开销)+1000次事务提交+1000次磁盘fsync — 批量=1次网络+1次事务+1次fsync (B1 Ch5 §5.4, B5 Ch9 §9.3)
  - JDBC批量: rewriteBatchedStatements=true → INSERT ... VALUES (1,2), (3,4), ... → 单条SQL多条值 — 比addBatch+executeBatch更彻底(再合并一次) (B5 Ch9 §9.3)
  - Redis Pipeline: 1000次SET→1000次RTT(500ms)→Pipeline→1次RTT(0.5ms) — 吞吐1000x, 但注意Pipeline中出错处理 (B5 Ch10 §10.4) [工程: 批量vs异步 — 批量=把同类操作打包发(空间聚合), 异步=把操作推迟发(时间分拆), 二者互补: 批量减少网络次数, 异步提高响应速度]
  - MQ削峰: 同步写DB 1000 QPS→前端5000 QPS→MQ缓冲→DB仍然1000 QPS消费 — 削峰的本质是让消费者按自己的节奏处理, 而不是被生产者的速度压垮

### 5. 预热与序列化优化 — "第一次慢"的问题怎么解决?
  JIT没编译完、缓存没填充、连接池刚创建 — 你的服务刚启动就这么扛流量, 上来就打崩。
  - JIT预热: -XX:CompileThreshold(方法调用次数+循环回边次数达到阈值才触发编译) — 启动后用流量回放/JMH(预热fork)预热热点代码 (B4 Ch5 §5.1)
  - 缓存预热: 启动时加载热点数据到Redis/Caffeine → 避免启动瞬间所有请求穿透到DB [案例: 电商首页缓存预热 — 服务启动时异步加载Top 1000商品到Caffeine→等Caffeine命中率稳定后再注册到服务发现→避免冷启动穿透]
  - 连接池预热: HikariCP初始化时填充minimumIdle连接→避免第一次请求等连接创建
  - 序列化优化: JSON→Protobuf(体积减少60-80%+序列化快3-5x)→字段裁剪(按需序列化, 只传需要的字段)→Kryo(Java专精, 不需要Schema) (B4 Ch5 §5.5, B5 Ch6 §6.3)

### 6. 收束 — 代码优化的本质是减少浪费
  - 零拷贝: 减少CPU拷贝次数
  - 对象池: 减少对象创建销毁
  - 批量: 减少网络往返次数
  - 异步: 减少响应等待时间
  - 预热: 减少"第一次"开销
  - 每一项优化的背后都是"减少不必要的操作" — 不是魔法, 是精准的资源管理

---

### 核心悬念
**"你优化了代码, QPS上去了 — 但GC变得频繁, P99毛刺反而增加。你的代码快了, 但JVM成了新的瓶颈 — 怎么办?"**

→ 引出 JVM调优: GC选型/堆配置/直接内存/JIT编译/Arthas在线诊断 (09-jvm-tuning)
