# C-10 ClassPathIndex — 组件索引 (@Indexed → 加载合并 → 扫描分派)

> 依赖 C-9 ApplicationRunner | 🟡 Working | 6 KP | [模式: 缓存 + 索引查询 + 降级]

**读者处境**: 大项目启动慢, 一部分在"组件扫描"遍历整个类路径的 .class 找 @Component — 能不能提前知道"哪个类是组件"？这就是组件索引: 编译期生成清单, 运行期直接查。

### 1. 索引的诞生 — @Indexed 注解 + spring-indexer 生成 spring.components

场景: `@Service`/`@Component` 类在 classpath 哪都是 — 扫描器靠遍历 .class + ASM 读注解识别。若编译期就把"类→stereotype"关系写进清单文件, 运行期就不用扫描了。

源码路径:
- `Indexed.java:90`(spring-context/stereotype) — **@Indexed**: 标记注解 — `@Component` 等已带 @Indexed 元注解 — 标注后 spring-indexer(编译期注解处理器)生成索引
- 产物: **META-INF/spring.components** — 格式 `全限定类名=stereotype列表`(逗号分隔), 每个 jar 一份
- `CandidateComponentsIndexLoader.java:48` — **位置常量**: COMPONENTS_RESOURCE_LOCATION = "META-INF/spring.components"

关键设计: **Why 编译期生成而非运行期扫描？** 时间换空间: 编译期(一次性)把注解信息写成清单, 运行期读文件比扫类路径快一个数量级; 且 ASM 全扫描的类加载成本被省掉。这是"元数据索引"的通用思路(类似 APT 生成类)。[模式: 编译期索引]

数据流: 项目依赖含 spring-indexer → 编译时 @Service/@Component/@Repository 类被处理器收集 → 写 META-INF/spring.components: `com.x.UserService=org.springframework.stereotype.Component,...` → 打进 jar。

### 2. 索引加载 — CandidateComponentsIndexLoader 合并与缓存

场景: 多个依赖 jar 各带一个 spring.components — 运行期把这些清单合并成一个索引对象, 且每个类加载器只加载一次。

源码路径:
- `CandidateComponentsIndexLoader.java:42,84` — **loadIndex()**: L89 cache.computeIfAbsent(classLoader, doLoadIndex) — 按 ClassLoader 缓存, 避免重复解析
- `CandidateComponentsIndexLoader.java:93` — **doLoadIndex()**: L99 classLoader.getResources(META-INF/spring.components)(拿到多个 jar 的 URL) → 无→null → L105-108 每个 URL 用 PropertiesLoaderUtils.loadProperties 读成 Properties → result 列表 → L113 totalCount>0→new CandidateComponentsIndex(result)
- `CandidateComponentsIndex.java:49,81` — **查询**: getCandidateTypes(basePackage, stereotype) — 从合并索引 Map 查(包,类型)→候选类集合

关键设计: **Why getResources 而非 getResourceAsStream？** 一个 ClassLoader 下可能有多个 jar 各带索引 — getResources 返回全部匹配 URL, 逐个合并 — 保证跨模块(spring/自定义/第三方)的组件都被索引到。**Why 按 ClassLoader 缓存？** 不同应用模块可能用不同类加载器(Web 容器) — 索引依赖类加载器隔离。[模式: 多源合并 + 缓存]

数据流: 应用启动 → 扫描器构造 → CandidateComponentsIndexLoader.loadIndex(classLoader) → cache miss → doLoadIndex: getResources 找到 jar1 和 jar2 的 spring.components → 读成两个 Properties → 合并成 CandidateComponentsIndex(内含 类名→stereotype 集合 的 Map) → 缓存。

### 3. 扫描分派 — 有索引精准定位 vs 无索引全扫描

场景: findCandidateComponents(basePackage) 是扫描入口 — 有没有索引, 走两条完全不同的路径。

源码路径:
- `ClassPathScanningCandidateComponentProvider.java:301,345` — **构造与分派**: L301 componentsIndex=loadIndex(构造时); findCandidateComponents L345: `if (componentsIndex != null && indexSupportsIncludeFilters())` → L347 addCandidateComponentsFromIndex(index, basePackage) / 否则 L350 scanCandidateComponents(basePackage)
- `ClassPathScanningCandidateComponentProvider.java:408` — **索引路径**: 对每个 includeFilter 提取 stereotype(L414) → index.getCandidateTypes(basePackage, stereotype) → 每个类型 getMetadataReader(L421, 只读该类的元数据, 非全扫描)+isCandidateComponent → ScannedGenericBeanDefinition
- `ClassPathScanningCandidateComponentProvider.java:451` — **全扫描兜底**: resourcePatternResolver 扫描 classpath*: 全部 .class → ASM 读注解 → 慢

关键设计: **Why 默认仍能工作(无索引)？** 索引是可选项 — 没生成 spring.components 就回退全扫描, 功能完全一致只是慢; 大项目加 @Indexed/索引处理器换取启动加速。**Why indexSupportsIncludeFilters() 判断？** 只有默认 includeFilter(@Component 族, 能提取出 stereotype)才可用索引; 自定义 filter(如按注解自定义)无法映射到 stereotype, 必须全扫描。[模式: 双路径 + 降级]

数据流: @ComponentScan("com.x") → findCandidateComponents: componentsIndex 非空 且 默认 includeFilter → addCandidateComponentsFromIndex: 提取 stereotype=@Component → index.getCandidateTypes("com.x", "org.springframework.stereotype.Component") → 命中 [UserService,...] → 逐个 isCandidateComponent → BeanDefinition 列表(无需遍历类路径)。无索引 → scanCandidateComponents: 遍历 classpath*:com/x/**/*.class + ASM → 同样结果, 慢。

→ 引出 6-2: DataSource — 组件索引定了"Bean 类在哪", 数据库连接定"数据在哪" — 进入 JDBC 数据源与池化(DriverManagerDataSource/Hikari)。
