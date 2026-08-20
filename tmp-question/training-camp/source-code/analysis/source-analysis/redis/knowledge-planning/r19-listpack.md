# R-19 listpack — 知识规划 (knowledge-planning)

> 项目: Redis 7.4.2 | 🔴 A / 1 篇 (+harness) | listpack.c (3150)+listpack.h (95)
> 基线: REDIS-PLAN R-19 — 前置: **R-33 (分配) + R-4 (sds 内容)** — 展开 布局→backlen 无级联→编码族→整数嗅探→写路径→双向遍历→批量→完整性→消费阈值
> **认知修正**: REDIS-PLAN "级联更新"表述错误 — listpack 的设计卖点是**消除级联** (对比 ziplist 的 prevlen 级联传播)

---

## §0.8

- 🔴 A，1篇 — 布局(**6B 头 (4B total+2B numele) + entry* + 0xFF EOF; entry=[编码+数据][backlen 自身长度 1-5B]**) → **无级联** (**backlen 存自身长度+固定 5B 空间: 插入/替换零传播, 只改头部; 对比 ziplist prevlen 存前驱+1B/5B 可变→级联 L55-69**; lpPrev O(1) 跳回 L473-482) → 编码族(**9 种: 整数 7/13/16/24/32/64BIT (2-10B) + 字符串 6/12/32BIT (1-5B 头); 前缀位分层 0/10/110/1110/1111 L30-82**) → 整数嗅探(**lpEncodeGetType→lpStringToInt64 (string2ll 移植, 严格语义 L154-179): "123"→7BIT_INT 省空间**) → 写路径(**lpInsert 三合一 (插入/删除/替换, L821-968): delete→REPLACE 零长, AFTER→跳转 BEFORE; 扩先 realloc 后 memmove / 缩先 memmove 后 realloc L893-914; UINT32_MAX 上限+LISTPACK_MAX_SAFETY_SIZE 1GB**) → 批量(**lpBatchInsert 单次 realloc+memmove, 栈 3 元素缓冲 L993-1080**) → 惰性计数+校验(**numele UNKNOWN (65535) 全扫描+回填 L505-521; lpValidateIntegrity 头部一致性+deep 逐元素 L1541+ — RDB 加载防御**) → 消费(**hash ≤512 字段/值≤64B (config.c:3215-3223), zset ≤128/64B, stream 消息 — 超阈值单向转 dict t_hash.c:605,893**)
- 设计模式: [模式: 自描述布局 (长度放自己身上)+前缀编码+统一写路径]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| listpack.c:30-82,434-446 | 编码族 | 9 种编码前缀分层; 整数 2-10B/字符串 1-5B 头 | High |
| listpack.c:452-482,869-872; ziplist.c:55-69 | backlen | 自身长度+固定 5B = 无级联; lpPrev O(1); ziplist prevlen 级联对照 | High |
| listpack.c:154-179,660+ | 嗅探 | lpStringToInt64 严格解析; 字符串→整数编码 | High |
| listpack.c:821-968 | 写路径 | 三合一; realloc/memmove 顺序; 上限检查 | High |
| listpack.c:993-1080 | 批量 | 单次 realloc+memmove; 栈缓冲 | High |
| listpack.c:505-521,1541-1553 | 惰性+校验 | numele 回填; 完整性双保险 | High |
| config.c:3215-3223; t_hash.c:487,605,893 | 消费 | 阈值 512/128/64B; 单向转 dict | High |

---

## 02-04 聚合+分类+聚类 (1篇+harness)

**1篇理由**: listpack 是单机制闭环 (布局→编码→写→遍历), 1篇 (~75行) 按"布局与 backlen→编码族→嗅探→写路径→遍历→批量→完整性→消费"展开; harness 验证编码/无级联/遍历/写路径。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | backlen 无级联设计 (vs ziplist) | 🔴 | **为什么🔴**: 核心卖点 |
| P1-2 | 编码族 (9 种前缀分层) | 🔴 | **为什么🔴**: 空间策略 |
| P1-3 | lpInsert 三合一写路径 | 🔴 | **为什么🔴**: 核心算法 |
| P1-4 | 整数嗅探 | 🔴 | **为什么🔴**: 空间优化 |
| P2-1 | 双向遍历 (lpPrev O(1)) | 🟡 | **为什么🟡**: 访问面 |
| P2-2 | 批量接口 | 🟡 | **为什么🟡**: 构建优化 |
| P2-3 | 惰性计数+完整性 | 🟡 | **为什么🟡**: 可靠性 |
| P3-1 | 消费阈值转换 | 🟢 | **为什么🟢**: 策略面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **布局与编码** | 🔴 | 核心 |
| B | **写路径** | 🔴 | 算法 |
| C | **遍历与构建** | 🟡 | 访问 |
| D | **可靠性与消费** | 🟡 | 工程 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 无级联 | backlen 存**自身**长度+固定 5B = 插入/替换零传播 (只改头部); ziplist prevlen 存前驱+1B/5B 可变 = 级联 — "长度放自己身上"的根本改进 | listpack.c:452-482,869-872; ziplist.c:55-69 |
| q2 | 编码族 | 9 种前缀分层 (0/10/110/1110/1111): 整数 2-10B/字符串 1-5B 头; 小值空间最优 | listpack.c:30-82 |
| q3 | 嗅探 | 字符串写入先严格 int64 嗅探 (string2ll): 能解析→整数编码省 50%+; 无损往返 | listpack.c:154-179 |
| q4 | 写路径 | lpInsert 三合一 (删除=替换零长/AFTER 转 BEFORE); 扩先 realloc 后 memmove; UINT32_MAX 上限 | listpack.c:821-968 |
| q5 | 双向遍历 | lpPrev O(1): 前驱 backlen 紧贴当前 entry 前 → 解码跳回; 1-5B 前缀编码无歧义 | listpack.c:335-374,473-482 |
| q6 | 批量 | lpBatchInsert 单次 realloc+memmove (O(N) vs O(N²)); 栈 3 元素缓冲 | listpack.c:993-1080 |
| q7 | 惰性+校验 | numele 超 65535 降级全扫描+回填; lpValidateIntegrity 头部一致性+deep 逐元素 (RDB 防御) | listpack.c:505-521,1541+ |
| q8 | 消费阈值 | hash ≤512 字段/值≤64B, zset ≤128/64B 用 listpack; 超阈值单向转 dict; stream 消息存储 | config.c:3215-3223; t_hash.c:605,893 |

→ 引出 R-7: intset 是另一条紧凑路线 (整数有序数组) → [[R-7-intset]]
