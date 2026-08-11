# stage-2 · 第 9 节：Zookeeper 数据模型 — 知识点提取

> 课程：stage-2 模式设计与实现 第 9 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/09. 第九节：Zookeeper 数据模型.md`
> 提取时间：2026-08-11 | 权重：核心（ZooKeeper 数据模型主线）

---

## 一、本节概览

- **技术域**：ZooKeeper 数据模型（znode/Stat/节点类型/监视/序列化 Jute）
- **维度**：`[分布式问题]`（协调服务/命名/同步）+ `[工程问题]`（数据模型/监视机制/序列化）
- **核心命题**：理解 ZooKeeper 的 znode 分层数据模型、节点类型、Stat 结构、监视机制
- **知识点数**：15 个
- **前置**：分布式协调基本概念、路径/命名空间、CAP（第 1 节）

## 前置条件清单
读者需先掌握：
1. **分布式协调服务**（配置/命名/同步/组服务场景）
2. **CAP 理论**（第 1 节）
3. **文件系统命名空间**（znode 类比）
4. **Raft/ZAB 一致性**（第 3/4 节，服务复制背景）
未达前置者，先补：第 1 节 CAP + 文件系统路径概念

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：znode/Stat/监视直接对照源码讲
- **工程化弱**：部署配置、序列化补基础
- **必做**：对照本地 `code/spring/zookeeper` 源码验证（非只看 docs，08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 ZooKeeper 简介（集中式协调服务）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（ZK 当前主流） | **置信度**：High
- **前置**：分布式协调
- **来源**：docs §简介 + §总揽
- **需求**：提供集中式服务维护配置/命名/同步/组服务，避免应用重复实现
- **自主实现**：若我设计——共享分层命名空间(znode)供分布式进程协调
- **参考实现**（docs）：ZooKeeper 用于维护配置信息、命名、分布式同步、组服务的**集中式服务**；分布式进程通过共享分层命名空间(znode)协调，类似文件系统；提供高吞吐/低延迟/高可用/严格有序的 znode 访问；服务在机器上复制，多数可用即服务可用
- **对比取舍**：**集中协调 + 内存数据**——数据树内存映像(高吞吐低延迟)，多数可用(可靠)；内存限制数据量小
- **测试佐证**：本地 `code/spring/zookeeper` 源码

### KP-02 命名空间与路径规则
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：文件系统路径
- **来源**：docs §数据模型 + 源码验证
- **需求**：定义 znode 路径的规范与约束
- **自主实现**：若我设计——路径规范绝对、斜线分隔，约束非法字符
- **参考实现**（docs + 源码）：分层命名空间类似文件系统，路径绝对/斜线分隔/无相对；约束——空字符、\u0001-\u001F 等不可用、保留 "zookeeper" 关键字、不允许 "." /".."；关联 API `org.apache.zookeeper.common.PathUtils#validatePath`
- **对比取舍**：**路径约束**——保证跨语言绑定安全；`zookeeper` 关键字保留
- **测试佐证**：源码 `common/PathUtils.java`(validatePath)

### KP-03 ZNode 基本概念（节点与 Stat）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §ZNodes
- **需求**：理解 znode 是可带数据+子节点的树节点
- **自主实现**：若我设计——每个 znode 维护 Stat 结构（数据/ACL 版本号+时间戳），协调更新
- **参考实现**（docs）：ZooKeeper 树中每个节点叫 znode；维护 **Stat 结构**（数据更改和 ACL 更改版本号 + 时间戳）；每次数据变化版本号递增；客户端更新/删除必须提供版本，不匹配则失败(可覆盖)；znode 与服务器/对等体/客户端术语区分
- **对比取舍**：**版本控制并发**——版本号验证缓存/协调更新，防并发覆盖
- **测试佐证**：源码 `server/DataNode.java` + `data/Stat.java`

### KP-04 临时节点（Ephemeral Nodes）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：会话概念
- **来源**：docs §临时节点 + 源码验证
- **需求**：创建随会话存活的节点（会话结束自动删除）
- **自主实现**：若我设计——ephemeral 节点绑定会话生命周期
- **参考实现**（docs + 源码）：只要创建 znode 的会话活跃就存在，会话结束删除；**临时节点不允许子节点**；`getEphemerals()` 检索会话临时节点列表（服务发现典型用例）；源码 `CreateMode.EPHEMERAL(1, true, ...)`
- **对比取舍**：**会话绑定生命周期**——适合服务发现/锁（会话失效自动清理）
- **测试佐证**：源码 `CreateMode.java`(EPHEMERAL 43 行)

### KP-05 顺序节点（Sequence Nodes）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §顺序节点 + 源码验证
- **需求**：创建时自动附加单调递增计数器（唯一命名）
- **自主实现**：若我设计——路径末尾附加计数器，父节点唯一
- **参考实现**（docs + 源码）：创建时请求附加单调递增计数器，父节点唯一；格式 `%010d`(10位0填充，如 00000000001) 简化排序；例 `/locks/biz-1/00000000001`；源码 `CreateMode.PERSISTENT_SEQUENTIAL(2,...)`/`EPHEMERAL_SEQUENTIAL(3,...)`
- **对比取舍**：**唯一有序命名**——计数器格式简化排序，适合分布式锁/队列
- **测试佐证**：源码 `CreateMode.java`(PERSISTENT_SEQUENTIAL 39/EPHEMERAL_SEQUENTIAL 48)

### KP-06 容器节点与 TTL 节点（3.6 引入）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]`（3.6 新增特性） | **置信度**：High
- **前置**：KP-05
- **来源**：docs §容器节点 / §TTL节点 + 源码验证
- **需求**：提供容器(自动清理)与 TTL(超时删除)节点
- **自主实现**：若我设计——ContainerNode 最后子节点删除后自身候选删除；TTL 节点超时未修改无子节点则候选删除
- **参考实现**（docs + 源码）：
  - **容器节点**：最后子节点删除后容器成为候选删除；创建子级时可能 NoNodeException 需重建
  - **TTL 节点**：PERSISTENT/PERSISTENT_SEQUENTIAL 可设毫秒 TTL，超时未修改且无子节点则候选删除；**默认禁用，需 System 属性启用**
  - 源码 `CreateMode.CONTAINER(4,...)`/`PERSISTENT_WITH_TTL(5,...)`/`PERSISTENT_SEQUENTIAL_WITH_TTL(6,...)`
- **对比取舍**：**自动生命周期**——容器/TTL 免手动清理，适合引导/锁/缓存
- **测试佐证**：源码 `CreateMode.java`(CONTAINER 58/PERSISTENT_WITH_TTL 64/PERSISTENT_SEQUENTIAL_WITH_TTL 71)

### KP-07 ZooKeeper Stat 数据结构
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §Stat 数据结构 + 源码验证
- **需求**：用 Stat 记录 znode 元数据（版本/时间戳）
- **自主实现**：若我设计——Stat 字段含 zxid/时间/版本号
- **参考实现**（docs + 源码）：Stat 字段——`czxid`(创建 zxid)/`mzxid`(上次修改 zxid)/`pzxid`(子节点修改 zxid)/`ctime`/`mtime`(创建/修改时间)/`version`(数据变更次数)/`cversion`(子节点变更次数)/`aversion`(ACL 变更次数)/`ephemeralOwner`(临时节点所有者会话 id)/`dataLength`/`numChildren`；源码 `data/Stat.java`
- **对比取舍**：**版本 + zxid + 时间**——三版本号协调更新，zxid 排序，ephemeralOwner 标识会话
- **测试佐证**：源码 `data/Stat.java`（czxid/mzxid/version/ephemeralOwner 等字段）

### KP-08 ZooKeeper 中的时间（Zxid/version/Ticks/Realtime）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07
- **来源**：docs §Zookeeper 中的时间 + 源码验证
- **需求**：理解 ZK 如何追踪时间（非实时时钟）
- **自主实现**：若我设计——用 zxid/version/tick 而非实时时钟
- **参考实现**（docs + 源码）：ZK 四种时间——
  - **Zxid**：每次更改获得唯一戳，暴露总顺序(zxid1<zxid2 则先发生)
  - **version**：数据/cversion/aversion 三版本号
  - **Ticks**：多服务器用 tick 定义事件时间，最小会话超时=2×tickTime
  - **Realtime**：**不使用实时/时钟时间**，仅把时间戳放 Stat
  - 源码 `common/Time.java`——`currentElapsedTime()`(nanoTime，防系统时钟变更) vs `currentWallTime()`(墙钟)
- **对比取舍**：**逻辑时间优先**——zxid/version/tick 保证有序，不依赖实时时钟（呼应 CAP/分布式时钟）
- **测试佐证**：源码 `common/Time.java`(currentElapsedTime/currentWallTime)

### KP-09 数据存储（Data Access / 原子读写）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §数据存储
- **需求**：理解 znode 数据读写特性与大小限制
- **自主实现**：若我设计——原子读写全部数据，ACL 控制权限
- **参考实现**（docs）：znode 数据**原子读写**（读全量、写替换全量）；每节点有 **ACL** 限制权限；**非通用数据库/大对象存储**，管理协调数据(配置/状态/会合，KB 级)；**1M 健全性检查**；大数据存 NFS/HDFS 只存指针
- **对比取舍**：**协调数据专用**——1M 限制防误用为大数据存储；大对象存外部只存指针
- **测试佐证**：源码 `server/DataNode.java`

### KP-10 监视（Watches）机制
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-09
- **来源**：docs §数据变更·监视 + 源码验证
- **需求**：客户端监听 znode 变化，获得通知
- **自主实现**：若我设计——读取操作设置监视，数据变化触发一次性通知
- **参考实现**（docs + 源码）：客户端可在 znode 设监视，改变触发后清除；**一次触发**(一次性)、**发送到客户端**(异步)、**为其设置监视的数据**；两类监视——数据监视(getData/exists) 与 Children 监视(getChildren)；`setData` 触发数据监视、`create` 触发新节点数据监视+父子监视、`delete` 触发删除节点数据/子监视+父子监视；监视在连接的服务器本地维护
- **对比取舍**：**一次触发模型**——收到事件后需重设；保证"先见事件后见数据"(顺序保证)
- **测试佐证**：源码 `server/watch/WatchManager.java`(triggerWatch)+`server/WatchManager`

### KP-11 监视语义 + 持久递归监视 + 移除
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-10
- **来源**：docs §监视语义/持久递归/移除
- **需求**：理解监视触发事件类型与持久/递归监视
- **自主实现**：若我设计——按调用分类事件，支持持久递归监视
- **参考实现**（docs）：
  - **事件语义**：Created(exists 启用)/Deleted(exists,getData,getChildren)/Changed(exists,getData)/Child(getChildren)
  - **持久递归监视**：`addWatch()` 设置，触发不移除，递归触发 NodeCreated/NodeDeleted/NodeDataChanged(不触发 NodeChildrenChanged，冗余)
  - **移除监视**：`removeWatches()` 删除；Child Remove/Data Remove/Persistent Remove 事件
  - **保证**：监视按其他事件/异步回复排序；先见监视事件再见数据；顺序与服务器更新序一致
- **对比取舍**：**持久递归扩展标准监视**——免重复设置，递归覆盖子树；一次性 vs 持久权衡

### KP-12 监视核心 API + 服务器处理（客户端/服务器）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-10/11
- **来源**：docs §核心 API + 源码验证
- **需求**：理解监视的客户端/服务器 API 实现
- **自主实现**：若我设计——客户端 ZooKeeper/Watcher，服务器 WatchManager/RequestProcessor
- **参考实现**（docs + 源码）：
  - **客户端**：`ZooKeeper`(getData 可带 watch)、`Watcher`(process)、`WatchedEvent`(keeperState/eventType/path)、`Event.KeeperState`(Disconnected/SyncConnected/Expired 等)、`Event.EventType`(NodeCreated/NodeDeleted/NodeDataChanged/NodeChildrenChanged)、`WatchRegistration`(Data/Child/Exists)、`ClientCnxn`(submitRequest 同步等待 + SendThread.finishPacket 唤醒)
  - **服务器**：`FinalRequestProcessor`(OpCode.getData 处理，反序列化 GetDataRequest → 查 DataNode → checkACL → getData)、`OpCode`(create/delete/exists/getData/setData 等)、`WatchManager.triggerWatch`(watchTable 移除 + watch2Paths 更新，Path↔Watcher N:M)、`ServerCnxn`(NIOServerCnxn/NettyServerCnxn)
- **对比取舍**：**客户端/服务器分工**——客户端 submitRequest 同步等待、SendThread 异步唤醒；服务器 WatchManager 维护 path↔watcher 映射触发
- **测试佐证**：源码 `server/FinalRequestProcessor.java`(getData case)+`server/watch/WatchManager.java`(triggerWatch)+`server/ServerCnxn.java`

### KP-13 序列化 Jute
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟢 | **优先级**：P3 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：序列化
- **来源**：docs §序列化 Jute（仅标题，架构师发散 + 源码）
- **需求**：ZK 自定义序列化框架
- **自主实现**：若我设计——Record 接口 + OutputArchive/InputArchive 序列化器
- **参考实现**（docs 仅标题 + 源码）：**Jute** 是 ZK 自定义序列化框架——`Record` 接口(可序列化对象)、`OutputArchive`(序列化器)、`InputArchive`(反序列化器)；用于客户端/服务器协议消息(如 GetDataRequest)
- **对比取舍**：**自定义序列化**——Jute 轻量专用于 ZK 协议，对比 Java 原生序列化/Protobuf
- **测试佐证**：源码 `server/DataNode`/协议类用 Jute；`zookeeper-jute` 模块

### KP-14 读写请求处理策略（读本地/写协商）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **来源**：docs §总揽（第 12 行）
- **需求**：理解 ZK 读写吞吐特性（读扩写缩）
- **自主实现**：若我设计——读本地处理(水平扩展)、写转发协商一致(串行化)
- **参考实现**（docs）：读请求在客户端连接的服务器**本地处理**(监视也本地跟踪)；**写请求转发其他服务器，生成响应前经过协商一致**；同步请求转发但不过协商一致
- **对比取舍**：**读写不对称**——读吞吐随服务器数增加，写吞吐随服务器数减少(需多数共识)；这是 ZK 读多写少工作负载的设计
- **测试佐证**：源码 `server/FinalRequestProcessor`(getData 本地)/ZAB(写协商，第 4 节)

### KP-15 部署配置（单机/集群）
- **维度**：`[工程问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]`（ZK 部署配置） | **置信度**：High
- **前置**：无
- **来源**：docs §快速上手（单机/集群部署）
- **需求**：理解 ZK 单机与集群部署配置
- **自主实现**：若我设计——zoo.cfg 配置 + 集群 server.X/myid
- **参考实现**（docs）：**单机 zoo.cfg**——`tickTime`(基本时间单位，心跳，最小会话超时=2×tickTime)、`dataDir`(快照+事务日志存储)、`clientPort`(客户端端口)；**集群**——至少 3 台(奇数)，`initLimit`(连接 leader 超时)、`syncLimit`(与 leader 过时限制)、`server.X=host:2888:3888`(X 由 dataDir 的 myid 文件标识)、2888=对等连接(follower→leader)、3888=leader 选举；单机多服务器需唯一端口/数据目录，无冗余(同物理机易整体故障)
- **对比取舍**：**单机 vs 集群**——单机便于评估开发；生产需复制模式(多数仲裁)；2888 通讯/3888 选举双端口分离
- **测试佐证**：`conf/zoo.cfg` 配置示例

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| ZooKeeper 简介 | 分布式问题 | 核心 | P1 | 🔴 | 有效 | High |
| 命名空间/路径规则 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| ZNode 基本概念 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 临时节点 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 顺序节点 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 容器/TTL 节点 | 分布式问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| Stat 数据结构 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| ZK 时间(Zxid/version/Tick) | 分布式理论 | 核心 | P1 | 🟡 | 时间无关 | High |
| 数据存储(原子/1M) | 工程问题 | 支撑 | P2 | 🟡 | 时间无关 | High |
| 监视机制 | 分布式问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| 监视语义/持久递归 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 监视 API/服务器处理 | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 序列化 Jute | 工程问题 | 支撑 | P3 | 🟢 | 时间无关 | Medium |
| 读写请求处理策略 | 分布式问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| 部署配置(单机/集群) | 工程问题 | 支撑 | P2 | 🟡 | 有效 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/zookeeper`——CreateMode/Stat/PathUtils/DataNode/WatchManager/FinalRequestProcessor/ServerCnxn/Time 全部验证
- **关键源码类**（本次实证）：`CreateMode`(PERSISTENT 0/EPHEMERAL 1/PERSISTENT_SEQUENTIAL 2/CONTAINER 4/PERSISTENT_WITH_TTL 5/PERSISTENT_SEQUENTIAL_WITH_TTL 6)、`common/PathUtils`(validatePath)、`common/Time`(currentElapsedTime/currentWallTime)、`server/watch/WatchManager`(triggerWatch)、`server/FinalRequestProcessor`(getData case)
- **关联标注**：microsphere-* 用 zookeeper 作协调存储 `[待验证]`；衔接 ZAB(第 4 节)

---

## 五、本节小结（三层次视角）

**需求**：提供集中式协调服务，用 znode 分层命名空间存储配置/命名/同步数据。

**自主实现核心**：若我设计——
1. znode 分层命名空间 + 路径约束
2. 节点类型：临时(会话绑定)/顺序(计数器)/容器(自动清理)/TTL(超时删除)
3. Stat 版本号 + zxid 协调并发
4. 逻辑时间(zxid/version/tick) 非实时时钟
5. 一次触发监视(数据/Children 监视) + 持久递归扩展
6. Jute 序列化
7. 读写不对称：读本地扩、写协商缩
8. 部署：单机 zoo.cfg / 集群 server.X+myid(2888 通讯/3888 选举)

**参考实现**：ZooKeeper 源码（`code/spring/zookeeper` 完整验证）+ docs。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**ZooKeeper 数据模型**"。核心洞察：**znode 分层命名空间、节点类型(临时/顺序/容器/TTL)、Stat 版本控制、逻辑时间、一次触发监视**。为第 10 节通讯与会话、第 11 节共识实现铺垫。

**待验证汇总**：
- microsphere 用 zookeeper 的具体场景
- Jute 序列化细节

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为数据模型说明 + 源码片段 + 本地源码验证；补全聚焦"ZK 数据模型的工程价值"。

### 完整认知：ZooKeeper 数据模型在真实架构中完整该讲什么

docs 覆盖了 znode/Stat/监视/序列化。作为架构师，这个主题完整还该包含：

1. **znode 是分布式协调的通用原语**：配置、命名、锁、队列、服务发现都建立在 znode 之上——理解 znode 是理解 ZK 一切应用的基础
2. **临时节点 + 会话 = 故障检测**：ephemeral 节点随会话删除，天然实现"进程崩溃自动清理"（锁自动释放、服务自动下线）——这是 ZK 做分布式锁/服务的核心
3. **顺序节点 = 分布式唯一 ID/公平锁**：计数器格式(00000000001)保证排序，配合临时节点实现公平分布式锁
4. **Watch 的"一次触发 + 先见事件再见数据"**：是观察者模式在分布式的一致性体现；注意一次性需要重设，且可能错过变化(事件间隙)
5. **逻辑时间 vs 实时时钟**：zxid/version 保证全序，不依赖时钟——呼应分布式无全局时钟问题
6. **1M 数据限制的架构含义**：ZK 存"小协调数据 + 大对象指针"，不是存储引擎——架构上应避免把大对象塞 ZK
7. **Watch + 长连接**：监视在连接服务器本地维护，断连重连时重注册——与第 10 节会话机制紧密相关

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| 内存数据 vs 磁盘 | 内存高吞吐低延迟；受内存限制(数据小) |
| 一次性 Watch vs 持久递归 | 一次性简单；持久递归免重设但复杂 |
| 逻辑时间 vs 实时时钟 | 逻辑全序可靠；实时时钟不可靠 |
| 1M 限制 | 防误用存储；大对象需外置 |
| 临时 vs 持久节点 | 临时会话绑定(自动清理)；持久需手动管理 |

### 常见坑/反模式

1. **把 ZK 当数据库**：塞大对象超过 1M——存指针而非数据
2. **临时节点误用**：需要持久数据却用临时(会话断即删)——按生命周期选
3. **Watch 不重设**：一次触发后不重设，错过后续变化
4. **依赖实时时钟**：ZK 用逻辑时间，跨节点实时时钟不可靠
5. **路径含非法字符**：违反 PathUtils 约束

### 生态位置

- **分布式问题维度**：ZooKeeper 数据模型是**协调服务基础**——衔接 ZAB(第 4 节一致性)、第 10 节通讯会话、第 11-12 节共识实现/运用
- **衔接**：ZAB(第 4 节) → 数据模型(本篇) → 通讯会话(第 10 节) → 共识实现(第 11 节)
- **与源码提取的关系**：zookeeper-server 是核心源码

**架构师视角结论**：本篇不只是背 znode/Stat，而是"**理解 ZooKeeper 作为协调服务的数据模型地基**"——znode 分层命名空间、节点类型(临时/顺序/容器/TTL)、Stat 版本控制、逻辑时间、一次触发监视；这是 ZK 实现锁/服务发现/配置的一切实用基础。
