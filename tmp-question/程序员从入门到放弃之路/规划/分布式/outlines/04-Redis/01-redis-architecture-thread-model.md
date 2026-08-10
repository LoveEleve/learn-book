# 10万QPS打过来MySQL直接崩 — Redis凭什么10万QPS稳如狗?

> Cluster A: 4 KPs | 依赖: 域1-01/02(CAP/BASE) | 读者基线: 用过Redis的GET/SET, 被问过"为什么快"

---

### 1. 一条SET key value背后 — CPU和内存都发生了什么?
  你在终端敲下`SET name "zhang"` , 返回OK不到1ms — 这1ms里Redis做了多少件事?
  - 通信协议: RESP(REdis Serialization Protocol) — `*3\r\n$3\r\nSET\r\n$4\r\nname\r\n$6\r\nzhang\r\n` → Redis解析这串字节的代价 ~几十纳秒 (B1 Ch1 §1.2)
  - 整体架构: 全局dict(键→redisObject)→redisObject(类型+编码+指针)→底层编码(整数/SDS/压缩列表/quicklist/skiplist) — 多层映射, 每层都有优化理由 (B1 Ch1 §1.2)
  - 命令执行过程: networking(epoll收到事件)→readQueryFromClient(解析RESP协议)→processCommand(lookupKey)→call(执行命令)→addReply(写入输出缓冲区)→sendReplyToClient(epoll可写发送) — 单线程串行, 但每一步都极短 (B1 Ch1 §1.2) [工程: RESP协议设计 — 简单文本协议, 解析零回溯, 一行命令一个O(N)]
  - redisObject的编码字段: type(5种数据类型)/encoding(底层编码)/ptr(数据指针)/lru(24bit时钟)/refcount(引用计数) — 每个key对应的value都包在redisObject里 (B1 Ch1 §1.2)
  - Redis数据库空间: 默认16个db(db0-db15, 可用SELECT切换)→每个db是一个独立dict→集群模式下只使用db0 — 多db是历史遗留, 新项目不建议用 (B1 Ch1 §1.2)
  - 为什么用全局dict: 所有key在一个dict里, O(1)查找→但dict冲突(链地址法+渐进式rehash)不能再以旧理解混过去 (B1 Ch1 §1.2)
  - 关键设计: 单线程避免锁竞争 — 没有线程切换、没有锁争用、没有cache line bouncing — 但代价是任何慢操作(keys */大value del)阻塞所有人

### 2. 面试官追问: "真就单线程扛一切?"
  你答"单线程", 面试官一笑:"那IO多线程又是什么?" — 单线程是上半场, IO多线程是下半场。
  - 纯内存操作: 读写不用磁盘, 每次操作是O(logN)或O(1)内存访问→数据量再大, 访问延迟是纳秒级 [理论: 内存随机访问~100ns vs 磁盘随机I/O~10ms → 快10万倍]
  - 单线程为什么能快: CPU不是你瓶颈 — 网络带宽才是 — 命令执行占10%CPU, 网络收发占90% — 单线程足够处理这些命令, 多线程反而增加上下文切换开销
  - IO多路复用: epoll(IO multiplexing) — 一个线程同时监听多个socket→epoll_create/epoll_ctl/epoll_wait → 一旦描述符就绪, 事件分派器处理→非阻塞+事件驱动 (B1 Ch4 §4.6) [理论: select的最大缺陷 — FD_SETSIZE=1024限制 → O(N)轮询→对于10000连接性能为0 — epoll用红黑树+就绪链表, O(1)获取就绪fd]
  - IO多线程: 网络读/写由IO线程池处理→命令执行仍然单线程→瓶颈从CPU网络解析移走后, 单线程处理命令足够 (B1 Ch4 §4.6) [案例: 无IO多线程QPS~8万→开4个IO线程→QPS~15万]
  - 单线程的劣势: 一个慢命令(如KEYS * 200万条)阻塞所有请求 — 所有客户端都在等你 — 这是单线程的核心风险, 也是为什么需要SCAN等渐进式替代 (B1 Ch2 §2.11)
  - 关键设计: Redis的单线程是"命令执行单线程" — 不是全部单线程 — IO操作可以并行, 计算操作串行保证原子性

### 3. 高效数据结构 — 同样的功能, 用不同的编码
  Redis的SDS、ziplist、skiplist、dict — 每种数据结构都不是教科书上的标准实现。
  - 字符串SDS: 不是C char* — 有len记录长度(O(1)取长度)、free减少重分配、二进制安全(不用\0终址)、空间预分配(减少内存重分配) (B1 Ch2 §2.1)
  - SDS五种类型: sdshdr5(32B)/sdshdr8(256B)/sdshdr16(64KB)/sdshdr32(4GB)/sdshdr64(超级大) — strlen()拿到sdshdr的len字段就是O(1), 不需要遍历 (B1 Ch2 §2.1)
  - 小对象编码: intset(整数集合连续存储)/ziplist(压缩列表紧凑/无指针)/listpack(ziplist的替代/消除连锁更新) — 小对象用紧凑结构省内存, 超过阈值自动升级 (B1 Ch2 §2.2-2.4)
  - 全局散列表dict: 两个ht[2] — ht[0]服务→ht[1]扩容中 — 渐进式rehash(每次操作搬几个bucket, 不就搬完) — 不阻塞服务 (B1 Ch2 §2.4)
  - 多级编码的权衡: 整数可用embstr(嵌入式字符串, ≤44字节)或raw(独立分配)→短字符串用embstr一次分配, 长字符串raw两次 — 内存分配数量减半 (B1 Ch2 §2.1)
  - 关键设计: 数据量小时不用大结构(链表) — Redis根据元素数量/元素大小动态切换底层编码 — 一个List可能有3种形态(zipList/quicklist/listpack)

### 4. 收束 — Redis的性能秘密在一组互相配合的设计
  - 单线程(零竞争)+内存(零磁盘)+IO多路复用(零阻塞)+高效数据结构(零浪费) = "一万行C的真正价值"
  - Redis每操作的花费: 命令解析(纳秒级RESP) + 键查找(全局dict O(1)) + 编码操作(紧凑数据结构 O(logN)) + 网络发送 = 微秒级全程 — 这就是为什么它能10万+QPS
  - 域1的BASE最终一致性在Redis里体现为主从复制 — 写主读从, 复制是异步的, 从节点数据可能落后

---

### 核心悬念
**"你说String/Hash/Set这些都用不同底层编码 — 那到底什么时候用什么编码? 排行榜用Skiplist不是字典就能做?"**

→ 引出 5种基础数据结构底层实现: SDS/intset/dict/skiplist/quicklist — 每种编码的精巧设计 (02-core-data-structures-1)
