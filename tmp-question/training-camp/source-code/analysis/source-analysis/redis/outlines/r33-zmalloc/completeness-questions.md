# R-33 zmalloc — completeness-questions

## 开发者视角

1. 项目里为什么不能用裸 malloc? zmalloc 包一层带来了什么?
2. zmalloc 和 ztrymalloc 的区别? 什么时候用哪个?
3. PREFIX_SIZE 是什么? jemalloc 下为什么是 0?
4. zmalloc_usable 返回的大小和请求的大小什么关系? 谁在用?
5. used_memory 是原子的吗? 单线程 Redis 为什么需要?
6. OOM 时 Redis 会怎样? 能自定义行为吗?
7. 分配 10 字节, INFO memory 记多少? 为什么?
8. zfree 对称记账怎么保证不漏 (内存泄漏检测)?

## 架构师视角

9. 双路径记账 (usable vs 前缀) 的取舍? 为什么保留前缀路径?
10. "分配失败即崩溃" vs "返回 null 重试" — Redis 为什么选崩溃? try 家族怎么划定边界?
11. usable 剩余空间利用触碰了 C 语义红线 (_FORTIFY_SOURCE SIGABRT) — extend_to_usable 的工程权衡?
12. 原子统计的引入历史 (bio 线程) — 单线程模型的演进怎么影响底层?
13. 统计分层 (原子账本/RSS/smaps/mallctl) 各服务什么消费方? 为什么不做一层全能的?
14. jemalloc 第一公民绑定 (flags/tcache/arena/定制 FRAG_HINT) — 换分配器的代价?
15. 软上限 (maxmemory evict) 与硬崩溃 (OOM) 的双层防线 — 为什么不能只有一层?

## 学生视角

16. zmalloc(100) 从调用到记账完成经历了哪些步骤?
17. 为什么请求 10B 实际得 16B? size class 是什么?
18. SDS 的 alloc 字段为什么比 len 大? 这和 zmalloc 有什么关系?
19. INFO memory 的 used_memory / used_memory_rss 分别怎么来的?
20. fork 子进程的 CoW 内存怎么评估? 为什么用 smaps?
