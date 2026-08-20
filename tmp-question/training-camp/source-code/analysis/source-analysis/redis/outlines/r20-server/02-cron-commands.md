# R-20 下 — 周期引擎与命令表: serverCron 时间分级 + 122 命令注册

> 前置: [[R-20-上]] (启动) + [[R-2-event-loop]] (宿主) + [[R-22-expire]] (databasesCron) | 引出: [[R-21-db]] (键空间宿主)
> 🔴 A | 4 KP | [模式: 时间分级调度+负载自适应+声明式命令表+双面批处理]
> Pass 2 闭环: q3(cron 分级) q4(hz 自适应) q5(命令表) q6(beforeSleep)

**读者处境**: 每秒 10 次的 serverCron 都干什么?为什么客户端多了 cron 会变快?122 个命令的定义在哪?AOF 为什么在"事件前"写?这篇拆 Redis 的周期引擎: 时间分级、hz 自适应、生成式命令表、beforeSleep 双面批处理。

### 1. serverCron — 时间分级调度

场景: 什么任务每 tick?什么任务 5 秒一次?
源码路径:
- 每 tick (server.c:1273-1300): watchdog → hz 自适应 → lruclock → 内存统计 → **clientsCron → databasesCron (R-22 主动过期) → updateDictResizePolicy (R-3 COW)**
- run_with_period(100) (L1303): 时间缓存/内存抽样/modulesCron/replicationCron
- run_with_period(1000) (L1404): receiveChildInfo (子进程 COW, R-8) / tracking resize / BGSAVE scheduled
- run_with_period(5000) (L1361): 统计日志
- **返回 1000/server.hz** (L1538) — 下次调度毫秒数 (hz=10 → 100ms)
关键设计: 时间分级 (q3): 必须快的每 tick (过期/COW), 常规轮询 100ms (复制/模块), 统计 1s/5s — 昂贵任务降频。[模式: 分级周期]
数据流: aeCreateTimeEvent(1ms) → 每 100ms 执行 → 分级任务 → 返回下次间隔。

### 2. hz 自适应 — 负载响应

场景: 客户端多了 cron 为什么变快?上限多少?
源码路径:
- `server.c:1282-1295` — `while (clients / hz > MAX_CLIENTS_PER_CLOCK_TICK(200)) hz *= 2` — **每 tick 客户端配额 200**
- 范围: CONFIG_DEFAULT_HZ=10 / MIN=1 / MAX=500 (server.h:100-102)
关键设计: 配额闭环 (q4): 客户端多 → hz 翻倍 (上限 500) — 每 tick 工作量稳定; 客户端少 → 回默认。[模式: 负载自适应]
数据流: 客户端数 / hz > 200 → hz×2 → clientsCron 每 tick 处理量稳定。

### 3. 命令表 — 生成式定义 + 双字典

场景: 122 个命令从哪来?rename-command 影响谁?
源码路径:
- commands.c (13 行薄壳) → **commands.def (11235 行, 生成文件, 122 命令)**
- `server.c:3075-3095` (populateCommandTable) — 遍历 → populateCommandStructure (sentinel 过滤 L3034 / ACL 隐式分类 / 直方图延迟分配) → **双字典: server.commands + server.orig_commands** (rename-command 免疫)
- redisCommand 结构 (server.h:2341+): 声明字段 (doc) + proc/arity/flags (执行) + key_specs (cluster/ACL)
关键设计: 生成式 + 双注册 (q5): JSON 命令定义单一来源 (文档/ACL/key_specs 同源); orig_commands 让 rename-command 后 ACL/文档不受影响。[模式: 单一来源生成]
数据流: commands.def → populateCommandTable → 双字典 → lookupCommand。

### 4. beforeSleep — 每轮必做的批处理

场景: 事件处理前都做什么?和 cron 什么关系?
源码路径:
- `server.c:1637+` (beforeSleep, aeSetBeforeSleepProc L2772 注册): **flushAppendFileOnly (每轮 AOF)** + handleClientsWithPendingWrites (客户端写) + handleClientsBlockedOnKeys (R-26) + 时间缓存 + 计时
- 与 cron 分工: **cron = hz 定时; beforeSleep = 事件驱动 (每轮必做)** — 高吞吐路径的批处理点
关键设计: 双时间面 (q6): 事件到达才触发 beforeSleep (AOF/写面批量), 周期任务归 cron — 两条时间轴互补。[模式: 事件批处理]
数据流: 事件循环轮转 → beforeSleep (AOF/写/阻塞键) → 事件处理 → 下一轮。

### 负面空间 — 周期引擎刻意不做的事

- **不做实时调度**: 全部尽力而为 (无硬实时保证)
- **不做多线程 cron**: 单线程串行 (hz 内任务顺序执行)
- **不做任务优先级抢占**: run_with_period 是"跳过"不是"抢占"
- **不做动态任务增删**: 任务集编译期固定 (模块事件除外)

→ 引出: 键空间 (db) 是全部数据结构的宿主 — kvstore 分片与 expires 联动 → [[R-21-db]]
