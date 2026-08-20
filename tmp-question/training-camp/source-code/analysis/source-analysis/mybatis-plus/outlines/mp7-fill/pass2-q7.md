# 闭环笔记 q7: 时序 — 主键先行 + 拦截器外层/填充内层

## 假设
INSERT 时主键生成 (populateKeys) 先于 insertFill; 整体时序: 插件拦截器 (乐观锁 beforeUpdate) 在 Executor 代理层, 填充在 ParameterHandler 构造 (更内层) — 拦截器先, 填充后。

## 验证过程
- MybatisParameterHandler.java:101-106: INSERT 分支 `populateKeys(tableInfo, metaObject, entity); insertFill(metaObject, tableInfo);` — **主键先, fill 后** (同一方法顺序执行)
- L111-126 populateKeys: `idType.getKey() >= 3` 才生成 (IdType: AUTO=0/NONE=1/INPUT=2/ASSIGN_ID=3/ASSIGN_UUID=4) — 只自动生成 ASSIGN_ID/ASSIGN_UUID; AUTO 交数据库, INPUT 用户自设; `identifierGenerator.assignId(idValue)` 实体已有 id 则跳过
- 时序链 (UPDATE):
  1. MybatisPlusInterceptor.intercept (MP-4): isUpdate → willDoUpdate → beforeUpdate (L84-89) — 乐观锁改实体版本值
  2. invocation.proceed() → executor.update → doUpdate
  3. doUpdate → newStatementHandler (SimpleExecutor.java:75) → BaseStatementHandler 构造 → newParameterHandler → **MybatisParameterHandler 构造 → updateFill** — 填充在乐观锁之后, 拿到的实体版本值已是乐观锁回写后的新值
- INSERT 同理: 拦截器 beforeUpdate 不拦 INSERT (只有 UPDATE), 主键+填充都在 ParameterHandler

## 代码类型
Glue (时序编排) + Implementation (主键生成)

## 跨域关联
- MP-6 (OptimisticLockerInnerInterceptor.beforeUpdate) → 先于 updateFill; 填充不影响 version 字段 (version 字段不会标 fill)
- MP-2 (idType/keyProperty) → populateKeys 数据源
- M-2 (Executor.update → doUpdate → newStatementHandler 链) → 时序锚点

## 结论
INSERT: populateKeys (主键) → insertFill; 整体: 拦截器 (乐观锁) → Executor → ParameterHandler 填充。乐观锁的版本条件先写好, 填充的审计字段后写入实体 — 两者互不干扰 (version 字段不参与 fill)。
源码位置: MybatisParameterHandler.java:101-106, 111-126; MybatisPlusInterceptor.java:84-89
