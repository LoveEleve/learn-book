# S-1 @SpringBootApplication — 组合注解拆解 (配置 + 自动装配 + 扫描)

> 依赖 C-5/s9/s23/C-6 | 🟡 Working | 6 KP | [模式: 组合注解 + ImportSelector 桥]

**读者处境**: `@SpringBootApplication` 一个注解代替了 @Configuration+@EnableAutoConfiguration+@ComponentScan — 这三个各做什么?主类包怎么被记录?扫描怎么排除?参数怎么转发?

### 1. 组合注解拆解 — 三层职责

场景: 主类上写 `@SpringBootApplication` — 它本质是三个注解的组合, 每个管一件事: 配置(@SpringBootConfiguration)、自动装配(@EnableAutoConfiguration)、组件扫描(@ComponentScan)。

源码路径:
- `SpringBootApplication.java:50,58` — **元注解组合**: L50-53 @Target/@Retention/@Documented/@Inherited + L54 @SpringBootConfiguration + L55 @EnableAutoConfiguration + L56-57 @ComponentScan(excludeFilters) — 一键替代三个注解
- `SpringBootApplication.java:71,95` — **属性转发**: exclude(L71)/excludeName(L80) 转发给 @EnableAutoConfiguration; scanBasePackages(L95)/scanBasePackageClasses(L105) 转发给 @ComponentScan
- `SpringBootConfiguration.java:47,49` — **配置层**: @Configuration(L47) + @Indexed(L48) — 主配置类; @Indexed 让组件索引(C-10 的 spring.components)能发现它

关键设计: **Why 三个注解合成一个？** 约定优于配置: Boot 应用必然同时需要三者 — 组合注解让"配置类+自动装配开关+扫描"永远成套出现, 避免遗漏; 参数转发保持各层注解的灵活性(排除/扫描包可调)。组合机制复用 C-5 的元注解体系 — 无新机制。[模式: 组合注解]

数据流: `@SpringBootApplication` 主类 → 元注解解析(C-5 MergedAnnotation): @SpringBootConfiguration(→@Configuration 语义: 主配置类, 且 @Indexed 可被索引) + @EnableAutoConfiguration(→自动装配开关, 见 §2) + @ComponentScan(→扫描主包, 见 §3)。

### 2. @EnableAutoConfiguration — 自动装配的启动开关

场景: 这个注解做了什么, 让"引入 starter 就自动配好 Bean"?两个机制: ①@AutoConfigurationPackage 记录主类包 ②@Import(ImportSelector) 引入自动装配加载器(S-2)。

源码路径:
- `EnableAutoConfiguration.java:81,83` — **开关**: @AutoConfigurationPackage(L81) + @Import(AutoConfigurationImportSelector.class)(L82) — 后者是 **S-2 的入口**
- `AutoConfigurationPackage.java:41,42` — **包注册**: @Import(AutoConfigurationPackages.Registrar.class) — 记录主类所在包
- `AutoConfigurationPackages.java:93,128` — **注册/读取**: Registrar.register(L128)→register(L93): 以 BasePackages bean 存入容器; get(L73) 供自动装配扫描读取主包 — 自动装配只扫描主包相关
- `EnableAutoConfiguration.java`(exclude 属性) — 排除指定自动装配类

关键设计: **Why 记录主类包？** 自动装配需要知道"应用的根包" — 否则无法判断"组件扫描范围/哪些自动装配可选"(@ConditionalOnMissingBean 要排除应用自己的 Bean); AutoConfigurationPackages 以 bean 形式存储, 任何自动装配类可读取。[模式: 注册器 + 全局状态]

数据流: @EnableAutoConfiguration → ①AutoConfigurationPackages.Registrar 注册主包("com.example")为 BasePackages bean → ②AutoConfigurationImportSelector(S-2) 读取 spring-autoconfigure imports 文件 → 候选自动装配类 → (用主包判断缺失条件) → 返回导入列表。exclude(MyAutoConfig.class) → 从候选移除。

### 3. @ComponentScan 定制 — 排除过滤器

场景: 组件扫描默认扫主包 — 但自动装配类也在 classpath, 怎么避免被当普通组件重复注册?excludeFilters 解决。

源码路径:
- `SpringBootApplication.java:56` — **@ComponentScan(excludeFilters)**: 两个 TypeFilter — TypeExcludeFilter + AutoConfigurationExcludeFilter
- `AutoConfigurationExcludeFilter.java:36,48` — **match L48**: 判定候选是否为自动装配类(在 imports 列表中)— 是则排除(防自动装配类被组件扫描重复注册)
- `TypeExcludeFilter.java:50,62` — **match L62**: 委托注册的 delegates — 用户可扩展自定义排除器
- scanBasePackages(L95) — 覆盖默认扫描包(主类包)

关键设计: **Why 两个过滤器？** ①AutoConfigurationExcludeFilter: 自动装配类(@Configuration 但通过 imports 加载)不能同时被 @ComponentScan 当普通配置类扫到 — 双注册会冲突; ②TypeExcludeFilter: 通用扩展点(如 @SpringBootApplication 排除特定类型)。[模式: TypeFilter 排除]

数据流: @ComponentScan("com.example") → 扫描: TypeExcludeFilter.match(delegates)→false(无自定义) → AutoConfigurationExcludeFilter.match: 候选类是自动装配类(在 imports)→排除 → 普通 @Service/@Controller 正常注册。scanBasePackages 显式指定 → 覆盖默认主包。

→ 引出 S-2: 自动装配加载机制 — AutoConfigurationImportSelector: imports 文件读取 → 过滤(OnClassCondition 等) → 排序 → 返回导入 — Boot 的灵魂。
