# 闭环笔记 q1: activeExpireCycle — SLOW/FAST 双循环

## 假设
SLOW 是主循环 (25% CPU 预算), FAST 是事件循环高频补漏 (1000us), 二者触发条件与预算不同。

## 验证过程
- 常量 (expire.c:92-96): KEYS_PER_LOOP=20 / FAST_DURATION=1000us / SLOW_TIME_PERC=25 / ACCEPTABLE_STALE=10
- effort 缩放 (L191-200): effort=active_expire_effort-1 (0..9); keys_per_loop += 20/4*effort; fast_duration += 1000/4*effort; slow_time_perc += 2*effort; acceptable_stale -= effort
- 全局态 (L204-206): current_db (跨调用轮转) / timelimit_exit (上次超时) / last_fast_cycle
- **FAST 拒绝运行两条件** (L218-231):
  1. `!timelimit_exit && stat_expired_stale_perc < acceptable_stale` → return (上次没超时 + 过期比例低 → 无需快速补漏)
  2. `start < last_fast_cycle + fast_duration*2` → return (2×1000us 内不重复)
- dbs_per_call (L240-241): 默认 CRON_DBS_PER_CALL=16 (server.h:104); 超时后全 DB 扫 (timelimit_exit → dbnum)
- timelimit (L247-252): SLOW = `slow_time_perc*1000000/hz/100` (hz=10 → 25×1000000/10/100 = 25000us = 25ms); FAST = fast_duration (1000us)
- 调用点: SLOW → databasesCron (server.c:1059, iAmMaster); FAST → beforeSleep (server.c:1687-1690, 同样 iAmMaster + active_expire_enabled)
- PAUSE_ACTION_EXPIRE 暂停 (L216)
- 主循环 (L268): dbs_performed < dbs_per_call && !timelimit_exit && j < dbnum; current_db++ 先于处理 (L283, 超时也推进)
- 时间检查每 16 迭代 (L383-390, iteration & 0xf)

## 代码类型
Mechanism (自适应时间预算)

## 跨域关联
- R-20 (databasesCron/beforeSleep 调用点) / R-21 (kvstoreScan 消费) / R-19 无

## 结论
双循环 = 预算制自适应: SLOW 每 hz tick 一次, 最多 25% CPU (25ms@hz=10), 超时标记传递 (下轮全 DB); FAST 每事件循环轮询 (beforeSleep), 1000us 预算且 2ms 冷却, 只在"上次超时或有相当比例过期键"时运行 — 避免空转。两循环均需 iAmMaster (从库只等 DEL)。effort 配置统一放大/缩小基线。
源码位置: expire.c:92-96,187-408
