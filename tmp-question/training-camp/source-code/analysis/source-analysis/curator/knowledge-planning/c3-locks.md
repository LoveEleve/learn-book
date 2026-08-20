# C-3 分布式锁 — 知识规划 (KP)

> 域级: 🔴 A | 模块: curator-recipes/.../locks/ (18 文件: InterProcessMutex 221 / LockInternals 288 / StandardLockInternalsDriver 93 / InterProcessReadWriteLock 210 / InterProcessSemaphoreV2 420 / InterProcessMultiLock 163 / Revoker 45 / Lease / LockInternalsSorter / PredicateResults / InterProcessLock / InterProcessSemaphoreMutex / Locker / Revocable / RevocationListener / RevocationSpec / InterProcessSemaphore / LockInternalsDriver)
> 日期: 2026-08-15 | 版本: 5.8.0

## 一、机制提取 (逐源)

### M1 InterProcessMutex: 可重入互斥锁 (221)
- 核心: threadData ConcurrentMap<Thread, LockData> (L42, LockData L44); **LockData {Thread, lockPath, lockCount}**
- **可重入**: internalLock — 线程已有 LockData → lockCount++ 直接返回 (L205-210); 首次 → internals.attemptLock (L212)
- **release 平衡**: lockCount 递减, >0 不释放 ZK 节点 (L132-135), =0 → internals.releaseLock + threadData.remove (L139-143); 非持有者 release 抛 IllegalMonitorStateException (L127-130)
- isOwnedByCurrentThread (L183-186); isAcquiredInThisProcess (L109-111, threadData.size()>0)
- getLockNodeBytes() protected 默认 null — LeaderSelector 覆写 (C-2)
- 构造: basePath 校验 + LockInternals(client, driver, path, LOCK_NAME="locks-", maxLeases=1)

### M2 LockInternals: 锁核心算法 (288)
- **attemptLock** (L171-207): while 循环 { driver.createsTheLock → internalLockLoop }; NoNodeException (session 过期后找不到自己节点) → retryPolicy.allowRetry 决定重来或抛
- **internalLockLoop** (L223-271): while(STARTED && !haveTheLock): getSortedChildren → driver.getsTheLock(children, 自己序号, maxLeases)
  - 拿到锁 → haveTheLock=true
  - 否则 → **watch 前驱** (getData().usingWatcher(watcher) 而非 exists, 注释: 避免遗留 watcher 资源泄漏 L242-243) → 有超时则计时递减 wait(millisToWait), 无超时 wait() 无限
  - NoNodeException (前驱已释放) → 重新循环
  - 异常 → deleteOurPathQuietly + 抛
- **watcher** (L61-66): 任何事件 → client.postSafeNotify(this) 唤醒 wait
- **getSortedChildren** (L129-157): getChildren + Collections.sort(fixForSorting) — 字符串序
- **releaseLock** (L106-110): client.removeWatchers + revocable=null + deleteOurPath (guaranteed)
- **revocable 机制** (L51-59, L209-221): makeRevocable + 监听自己节点数据变化 = REVOKE_MESSAGE → executor 执行撤销回调 (C-3 补充面)
- **clean()** (L77-85): 删除 basePath (BadVersion/NotEmpty 忽略)
- getParticipantNodes (L116-127)

### M3 StandardLockInternalsDriver: 判定策略 (93)
- **createsTheLock** (L49-72): CreateMode.EPHEMERAL_SEQUENTIAL + creatingParentContainersIfNeeded + **withProtection()** (C-1 M10 的 GUID 保护 — 创建响应丢失可恢复)
- **getsTheLock** (L33-42): ourIndex = children.indexOf(自己); ourIndex < 0 → NoNodeException; **ourIndex < maxLeases → 得锁, pathToWatch = children[ourIndex - maxLeases]** (maxLeases=1 → 前驱; maxLeases>1 → 前面第 maxLeases 个)
- **fixForSorting** (L75-86): 截掉 "locks-" 前缀后按序比较 — 序列号字符串序 (ZK 10 位补零 %010d)
- getSortingSequence protected 可覆写 (读写锁用)

### M4 InterProcessReadWriteLock: 读写锁 (210)
- 双锁: ReadLock (maxLeases=**Integer.MAX_VALUE** L123) + WriteLock (maxLeases=1 L112)
- **节点名前缀必须等长** (实测均为 8 字符): READ_LOCK_NAME="__READ__" / WRITE_LOCK_NAME="__WRIT__" — 注释 "must be the same length. LockInternals depends on it" (InterProcessReadWriteLock.java:60-62); 等长的原因: 读写节点混排后 fixForSorting 依次剥两前缀, 前缀不等长则剥后字符串序错乱
- **SortingLockInternalsDriver** (L64-71): fixForSorting 先按 READ 再按 WRITE 剥前缀 — 保证读写节点混排时按序号排序
- **ReadLock.getsTheLock 覆写** (L134-159): 写锁持有者本线程 → 直接得锁 (写降级读); 否则找 **firstWriteIndex** — 自己序号 < 第一个写节点 → 得锁, 否则 watch 第一个写节点 (pathToWatch = children[firstWriteIndex])
- 写锁获取: 标准 driver (写锁互斥天然)
- **语义** (Javadoc L43-54): 可重入; 写→读降级允许; 读→写升级**永远不可能** — 推导: 写锁用标准 driver, 但节点排序是读写**混排** (fixForSorting 依次剥两前缀, 等长前缀保证序一致); 读锁持有者再 acquire 写锁时, 其写节点序号必大于自己的读节点 → 混排全集中 ourIndex ≥ 1 → 永远等不到; 这正是前缀必须等长的原因 (等长 → 混排序号序正确)
- WriteLock.getLockPath / ReadLock 用于降级判定

### M5 InterProcessSemaphoreV2: 信号量 (420)
- 双路径: leasesPath = path + "/leases" (L122); 内部锁 LOCK_PARENT="locks" (L94)
- maxLeases 两种模式: 固定值 或 **SharedCountReader 动态共享计数** (L113-133 — countHasChanged 更新 maxLeases + postSafeNotify 唤醒)
- **acquire(qty, time, unit)** (L241): 循环逐租约获取; internalAcquire1Lease (L305): 内部 lock.acquire (互斥保护 leases 计数) → 持锁后 getChildren 计数 ≤ maxLeases → 创建 **EPHEMERAL_SEQUENTIAL lease- 节点** (L327-332) → makeLease; > maxLeases → 等待 (postSafeNotify + 超时)
- **Lease 生命周期** (L355+): close() → delete().guaranteed() (L358-360) — 会话死亡自动清理 (ephemeral)
- returnLease/returnAll (L165-178)
- 内部锁路径 = path + "/locks" — 与 leases 子路径分离 (LOCK_SCHEMA L97)

### M6 InterProcessMultiLock: 多锁 (163)
- acquire: 顺序逐锁获取, 任一失败 → **逆序释放已获取** (L104-112); 全成功才返回 (L79-119)
- release: **逆序释放** (Javadoc L124 注释)

### M7 Revoker/Revocation (45+)
- Revoker.attemptRevoke (L29-45 附近): 外部强制撤销 — setData REVOKE_MESSAGE
- Revocable: makeRevocable(listener, executor) — 持锁者监听 REVOKE_MESSAGE → 回调 (InterProcessMutex L157-170)

### M8 其他
- InterProcessSemaphoreMutex: 非重入信号量锁 (maxLeases=1, 无 LockData 计数?)
- Locker: AutoCloseable 语法糖 try-with-resources
- LockInternalsSorter/PredicateResults: 排序与判定载体
- InterProcessLock 接口: acquire()/acquire(time,unit)/release()/isAcquiredInThisProcess()

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M2 LockInternals 前驱 watch + wait/notify | P1 | 锁算法核心; 事件驱动 + 本地等待双通道 |
| M3 判定策略 (ourIndex < maxLeases) | P1 | 唯一互斥判定; 面试必问 |
| M1 可重入计数 | P1 | 本地锁与 ZK 锁的分界; 常见误解 |
| M4 读写锁混排 + 等长前缀 | P1 | 设计巧思; 降级/升级语义 |
| M5 信号量 | P2 | 租约模型 + 动态 maxLeases |
| M6 MultiLock | P2 | 逆序释放 |
| M7 撤销机制 | P3 | 低频扩展面 |

## 三、负面空间

- **不做锁超时自动过期**: 持锁者崩溃 → 会话过期才释放 (ZK 语义)
- **不做锁续约**: 会话不死锁不丢 (无 watchdog, 对照 Redisson 自动续期)
- **不做公平性保证之外的饥饿控制**: FIFO 顺序由 ZK 序号保证, 但无优先级
- **不做本地锁与 ZK 锁的混合**: 可重入仅在单 JVM 内 (threadData), 跨进程互斥靠 ZK
- **不做 CAS 型锁**: 锁语义只有互斥, 无版本校验 (对照 C-6 DistributedAtomicValue)
- **不做跨进程可重入**: threadData 仅本 JVM 线程

## 四、时空溯源

- 无 git 历史 (浅克隆); 代码内证据: "use getData() instead of exists() to avoid leaving unneeded watchers" (LockInternals.java:242-243) — watcher 泄漏修复; Revocable @since 2.0.0 附近
- InterProcessSemaphore (旧) vs InterProcessSemaphoreV2 — V2 引入 SharedCount 动态上限 (类注释)
- 与 ZK 官方 recipes WriteLock 对照: WriteLock 无超时/无可重入/无保护模式 — Curator 的增量
