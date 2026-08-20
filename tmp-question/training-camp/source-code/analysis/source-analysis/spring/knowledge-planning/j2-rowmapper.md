# J-2 RowMapper + ResultSetExtractor + NamedParameterJdbcTemplate

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 5接口/类
> 基线: J-1 JdbcTemplate — query方法接收RowMapper作为参数

---

## §0.8

- 🟡 Working，1篇 — RowMapper(逐行映射) vs ResultSetExtractor(全结果集处理) + BeanPropertyRowMapper + NamedParameterJdbcTemplate(:name占位符)

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RowMapper.java:52 | mapRow(rs, rowNum) | **逐行**: JdbcTemplate内部遍历ResultSet→每行调mapRow→返回T—RowMapper不处理ResultSet遍历 | High |
| ResultSetExtractor.java:52 | extractData(rs) | **全控制**: 接收整个ResultSet—自己决定遍历/跳过/聚合—适合复杂聚合(如GROUP BY结果转Map) | High |
| BeanPropertyRowMapper.java:92 | mapRow | **自动映射**: 通过反射: ResultSet column名→Bean属性名(驼峰/下划线自动转换)→setProperty | High |
| NamedParameterJdbcTemplate: :name | 占位符 | `:name`占位符替代`?`—内部通过ParsedSql解析→Map参数→PreparedStatement?占位符 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: J-1 的进阶补充—RowMapper/ResultSetExtractor是两种结果集处理策略(:行级vs全集级)—BeanPropertyRowMapper是RowMapper的常用实现—NamedParameterJdbcTemplate是参数化的语法糖。1篇(~35行)。

**P1 核心** 🔴: RowMapper(逐行) vs ResultSetExtractor(全控制) — **为什么两个接口** 🔴** 🔴: RowMapper适合"逐行映射为对象→返回List"—ResultSetExtractor适合"整个ResultSet聚合为一个结果"(如 Map<Long, List<Item>> 分组)—前者简单、后者灵活
