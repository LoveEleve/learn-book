# C-3 分布式锁 — 临时顺序节点上的公平队列

> 前置: [[C-1-CuratorFramework]] (fluent API/ProtectedMode/后台操作) + [[Z-6-Watcher]] (一次性 watcher) + [[Z-5-Session]] (临时节点) | 引出: [[C-2-Leader选举]] (LeaderSelector 复用) + [[C-6-共享状态与原子量]] (PromotedToLock) | 对照: ZK 官方 WriteLock + Redisson RLock
> 🔴 A | 8 KP | [模式: 顺序节点排队 + 前驱 watch]
> Pass 2 闭环: q1(可重入) q2(前驱算法) q3(读写锁) q4(信号量)

**读者处境**: 30 台机器同时抢一把锁, 怎么保证"只有一个赢家"且"先来先得"? 抢锁的进程崩了, 锁会不会永远锁死? 面试问"可重入分布式锁怎么实现"该怎么答?

### 1. 可重入 — 本地计数与 ZK 节点的分界

场景: 同一线程递归加锁为什么不死锁?
源码路径:
- threadData ConcurrentMap<Thread, LockData>; LockData{thread, lockPath, lockCount} (InterProcessMutex.java:42,44)
- internalLock 重入路径: 线程已有 LockData → **lockCount++ 直接返回, 不碰 ZK** (L205-210); 首次 → internals.attemptLock (L212)
- release 平衡: count 递减 >0 → return (L132-135); =0 → internals.releaseLock + threadData.remove (L139-143); 非持有线程 release → IllegalMonitorStateException (L127-130)
- isAcquiredInThisProcess = threadData.size() > 0 (L109-111)
关键设计 (q1): **可重入是"本地假象", 互斥是"ZK 事实"** — 重入只加计数不创建节点, 因此不会产生多余的顺序节点; 释放时计数归零才真正删节点。对比 ZK 官方 WriteLock 无此能力。 [模式: 引用计数]

### 2. 抢锁 — 创建顺序节点 + 判定自己是否最前

场景: 抢锁的第一步做了什么?
源码路径:
- attemptLock (LockInternals.java:171-207): while 循环 driver.createsTheLock (StandardLockInternalsDriver.java:49-72) → internalLockLoop (LockInternals.java:223-271)
- createsTheLock (StandardLockInternalsDriver.java:49-72): **EPHEMERAL_SEQUENTIAL + creatingParentContainersIfNeeded + withProtection()** (GUID 保护 — C-1 M10)
- getsTheLock (StandardLockInternalsDriver.java:33-42): ourIndex = children.indexOf(自己序号名); **ourIndex < maxLeases → 得锁**; 不得 → pathToWatch = children[ourIndex - maxLeases]
- 排序: fixForSorting 剥前缀按字符串序 (StandardLockInternalsDriver.java:75-86) — ZK 10 位补零序号
- 会话过期后找不到自己节点 (NoNodeException) → 走 retryPolicy 重来 (LockInternals.java:186-199)
关键设计 (q2): **互斥判定不靠任何"锁状态"而靠序号位置** — 全序序号天然唯一最小, 无二主; 服务端串行化创建即排队登记。 [模式: 序号全序]

### 3. 等待 — 前驱 watch + wait/notify 双通道

场景: 没抢到锁的线程在干嘛? 轮询吗?
源码路径:
- internalLockLoop (L223-271): 未得锁 → **watch 前驱节点** — getData().usingWatcher(watcher) (L244, 注释: 用 getData 而非 exists 避免遗留 watcher 资源泄漏 L242-243) → synchronized wait(剩余超时) 或无超时 wait()
- watcher 收到任何事件 → client.postSafeNotify(this) 唤醒 (L61-66)
- 前驱 NoNode (已被释放) → 重查 children 再判定 (L256-258)
- 有超时: millisToWait 递减, ≤0 → 放弃并删除自己节点 (L245-252, L267-269)
- 异常路径: deleteOurPathQuietly 清理再抛 (L262-266)
关键设计 (q2): **每个候选只 watch 一个前驱** — ZK watcher 负载 O(1) 而非全员互看 O(n²); wait/notify 在本地完成唤醒, 事件线程不被用户代码阻塞; 前驱删除是"事件", 序号是"事实", 两者解耦。 [模式: 通知链]

### 4. 释放与安全 — guaranteed 删除 + 撤销机制

场景: 释放锁时断网了, 节点会不会永远留在 ZK?
源码路径:
- releaseLock (LockInternals.java:106-110): removeWatchers + revocable 清空 + deleteOurPath (**guaranteed()** — C-1 的 FailedDeleteManager 后台重删, LockInternals.java:283)
- 会话过期: 临时节点被 ZK 服务端自动清理 — 无需客户端兜底
- revocable (LockInternals.java:51-59, 209-221): 持锁者挂 REVOKE watcher; Revoker.attemptRevoke 写入 REVOKE_MESSAGE → 回调 executor 执行撤销
关键设计 (q1): **崩溃安全 = 临时节点 + guaranteed 删除双保险**; 撤销机制是"管理员强拆锁"的逃生门 (对比 Redisson 无此面)。 [模式: 租约]

### 5. 读写锁 — 等长前缀与混排排序

场景: 为什么 __READ__ 和 __WRIT__ 前缀必须等长?
源码路径:
- READ_LOCK_NAME="__READ__" / WRITE_LOCK_NAME="__WRIT__" — **注释 "must be the same length. LockInternals depends on it"** (InterProcessReadWriteLock.java:60-62, 实测均为 8 字符)
- SortingLockInternalsDriver.fixForSorting: 先剥 READ 再剥 WRIT 前缀 (InterProcessReadWriteLock.java:64-71) — 读写节点混排后按序号排序
- ReadLock: maxLeases=**Integer.MAX_VALUE** (L123) — 读者互不竞争
- **ReadLock.getsTheLock 覆写** (InterProcessReadWriteLock.java:134-159): 本线程持有写锁 → 直接得锁 (写降级读, L137-139); 否则扫描 firstWriteIndex — 自己的序号 < 第一个写节点 → 得读锁, 否则 watch 第一个写节点 (L141-158)
- 写锁: 标准 driver — 写节点在混排中的全局位置判定互斥
关键设计 (q3): **等长前缀保证剥前缀后序仍可比** — 若前缀不等长, 剥前缀后的字符串序会错乱; 读锁"看写节点"而非"看前驱" — 所有读者排在第一个写节点前即可共存。 [模式: 前缀对齐 + 位置判定]

### 6. 降级与升级 — 单向转换

场景: 写锁能变读锁, 读锁为什么变不了写锁?
源码路径:
- Javadoc (L43-54): **写→读降级允许** (先拿写锁再拿读锁再放写锁); **读→写升级"永远不可能"** — 读者 acquire 写锁时, 自己的读节点排在写节点前 → ourIndex ≥ 1 → 死等
- ReadLock.getsTheLock 的 writeLock.isOwnedByCurrentThread 分支 (L137-139) 实现降级
关键设计 (q3): **升级死锁是分布式锁的通病** — 两个读者都想升级 → 互相等待对方的读锁; Curator 用"写节点位置判定"天然拒绝升级, 而不是靠检测死锁。 [模式: 单向状态转换]

### 7. 信号量 — 租约模型与动态上限

场景: 限流 5 个并发任务, 上限还能动态调?
源码路径:
- InterProcessSemaphoreV2: leasesPath = path/leases + 内部锁 path/locks (InterProcessSemaphoreV2.java:94-97, 122)
- maxLeases 双模式: 固定 int 或 **SharedCountReader 动态共享计数** — countHasChanged → 更新 maxLeases + postSafeNotify (InterProcessSemaphoreV2.java:113-133)
- acquire(qty,time,unit): 循环逐租约; internalAcquire1Lease: 内部 lock.acquire 保护 → getChildren 计数 ≤ maxLeases → 创建 **EPHEMERAL_SEQUENTIAL lease-** 节点 (L327-332) → Lease; 超上限 → 等待唤醒
- Lease.close → delete().guaranteed() (InterProcessSemaphoreV2.java:395-410); 会话死 → ephemeral 自动回收
关键设计 (q4): **内部锁 + 租约节点 = 计数与占位分离** — 信号量计数是"有多少 lease 节点", 谁创建谁持有; 动态上限来自 SharedCount (C-6 机制) — 这是跨配方复用的典型。 [模式: 租约]

### 8. 多锁与语法糖 — 顺序获取与逆序回滚

场景: 一次锁多个路径, 失败怎么办?
源码路径:
- InterProcessMultiLock.acquire: 顺序获取, 任一失败 → **逆序释放已获** (InterProcessMultiLock.java:104-112); release 逆序 (InterProcessMultiLock.java:124 注释)
- Locker: try-with-resources 语法糖; InterProcessSemaphoreMutex: 非重入信号量锁
关键设计 (q4): **多锁=顺序获取+回滚** — 无死锁检测, 靠获取顺序约定防死锁 (所有人按相同顺序加锁)。 [模式: 组合]

## 代码类型
Architecture (分布式原语)

## 负面空间 — Curator 锁刻意不做的事

- **不做锁超时自动过期/续约**: 持锁进程崩溃 → 等会话过期 (对照 Redisson Watchdog 30s 续期 — 面经对比点)
- **不做锁优先级/抢占**: 严格 FIFO, 后来的永远排队
- **不做读锁饥饿控制**: 写锁可能被连续读者饿死 (读者不断插入写节点前)
- **不做跨进程可重入**: 可重入仅限本 JVM 线程 (threadData 本地)
- **不做锁元数据事务**: 锁节点 data 可选, 无版本校验 (对照 C-6 原子量)
- **不做本地快速路径**: 每次 acquire 都至少一次 ZK 往返 (无 Redisson 的本地锁优化)

→ 引出: 选举配方怎么把"锁"变"领导权"? → C-2 Leader 选举
