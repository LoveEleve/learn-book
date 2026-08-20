# RD-1 篇3 — pubsub-dns: 懒连接单飞与 DNS 故障切换

> 前置: [[RD-1-篇2]] (连接池) + [[r22-expire]] | 复用: [[r20-server]] (serverCron 周期任务对照) | 对照: [[r2-events]] (io 线程轮询 vs HashedWheelTimer 定时) | 引出: [[rd2-rlock]] (订阅通道 + 看门狗续期) + [[rd6-localcachedmap]] (缓存失效订阅)
> 🔴 A | 2 KP (+订阅面) | [模式: 单飞锁 + 轮询切换 + 成功才提交]
> Pass 2 闭环: q2(lazyConnect 单飞) q4(DNS 切换) + 订阅服务 (ElementsSubscribeService 素材)

**读者处境**: lazyInitialization=true 时, 第一个命令触发连接 — 但 100 个线程同时发命令怎么办?连接线程自己又调一次 connect 会不会死锁?DNS 记录的 IP 变了谁发现、怎么切、切一半失败了怎么办?这篇拆懒连接的"单飞锁"协议和 DNSMonitor 的"轮询切换"协议 — 两个"并发正确性 + 高可用"的组合拳。

### 概念依赖链
q2(懒连接单飞) ← q4(DNS 切换) + 订阅面 — 先讲首次连接的并发正确性 (单飞锁), 再讲运行中拓扑变化的高可用 (轮询切换), 订阅器作为 RD-2 的通道交代。

### 核心悬念
"懒模式下 100 线程同时首连怎么保证只连一次、连接线程重入为什么不死锁？DNS 变了谁发现、怎么切、切一半失败怎么办？"

### 叙事顺序
1. 问题引入: 懒模式第一个命令进场, 连接只该发生一次
2. 单飞锁 (q2) — CAS latch 定所有者 + volatile 重入防护 + 失败重试
3. 轮询切换 (q4) — 自循环 + 多轮确认 + 成功才提交 + 失败回滚
4. 订阅面 — ElementsSubscribeService 边界 (RD-2 通道)
5. 收束: "高可用 = 并发单飞 + 拓扑切换" 组合拳, 引出锁域

### 1. lazyConnect — 单飞锁: 100 线程只连一次

场景: 懒模式下第一个命令进场, 连接怎么保证只触发一次?
源码路径:
- `MasterSlaveConnectionManager.lazyConnect` (MasterSlaveConnectionManager.java:190-227):
  - `private volatile Thread connectingThread` (L70, MasterSlaveConnectionManager.java) — **volatile 保证线程身份可见性**
  - L191-193: `isInitialized()` 快速路径 (已连好直接返回)
  - L195-199: **重入防护** — `Thread.currentThread() == connectingThread` → return (注释原文: "Re-entry by the connecting thread itself: return rather than join() the latch it holds, which would self-deadlock")
  - L201-213: `lazyConnectLatch.compareAndSet(null, newFuture)` — 首个线程 CAS 成功=获得连接所有权; 失败→读 currentFuture:
    - `isCompletedExceptionally()` → CAS 替换 (L204-208, MasterSlaveConnectionManager.java) **失败可重试**
    - 否则 `join()` (L210, MasterSlaveConnectionManager.java) 等别人连完
  - L217-226: 所有权线程标记 connectingThread → connect() → complete / completeExceptionally → finally 清标记
- 三测试兜底: MasterSlaveConnectionManagerTest:179 (重入不死锁) / L144 (失败重试) / testDoConnectBoundsWait (有界等待)
关键设计 (q2): 单飞锁 = **CAS latch 定所有者 + 异常检测放行重试 + 线程身份防自死锁** — 一个 AtomicReference 承载了并发正确性全部要求。[模式: CAS 单飞锁]
数据流: 首线程 connect → 其余 join 等 → 失败 → isCompletedExceptionally 放新线程重试。

### 2. DNSMonitor — 轮询切换: 多轮确认防抖动

场景: 主节点 DNS 记录变了, 谁发现?怎么切?
源码路径:
- 构造 `DNSMonitor` (DNSMonitor.java:53-64): 先 resolveAddr 建立初始 maps (master/slaves: RedisURI→InetSocketAddress)
- `start()` → `monitorDnsChange()` (DNSMonitor.java:77-88): `newTimeout(task, dnsMonitoringInterval)` → monitorMasters + monitorSlaves → **allOf.whenComplete 重新调度下一轮** (自循环, 每轮结束才排下轮 = 天然防重叠)
- **resolveTimes 多轮确认** (DNSMonitor.java:268-283): 每轮 resolveAll (可能多次 DNS) → 解析结果含当前地址 → true (未变); 否则 `times+1 < dnsMonitoringTimes` → 再解析 → 最终 false = 判定变化 — **多轮确认防 DNS 抖动**
- monitorMasters (DNSMonitor.java:90-170): 解析失败/空 → 跳过; **addressSet.size()>1 → 警告 "Use Redisson PRO with Proxy mode"** (L110-116, 开源自取一个); 变化 → `changeMaster` (L152) → **成功才更新 maps** (L157)
- monitorSlaves (DNSMonitor.java:172-266): 变化 → 找含旧 slave 的 entry → `hasSlave(new)? slaveUpAsync : addSlave` → 成功 → slaves 更新 + `slaveDown(old)` (L225-249)
- 切换协议 `changeMaster` (MasterSlaveEntry.java:500-545): 先 setupMasterEntry (建新) → 成功 → 从 slaves 剔除同址项 (L527-534) + removeMaster(old) + **useMasterAsSlave()** (旧 master 降级为 slave, L540-543) → 失败 → **回滚恢复旧 master** (L515-518)
关键设计 (q4): 轮询切换 = **自循环不重叠 + dnsMonitoringTimes 多轮确认 + 成功才提交 + 主切换保底有 slave**; 失败自动回滚。[模式: 周期轮询 + 成功才提交]
数据流: 周期到 → resolveTimes 确认 → 发现变化 → changeMaster (新池→剔旧→降级) → 成功才更新镜像。

### 3. 订阅服务面 — ElementsSubscribeService (与 RD-2 边界)

场景: 谁能收到"锁释放"通知?订阅断了怎么恢复?
源码路径 (本域点到, RD-2 详述):
- `ElementsSubscribeService` (126 行): subscribeOnElements (按 consumer 弱 key 注册) + **resubscribe** (订阅断开自动恢复) — 跨实例订阅的管理器
- `ServiceManager` fields: L144 elementsSubscribeService — 构造期创建
- LockPubSub (pubsub/6 文件): `redisson_lock__channel` — RLock 释放通知的通道 (RD-2 消费)
关键设计: 订阅 = 注册表 + 断线重连恢复 (resubscribe); 机制细节在 RD-2 锁域, 本域只交代"订阅器住在 ServiceManager 里"。
数据流: RLock.tryLock → subscribe(channel) → 释放信号 → receiver 唤醒。

### 负面空间 — 高可用面刻意不做的事

- **不做主动探活**: DNS 切换是被动的 (域名变化才动), 节点健康由命令失败重试覆盖 (RD-4)
- **不做脑裂仲裁**: 多 IP 需 Pro (Proxy mode), 开源单 IP 直取 — 明确标注 Pro 边界
- **不做懒连接的公平队列**: 后到线程 join 等待, 无 FIFO 保证 (Java join 语义)
- **不保证切换零丢失**: changeMaster 瞬间的在飞请求由 RedisExecutor 重试兜底, 不是本域责任
- **不做订阅预警监控**: ElementsSubscribeService 无告警面 (生产接入 APM 靠 RedisClientTest 层)

→ 引出: 锁在这订阅通道上怎么 wait/release?看门狗怎么连续续期?→ [[rd2-rlock]]
→ 对照: serverCron 的周期任务与 HashedWheelTimer 定时谁更配客户端?→ [[r20-server]] [[r2-events]]