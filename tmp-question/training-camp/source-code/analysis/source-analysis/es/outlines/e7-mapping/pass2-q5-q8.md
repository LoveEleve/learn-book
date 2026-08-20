# E-7 闭环笔记 Q5-Q8: 失败语义/合并/动态四态/无模式对照

## Q5: 解析失败的错误工程 — value preview 从哪来

假设: FieldMapper.parse 捕获异常后用 parser 当前位置重读值, 拼进 DocumentParsingException。

验证过程:
- Read FieldMapper.parse (FieldMapper.java:178-193): try { parseCreateField } catch → `rethrowAsDocumentParsingException(context, e)` (FieldMapper.java:183)
- Read rethrowAsDocumentParsingException (FieldMapper.java:206-244): `AbstractXContentParser.readValue(parser, HashMap::new)` (FieldMapper.java:207) — **从当前 token 重读整个值** → valuePreview; 失败 → "Could not parse field value preview" (FieldMapper.java:216-224)
- 错误格式 (FieldMapper.java:231-236): "failed to parse field [%s] of type [%s] in %s. Preview of field's value: '%s'" — 字段名/类型/文档描述/值预览 四要素
- 入口: DocumentMapper.parse (DocumentMapper.java:91) → documentParser.parseDocument

代码类型: Interface (错误契约)

结论: **解析失败时从 parser 当前位置重读值作为 preview, 拼入错误消息 — "哪个字段/什么类型/什么值" 三要素让排障零成本; 若 preview 本身读失败 (值结构复杂), 降级为 "Could not parse field value preview"**。FieldMapper.java:199-244 + DocumentMapper.java:91

## Q6: 映射合并 merge — 冲突收集器

假设: 合并 = 逐参数 mergeValidator 检查 + 冲突聚合 + 统一抛错。

验证过程:
- Read MapperService.merge (MapperService.java:376-400): 多源映射 XContentHelper.merge → 若与现有相同直接返回 (MapperService.java:378-381)
- Read FieldMapper.merge (FieldMapper.java:396-401): `Conflicts conflicts = new Conflicts(name()); builder.merge(...); conflicts.check();` — 冲突先收集后抛
- Read Conflicts.check (FieldMapper.java:1174-1186): 聚合消息 "Mapper for [name] conflicts with existing mapper:\n\t..." + IllegalArgumentException
- 类型冲突: checkIncomingMergeType (FieldMapper.java:402-412) — "cannot be changed from type [text] to [keyword]" (类不同即抛)
- 参数冲突: Parameter.mergeValidator 默认 = updateable ? true : Objects.equals (FieldMapper.java:641-643)

代码类型: Implementation (合并状态机)

结论: **字段类型改变 (text→keyword) 直接抛; 同类型参数合并按 updateable 判定 (可更新直接覆盖, 不可更新须相等); 冲突聚合为单条多行错误消息 — "批量冲突一次报全" 是合并的可用性设计**。MapperService.java:376 + FieldMapper.java:396-401,641,1174

跨域关联: E-10 ClusterState (映射变更随集群状态发布)

## Q7: dynamic 四态 (非规划的三态) — TRUE/FALSE/STRICT/RUNTIME

假设: 09 审计称"dynamic 三态" — 实测 4.x 加 RUNTIME 变四态。

验证过程:
- Read ObjectMapper.Dynamic 枚举 (ObjectMapper.java:45-58): TRUE / FALSE / STRICT / **RUNTIME** (4 值, 规划遗漏 RUNTIME)
- Read DocumentParser.parseDynamicValue (DocumentParser.java:673-682): ensureNotStrict → FALSE 分支 `failIfMatchesRoutingPath` 后 return (忽略) → 否则 createDynamicFieldFromValue
- strict 抛错 (DocumentParser.java:688-693): StrictDynamicMappingException
- 策略: TRUE/RUNTIME 各有 DynamicFieldsBuilder (DynamicFieldsBuilder.java:47-50, DYNAMIC_TRUE/DYNAMIC_RUNTIME), FALSE 无 builder 直接忽略
- 测试实证: DynamicMappingTests.testDynamicRuntime (DynamicMappingTests.java:80) / testDynamicStrict (DynamicMappingTests.java:113)

代码类型: Implementation (策略枚举)

结论: **dynamic 是四态枚举 (TRUE/FALSE/STRICT/RUNTIME) — 09 审计的"三态"遗漏 RUNTIME (8.x 新增, 动态字段建 runtime field 而非具体类型); FALSE 忽略 + 路由路径保护, STRICT 抛 StrictDynamicMappingException**。ObjectMapper.java:45-58 + DocumentParser.java:673-693

## Q8: 与 Redis 无模式对照 — 为什么 ES 必须 schema

假设: ES 强模式根因 = 索引面 (倒排/docValues/points/stored) 必须在写入前确定; Redis 无模式因为只存裸字节。

验证过程:
- MapperRegistry (MapperRegistry.java:24-31): TypeParser 注册表 (类型→解析器) — 类型系统驱动
- TextFieldMapper vs KeywordFieldMapper vs DateFieldMapper: 同一字段值, 三种 Lucene 索引面完全不同 (Field/Point/docValues) — DateFieldMapper.indexValue 三路 (LongField/SortedNumericDocValuesField/LongPoint, L924-937)
- 对照 Redis r21-db: 键空间 value 裸字节, 无字段概念 — 写入即存, 查询全量扫描
- ES 代价: 新字段必须动态推断 (Q2) 或拒绝 (strict) — schema 演化成本内建

代码类型: 对照分析

结论: **ES 强模式是"倒排索引物理结构"的必然: 每个字段的索引面 (倒排/点/列式/stored) 写入前必须确定, 无法事后推导; Redis 无模式是"裸字节存取"的自然结果 — 两者差异 = 查询能力 vs 写入灵活性 的根本取舍**。MapperRegistry.java:24 + DateFieldMapper.java:933

跨域关联: [[r21-db]] (Redis 键空间) / [[rd3-codec]] (客户端 codec 对照)
