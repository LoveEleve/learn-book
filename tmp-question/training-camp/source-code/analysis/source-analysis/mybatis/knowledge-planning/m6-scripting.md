# M-6 动态 SQL — XMLScriptBuilder→SqlNode 树→运行时求值

> 项目: MyBatis | 🔴 Deep / 1 篇 | XMLScriptBuilder(251)+SqlNode 族(If/ForEach 236/Where/Trim/Set/Choose/Mixed/Text/StaticText/VarDecl)+DynamicSqlSource+DynamicContext+OgnlCache+XMLLanguageDriver
> 基线: M-PLAN M-6 — 前置: **M-1 (LanguageDriverRegistry/Configuration)** — 展开 XML→SqlNode 树→动态/静态分流→运行时求值

---

## §0.8

- 🔴 Deep，1篇 — 入口(**XMLLanguageDriver.createSqlSource L30-48: XNode→XMLScriptBuilder.parseScriptNode / String→`<script>` 注解分支 L32-36+PropertyParser ${} 变量 L39**) → 构建期(**XMLScriptBuilder: 9 标签 Handler 映射表 L53-63[trim/where/set/foreach/if/choose/when/otherwise/bind]; parseDynamicTags 递归 L76-101[文本节点→TextSqlNode.isDynamic(${} 检查) 分流/元素节点→Handler, 未知抛 "Unknown element" L93-95; isDynamic 成员变量传播 L86,97]; parseScriptNode 分流 L65-74[isDynamic→DynamicSqlSource / 否则 RawSqlSource 构建期解析]**) → 节点族(**SqlNode 接口 apply(DynamicContext) L23; IfSqlNode: ExpressionEvaluator.evaluateBoolean(test) OGNL 求值, apply 返回内容是否应用; ForEachSqlNode: evaluateIterable→open/每元素[PrefixedContext(separator)+applyIndex/Item[itemizeItem="__frch_"+item+"_"+i L28,137]+FilteredDynamicContext #{item}→#{__frch_item_i} 参数唯一化]→close→remove bindings; TrimSqlNode: FilteredDynamicContext 缓冲→applyAll[trim→toUpperCase→applyPrefix[prefixOverrides 匹配移除]→applySuffix]→delegate.appendSql; WhereSqlNode=Trim("WHERE",[AND/OR 前缀列表]); ChooseSqlNode: when 列表+otherwise[>1 抛 "Too many default" L244-245]**) → 求值上下文(**DynamicContext: ContextMap[HashMap+MetaObject 回退四层 L78-94: containsKey→null→fallbackParameterObject 原对象→getValue; issue#61 读不改]; bindings[_parameter/_databaseId L53-54]; StringJoiner(" ") sqlBuilder; uniqueNumber**) → ${} vs #{}(**TextSqlNode: GenericTokenParser("${","}") isDynamic 检查; apply: BindingTokenParser[OgnlCache.getValue→null→""(issue#274)→checkInjection(injectionFilter 正则防注入 L69-71)]; SqlSourceBuilder.parse: #{} →ParameterMappingTokenHandler→"?"+ParameterMapping 列表→StaticSqlSource; shrinkWhitespacesInSql 去空白 L48-52**) → 运行时(**DynamicSqlSource.getBoundSql: new DynamicContext→rootSqlNode.apply→SqlSourceBuilder.parse(context.getSql())→BoundSql+bindings.forEach(boundSql::setAdditionalParameter)[foreach 附加参数]; RawSqlSource: 构建期 getSql+parse 一次性, getBoundSql 直转 StaticSqlSource[每次 new BoundSql 但无重复解析]; StaticSqlSource.getBoundSql L29-32**)
- 设计模式: [模式: 解释器+组合+模板方法(Handler 映射)+策略(动态/静态分流)]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| XMLLanguageDriver.java:30-48 | 入口 | **createSqlSource 双入口**: XNode(XMLScriptBuilder)/String(`<script>` 注解 L32-36 或 PropertyParser ${} 变量后 TextSqlNode.isDynamic 分流 L39-47) | High |
| XMLScriptBuilder.java:53-63 | Handler 映射 | **9 标签节点处理器映射表**: trim/where/set/foreach/if/choose/when(→IfHandler)/otherwise/bind — 策略注册表 | High |
| XMLScriptBuilder.java:76-101 | 递归解析 | parseDynamicTags: 文本节点按 TextSqlNode.isDynamic 分 TextSqlNode/StaticTextSqlNode(L81-89); 元素节点查映射表, 未知抛 "Unknown element"(L93-95); **isDynamic 成员变量传播**(L86,97) | High |
| XMLScriptBuilder.java:65-74 | 分流 | parseScriptNode: isDynamic→DynamicSqlSource(每次执行求值)/否则 RawSqlSource(构建期解析) | High |
| DynamicContext.java:78-94 | ContextMap | **四层取值回退**: ①containsKey 直返 ②metaObject==null→null ③fallbackParameterObject&&!hasGetter→原对象 ④getValue — issue#61 读不改 | High |
| DynamicContext.java:53-54 | bindings | _parameter/_databaseId 初始绑定; bind() 追加; getBindings 供 OGNL 求值 | High |
| ForEachSqlNode.java:69-137 | foreach | evaluateIterable→null/空返回→applyOpen→每元素 PrefixedContext(separator)+applyIndex/Item+**itemizeItem=ITEM_PREFIX+item+"_"+i(L28,137, __frch_ 前缀)**+FilteredDynamicContext #{} 参数唯一化→applyClose→remove bindings | High |
| TrimSqlNode.java:56-60,89-99 | trim | FilteredDynamicContext 缓冲→applyAll: trim→toUpperCase→applyPrefix(prefixOverrides 匹配移除→加 prefix)→applySuffix→delegate.appendSql; parseOverrides "|" 分隔转大写 L62-73 | High |
| WhereSqlNode.java:25-32 | where | **Where=Trim("WHERE",[AND/OR 前缀列表])** — 特化实现 | High |
| TextSqlNode.java:63-83 | ${} | BindingTokenParser: OgnlCache.getValue→**null→""(issue#274 非 "null")**→checkInjection(injectionFilter 正则防注入 L69-71) | High |
| SqlSourceBuilder.java:42-52 | #{} | parse: GenericTokenParser("#{","}")→ParameterMappingTokenHandler 收集 mapping→"?" 替换→StaticSqlSource; shrinkWhitespacesInSql 时 removeExtraWhitespaces | High |
| DynamicSqlSource.java:30-43 | 运行时 | getBoundSql: new DynamicContext→rootSqlNode.apply→SqlSourceBuilder.parse(context.getSql())→getBoundSql→**bindings.forEach(setAdditionalParameter)**(foreach item 附加参数) | High |
| RawSqlSource.java:28-49 | 静态 | 构建期 getSql(apply)+parse 一次性 → StaticSqlSource; getBoundSql 直转(每次 new BoundSql 无重复解析) | High |
| StaticSqlSource.java:29-32 | 终端 | getBoundSql: new BoundSql(configuration, sql, parameterMappings, parameterObject) | High |
| OgnlCache.java:44-58 | OGNL | 表达式解析缓存(ConcurrentMap)+OgnlClassResolver/OgnlMemberAccess 定制安全 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 动态 SQL 是单管线 — 1篇 (~69行) 按"入口双路径→Handler 映射+递归解析→分流→节点族(if/foreach/trim 家族/choose)→求值上下文→${} vs #{}→运行时"展开; 执行链路消费点衔接 M-2(导航), DefaultParameterHandler 衔接 M-5(导航)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 9 标签 Handler 映射 + parseDynamicTags 递归 + isDynamic 传播 | 🔴 | **为什么🔴**: 解析骨架 |
| P1-2 | 动态/静态分流 (DynamicSqlSource vs RawSqlSource) | 🔴 | **为什么🔴**: 性能设计核心 |
| P1-3 | ForEach __frch_ 参数唯一化 + FilteredDynamicContext | 🔴 | **为什么🔴**: 嵌套参数正确性 |
| P1-4 | Trim 家族 (trim/where/set 前缀处理) | 🔴 | **为什么🔴**: 条件 SQL 拼装 |
| P1-5 | ContextMap 四层回退 + ${}/#{} 双 Token 解析 | 🔴 | **为什么🔴**: 取值与占位符语义 |
| P2-1 | DynamicSqlSource 运行时 getBoundSql + 附加参数 | 🟡 | **为什么🟡**: 执行时求值 |
| P2-2 | OgnlCache 表达式缓存 + injectionFilter | 🟡 | **为什么🟡**: 性能与安全 |
| P3-1 | 与 M-2 执行链的消费关系 (getBoundSql) | 🟢 | **为什么🟢**: 内核边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **解析骨架与分流** | 🔴 | 定义特征 |
| B | **节点族实现** | 🔴 | 标签语义 |
| C | **取值与 Token** | 🔴 | 参数机制 |
| D | **运行时与边界** | 🟡 | 执行衔接 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 分流时机 | isDynamic 判定发生在**构建期**: 任一 ${} 文本(TextSqlNode.isDynamic)或任一元素标签置 true → DynamicSqlSource(每次执行求值); 纯静态 → RawSqlSource(构建期一次解析) — 性能差异: 静态 SQL 的 #{} 解析只做一次 | XMLScriptBuilder.java:65-74,76-101; XMLLanguageDriver.java:39-47 |
| q2 | Handler 注册表 | 9 标签→Handler 映射(trim/where/set/foreach/if/choose/when→IfHandler 复用/otherwise/bind), 未知元素构建期抛 "Unknown element" — 策略注册表, 新增标签=新增映射 | XMLScriptBuilder.java:53-63,93-95 |
| q3 | __frch_ 唯一化 | foreach 每元素把 item/index 的 #{} 引用替换为 `#{__frch_item_i}`(ITEM_PREFIX L28), 且 bind item_i 值 — 嵌套 foreach 参数不冲突; FilteredDynamicContext 用正则 replaceFirst 只替换 item 开头的属性引用 | ForEachSqlNode.java:28,69-137; FilteredDynamicContext |
| q4 | Trim 家族特化 | Where=Trim("WHERE",[AND/OR…]); Set=Trim("SET",[,],suffixOverrides=[,]); applyAll: trim→toUpperCase→按 prefixOverrides 匹配移除旧前缀→加新 prefix — 条件标签拼装的核心技巧 | WhereSqlNode.java:25-32; TrimSqlNode.java:56-60,89-99 |
| q5 | ContextMap 四层回退 | bindings 取值: containsKey→metaObject null→fallbackParameterObject 返回原对象(单参数对象直接取值)→metaObject.getValue — 兼顾 Map 参数/POJO 参数/裸值; issue#61 读不改上下文 | DynamicContext.java:78-94 |
| q6 | ${} vs #{} | ${}=OGNL 求值直接拼接(注入风险, injectionFilter 正则校验, null→"" 非 "null" issue#274); #{}=SqlSourceBuilder 解析为 ParameterMapping+"?" 占位(预编译安全) — 两种占位符语义分离 | TextSqlNode.java:63-83; SqlSourceBuilder.java:42-52 |
| q7 | 运行时附加参数 | DynamicSqlSource.getBoundSql: apply 后 SqlSourceBuilder.parse(此时 foreach 的 __frch_ 参数已写入 context)→BoundSql→bindings.forEach(boundSql::setAdditionalParameter) — foreach 产生的参数以附加参数进 BoundSql, 供 M-5 DefaultParameterHandler 绑定 | DynamicSqlSource.java:30-43 |
| q8 | RawSqlSource 构建期 | RawSqlSource 构造: getSql(apply 一次, null 参数)→SqlSourceBuilder.parse 一次性→StaticSqlSource; getBoundSql 直转(每次 new BoundSql 但零重复解析) | RawSqlSource.java:28-49; StaticSqlSource.java:29-32 |
| q9 | OGNL 缓存 | OgnlCache 表达式→OgnlNode 编译结果 ConcurrentMap 缓存(parseExpression L57); OgnlClassResolver 防类加载攻击 | OgnlCache.java:44-58 |

→ 引出 M-5: 动态 SQL 求值产物 BoundSql(含 parameterMappings+additionalParameters)是 DefaultParameterHandler 的输入 — 参数绑定在 M-5 展开; M-2 执行链 getBoundSql 调用点(导航)。
