# MP-3 Lambda 条件构造器 — completeness-questions

## 开发者视角

1. User::getName 方法引用怎么变成列名?能绕过方法引用直接用字符串吗(QueryWrapper)?
2. 为什么条件参数在 SQL 里是 #{ew.paramNameValuePairs.MPGENVAL1}?MPGENVAL 序列怎么来的?
3. eq 方法的第一个参数 boolean condition 是干什么的?false 时条件会怎样?
4. eq().or().eq() 拼出的 SQL 长什么样?第一个 eq 前有 AND 吗?连续 or 呢?
5. and(c -> c.eq(a).or().eq(b)) 的括号语义怎么实现?
6. select(User::getName, User::getAge) 和 select(entityClass, 谓词) 两种方式区别?
7. lambdaQuery() 链式最后怎么触发查询?list() 内部调谁?
8. IDEA 调试模式下 lambda 表达式会怎样?为什么会有三种提取方式?

## 架构师视角

9. SerializedLambda 的 resolveClass 重写解决了什么问题?为什么不用 JDK 的 SerializedLambda?
10. 三路提取降级链 (IDEA 代理/反射 writeReplace/序列化兜底) 各自应对什么环境?
11. 列名解析为什么坚持"方法引用→属性名→ColumnCache"而不直接字符串?与 MP-2 的耦合设计?
12. addCondition 的统一入口设计 (maybeDo+appendSqlSegments+formatParam) 怎么保证所有条件方法行为一致?
13. NormalSegmentList.transformList 的净化规则 (首段/相邻同类/not) 覆盖了哪些用户书写错误?
14. MergeSegments 四段 (normal/groupBy/having/orderBy) 分离的意义?getSqlSegment 拼接顺序?
15. 参数命名 MPGENVAL 序列与注入 SQL 的 #{ew.paramNameValuePairs.xxx} 怎么对接(导航 MP-1)?

## 学生视角

16. User::getName 到 "user_name" 的完整链路 (序列化→methodToProperty→formatKey→ColumnCache)?
17. 一次 lambdaQuery().eq(...).list() 的完整调用链?
18. Wrapper 的 getSqlSegment 输出什么?和 M-6 的 SqlNode 有什么对照?
19. 嵌套条件 (nested/and/or Consumer) 与 SQL 括号的对应关系?
20. QueryWrapper 与 LambdaQueryWrapper 的区别?为什么都要保留?
