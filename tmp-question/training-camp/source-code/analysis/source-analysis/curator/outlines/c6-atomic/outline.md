# C-6 共享状态与原子量 — ZK 上的分布式 CAS: 乐观重试与锁升级

> 前置: [[C-1-CuratorFramework]] (setData withVersion) + [[C-3-分布式锁]] (PromotedToLock 升级) + [[Z-3-DataTree]] (版本号 stat) | 引出: [[C-8-服务发现]] (DownInstanceManager 计数) | 对照: Redisson RAtomicLong + Redis INCR
> 🟡 B | 5 KP | [模式: 版本 CAS + 乐观重试]
> Pass 2 闭环: q1(SharedValue) q2(原子量) q3(升级) q4(分块)

**读者处境**: 全局计数器/开关量放 ZK, 并发"读-改-写"怎么保证原子? 高冲突时乐观重试会活锁, 怎么办?

### 1. SharedValue — zxid 单调性与版本 CAS

场景: 配置中心共享开关, 多个客户端并发改
源码路径:
- 本地缓存 AtomicReference<VersionedValue> + watcher 后台更新 (SharedValue.java:60-71); **updateValue zxid 单调 CAS** (L202-214): 只允许 zxid 更大者覆盖 — 乱序到达的旧数据不覆盖新数据
- **trySetValue(previous, newValue)** (L177-200): ①前值与本地 current 比对 zxid+value (L183-185) ②**previous.getVersion()==-1 抛 IllegalTrySetVersionException** (L186-188, 版本溢出时 setData(-1) 盲写破坏 CAS) ③setData().withVersion (L191) ④BadVersion → readValue 刷新 → false (L194-199)
- 旧 trySetValue(byte[]) 废弃: 内部缓存可能过期, 调用方必须自己持有读到的版本 (L151-163 注释)
关键设计 (q1): **zxid 判断"谁更新"比 version 可靠** — version 会溢出 (VersionedValue 注释, ZOOKEEPER-4743 引用); 版本 CAS 让并发读改写原子化, 失败方重读重试。 [模式: 版本化状态]

### 2. DistributedAtomicValue — 乐观重试循环

场景: 分布式计数器 increment, 十次有八次冲突怎么办
源码路径:
- **tryOptimistic** (DistributedAtomicValue.java:252-271): while 循环 tryOnce, 失败按 retryPolicy.allowRetry 退避重试
- **tryOnce** (L273-296): getCurrentValue → makeValue.makeFrom(previous) 算新值 → 不存在 create / 存在 **setData().withVersion(stat.getVersion())** (L280-284); NodeExists/BadVersion/NoNode 捕获重试 (L287-293)
- MakeValue: makeFrom(previous) 把"读-算-写"的"算"抽象出来 (L23)
- initialize: 仅不存在才 create (L173-181) — INSERT 语义非 UPSERT
关键设计 (q2): **乐观 = 无锁尝试 + 版本校验** — 冲突检测靠 BadVersion, 重试由 retryPolicy 控节奏; 每个方法返回 AtomicValue, succeeded 必须检查 (接口注释 "没有方法保证一定成功" L36-37)。 [模式: 乐观 CAS]

### 3. 锁升级 — 高冲突时从乐观到悲观

场景: 写冲突 90%, 乐观重试变成活锁
源码路径:
- **tryWithMutex** (L221-250): mutex.acquire(maxLockTime) (L225) → 持锁后 tryOnce + 独立 retryPolicy (默认 RetryNTimes(0,0) 不重试, L234-241) → finally release (L245)
- **PromotedToLock** builder: lockPath 必填 + retryPolicy + timeout (L68-95)
关键设计 (q3): **乐观→悲观两段式** — 冲突低时零锁开销, 冲突高时降级互斥保证推进; 这是"自适应并发控制"的简化版 (对照数据库锁升级)。 [模式: 升级]

### 4. CachedAtomicLong — 分块摊销 ZK 往返

场景: 每秒 1 万次 next(), 每次都走 ZK?
源码路径:
- next(): **add(cacheFactor) 一次领一整块** → 本地逐次 +1 分发 (CachedAtomicLong.java:48-70); 块用完再取; 失败置 null (L51-57)
- 类注释: 每次分布式 CAS 太贵, 分块把往返摊薄到 cacheFactor 次调用 (L22-24)
关键设计 (q4): **批量的代价是窗口内的不精确** — 块内值是本机私有递增, 全局只保证块边界一致; 应用需容忍"瞬时重复号段" (对比雪花算法号段)。 [模式: 分块缓存]

### 5. SharedCount 与契约 — 计数之上的统一 CAS 引擎

场景: C-3 信号量 (InterProcessSemaphoreV2) 的动态上限就是 SharedCount
源码路径:
- SharedCount 委托 SharedValue, int ↔ 4 字节大端 (SharedCount.java:48-50, L160-168); trySetCount(VersionedValue<Integer>, int) (L109-112)
- AtomicValue 四元组: succeeded/preValue/postValue/stats (AtomicValue.java:32-53); AtomicStats 乐观/锁尝试统计 (L26-29)
关键设计 (q2): **跨配方复用** — C-4 信号量上限、本域计数都用同一版本 CAS 引擎。 [模式: 契约统一]

## 代码类型
Architecture (分布式原语)

## 负面空间 — Curator 原子量刻意不做的事

- **不做悲观锁默认**: 高冲突需显式 PromotedToLock
- **不做事务**: 单节点 CAS, 无多节点原子更新
- **不做溢出保护**: 溢出后 version=-1 直接拒绝 CAS (IllegalTrySetVersionException), 数值溢出需业务处理
- **不做自动重试策略**: 重试节奏全由 retryPolicy 决定, 无指数上限保护
- **不做客户端缓存一致性**: SharedValue 本地缓存过期由调用方版本控制兜底
- **不做 Lua 脚本类原子操作**: 无法在一个操作里做"读-判-写"组合 (对照 Redis EVAL)

→ 引出: 持久节点配方怎么让临时节点跨会话活下来? → C-7 持久节点与组成员
