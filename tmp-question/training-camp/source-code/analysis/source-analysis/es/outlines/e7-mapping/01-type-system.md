# E-7 Mapping 篇 1/3 — 类型体系与参数化: 字段是怎么定义的

> 前置: [[E-3-translog-03]] (索引写入前置) | 复用: — | 对照: [[r21-db]] (Redis 无模式) [[rd3-codec]] (客户端 codec) | 引出: [[E-7-mapping-02]] [[E-7-mapping-03]] [[E-1-engine]] [[E-2-search]]
> 🔴 A | 来源: FieldMapper.java:57,595 + MapperService.java:52,376 + MapperRegistry.java:24
> 定位: Mapping 卷开篇 — 回答"一个字段定义包含哪些设计决策 + 为什么 ES 必须强 schema"

**读者处境**: 你用 Redis 存 JSON 随手放, 用 MySQL 要建表。ES 呢? 你要先写一个 mapping: `{"type": "text", "fields": {"keyword": {"type": "keyword"}}}` — 为什么不能像 Redis 一样直接存? 面试官问 "ES 为什么要 schema? text 和 keyword 到底差在哪?" 这篇回答: 字段定义背后的设计决策 + 无模式对照。

### 1. 问题引入 — 一个字段定义, 藏着多少决策?

场景: 同一个 JSON 字段 "age": 25 — 存成 text (全文搜) / keyword (精确匹配) / integer (范围查询) 三个类型, 底层 Lucene 索引结构完全不同。
- 类型系统: MapperRegistry.java:24-31 注册表: 类型名 → TypeParser — "text"/"keyword"/"long" 等 30+ 类型
- 本篇问题: 字段定义的骨架是什么? 怎么演化来的?

### 2. Mapper 三级类型体系

场景: Mapper 接口下面有什么?
- Mapper (抽象, L23): name/typeName/parse — 基类
- FieldMapper.java:57: 值字段 — parseCreateField 抽象 (FieldMapper.java:250) — 子类: Text/Keyword/Number/Date/Boolean/Binary/Range/Completion (8 大)
- ObjectMapper.java:35: 对象容器 (object/nested)
- MetadataFieldMapper: 元字段 (_id/_source/_routing/_seq_no/_version)
- 关键设计: 为什么分三级? — 值字段 (单个值) / 对象 (层级) / 元数据 (系统字段) 生命周期不同

### 3. FieldMapper.Parameter — 声明式参数体系 (核心)

场景: 为什么 2020 年要重构出 Parameter?
- Parameter 五元组 (FieldMapper.java:595-660): name / defaultValue / parser / initializer / serializer + mergeValidator + conflictSerializer
- **合并策略内建** (FieldMapper.java:641-643): `mergeValidator = updateable ? true : Objects.equals(previous, toMerge)` — 可更新参数直接覆盖, 不可更新必须相等
- 实例: KeywordFieldMapper 14 个 Parameter (KeywordFieldMapper.java:142-177): indexed/docValues/stored/nullValue/ignoreAbove/indexOptions/norms/similarity/normalizer...
- 互斥约束: requires/precludes (FieldMapper.java:611-612) — 如 script 与 indexed 互斥
- 时空溯源: v0.90 手写 Builder (NumberFieldMapper.Builder 手写 precisionStep L95-106) → 2020-08-12 逐 mapper 转 Parameter (#60645) → 2020-11-02 合并回 FieldMapper (#64365)

### 4. 合并冲突 — 映射不能随便改

场景: 索引建好后想改字段类型?
- 类型改变直接抛: checkIncomingMergeType (FieldMapper.java:402-412) "cannot be changed from type [text] to [keyword]"
- 参数冲突聚合: Conflicts 收集器 (FieldMapper.java:1168-1186) — "Mapper for [name] conflicts with existing mapper:\n\t..." 一次报全
- **成功合并路径** (FieldMapper.java:798-810): mergeValidator.canMerge 通过 → `setValue(value)` 直接覆盖 (updateable 参数); 失败 → addConflict
- 合并入口: MapperService.merge (MapperService.java:376-400): 多源合并 + 与现有相同短路返回
- 关键设计: 为什么 type 不可改? — 索引面物理结构已写入 (倒排/docValues/points), 无法迁移

### 5. 收束 — 无模式对照: 为什么 ES 必须 schema

- Redis 键空间: 裸字节, 无类型 — 写入即存, 查询全扫 (对照 [[r21-db]])
- ES 强模式: 索引面 (倒排/docValues/points/stored) 写入前必须确定 — DateFieldMapper.indexValue 三路 (LongField/SortedNumericDocValuesField/LongPoint, L924-937)
- 对照结论: 查询能力 vs 写入灵活性的根本取舍 — 这就是"搜索引擎要建表, 缓存不用"
- 引出: 篇 2 (解析路径: 定义好的字段怎么消费) — 篇 3 (动态映射: 没定义怎么办)

### 核心悬念
"Redis 不用建表, ES 为什么必须?" — 因为倒排索引的每个索引面都要在写入前定型, schema 是搜索能力的物理前提。

### 概念依赖链
Q1 Parameter 体系 → Q6 合并冲突 → Q8 无模式对照 → 时空溯源

### 源码锚点清单
- FieldMapper.java:23 (Mapper 基类) / 57 (FieldMapper) / 68-74 (ignore_malformed/coerce 默认) / 250 (parseCreateField 抽象) / 383-412 (类型冲突) / 595-660 (Parameter 五元组) / 641-643 (mergeValidator 默认) / 1168-1186 (Conflicts)
- KeywordFieldMapper.java:142-177 (14 Parameter)
- MapperService.java:52 (类) / 376-400 (merge 入口)
- MapperRegistry.java:24-31 (TypeParser 注册表)
- ObjectMapper.java:35 (对象容器) / 45-58 (Dynamic 枚举)
- DateFieldMapper.java:924-937 (三路索引面)
