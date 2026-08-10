# Redis报OOM了 — 你设置了maxmemory 4GB, 但淘汰策略选了noeviction, 写不进去了

> Cluster C: 4 KPs | 依赖: 域4-01(单线程/事件循环) | 读者基线: 遇到过OOM, 设置过maxmemory-policy, 不理解allkeys-lru和volatile-lru区别

---

### 1. 8种淘汰策略 — allkeys-lru 和 volatile-lfu 不是换个词就能混过去
  你设了`maxmemory-policy volatile-lru`, 但内存满了, 键过期了也没删 — 因为过期键不是定时被清除。
  - 策略矩阵: noeviction(写返回error)/allkeys-lru/allkeys-lfu/allkeys-random/volatile-lru/volatile-lfu/volatile-random/volatile-ttl — 区别只在于"候选集是全部key还是设置了过期时间的key"+"淘汰算法是LRU/LFU/Random/TTL" (B1 Ch4 §4.2)
  - LRU近似: Redis不维护完整的双向链表(爆内存) — 用redisObject里的24bit时钟 — 采样N个key, 淘汰最久的那个 — maxmemory-samples越大精度越高但性能开销越大 (B1 Ch4 §4.2)
  - LFU对数计数器: 不是纯计数器 — 每次访问: counter+概率增加值(如counter=255时+1概率=1/255) — 同时随时间衰减(counter每分钟/每小时减N) — 防止旧频率值"永生" [理论: LFU的Morris Counter = 用对数空间存近似频率 — 存储大小从32bit降到8bit, 误差~10% — 淘汰不用精确, 近似就够了]
  - 关键设计: allkeys-lru vs volatile-lru — 如果所有key都设了TTL, volatile和allkeys没区别 — 但缓存场景通常不设TTL, volatile就白设了(没有候选集)

### 2. 过期删除不是定时器 — 惰性删除+定期删除, 两招组合
  你设了EXPIRE 3600, 但1小时后key还在 — 过期不等于删除, 删除是Redis"顺便"做的。
  - 惰性删除: 访问key时检查过期时间→expired? 删除返回nil→不expired? 正常返回 — 从不访问=不删=占内存 (B1 Ch4 §4.2)
  - 定期删除: serverCron每100ms执行→每次随机采20个设了TTL的key→删掉已过期的→>25%过期? 重复→不重复——每次有200ms时间预算(25微秒/serverCron, 25%CPU) [工程: 定期删除不是"全部扫描" — 是随机抽样 — key越多, 扫描覆盖越低 — 但100ms做一次, 持续进行, 过期的最终会被发现并删除]
  - 关键设计: 主从关系中的过期: 从节点不主动删过期key — 等主节点发送DEL — 从节点读到过期key仍会返回(保证主从数据一致→防止时钟偏差) — 但要依赖主节点的定期删除

### 3. Reactor事件驱动 — Redis的心脏是怎么跳动的
  网络连接/文件事件/时间事件同时发生 — Redis怎么在单线程里处理所有事情?
  - 事件循环: aeMain→aeProcessEvents — 先怼文件事件(epoll_wait)直到没新事件或到时间→再怼时间事件(serverCron) — 这是一个loop, 永不退出 (B1 Ch4 §4.3)
  - 文件事件: acceptTcpHandler(新连接)/readQueryFromClient(客户端请求)/sendReplyToClient(响应) — 全部注册在epoll上 — 单线程处理, 无需锁 (B1 Ch4 §4.3)
  - 事件处理器的分发: 每个fd绑定了rfileProc(读处理器)和wfileProc(写处理器)→epoll_wait返回就绪→调用对应的处理器函数 — 这就是Reactor模式 (B1 Ch4 §4.3)
  - 时间事件: serverCron — clientsCron(超时客户端清理)/databasesCron(过期删除+rehash)/AOF+AOF重写触发/复制重连 — 每100ms做一次家务 (B1 Ch4 §4.3) [理论: aeProcessEvents = select/poll/epoll统一封装 + 最近时间事件时间作为epoll_wait超时 — 保证epoll不因为等不到事件而让时间事件延迟]
  - 关键设计: serverCron不是"定时器" — 是事件循环中优先文件事件(网络请求)然后做时间事件 — 高负载时serverCron可能被文件事件挤掉

### 4. 内存碎片 — 你的used_memory=1GB, 但OS看到的RSS=1.8GB
  重启Redis, 内存占用下去了 — 但生产不能随便重启。
  - 碎片成因: jemalloc分配固定Size Classes(8, 16, 32, 48, 64...) — 释放的内存不一定归还OS — 特别是频繁增删大value, 分配32字节释放10次, 合并成一个大块(归不回去) (B1 Ch4 §4.7)
  - activedefrag: 手动触发或自动 — 碎片率>(active-defrag-threshold-upper)启动, <(active-defrag-threshold-lower)停止 — 用CPU百分比限制(defrag-cycle-max)不影响正常请求 [工程: 碎片整理不阻塞 — 碎片整理时锁定单个分配块, 非整个key — 少量多次, 不让延迟尖刺]
  - allocator选型: jemalloc ≠ tcmalloc ≠ glibc malloc — Redis在jemalloc上碎片少, 但内存>大时还可能有碎片 — 重启是最简单但不是万能的方案
  - 关键设计: jemalloc并非Redis特意选的 — 是glibc malloc在内存>128KB时性能下降 — Redis在4.0切到jemalloc, 性能+内存碎片一起解决

### 5. 收束 — 内存管理的黄金三角
  - 淘汰(主动释放空间) + 过期(被动删无用数据) + 碎片整理(回收浪费的空间) = 内存的全生命周期管理
  - 每个机制都是"概率性"而非"确定性"的: LRU是近似的, 过期是抽样的, 碎片是渐进的 — Redis在性能和精确之间都选了偏性能的近似
  - Redis的内存管理不是自动的 — maxmemory-policy必须配, 过期删除有延迟, 碎片需要手动或自动整理 — 三件事都做对了才是合格的生产部署

---

### 核心悬念
**"内存管理让Redis不会撑死 — 但你对Redis用MULTI/EXEC做转账, 事务真的可靠吗? PubSub为什么不能替代MQ?"**

→ 引出 事务/发布订阅/客户端缓存/使用规范: Redis的"半功能"和"不是功能" (07-transaction-pubsub-cache)
