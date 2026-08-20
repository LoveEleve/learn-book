# R-1 redisObject — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 1 篇 (+harness) | object.c (1677)+server.h robj (903-911)
> 基线: REDIS-PLAN R-1 — 前置: **R-33/R-4/R-19/R-7/R-6/R-5 (全部编码)** — 展开 外壳→EMBSTR→INT→优化链→引用→共享池→解码→可观测

---

## §0.8

- 🔴 A，1篇 — 外壳(**16B: type:4+encoding:4+lru:24+refcount+ptr server.h:903-911; OBJ_STATIC_REFCOUNT/OBJ_SHARED_REFCOUNT L901-902**) → EMBSTR(**同 chunk: robj+sdshdr8+buf 一次分配; 16+3+44+1=64B 恰好 jemalloc 64B arena (L99-101); 不可变 (追加转 RAW)**) → INT(**ptr 直接存值零分配 L128-140; 共享整数 (<10000) 优先; maxmemory 下禁共享 (NO_SHARED_INTEGERS, 需私有 LRU L627-635)**) → 优化链(**tryObjectEncodingEx: refcount>1 不编码 → INT (≤20 字符 string2l) → EMBSTR (≤44B); 写入时 O(1) 降级 L607-683**) → 引用计数(**incr/decr 三态: 1→分派释放 (freeXxxObject) / >1→-- / 特殊值不碰 L349-377; makeObjectShared L56-60**) → 共享池(**shared.integers[10000] (server.c:1992-1995, INT 编码+makeObjectShared) + 响应串族 (+OK/错误/批量前缀 L1847+); 回复路径零分配**) → 解码(**getDecodedObject: sds 直返 (incr)/INT→ll2string 临时 L685-697; 比较直接比值免解码 L706-715**) → LRU/LFU(**lru 24bit 双用途: LRU 分钟时钟 / LFU 高 16bit 时间+低 8bit 频率 L32-43; 共享对象无 lru**) → 可观测(**objectCommand: ENCODING/REFCOUNT/IDLETIME/FREQ L1442+**)
- 设计模式: [模式: 统一外壳+编码降级链+共享池+引用计数]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| server.h:896,901-911 | 外壳 | 16B 位域; 特殊 refcount 值 | High |
| object.c:71-107,99-101 | EMBSTR | 同 chunk; 44=64B arena 数学; 不可变 | High |
| object.c:128-140,159,607-678 | INT/优化链 | 零分配; 共享条件双路径; 写入时降级 | High |
| object.c:56-60,349-377 | 引用 | 三态; 分派释放 | High |
| server.c:1847+,1992-1995; server.h:108 | 共享池 | 10000 整数; 响应串; 免计数 | High |
| object.c:685-715 | 解码 | 按需临时; 比较免解码 | High |
| object.c:32-43 | LRU/LFU | 双用途; 共享无 lru | High |
| object.c:1442+ | 可观测 | OBJECT 命令族 | High |

---

## 02-04 聚合+分类+聚类 (1篇+harness)

**1篇理由**: redisObject 是单机制闭环 (外壳→编码→引用→共享), 1篇 (~80行) 按"外壳→EMBSTR→INT→优化链→引用→共享池→解码→可观测"展开; harness 验证 EMBSTR 尺寸数学/INT 零分配/优化链。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 16B 外壳 + 位域 | 🔴 | **为什么🔴**: 根基 |
| P1-2 | EMBSTR 64B arena 数学 | 🔴 | **为什么🔴**: 缓存优化 |
| P1-3 | INT 零分配 + 共享条件 | 🔴 | **为什么🔴**: 内存优化 |
| P1-4 | 优化链 (tryObjectEncoding) | 🔴 | **为什么🔴**: 写入路径 |
| P2-1 | 引用计数三态 | 🟡 | **为什么🟡**: 生命周期 |
| P2-2 | 共享池 (10000+响应串) | 🟡 | **为什么🟡**: 启动优化 |
| P2-3 | 解码/LRU 双用途 | 🟡 | **为什么🟡**: 访问面 |
| P3-1 | OBJECT 可观测 | 🟢 | **为什么🟢**: 运维 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **外壳与编码** | 🔴 | 核心 |
| B | **引用与共享** | 🟡 | 生命周期 |
| C | **访问与可观测** | 🟡 | 工程 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 外壳 | 16B 位域 (type:4+encoding:4+lru:24+refcount+ptr); 特殊 refcount (共享/栈上) | server.h:896,901-911 |
| q2 | EMBSTR | 同 chunk 分配: 16+3+44+1=64B 恰好 jemalloc 64B 桶; 一次 malloc+同缓存行; 不可变 | object.c:71-107 |
| q3 | INT | ptr 存值零分配; 共享整数池优先; maxmemory 禁共享 (私有 LRU) | object.c:128-140,627-635 |
| q4 | 优化链 | 写入时 O(1): refcount>1 跳过 → INT (≤20 字符) → 共享/EMBSTR (≤44B) | object.c:607-683 |
| q5 | 引用 | 三态: 1→分派释放 / >1→-- / 特殊值不碰; 共享对象免计数线程安全 | object.c:56-60,349-377 |
| q6 | 共享池 | 10000 INT 整数 (160KB) + 响应串族 — 回复路径零分配 | server.c:1847+,1992-1995 |
| q7 | 解码/LRU | getDecodedObject 按需临时; lru 24bit 双用途 (LRU/LFU); 共享无 lru | object.c:32-43,685-715 |
| q8 | 可观测 | OBJECT ENCODING/REFCOUNT/IDLETIME/FREQ — 编码状态排查入口 | object.c:1442+ |

→ 引出 R-20: server 骨架 — 键空间是 robj 的宿主 → [[R-20-server]]
