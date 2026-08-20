# E-7 闭环笔记 Q1-Q4: Parameter 体系/动态推断/字段差异/兜底链

## Q1: FieldMapper.Parameter 声明式参数体系

假设: 参数化重构 (4.x) 用声明式替代手写 parse/serialize/merge, 解决"每个 mapper 重复三套代码"。

验证过程:
- grep Parameter 使用 → KeywordFieldMapper.java:142-177: `Parameter<Boolean> indexed = Parameter.indexParam(...)` / `ignoreAbove = Parameter.intParam("ignore_above", true, ..., Defaults.IGNORE_ABOVE)` — 每个参数声明 5 要素: name/updateable/defaultValue/parser/initializer
- Read Parameter 类 (FieldMapper.java:595-660): `mergeValidator` 默认规则 = `updateable ? true : Objects.equals(previous, toMerge)` (L641-643) — **可更新参数默认允许, 不可更新参数默认必须相等**
- Read merge 冲突链 (FieldMapper.java:396-401): `Conflicts conflicts = new Conflicts(name()); builder.merge(...); conflicts.check();` — 冲突收集后统一抛
- Read Conflicts.check (FieldMapper.java:1174-1186): "Mapper for [name] conflicts with existing mapper:\n\t..." 聚合错误
- 时空溯源: 这是 4.x "parametrized mapper" 重构的核心产物

代码类型: Interface (声明式契约)

结论: **Parameter 五元组把"解析+校验+序列化+合并"四件重复事收敛为声明 — 每个 mapper 只声明参数, 框架统一执行; 合并冲突策略内建 (updateable 参数可改, 不可更新参数相等校验)**。FieldMapper.java:595,641 + KeywordFieldMapper.java:142

## Q2: 动态类型推断顺序 — 为什么先 Long 再 Double, 纯数字拒绝 date

假设: 推断顺序 = 精确性递减 (整数优先), 且纯数字字符串不试日期防误判。

验证过程:
- Read DynamicFieldsBuilder.createDynamicFieldFromValue (DynamicFieldsBuilder.java:47-152): VALUE_STRING 分支先 `Long.parseLong` 尝试 (DynamicFieldsBuilder.java:51-59) → 再 `Double.parseDouble` (DynamicFieldsBuilder.java:62-67) → 都失败才试 date (DynamicFieldsBuilder.java:74-91)
- 关键注释 (DynamicFieldsBuilder.java:75-78): "We refuse to match pure numbers, which are too likely to be false positives with date formats that include eg. `epoch_millis` or `YYYY`"
- VALUE_NUMBER 分支 (DynamicFieldsBuilder.java:107-140): INT/LONG/BIG_INTEGER → LONG; FLOAT/DOUBLE/BIG_DECIMAL → DOUBLE
- 三开关: numericDetection / dateDetection / dynamicDateTimeFormatters (DynamicFieldsBuilder.java:57,74,80)

代码类型: Algorithmic (推断优先级算法)

结论: **推断顺序 = Long → Double → Date → String; 纯数字字符串被 dateDetection 显式排除 (epoch_millis/YYYY 格式会把 "2024" 误判为日期); numeric/date detection 是索引级开关 (默认 true)**。DynamicFieldsBuilder.java:51-91

## Q3: text vs keyword 索引差异 — 底层 Lucene 面

假设: text 分词+位置, keyword 原样+忽略, 差异体现在 FieldType 的 indexOptions/omitNorms/tokenized。

验证过程:
- TextFieldMapper 默认: `indexOptions = TextParams.textIndexOptions(...)` (TextFieldMapper.java:245), `norms = TextParams.norms(true, ...)` (TextFieldMapper.java:246) — **norms 默认 true** (相关度计算)
- KeywordFieldMapper 默认: `norms = TextParams.norms(false, ...)` (KeywordFieldMapper.java:163) — **norms 默认 false** (无相关度需求); `ignoreAbove = Defaults.IGNORE_ABOVE` (KeywordFieldMapper.java:161)
- Keyword indexValue (KeywordFieldMapper.java:898-901): `value.length() > ignoreAbove` → addIgnoredField — 超长丢弃
- TextFieldMapper.parseCreateField (TextFieldMapper.java:1243-1265): `new Field(name, value, fieldType)` 直接索引 + prefix/phrase 子字段 (TextFieldMapper.java:1255-1262)

代码类型: Implementation (字段面差异)

结论: **text = 分词 + 位置 + norms (BM25 打分需要), keyword = 原样 + ignoreAbove 截断 + 无 norms; 同一值两种底层索引面 (倒排 vs 精确匹配+docValues)**。TextFieldMapper.java:245-246,1243 vs KeywordFieldMapper.java:161-163,905

跨域关联: E-2 Search (BM25 依赖 norms) / E-11 FieldData (keyword 的 docValues)

## Q4: ignore_malformed/coerce 默认值 — 静态遮蔽坑

假设: 全局默认 ignore_malformed=false, coerce 有同 key 双默认 (FieldMapper=false, NumberFieldMapper=true)。

验证过程:
- grep '"index.mapping.coerce"' → 3 处注册: FieldMapper.java:67 (**false**), NumberFieldMapper.java:84 (**true**), RangeFieldMapper.java:59 (**true**)
- 关键: NumberFieldMapper.Builder 构造 (NumberFieldMapper.java:136) `COERCE_SETTING.get(settings)` — Java 静态遮蔽, 解析到**本类**的 COERCE_SETTING (默认 true) 而非 FieldMapper 的
- IGNORE_MALFORMED 只有 FieldMapper.java:61 一处 (默认 false), NumberFieldMapper 继承使用
- 测试实证: NumberFieldMapperTests.java:68 更新检查 coerce=false 生效 — 默认 true 可被显式关
- 语义: coerce=true (数字字段) 允许 "5"→5 字符串转数字; ignore_malformed=false 时非法值直接抛 DocumentParsingException

代码类型: Implementation (默认值遮蔽陷阱)

结论: **数值字段 coerce 默认 true (字符串强转数字), ignore_malformed 默认 false (非法值抛错) — 同 key "index.mapping.coerce" 在 FieldMapper/NumberFieldMapper/RangeFieldMapper 三处定义不同默认值, 靠静态遮蔽生效, 是易踩的代码坏味道**。NumberFieldMapper.java:84,136 + FieldMapper.java:61-74

跨域关联: E-1 Engine (malformed 兜底影响索引路径)
