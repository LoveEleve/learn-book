# MP-8 逻辑删除 — Pass 1 探索笔记

> 域: MP-8 逻辑删除 (core/metadata + core/injector) | 🟡 方案 B | 2026-08-13
> 源码: mybatis-plus-core/src/main/java/com/baomidou/mybatisplus/core/
> 已读测试: LogicDelTest (deleteById/delete(wrapper)/deleteByIds 实体集合 + deleteBy 填充实证), TableInfoHelperTest (组合注解 @MyTableLogic 穿透 + 值断言), H2 配置 (logicDeleteValue="NOW()"/logicNotDeleteValue="NULL")

## 继承树/调用图

```
@TableLogic (annotation 模块, 40 行)          GlobalConfig.DbConfig (config/GlobalConfig.java)
  ├─ value: 未删除值 (默认空→全局)             ├─ logicDeleteField (全局属性名, 默认 null)
  ├─ delval: 已删除值 (默认空→全局)             ├─ logicDeleteValue = "1" (默认已删除)
  └─ 组合注解可穿透 (AnnotationUtils)           └─ logicNotDeleteValue = "0" (默认未删除)

TableFieldInfo.initLogicDelete (L406-430) ← 双路径: @TableLogic 注解值优先 / 全局属性名兜底
  → logicDelete = true + logicNotDeleteValue/logicDeleteValue

TableInfo.setFieldList 聚合 (L493-517):
  → logicDeleteFieldInfo = 该字段 + withLogicDelete = true
  → Assert 多个 @TableLogic 抛错 (fail-fast)

SQL 生成 (注入期, MP-1):
  AbstractMethod.sqlLogicSet (L106-108): "SET " + getLogicDeleteSql(false, false) ← 删除值
  TableInfo.getLogicDeleteSql (L437-448): startWithAnd + isWhere 双语义
  TableInfo.formatLogicDeleteSql (L450-470): isWhere=true→未删除值(查询), false→删除值(SET)
    → "NULL" 字符串特例: IS NULL (查) / =NULL (删); charSequence → '值' 加引号
  方法类双路径 (动态二选一):
    Delete.java: isWithLogicDelete → LOGIC_DELETE (UPDATE) + addUpdateMappedStatement
                            否则 → DELETE (物理) + addDeleteMappedStatement
    DeleteById.java:57-71 / DeleteByMap / DeleteByIds: 同构
  withUpdateFill 直拼: DeleteById.java:58-66 filter(isWithUpdateFill).filter(!isLogicDelete) → getSqlSet

查询注入 (三路):
  1. SelectById.java:50 / SelectBatchByIds:51: WHERE id=#{id} + getLogicDeleteSql(true,true) (AND deleted=0)
  2. AbstractMethod.sqlWhereEntityWrapper (L228-260): 逻辑删除分支 convertWhere(getLogicDeleteSql(false,true) + ...) — wrapper 路径
  3. TableInfo.getAllSqlWhere (L393-414): ignoreLogicDelFiled=true 排除 deleted 字段自身 (不参与普通条件)

更新面:
  UpdateById.java:49: sqlSet(isWithLogicDelete, ...) → SET 排除 deleted; additional=optlockVersion + getLogicDeleteSql(true,true) → AND deleted=0
  Update.java:45: sqlSet(true, true, ...) 恒排除 deleted

执行期 (MP-7 连接):
  MybatisParameterHandler.updateFill: 逻辑删除 SQL 是 UPDATE 命令 → 触发 updateFill
  DeleteById 的 SET 里 withUpdateFill 字段直拼 → 删除人/删除时间自动填充 (LogicDelTest deleteBy 实证)
```

## 基本元素分解

1. **元数据标记**: @TableLogic(value/delval) + 全局 DbConfig 兜底 → TableFieldInfo.logicDelete + 双值
2. **SQL 片段生成**: getLogicDeleteSql(startWithAnd, isWhere) 双语义 + formatLogicDeleteSql 值渲染 (引号/NULL 特例)
3. **方法类双路径**: 同一注入方法类根据 isWithLogicDelete 动态生成 UPDATE(逻辑) 或 DELETE(物理) — 命令类型同步切换
4. **查询三路注入**: 主键查询尾缀 / wrapper 路径 convertWhere / 普通 where 排除 deleted 自身
5. **更新排除**: SET 里 deleted 字段恒排除 (logic 参数), 更新带 AND deleted=0
6. **填充协同**: withUpdateFill 直拼 SET + UPDATE 命令触发 updateFill (MP-7)

## 标记问题 (8 个)

1. 为什么 deleteById 用 isWithLogicDelete 动态二选一 (UPDATE vs DELETE)? 命令类型怎么切换? 物理/逻辑能否共存?
2. value/delval 双值 + "0"/"1" 默认 + 全局兜底 + "NULL" 字符串特例 (IS NULL/`=NULL`) + charSequence 引号 — 值渲染规则?
3. getLogicDeleteSql 的 isWhere 语义为什么反直觉 (isWhere=true 用未删除值)?
4. 查询三路 (SelectById 尾缀/wrapper convertWhere/普通 where 排除) 各自的拼装点? deleted 字段为什么不进普通 where/set?
5. 更新面: updateById 为什么带 AND deleted=0? SET 为什么恒排除 deleted? 更新已删记录会发生什么?
6. 批量删除: DeleteByIds 的 foreach 双形态 (SimpleType→#{item}/实体→#{item.id})? 实体集合删除转换 (主键类型匹配)?
7. 表级聚合与 fail-fast: 多个 @TableLogic 抛错? logicDeleteFieldInfo 怎么选?
8. 与 MP-7 的连接: 逻辑删除触发 updateFill 的完整链路? withUpdateFill 直拼 SET 的字段过滤?

## 测试要点 (已读 3 个)

- LogicDelTest: deleteById 后查询记录仍在 (deleted=true); delete(wrapper)/deleteByIds(实体集合) 均逻辑删除; **deleteBy 字段 (fill=UPDATE) 在逻辑删除时被填充** ("聂秋秋" 断言)
- TableInfoHelperTest: 组合注解 @MyTableLogic (value=false/delval=true) 穿透 — 值断言 (逻辑删除双值来自组合注解)
- H2 全局配置: logicDeleteValue="NOW()" / logicNotDeleteValue="NULL" → 查询条件 deleted IS NULL (NULL 特例实证); 自定义注入 LogicDeleteByIdWithFill
