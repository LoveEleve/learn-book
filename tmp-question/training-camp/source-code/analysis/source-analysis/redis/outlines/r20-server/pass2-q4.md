# 闭环笔记 q4: hz 自适应 — 客户端数驱动频率

## 假设
dynamic_hz 让 serverCron 频率随客户端数调整: 客户端多 → hz 翻倍 (每 tick 处理更多) — MAX_CLIENTS_PER_CLOCK_TICK=200 为每 tick 客户端配额。

## 验证过程
- serverCron (server.c:1282-1295): `server.hz = server.config_hz; if (server.dynamic_hz) { while (listLength(server.clients) / server.hz > MAX_CLIENTS_PER_CLOCK_TICK) server.hz *= 2; }` — **客户端数 / hz > 200 → hz 翻倍** (每 tick 分摊 200 客户端)
- 上限: CONFIG_MAX_HZ=500 (server.h:102); 默认 10 (L100); 下限 1
- 为什么: clientsCron 每 tick 处理部分客户端 (超时检查/内存统计) — 客户端多而 hz 低 → 处理不完; hz 翻倍 → 每 tick 配额稳定
- 权衡: hz 高 → cron 开销高 (每次 1000/hz ms 间隔); 客户端少 → 回到 config_hz
- 配套: server.cronloops 计数 (每 tick); el_cron_duration 计时 (性能监控)

## 代码类型
Algorithmic (自适应调度) — 负载响应

## 跨域关联
- q3 (时间分级) → 宿主
- R-28 (客户端管理) → 数量来源
- 配置面: hz/dynamic-hz (config.c)

## 结论
hz 自适应 = 每 tick 客户端配额的闭环: 客户端/hz > 200 → 翻倍 (上限 500); 客户端少 → 回默认 10。cron 频率随负载伸缩, 让"每 tick 工作量"稳定。
源码位置: server.c:1282-1295; server.h:100-103
