# stage-2 · 第 11 节：Apache Zookeeper 共识算法的实现 — 知识点提取

> 课程：stage-2 模式设计与实现 第 11 节
> 来源 docs：`/data/workspace/java-training-camp/stage-2/docs/11. 第十一节：Apache Zookeeper 共识算法的实现.md`
> 提取时间：2026-08-11 | 权重：核心（ZooKeeper 共识实现，源码主线）

---

## 一、本节概览

- **技术域**：ZooKeeper 共识算法实现（Leader 选举术语/规则/FastLeaderElection/RequestProcessor 链）
- **维度**：`[分布式理论]`（共识/选举算法）+ `[工程问题]`（选举实现/请求处理链，源码级）
- **核心命题**：理解 ZK 如何实现共识——Leader 选举（SID/ZXID/Vote/Quorum/变更规则/FastLeaderElection）+ 请求处理链
- **知识点数**：14 个
- **前置**：第 4 节 ZAB、第 9 节数据模型、第 10 节通讯会话

## 前置条件清单
读者需先掌握：
1. **ZAB 协议**（第 4 节：选举/同步/广播）
2. **ZooKeeper 数据模型**（第 9 节：znode/Stat）
3. **ZooKeeper 通讯**（第 10 节：会话/客户端）
4. **Leader 选举基本概念**（SID/ZXID）
未达前置者，先补：第 4 节 ZAB + 第 9 节

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 已确认
讲解策略（源码节）：
- **源码理解强**：选举算法/RequestProcessor 链直接对照源码讲
- **工程化弱**：选举参数/请求链补基础
- **必做**：对照本地 `code/spring/zookeeper` 源码验证（非只看 docs，08 教训）

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 Leader 选举算法概述（三种算法 + 演进）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[有效]`（TCP FastLeaderElection 当前） | **置信度**：High
- **前置**：ZAB
- **来源**：docs §简介
- **需求**：提供 Leader 选举算法选择集群 Leader
- **自主实现**：若我设计——选举算法需在多数机器可用时选唯一 Leader
- **参考实现**（docs）：ZK 提供三种 Leader 选举算法，`electionAlg` 指定 0-3——**0=LeaderElection**(纯 UDP)、**1=UDP FastLeaderElection**(非授权)、**2=UDP FastLeaderElection**(授权)、**3=TCP FastLeaderElection**；从 **3.4.0 起废弃 0/1/2**，只保留 TCP FastLeaderElection
- **对比取舍**：**演进到 TCP**——UDP 版本废弃，TCP FastLeaderElection 是当前标准
- **测试佐证**：源码 `server/quorum/FastLeaderElection.java`（TCP 实现）

### KP-02 术语约定（SID/ZXID/Vote/Quorum）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs §术语约定
- **需求**：明确选举核心术语
- **自主实现**：若我设计——SID 标识服务器、ZXID 标识事务、Vote 投票、Quorum 过半
- **参考实现**（docs）：**SID**(服务器 ID，唯一标识机器，=myid)、**ZXID**(事务 ID，唯一标识状态变更，各机器可能不一致)、**Vote**(投票，无法检测 Leader 时开始投票)、**Quorum**(过半机器数，`quorum = n/2+1`)
- **对比取舍**：**Quorum 过半**——`n/2+1` 保证多数，是选举安全的基础
- **测试佐证**：源码 `server/quorum/Vote.java`/`QuorumPeer`(myid)

### KP-03 变更投票规则（vote_sid/vote_zxid 对比）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §变更投票
- **需求**：收到其他投票后决定是否变更自己投票
- **自主实现**：若我设计——对比 (vote_sid,vote_zxid) 与 (self_sid,self_zxid)
- **参考实现**（docs）：每次投票处理是对 `(vote_sid,vote_zxid)` 与 `(self_sid,self_zxid)` 的对比：
  - **规则1**：`vote_zxid > self_zxid` → 认可并转发
  - **规则2**：`vote_zxid < self_zxid` → 坚持自己
  - **规则3**：`vote_zxid == self_zxid` 且 `vote_sid > self_sid` → 认可并转发
  - **规则4**：`vote_zxid == self_zxid` 且 `vote_sid < self_sid` → 坚持自己
- **对比取舍**：**ZXID 优先、SID 次之**——先比 zxid(数据新旧)，再比 sid(编号)，是选举排序核心
- **测试佐证**：docs 规则 + `FastLeaderElection.totalOrderPredicate`(对比逻辑)

### KP-04 服务器状态（LOOKING/FOLLOWING/LEADING/OBSERVING）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §服务器状态 + 源码验证
- **需求**：定义服务器角色状态
- **自主实现**：若我设计——4 种状态区分选举/跟随/领导/观察
- **参考实现**（docs + 源码）：`QuorumPeer.ServerState` 枚举——**LOOKING**(寻找 Leader，进入选举)/**FOLLOWING**(Follower)/**LEADING**(Leader)/**OBSERVING**(Observer)；源码 `QuorumPeer.java`(LOOKING 527/FOLLOWING 528/LEADING 529/OBSERVING 530)
- **对比取舍**：**4 状态含 Observer**——相比 Raft 3 状态，ZK 增加 OBSERVING(只读不参与选举)
- **测试佐证**：源码 `server/quorum/QuorumPeer.java`(ServerState 527-530)

### KP-05 选票 Vote 结构
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §核心 API·选票 + 源码验证
- **需求**：定义选举投票的数据结构
- **自主实现**：若我设计——Vote 含 id/zxid/epoch
- **参考实现**（docs + 源码）：`Vote` 类字段——`version`(版本)、`id`(推举 Leader 的 SID)、`zxid`、`electionEpoch`(选举纪元)、`peerEpoch`(Leader 纪元)；源码 `Vote.java`(字段)
- **对比取舍**：**epoch 双字段**——electionEpoch(选举轮次) + peerEpoch(Leader 纪元)，用于比较新旧
- **测试佐证**：源码 `server/quorum/Vote.java`

### KP-06 QuorumPeer（仲裁成员）+ QuorumVerifier（仲裁校验器）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-04
- **来源**：docs §核心 API·QuorumPeer/QuorumVerifier + 源码验证
- **需求**：管理仲裁协议与校验
- **自主实现**：若我设计——QuorumPeer 管理选举/跟随/领导状态 + QuorumVerifier 判定
- **参考实现**（docs + 源码）：
  - **QuorumPeer**：管理仲裁协议，三状态(选举/追随者/领导者)；设数据报套接字响应领导视图
  - **ZabState** 枚举：ELECTION/DISCOVERY/SYNCHRONIZATION/BROADCAST(对应 ZAB 第 4 节三阶段+选举)
  - **SyncMode**：NONE/DIFF/SNAP/TRUNC(同步方式)
  - **LearnerType**：PARTICIPANT/OBSERVER
  - **QuorumVerifier**(接口)/`QuorumMaj`(多数)/`QuorumOracleMaj`(神谕)/`QuorumHierarchical`(层次)
- **对比取舍**：**可插拔仲裁校验**——多数/神谕/层次三种 QuorumVerifier 策略
- **测试佐证**：源码 `QuorumPeer.java`(ZabState 538-541/SyncMode 549-552) + `flexible/`(QuorumVerifier/QuorumMaj/QuorumOracleMaj/QuorumHierarchical)

### KP-07 请求处理器链（ZooKeeperServer.setupRequestProcessors）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：请求处理
- **来源**：docs §ZooKeeperServer + 源码验证
- **需求**：用 RequestProcessor 链处理事务
- **自主实现**：若我设计——链接多个处理器按序处理请求
- **参考实现**（docs + 源码）：`ZooKeeperServer.setupRequestProcessors`——`FinalRequestProcessor`(最终) ← `SyncRequestProcessor`(同步磁盘) ← `PrepRequestProcessor`(预处理器，`firstProcessor`)；RequestProcessor 按顺序处理，请求经 `processRequest` 前移
- **对比取舍**：**链式处理**——Prep→Sync→Final 依次处理，可替换不同链(Leader/Follower/Observer)
- **测试佐证**：源码 `server/ZooKeeperServer.java`(setupRequestProcessors)+`server/RequestProcessor.java`

### KP-08 Leader 请求处理器链（LeaderZooKeeperServer）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07
- **来源**：docs §抽象仲裁服务器·Leader 实现 + 源码验证
- **需求**：Leader 特有请求处理链（含提案/提交）
- **自主实现**：若我设计——Leader 链加 ProposalRequestProcessor/CommitProcessor
- **参考实现**（docs + 源码）：`LeaderZooKeeperServer` 替换请求处理器：`PrepRequestProcessor → ProposalRequestProcessor → CommitProcessor → Leader.ToBeAppliedRequestProcessor → FinalRequestProcessor`；`QuorumZooKeeperServer`(仲裁服务器抽象基类)
- **对比取舍**：**Leader 链最复杂**——加入提案(Proposal)/提交(Commit)/待应用(ToBeApplied)处理器，实现共识
- **测试佐证**：源码 `server/quorum/LeaderZooKeeperServer.java`(setupRequestProcessors)

### KP-09 Follower/Observer 请求处理器链
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07
- **来源**：docs §抽象仲裁服务器·Follower/Observer + 源码验证
- **需求**：Follower/Observer 特有请求处理链
- **自主实现**：若我设计——Follower 处理请求+同步日志；Observer 类似 Follower 但无 ack
- **参考实现**（docs + 源码）：
  - **FollowerZooKeeperServer**：`FollowerRequestProcessor → CommitProcessor → FinalRequestProcessor` + `SyncRequestProcessor`(记录 Leader 提案，next=SendAckRequestProcessor)
  - **ObserverZooKeeperServer**：`ObserverRequestProcessor → CommitProcessor → FinalRequestProcessor`，Sync 到磁盘(写 txnlog，可关 syncRequestProcessorEnabled)
  - **LearnerZooKeeperServer**：Learner 父类
- **对比取舍**：**Follower 同步+ack、Observer 无 ack**——Follower 记录提案并 ack，Observer 只写已提交 txn
- **测试佐证**：源码 `server/quorum/FollowerZooKeeperServer.java`/`ObserverZooKeeperServer.java`/`LearnerZooKeeperServer.java`

### KP-10 FastLeaderElection（TCP 选举）
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]`（当前默认） | **置信度**：High
- **前置**：KP-02/04
- **来源**：docs §快速 Leader 选举 + 源码验证
- **需求**：用 TCP 实现高效 Leader 选举
- **自主实现**：若我设计——基于推送的选举，用 QuorumCnxManager 管理连接
- **参考实现**（docs + 源码）：`FastLeaderElection` 用 TCP 实现，用 `QuorumCnxManager` 管理连接；基于推送；`finalizeWait` 决定等待时间；`Notification`(通知消息，含 leader/zxid/electionEpoch/state/sid/qv/peerEpoch)、`ToSend`(待发消息)、`Messenger`(WorkReceiver/WorkSender 多线程)
- **对比取舍**：**TCP 连接管理**——QuorumCnxManager 每对服务器一连接，平局打破机制
- **测试佐证**：源码 `FastLeaderElection.java`(Notification/ToSend/Messenger)+`QuorumCnxManager.java`

### KP-11 选举主流程 lookForLeader
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-10
- **来源**：docs §lookForLeader + 源码验证
- **需求**：实现 Leader 选举主循环
- **自主实现**：若我设计——注册 JMX→初始化变量→更新逻辑时钟→通知交换直到找到 Leader
- **参考实现**（docs + 源码）：`FastLeaderElection.lookForLeader` 流程——
  1. 注册 JMX 服务器
  2. 初始化变量：`recvset`(当前选举选票，electionEpoch==logicalclock)/`outofelection`(历史+当前非 LOOKING 选票)/`notTimeout`(超时，默认 200ms=minNotificationInterval)
  3. 更新逻辑时钟 + `updateProposal` + `sendNotifications`(向所有 voters 发 LOOKING 通知)
  4. `while(LOOKING)` 通知交换——`recvqueue.poll(notTimeout)` 两条分支：
  - **分支一（超时未收到选票，n==null）**：`manager.haveDelivered()` 已送达则重发 sendNotifications，否则 `manager.connectAll()` 连接；**指数退避** `notTimeout = min(notTimeout<<1, maxNotificationInterval=60000)`；若 `QuorumOracleMaj`(神谕)且 `revalidateVoteset` 则直接选为 Leader(`leaveInstance`)——处理 2 实例单点无法选主的场景
  - **分支二（收到合法投票 `validVoter(n.sid) && validVoter(n.leader)`）**，按 `n.state` 处理，其中 **LOOKING 状态三情况**：
    - `n.electionEpoch > logicalclock`：外部纪元更高→更新 logicalclock、清空 recvset、按 totalOrderPredicate 决定采用外部/本地 proposed 并 sendNotifications
    - `n.electionEpoch < logicalclock`：外部纪元更低→忽略(break)
    - `n.electionEpoch == logicalclock`：纪元相等→`totalOrderPredicate` 判定外部是否优先，是则 updateProposal+sendNotifications
- **对比取舍**：**recvset/outofelection 分离**——当前轮选票 vs 历史选票判断多数/迟到；**指数退避 + 神谕重验**处理超时与单点场景
- **测试佐证**：源码 `FastLeaderElection.lookForLeader`/`sendNotifications`/`updateProposal`/`totalOrderPredicate`

### KP-12 选票全序比较 totalOrderPredicate
- **维度**：`[分布式理论]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-03
- **来源**：docs §选票全序比较 + 源码验证
- **需求**：定义选票的全局顺序判定
- **自主实现**：若我设计——epoch→zxid→sid 逐级比较
- **参考实现**（docs + 源码）：`totalOrderPredicate(newId,newZxid,newEpoch,curId,curZxid,curEpoch)`——三个条件任一成立返回 true：
  - `newEpoch > curEpoch`(新纪元更高)
  - `newEpoch == curEpoch && newZxid > curZxid`(zxid 更高)
  - `newEpoch == curEpoch && newZxid == curZxid && newId > curId`(sid 更高)
  - 权重为 0 直接返回 false
- **对比取舍**：**Epoch→Zxid→SID 全序**——三级比较是选举核心(对应 KP-03 变更规则的形式化)
- **测试佐证**：源码 `FastLeaderElection.totalOrderPredicate`

### KP-13 仲裁连接管理器（QuorumCnxManager）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：网络
- **来源**：docs §仲裁连接管理器 + 源码验证
- **需求**：管理选举节点间的 TCP 连接
- **自主实现**：若我设计——每对服务器一连接，维护消息队列
- **参考实现**（docs + 源码）：`QuorumCnxManager` 用 TCP 实现选举连接管理器；每对服务器一连接；**平局打破机制**——两服务器同时连则按 IP 决定断开哪个；每对等点维护发送消息队列，断连重放
- **对比取舍**：**一对一连接 + 平局打破**——保证每对正确连接，IP 仲裁
- **测试佐证**：源码 `QuorumCnxManager.java`

### KP-14 RequestProcessor 链详解（Proposal/Sync/Commit/Ack/Final）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-07
- **来源**：docs §请求处理器 + 源码验证
- **需求**：理解各 RequestProcessor 职责
- **自主实现**：若我设计——预处理/提案/同步/提交/确认/最终各司其职
- **参考实现**（docs + 源码）：
  - **PrepRequestProcessor**：预处理器
  - **ProposalRequestProcessor**：转发到 Ack/SyncRequestProcessor，`zks.getLeader().propose(request)` 提案
  - **SyncRequestProcessor**：记录到磁盘，批处理；Leader/Follower/Observer 三种用法
  - **CommitProcessor**：匹配已提交请求，多线程，保证写按 zxid 顺序、会话内顺序
  - **AckRequestProcessor**：Leader 本地 ack
  - **SendAckRequestProcessor**：Follower ack 发 leader
  - **Leader.ToBeAppliedRequestProcessor**：维护 toBeApplied 列表
  - **FinalRequestProcessor**：应用事务/查询，链末尾
  - **Leader.processAck**：处理 ack，`outstandingProposals` + `tryToCommit`(1047 行)
- **对比取舍**：**完整共识管道**——提案→同步→ack→提交→应用，实现 ZAB 广播
- **测试佐证**：源码 `server/quorum/ProposalRequestProcessor.java`/`CommitProcessor.java`/`AckRequestProcessor.java`/`SendAckRequestProcessor.java`/`Leader.java`(processAck 1047)/`server/SyncRequestProcessor.java`/`FinalRequestProcessor.java`

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 选举算法概述 | 分布式理论 | 核心 | P1 | 🟡 | 有效 | High |
| 术语(SID/ZXID/Vote/Quorum) | 分布式理论 | 核心 | P1 | 🔴 | 时间无关 | High |
| 变更投票规则 | 分布式理论 | 核心 | P1 | 🔴 | 时间无关 | High |
| 服务器状态 | 分布式理论 | 核心 | P1 | 🟡 | 时间无关 | High |
| 选票 Vote | 分布式理论 | 核心 | P1 | 🟡 | 时间无关 | High |
| QuorumPeer + QuorumVerifier | 分布式理论 | 核心 | P1 | 🟡 | 时间无关 | High |
| 请求处理器链(基础) | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Leader 请求链 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| Follower/Observer 请求链 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |
| FastLeaderElection | 分布式理论 | 核心 | P1 | 🔴 | 有效 | High |
| lookForLeader 主流程 | 分布式理论 | 核心 | P1 | 🔴 | 时间无关 | High |
| totalOrderPredicate | 分布式理论 | 核心 | P1 | 🔴 | 时间无关 | High |
| QuorumCnxManager | 工程问题 | 核心 | P1 | 🟡 | 时间无关 | High |
| RequestProcessor 详解 | 工程问题 | 核心 | P1 | 🔴 | 时间无关 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：`code/spring/zookeeper`——FastLeaderElection/Vote/QuorumPeer/QuorumCnxManager/QuorumVerifier 族/Leader/Follower/Observer/各 RequestProcessor 全部验证
- **关键源码类**（本次实证）：`QuorumPeer.ServerState`(LOOKING 527/FOLLOWING 528/LEADING 529/OBSERVING 530)、`ZabState`(ELECTION 538/DISCOVERY 539/SYNCHRONIZATION 540/BROADCAST 541)、`SyncMode`(NONE 549/DIFF 550/SNAP 551/TRUNC 552)、`flexible/`(QuorumVerifier/QuorumMaj/QuorumOracleMaj/QuorumHierarchical)、`Leader.processAck`(1047)
- **关联标注**：microsphere 用 zookeeper 做一致性 `[待验证]`；衔接 ZAB(第 4 节)、数据模型(第 9 节)

---

## 五、本节小结（三层次视角）

**需求**：实现 ZK 共识——Leader 选举（SID/ZXID/Vote/Quorum/变更规则/FastLeaderElection）+ 请求处理链。

**自主实现核心**：若我设计——
1. 术语：SID/ZXID/Vote/Quorum(n/2+1)
2. 变更投票规则：zxid 优先、sid 次之
3. ServerState 四态(含 Observer)
4. FastLeaderElection：TCP + recvset/outofelection + totalOrderPredicate(epoch→zxid→sid)
5. RequestProcessor 链：Leader(Prep→Proposal→Commit→ToBeApplied→Final)/Follower/Observer
6. QuorumCnxManager 连接管理 + Leader.processAck 提交

**参考实现**：ZooKeeper 源码（`code/spring/zookeeper` 完整验证）+ docs。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**ZooKeeper 共识算法实现**"。核心洞察：**Leader 选举(SID/ZXID/Quorum/变更规则/totalOrderPredicate)、FastLeaderElection(TCP)、RequestProcessor 链(提案/同步/提交/应用)**。为第 12 节共识运用铺垫。

**待验证汇总**：
- microsphere 用 zookeeper 的具体场景
- Leader 选举各分支完整时序

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为选举/请求链 + 源码片段 + 本地源码验证；补全聚焦"ZK 共识实现的工程价值"。

### 完整认知：ZK 共识实现在真实架构中完整该讲什么

docs 覆盖了选举术语/规则/FastLeaderElection/请求链。作为架构师，这个主题完整还该包含：

1. **Leader 选举是 ZK 高可用的核心**：无 Leader 时集群不可用(不可写)，选举保证多数可用时快速恢复——`quorum=n/2+1` 是可用性阈值
2. **ZXID 决定选谁**：选票先比 zxid(哪个节点数据最新)再比 sid——新 Leader 必有最新数据，减少同步量
3. **变更规则 = totalOrderPredicate 的直观版**：KP-03 四规则是 totalOrderPredicate(epoch→zxid→sid) 的简化和抽象，理解其一即懂其二
4. **RequestProcessor 链 = 共识管道**：从 Prep(校验)→Proposal(提案)→Sync(落盘)→Ack(确认)→Commit(提交)→ToBeApplied→Final(应用)——完整复现 ZAB 广播，是理解 ZK 写路径的钥匙
5. **OBSERVING 只读扩展**：Observer 不参与选举/不写，只读扩展——读扩展能力(呼应第 9 节读写不对称)
6. **Leader 选举 vs Raft 选举对比**：ZK 用 zxid+sid 排序，Raft 用 term+log 比较——思想等价、实现细节不同
7. **与 ZAB 三阶段呼应**：ZabState(ELECTION/DISCOVERY/SYNCHRONIZATION/BROADCAST) 即第 4 节三阶段 + 选举

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| TCP vs UDP 选举 | TCP 可靠；UDP 快但不可靠(废弃) |
| zxid 优先 vs sid 优先 | zxid 选最新数据；sid 作平局裁决 |
| Observer vs Participant | Observer 只读扩展；Participant 参与选举 |
| QuorumVerifier 策略 | 多数/神谕/层次——按部署选 |
| RequestProcessor 链 | 链式解耦；节点类型链不同 |

### 常见坑/反模式

1. **Quorum 误解**：quorum=n/2+1 是可用阈值，非所有节点
2. **SID/ZXID 混淆**：SID 标识机器、ZXID 标识事务——决定选举比较
3. **忽略 Observer 语义**：Observer 不参与选举，只读扩展
4. **请求链乱序**：RequestProcessor 必须按序，写按 zxid 序——CommitProcessor 保证
5. **UDP 选举残留**：老版本 UDP 算法已废弃——用 TCP FastLeaderElection

### 生态位置

- **分布式理论维度**：ZK 共识实现是 **ZAB 的落地**——承接第 4 节 ZAB、第 9 节数据模型，为第 12 节运用铺垫
- **衔接**：ZAB(第 4 节) → 数据模型(第 9 节) → 通讯(第 10 节) → 共识实现(本篇) → 共识运用(第 12 节)
- **与源码提取的关系**：server/quorum 包是核心源码

**架构师视角结论**：本篇不只是背选举规则，而是"**理解 ZK 如何用共识实现高可用**"——Leader 选举(zxid+sid 排序、FastLeaderElection)、RequestProcessor 共识管道(提案/同步/提交/应用)；这是 ZK 作为协调服务一致性地基，也呼应你关心的"集群内数据同步(Leader↔Follower)"。
