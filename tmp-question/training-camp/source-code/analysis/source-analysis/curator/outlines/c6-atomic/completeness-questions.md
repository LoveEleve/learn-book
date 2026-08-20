# C-6 共享状态与原子量 — completeness-questions (全视角提问验证)

## 开发者视角

1. SharedValue.start() 后本地缓存怎么初始化? watcher 什么时候触发更新?
2. trySetValue(previous, newValue) 为什么必须传 previous? 从哪拿?
3. trySetCount 的 VersionedValue<Integer> 怎么构造?
4. DistributedAtomicLong.increment() 返回的 AtomicValue 要检查什么?
5. compareAndSet(expected, new) 内部怎么校验?
6. CachedAtomicLong.next() 的 cacheFactor 是什么意思? 取值多大合适?
7. initialize() 和 forceSet() 语义差别?
8. PromotedToLock 怎么配置? lockPath 和 retryPolicy 必填?

## 架构师视角

9. zxid 为什么能判"谁更新"? 与 version 相比?
10. version 溢出到 -1 会发生什么? IllegalTrySetVersionException 的意义?
11. 乐观重试的活锁问题: 什么场景必然活锁? PromotedToLock 怎么救?
12. 为什么 trySetValue 不能信任内部缓存版本? 旧 API 废弃原因?
13. makeValue.makeFrom(previous) 的抽象价值? 乐观/锁两条路径怎么共用?
14. CachedAtomicLong 分块的正确性边界: 窗口内值唯一吗? 崩溃后?
15. DistributedAtomicNumber 为什么"没有方法保证成功"? API 设计哲学?
16. 与 Redis INCR 对比: 为什么 ZK 需要 CAS 而 Redis 单线程天然原子?
17. 与 Seata 全局锁对比: CAS 版本号 vs 数据库行锁?

## 学生视角

18. 什么是 CAS? 版本号怎么当"锁"?
19. 什么是乐观锁和悲观锁? 什么时候用哪个?
20. 什么是原子操作? 为什么需要原子?
21. 什么是号段? 分布式 ID 怎么批量取?
22. 什么是共享值? 和配置文件有什么区别?
