# 闭环笔记 q8: 边界 — update(wrapper) 不填充 / 逻辑删除填充 / SELECT 不处理

## 假设
填充只在 INSERT/UPDATE 命令 + 实体可定位时发生: update(wrapper) 无实体不填充; 逻辑删除 (DELETE 转 UPDATE) 会触发 updateFill; SELECT 完全不处理。

## 验证过程
- 命令过滤: MybatisParameterHandler.java:75 `INSERT == sqlCommandType || UPDATE == sqlCommandType` — SELECT/DELETE 直通 (测试: SELECT 后 id/insertOperator/updateOperator 全 null)
- update(wrapper) 不填充: Map 只有 WRAPPER 键 → extractParameters 展开 wrapper → process: 非 Map 非实体 → getTableInfo(UpdateWrapper.class) → null → 跳过。CHANGELOG 56 (3.5.6): "doc: 增加update(Wrapper)相关api无法自动填充注释" — **官方声明的不填充场景**
- 逻辑删除填充: DeleteById.java:57-67 — 有 withLogicDelete 时用 SqlMethod.LOGIC_DELETE_BY_ID ("UPDATE %s %s WHERE..." ) + **withUpdateFill 字段 getSqlSet(EMPTY) 直拼进 SET** (filter 排除 logicDelete 字段自身) + addUpdateMappedStatement (命令类型 UPDATE) → 执行时 updateFill 触发 — 逻辑删除把删除时间/操作人等字段一起填充
- CHANGELOG 93: "ActiveRecord模式下deleteById(逻辑删除)方法支持自动填充功能" (5bf9f6290)
- SimpleType 过滤: L74 非 SimpleTypeRegistry.isSimpleType — 简单类型 (String/Integer...) 直接跳过 (不可能有 fill 字段)

## 代码类型
边界 (Behavioral) + Implementation

## 跨域关联
- MP-8 (逻辑删除双路径: TableFieldInfo L406-430) → DeleteById 的 getSqlSet 直拼是 MP-8 的填充连接点
- MP-9 (ServiceImpl.update(wrapper) 变体) → wrapper-only 路径不填充的文档化边界
- M-2 (SqlCommandType 判定) → 命令过滤依据

## 结论
填充边界 = 命令 (INSERT/UPDATE) × 实体可定位 × 表有 fill 字段。三个已知不填充: SELECT/DELETE、update(wrapper) 无实体、SimpleType。逻辑删除 (UPDATE 命令) 是隐藏触发面 — 删除操作也会跑 updateFill (填充删除人/删除时间)。
源码位置: MybatisParameterHandler.java:72-79; DeleteById.java:57-67; CHANGELOG.md:56,93
