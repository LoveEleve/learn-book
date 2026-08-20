# R-14a Sentinel 监控 — 三角结构与判活

> 前置: [[R-28-networking]] (异步连接) + [[R-20-server]] (命令表) + [[R-2-events]] (事件循环) | 引出: [[R-14b-sentinel]] (故障转移) | 对照: [[R-15-cluster]] (gossip vs 投票)
> 🔴 A (拆篇 1/2) | 6 KP | [模式: 三角监控 + 周期命令 + 双线判活 + 信息共享]
> Pass 2 闭环: q1(三角结构) q2(周期命令) q3(SDOWN) q4(ODOWN) q5(信息共享) q6(TILT)

**读者处境**: Sentinel 怎么发现主从? SDOWN 和 ODOWN 什么区别? 哨兵之间怎么通信? 这篇拆监控: 三角实例结构、PING/INFO 周期、主观/客观下线、pub/sub 信息共享、TILT 保护。

### 1. 三角结构 — 主/从/哨兵实例

场景: Sentinel 怎么组织监控对象?
源码路径:
- **sentinelRedisInstance** (sentinel.c): 统一实例结构 — flags (SRI_MASTER/SRI_SLAVE/SRI_SENTINEL) + 地址 + runid + 链接
- **master->slaves / master->sentinels 双字典** (sentinel.c, sentinelRedisInstanceLookup): 主实例下挂从/哨兵
- **双链接** (instanceLink, sentinel.c:134): cc (命令链接) + pc (pub/sub 链接) — 分离职责
- 发现机制: INFO replication (主→从) + hello 消息 (哨兵互知)
- 解析: sentinelRedisInstanceLookupMaster/ByName (sentinel.c 查询)
关键设计 (q1): **以主为根的树**: 所有监控对象挂 master 实例下 — 递归处理 (sentinelHandleDictOfRedisInstances L5394-5416)。[模式: 三角监控]
数据流: 配置 master → INFO 发现从 → hello 发现哨兵 → 挂载。

### 2. 周期命令 — PING/INFO/PUBLISH

场景: Sentinel 多久问一次? 问什么?
源码路径:
- **sentinelSendPeriodicCommands** (sentinel.c:3095): PING (哨兵探活) + INFO (角色/复制信息) + PUBLISH hello (广播)
- **频率分级**: INFO 10s 正常 → 1s 主疑似下线后 (sentinel_info_period 缩放); PING 1s (sentinel_ping_period)
- 异步: sentinelReconnectInstance (L2377) 断线重连 + 回调 (sentinelInfoReplyCallback L2747 / PingReplyCallback L2772)
- 超时: down_after_period (配置, 默认 30s)
- sentinelFlushConfig (L2261): 配置自写持久化 (监控变化写回配置文件)
关键设计 (q2): **三类命令三职责**: PING 探活 / INFO 状态 / PUBLISH 广播 — 频率随状态动态 (正常 10s, 疑下线 1s)。[模式: 周期命令]
数据流: 每周期 → PING+INFO+HELLO → 回调更新实例状态。

### 3. SDOWN — 主观下线

场景: 单个哨兵怎么看实例挂了?
源码路径:
- **sentinelCheckSubjectivelyDown** (L4516-4582):
  - elapsed 计算 (L4519-4522): act_ping_time 或 disconnected 起算
  - **三条件** (L4561-4567): ① elapsed > down_after_period ② 主报告为从超过 down_after_period+2×info_period (角色突变) ③ 主重启 (master_reboot 特殊期)
  - 置 SRI_S_DOWN + s_down_since_time (L4570-4573) + **+sdown 事件**
  - 恢复: elapsed 归零 → 清 SRI_S_DOWN + **-sdown 事件** (L4577-4580)
- **双链接健康** (L4524-4553): 命令链接 ping 超时一半 / pubsub 链接 3×publish_period 无活动 → 重连
关键设计 (q3): **SDOWN = 本地视角**: 只代表当前哨兵的判断 — "主观"二字的语义 (可误判)。[模式: 主观判活]
数据流: 每周期 → elapsed 计算 → 超时? → SRI_S_DOWN + 事件。

### 4. ODOWN — 客观下线

场景: 多个哨兵一致才敢动?
源码路径:
- **sentinelCheckObjectivelyDown** (L4590-4623):
  - **quorum = 自己(1) + 其他哨兵 SRI_MASTER_DOWN 数** (L4595-4605)
  - `quorum >= master->quorum` → SRI_O_DOWN (L4606) + **+odown 事件** (带 quorum x/y)
  - 注释 (L4584-4589): **"ODOWN is a weak quorum"** — 消息延迟下无强一致保证
- 投票获取: sentinelAskMasterStateToOtherSentinels (L4670) → **SENTINEL is-master-down-by-addr** → sentinelReceiveIsMasterDownReply (L4627)
- 触发: sentinelHandleRedisInstance 主实例处理 (L5383-5389)
关键设计 (q4): **ODOWN = 弱 quorum 共识**: 自票+他票 ≥ 配置 quorum — 不是强一致但足够触发转移。[模式: 客观判活]
数据流: SDOWN → 问其他哨兵 → 票数 ≥ quorum → ODOWN。

### 5. 信息共享 — hello 广播

场景: 哨兵之间怎么互知主从状态?
源码路径:
- **sentinelSendHello** (L2996): PUBLISH `__sentinel__:hello` 频道 — 消息含 myid/地址/当前主 epoch/主地址
- **sentinelProcessHelloMessage** (L2838): 收 hello → 更新哨兵实例 (匹配 runid 或地址) → 主地址变化/epoch 更新
- **sentinelReceiveHelloMessages** (L2955): 订阅回调
- sentinelForceHelloUpdateDictOfRedisInstances (L3040): 强制刷新
- 用途: 哨兵自动发现 (无需配置所有哨兵) + 配置收敛
关键设计 (q5): **pub/sub 即发现协议**: 新哨兵上线只配一个已知节点, 其余靠 hello 自动发现 (对照 R-29 pubsub 机制复用)。[模式: 广播发现]
数据流: 每周期 → PUBLISH hello → 其他哨兵订阅 → 实例表更新。

### 6. TILT — 时钟保护

场景: 哨兵自己的时钟乱了怎么办?
源码路径:
- **sentinelCheckTiltCondition** (L5437-5447): 两次 timer 间 delta < 0 (时钟回退) 或 > **sentinel_tilt_trigger=2s** (L69, 被阻塞) → **SENTINEL_TILT 模式**
- TILT 语义 (L5418-5436 注释): **只收集不行动** — 暂停判活/转移, 等 **sentinel_tilt_period = PING×30 = 30s** (L70)
- 入口: sentinelHandleRedisInstance (L5368-5372): TILT 中直接 return
- 周期常量 (L62-83): PING=1s / INFO=10s / publish=2s / **down_after 默认 30s** (L68) / MAX_DESYNC=1000ms (L83)
关键设计 (q6): **时钟异常 = 判断失效**: 时钟跳变会让所有超时误判 — TILT 是哨兵自身的故障保险。[模式: 时钟保护]
数据流: timer → delta 异常 → TILT → 只收数据不判活 → 恢复。

### 负面空间 — 监控刻意不做的事

- **不做实时推送**: 状态靠周期拉取 (PING/INFO 轮询)
- **不做强一致判活**: ODOWN 是弱 quorum (注释实证)
- **不做哨兵间数据全同步**: 只有 hello 摘要 + is-master-down 投票
- **不做监控数据持久化**: 配置自写但运行时状态不落盘
- **不做多主监控**: 单主为主根 (哨兵配对主)

→ 引出: 判活之后怎么转移? → [[R-14b-sentinel]]
