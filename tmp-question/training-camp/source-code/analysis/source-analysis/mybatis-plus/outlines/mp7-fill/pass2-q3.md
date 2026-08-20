# 闭环笔记 q3: fill 字段 SQL 直拼 (无 if 标签) — 必有值断言

## 假设
fill 字段在注入的 INSERT/UPDATE SQL 中**不生成 if 非空守卫** — 列和值始终进 SQL, 值由 MetaObjectHandler 保证非空; 而非 fill 字段按 FieldStrategy 规则 if 包裹。

## 验证过程
- TableFieldInfo.java:455-462 getInsertSqlPropertyMaybeIf: `if (withInsertFill) { return sqlScript; } return convertIf(sqlScript, ...)` — **withInsertFill 直接返回无 if 的值片段**
- TableFieldInfo.java:486-493 getInsertSqlColumnMaybeIf: 同样逻辑 — 列片段无 if
- TableFieldInfo.java:512-530 getSqlSet: `if (withUpdateFill) { return sqlSet; }` — SET 片段无 if 包裹
- 注入 SQL 侧: TableInfo.java:343-351 getAllInsertSqlPropertyMaybeIf (MP-1 的 Insert 方法类调用) — fill 字段列/值直拼
- convertIf 生成的 if 是 `<if test="字段 != null">` 形式 (FieldStrategy 判定) — fill 字段跳过这层
- 推论: 用户不实现 MetaObjectHandler 或 fill 后值仍 null → INSERT 语句该列为 null 直入 → DB 约束兜底 (NOT NULL 会报错); 这是"直拼"的设计代价

## 代码类型
Algorithmic (SQL 脚本片段策略)

## 跨域关联
- MP-1 (AbstractMethod Insert/Update 方法类用 getAllInsertSqlColumnMaybeIf) → fill SQL 直拼的生成方
- MP-2 (TableFieldInfo.fieldFill 来自 @TableField(fill) L220-222) → 直拼开关
- MP-6 (getVersionOli 同类: convertIf 非空守卫) → 对照: version 字段有 if 守卫, fill 字段无

## 结论
fill 字段 = "必有值断言"的 SQL 直拼: 注入时列+值无 if 包裹 (保证填充字段一定参与 INSERT/SET), 值非空由执行期填充保证; 与乐观锁 version 字段 (if 守卫) 形成对照 — 两条"执行期注入"路线的 SQL 侧差异。
源码位置: TableFieldInfo.java:455-462, 486-493, 512-530; TableInfo.java:343-382
