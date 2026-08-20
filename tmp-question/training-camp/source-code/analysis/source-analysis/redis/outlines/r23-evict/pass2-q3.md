# 闭环笔记 q3: LRU 近似 — 24bit 时钟与回绕

## 假设
LRU 用 24bit 降精度时钟 (1000ms 分辨率), 回绕用模运算处理。

## 验证过程
- 常量 (server.h:896-898): LRU_BITS=24 / LRU_CLOCK_MAX = 2^24-1 = 16777215 / LRU_CLOCK_RESOLUTION = 1000ms
  - 满量程 = 16777215×1000ms ≈ 194 天 (回绕周期)
- getLRUClock (evict.c:52-54): `(mstime()/1000) & 0xFFFFFF` — 降精度 + 截断
- **LRU_CLOCK 缓存** (L60-68): `1000/server.hz <= 1000` (hz≥1) → 用 server.lruclock (serverCron 更新, R-20); 否则实时算 — 免系统调用
- estimateObjectIdleTime (L72-80): 回绕感知:
  - `lruclock >= o->lru` → (lruclock - o->lru)×1000
  - 否则 (回绕了) → (lruclock + (MAX - o->lru))×1000
- 写面 (R-21 已交付): lookupKey 命中时 `val->lru = LRU_CLOCK()` (db.c:111)
- 近似本质 (注释 L82-100): "approximation of the LRU algorithm that runs in constant memory" — 采样 N=5 键进 M=16 池, 而非全局精确排序

## 代码类型
Mechanism (降精度时钟 + 近似)

## 跨域关联
- R-1 (robj lru:24bit 字段) / R-21 (lookupKey 写 lru) / R-20 (serverCron lruclock 更新)

## 结论
LRU 是"常数内存近似": 24bit×1000ms 时钟 + 采样池排序。回绕 (194 天) 由符号比较处理。hz≥1 时时钟缓存免系统调用。
源码位置: evict.c:52-100; server.h:896-898
