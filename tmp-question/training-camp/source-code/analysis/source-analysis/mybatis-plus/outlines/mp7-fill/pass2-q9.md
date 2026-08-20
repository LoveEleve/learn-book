# 闭环笔记 q9: MybatisUtils 与 MP-7 的关系澄清

## 假设
HANDOFF-STAGE3 §七 将 MybatisUtils 列为 MP-7 源码 — 需验证其真实作用 (是否与填充相关)。

## 验证过程
- MybatisUtils.java (111 行) 三个方法:
  1. newJsonTypeHandler (L38-57): IJsonTypeHandler 实例化 (MP-2 的 autoResultMap 用, resultMap 三态)
  2. getSqlSessionFactory (L66-80): 从 MybatisMapperProxy 反射提取 SqlSessionFactory (MP-9 用)
  3. getMybatisMapperProxy (L89-109): AOP 代理穿透取 MybatisMapperProxy (MP-9 用)
- grep 填充相关: MybatisUtils 无任何 MetaObjectHandler/insertFill/updateFill/StrictFill 引用
- 结论: MybatisUtils 与自动填充**无直接关系** — 是 MP-2 (JSON handler) / MP-9 (代理提取) 的工具类, HANDOFF 将其列为 MP-7 源码为**误导性提示** (可能是相邻 handlers/ 包文件的联想)

## 代码类型
Glue (工具)

## 跨域关联
- MP-2 (IJsonTypeHandler 每实例化) → newJsonTypeHandler
- MP-9 (getMybatisMapperProxy 代理提取) → getMybatisMapperProxy/getSqlSessionFactory

## 结论
MybatisUtils 不是 MP-7 组件 — 归属 MP-2/MP-9。MP-7 真正源码面: MetaObjectHandler + StrictFill + MybatisParameterHandler + MybatisXMLLanguageDriver + TableFieldInfo fill 段 + TableInfo fill 聚合 + GlobalConfig.metaObjectHandler。
源码位置: MybatisUtils.java:38-109; (对照) HANDOFF-STAGE3.md §七
