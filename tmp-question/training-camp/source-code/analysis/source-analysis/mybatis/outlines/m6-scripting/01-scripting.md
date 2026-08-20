# M-6 动态 SQL — XML→SqlNode 树→动态/静态分流→运行时求值
> 前置: [[M-1-configuration]] (LanguageDriverRegistry) | 复用: [[M-2-executor]] (getBoundSql 消费点) | 对照: [[D-6-parser]] (Druid 参数化 AST vs MyBatis SqlNode 树) | 引出: [[M-5-mapping]] [[MP-3-lambda-wrapper]]
> 🔴 Deep | 9 KP | [模式: 解释器+组合+策略注册表+模板方法]
> Pass 2 闭环: q1(分流时机) q2(Handler 注册表) q3(__frch_ 唯一化) q4(Trim 家族特化) q5(ContextMap 四层回退) q6(${} vs #{}) q7(运行时附加参数) q8(RawSqlSource 构建期) q9(OGNL 缓存)

**读者处境**: XML 里 `<where><if test="name != null">AND name=#{name}</if></where>` — 为什么首个条件前 AND 被吃掉?foreach 里 `#{item}` 为何变 `#{__frch_item_0}`?静态 SQL 为何比动态快?这篇拆两段式: 构建期建树、执行期求值。

### 1. 入口双路径 — XMLLanguageDriver.createSqlSource

场景: 注解里的 `<script>` SQL 和 XML mapper 里的 SQL 走同一套解析吗?
源码路径:
- `XMLLanguageDriver.java:30-48` — createSqlSource 双入口(q1): XNode→`XMLScriptBuilder.parseScriptNode`(L31-34); String→`<script>` 注解分支(L36-37, issue#3)→XPathParser; 普通字符串→`PropertyParser.parse` ${} 变量替换(L39, issue#127)→TextSqlNode.isDynamic 分流(L41-47); "XML"→XMLLanguageDriver 别名注册(M-1)
关键设计: 双入口统一到同一套节点解析 — XML/注解/字符串三种来源殊途同归; ${} 变量入口先替换(configuration.variables, M-1 三级合并产物)。[模式: 门面+统一管线]
数据流: XNode→XMLScriptBuilder.parseScriptNode / String→TextSqlNode.isDynamic 分流 → SqlSource(Dynamic/Raw)。

### 2. 解析骨架 — 9 标签 Handler 注册表 + parseDynamicTags 递归

场景: `<when>` 为什么能复用 IfHandler?未知标签何时报错?一个 SQL 里有多个 if 时 isDynamic 怎么判定?
源码路径:
- `XMLScriptBuilder.java:53-63` — **9 标签 Handler 映射表**: trim/where/set/foreach/if/choose/when(→IfHandler)/otherwise/bind
- `XMLScriptBuilder.java:76-101` — parseDynamicTags 递归(q2): 文本节点→`TextSqlNode.isDynamic()`(${} 检查)→动态/StaticTextSqlNode 分流(L81-89); 元素节点→查表, 未知抛 "Unknown element"(L93-95); **isDynamic 成员变量传播**(L86,97); parseScriptNode 分流(q1): isDynamic→DynamicSqlSource/否则 RawSqlSource(L65-74)
- `XMLScriptBuilder.java:226-248` — ChooseHandler: when/otherwise 收集, 多个 otherwise 抛 "Too many default"(L244-245)
关键设计: 注册表=策略模式(q2) — 标签→处理器映射, when 复用 IfHandler 的注册表技巧; 递归下降: 每个标签 Handler 内再调 parseDynamicTags 解析子节点 → 任意嵌套; isDynamic 是解析器实例级标志, 子节点置 true 传播到根 → 一棵树一个判定。[模式: 策略注册表+递归下降]
数据流: parseScriptNode → parseDynamicTags(根) → 子节点递归 → MixedSqlNode(根, children) → isDynamic? Dynamic : Raw。

### 3. 节点族 — SqlNode.apply(DynamicContext) 契约

场景: 每个标签的 apply 如何协作拼出 SQL?
源码路径:
- `SqlNode.java:23` — 接口契约: `boolean apply(DynamicContext context)` 返回值=内容是否应用; IfSqlNode: `evaluator.evaluateBoolean(test, bindings)` OGNL 求值 → true 才 contents.apply
- `ForEachSqlNode.java:69-137` — apply: `evaluateIterable(collection)`→null/空返回→applyOpen(open)→每元素:`PrefixedContext(首元素 ""/其余 separator)` 包装+applyIndex/applyItem(`itemizeItem`=**ITEM_PREFIX+item+"_"+i**, L28,137 `__frch_`)→contents.apply(FilteredDynamicContext)→applyClose→remove item/index bindings
- `TrimSqlNode.java:56-60,89-99` + `WhereSqlNode.java:25-32` — Trim: 缓冲→applyAll: trim→toUpperCase→applyPrefix(overrides 匹配移除→加 prefix)→applySuffix; **Where=Trim("WHERE",[AND/OR…])/Set=Trim("SET",[,],null,[,]) 特化**(q4, suffixOverrides 去尾逗号); ChooseSqlNode 依次尝试 when 全不中走默认
关键设计: 组合模式(q4): 树形 apply 递归拼 SQL — 每节点只处理自己的片段; __frch_ 唯一化(q3): 嵌套 foreach 同 item 名不冲突, FilteredDynamicContext replaceFirst 只替换 item 开头的 #{} 引用; Trim 家族"先缓冲后修剪"解决 AND/逗号粘连。[模式: 组合+装饰]
数据流: rootSqlNode.apply(context) → MixedSqlNode 逐个 apply → If/ForEach/Trim 各管片段 → context.appendSql 累积。

### 4. 求值上下文 — DynamicContext 与 ContextMap 四层回退

场景: `<if test="name != null">` 里的 name 从哪来?Map 参数/POJO/裸值三种参数怎么统一?
源码路径:
- `DynamicContext.java:32-54` — 构造: 非 Map 参数→`MetaObject` 包装+`hasTypeHandler` 判定; bindings 初始 `_parameter`/`_databaseId`(L53-54)
- `DynamicContext.java:78-94` — **ContextMap.get 四层回退**(q5): ①containsKey 直返 ②metaObject==null→null ③fallbackParameterObject&&!hasGetter→原对象(裸值) ④metaObject.getValue — issue#61 读不改; appendSql StringJoiner(" ") 累积+uniqueNumber 供 __frch_ 编号(L65-73)
关键设计: OGNL 求值统一入口(q5): bindings 是 Map 视图, 属性访问回退到 MetaObject — 同一表达式语法支持 Map/POJO/裸值三种参数; 静态块注册 ContextAccessor(OGNL 属性访问器定制)。[模式: 适配器+回退链]
数据流: if test="name != null" → ExpressionEvaluator → OgnlCache.getValue(test, bindings) → ContextMap.get("name") → 四层回退取参。

### 5. ${} vs #{} — 双 Token 解析的语义分离

场景: ${} 和 #{} 为什么一个危险一个安全?${} 为 null 拼接什么?
源码路径:
- `TextSqlNode.java:63-83` — ${} 处理(q6): BindingTokenParser.handleToken: `OgnlCache.getValue(content, bindings)` → **null→""(issue#274, 非 "null")** → `checkInjection(injectionFilter 正则, 不匹配抛 ScriptingException)`
- `SqlSourceBuilder.java:42-52,64-76` — #{} 处理(q6): GenericTokenParser("#{","}")→ParameterMappingTokenHandler 收集 mapping→`"?"` 替换→StaticSqlSource; shrinkWhitespacesInSql 时 removeExtraWhitespaces(L48-52)
关键设计: 语义分离(q6): ${}=OGNL 求值**直接拼接**(性能好但注入风险, injectionFilter 正则兜底); #{}=ParameterMapping 收集+`?` 占位(预编译安全, 绑定在 M-5)。injectionFilter 只对 ${} 生效 — 官方推荐 #{}。[模式: 双 Token 解析器]
数据流: "${name}" → OgnlCache 求值 → 校验 → 拼接; "#{name}" → mapping+? → StaticSqlSource → BoundSql(M-5 绑定)。

### 6. 动态/静态两段式 — DynamicSqlSource 运行时 vs RawSqlSource 构建期

场景: 为什么静态 SQL 每次执行快?动态 SQL 的求值到底发生在哪一步?
源码路径:
- `DynamicSqlSource.java:30-43` — 运行时(q7): getBoundSql: `new DynamicContext(parameterObject)`→`rootSqlNode.apply(context)`(此时参数相关分支展开+__frch_ 写入)→`SqlSourceBuilder.parse(context.getSql(), type, bindings)`→getBoundSql→**`bindings.forEach(boundSql::setAdditionalParameter)`**(foreach 参数进 BoundSql)
- `RawSqlSource.java:28-49` + `StaticSqlSource.java:29-32` — 构建期(q8): 构造: getSql(apply 一次)→parse 一次性→StaticSqlSource; getBoundSql 直转 new BoundSql(零重复解析)
- `OgnlCache.java:44-58` — **OGNL 表达式编译缓存**(ConcurrentMap, parseExpression L57)+OgnlClassResolver 防类加载攻击(q9)
关键设计: 两段式(q1/q8): **构建期**建 SqlNode 树+静态 SQL 一次解析; **执行期**动态 SQL 才 apply 求值 — 静态 SQL 的 #{} 解析成本发生在启动而非每次查询; OGNL 表达式编译缓存让动态判断也不慢。[模式: 构建期/执行期分离+缓存]
数据流: 动态: query → getBoundSql(param) → apply(分支展开) → parse #{} → BoundSql+附加参数; 静态: 构建期一次 parse → 每次 getBoundSql 直转。

### 负面空间 — 动态 SQL 刻意不做的事
- **不做数据库方言拼装**: SqlNode 只产出标准 SQL 片段, 分页/方言改写归属 MP-5 插件(M-4 链)
- **不做 ${} 全量防护**: injectionFilter 仅编程式构造可传(TextSqlNode(text, filter)), XML/注解路径恒为 null — 官方靠 #{} 预编译兜底, ${} 仅限内部可信值
- **不缓存 BoundSql**: 动态 SQL 每次执行新建 BoundSql(参数化值随参数变化), 只有 OGNL 表达式编译结果被缓存

→ 引出: 求值产物 BoundSql(parameterMappings+additionalParameters)是 M-5 DefaultParameterHandler 的绑定输入; M-2 执行链 query 首步 getBoundSql 即本域求值点 → [[M-5-mapping]] [[MP-3-lambda-wrapper]]
