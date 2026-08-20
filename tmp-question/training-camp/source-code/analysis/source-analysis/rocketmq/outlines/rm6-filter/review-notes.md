# RM-6 消息过滤 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **补充锚点** | **isMsgInLive 精确语义** = `msgStoreTime > getBornTime()` (订阅注册时间, 非死期) (ConsumerFilterData:58) — 订阅注册前的消息位图无值 → 粗筛放行回退精筛 | 大纲 §5 修正 |
| 2 | **语义标注** | **evaluate 异常 → 位图不置位 → 消息被滤** (CalcBitMap catch Throwable 跳过): 属性类型冲突 (如 a>10 对 a="abc") = 不匹配 — 激进语义 | 大纲 §5 补注 |
| 3 | 验证 | bloomFilter **全局共享实例** (f=20/n=64) + bloomFilterData 每订阅者 (generate(group#topic)) — 参数固定, 数据独立 | 大纲 §5 补注 |
| 4 | 验证 | ExpressionForRetryMessageFilter: RETRY_GROUP_TOPIC_PREFIX 判定 → realFilterData 精筛 | 记录 |
| 5 | 行号验证 | 全函数 22 锚点 + 跨文件 8 处 grep (ExpressionType 37-53 / BloomFilter 47-108 / SelectorParser.jj / ExpressionMessageFilter 60-130 / CalcBitMap 35-100 / ConsumerFilterManager 53-155 / ConsumerFilterData 32-58 / BrokerConfig 160) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 双类型契约 / TAG 哈希链
- 两级过滤 (粗筛跳过/精筛兜底)
- 布隆不漏判 (假阳性可纠)
- isMsgInLive 窗口

### 维度2 性能
- 位图 O(1) 粗筛
- 双哈希降成本
- 编译一次运行多次

### 维度3 内存
- 位图 m 位/订阅者
- 过滤数据表 (topic 分组)

### 维度4 一致性
- 写时位图与读时判定对称
- 订阅变更重新编译
- evaluate 异常语义

### 维度5 负面空间 (已写入大纲 5 条)
- 不 FilterServer/不正则/不属性索引/不联合过滤/不运行时更新

## 结论
RM-6 全部锚点 ~35 处验证, 6 闭环完成, **补锚 2 + 语义标注 1**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-14, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 6 | 通过项 | 前置 RM-3/RM-5 (已交付) ✅; 引出 RM-8 ✅; 对照 Kafka ✅; 读者处境场景化 ✅; 锚点 ~35 ✅; 负面空间 5 条 ✅; 横切 (解析/概率/存储/元数据) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | **反写测试发现** | 大纲 §3 "两级: CQ 位图粗筛 → CommitLog 表达式精筛" — **粗筛"未命中"的语义未明**: 布隆不假阴性 → 未命中 = 确定不匹配 (跳过), 命中 = 可能匹配 (精筛) — 需补"无假阴性"性质 | 大纲 §3 补注 |
| 8 | 通过项 | 其余 ~30 句机制描述逐句对源码一致 ✅ (SQL92 语法/TAG codeSet/JavaCC/AST 家族/ActiveMQ 移植/编译缓存/布隆数学/双哈希/写时位图/默认关/注册表/生命周期/重试过滤/测试 7 文件) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (布隆无假阴性性质 #7), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-14, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 布隆参数数学 | f=20% → k=ceil(log(0.5,0.2))=ceil(2.32)=3; n=64 → m=64×log2(5)×log2(e)=64×2.32×1.44=214→216 (8 对齐) — 3 哈希 216 位/订阅者 ✅ | 通过 |
| V2 | 无假阴性 | 同一哈希函数集: 已置位集合 ⊇ 真匹配集合 → 未命中必不匹配 ✅ | 通过 |
| V3 | isMsgInLive 方向 | msgStoreTime > bornTime — 订阅前消息无位图 → 放行精筛 ✅ | 通过 |
| V4 | 误判成本 | 20% 假阳性 → 粗筛通过率 20%+真阳性 — 精筛处理, 省 80% 属性解析 ✅ | 通过 |
| V5 | codeSet 位宽 | tagsCode intValue (32 位哈希) — codeSet 为 int 集合 (RM-3 tags.hashCode) ✅ | 通过 |
| V6 | 位图失败回退 | filterBitMap null/位宽不符 → return true (粗筛放行) — 精筛兜底 ✅ | 通过 |
| V7 | 重试精筛 | RETRY_GROUP_TOPIC_PREFIX → realFilterData (原 topic 过滤数据) — 重试保留原订阅语义 ✅ | 通过 |
| V8 | 默认关语义 | enableCalcFilterBitMap false → 无位图 → 全走精筛 (功能正确, 性能降) ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **覆盖缺口** | 大纲未提 **类过滤模式 (isClassFilterMode)**: 订阅数据标记类过滤 → 两级过滤都直接放行 (L63-65/L121-123) — 遗留面 | 大纲 §1 补注 |

## 三次 REVIEW 汇总
推理验证 8 项全过 (V1-V8); 新发现 **1 处** (类过滤模式遗留面), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-14, 用户要求深度 REVIEW — 逐锚点 re-grep + 反写测试 + 推理验证)

> 动机: 对全部锚点重新 grep, 追查六个存疑点 (精筛异常/重试 CQ 级/codeSet 限制/NowExpression/属性提取/SPI 并发), 并做反写测试。

## 追查过程 (六个存疑点全部实证)

| # | 存疑点 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 精筛 evaluate 异常? | ExpressionMessageFilter L150-165: catch Throwable → ret=null → **`ret==null || !(ret instanceof Boolean) → return false` (消息被滤)** — 与粗筛激进语义一致 | 发现 1 (验证+补锚) |
| T2 | 重试 CQ 级? | ExpressionForRetryMessageFilter **未覆写 isMatchedByConsumeQueue** — 沿用父类 (粗筛位图路径) | 通过 (验证) |
| T3 | codeSet 限制? | SubscriptionData.codeSet = HashSet<Integer> **无大小限制**; classFilterMode 默认 false | 发现 2 (补锚) |
| T4 | NowExpression? | **evaluate = System.currentTimeMillis()** — 时间比较表达式面 | 发现 3 (补锚) |
| T5 | 属性提取时机? | L142-146: tempProperties null 且 msgBuffer 非空 → **MessageDecoder.decodeProperties(msgBuffer)** — **精筛时才解析 (延迟解析)** | 发现 4 (机制闭环: 粗筛省解析的价值链) |
| T6 | FilterFactory 并发? | FILTER_SPI_HOLDER = HashMap (非并发); 静态初始化注册 SqlFilter; 运行期 register 需外部同步 — 标注 | 发现 5 (观察标注) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 激进语义闭环 | 粗筛 (evaluate 异常→不置位→被滤) + 精筛 (异常→false) — 一致 ✅ | 通过 |
| V2 | 延迟解析收益 | 粗筛拒绝的消息零属性解析 (216 位位图判定); 命中才 decodeProperties — 性能链完整 ✅ | 通过 |
| V3 | 重试粗筛 | 重试消息 CQ 位图沿用父类判定 — 与原 topic 订阅数据匹配 ✅ | 通过 |
| V4 | NOW 表达式 | NOW() - 1000 类时间窗表达式可用 ✅ | 通过 |
| V5 | codeSet 无界 | 标签数无上限 (内存随订阅增长) — 标注 ✅ | 通过 |
| V6 | 位图 216 位 | m=216 (27B) 存 Ext — 每消息 27B 位图开销 ✅ | 通过 |

## 新发现问题 (5 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | 补充锚点 | 精筛 evaluate 异常 → return false (被滤) — 激进语义在精筛同样成立 | 大纲 §2 补注 |
| 11 | 补充锚点 | **延迟属性解析**: 精筛时才 decodeProperties — 粗筛省解析的价值链 | 大纲 §3 补注 |
| 12 | 补充锚点 | NowExpression = 当前毫秒 (时间表达式) | 大纲 §2 补注 |
| 13 | 补充锚点 | codeSet HashSet 无大小限制; classFilterMode 默认 false | 大纲 §1 补注 |
| 14 | 观察标注 | FilterFactory HashMap 非并发 (运行期 register 外部同步) | 记录 (不修大纲) |

## 反写测试 (只读大纲能否写文章)

- §1 双类型: SQL92 语法/TAG codeSet (修复后)/默认 TAG/类过滤 — 可写 ✅
- §2 解析: JavaCC/AST/ActiveMQ 移植/精筛异常 (修复后)/NOW (修复后) — 可写 ✅
- §3 布隆: 数学/双哈希/无假阴性/延迟解析 (修复后) — 可写 ✅
- §4 写时位图: CalcBitMap/默认关/异常语义 — 可写 ✅
- §5 元数据: 共享实例/数据独立/isMsgInLive/回退 — 可写 ✅
- §6 重试+测试: 精筛覆写/测试 7 文件 — 可写 ✅
- 负面空间 5 条 — 完整 ✅

## 四次 REVIEW 汇总

六存疑点全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 5 处修复** — #11 最有价值 (延迟属性解析: 粗筛省解析的完整价值链, 布隆存在的根本理由)。大纲经修复后反写测试全过。
