# Z-1 Leader 选举 — FastLeaderElection 与投票裁决

> 前置: [[K-9-KRaft]] (Kafka 元数据选举对照) + [[RM-12-HA]] (RocketMQ Controller 选举对照) | 引出: [[Z-2-原子广播]] | 对照: sofa-jraft (4.6, Raft 选举 vs ZAB)
> 🔴 A | 8 KP | [模式: 投票共识 + 全序比较 + 稳定窗口]
> Pass 2 闭环: q1(投票结构) q2(提议广播) q3(收票判定) q4(双集合语义)

**读者处境**: 集群里多台机器同时宕机/启动, 谁当 leader? 数据最新者优先怎么保证? 这篇拆 FastLeaderElection: 投票四要素 (epoch/zxid/sid/weight) + 双集合 (recvset/outofelection) + 稳定窗口 + 指数退避。

### 1. 投票结构 — Notification/ToSend/Vote

场景: 选票长什么样?
源码路径:
- **Notification** (FastLeaderElection:112-145): leader/zxid/electionEpoch/state/sid/peerEpoch/qv — 通知消息 (别人投票广播); **CURRENTVERSION=0x2** (L117)
- **ToSend** (L154-202): 发送队列元素 — mType 四类 (crequest/challenge/notification/ack); configData (reconfig 面)
- **Vote**: 本地选票 — (leader, zxid, electionEpoch, peerEpoch)
- **双队列**: sendqueue + recvqueue (LinkedBlockingQueue) — Messenger 双线程 (WorkerSender/WorkerReceiver)
- **协议兼容**: 28B (ZK-107 前) / 40B (peerEpoch+version 后) — WorkerReceiver 按 capacity 区分 (L245-257)
关键设计 (q1): **投票 = (leader, zxid, electionEpoch, peerEpoch) 四元组**; epoch 区分选举轮次, zxid 带数据新鲜度。[模式: 投票消息]

### 2. 提议广播 — logicalclock + updateProposal + sendNotifications

场景: 选举开始怎么发起?
源码路径:
- **lookForLeader 起点** (L880-908): **logicalclock.incrementAndGet()** (选举轮++) → updateProposal(initId, initZxid, peerEpoch) (初始投自己) → sendNotifications
- **sendNotifications** (L509+): 遍历 QuorumVerifier 成员 → ToSend 入 sendqueue (WorkerSender 异步发)
- **指数退避**: recvqueue.poll(notTimeout) null → `manager.haveDelivered() ? sendNotifications() : connectAll()` (**有投递连接重发/无先建连接 — 连接面耦合**) + `notTimeout = min(notTimeout << 1, 60000)` (L957-978) — min=200ms (finalizeWait) → max=60s **共 10 档**; **退避 = 连续无票计数器** (仅超时分支递增, 收票不重置 ⚠)
- **权重排除**: totalOrderPredicate 开头 weight==0 → false (L732-734) — 3.4.10+ 加权投票
关键设计 (q2): **轮次递增 + 自荐 + 全员广播**; 收不到回执指数退避重发 (200ms→60s)。[模式: 自荐广播]

### 3. 收票判定 — totalOrderPredicate + hasAllQuorums + 稳定窗口

场景: 收到票怎么比?
源码路径:
- **totalOrderPredicate** (L723-749): **peerEpoch 高者胜 → zxid 高者胜 → sid 高者胜** (L744-748) — 全序三要素
- **LOOKING 通知处理** (L966-1067): n.electionEpoch > logicalclock → **清 recvset + 重算提议** (对方轮次更新, 自己落后 → 换轮!) / < → 忽略 (旧轮票) / == → totalOrderPredicate 比较 → 更新提议 + 广播
- **收票**: recvset.put → **getVoteTracker (双 QuorumVerifier — reconfig 兼容, L761-767)** → **hasAllQuorums()**
- **finalizeWait=200ms 稳定窗口** (L1050-1055): 达成多数后仍 poll 200ms — 有更高票 → 放回重来 (防瞬间当选后又变); n==null → **setPeerState + leaveInstance** 当选
- **validVoter 守卫** (L958): 仅当前/下一视图成员 (reconfig)
关键设计 (q3): **三要素全序 (epoch>zxid>sid) 保证确定性**; 稳定窗口防抖动; 换轮清集合。[模式: 全序裁决]

### 4. 双集合语义 — recvset vs outofelection

场景: 迟到的参与者怎么快速跟上?
源码路径:
- **recvset**: 当前轮投票 (electionEpoch == logicalclock) — 判定多数用 (L849-855 注释)
- **outofelection**: 历史轮 + FOLLOWING/LEADING 通知 — **迟到者学习已存在 leader** (L855-862 注释: 晚到 = 更高 logicalclock)
- **receivedFollowingNotification** (L1146-1164): 同 epoch → recvset + hasAllQuorums + **checkLeader** → 当选; 异 epoch → **outofelection 路径验证多数跟随同一 leader** (L1163-1164 注释)
- **FOLLOWING/LEADING 分离** (ZOOKEEPER-3922, L1073-1118): 2 节点配置 majority=2 → 恢复节点无法达成 → **QuorumOracleMaj 裁决** (**接口可插拔扩展点**; Oracle 只允许一台维持进度; LEADING 通知 + Oracle 拒绝 = 该 leader 有效 — 单向授权)
- **OBSERVING 忽略** (L1069-1071): observer 不投票
关键设计 (q4): **双集合分离"当前轮裁决"与"历史学习"**; 2 节点 Oracle 例外 (ZK 不推荐 2 节点, 但兼容)。[模式: 迟到学习]

### 负面空间 — ZK 选举刻意不做的事

- **不做 Raft 式 term 持久化**: logicalclock 仅内存轮次 (重启归零 — 靠 zxid 兜底)
- **不做随机超时竞选**: 全员同时自荐广播 (对照 Raft 随机 election timeout) — 网络风暴面
- **不做 prevote**: 无预投票阶段 (对照 jraft)
- **不做租约**: 无 leader 租期, 心跳 (Learner ping) 驱动
- **不做脑裂绝对防护**: 靠多数 + Oracle (2 节点) — 分区即不可用 (CP)
- **不做成员自动变更**: reconfig 人工触发 (动态 reconfig 3.5+)

→ 引出: 选出的 leader 怎么广播数据? → [[Z-2-原子广播]]
