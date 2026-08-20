# M-7 缓存体系 — completeness-questions

## 开发者视角

1. `<cache eviction="LRU" size="1024"/>` 一行配置经过 CacheBuilder 变成几层装饰?顺序是什么?
2. readWrite 默认是什么?为什么默认缓存存的是序列化副本?
3. 为什么 commit 前其他会话看不到新写入的二级缓存?TransactionalCache 的 pending 机制?
4. 同 key 并发未命中时会发生什么?BlockingCache 的 CountDownLatch 怎么工作?
5. 配置了自定义缓存实现 (type 属性) 还会被包装饰器吗?为什么 (issue#352)?
6. 一级缓存和二级缓存的查询顺序?各自生命周期?
7. 清了二级缓存一级缓存会跟着清吗?cache-ref 是什么?
8. CacheKey 由哪些部分构成?为什么参数值参与?

## 架构师视角

9. CacheBuilder 的装饰链顺序 (Scheduled→Serialized→Logging→Synchronized→Blocking) 为什么这样排?Logging 在 Synchronized 内的意义?
10. Cache 接口 3.2.6 起核心不再调用 getReadWriteLock — 同步由缓存自持的设计取舍?
11. TransactionalCache 三暂存 (clearOnCommit/entriesToAddOnCommit/entriesMissedInCache) 各解决什么问题?commit 与 rollback 的差异?
12. getObject 的 clearOnCommit 分支 (issue#146) 为什么返回 null?
13. LruCache 双 map 同步 (keyMap 访问序+delegate 数据) 为什么 getObject 也要 touch?
14. 一级/二级缓存的分级设计 vs 单一缓存: 各自防什么?
15. 与 Redis 等分布式缓存的对比: MyBatis 二级缓存为什么不适合多实例?

## 学生视角

16. 一次带二级缓存的 selectList 的完整调用链 (CachingExecutor→tcm→TransactionalCache→装饰链→PerpetualCache)?
17. CacheKey 的累加式哈希 (37 乘子/17 初始) 怎么保证顺序敏感?
18. PerpetualCache 只是 HashMap 的 Cache 化 — 线程安全靠什么?
19. 8 种装饰器 (Fifo/Lru/Scheduled/Serialized/Soft/Weak/Synchronized/Blocking) 各管什么能力?
20. 二级缓存的写入时机: 为什么 commit 才真正写入?
21. cache-ref 跨 namespace 共享缓存的配置与解析 (关联 M-1 incomplete 机制)?
