# 闭环笔记 q7: 表级聚合与 fail-fast — logicDeleteFieldInfo 选取 + 多注解抛错

## 假设
TableInfo.setFieldList 聚合逻辑删除字段: 恰好一个 @TableLogic 字段成为 logicDeleteFieldInfo, 多个抛异常 (fail-fast)。

## 验证过程
- TableInfo.setFieldList L493-517: `AtomicInteger logicDeleted` 计数 — `if (i.isLogicDelete()) { this.withLogicDelete = true; this.logicDeleteFieldInfo = i; logicDeleted.getAndAdd(1); }`
- L510-512: `Assert.isTrue(logicDeleted.get() <= 1, "@TableLogic not support more than one in Class: \"%s\"")` — **多个 @TableLogic 注入期抛错** (fail-fast)
- 全局兜底双路径: initLogicDelete (TableFieldInfo L422-429) — 无 @TableLogic 时, `dbConfig.getLogicDeleteField()` 与 property 匹配 → 也标记 logicDelete (全局属性名路径)
- 全局 + 注解并存: 注解字段优先 (L410-421 先处理注解), 全局 logicDeleteField 只在"类中无注解"时兜底 (L422 的 else if)
- withLogicDelete 下游消费: 所有方法类分支 (q1) + getLogicDeleteSql (q3) + getAllSqlWhere/Set 排除 (q3/q4)

## 代码类型
Interface (聚合契约) + Algorithmic (一致性校验)

## 跨域关联
- MP-2 (TableInfo.setFieldList 同族聚合: withInsertFill/withVersion/逻辑删除) → 表级布尔面
- MP-6 (version 同款 Assert <= 1) → 同一 fail-fast 模式
- MP-5 (分页 count 对逻辑删除表的兼容) → 消费面

## 结论
逻辑删除是"单字段特权"设计: 表级只能有一个 @TableLogic 字段 (Assert<=1 注入期抛错), logicDeleteFieldInfo 唯一化; 全局 logicDeleteField 兜底只在类中无注解时生效。与 @Version 同款 fail-fast (MP-6)。
源码位置: TableInfo.java:493-512; TableFieldInfo.java:406-430
