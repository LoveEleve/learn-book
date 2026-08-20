# E-7 Mapping 篇 2/3 — 文档解析: JSON 怎么变成 Lucene 文档

> 前置: [[E-7-mapping-01]] (类型体系) | 复用: — | 对照: [[r8-persistence]] (RDB 序列化) [[rd3-codec]] (序列化对照) | 引出: [[E-7-mapping-03]] [[E-1-engine]] (写入消费) [[E-11-fielddata]] (docValues 面)
> 🔴 A | 来源: DocumentMapper.java:91 + DocumentParser.java:77,131,263 + FieldMapper.java:178,206 + NumberFieldMapper.java:1830 + DateFieldMapper.java:899
> 定位: Mapping 卷中篇 — 回答"一次 index 请求, JSON 怎么逐字段变成 Lucene Field"

**读者处境**: mapping 定义好了 (篇 1)。现在一条 `{"name": "张三", "age": 25, "birth": "2020-01-01"}` 写进来 — 解析器怎么知道 name 走 text 分词、age 走 long 点索引、birth 转成毫秒时间戳? 如果 age 写成 "abc" 会怎样? 面试官问 "ES 解析报错为什么能看到字段值预览?" — 答案在这篇。

### 1. 问题引入 — 从 JSON 到 Lucene Field 的旅程

场景: index 请求 → DocumentMapper.parse (DocumentMapper.java:91) → DocumentParser.parseDocument (L77) → ParsedDocument
- 旅程: JSON 流 → 逐字段分派 → 类型化解析 → Lucene Field 落进 context.doc()
- 本篇问题: 分派逻辑 (谁解析哪个字段) + 每个类型怎么解析 + 失败怎么兜底

### 2. 主循环 — internalParseDocument 五步

场景: 一次解析有几个阶段?
- Read DocumentParser.parseDocument (DocumentParser.java:77-129): 空文档检查 (DocumentParser.java:79-81) → 建 context + parser → validateStart → internalParseDocument → validateEnd → createDynamicUpdate
- internalParseDocument (DocumentParser.java:131-157): metadata preParse → parseObjectOrNested (DocumentParser.java:263) → executeIndexTimeScripts → metadata postParse
- **解析性能**: 单遍遍历 (逐 token 前进, 不回溯) + Field 对象累积进 context.doc() — 每个字段一个 Lucene Field, 内存分配在 context 内; 复杂度 = O(字段数)
- 关键设计: **metadata 钩子 (preParse/postParse)** — _id/_source/_routing 在文档前后挂载, 与业务字段分离

### 3. 字段分派 — FieldMapper.parse 模板方法

场景: 一个字段值到了, 怎么决定用哪个 mapper?
- FieldMapper.parse (FieldMapper.java:178-193): hasScript 检查 → parseCreateField (子类实现) → 多字段递归 (doParseMultiFields L195-199, 如 text.keyword)
- parseCreateField 四变体:
  - Text (TextFieldMapper.java:1243-1265): textOrNull → `new Field(name, value, fieldType)` (L1248) + prefix/phrase 子字段 (L1255-1262) — **直接倒排**
  - Keyword (KeywordFieldMapper.java:874-915): ignoreAbove 超长丢弃 (L898-901) — **原样索引 + 截断**
  - Number (NumberFieldMapper.java:1830-1851): 解析失败 ignoreMalformed 兜底 → addIgnoredField (NumberFieldMapper.java:1833-1841) — **强转数值**
  - Date (DateFieldMapper.java:899-930): 字符串→epoch millis (L910) + 三异常兜底 (L911-918) → 三路索引 (LongField/SortedNumericDocValuesField/LongPoint, L924-937)
- 关键设计: 模板方法 + 每个类型一个 parseCreateField — 新增类型只需实现一个方法

### 4. 失败兜底 — ignore_malformed/coerce + 错误工程

场景: "age": "abc" 会怎样?
- 默认 ignore_malformed=false (FieldMapper.java:61-64): 抛 DocumentParsingException
- ignore_malformed=true: Number 分支 addIgnoredField 丢弃 (NumberFieldMapper.java:1833-1841) + storeMalformedFields 保留原值 (NumberFieldMapper.java:1837-1839)
- **coerce 遮蔽坑** (Q4): "index.mapping.coerce" 三处定义 — FieldMapper=false (FieldMapper.java:67) / NumberFieldMapper=true (NumberFieldMapper.java:84) / RangeFieldMapper=true (RangeFieldMapper.java:59); NumberFieldMapper.Builder (NumberFieldMapper.java:136) 经静态遮蔽用本类默认 **true** — 数值字段默认允许 "5"→5
- **错误工程** (Q5): rethrowAsDocumentParsingException (FieldMapper.java:206-244) — 从 parser 当前位置重读值作 preview (FieldMapper.java:207), 错误消息 "failed to parse field [name] of type [text] in [_doc]. Preview of field's value: 'abc'" (FieldMapper.java:231-236)

### 5. 收束 — 解析结果的去向

- ParsedDocument: version/seqID/id/routing/docs (DocumentMapper.java:91 返回) → E-1 Engine 消费 (写 translog + Lucene)
- docValues 由 hasDocValues 隐式决定 (DateFieldMapper L924-937) → E-11 FieldData 读取面
- 对照: 与 Redis RDB 序列化 (r8-persistence) — 同一数据两种物化 (类型化字段 vs 字节流)
- 引出: 篇 3 (动态映射: 字段没定义时怎么办)

### 核心悬念
"ES 解析报错, 为什么错误信息里带着字段值的预览?" — 因为解析器在失败瞬间从当前位置重读了值, 错误工程让排障零成本。

### 概念依赖链
Q3 parseCreateField 四变体 → Q4 兜底链 → Q5 错误工程 → (篇 1 的类型体系)

### 源码锚点清单
- DocumentMapper.java:91 (parse 入口)
- DocumentParser.java:77-129 (parseDocument 流程) / 131-157 (五步循环) / 275 (parseObjectOrNested)
- FieldMapper.java:178-193 (parse 模板) / 195-199 (多字段) / 206-244 (rethrowAsDocumentParsingException) / 61-74 (默认值)
- TextFieldMapper.java:1243-1265 (parseCreateField)
- KeywordFieldMapper.java:874-915 (parseCreateField + ignoreAbove)
- NumberFieldMapper.java:1830-1851 (parseCreateField) / 84 (COERCE_SETTING true) / 136 (Builder 遮蔽)
- DateFieldMapper.java:899-930 (parseCreateField) / 924-937 (三路索引)
