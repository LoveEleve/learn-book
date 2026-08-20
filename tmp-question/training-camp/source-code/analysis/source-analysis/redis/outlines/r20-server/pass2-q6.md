# 闭环笔记 q6: beforeSleep — 事件循环前置批处理

## 假设
beforeSleep 在每轮事件循环前执行: 周期性任务 (计数/时间) + 写面批处理 (客户端待写/AOF flush) — 与 serverCron 分工: cron 管周期, beforeSleep 管"每轮必做"。

## 验证过程
- beforeSleep (server.c:1637+): 每轮 aeMain 循环前置调用 (aeSetBeforeSleepProc, L2772 注册)
- 职责:
  - 计时: cron 耗时记录 (el_cron_duration 等, L1681-1792 区域注释: "cron 时间 = active-expire/active-defrag 等, 不含 read/write/AOF")
  - 写面: `flushAppendOnlyFile(0)` (AOF 缓冲写, 每轮!) + `handleClientsWithPendingWrites()` (客户端待写) — 注释: TLS pending 数据先于 AOF flush (appendfsync=always 语义)
  - 阻塞键: handleClientsBlockedOnKeys (R-26)
  - 时间缓存: updateCachedTime (L1773 区域)
  - 睡眠控制: 无事可做 → 延长睡眠 (L1792)
- 与 serverCron 分工: **cron = 定时任务 (hz 驱动); beforeSleep = 每轮必做** (事件到达才触发) — 高吞吐路径的批处理点
- processEventsWhileBlocked 兼容: 阻塞命令期间的受限 beforeSleep

## 代码类型
Glue (批处理编排) — 高吞吐路径

## 跨域关联
- R-8 (AOF flush) → 每轮
- R-28 (客户端待写) → 写面
- R-26 (阻塞键) → 唤醒面
- R-2 (ae 前置回调) → 宿主

## 结论
beforeSleep = 每轮事件循环的"必做清单": AOF 写/客户端写/阻塞键唤醒/时间缓存 — 在事件处理前批量完成。与 serverCron (hz 定时) 构成双时间面: 事件驱动 + 周期驱动。
源码位置: server.c:1637-1800,2772
