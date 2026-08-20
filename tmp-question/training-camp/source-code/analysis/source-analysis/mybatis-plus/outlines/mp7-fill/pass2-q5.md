# 闭环笔记 q5: 三重开关注门 + 3.5.6 MappedStatement 签名动机

## 假设
填充是否执行由 3 层开关共同决定: handler 级 open 双签名 + 元数据级 isWithInsertFill; 3.5.6 加 MappedStatement 参数是为了按 mapper/方法精确跳过填充。

## 验证过程
- MybatisParameterHandler.java:128-142:
  - insertFill: `metaObjectHandler.openInsertFill() && metaObjectHandler.openInsertFill(mappedStatement) && tableInfo.isWithInsertFill()` → insertFill(metaObject)
  - updateFill: 同构 (openUpdateFill ×2 + isWithUpdateFill)
- MetaObjectHandler.java:43-78: openInsertFill() @Deprecated (默认 true) / openInsertFill(MappedStatement) @since 3.5.6 (默认 true) — 双签名并存, 旧签名 deprecated
- TableInfo.java:503-508 setFieldList: 任一字段 isWithInsertFill → this.withInsertFill = true (聚合)
- TableFieldInfo.java:221-222: withInsertFill = fieldFill==INSERT || INSERT_UPDATE
- 3.5.6 动机 (cd0238821, 2024-03-31 "新增参数填充器跳过方式(基于MappedStatement#id)"): 用户可按 MappedStatement.id (mapper 方法全名) 跳过特定方法的填充 — 用 ms.getId() 匹配即可, 不用全局关
- GlobalConfigUtils.java:110-111: getMetaObjectHandler(configuration) → Optional (handler 未配置则 Optional.empty → 不填充, 静默跳过)

## 代码类型
Interface (开关契约) + Glue (配置路由)

## 跨域关联
- M-1 (MappedStatement.id = namespace.method 全名, M-3 SqlCommand) → ms.getId() 可按 mapper 方法精确匹配
- MP-2 (TableInfo.isWithInsertFill 聚合) → 元数据层守门
- MP-4 (Interceptor 的 @InterceptorIgnore 各插件自查) → 对照: 插件用注解自查, 填充用 open 回调自查 — 同族"宿主不感知"模式

## 结论
三层守门: open 旧签名 (全局) AND open 新签名 (按 ms 精确跳过) AND isWithInsertFill (表级有无 fill 字段); handler 未配置 (Optional 空) 则整链静默跳过。3.5.6 的 ms 参数是"按 mapper 方法级"控制扩展。
源码位置: MybatisParameterHandler.java:128-142; MetaObjectHandler.java:43-78; GlobalConfigUtils.java:110-111
