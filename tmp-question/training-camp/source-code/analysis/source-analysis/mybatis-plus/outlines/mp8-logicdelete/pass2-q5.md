# 闭环笔记 q5: MP-7 连接 — 逻辑删除触发 updateFill + withUpdateFill 直拼 SET

## 假设
逻辑删除的 DELETE 转 UPDATE 使删除操作成为填充的隐藏触发面: 删除人/删除时间等 withUpdateFill 字段在删除时自动填充。

## 验证过程
- 命令类型: DeleteById.java:65-67 → addUpdateMappedStatement (q1) → MappedStatement.sqlCommandType=UPDATE
- 执行期: MybatisParameterHandler.processParameter (MP-7): UPDATE 命令 + 实体可定位 → updateFill (MP-7 q8 已证) → handler.updateFill 填充
- SQL 侧: DeleteById.java:58-64 — `filter(TableFieldInfo::isWithUpdateFill).filter(f -> !f.isLogicDelete())` → withUpdateFill 字段 `getSqlSet(EMPTY)` 直拼进 SET (无 if 包裹, MP-7 q3) + **`!isSimpleType(_parameter)` 运行时守卫 (L64 convertIf)** — 简单类型参数 (deleteById(1L)) 时填充 SET 整体不生成, 只剩 SET deleted=1
- 执行期一致性: MybatisParameterHandler.processParameter 最外层 `!SimpleTypeRegistry.isSimpleType(parameter.getClass())` 过滤 (MP-7 q1) — deleteById(id) 不触发 updateFill, 与 SQL 守卫双重一致
- DeleteByIds.logicDeleteScript L80-100: 同构 — withUpdateFill 字段 (ENTITY 前缀) + convertIf 实体非空 + logicDeleteSql
- 排除自身: `.filter(f -> !f.isLogicDelete())` — deleted 字段即使标了 fill (LogicDelTest 的 Entity 双注解) 也不进填充 SET (防止填充覆盖删除标记)
- LogicDelTest 实证: deleteBy @TableField(fill=UPDATE) → 逻辑删除后断言 deleteBy="聂秋秋"; deleted @TableLogic + @TableField(fill=UPDATE) 双注解 → 不进 SET
- CHANGELOG 16: "BaseMapper方法逻辑删除默认支持填充" (be2f0c3da); a46268c35: "修复逻辑删除在字段填充下存在字段重复"

## 代码类型
Glue (跨机制协同) + Algorithmic (字段过滤)

## 跨域关联
- MP-7 (updateFill 触发 + 直拼策略) → 本机制的执行端
- MP-2 (isWithUpdateFill 元数据) → 字段面
- MP-9 (deleteById 逻辑删除的 IService 层) → 使用者

## 结论
逻辑删除 = 填充的"隐藏触发面": UPDATE 命令 + withUpdateFill 字段直拼 SET, 删除操作自动带审计字段; **但仅实体参数形态** — 简单类型 id 删除 (deleteById(1L)) 无实体可填, SQL 守卫 (`!isSimpleType(_parameter)`) + 填充过滤双重一致地跳过; deleted 字段自身双重排除 (filter + 不标 fill 的实践), 删除标记由 getLogicDeleteSql 独占管理。
源码位置: DeleteById.java:57-67; DeleteByIds.java:80-100; LogicDelTest.java:83-94,127
