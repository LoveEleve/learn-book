# 前面学的CAP/Paxos/TrueTime, Google的GFS/ZK/Spanner是怎么用的? — 五位一体案例

> Cluster D: 15 KPs | 依赖: A+B+C(全栈, 已知CAP+共识+事务) | 读者基线: 完成前11篇

---

### 1. GFS — "你要存1PB日志文件, 单机存不下, Google怎么解决的?"
  GFS用master+chunkserver架构, 64MB chunk减少寻道开销 — 2003年的论文, 这些设计现在看很粗糙, 但思想影响了一切分布式存储。
  - B1 Ch7 §1: GFS架构 — Master(元数据, 单点但无状态)+ChunkServer(数据, 3副本) → 客户端读文件: 问Master chunk位置→直接读ChunkServer(数据不流经Master)
  - 关键设计: 租约(Lease) — Master给Primary副本发租约(60s) → Primary定序写操作 → 转发给Secondary → 全部成功→返回客户端
  - 追加写 vs 随机写: GFS只支持追加(append) → 简化一致性模型(至少一次语义, 可能有padding/duplicate) → 语义放宽换来高性能
  - 为什么Master单点? Google认为通过外部机制(Chubby锁服务+快速恢复)可以保证可用性 — 典型的设计选择: 简单 > 完美 [案例: GFS选择大块(64MB)而非小块 — 磁盘寻道>网络传输，大块减少元数据]

### 2. ZooKeeper — "50个微服务需要一个配置中心, 怎么保证配置修改后所有服务立刻感知?"
  ZK用Watch机制+ZAB保证配置变更的顺序一致性 — 类文件系统的ZNode树是分布式协调的瑞士军刀。
  - B1 Ch7 §2: ZK数据模型 — 类文件系统的ZNode树(/path/to/node) — 每个ZNode可存小数据(~1M) + 版本号(每次写递增)
  - 关键设计: 会话(Session) — 客户端连接ZK时创建, 心跳(默认30s)维护 → 会话过期 → ZK删除该客户端的临时节点 → Watch通知其他客户端
  - Watch机制: 一次触发 → 客户端收到通知后需重新注册 → 保证了"至少通知一次"但不保证"通知全部变化" — 适合"节点存在性"、"配置变化"这类一次性事件
  - 典型原语: 分布式锁(临时顺序节点+watch前节点, Curator封装), 配置中心(watch永久节点), 选主(临时节点+watch), 服务发现(临时节点+watch子节点)

### 3. Bigtable — "10亿条网页要存, 需要按URL前缀快速扫描, 关系型数据库完全不适用"
  Bigtable用行键排序+列族+SSTable实现10ms级范围扫描 — 一个能存PB级数据, 按(row, column, timestamp)→value的巨大多维排序Map。
  - B1 Ch7 §3: Bigtable数据模型 — (row:string, column:family:qualifier, timestamp:int64) → cell contents — 排序存储, 按row key字典序
  - 关键设计: Tablet分裂 — 表按row range分成Tablet(~100-200MB) → 变大自动分裂 → Tablet是分布和负载均衡的基本单位
  - LSM-Tree SSTable: 写入→memtable(内存)→刷到SSTable(排序的不可变文件)→compaction合并 → 写入快(顺序写), 读取需要查多个SSTable→用Bloom Filter加速
  - 底层: GFS存SSTable + Chubby做锁服务 + 单Master做元数据 → 组件化设计: 存储/GFS, 锁/Chubby, 元数据/Master

### 4. Dynamo/Cassandra — 永远可写, 允许不一致
  Amazon的Dynamo说"购物车绝对不能拒绝写入" — 怎么做到?
  - B1 Ch7 §4: Dynamo架构 — 一致性Hash环(物理节点多份虚拟节点→均衡) + Sloppy Quorum(临时接管, Hinted Handoff) + 向量时钟(并发版本检测)
  - 关键设计: 永远可写的代价 — 不保证强一致(最终一致) → 读修复(Read Repair, 读时发现旧版本→更新) + 反熵(Anti-Entropy, 后台Merkle树比较副本差异)
  - Cassandra继承: 同样的Hash环 + 加入可调一致性(写W/读R, W+R>ReplicationFactor→强一致, W+R≤RF→最终一致)
  - Dynamo vs GFS: Dynamo选AP(永远可写, 最终一致), GFS选CP(租约保证一致性, Master单点但可恢复) → 这正是CAP的工程体现

### 5. Spanner — Google把CAP三角的第三条腿补齐了
  Dynamo选AP, Bigtable/GFS选CP — 直到Spanner用TrueTime告诉你"我既要强一致又要全球可用"。
  - B1 Ch7 §5: Spanner = 全球分布式SQL数据库 — 数据跨数据中心分片(Paxos共识组) → TrueTime提供`commit_ts` → 外部一致(比线性一致还强: 物理时间上也一致)
  - 关键设计: Paxos per shard — 每个分片独立Paxos组(独立Leader) → 跨分片事务用2PC(协调者是其中一个Paxos组Leader) → 结合Paxos(分片内强一致/高可用)+2PC(跨分片原子性)+TrueTime(全局时间序)
  - TrueTime + Commit Wait: 事务选commit_ts(TrueTime now() + ε) → 等TT.after(commit_ts)=true(即所有TT时间都已过ts) → 再release锁 — 保证外部一致(T1的commit_ts在物理时间上早于T2的start → T2必读T1的修改) [案例: Spanner TrueTime — 用原子钟+GPS硬件实现不确定区间[ε, ε]，对外承诺外部一致性]
  - 这是CAP三元都用的系统: 用Paxos保C(一致性), 用多副本保A(可用性但P发生时选C), TrueTime让弱C的业务能感知排序 — 不是打破CAP而是扩大"正常状态"的范围

### 6. 收束 — 五位一体, 理论全验证
  - GFS: 单一Master+租约+追加写 — CAP选CP, 牺牲"精确一致性"换性能
  - ZooKeeper: ZAB共识+Watch — CAP选CP, 用会话+临时节点做协调原语的基石
  - Bigtable: LSM-Tree+Tablet分裂 — 不做分布式事务, 单行原子, 交给上层(Percolator/Spanner)
  - Dynamo/Cassandra: 永远可写+向量时钟+读修复 — CAP选AP, 用现实接受不一致换极高可用
  - Spanner: Paxos+2PC+TrueTime — CAP中首次实现真正的"全球范围外部一致", 补齐三角

---

### 核心悬念
**"分布式理论入门完成了——CAP告诉你不可能, Paxos告诉你可能, Spanner告诉你可能且实用。下一站: 微服务架构怎么把这些理论落在Spring Cloud/Dubbo/K8s上?"**

→ 引出 分布式专题 域2: 架构与微服务 (Spring Cloud/K8s/Istio架构篇)
