# R-20 server 骨架 — Pass 1 探索笔记

> 域: R-20 server 骨架+cron (巨型域) | 🔴 方案 A | 2026-08-13
> 源码: src/server.c (7256) + config.c (3413) + commands.c | Redis 7.4.2

## 继承树/调用图

```
main (L6917): 参数解析 → sentinel init (L7006-7012) → redis-check 模式 (L7015-7021)
  → 配置加载 (loadServerConfig) → createSharedObjects (L6970 前) → initServer
  → 就绪通知 (systemd) → maxmemory 警告 → CPU 亲和 → setOOMScoreAdj → aeMain (L7251) → 退出

initServer (L2591):
  信号 (SIGHUP/SIGPIPE 忽略) → createSharedObjects → aeCreateEventLoop
  → server.db = zmalloc (kvstoreCreate 键空间 7.x + expires + hexpires + blocking/ready/watched keys)
  → list 族初始化 (clients/slaves/monitors/pending_write/...)

populateCommandTable (L3075): redisCommandTable (commands.c) 遍历注册 → lookupCommand
serverCron (L1273-1540):
  watchdog → hz 自适应 (dynamic_hz, MAX_CLIENTS_PER_CLOCK_TICK=200)
  → lruclock 更新 → cronUpdateMemoryStats → clientsCron → databasesCron (R-21)
  → updateDictResizePolicy (R-3) → run_with_period 分级:
    100ms: 时间缓存/内存抽样/modulesCron/replicationCron
    1000ms: receiveChildInfo (COW)/tracking resize/BGSAVE scheduled/replicationCron
    5000ms: 统计日志
  → 返回 1000/server.hz
beforeSleep (L1637): 事件循环前置 — cron 计时/客户端缓冲写/AOF 写/IO 写

配置 (config.c): createXxxConfig 族 (INT/STRING/BOOL/SIZE/MEMORY/ENUM...)
  CONFIG_DEFAULT_HZ=10 / MIN=1 / MAX=500 (server.h:100-102)
```

## 基本元素分解

1. **启动管线**: main 的阶段编排 (参数/哨兵/配置/初始化/就绪/事件循环)
2. **初始化矩阵**: initServer 的分配/注册 (信号/事件循环/键空间/list 族)
3. **周期引擎**: serverCron — hz 自适应 + run_with_period 时间分级
4. **命令表**: commands.c 静态表 → populateCommandTable 动态注册
5. **事件循环前置**: beforeSleep (每轮事件前的批处理)
6. **配置系统**: createXxxConfig 族 + CONFIG SET 动态

## 标记问题 (10 个)

1. main 启动管线的阶段划分 — 什么必须最早? (sentinel 先于配置?)
2. initServer 的初始化矩阵 — kvstore 键空间 (7.x) vs dict?
3. serverCron 的时间分级 (run_with_period 100ms/1s/5s) — 什么任务什么频率?
4. hz 自适应 (dynamic_hz) — 客户端数怎么影响频率?
5. populateCommandTable — 命令表从哪来 (commands.c 生成)?
6. beforeSleep 的职责 — 为什么不在 cron 里?
7. 配置系统 createXxxConfig 族 — 类型分类? CONFIG SET 动态?
8. 就绪/监督 (systemd/OOM score/CPU 亲和) — 生产面
9. watchdog 机制
10. 返回 1000/server.hz 的语义 (下一次 cron 调度)

## 时空溯源 (代码内痕迹)

- main/initServer 结构自 2009 年延续 (稳定骨架)
- 7.x: server.db 从 dict 升级 kvstore (键空间分片); hexpires (ebuckets)
- hz 自适应: 早期固定 10 → dynamic_hz 演进
- run_with_period 宏: 历史稳定
- config.c: 配置族重构 (createXxxConfig 统一, 早期散落)

## 大域拆分规划 (01 §大域)

8 闭环 → **2 篇**:
- 篇 1 (启动与初始化): q1 (main 管线) + q2 (initServer 矩阵) + q7 (配置系统) + q8 (就绪/监督)
- 篇 2 (周期引擎与命令表): q3 (cron 时间分级) + q4 (hz 自适应) + q5 (命令表) + q6 (beforeSleep) + q9 (watchdog) + q10 (返回语义)
