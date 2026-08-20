# R-4 SDS — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 1 篇 (+harness) | sds.c (1473)+sds.h (264)
> 基线: REDIS-PLAN R-4 — 前置: **R-33 (zmalloc usable 联动)** — 展开 指针即对象→分级头→扩容三路→预分配→缩容→零拷贝→复用

---

## §0.8

- 🔴 A，1篇 — 指针即对象(**sds=char*, sds.h:20; 头部 packed 内嵌 buf 前, s[-1]=flags; inline 访问族 sdslen/avail/alloc L64-193**) → 分级头(**sdsReqType 阈值 31/255/65535/2^32 L40-52; sdshdr5 仅 1B (3bit type+5bit len) 从不作 struct (sds.h:22), 空串/扩容强制跳 8 L87/244; would_regrow 禁降 5 L316-318**) → 扩容三路(**avail 够→原地; oldtype==type→realloc (L248); 升级→malloc+memcpy+free 注释 "can't use realloc" L253-254**; 缩容 use_realloc="伪降型" 保留旧头只缩分配 L327-343) → 预分配(**greedy: <1MB→2× / ≥1MB→+1MB (SDS_MAX_PREALLOC sds.h:13); NonGreedy=querybuf 读取按需 L277-279, networking.c:2401,2698**) → usable 联动(**创建/扩容 alloc=usable 免费膨胀 L93-105/248-266; sdsResize je_nallocx 预查询免无谓 realloc L332-338**) → 双标准(**总 \0 结尾 (printf 兼容 L142) + len 为准 (二进制安全, 中间 \0 合法 L73-80); sdsupdatelen 手动修复 L191-194**) → 零拷贝(**sdsIncrLen: MakeRoomFor→read 直写→递增 len; 断言守卫正负增量 L399-440; querybuf 消费 networking.c:2431/2727; 负增量去 CRLF**) → 复用(**sdsclear 留缓冲 (aof_buf 每轮复用 aof.c:1216); SDS_NOINIT 免 memset L97-98; 导出分配器 sds_malloc/realloc/free (sds.h:256-258, Lua 宿主)**)
- 设计模式: [模式: 指针即对象+分级头+三路扩容+双标准+零拷贝桥]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| sds.h:20-79 | 指针即对象 | sds=char*, s[-1] flags 分派; packed 头部内嵌 | High |
| sds.c:40-52,87,241-244; sds.h:22-27 | 分级头 | 5 类型阈值; sdshdr5 1B/上限 31/从不增长; 空串强制 8 | High |
| sds.c:217-268 | 扩容三路 | avail→原地; 同型 realloc; 升级 malloc+memcpy+free | High |
| sds.c:327-343 | 伪降型 | use_realloc=保留旧头只缩分配; s[-1] 不更新; 真换头只在升级/降到 8/5 | High |
| sds.c:232-237,277-279; networking.c:2401,2698 | 预分配 | greedy 2×/+1MB (1MB 分界); NonGreedy=querybuf 按需 | High |
| sds.c:93-105,248-266,332-338 | usable 联动 | alloc=usable 免费膨胀; je_nallocx 免无谓 realloc | High |
| sds.c:73-80,142,463-472 | 双标准 | \0 结尾 (C 兼容) + len 为准 (二进制安全) | High |
| sds.c:385-440; networking.c:2431,2727 | 零拷贝 | sdsIncrLen read 直写; 断言守卫; 负增量去 CRLF | High |
| sds.c:200-203,97-98; sds.h:256-258; aof.c:1216 | 复用 | sdsclear/SDS_NOINIT/导出分配器 | High |

---

## 02-04 聚合+分类+聚类 (1篇+harness)

**1篇理由**: SDS 是单机制闭环 (布局→分级→扩容→预分配→缩容→零拷贝), 1篇 (~75行) 按"指针即对象→分级头→扩容→预分配→缩容→usable 联动→零拷贝→复用"展开; harness 验证分级选型/扩容路径/预分配/伪降型。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 指针即对象 + s[-1] 分派 | 🔴 | **为什么🔴**: 布局根基 |
| P1-2 | 分级头 + sdshdr5 特殊性 | 🔴 | **为什么🔴**: 空间策略 |
| P1-3 | 扩容三路 + 伪降型 | 🔴 | **为什么🔴**: 核心算法 |
| P1-4 | 预分配 greedy/NonGreedy | 🔴 | **为什么🔴**: 摊还策略 |
| P2-1 | usable 联动 + nallocx 优化 | 🟡 | **为什么🟡**: R-33 接力 |
| P2-2 | 双标准 (二进制安全) | 🟡 | **为什么🟡**: 契约面 |
| P2-3 | 零拷贝 sdsIncrLen | 🟡 | **为什么🟡**: 网络场景 |
| P3-1 | 复用三件 (clear/NOINIT/导出) | 🟢 | **为什么🟢**: 微观优化 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **布局与分级** | 🔴 | 根基 |
| B | **扩容与预分配** | 🔴 | 算法核心 |
| C | **联动与契约** | 🟡 | 跨层 |
| D | **复用微优化** | 🟢 | 工程 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 指针即对象 | sds=char* + packed 头部内嵌, s[-1] flags 分派 — C 生态零摩擦 (printf/系统调用直传), 头部访问 O(1) | sds.h:20-79; sds.c:101-142 |
| q2 | 分级头 | 5 类型阈值 31/255/65535/2^32; sdshdr5 1B (5bit len) 静态串专用, 无 alloc 字段 → 增长型强制跳 8 | sds.c:40-52,87,241-244 |
| q3 | 扩容三路 | 同型 realloc (原地最优); 升级 malloc+memcpy+free (buf 偏移变, 不能 realloc); 缩容 use_realloc=**伪降型** (保留旧头只缩 alloc, s[-1] 不更新) | sds.c:217-268,327-343 |
| q4 | 预分配 | <1MB→2× (摊还 O(1)) / ≥1MB→+1MB (浪费封顶); NonGreedy=querybuf 按需读取防恶意膨胀 | sds.c:232-237; networking.c:2401,2698 |
| q5 | usable 联动 | 创建/扩容 alloc=分配器实际 (免费膨胀); sdsResize je_nallocx 预查询免"无意义 realloc" | sds.c:93-105,332-338 |
| q6 | 双标准 | 总 \0 结尾 (printf 兼容) + len 为准 (中间 \0 合法) — 兼容不丢安全不失 | sds.c:73-80,142 |
| q7 | 零拷贝 | sdsIncrLen: 扩好→read 直写→递增 (含负向去 CRLF); 断言守卫防越界 — querybuf 零中间拷贝 | sds.c:385-440; networking.c:2431,2727 |
| q8 | 复用三件 | sdsclear (aof_buf 每轮复用) / SDS_NOINIT (免 memset) / 导出分配器 (Lua 宿主互通) | sds.c:200-203; aof.c:1216; sds.h:256-258 |

→ 引出 R-3: Dict 的键是 sds — 键哈希 (siphash 对内容计算) 与键值存取 → [[R-3-Dict]]
