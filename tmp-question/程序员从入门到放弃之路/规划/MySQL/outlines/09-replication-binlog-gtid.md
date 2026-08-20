# MySQL 复制、Binlog 与 GTID — 一次提交如何跨实例变成可回放事务

> Cluster A | 覆盖知识元: 5.1-5.3 + 5.6-5.7 | 依赖: 03 Group Commit/Undo/Recovery、08 锁与事务 | 读者基线: 事务提交、Redo/binlog、MySQL 实例
> 文章定位: MySQL 阶段 5 复制的第一篇；先建立传统位点复制、Binlog Event、GTID 和半同步的主线，再进入并行复制与延迟分析
> 打开新视角: 复制不是“把数据文件拷过去”，而是**主库产生事件日志，副本接收/持久化/回放，并用位点或 GTID 描述进度**

---

### 概念依赖链

```
03 Group Commit/binlog + 08 事务锁 → 本篇: 复制日志与复制进度
  ├─ §1 三线程复制模型(Dump/IO/SQL)
  ├─ §2 Binlog Event/row image(事件内容)
  ├─ §3 位点与 GTID(进度标识)
  ├─ §4 半同步(主库提交等待边界)
  └─ §5 延迟/Relay Log/基础设施(复制运行状态)
先讲: 事件产生 → 传输/落 Relay → 回放 → 位点/GTID → 半同步/延迟
后续依赖: 10-parallel-replication-failover(并行回放与高可用故障)
```

### 叙事顺序

1. 问题引入——主库提交后，副本怎样知道改了什么、改到哪里、是否已经回放？
2. 三线程模型——Dump、IO、SQL/worker
3. Binlog Event——statement/row、GTID/XID、row image
4. 位点与 GTID——复制进度如何表达
5. 半同步——主库何时向客户端确认
6. Relay Log、延迟和观测——副本到底落后在哪
7. 收束

### 1. 三线程模型 — 复制不是“主库直接推数据文件”

场景提示: 主库执行一条事务后，副库有哪些线程分别负责网络传输、落盘和执行？ [写作时展开]

关键设计: 经典异步复制把生产、接收和回放拆成不同角色：

```[pseudocode]
主库:
  Binlog Dump/IO source thread
  → 从 binlog 读取事件
  → 发送给副本连接

副本 IO thread:
  → 接收 binlog events
  → 写入 Relay Log

副本 SQL/applier thread:
  → 读取 Relay Log
  → 重建/执行事务
  → 更新回放进度
```

Why: 为什么副本不能只同步主库数据文件？——**数据文件包含引擎页、缓存、日志和实例状态，直接复制会破坏并发与恢复边界**；Binlog 把逻辑变更/行事件变成可传输、可过滤、可回放的日志。拆分 IO 与 SQL 也允许网络接收和本地回放分别观测/并行。 [MySQL: 传统复制线程和新版本 applier/并行回放实现存在差异，按版本区分]

比喻锚点: 主库是出单仓库，Dump 是发货员，副本 IO 是收货登记，Relay Log 是待处理货单，SQL/applier 是执行工人。 [写作时展开]

### 2. Binlog Event — 复制传输的最小叙事单位

场景提示: 副本拿到的不是“SQL 文件”，Binlog 里怎样表达 DDL、行变化和事务提交？ [写作时展开]

关键设计: Binlog 由事件组成，格式和事件集合取决于 binlog format、版本和配置：

```[pseudocode]
Event:
  header + event data

典型事件:
  GTID_EVENT
  QUERY_EVENT(部分DDL/语句)
  TABLE_MAP_EVENT(行事件对应表结构)
  WRITE_ROWS/UPDATE_ROWS/DELETE_ROWS
  XID_EVENT(事务提交相关)

row image:
  full/minimal/noblob 等配置
  → 影响事件大小与副本更新所需信息
```

Why: 为什么 row-based replication 通常更容易保证副本按行重放，却不等于事件一定更小？——**row event 携带行镜像，事件大小和 `binlog_row_image`、列宽、更新范围有关**；statement/row/mixed 还涉及非确定性函数、触发器、环境差异和可复现性。Event header 的字段/长度必须按目标 binlog 版本核对，不能固定套一个常数描述全部格式。 [MySQL: binlog_format、row image、GTID/XID 事件和工具解析器有版本边界]

比喻锚点: Binlog 像仓库的标准出货单：有的写“执行这条指令”，有的写“把这些行改成这些值”；副本按货单重建自己的库存。 [写作时展开]

### 3. 位点与 GTID — 副本如何表达“我已经追到哪里”

场景提示: 主库 binlog 文件不断滚动，副本重连后怎样避免重复或漏传？ [写作时展开]

关键设计: 位点模式用文件名/偏移，GTID 模式用事务身份集合：

```[pseudocode]
传统位点:
  MASTER_LOG_FILE + MASTER_LOG_POS
  → 从指定文件/偏移继续请求
  → 对文件保留和人工操作敏感

GTID:
  server_uuid:transaction_id
  → 事务全局标识
  → 副本发送已执行 GTID 集合
  → 主库过滤/发送缺失事务
  → auto-position 重连

gtid lifecycle:
  事务生成/提交
  → 写入 binlog/GTID 状态
  → 副本接收/执行
  → executed/purged 集合推进
```

Why: 为什么 GTID 比手工位点更适合故障切换？——**事务身份独立于某个文件偏移，副本可以按集合判断缺哪些事务**；但 GTID 集合、purged 事务、跨拓扑复制和恢复工具仍需正确维护，GTID 不会自动解决冲突或数据漂移。 [MySQL: `gtid_executed`、`gtid_purged` 和持久化状态按版本/配置核对]

比喻锚点: 位点像“第 3 本账本第 100 页”，GTID 像每笔交易的全球订单号；换一套账本后，订单号仍能判断哪些交易已经处理。 [写作时展开]

### 4. 半同步 — 主库确认的时机是可靠性与延迟的交易

场景提示: 主库写入成功后，客户端何时能认为至少有一个副本收到？ [写作时展开]

关键设计: 半同步复制在异步发送之外增加确认等待，但确认点和超时降级策略取决于配置：

```[pseudocode]
事务提交:
  主库写 binlog/引擎日志
  → 发送副本

副本:
  接收并按半同步协议确认到达/持久化边界

主库:
  after_sync / after_commit 等时机
  → 收到满足条件的 ACK
  → 向客户端确认
  → 超时可能降级为异步
```

Why: 为什么半同步不等于“永不丢数据”？——**ACK 的含义、确认时机、超时、网络故障、副本落盘和故障切换策略共同决定保证边界**；after_sync/after_commit 在异常窗口上的可见性不同，配置名不能替代故障演练。 [MySQL: 半同步插件参数、ACK 语义与降级/恢复行为按版本文档核对]

比喻锚点: 异步像发快递后立即告诉客户“已寄出”，半同步像等一个分仓回短信“已收到”再确认；短信和货物真正入库仍是不同保障层。 [写作时展开]

### 5. Relay Log、延迟与复制基础设施

场景提示: `Seconds_Behind_Master` 显示 0，副本是否一定和主库完全同步？ [写作时展开]

关键设计: 复制状态要分网络接收、Relay Log 落盘、applier 回放和可见性：

```[pseudocode]
主库 binlog
  → 网络传输
  → Relay Log
  → applier/SQL thread
  → 数据页/事务提交

延迟观测:
  SHOW REPLICA STATUS
  → IO/applier 状态、执行位点/GTID、错误
  → Relay Log 量、事务大小、回放吞吐

Seconds_Behind_Master:
  基于事件时间戳/执行状态的近似指标
  → 受时钟、空闲、长事务和并行回放影响
  → 不能单独证明读一致性或零延迟
```

Why: 为什么大事务会让 Seconds_Behind_Master 出现突发或误导？——**事件时间戳、SQL/applier 当前执行位置、主从时钟和空闲状态不等于真实数据可读一致性**；应结合 GTID executed、Relay Log、事务提交点、延迟分位和业务读路由判断。 [MySQL: `SHOW REPLICA STATUS` 字段语义与版本名称变化需核对]

### 6. 收束

复制全链路：

```[pseudocode]
主事务提交
  → binlog event/GTID
  → Dump 发送
  → IO thread 写 Relay Log
  → SQL/applier 回放
  → executed 状态推进
  → 半同步 ACK/延迟观测
```

**Aha Moment**: "复制不是“数据自动出现”，而是**事件生产、网络传输、Relay 持久化、事务回放和进度确认**五个阶段；位点/GTID告诉你进度，半同步告诉你确认边界，Seconds_Behind只是近似观测。"
**回答读者三问**: ①副本为什么要 Relay Log=把接收与回放解耦；②GTID解决什么=按事务身份定位缺失集合；③SBM 为何不可靠=时间戳/事务大小/时钟/回放状态都会影响它。

---

### 核心悬念

**"单线程回放跟不上主库怎么办？DATABASE、LOGICAL_CLOCK、WRITESET 如何并行应用事务，又怎样保持冲突与提交顺序正确？"**

→ 引出 10-parallel-replication-failover — 并行复制、MTS、故障切换与复制运维。