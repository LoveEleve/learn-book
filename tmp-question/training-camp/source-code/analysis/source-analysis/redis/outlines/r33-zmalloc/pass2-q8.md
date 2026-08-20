# 闭环笔记 q8: jemalloc 特有面 — with_flags / no_tcache / defrag 连接

## 假设
zmalloc 对 jemalloc 暴露三类特有能力: mallocx flags 分配 (zmalloc_with_flags), 绕过 tcache 的分配 (no_tcache, 供 defrag), 以及 arena 级查询。

## 验证过程
- zmalloc_with_flags (zmalloc.c:149-156): `mallocx(size+PREFIX_SIZE, flags)` — jemalloc 扩展 API, flags 控制 (arena 选择/零化/tcache 策略); zrealloc_with_flags L158-187: size=0→free 重定向 / ptr=NULL→malloc 重定向 (realloc 语义三态); zfree_with_flags L189-193: dallocx
- **no_tcache 面 (L196-213)**: `mallocx(size, MALLOCX_TCACHE_NONE)` — 注释明说 "Currently implemented only for jemalloc. Used for online defragmentation" — 绕过线程缓存直达 arena bins, 让 defrag (R-18) 移动对象时避免 tcache 干扰
- zmalloc.h:76-78: HAVE_DEFRAG 条件 = `USE_JEMALLOC && JEMALLOC_FRAG_HINT` — 仅 jemalloc 特殊版本 (Redis 定制, 返回 per-allocation 碎片提示)
- set_jemalloc_bg_thread / jemalloc_purge: 后台线程 (purge 释放给 OS) 与主动 purge
- allocator_info_by_arena (L680+): 指定 arena 查询 (Lua VM 独立 arena — RELEASENOTES #13133 "Lua: allocate VM code with jemalloc instead of libc and count it as used memory" — R-30 连接)
- 调用面: defrag.c 用 zmalloc_no_tcache/zfree_no_tcache (R-18 详述)

## 代码类型
Interface (分配器扩展契约) + Implementation

## 跨域关联
- R-18 (defrag.c) → no_tcache 族的唯一消费方
- R-30 (Lua arena) → allocator_info_by_arena + lua_arena 配置
- R-20 (INFO memory/MEMORY STATS) → 展示端

## 结论
zmalloc 对 jemalloc 的绑定面: flags 分配 (arena/tcache 控制)、no_tcache (defrag 专用, 需 Redis 定制 jemalloc 的 FRAG_HINT)、arena 级统计 (Lua VM 独立核算)。这些是"深度绑定"证据 — Redis 的生产内存模型以 jemalloc 为第一公民。
源码位置: zmalloc.c:149-213,640-1004; zmalloc.h:76-78
