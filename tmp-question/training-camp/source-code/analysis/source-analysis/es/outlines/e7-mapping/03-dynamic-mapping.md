# E-7 Mapping 篇 3/3 — 动态映射: 没定义过的字段从哪来

> 前置: [[E-7-mapping-01]] [[E-7-mapping-02]] | 复用: — | 对照: [[r21-db]] (Redis 无 schema) [[r22-expire]] (schema 演化) | 引出: [[E-10-clusterstate]] (映射发布) [[E-1-engine]]
> 🔴 A | 来源: ObjectMapper.java:45-58 + DocumentParser.java:673-693 + DynamicFieldsBuilder.java:47-152 + DocumentParser.java:249 (createDynamicUpdate)
> 定位: Mapping 卷收尾 — 回答"写入没定义过的字段, ES 怎么决定类型"

**读者处境**: 你没写 mapping 就 index 了一条数据 — 居然成功了? ES 自己给字段猜了类型。猜错了怎么办 (把 25 当成了 text)? 面试官问 "dynamic 有几种模式? 动态模板怎么匹配?" — 这篇是动态映射的完整答案。

### 1. 问题引入 — 无 mapping 也能写?

场景: `PUT my-index/_doc/1 {"name": "张三", "age": 25, "score": 3.14, "active": true, "birth": "2020-01-01"}`
- 全部自动建 mapping: name→text+keyword, age→long, score→double, active→boolean, birth→date
- 本篇问题: 推断算法 (Q2) + 四态模式 (Q7) + 动态模板

### 2. dynamic 四态 — TRUE/FALSE/STRICT/RUNTIME

场景: mapping 里 "dynamic": "xxx" 有什么选项?
- ObjectMapper.Dynamic 枚举 (ObjectMapper.java:45-58): **四态** (09 审计的"三态"遗漏 RUNTIME)
  - TRUE: 自动推断建具体字段
  - FALSE: 忽略新字段
  - STRICT: 抛 StrictDynamicMappingException
  - RUNTIME: 建 runtime field (8.x 新增, 不落盘具体类型)
- **RUNTIME 设计意图**: 动态字段以 runtime field 形态存在 — 不建具体索引面 (倒排/docValues/points), 查询时按脚本计算 (RuntimeField.java:29 定义); 权衡 = 省 schema 固化成本, 换查询性能 (无索引, 每次计算)
- 分派: parseDynamicValue (DocumentParser.java:673-682): ensureNotStrict → FALSE 分支 failIfMatchesRoutingPath 后忽略 → 否则 createDynamicFieldFromValue
- strict 抛错 (DocumentParser.java:688-693): StrictDynamicMappingException
- 测试实证: DynamicMappingTests L48/L97/L113/L80 (四态各有测试)

### 3. 类型推断算法 — Long → Double → Date → String

场景: 一个字符串值怎么猜类型?
- VALUE_STRING 分支 (DynamicFieldsBuilder.java:47-105):
  ① Long.parseLong 尝试 (DynamicFieldsBuilder.java:51-59)
  ② Double.parseDouble 尝试 (DynamicFieldsBuilder.java:62-67)
  ③ **纯数字拒绝 date 检测** (DynamicFieldsBuilder.java:75-78): "We refuse to match pure numbers, which are too likely to be false positives with date formats that include eg. `epoch_millis` or `YYYY`"
  ④ date 格式逐个尝试 (DynamicFieldsBuilder.java:79-91) → 命中建 date 字段
  ⑤ 兜底 String
- VALUE_NUMBER 分支 (DynamicFieldsBuilder.java:107-140): INT/LONG/BIG_INTEGER → long; FLOAT/DOUBLE/BIG_DECIMAL → double
- VALUE_BOOLEAN → boolean (DynamicFieldsBuilder.java:141-145); EMBEDDED → binary (DynamicFieldsBuilder.java:146-152)
- 三开关: numericDetection / dateDetection / dynamicDateTimeFormatters (索引级, L57/74/80)

### 4. 动态模板 — match_mapping_type / match / path_match

场景: 想让所有 string 字段自动加 keyword 子字段? 想按命名规则定制?
- DynamicTemplate (DynamicTemplate.java:29): match / match_mapping_type / path_match / mapping — 模板注册表
- XContentFieldType 枚举 (DynamicTemplate.java:174): STRING/LONG/DOUBLE/BOOLEAN/DATE/BINARY — 与推断结果对接
- 应用点: DynamicFieldsBuilder.createDynamicFieldFromValue → createDynamicField → 查模板 (DynamicFieldsBuilder.java:47 流程)

### 5. 动态更新回写 — createDynamicUpdate

场景: 动态建出的字段怎么变成正式 mapping?
- DocumentParser.createDynamicUpdate (DocumentParser.java:249-267): context 收集的 dynamicMappers → RootObjectMapper.Builder.addDynamic → mappingUpdate
- 返回: ParsedDocument.dynamicMappingsUpdate() → E-1 Engine 写映射变更 → E-10 ClusterState 发布
- 关键设计: **动态映射 = 文档解析的副产品** — 先解析, 发现新字段, 再补 mapping (两次解析: 有 update 时触发第二次, L107-113)

### 6. 收束 — 无模式对照的收束

- Redis: 无 schema — 写入永远成功, 类型问题留到查询时 (对照 [[r21-db]])
- ES: 动态映射 = "写入时推断 + 事后固化" — 灵活性折中: 第一次写入定类型, 之后严格
- 终极结论: 动态映射是"无模式体验 + 强模式能力"的桥梁 — 但推断错误 (数字当 text) 只能重建索引, 这就是 schema 的代价
- 引出: E-10 ClusterState (映射发布) — E-1 Engine (动态映射触发二次解析)

### 核心悬念
"没建 mapping 也能写数据, ES 怎么做到的?" — 动态映射: 解析时按值推断类型, 把新字段追加进 mapping 并随集群状态发布。

### 概念依赖链
Q7 四态 → Q2 推断算法 → 动态模板 → createDynamicUpdate

### 源码锚点清单
- ObjectMapper.java:45-58 (Dynamic 四态枚举)
- DocumentParser.java:673-682 (parseDynamicValue) / 688-693 (strict 抛错) / 249-267 (createDynamicUpdate) / 107-113 (二次解析)
- DynamicFieldsBuilder.java:47-152 (类型推断) / 51-59 (Long) / 62-67 (Double) / 75-78 (纯数字拒绝 date) / 79-91 (date 尝试) / 107-140 (VALUE_NUMBER)
- DynamicTemplate.java:29 (类) / 174 (XContentFieldType)
- DynamicMappingTests.java:48/97/113/80 (四态测试)
