# ZooKeeper 源码学习范围规划

> **版本**: release-3.9.5
> **仓库**: `/data/workspace/source-code/code/spring/zookeeper/`
> **规模**: 387 Java 文件(zookeeper-server) + 17 client/contrib 子模块
> **日期**: 2026-08-04
> **R1**: 探索 387 文件 Quorum/DataTree/Session/Watcher — 6🔴+3🟡=9 域
> **R2**: 深度读 FastLeaderElection:logicalclock+recvset/outofelection+ZOOKEEPER-3922；Leader:outstandingProposals+tryToCommit+reconfig级联；SessionTrackerImpl:ExpiryQueue分桶+三态模型——Z-1/Z-2/Z-5 全面增强
> **R3**: PrepRequestProcessor 10+OpCode+multi-op+closeSession；CommitProcessor读写分类+worker pool+线性一致性；WatchManager双向索引+WatchStats位掩码+PathParentIterator——Z-4/Z-6增强
> **R4**: SyncRequestProcessor batched flush(ArrayDeque toFlush)+snapCount/2+randRoll去同步快照+Semaphore+读透传优化；LearnerHandler SNAP/DIFF/TRUNC+BinaryProtocol+Sender线程；Follower PROPOSAL(deserializeTxn→logRequest→ACK)+COMMIT(fzk.commit)→Z-4/Z-2增强 — 全部6核心域方法体覆盖完成

---

## 一、仓库概况

ZooKeeper 是 Apache 分布式协调服务，核心是 **ZAB 协议（ZooKeeper Atomic Broadcast）**——通过 Leader 选举 + 原子广播 + 两阶段提交保证写操作的顺序一致性。

**Maven 模块**：

| 模块 | 文件数 | 职责 | 纳入 |
|:---:|:---:|---|---|
| `zookeeper-server/` | 387 | ZK Server 核心——Quorum/ZAB/DataTree/Session/Watcher | ✅ |
| `zookeeper-jute/` | — | Jute 序列化框架(IDL 生成) | 融入 |
| `zookeeper-client/` | — | ZK Client API(ZooKeeper/Watcher) | 🟡 |
| `zookeeper-recipes/` | — | 分布式锁/选主/队列等 Recipes | 🟡 |
| `zookeeper-contrib/` | — | 贡献代码(废弃) | 淘汰 |
| `zookeeper-it/` `zookeeper-compatibility-tests/` | — | 集成测试 | 淘汰 |

**Server 核心包**：

| 包 | 文件数 | 职责 | 纳入 |
|:---:|:---:|---|---|
| `server/quorum/` | 61 | QuorumPeer、Leader/Follower、FastLeaderElection、LearnerHandler | ✅ 核心 |
| `server/` (根) | 67 | ZooKeeperServer、ZKDatabase、DataTree、SessionTracker、RequestProcessor 链 | ✅ 核心 |
| `server/watch/` | 13 | WatcherManager、WatchManager | ✅ 核心 |
| `server/persistence/` | 10 | FileTxnLog、FileSnap——事务日志+快照持久化 | ✅ |
| `server/command/` | 19 | 4 字母命令(ruok/stat/mntr/conf) | 淘汰 |
| `server/auth/` | 13 | 认证/ACL | 淘汰 |
| `server/controller/` | 8 | Admin 控制器 | 淘汰 |
| `server/admin/` `jmx/` `metrics/` | — | 管理/JMX/指标 | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（6 个—ZAB + DataTree + Session + Watcher）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| Z-1 | **ZAB—Leader 选举** | QuorumPeer(2711行), FastLeaderElection(1225行), Vote, QuorumCnxManager | **FastLeaderElection 完整算法(R2 源码验证)**：`logicalclock`(AtomicLong)——election epoch计数器，每轮选举自增1。**双 Map 结构**：①`recvset`(HashMap<Long,Vote>)——当前选举轮次(epoch==logicalclock)的投票 ②`outofelection`——之前轮次或 FOLLOWER/LEADER 状态的其他节点投票(用于迟到的参与者快速发现 Leader)。**Vote 比较规则 `totalOrderPredicate(newEpoch,newZxid,newId vs cur)`**：`epoch大者赢 > epoch相等则zxid大者赢 > epoch+zxid相等则id大者赢`。**消息循环**：`recvqueue`(LinkedBlockingQueue) 接收 Notification → LOOKING:比较投票→更新自己的投票→`sendNotifications()`广播；FOLLOWING/LEADING(ZOOKEEPER-3922 分离2节点支持) → `outofelection` 记录。**Quorum 判定**：`voteSet.hasAllQuorums()` → 多数派达成 → 排空 `recvqueue` 检查是否有更优 leader → 无更优→`leaveInstance(endVote)` 返回。**QuorumCnxManager**：TCP 点对点投票传输——`senderWorkerMap` + 每个 peer 的 `BlockingQueue<ByteBuffer>` |
| Z-2 | **ZAB—原子广播(BROADCAST)** | Leader(1817行), Follower, LearnerHandler, Proposal | **Leader.lead() DISCOVERY→BROADCAST(R2 源码验证)**：`getEpochToPropose()` 确定新 epoch → `ZxidUtils.makeZxid(epoch, 0)` 设置 zxid → `NEWLEADER` Proposal → `waitForNewLeaderAck()` 等待 quorum ACK → `startZkServer()` 启动请求处理。**两阶段提交 `processAck(sid, zxid)`**：①找到 `outstandingProposals`(LinkedHashMap<zxid, Proposal>) 中对应 zxid 的 Proposal → `p.addAck(sid)` 记录 ACK → `tryToCommit(p, zxid)` 检查。**`tryToCommit()` 提交条件**：①前序 proposal 必须已提交(`outstandingProposals.containsKey(zxid-1)` → false) ②所有 quorum version 都收集到 majority(`p.hasAllQuorums()`) ③zxid 严格顺序提交(`zxid == lastCommitted+1`)。**`Proposal extends SyncedLearnerTracker`**：`qvAcksetPairs` 支持多版本 quorum(reconfig 期间新旧配置共存的 ACK 收集)。**Reconfig 级联提交**：reconfig 提交后可能解锁后续 ops——`while(hasCommitted && p!=null)` 连续 tryToCommit。**Leader 重指派**：`getDesignatedLeader()`(leader离开配置时) 选出 ACK 最多新 config ops 的 follower 作为新 Leader
| Z-3 | **DataTree—内存数据树** | DataTree, DataNode, ZKDatabase(806行), NodeHashMapImpl | **In-Memory 树结构**：`DataTree` 维护 `/path` → `DataNode` 的映射，使用 `NodeHashMap`(ConcurrentHashMap 实现)。**DataNode** 含 `data(byte[])` + `acl` + `stat(PersistedStat含 czxid/mzxid/ctime/mtime/version/cversion/aversion/ephemeralOwner/dataLength/numChildren/pzxid)`。**ZKDatabase**：整合 `DataTree`(内存) + `FileTxnLog`(事务日志) + `FileSnap`(快照)。**事务日志**：每个事务(pkt/crt/del/setData)以 `TxnHeader(zxid+type+time)` + `Record` 写入 `FileTxnLog`(预写日志) → `SyncRequestProcessor` flush 到磁盘。**快照**：`ZKDatabase.takeSnapshot()` → 序列化整个 DataTree 到 `.snapshot` 文件 → 同时记录 `lastProcessedZxid`。**恢复**：从最近 snapshot 加载 + 从事务日志回放 snapshot 之后的增量 |
| Z-4 | **Request Processor—请求处理器链** | PrepRequestProcessor(1122行), CommitProcessor(644行), SyncRequestProcessor, FinalRequestProcessor | **PrepRequestProcessor(R3 源码验证)**：`pRequestHelper()` switch 10+ OpCode(create/create2/createTTL/createContainer/delete/deleteContainer/setData/setACL/check/reconfig/multi/closeSession)——每个执行：session验证+ACL检查+版本递增+构造Txn+记录ChangeRecord。**multi-op**：所有子操作共享同一 zxid → 遍历每个 Op → 遇失败后续全部 `ErrorTxn(RUNTIMEINCONSISTENCY)` → `pendingChanges` 支持回滚。**closeSession**：`synchronized(outstandingChanges)` 获取 ephemeral nodes → 扣除已在 outstandingChanges 中被删的 node → 生成 `DeleteTxn` 列表 → `CloseSessionTxn`。**CommitProcessor(Leader端)**：`queuedRequests`(LinkedBlockingQueue) 接收所有请求 → `needCommit()` 分类：写请求进 `queuedWriteRequests`(LinkedList) 等待 commit，读请求直接处理。`commit(request)` → `committedRequests.add()` → `wakeup()` 主线程 → 顺序匹配 committed 与 queued write → `nextProcessor.processRequest()`。**Worker 线程池**(numWorkerThreads=CPU核数)并行处理读请求。**线性一致性**：写请求必须等待 Leader commit 后才 apply——保证读看到最新已提交数据
| Z-5 | **Session—会话管理** | SessionTrackerImpl, SessionImpl, ExpiryQueue, SessionExpirer | **Session 生命周期(R2 源码验证)**：Client connect → `nextSessionId.getAndIncrement()` 分配 sessionId → `trackSession(id, timeout)` → `sessionsWithTimeout`(ConcurrentMap<Long,Integer>) 存储。**分桶过期机制**：`sessionExpiryQueue`(ExpiryQueue<SessionImpl>, 按 tickTime=2000ms 分桶) → `touchSession(sessionId, timeout)` 每次请求调用 → `expiryQueue.update(session, timeout)` 将 session 移到 `(now+timeout)` 所在桶。**过期主循环**(`run()`)：`expiryQueue.getWaitTime()` → sleep → `expiryQueue.poll()`(取当前时间桶内所有 session) → `setSessionClosing(sessionId)` → `expirer.expire(s)` → ZooKeeperServer **killSession**：删除 EphemeralNode + 通知 Watcher。**Session 三态模型**：①**active**——owner 已绑定，可响应请求 ②**closing**(isClosing=true)——正在过期清理中 ③**expired**——已从 sessionsById 移除。**Session 迁移**：Leader 变更 → `ZooKeeperServer.createSessionTracker()` 从 snapshot 恢复 `sessionsWithTimeout` → 所有 session 重新加入 `expiryQueue`
| Z-6 | **Watcher—事件监听** | WatchManager(376行), WatcherModeManager, WatchStats, PathParentIterator | **双向索引结构(R3 源码验证)**：①`watchTable`(HashMap<String,Set<Watcher>>) path→watchers ②`watch2Paths`(HashMap<Watcher,Map<String,WatchStats>>) watcher→path→stats。**WatchStats 模式位掩码**：`STANDARD`(一次性，触发后自动移除)/`PERSISTENT`(持久，触发后保留)/`PERSISTENT_RECURSIVE`(递归，子节点变更也触发)。**`triggerWatch(path, type)`**：`PathParentIterator` 从 path 遍历到 `/` → 收集每层 watcher → STANDARD 模式从 table 移除(一次性触发) → PERSISTENT 保留 → 构建 `WatchedEvent(type, SyncConnected, path)` → `ServerCnxn.process(event)` 发送通知。**`recursiveWatchQty` 优化**：无 PERSISTENT_RECURSIVE watcher 时 `PathParentIterator.forPathOnly()` 仅检查当前 path 不遍历父路径。**死连接清理**：`isDeadWatcher(watcher)` 过滤已关闭连接的 watcher → `removeWatcher()` 同步清理 `watchTable` + `watch2Paths`

### 🟡 扩展域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| Z-7 | **ZK Client API** | ZooKeeper, ClientCnxn, ClientWatchManager | **ClientCnxn**：客户端与服务端的 NIO 连接——`Packet` 封装(request+response+watch)→`SendThread`(写请求)+`EventThread`(处理事件)。**会话迁移**：连接断开后自动重连(新 Server) → `sessionId+password` 恢复会话。**连接状态**：`CONNECTING → CONNECTED → CLOSED` + `AUTH_FAILED`/`SASL_AUTHENTICATED` |
| Z-8 | **Distributed Recipes** | LeaderSelector, DistributedLock, DistributedQueue | **zookeeper-recipes/** 模块——基于 ZK 原语实现的分布式组件：①**Leader Election**(LeaderSelector, LeaderLatch) ②**Distributed Lock**(InterProcessMutex, InterProcessReadWriteLock) ③**Distributed Queue**(DistributedQueue, DistributedPriorityQueue) ④**Distributed Barrier**(DistributedBarrier, DistributedDoubleBarrier) ⑤**Service Discovery**(ServiceDiscovery, ServiceProvider)。这些 Recipes 展示了如何用 ZK 构建分布式系统——Ephemeral Sequential Node + Watcher 的经典组合 |
| Z-9 | **FileTxnLog & Snapshot** | FileTxnLog, FileTxnSnapLog, FileSnap, Util | **事务日志格式**：`log.{zxid}` 文件——每条记录 `[checksum:8B][length:4B][TxnHeader+Record bytes]` → `FilePadding` 预分配空间减少碎片。**Snapshot 格式**：`snapshot.{zxid}` 文件——序列化 `DataTree` + `sessionsWithTimeouts` → `FileSnap.serialize()` → checksum + gzip 可选压缩。**PurgeTxnLog**：`autopurge.snapRetainCount=3` + `autopurge.purgeInterval=1h`——定期删除旧日志和快照 |

---

## 三、淘汰清单

| 模块/包 | 理由 |
|:---:|---|
| `zookeeper-contrib/` | 社区废弃代码(Hedwig/Loggraph/BookKeeper 旧版) |
| `zookeeper-compatibility-tests/` `zookeeper-it/` | 集成测试 |
| `server/command/` | 4 字母管理命令(ruok/stat)——运维工具 |
| `server/auth/` `server/jmx/` | 认证/JMX——安全/运维 |
| `server/controller/` | Admin controller HTTP 接口 |
| `server/admin/` `server/metrics/` | 管理/metrics |
| `zookeeper-assembly/` `zookeeper-docs/` | 打包/文档 |
| `server/NettyServerCnxn/` `server/NIOServerCnxn/` | 网络层(Netty/NIO)——中间件 |

---

## 四、统计

| 类别 | 数量 |
|:---:|:---:|
| 🔴 核心域 | 6 |
| 🟡 扩展域 | 3 |
| **总域** | **9** |

## 五、关键架构关系

```
客户端: ZooKeeper API → ClientCnxn(NIO) → ServerCnxn → RequestProcessor 链

写路径(Leader): PrepRequestProcessor → ProposalRequestProcessor → SyncRequestProcessor
    → AckRequestProcessor(collected ACK) → CommitProcessor(wait majority)
    → FinalRequestProcessor(apply to DataTree) → WatchManager.trigger()

Leader选举: QuorumPeer.run() → LOOKING → FastLeaderElection.lookForLeader()
    → Vote(myid, zxid, epoch) → QuorumCnxManager(TCP) → collect majority → LEADING

数据持久化: FileTxnLog(每条事务 append+flush) + FileSnap(每100000事务 snapshot)
    恢复: load snapshot + replay txn log from snapshot zxid

会话: SessionTrackerImpl.run() → 分桶过期(tickTime=2s) → expire(sessionId)
    → killSession(ephemeral nodes) + close(connections) + triggerWatch
```

## 六、阅读顺序

```
Z-3 DataTree → Z-2 ZAB广播 → Z-1 Leader选举 → Z-4 Processor链
→ Z-5 Session → Z-6 Watcher → Z-7 Client → Z-8 Recipes → Z-9 持久化
```

数据层(DataTree) → 协议层(ZAB) → 处理层(Processor) → 会话/监听(Session/Watcher) → 客户端/Recipes
