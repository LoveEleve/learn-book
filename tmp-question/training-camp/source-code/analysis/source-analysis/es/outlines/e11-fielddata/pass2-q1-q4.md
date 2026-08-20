# E-11 闭环笔记 Q1-Q4: 加载契约/global ordinals/断路器/历史

## Q1: load vs loadGlobal — 段内序号 vs 全局序号

假设: load 按段加载 (per-segment ordinals), loadGlobal 跨段合并 (global ordinals) — terms 聚合需要全局去重。

验证过程:
- Read IndexFieldData 接口 (IndexFieldData.java:43-64): `FD load(LeafReaderContext context)` (IndexFieldData.java:59) — 按段; `loadDirect` (IndexFieldData.java:64) 直载; loadGlobal/loadGlobalDirect (IndexFieldData.java:250-252) 跨段
- GlobalOrdinalsIndexFieldData (GlobalOrdinalsIndexFieldData.java:44): 包装 atomicFD[] (各段) + ordinalMap (合并映射)
- 何时需要 global: terms 聚合跨段去重计数 (E-12 衔接); 排序/单段查询只需 per-segment

代码类型: Interface (加载契约)

结论: **load = 单段 docValues 读取 (per-segment ordinals), loadGlobal = 跨段合并 (global ordinals + OrdinalMap) — 聚合需要全局去重编号, 排序只需段内; loadGlobal 是"重"操作 (全段扫描+合并)**。IndexFieldData.java:59,250-252

## Q2: GlobalOrdinalsBuilder — 跨段合并与记账

假设: build 逐段加载 → TermsEnum 包装 (断路器检查点) → OrdinalMap.build 合并 → 内存记账。

验证过程:
- Read GlobalOrdinalsBuilder.build (GlobalOrdinalsBuilder.java:39-90): 逐段 load (GlobalOrdinalsBuilder.java:51-52) → FilterTermsEnum 包装 (GlobalOrdinalsBuilder.java:59-72, **每 65536 次 next 检查断路器** L65) → `OrdinalMap.build` (GlobalOrdinalsBuilder.java:72) → `memorySizeInBytes = ordinalMap.ramBytesUsed()` (GlobalOrdinalsBuilder.java:73) → `breaker.addWithoutBreaking(memorySizeInBytes)` (GlobalOrdinalsBuilder.java:74)
- 返回 GlobalOrdinalsIndexFieldData (GlobalOrdinalsIndexFieldData.java:78-90)

代码类型: Algorithmic (跨段合并算法)

结论: **GlobalOrdinalsBuilder = 加载全段 → 逐段 termsEnum 遍历 (每 64K 次查断路器, 防止长遍历 OOM) → OrdinalMap 合并映射 → ramBytesUsed 记账 → addWithoutBreaking (已建完才记账, 不中断)**。GlobalOrdinalsBuilder.java:51-76

## Q3: 断路器 — fielddata 内存保护

假设: fielddata 用独立断路器 (FIELDDATA, 默认 40% heap), 超限抛 CircuitBreakingException。

验证过程:
- Read HierarchyCircuitBreakerService (HierarchyCircuitBreakerService.java:81-92): `FIELDDATA_CIRCUIT_BREAKER_LIMIT_SETTING` (HierarchyCircuitBreakerService.java:81-86) 默认 **"40%"** + overhead **1.03** (HierarchyCircuitBreakerService.java:88-92)
- CircuitBreaker 接口 (CircuitBreaker.java:84): `addEstimateBytesAndMaybeBreak(bytes, label) throws CircuitBreakingException` — 超限即抛
- 消费点: AbstractIndexOrdinalsFieldData.loadGlobalDirect (AbstractIndexOrdinalsFieldData.java:148) `breakerService.getBreaker(FIELDDATA)`
- 层次: PARENT (CircuitBreaker.java:26) / FIELDDATA (CircuitBreaker.java:31) / REQUEST (CircuitBreaker.java:39) / IN_FLIGHT_REQUESTS (CircuitBreaker.java:44)

代码类型: Implementation (内存保护)

结论: **fielddata 独立断路器默认 40% heap (可配) + 1.03 overhead; load 时 addEstimateBytesAndMaybeBreak 超限抛 CircuitBreakingException 拒绝查询 — 聚合/排序的内存峰值被硬限制, 防 OOM**。HierarchyCircuitBreakerService.java:81-92 + CircuitBreaker.java:84

## Q4: fielddata 历史 — 5.0 迁移到 docValues

假设: 5.0 前聚合/排序走 fielddata (倒排字段内存加载, 易 OOM), 5.0 后迁移 docValues (磁盘列式)。

验证过程:
- 现代 text 字段: `fielddata` 参数默认 **false** (TextFieldMapper.java:249) — 需显式开启
- keyword/numeric 默认 docValues=true (KeywordFieldMapper.java:143, E-7 已证)
- 历史: 5.0 前 text 字段聚合需要 fielddata=true (加载倒排词条到堆) → 大量文档 OOM (经典事故)
- 迁移证据: doc_values 默认值变化 commit (7290b2dc916 "Clarify change to the default value of doc_values")
- 现代: fielddata 仅 text 字段 + 显式开启 + 断路器保护

代码类型: 演进分析

结论: **fielddata 是 5.0 前的"堆内倒排加载"方案 (聚合/排序用), 因 OOM 事故被 docValues (磁盘列式) 取代; 现代仅 text 字段可显式开启 fielddata (默认 false) 且受断路器保护 — "5.0 前 fielddata OOM" 是经典面试事故题**。TextFieldMapper.java:249 + KeywordFieldMapper.java:143

跨域关联: E-7 Mapping (docValues 字段面) / E-12 Aggregations (消费方)
