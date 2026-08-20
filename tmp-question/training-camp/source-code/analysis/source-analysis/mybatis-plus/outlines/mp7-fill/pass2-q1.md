# 闭环笔记 q1: 填充 hook 为什么在 ParameterHandler 构造时执行?

## 假设
MP 通过覆写 MyBatis 的 ParameterHandler 在 SQL 参数绑定前填充实体 — 填充必须发生在 setParameters 之前, 且每次执行都要触发。

## 验证过程
- grep LanguageDriver.createParameterHandler → LanguageDriver.java:44 (MyBatis)
- grep XMLLanguageDriver.createParameterHandler → XMLLanguageDriver.java:37 实现
- MybatisXMLLanguageDriver.java:45-46 (MP): `return new MybatisParameterHandler(mappedStatement, parameterObject, boundSql);` — 覆写 createParameterHandler
- BaseStatementHandler.java:70 (MyBatis): `this.parameterHandler = configuration.newParameterHandler(...)` — StatementHandler 构造时创建
- SimpleExecutor.java:48,62,75: 每次 query/update 都 `configuration.newStatementHandler(...)` → **每次 SQL 执行都 new 一个 ParameterHandler**
- MybatisParameterHandler.java:64-70: 构造器 **super 先 (L65, DefaultParameterHandler 构造存参)**, 然后 processParameter(parameter) L69 — 填充发生在 setParameters (参数绑定) 之前
- MyBatis 的 DefaultParameterHandler.setParameters 在 PreparedStatementHandler.parameterize 时调用 (M-2 已述)

## 代码类型
Implementation (扩展点覆写) + Glue (MP 与 MyBatis 内核的胶水)

## 跨域关联
- M-6 (LanguageDriver 可插拔扩展点) → 本机制是 LanguageDriver 扩展点的实际消费
- M-2 (DefaultParameterHandler 取参四路) → MP 在构造时先改实体, setParameters 时取到的是填充后的值

## 结论
填充 hook 挂在 ParameterHandler **构造时** (每次执行必触): super 存参后立刻 processParameter 改实体, 稍后 setParameters 绑定时读到的是填充后的值。幂等性由 fillStrategy"有值不覆盖"保证 (q6)。
源码位置: MybatisXMLLanguageDriver.java:45-46; BaseStatementHandler.java:70; MybatisParameterHandler.java:64-70
