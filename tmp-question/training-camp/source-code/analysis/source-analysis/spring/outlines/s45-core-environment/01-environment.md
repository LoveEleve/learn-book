# C-3 Environment — 配置属性体系 (接口 → 来源容器 → 查找链 → 占位符)

> 依赖 C-2 类型转换 | 🟡 Working | 6 KP | [模式: 责任链 + 组合 + 模板方法]

**读者处境**: `@Value("${server.port:8080}")` — ${} 里的值从哪来？配置文件、启动参数 -D、环境变量、容器 JNDI — 多个来源同时有值用哪个？${} 解析失败会怎样？嵌套 ${a.${b}} 支持吗？

### 1. 接口层次 + PropertySource 抽象 + MutablePropertySources 容器

场景: 同一个 key "server.port" 可能同时存在于 application.yml、-Dserver.port、env SERVER_PORT — Spring 需要"多个来源按优先级排列"的模型: 来源是一个个 PropertySource, 有序组织在容器里。

源码路径:
- `PropertyResolver.java:30,47,67` — **接口**: getProperty(key) / getProperty(key, default) / getProperty(key, Class)(内部走 C-2 类型转换) + containsProperty(L36)
- `Environment.java:72,87` — **Environment 接口**: extends PropertyResolver — 加 getActiveProfiles — "属性+激活环境"统一入口
- `PropertySource.java:62` — **来源抽象**: getName() + getProperty(key)→value — 每种来源一种子类(PropertiesPropertySource/SystemEnvironmentPropertySource…)
- `MutablePropertySources.java:41,43,104` — **有序容器**: propertySourceList=CopyOnWriteArrayList(线程安全, 迭代即优先级顺序) — addFirst(L104 最高优先级)/addLast(最低) — 迭代顺序=查找顺序

关键设计: **Why 用"来源列表+优先级"而非单一 Map？** 配置可能来自 classpath 文件、外部文件、系统属性、环境变量 — 且顺序必须可动态调整(Boot 外部化配置 17 级优先级就是反复 addFirst/addLast 的产物); CopyOnWriteArrayList 让"运行期调整来源"与"并发读"共存。[模式: 组合 — 有序聚合]

数据流: `environment.getPropertySources()` → MutablePropertySources[addFirst(servletConfig), addFirst(servletContext), addFirst(jndi), addFirst(systemProperties), addLast(systemEnvironment), addLast(application.yml)] — 优先级从前到后递减, 查找从 index 0 开始。

### 2. PropertySourcesPropertyResolver 查找链 — 首个非 null 即返回

场景: getProperty("server.port") 调用后内部发生什么？为什么 -D 参数能覆盖 yml 里的值？找不到 key 会怎样？

源码路径:
- `PropertySourcesPropertyResolver.java:32,78` — **getProperty()**: L80 for 遍历 propertySources → propertySource.getProperty(key) 非 null → resolveNestedPlaceholders(嵌套 ${}) → convertValueIfNecessary(targetType, C-2) → 返回; 全遍历完无命中→返回 null
- `StandardEnvironment.java:55,96` — **默认两个来源**: customizePropertySources: L98 addLast(systemProperties) + L101 addLast(systemEnvironment) — 所以 Java 系统属性(-D)先于 OS 环境变量
- `AbstractEnvironment.java:55(类声明)` — **customizePropertySources 钩子**: 子类(Standard/ServletEnvironment)覆写加默认来源; 用户通过 ConfigurableEnvironment.getPropertySources() 调整

关键设计: **Why "首个非 null 即返回"？** 优先级语义: 前面的来源说了算 — 后来源只在前面全部没有该 key 时才生效。**Why 找不到返回 null 而非抛错？** 与占位符解析配合: 宽松模式(ignoreUnresolvable)下 ${missing} 原样保留, 由上层决定(PropertyPlaceholderConfigurer 可配 ignoreUnresolvablePlaceholders)。[模式: 责任链 — 首个命中]

数据流: @Value("${server.port}") → getProperty("server.port", String) → L80 遍历: systemProperties 无 → systemEnvironment 无 → application.yml 命中 "8080" → resolveNestedPlaceholders(无嵌套) → convertValueIfNecessary → "8080" → C-2 转 int。若 -Dserver.port=9090 → 第一个 source 即命中 → 9090, yml 被覆盖。

### 3. PlaceholderParser — ${} 占位符的完整解析语义

场景: `${server.port:8080}` 默认值、`${a.${b}}` 嵌套、`\${literal}` 转义 — 这些语法谁实现？怎么解析？

源码路径:
- `PlaceholderParser.java:62,122` — **解析器** (Spring 6.2 重构, 原 PropertyPlaceholderHelper 内部): replacePlaceholders: L123 parse(value) 把字符串切分为 Part 列表(文本/占位符, 支持 `\` 转义前缀) → ParsedValue.resolve(PartResolutionContext) → 占位符部分: resolver.resolvePlaceholder(名字) → 递归解析(L154: 嵌套 `${a.${b}}` → 外层先切分, 内层值再解析)
- `PropertyPlaceholderHelper.java:37(类声明)` — **外观**: 默认 ${} 前缀/后缀; DEFAULT_PLACEHOLDER_PREFIX="${" 语义 — resolvePlaceholders(宽松: 未解析保留原文) vs resolveRequiredPlaceholders(严格: 抛 IllegalArgumentException)
- 默认值语法: `${key:default}` — valueSeparator 为 ":" 时解析默认值

关键设计: **Why 用"Part 列表+递归"而非正则？** 占位符可嵌套、可转义、可带默认值 — 单遍正则无法表达递归; Part 化让"文本/占位符"边界清晰, 递归处理嵌套。**Why 宽松/严格双模式？** 同一语法两套语义: Boot 环境 ${missing} 保留下游报错(信息更准), 老 PropertyPlaceholderConfigurer 默认静默忽略。[模式: 递归下降解析]

数据流: "${server.port:8080}" → parse: [占位符Part("server.port:8080")] → resolve: 分隔符 ':' → key=server.port, default=8080 → resolver.resolvePlaceholder("server.port") → "9090"(-D) → 命中返回。${a.${b}} → 外层 Part("a.${b}") → resolvePlaceholder("a.${b}") → 递归 parse 内层 "${b}" → 先解 b=profile → "a.profile" → 再查。\${literal} → 转义 → 输出 "${literal}" 字面量。

→ 引出 0-4: Ordered — 配置来源的 addFirst 顺序还靠手工 — @Order/Ordered/PriorityOrdered 与 AnnotationAwareOrderComparator 如何统一排序 Bean/拦截器/转换器。
