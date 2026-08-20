# E-7 Mapping — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 叶子域 (被 E-1/E-2/E-11 消费) | 对照: [[rd3-codec]] (客户端 codec vs 服务端 mapping) [[r21-db]] (键空间 vs 强模式)
> 源码: server/src/main/java/org/elasticsearch/index/mapper/ (123 文件 + flattened/ vectors/ 子目录, 34193 行)
> 测试地图: server/src/test/.../index/mapper/ 132 文件

## 继承树/调用图

```
Mapper (抽象, L23)
├── FieldMapper (1476 行, L57) — 值字段基类
│   ├── TextFieldMapper (1454, parseCreateField L1243)
│   ├── KeywordFieldMapper (1070, parseCreateField L874)
│   ├── NumberFieldMapper (1947, parseCreateField L1830)
│   ├── DateFieldMapper (987, parseCreateField L899)
│   ├── BooleanFieldMapper (410, parseCreateField L410)
│   ├── BinaryFieldMapper / RangeFieldMapper / CompletionFieldMapper (705) ...
├── ObjectMapper (717) — 对象容器
│   └── RootObjectMapper — 根对象 (dynamic 模式持有者)
│   └── NestedObjectMapper — nested 类型
└── MetadataFieldMapper — 元字段 (id/_source/_routing/_seq_no...)

调用链 (写入路径):
    IndexShard.applyIndexOperationOnPrimary → Engine.index → DocumentMapper.parse (DocumentMapper.java:91)
    → DocumentParser.parseDocument (DocumentParser.java:77): metadata preParse → parseObjectOrNested (DocumentParser.java:263)
      → FieldMapper.parse (FieldMapper.java:178) → parseCreateField (子类实现) → context.doc().add(Field)
    → createDynamicUpdate (DocumentParser.java:249): 动态字段 → Mapping dynamicUpdate

字段类型注册: MapperRegistry (TypeParser 注册表) → MapperService (MapperService.java:52)
```

## 基本元素分解 (原则二)

1. **Mapper 层级** — Mapper (字段基类, name/typeName) → FieldMapper (值字段, parseCreateField 抽象 L250) / ObjectMapper (容器) / MetadataFieldMapper (元字段) — 三级类型体系
2. **FieldMapper.Parameter 声明式参数** — FieldMapper.java:595-660: name/defaultValue/parser/mergeValidator/conflictSerializer 五元组 — 每个参数声明"可更新?默认值?冲突策略?"
3. **DocumentParser 主循环** — parseDocument (DocumentParser.java:77) → internalParseDocument (L131): metadata preParse → parseObjectOrNested → index-time scripts → postParse; 动态字段收集进 context
4. **DynamicFieldsBuilder 类型推断** — createDynamicFieldFromValue (DynamicFieldsBuilder.java:47): VALUE_STRING→Long 尝试→Double 尝试→date 尝试→String; VALUE_NUMBER→INT/LONG→LONG, FLOAT/DOUBLE→DOUBLE; BOOLEAN→boolean (DynamicFieldsBuilder.java:40-152)
5. **parseCreateField 四变体** — Text (TextFieldMapper.java:1243: 直接 new Field + 可选 prefix/phrase 子字段) / Keyword (KeywordFieldMapper.java:874: ignoreAbove 截断 L898-901) / Number (NumberFieldMapper.java:1830: 解析失败 ignoreMalformed 兜底 L1833-1841) / Date (DateFieldMapper.java:899: 字符串→epoch millis 三异常兜底 + 三路索引 L924-937)
6. **MetadataFieldMapper preParse/postParse 钩子** — _id/_source/_routing 在文档解析前后挂载 (DocumentParser.java:135-155)

## 标记问题 (≥5)

1. **Q1: FieldMapper.Parameter 体系解决了什么?** — 为什么用声明式参数替代手写解析? (updateable/mergeValidator/conflictSerializer 语义) — 4.x 大重构?
2. **Q2: 动态类型推断的顺序** — 为什么先试 Long 再 Double? 为什么"纯数字拒绝 date 检测"? (DynamicFieldsBuilder.java:57-91 注释 "We refuse to match pure numbers...false positives with date formats that include eg. epoch_millis")
3. **Q3: text vs keyword 的索引差异** — Text: 分词 (analyzer) + position; Keyword: 原样 + ignoreAbove — 底层 Lucene 面差异 (FieldType indexOptions/omitNorms)?
4. **Q4: ignore_malformed/coerce 兜底链** — Number 解析失败 → ignoreMalformed → addIgnoredField; Date 三异常 → ignoreMalformed — 配置默认 false (FieldMapper.java:68-74), 为什么默认不宽容?
5. **Q5: 文档解析的失败语义** — rethrowAsDocumentParsingException (FieldMapper.java:206-244) 带 value preview — 错误信息工程 (面试必问: ES 解析报错的 preview 哪来的)
6. **Q6: 映射合并 (merge) 冲突策略** — MapperService.merge (MapperService.java:364-376) + Parameter.mergeValidator — 字段类型冲突 (text→keyword) 怎么报错?
7. **Q7: dynamic 三态 (true/false/strict)** — DynamicFieldsBuilder 策略模式: CONCRETE/RUNTIME? strict 抛 StrictDynamicMappingException?
8. **Q8: 与 Redis 无模式对照** — Redis 键空间裸字节 vs ES 强模式 (字段类型决定索引面) — 为什么 ES 必须 schema?

## 已读测试 (3 个)

- `DynamicMappingTests.testDynamicTrue` (DynamicMappingTests.java:48-78): dynamic=true 时新字段自动 text+keyword 子字段, dynamicMappingsUpdate 返回更新
- `DynamicMappingTests.testDynamicFalse` (DynamicMappingTests.java:97): dynamic=false 忽略新字段
- `DynamicMappingTests.testDynamicStrict` (DynamicMappingTests.java:113): dynamic=strict 抛错
- `DocumentMapperTests.testMergeObjectDynamic` (DocumentMapperTests.java:77): 对象合并

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: E-7 Pass 1 — DateFieldMapper.indexValue (DateFieldMapper.java:924-937) 三路索引 (LongField/SortedNumericDocValues/LongPoint + StoredField)
- 发现: docValues 由字段类型隐式决定 (hasDocValues), 与 E-11 FieldData 加载面衔接; 数值索引用 Lucene Point (BKD) — E-2 Search 的 range 查询依赖
- 已对照验证: DateFieldMapper.java:924-937
