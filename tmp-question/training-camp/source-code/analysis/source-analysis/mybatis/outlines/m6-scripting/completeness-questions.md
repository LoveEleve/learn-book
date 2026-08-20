# M-6 动态 SQL — completeness-questions

## 开发者视角

1. `<where>` 标签为什么能吃掉第一个条件前的 AND/OR?底层是哪个类?它和 `<trim>` 什么关系?
2. `<set>` 标签怎么去掉 SET 子句里多余的逗号?和 `<trim suffixOverrides=",">` 等价吗?
3. foreach 里 `#{item}` 执行时变成了什么?为什么嵌套 foreach 用相同 item 名不会冲突?
4. `<when test="...">` 为什么能复用 if 的处理器?`<otherwise>` 和 `<choose>` 的关系是什么?
5. `${}` 和 `#{}` 的区别是什么?${} 为 null 时拼接什么?injectionFilter 是干什么的?
6. 静态 SQL (无动态标签) 和动态 SQL 的执行性能差异在哪?isDynamic 是谁决定的?
7. `<bind name="xxx" value="..."/>` 生成什么节点?绑定的变量在哪可用?
8. SQL 里出现未知标签 (如 `<foo>`) 会怎样?什么时候报错?
9. 注解里 `@Select("<script>...</script>")` 和 XML mapper 的解析路径有什么异同?

## 架构师视角

10. XMLScriptBuilder 的 Handler 注册表 (9 标签) 设计 — 新增一个标签要改哪些地方?when→IfHandler 复用的意义?
11. isDynamic 作为解析器成员变量传播 — 一棵树一个判定; 若改成按节点判定会有什么问题?
12. 动态/静态分流 (DynamicSqlSource vs RawSqlSource) 的性能设计: 静态 SQL 的 #{} 解析发生在哪一步?为什么动态 SQL 无法在构建期解析?
13. ContextMap 的四层取值回退 (containsKey→null→fallbackParameterObject→metaObject) 各覆盖什么参数形态?issue#61 读不改的意义?
14. __frch_ 参数唯一化的完整链路: FilteredDynamicContext 替换 → bind item_i → SqlSourceBuilder 收集 → BoundSql.setAdditionalParameter — 每一步谁在做?
15. TrimSqlNode 的"先缓冲后修剪" (FilteredDynamicContext+applyAll) 解决什么问题?为什么不能边拼边修?
16. ${} 的 injectionFilter 为什么默认不开启?设计上的取舍是什么?

## 学生视角

17. 一次动态 SQL 执行的两段式: 构建期 (建树) 和执行期 (apply 求值) 分别做了什么?
18. SqlSource 家族 (SqlSource→DynamicSqlSource/RawSqlSource/StaticSqlSource) 的继承关系和作用?
19. TextSqlNode.isDynamic 用什么机制检测 ${}?GenericTokenParser 的作用?
20. DynamicSqlSource.getBoundSql 为什么要把 bindings 写进 BoundSql.setAdditionalParameter?M-5 的参数绑定怎么用到它们 (导航)?
21. OgnlCache 的表达式编译缓存解决什么问题?OgnlClassResolver 定制了什么?
