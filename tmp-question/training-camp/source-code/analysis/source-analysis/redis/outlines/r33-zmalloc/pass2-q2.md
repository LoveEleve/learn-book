# 闭环笔记 q2: try 家族 — OOM 降级 vs 崩溃的调用方选择

## 假设
zmalloc (OOM→handler 崩溃) 是默认; ztry* (OOM→NULL) 只在"大块分配且失败可降级"的路径使用。

## 验证过程
- zmalloc.c:124-128: `zmalloc → ztrymalloc_usable_internal; if (!ptr) zmalloc_oom_handler(size)` — OOM 必崩溃 (默认 handler → serverPanic)
- zmalloc.c:131-134: ztrymalloc — 失败返回 NULL, 调用方自决
- 调用方穷举 (grep ztry):
  - rdb.c:389,396,555: RDB 加载大对象 (string/双精度解析缓冲) — 失败返回错误而非崩溃
  - t_stream.c:3411: XDEL 批量删除 ID 数组 — 大数组降级
  - module.c:521,546: 模块分配 — 模块面可控
- **为什么不是全局用 try**: Redis 正常路径 (命令处理/数据结构) 分配失败 = 状态已破坏, 崩溃比继续执行安全 (serverPanic 打印分配大小); try 面 = "一次分配, 失败可放弃操作"的路径
- OOM handler 可替换: server.c:6970 `zmalloc_set_oom_handler(redisOutOfMemoryHandler)` → server.c:6712-6717 serverLog+serverPanic

## 代码类型
Algorithmic (错误策略分层) + Glue (可替换 handler)

## 跨域关联
- R-23 (evict) → maxmemory 是软上限 (淘汰兜底), zmalloc OOM 是硬崩溃 — 两层防线
- R-8 (rdb.c 加载路径) → try 家族的主消费方
- R-10 (t_stream XDEL) → 大数组降级

## 结论
双轨错误策略: 默认 zmalloc 失败即崩溃 (serverPanic 可观测), try 家族 = 特定路径的降级开关 (RDB 加载/流批量/模块)。OOM handler 单例可替换, main 装配 redisOutOfMemoryHandler。
源码位置: zmalloc.c:75-82,124-134; server.c:6712-6717,6970; rdb.c:389
