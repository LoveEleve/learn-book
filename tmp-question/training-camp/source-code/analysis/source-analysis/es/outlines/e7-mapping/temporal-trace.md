# E-7 Mapping — 时空溯源 (v0.90 → v8.12.2)

> 方法: git show 早期 tag + git log 关键 commit 日期实证
> 断代锚点: v0.90.0 / v1.0.0.Beta1 / v5.0.0-alpha1 / v8.12.2

## 演进主线 (4 代)

| 代 | 版本 | 结构 | 关键决策 |
|:--:|---|---|---|
| 1 | v0.90 | `mapper/` + `mapper/core/` 子包: FieldMapper/MapperService/DocumentMapper + core/StringFieldMapper + core/NumberFieldMapper + core/DateFieldMapper; DocumentMapperParser | **手写 Builder 链**: NumberFieldMapper.Builder 手写 precisionStep/ignoreMalformed 字段 + builder 方法 (NumberFieldMapper.java:67-106) — 每个 mapper 重复 parse/merge/serialize 三套代码 |
| 2 | v1.0 | 结构同 v0.90, 增加 action/admin/indices/mapping/* (PutMapping/DeleteMapping API 面) | API 完善期, 核心结构未变 |
| 3 | v5.0 | + DocumentParser (独立解析器) + MappedFieldType (字段类型分离) + FieldTypeLookup; core/ 子包并入 mapper/ | **字段类型与映射分离**: MappedFieldType 承载 Lucene 面, FieldMapper 承载解析 — v8 的 FieldMapper.parseCreateField 雏形 |
| 4 | v8.12 | 现行 123 文件: FieldMapper 1476 行 + 参数化体系 | **参数化重构 (2020)**: c81dc2b8b7d (2020-08-12, "Convert KeywordFieldMapper to parametrized form") → 各 mapper 逐个转 Parameter; a5168572d5b (2020-11-02, "Collapse ParametrizedFieldMapper into FieldMapper") → 合并回主类 |

## 三个核心设计变迁

### 1. 手写 Builder → 声明式 Parameter (2020)

```
v0.90: NumberFieldMapper.Builder 手写字段 (precisionStep/ignoreMalformed) + 手写 builder 方法 (NumberFieldMapper.java:95-106)
v8.12: KeywordFieldMapper 声明 14 个 Parameter (indexed/docValues/stored/nullValue/ignoreAbove/indexOptions/norms/similarity... L142-177)
       统一: 解析 (parser) + 校验 (validator) + 序列化 (serializer) + 合并 (mergeValidator) 四件事收敛进五元组
```
- 重构路径: 2020-08 起逐 mapper 转换 (Keyword 首个, #60645) → 2020-11 全部合并回 FieldMapper (#64365)
- 设计收益: 合并冲突策略 (updateable) 内建, 序列化默认值控制 (serializerCheck), 参数互斥 (requires/precludes)

### 2. 字段类型与解析分离 (v5.0)

```
v0.90: FieldMapper 直接持有 Lucene FieldType + 解析逻辑混在一起
v5.0:  MappedFieldType (查询面: termQuery/rangeQuery/fielddataBuilder)
       FieldMapper (写入面: parseCreateField)
       FieldTypeLookup (名称→类型查找)
```
- 设计原因: 查询与写入两个生命周期 (写一次读多次) 需要独立演化 — E-2 Search 消费 MappedFieldType, E-1 Engine 消费 FieldMapper

### 3. dynamic 三态 → 四态 (8.x)

```
v5.0:  Dynamic.TRUE/FALSE/STRICT 三态 (ObjectMapper.Dynamic)
v8.12: + RUNTIME (动态字段建 runtime field 而非具体类型)
```
- 09 审计的"三态"遗漏 RUNTIME — 8.x 为 runtime fields 能力新增

## 对照 Redis (r21-db)

- Redis 键空间: 写入即存裸字节, 无类型概念 (v0.90 至今未变)
- ES mapping: 类型体系 4 代演进, 从手写 Builder 到声明式参数 — 复杂度根因 = 倒排索引需要写入前确定索引面
- 结论: "无模式演化慢 (Redis 无类型) vs 强模式演化频繁 (ES 每版本调类型)" — 两种数据模型的自然结果

## REVIEW 修正记录 (2026-08-14)

- ⚠️ ES-PLAN 声称 "dynamic 三态" — 实测四态 (TRUE/FALSE/STRICT/RUNTIME) → 已在 Q7 闭环标注
- ✅ 参数化重构断代: 2020-08-12 (c81dc2b8b7d) / 2020-11-02 (a5168572d5b) 日期实证

## 完成检查

- [x] v0.90 手写 Builder 已读 (NumberFieldMapper L67-106)
- [x] v5.0 MappedFieldType 分离已读 (FieldMapper L247-268)
- [x] 参数化重构 commit 日期实证 (2 个)
- [x] dynamic 四态修正记录
