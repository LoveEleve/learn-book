# 闭环笔记 q8: 边界与负面空间 — 全表更新守卫 / 手动 SQL 失效 / 类型不匹配

## 假设
逻辑删除有一组已知边界: 全表更新/自定义 SQL 不生效、类型不匹配异常、与物理删除的共存面。

## 验证过程
- **全表更新守卫**: d694f104b "当逻辑删除字段默认值为null时,阻止全表更新插件失效" (CHANGELOG 264) — update(et, 空 wrapper) 时 WHERE deleted=0 AND et 非空条件; BlockAttackInnerInterceptor 全表更新拦截与逻辑删除条件协同
- **自定义 SQL/mapper.xml 失效**: CHANGELOG 682 "修复存在 mapper.xml 情况下逻辑删除失效" — 注入 SQL 之外的 mapper.xml 手写 SQL **不做逻辑删除转换** (只有注入方法有双路径)
- **类型不匹配**: 48a61582a/2e954fa93 "增强逻辑删除入参下参数不匹配导致的异常" — 实体删除转换要求主键类型匹配 (4d5e4e45a "主键类型必须与值类型完全匹配")
- **物理/逻辑共存**: 同一方法类按表切换 (q1) — 一个表要么物理要么逻辑, 不存在混合; 但不同表可并存
- **插入不受影响**: INSERT 不涉及 deleted (DB 默认值或 null), insertFill 不填充 deleted
- **查询三路之外的裸 SQL**: RowBounds/自定义 SQL/Cursor 原生查询不带 AND deleted=0

## 代码类型
边界 (Behavioral)

## 跨域关联
- MP-5 (BlockAttack 全表更新拦截) → 与 deleted 条件协同
- MP-1 (注入器双路径) → mapper.xml 不转换的根源
- MP-7 (填充) → 类型匹配约束共享

## 结论
逻辑删除的保护面只在"注入方法"内: 手写 SQL/裸查询不做转换; 全表更新拦截与未删除条件协同防误删; 实体删除转换有主键类型匹配约束。边界 = "框架只管自己注入的 SQL"。
源码位置: CHANGELOG.md:264,682; DeleteByIds.java:84-100
