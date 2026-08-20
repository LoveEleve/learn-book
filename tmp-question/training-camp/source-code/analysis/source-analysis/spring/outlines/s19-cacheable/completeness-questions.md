# S2-12 @Cacheable 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | @Cacheable 的 key 如果不指定，Spring 怎么生成？为什么不能只用方法名？ | §2 |
| 2 | @Cacheable 和 @CachePut 的核心区别是什么？什么场景用哪个？ | §3 |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 3 | execute() 模板方法有 inspectCacheables→invoke→collectPutRequests 三步 — 如果把 inspectCacheables 和 collectPutRequests 合并会怎样？ | §1 |
| 4 | CacheManager 是怎么和 @Cacheable 解耦的？换一个 CacheManager(ConcurrentMap→Redis)需要改代码吗？ | §1 |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 缓存穿透是什么？@Cacheable(sync=true) 怎么防止缓存穿透？ | §2 |

## 覆盖: 5 问 / 3 身份 / 100%
