# S2-6 BFPP 全景 — BeanDefinition 修改与 ${} 占位符解析

> 依赖 S2-2 @Configuration + S2-1 refresh() | 🟡 Working | 3 KP | [模式: 模板方法 + 策略模式]

**读者处境**: S2-2 学了 ConfigurationClassPostProcessor(一个具体的 BFPP) — S2-1 学了 refresh() Step 5 invokeBeanFactoryPostProcessors — 但 BFPP 这个接口还有哪些重要实现？${server.port} 这样的占位符是怎么在 BeanDefinition 加载后被替换的？

### 1. BeanFactoryPostProcessor 接口 + PriorityOrdered/Ordered 三阶段排序

场景: refresh() Step 5 `invokeBeanFactoryPostProcessors(beanFactory)` → PostProcessorRegistrationDelegate 找出所有 BFPP → 按 PriorityOrdered→Ordered→no-priority 三阶段分批调用 → ConfigurationClassPostProcessor(PriorityOrdered)先执行解析 @Configuration → 然后 PropertySourcesPlaceholderConfigurer(未实现 Ordered/PriorityOrdered, 在无优先级批次)替换 ${} 占位符 — 三阶段顺序错了会导致 `@Configuration("${app.config}")` 中的占位符还没被处理。

源码路径:
- `BeanFactoryPostProcessor.java:80` — **postProcessBeanFactory(ConfigurableListableBeanFactory)**: 单方法接口 — 在 BeanDefinition 加载完成后、Bean 实例化前调用 — 参数是 ConfigurableListableBeanFactory(可获取所有 BeanDefinition 并修改)
- `PostProcessorRegistrationDelegate.java:68-208` — **invokeBeanFactoryPostProcessors()**: ①**BDRPP 三阶段**(L107-149): PriorityOrdered(L107-119)→Ordered(L121-132)→while 循环不断收集未处理的直到没有新的(L134-149) — 每轮 invoked 的可能注册新 BDRPP ②**BFPP 名单分类**(L162-184): 按 PriorityOrdered/Ordered/无优先级三类收集 → **BFPP 三阶段调用**(L187-204): PriorityOrdered(L187-189)→Ordered(L191-197)→no-priority(L199-204)
- `PostProcessorRegistrationDelegate.java:107-119` — **PriorityOrdered 阶段**: `beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor)`(L108-109) → 过滤 `isTypeMatch(ppName, PriorityOrdered.class)`(L111) → `processedBeans.add(ppName)` 标记已处理(L113) → `sortPostProcessors(currentRegistryProcessors)`(L116) → `invokeBeanDefinitionRegistryPostProcessors`(L118)

关键设计: **Why BDRPP 在 while 循环内而 BFPP 在循环外？** BDRPP 可以 `registry.registerBeanDefinition()` 注册新的 BeanDefinition — 这些新的 BD 可能是另一个 BDRPP — 必须循环直到没有新的 BDRPP 出现。而 BFPP 只修改已存在的 BD — 不会创建新的 BFPP — 单次调用足够。**这个 while 循环是 ConfigurationClassPostProcessor 能处理 @Import 注册的新配置类的关键 — 如果 while 循环不存在，@Import 的类不会被递归解析。**

数据流: refresh().Step 5→invokeBeanFactoryPostProcessors(beanFactory)→PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors(L68)→①BDRPP PriorityOrdered阶段(L107-119): beanFactory.getBeanNamesForType(BDRPP)→过滤PriorityOrdered→找到ConfigurationClassPostProcessor→sortPostProcessors→invoke→CCPP解析所有@Configuration→②BDRPP Ordered阶段: 过滤Ordered→排序→invoke→③BDRPP while循环: 收集所有未处理的→invoke→若有新BDRPP注册→下一轮→直到无新增→④BFPP PriorityOrdered阶段(L187-189): 过滤PriorityOrdered→invoke→⑤BFPP Ordered阶段(L191-197): 过滤→排序→invoke→⑥BFPP no-priority(L199-204): 其余→invoke→PropertySourcesPlaceholderConfigurer(未实现Ordered)在此批次执行→完成

### 2. PropertySourcesPlaceholderConfigurer — Environment 驱动的 ${} 占位符替换

场景: `application.properties` 中有 `server.port=8080` → Spring 的 `@Value("${server.port}")` 和 BeanDefinition 中的 `${server.port}` 在哪个阶段被替换？答案是 PropertySourcesPlaceholderConfigurer.postProcessBeanFactory() — 在 Step 5 的 no-priority BFPP 批次执行 — 遍历所有 BeanDefinition → 找到其中包含 `${...}` 的属性值 → 通过 Environment.getProperty() 查找真实值 → 替换。

源码路径:
- `PropertySourcesPlaceholderConfigurer.java:68` — **class implements EnvironmentAware**: 通过 setEnvironment() 注入 Environment → Environment 包含 propertySources(application.properties + System Properties + OS env)
- `PropertySourcesPlaceholderConfigurer.java:132-158` — **postProcessBeanFactory()**: ①`if (this.propertySources == null)`(L133) → 构建 MutablePropertySources: `addLast(environmentPropertySource)`(L140)→`addFirst/addLast(localPropertySource)`(L146/L149) — appliedPropertySources 到 L158 才赋值 ②`processProperties(beanFactory, createPropertyResolver(this.propertySources))`(L157, 参数是 this.propertySources, 不是 appliedPropertySources) → 遍历所有 BeanDefinition 替换 ${}
- `PropertySourcesPlaceholderConfigurer.java:175-193` — **processProperties()**: 设 placeholderPrefix/Suffix/valueSeparator/escapeCharacter(L178-181)→StringValueResolver 包装 resolvePlaceholders/resolveRequiredPlaceholders(L183-190)→`doProcessProperties(beanFactory, valueResolver)`(L193)→遍历 beanDefinitionNames → 每个 BeanDefinition → 替换 placeholder 值

关键设计: **Why 两代演进: PropertyPlaceholderConfigurer(Spring 2.0) → PropertySourcesPlaceholderConfigurer(Spring 3.1)？** 旧版读取单个 Properties 文件 — 新版通过 Environment 聚合多个 PropertySources(application.properties + system + env + command-line) — 这使 Spring Boot 的 `spring.profiles.active` 按 profile 覆盖配置成为可能。PropertyPlaceholderConfigurer 是"配置来自一个文件"的旧世界 — PropertySourcesPlaceholderConfigurer 是"配置来自多个来源叠加"的新世界。

数据流: Spring Boot 自动注册 PropertySourcesPlaceholderConfigurer BFPP → refresh() Step 5→invoke 到它(no-priority 批次) → L132 postProcessBeanFactory → L133 this.propertySources==null → L140 addLast(environmentPropertySource) 包含 ①application.properties(server.port=8080) ②systemProperties(java.version=17) ③OS env(PATH=...) → L149 addLast(localPropertySource) → L157 createPropertyResolver(this.propertySources) → new PropertySourcesPropertyResolver(propertySources) → processProperties → L178-181 设 prefix/suffix/separator/escape → L183-190 valueResolver 包装 → L193 doProcessProperties → 遍历beanDefinitionNames: "myBean" → BeanDefinition中的属性值→包含"${server.port}" → valueResolver.resolvePlaceholders("${server.port}") → PropertyPlaceholderHelper.replacePlaceholders → Environment.getProperty("server.port")→返回"8080"→替换BD中的属性值为"8080"→下一个BD

### 3. PropertyPlaceholderHelper — 递归嵌套 + 默认值的解析引擎

场景: 占位符可以嵌套: `${app.${profile}.url}` → 先解析内层 `${profile}` → "prod" → 再解析 `${app.prod.url}` → 获取最终值。还可以设置默认值: `${server.port:8080}` → Environment 中没有 → 返回默认值 "8080"。

源码路径:
- `PropertyPlaceholderHelper.java:37-39` — **类定义**: prefix="${" suffix="}" — 6.2+ 的实现委托给内部 PlaceholderParser: 构造器 `this.parser = new PlaceholderParser(placeholderPrefix, placeholderSuffix, valueSeparator, escapeCharacter, ignoreUnresolvablePlaceholders)`(L88-89) — **全代码库不存在 SimplePrefixTree 数据结构**
- `PropertyPlaceholderHelper.java:100-114` — **replacePlaceholders()**: 两重载: `(String, Properties)`→属性版本(L100-102) / `(String, PlaceholderResolver)`→策略版本(L112-114) — L114 delegate 给 `this.parser.replacePlaceholders`
- `PropertyPlaceholderHelper.java:117-119` — 旧的递归方法 **parseStringValue() 已在 L118 标记 @Deprecated(since 6.2.12)**, 仅保留兼容 — 当前解析算法见 PlaceholderParser(L122-200): ①parse 拆成 Part 列表(TextPart/SimplePlaceholderPart/NestedPlaceholderPart), 嵌套占位符递归展开(L134-170) ②ParsedValue.resolve 统一解析(L127-131) — 解析尽量延迟(lazily), 只有真正需要时才调用 placeholderResolver

关键设计: **Why 两遍 parse→resolve 而非单遍递归替换？** 正则无法处理嵌套 ${a${b}} — 6.2 前旧实现是"找到最内层 ${...} → 解析 → 替换后继续找下一个"的循环; 6.2+ 的 PlaceholderParser 改为两遍式: 第一遍 parse 把整段文本拆成 Part 树(嵌套占位符是 NestedPlaceholderPart, L154-166 递归展开) — 第二遍 ParsedValue.resolve 统一解析(L127-131), 尽量延迟解析(lazily as possible, L56)。三层嵌套(${a${b${c}}}) → 拆出嵌套 Part 树 → resolve 从内向外求值 — 占位符可以被无限组合。

数据流: resolvePlaceholders("${app.${profile}.url}") → L112 replacePlaceholders(value, resolver) → L114 parser.replacePlaceholders → L122-123 parse(value,false) → 发现"${profile}"嵌套→parse(placeholder,true) 递归(L157)→创建 NestedPlaceholderPart(L178-200) → L131 parsedValue.resolve → resolve 时先解析内层 "${profile}" → PlaceholderResolver→Environment.getProperty("profile")→"prod" → 再解析"${app.prod.url}" → Environment→return "https://api.prod.com" → 最终"https://api.prod.com" → 下一个 scenario: "${server.port:8080}" → parseSection 拆出 key="server.port" + fallback="8080"(L214-237) → resolvePlaceholder("server.port")→Environment→null→用 fallback→返回"8080"

→ spring-context 第六域完成。BFPP三阶段调度 + PropertySourcesPlaceholderConfigurer + PropertyPlaceholderHelper — 三个层次。引出 S2-7: Bean 作用域 — singleton/prototype/request/session/@Scope 注解 — getBean 时的作用域判定 + Scoped Proxy。
