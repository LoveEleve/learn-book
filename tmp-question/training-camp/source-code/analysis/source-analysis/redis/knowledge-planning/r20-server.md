# R-20 server 骨架 — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 2 篇 (+harness) | server.c (7256)+config.c (3413)+commands.c/def
> 基线: REDIS-PLAN R-20 — 前置: **R-33/R-3/R-1 (装配) + R-2 (宿主)** — 展开 启动管线→初始化矩阵→配置→生产面 | cron 分级→hz 自适应→命令表→beforeSleep

---

## §0.8

- 🔴 A，2篇 — 篇 1 启动与初始化: **main 管线 (依赖序: OOM handler L6970 → 哈希 seed L6985 → 哨兵先于配置 L7006-7012 → 配置 → initServer → aeMain L7251)** → **initServer 矩阵 (信号/事件循环 L2657/键空间 kvstore 分片 L2665-2680 (cluster 14bit)/list 族/cron 注册 L2757)** → **配置宏 DSL (createIntConfig 族 L2244+: 类型/范围/默认/验证/apply 五合一; 53 Bool+41 Int+36 String+20 Enum+13 SizeT+9 Special; CONFIG SET 失败回滚 restoreBackupConfig L760-780)** → **生产面 (systemd READY L7230 / CPU 亲和 L7250 / OOM score L7251 / watchdog 卡死检测 L1281)**; 篇 2 周期引擎: **serverCron 时间分级 (每 tick: clientsCron/databasesCron/updateDictResizePolicy; 100ms: 复制/模块/抽样; 1s: receiveChildInfo/tracking; 5s: 日志; 返回 1000/hz L1538)** → **hz 自适应 (clients/hz > 200 → hz×2, 上限 500 L1283-1295)** → **命令表 (commands.def 11235 行/122 命令 → populateCommandTable 双字典注册 (rename 免疫) L3075-3095)** → **beforeSleep 双面 (每轮 AOF flush/客户端写/阻塞键 L1637+; cron=定时 vs beforeSleep=事件驱动)** → **watchdog (cron 缺席 → SIGALRM → 栈 dump)**
- 设计模式: [模式: 依赖序编排+时间分级调度+宏 DSL 配置+声明式命令表]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| server.c:6917-7256 | main 管线 | 依赖序; 哨兵先于配置; 模式分支 | High |
| server.c:2591-2772 | initServer | 矩阵装配; kvstore 分片 | High |
| config.c:2244+,760-780 | 配置 | 宏 DSL 五合一; 失败回滚 | High |
| server.c:1273-1540 | cron | 时间分级; 返回 1000/hz | High |
| server.c:1282-1295; server.h:100-103 | hz | 配额闭环; 上限 500 | High |
| commands.c/def; server.c:3075-3095 | 命令表 | 生成式; 双字典 | High |
| server.c:1637-1800,2772 | beforeSleep | 双面批处理 | High |
| server.c:1281,2232,6783,7230-7251 | 生产面 | systemd/OOM/亲和/watchdog | High |

---

## 02-04 聚合+分类+聚类 (2篇+harness)

**2篇理由**: 8 闭环按主题聚类: 篇 1 启动与初始化 (q1/q2/q7/q8) — "从 argv 到就绪"; 篇 2 周期引擎与命令表 (q3/q4/q5/q6) — "运行时心跳"。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | main 依赖序编排 | 🔴 | **为什么🔴**: 生命周期根基 |
| P1-2 | initServer 矩阵 (kvstore 分片) | 🔴 | **为什么🔴**: 装配核心 |
| P1-3 | serverCron 时间分级 | 🔴 | **为什么🔴**: 周期引擎 |
| P1-4 | 配置宏 DSL | 🔴 | **为什么🔴**: 全配置基础 |
| P2-1 | hz 自适应 | 🟡 | **为什么🟡**: 负载响应 |
| P2-2 | 命令表生成式 | 🟡 | **为什么🟡**: 注册机制 |
| P2-3 | beforeSleep 双面 | 🟡 | **为什么🟡**: 批处理 |
| P3-1 | 生产面 (systemd/OOM/watchdog) | 🟢 | **为什么🟢**: 运维 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **启动与装配** | 🔴 | 根基 |
| B | **周期引擎** | 🔴 | 运行时 |
| C | **配置与命令表** | 🟡 | 框架 |
| D | **生产面** | 🟢 | 运维 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | main 管线 | 依赖序: OOM→seed→哨兵先于配置→initServer→aeMain; 哨兵先行是"配置填充依赖结构"硬约束 | server.c:6917-7256 |
| q2 | initServer | 矩阵装配: 信号/事件循环/kvstore 键空间分片 (cluster 14bit)/list 族/cron 注册 | server.c:2591-2772 |
| q3 | cron 分级 | 每 tick (过期/COW) / 100ms (复制/模块) / 1s (COW 报告) / 5s (日志); 返回 1000/hz | server.c:1273-1540 |
| q4 | hz 自适应 | clients/hz > 200 → hz×2 (上限 500); 每 tick 配额稳定 | server.c:1282-1295; server.h:100-103 |
| q5 | 命令表 | commands.def 生成 (122 命令) → 双字典注册 (rename 免疫); sentinel 过滤/ACL 分类 | commands.c/def; server.c:3075-3095 |
| q6 | beforeSleep | 每轮 AOF flush/客户端写/阻塞键; cron=定时 vs beforeSleep=事件驱动双面 | server.c:1637-1800 |
| q7 | 配置 DSL | 宏族五合一 (类型/范围/默认/验证/apply); CONFIG SET 失败回滚 | config.c:2244+,760-780 |
| q8 | 生产面 | systemd READY/CPU 亲和/OOM score/watchdog 卡死检测 | server.c:1281,2232,6783,7230-7251 |

→ 引出 R-21: db 键空间是全部数据的宿主 → [[R-21-db]]
