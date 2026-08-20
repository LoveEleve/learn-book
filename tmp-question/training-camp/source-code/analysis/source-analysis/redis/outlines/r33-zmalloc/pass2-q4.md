# 闭环笔记 q4: usable size 消费方 — SDS 的免费预分配

## 假设
zmalloc_usable 返回分配器实际可用大小 (≥请求), 消费方 (如 SDS) 用剩余空间免掉一次 realloc。

## 验证过程
- zmalloc.c:138-147 (zmalloc_usable): 分配后取 usable_size (jemalloc 实际), 通过出参返回
- **SDS 消费实证** (sds.c:90-105 sdsnew):
  - `s_trymalloc_usable(hdrlen+initlen+1, &usable)` — 请求 hdr+内容+1, 拿到 usable
  - `usable = usable-hdrlen-1` — 剩余内容容量
  - `if (usable > sdsTypeMaxSize(type)) usable = sdsTypeMaxSize(type)` — 上限封顶
  - alloc 字段直接记为 usable — **SDS 初始容量 = 分配器实际大小 (免费膨胀), 后续追加到 usable 都不用 realloc**
- 同理 sds.c 的 sdscat/sdsMakeRoomFor 用 s_realloc_usable (预分配策略的底层实现, R-4 域详述)
- jemalloc 的 size classes (8/16/32/... 幂次桶): 请求 10B 实际给 16B — usable=16 省 6B 的二次分配

## 代码类型
Glue (跨层优化) + Algorithmic (剩余空间利用)

## 跨域关联
- R-4 (SDS 预分配策略) → usable 是第一级预分配, sdsMakeRoomFor 是第二级
- R-1 (object.c 字符串创建) → createStringObject 走 sdsnew 链路
- networking.c 输出缓冲 (client.buf) → 同样用 zmalloc_usable 面

## 结论
usable 家族 = "分配器 size class 的免费膨胀利用": 请求 N 实际得 usable≥N, SDS 初始 alloc 直接取 usable — 后续追加免费。这是 Redis 性能取向的微观体现 (省 realloc 换记账复杂)。
源码位置: zmalloc.c:138-147; sds.c:90-105
