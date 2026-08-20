# 闭环笔记 q1: PREFIX_SIZE 双路径 — malloc_usable_size 省掉的 8 字节

## 假设
zmalloc 在 HAVE_MALLOC_SIZE (jemalloc 等) 时不在指针前存前缀, 用 malloc_usable_size 实时查询实际大小; 无该能力 (老 libc) 时退回 8 字节前缀方案。

## 验证过程
- zmalloc.h:37-45: `#ifdef HAVE_MALLOC_SIZE #define PREFIX_SIZE (0) #else PREFIX_SIZE (sizeof(size_t))` — 双路径编译期切换
- zmalloc.h:29-79: HAVE_MALLOC_SIZE 的来源: tcmalloc (tc_malloc_size) / jemalloc (je_malloc_usable_size) / macOS (malloc_size) / glibc+FreeBSD (malloc_usable_size) / NO_MALLOC_USABLE_SIZE 强制关闭
- zmalloc.c:93-111 (ztrymalloc_usable_internal): HAVE_MALLOC_SIZE 分支 — `size = zmalloc_size(ptr)` 实时查询, **记账用实际大小**; 无分支 — `*((size_t*)ptr) = size` 前缀存请求大小, 返回 `ptr+PREFIX_SIZE`
- 关键差异: **usable 路径记账的是 malloc_usable_size (≥请求大小), 前缀路径记账的是请求大小** — 同一分配在 jemalloc 下 used_memory 更高 (含分配器实际开销)
- jemalloc 生产默认 (Redis 官方构建) — 前缀路径仅是"无 usable_size 平台"兜底 (老 libc/嵌入式)

## 代码类型
Algorithmic (双路径记账) + Glue (平台抽象)

## 跨域关联
- R-1 (object.c 共享整数/对象分配) → 全部走 zmalloc
- R-4 (sds.c:90-105 s_malloc_usable) → SDS 直接消费 usable (q4)
- 平台面: USE_JEMALLOC 编译开关是 Redis 官方生产默认

## 结论
PREFIX_SIZE=0 依赖"分配器能报告实际大小" (usable_size ≥ 请求): 省 8 字节/次 + 记账精确到分配器真实开销 (jalloc 的 size classes 导致 usable > 请求); 无能力平台退回前缀存大小。**usable 路径的 used_memory 语义 = 分配器视角, 前缀路径 = 请求者视角**。
源码位置: zmalloc.h:29-79, zmalloc.c:93-111
