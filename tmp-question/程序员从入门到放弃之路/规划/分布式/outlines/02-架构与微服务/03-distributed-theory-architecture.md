# CAP书上说"三者不可兼得", 你线上8个服务到底哪个牺牲了C, 哪个牺牲了A?

> Cluster A: 16 KPs | 依赖: 域1-分布式理论(01-cap-consensus等) | 读者基线: 了解CAP/Paxos/Raft基本概念

---

### 1. CAP在工程中不是三选二 — 是"在哪些操作上牺牲哪个"
  你线上用了ZooKeeper(CP)做注册中心, Redis(AP)做缓存, MySQL(Paxos Group)做事务 — 同一个架构三种选择
  - B1 Ch2 §2+§4: CAP工程化 — 不是说整个系统选CP或AP, 而是每个数据操作独立选择; 库存扣减(CP, 多扣少扣都致命), 商品列表(AP, 少显示1个商品可接受), 订单状态(PACELC, 没分区时选Consistency低延迟)
  - PACELC: Partition(A/C二选一) Else(L/C二选一) — 更精确: 没分区时也在Latency和Consistency之间权衡 (B1 Ch3 §6)
  - BASE: Basically Available(核心可用)+Soft State(可接受中间态)+Eventually Consistent(最终收敛) — CAP中AP分支的工程可操作指南 (B1 Ch2 §5)
  - 关键设计: 一个系统三种CAP选择不是矛盾 — 是每个数据要求的SLA不同, ZK用于配置(少改/高要求)选CP, Redis用于热点数据(量大/可容忍短期不一致)选AP

### 2. 你引入了ZooKeeper — 带来的不只是服务发现
  你刚开始用ZK只为了注册中心 — 后来发现它可以做分布式锁/选主/配置中心/命名服务
  - B1 Ch2 §3.1: ZK核心机制 — 顺序一致性(所有操作全局有序), 临时节点(Client会话断开自动删除), Watch(一次性通知, 需要重新注册) [理论: ZK的顺序一致性=主备模型而非复制状态机 — 写操作全部走Leader, 读可从Follower]
  - B1 Ch2 §3.2(应用场景): 配置管理(统一配置/Watch通知所有节点→动态刷新), 分布式锁(临时顺序节点+Watch前一个→无惊群效应), 选主(最小序号节点, 临时保证宕机自动换主), 命名服务(全局唯一ID, 顺序节点天然递增)
  - B4 Ch2 §11: ZK vs Etcd — ZK(ZAB协议/CP/Java生态), Etcd(Raft协议/CP/Go生态/K8s选它), 两者都是CP—网络分区时宁可不可写也不错
  - 关键设计: ZK watch是一次性的 — 为什么不是持久订阅? 防止服务端积压大量Watcher+客户端必须处理watch丢失(收到NOTIFICATION后立即getData+reset watch)

### 3. 分布式锁 — Redis单实例锁比不加锁还危险
  你用redis SET lock_key NX EX 30实现分布式锁, 主从切换时可能两个Client同时拿到锁
  - B4 Ch7 §6: 锁方案对比 — Redis SET NX EX(AP, 简单但有脑裂风险), ZK临时顺序节点+Watch(CP, Watch前驱释放自动通知+羊群效应优化), DB唯一索引(最重, 一般不用)
  - B2 Ch12 §3: Redlock争议 — Redis多节点锁(奇数节点依次加锁, 过半成功+过期时间限制) — Martin Kleppmann认为不满足安全性(GC停顿导致过期+被抢), antirez认为工程上可接受
  - ZK分布式锁详解: 创建/locks/my_lock_临时顺序节点→get /locks下所有子节点→如果自己是最小→持锁; 否则watch前一节点, 前驱释放→自己变最小→持锁 [案例: Spring Cloud ZK Lock用InterProcessMutex封装此逻辑]
  - 关键设计: 分布式锁的正确性边界 — 无绝对正确, 只有你需要什么级别的保证: 防重复执行(Redis SET NX够) vs 关键资源互斥(类ZK方案) vs 金融级(外部Fencing Token)

### 4. Paxos/Raft在架构中的角色 — 共识不是论文而是你数据库的存储引擎
  你每天都在用Paxos/Raft — 但那是MySQL/RocketMQ/Etcd在替你跑
  - 域1结论引用: Paxos解决多副本一致性, Raft更易理解, ZAB更适合主备; 工程实现(Etcd/SOFAJRaft/TiKV)才是真需求
  - B2 Ch11 §1: 高可用+强一致性代价 — Kafka的ISR机制是"半同步复制"的工程化(leader等ISR内所有副本ACK, 不等全部), 但不是完全的强一致(B2 Ch11 §1.1-1.2)
  - Raft vs Multi-Paxos选型: Raft(单Leader, 日志方向一致, 成员变更联合共识) → Etcd/TiKV/Nacos; Multi-Paxos(多Leader可并存, 需Multi-Paxos协议避免冲突) → Chubby; ZAB(主备, 乱序提交但顺序应用) → ZK (B2 Ch11 §5)
  - 关键设计: 共识算法的本质=多数派+日志+Leader — 一旦有3/5节点存活就可用, 不管你用Paxos还是Raft, 多数派是唯一保证

### 5. 架构中的一致性选择 — 从领域模型到数据库到消息队列
  你发现自己同时在处理4种一致性: ZK的强一致/MySQL的主从延迟/Kafka的分区有序/Redis的最终一致
  - 域1引用一致性模型: 线性一致(全局单视图)→顺序一致(每节点视角一致)→因果一致(有因果保序)→最终一致(收敛)
  - 架构中的分层一致性: 配置层(ZK=CP, 线性一致)→数据库层(MySQL主从=最终一致, 读己之写走主库)→消息层(Kafka=分区有序+at-least-once)→缓存层(Redis=最终一致+Canal补偿) (B4 Ch7 §3)
  - Quorum NWR模型: W(写副本数)+R(读副本数)>N(总副本数)→强一致; W+R<=N→最终一致; 调W和R是CAP的定量控制(非0/1开关) (B4 Ch7 §2.4)
  - 关键设计: 一致性不是全局开关 — 每个子系统选不同一致性模型, 整体系统是多种一致性的叠加, 关键是明确每个数据的SLA

### 6. 收束 — 回到架构的选择时刻
  - CAP不是你读论文时理解的理论 — 是你上线时每个操作都在做的选择: 库存扣2PC(CP)+商品列表Redis(AP)+配置ZK(CP)+日志Kafka(分区有序)
  - ZK/Etcd/Redis/DistributedLock都是"一致性"这个概念的不同具现 — 没有银弹, 选型靠理解每个工具的CAP边界
  - 理论是架构的地图, 不是行军路线 — 理解为什么选, 不是背"ZK=CP"

---

### 核心悬念
**"CAP告诉我们不同数据选不同策略, 但缓存让你的数据同时在MySQL和Redis两份——两份数据不一样时, 用户看到的到底是什么?"**

→ 引出 缓存策略: 从本地缓存到分布式多级缓存, 以及那个永恒的缓存一致性噩梦 (04-caching-strategy)
