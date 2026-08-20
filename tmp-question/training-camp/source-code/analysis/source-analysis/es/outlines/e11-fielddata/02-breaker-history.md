# E-11 FieldData 篇 2/2 — 内存保护与历史: 断路器与 5.0 迁移

> 前置: [[E-11-fielddata-01]] (读取面) | 复用: — | 对照: [[rd6-localcachedmap]] (客户端缓存) [[r23-evict]] (Redis 淘汰) | 引出: [[E-12-aggregations]]
> 🟡 B | 来源: HierarchyCircuitBreakerService.java:81-92 + CircuitBreaker.java:84 + TextFieldMapper.java:249 + KeywordFieldMapper.java:143
> 定位: FieldData 卷收尾 — 回答"fielddata 怎么防 OOM? 为什么 5.0 迁到 docValues?"

**读者处境**: 面试官问 "ES 聚合把堆打爆过吗? fielddata 是什么?" 你答 "断路器 40%" — 但再问 "为什么 5.0 要把聚合从 fielddata 迁到 docValues? 事故什么样?" 你只记得"OOM"三个字。这篇是内存保护 + 演进史的完整答案, 收束 FieldData 域。

### 1. 问题引入 — 堆内存的三道防线

场景: 聚合/排序要在堆里放字段数据 — 无限放会 OOM, 怎么办?
- 防线 1: docValues (磁盘列式) — 按需加载
- 防线 2: 断路器 (40% heap 硬限制)
- 防线 3: 5.0 迁移 (让 fielddata 只服务 text 字段)
- 本篇问题: 断路器机制 (Q3) / 迁移历史 (Q4) / 与客户端缓存对照 (Q7)

### 2. 断路器 — 40% 硬限制

场景: fielddata 加载多少算超限?
- 设置 (HierarchyCircuitBreakerService.java:81-92): `indices.breaker.fielddata.limit` 默认 **"40%"** 即 **JVM heap 的 40%** (HierarchyCircuitBreakerService.java:83) + overhead **1.03** (HierarchyCircuitBreakerService.java:89)
- **运维视角**: 线上 CircuitBreakingException = fielddata 超 40% heap — 处理: 调高 limit (慎) / 降低聚合并发 / 排查高基数字段 (改用 keyword+docValues) / 清缓存
- 接口 (CircuitBreaker.java:84): `addEstimateBytesAndMaybeBreak(bytes, label) throws CircuitBreakingException` — 超限即抛
- 消费点: loadGlobalDirect (AbstractIndexOrdinalsFieldData.java:144) 取 FIELDDATA breaker; GlobalOrdinalsBuilder 长遍历每 64K 次检查 (L65)
- 层次: PARENT (CircuitBreaker.java:26) / FIELDDATA (CircuitBreaker.java:31) / REQUEST (CircuitBreaker.java:39) / IN_FLIGHT_REQUESTS (CircuitBreaker.java:44)

### 3. 5.0 迁移 — fielddata OOM 事故史

场景: 为什么现代聚合默认不用 fielddata?
- 历史: 5.0 前 text 聚合走 fielddata (加载倒排词条到堆) → 高基数字段 OOM (经典事故)
- 迁移: doc_values 默认 true (KeywordFieldMapper.java:143) — 磁盘列式按需加载
- 现代: text 字段 fielddata 默认 **false** (TextFieldMapper.java:249) — 显式开启 + 断路器保护
- 演进证据: doc_values 默认值变化 commit (7290b2dc916)
- 面试记忆点: "5.0 前聚合吃堆 (fielddata), 5.0 后吃磁盘 (docValues) + 断路器兜底"

### 4. 与客户端缓存对照 — 两类缓存哲学

场景: 服务端 fielddata 和客户端 RLocalCachedMap 都是缓存, 差在哪?
- 服务端: 只读磁盘数据的堆缓存 — 无一致性需求, 只有容量问题 (断路器)
- 客户端 ([[rd6-localcachedmap]]): 可变分布式数据的本地副本 — 需要失效协议 (INVALIDATE/UPDATE)
- 对照结论: "服务端缓存管内存 (断路器), 客户端缓存管一致 (失效协议)" — 维度不同
- 对照 Redis 淘汰 ([[r23-evict]]): 服务端内存管理的另一面 (LRU/LFU vs 断路器拒绝)

### 5. 收束 — FieldData 域总结

- 读取面 (篇 1): load/loadGlobal + global ordinals + 排序/缓存
- 保护 (本篇): 断路器 40% + 5.0 docValues 迁移
- 终极结论: fielddata = "docValues 的堆内加速层, 断路器限容, 5.0 后只服务 text" — 聚合安全的双保险
- 引出: E-12 Aggregations (global ordinals 消费方)

### 核心悬念
"为什么 5.0 前聚合会 OOM, 现在不会?" — 因为聚合从"加载倒排词条到堆" (fielddata) 迁到"磁盘列式按需读" (docValues), 且 40% 断路器硬限制 + 1.03 开销系数兜底。

### 概念依赖链
Q3 断路器 → Q4 5.0 迁移 → Q7 客户端对照 → (E-12 衔接)

### 源码锚点清单
- HierarchyCircuitBreakerService.java:81-92 (FIELDDATA limit) / 83 ("40%") / 89 (overhead 1.03)
- CircuitBreaker.java:26-44 (层次) / 84 (addEstimateBytesAndMaybeBreak)
- AbstractIndexOrdinalsFieldData.java:148 (breaker 消费点)
- GlobalOrdinalsBuilder.java:65 (64K 检查点)
- TextFieldMapper.java:249 (fielddata 默认 false)
- KeywordFieldMapper.java:143 (docValues 默认 true)
