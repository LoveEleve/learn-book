# RM-6 消息过滤 — SQL92/TAG 双类型 + 布隆两级过滤

> 前置: [[RM-3-commitlog]] (tagsCode/CQ Ext) + [[RM-5-Broker]] (装配) | 引出: [[RM-8-消费]] | 对照: Kafka 过滤 (broker 无内建, 客户端过滤)
> 🟡 B | 6 KP | [模式: 表达式引擎 + 概率位图 + 两级过滤]
> Pass 2 闭环: q1(类型) q2(解析求值) q3(布隆) q4(写时位图) q5(元数据) q6(重试+测试)

**读者处境**: 消费时怎么只拿想要的? TAG 和 SQL92 差多少? 布隆位图为什么省内存? 这篇拆消息过滤: 双表达式类型、JavaCC 解析、布隆两级过滤、写时位图。

### 1. 表达式类型 — SQL92 vs TAG

场景: 订阅怎么表达?
源码路径:
- **SQL92** (ExpressionType.java:37): 完整语法 (AND/OR/NOT/BETWEEN/IN/IS NULL/=TRUE + 类型) — **ActiveMQ 风格文档**
- **TAG** (L45-47): "tag1 || tag2"; null/* = 全订阅
- **isTagType** (L49-53): 默认 TAG (null/"" 兼容); **类过滤模式 (isClassFilterMode) 两级放行 (遗留面)**
- **TAG 匹配**: 订阅 → tag 哈希 **codeSet (HashSet 无大小限制)** → CQ tagsCode 比对 (RM-3); classFilterMode 默认 false
关键设计 (q1): **双类型契约**: 简单标签 vs 属性表达式; 默认 TAG 保兼容。[模式: 类型契约]

### 2. 解析与求值 — JavaCC → AST → evaluate

场景: SQL92 表达式怎么变成可执行?
源码路径:
- **JavaCC** (SelectorParser.jj) → SelectorParser 生成 (1401 行)
- **AST 家族** (expression/, **ActiveMQ 移植** "taken from ActiveMQ"): LogicExpression (AND/OR/NOT) / ComparisonExpression / UnaryInExpression (IN) / PropertyExpression / **NowExpression** (时间)
- **evaluate** (PropertyExpression:35): `context.get(name)` — 属性直查; **NowExpression = 当前毫秒** (时间表达式 NOW())
- **编译缓存**: ConsumerFilterData.compiledExpression (注册期编译); **精筛异常 → return false (消息被滤)** — 与粗筛激进语义一致 (L150-165)
关键设计 (q2): **表达式树 = 编译一次运行多次**; 属性上下文直查零拷贝。[模式: 表达式引擎]

### 3. 布隆过滤 — 参数数学 + 两级

场景: 百万消息怎么不逐个解析属性?
源码路径:
- **参数** (BloomFilter:70-86): f (误判率) + n (元素) → **k = ceil(log(0.5,f))** + **m = n×log2(1/f)×log2(e)** (8 对齐) — 数学注释
- **双哈希** (L92-108): murmur3 → hash1+hash2 → k 位置 (Kirsch-Mitzenmacher)
- **两级**: CQ 位图粗筛 (O(1), **布隆无假阴性: 未命中=确定不匹配跳过, 命中=可能匹配**) → CommitLog 表达式精筛 (兜底纠正); **延迟属性解析**: 精筛时才 MessageDecoder.decodeProperties(msgBuffer) (粗筛通过才解析 — 性能闭环)
关键设计 (q3): **概率粗筛 + 精确精筛** — 误判被下游纠正; 位图省去全量属性解析。[模式: 概率过滤]

### 4. 写时位图 — CalcBitMap 分发

场景: 位图什么时候算?
源码路径:
- **触发** (CommitLogDispatcherCalcBitMap:55-57): **enableCalcFilterBitMap** + topic 有 SQL92 订阅者
- **计算** (L65-100): 逐订阅者 evaluate → 真 → hashTo 置位 → request.setBitMap
- **存储**: RM-3 CQ Ext (filterBitMap)
- **默认关** (BrokerConfig:160)
关键设计 (q4): **写一次位图, 读 O(1) 粗筛** — 计算成本写路径摊薄。[模式: 写时位图]

### 5. 过滤元数据 — 注册表与生命周期

场景: 订阅者过滤信息怎么管理?
源码路径:
- **BloomFilter 实例** (ConsumerFilterManager:57): **createByFn(20, 64) 全局共享** (f=20%, n=64 — 参数固定), bloomFilterData 每订阅者 (generate(group#topic))
- **注册**: (topic,group) → filterDataByTopic; compile + **bloomFilter.generate(group#topic)**
- **ConsumerFilterData**: compiledExpression (transient) / bornTime / **deadTime** / bloomFilterData / clientVersion
- **isMsgInLive** (L58): **msgStoreTime > bornTime (订阅注册时间)** — 订阅注册前的消息位图无值 → 粗筛放行回退精筛; **evaluate 异常语义标注: CalcBitMap 异常 → 位图不置位 → 消息被滤 (激进)**, 属性类型冲突即不匹配
关键设计 (q5): **订阅期 = 位图有效窗口**; 注册即编译, 过期即失效。[模式: 元数据表]

### 6. 重试过滤与测试

- **重试** (ExpressionForRetryMessageFilter:40): 重试消息用真实 topic 过滤数据精筛 (覆写 isMatchedByCommitLog)
- **测试** (7 文件 2129 行): ParserTest (语法边界: 溢出/非法 BETWEEN) / ExpressionTest (运算符) / BloomFilterTest (**checkFalseHit**) / FilterSpiTest / BitsArrayTest + broker 端到端
- **配置**: enableCalcFilterBitMap 默认 false (SQL92 场景才开)

### 负面空间 — 过滤刻意不做的事

- **不做 FilterServer 独立进程**: 5.x 废弃, broker 内嵌 (执行计划 RM-4 "FilterServer" 已过时)
- **不做正则表达式**: 仅 TAG 与 SQL92 子集 (无 REGEX 类型)
- **不做属性索引**: 位图是唯一加速, 无倒排
- **不做多 topic 联合过滤**: 订阅按单 topic
- **不做运行时表达式更新**: 订阅变更需重新注册 (compiledExpression 重建)

→ 引出: 消费端怎么组织订阅与拉取? → [[RM-8-消费]]
