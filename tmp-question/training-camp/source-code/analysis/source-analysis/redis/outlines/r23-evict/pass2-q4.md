# 闭环笔记 q4: LFU — 对数计数 + 分钟衰减

## 假设
LFU 复用 24bit lru 字段: 16bit 分钟时间 + 8bit 对数计数; 概率递增 + 周期衰减适应冷热变化。

## 验证过程
- 布局 (evict.c:230-260 注释): `16bit LDT (last decr time, 分钟) + 8bit LOG_C`
- LFUGetTimeInMinutes (L265-267): `(server.unixtime/60) & 65535` — 16bit 分钟 (回绕 ~45 天)
- LFUTimeElapsed (L273-277): 回绕感知分钟差
- **LFULogIncr** (L281-289): 对数概率递增 — `p = 1.0/(baseval×lfu_log_factor + 1)`, baseval = counter - LFU_INIT_VAL (LFU_INIT_VAL=5, server.h:3537)
  - 计数越高递增概率越低 (对数饱和); r < p 才 +1; 255 饱和
  - 默认 lfu-log-factor=10 (config.c:3159) — 高频键要 ~1M 次访问才能到 ~100 (注释)
- **LFUDecrAndReturn** (L301-308): 每 lfu_decay_time 分钟 (默认 1, config.c:3160) 计数 -1 (按经过的 period 数衰减, 不更新字段 — 惰性衰减)
  - ldt = o->lru >> 8; counter = o->lru & 255; num_periods = elapsed / decay_time
- 池集成 (L162): idle = 255 - counter — 池按反频率排序 (低频率优先淘汰)
- 写面: updateLFU (db.c:42-46): LFUDecrAndReturn + LFULogIncr → `lru = (LFUGetTimeInMinutes()<<8) | counter`
- 启动值 LFU_INIT_VAL=5 (注释 L251-257: 新键不从头开始, 有机会积累访问)

## 代码类型
Mechanism (对数计数 + 惰性衰减)

## 跨域关联
- R-1 (lru 字段双用途) / R-21 (updateLFU 写面 db.c:42-46) / R-22 (stat_expired_stale_perc 类似滑动思想)

## 结论
LFU = 8bit 对数计数器 (概率递增模拟 2 的幂计数) + 16bit 分钟时间戳 (惰性衰减, 每次候选检查时算)。新键 5 起跳防刚创建即被淘汰。255 反序入池实现"最低频先删"。
源码位置: evict.c:230-308; server.h:3537; config.c:3159-3160
