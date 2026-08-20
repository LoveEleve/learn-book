# C-6 共享状态与原子量 — 知识规划 (KP)

> 域级: 🟡 B | 模块: recipes/shared/ (8 文件: SharedValue 291 / SharedCount 169 / VersionedValue 66 / 监听器 4 / IllegalTrySetVersionException 39) + recipes/atomic/ (11 文件: DistributedAtomicValue 297 / DistributedAtomicLong 211 / DistributedAtomicInteger 211 / DistributedAtomicNumber 109 / CachedAtomicLong 71 / CachedAtomicInteger 71 / PromotedToLock 122 / MakeValue 24 / AtomicValue 54 / MutableAtomicValue 57 / AtomicStats 82)
> 日期: 2026-08-15 | 版本: 5.8.0

## 一、机制提取 (逐源)

### M1 SharedValue: 版本化共享值 (291)
- 本地缓存 AtomicReference<VersionedValue> + watcher 更新 (L60-63); **zxid 单调 CAS** (L202-214: updateValue — zxid 更大才覆盖, CAS 自旋)
- 初始 (NO_ZXID=-1, UNINITIALIZED_VERSION=-1, seed) (L51-52, L105-107)
- **trySetValue(previous, newValue)** (L177-200): ①与本地 current 比对 zxid+value (L183-185) ②version==-1 抛 IllegalTrySetVersionException (L186-188) ③setData().withVersion(previous.getVersion()) 分布式 CAS (L191) ④BadVersion → 重读返回 false (L194-199)
- setValue 盲写 (L138-143); 旧 trySetValue 已废弃 (L161-163)
- watcher 后台重读+通知 (L63-71); 连接恢复重读 (L74-87)

### M2 SharedCount: int 包装 (169)
- 完全委托 SharedValue, int ↔ 4 字节大端 (L48-50, L160-168); trySetCount(VersionedValue<Integer>, int) (L109-112)

### M3 DistributedAtomicValue: 乐观重试 (297)
- **tryOptimistic** (L252-271): while 循环 tryOnce, 失败按 retryPolicy.allowRetry 重试
- **tryOnce** (L273-296): getCurrentValue → makeValue.makeFrom(previous) 算新值 → 不存在则 create / 存在则 **setData().withVersion** (L280-284); NodeExists/BadVersion/NoNode 捕获重试 (L287-293)
- **PromotedToLock 升级** (L221-250): 乐观失败 → mutex.acquire(maxLockTime) 持锁重试 (L225); 默认 RetryNTimes(0,0) (L234-241)
- compareAndSet (L120-138); initialize 仅不存在才 create (L173-181); forceSet 无原子性 (L97-107)
- MakeValue 函数式接口: makeFrom(previous) 抽象"读-算-写" (L23)

### M4 CachedAtomicLong: 分块 (71)
- next(): add(cacheFactor) 领一整块 → 本地逐次分发 (L48-70); 块用完再取 — **ZK 往返摊薄**

### M5 支持面
- AtomicValue: succeeded/preValue/postValue/stats (L32-53); AtomicStats 统计乐观/锁尝试 (L26-29)
- DistributedAtomicNumber 接口 10 方法, 全部返回 AtomicValue 必须检查 succeeded (L30-108)
- VersionedValue: (zxid, version, value) 不可变 (L32-41); zxid=Stat.getMzxid (L46), version=Stat.getVersion (L55, 注释警告会溢出不单调)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M3 乐观重试 + 升级 | P1 | 原子量核心; 面试点 (分布式 CAS) |
| M1 zxid 单调 + 版本 CAS | P1 | SharedValue 核心; IllegalTrySetVersion 边界 |
| M4 分块 | P2 | 性能优化面 |
| M2/M5 | P3 | 包装与契约 |

## 三、负面空间

- **不做悲观锁默认**: 默认乐观重试, 高冲突活锁靠 PromotedToLock 升级 (需显式配置)
- **不做事务**: 单节点 CAS, 无跨节点原子
- **不做数值溢出保护**: Long/Integer 溢出回绕, 调用方自检
- **不做失败回滚**: 原子操作失败后 preValue/postValue 由调用方处理
- **不做客户端缓存一致性**: SharedValue 本地缓存可能过期, 必须用 getVersionedValue 返回的版本
