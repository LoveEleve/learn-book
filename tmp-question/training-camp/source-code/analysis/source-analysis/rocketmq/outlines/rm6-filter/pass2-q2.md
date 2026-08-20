# 闭环笔记 q2: 解析与求值 — JavaCC → AST → evaluate

## 假设
SQL92 表达式 = JavaCC 解析成 AST, 运行时 evaluate 消息属性。

## 验证过程
- **语法源** (parser/SelectorParser.jj): JavaCC 文法 (grep 实证 .jj 文件) → 生成 SelectorParser (1401 行) + TokenManager — 编译期生成 (Maven javacc 插件)
- **AST 家族** (expression/, ActiveMQ 移植 "This class was taken from ActiveMQ org.apache.activemq.filter.PropertyExpression" 注释):
  - BooleanExpression/LogicExpression (AND/OR/NOT) / ComparisonExpression (> < = BETWEEN) / UnaryInExpression (IN) / PropertyExpression (属性引用) / ConstantExpression / **NowExpression** (NOW() 时间函数)
- **evaluate** (PropertyExpression L35-38): `context.get(name)` — 消息属性查询
- **上下文** (MessageEvaluationContext, broker/filter): properties Map → get (属性直查) + keyValues (全量拷贝)
- **编译缓存**: ConsumerFilterData.compiledExpression (transient — 注册时编译, 运行期复用)

## 代码类型
Algorithmic (解析树)

## 跨域关联
- RM-5 (Broker): ConsumerFilterManager 注册编译
- RM-3 (编码): 消息属性提取

## 结论
SQL92 = JavaCC 文法 → AST (ActiveMQ 移植家族) → evaluate (属性上下文); compiledExpression 注册期编译运行期复用。
源码位置: parser/SelectorParser.jj; expression/ 家族; MessageEvaluationContext.java
