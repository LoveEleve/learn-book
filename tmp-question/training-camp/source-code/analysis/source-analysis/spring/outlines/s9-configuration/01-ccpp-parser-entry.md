# S2-2-1 CCPP + Parser — 从 refresh() Step 5 到 doProcessConfigurationClass

> 依赖 S2-1 | 🔴 Deep | 4 KP | [模式: Template Method]

**读者处境**: S2-1 学了 refresh() 12 步 — "Step 5 调用 BFPP 解析 @Configuration"——但 ConfigurationClassPostProcessor 具体干了什么？Parser 怎么把注解读成 ConfigurationClass 模型？这篇回答"入口+主线"。

### 1. CCPP 入口 — refresh() Step 5 中拉起的全链路

场景: `new AnnotationConfigApplicationContext(AppConfig.class)` — refresh() Step 5 `invokeBeanFactoryPostProcessors()` → 遍历所有优先的 BeanDefinitionRegistryPostProcessor → 找到 ConfigurationClassPostProcessor(PriorityOrdered L130) → 调用 `postProcessBeanDefinitionRegistry()`。

源码路径:
- `ConfigurationClassPostProcessor.java:278-291` — **postProcessBeanDefinitionRegistry()**: identityHashCode 防重入检查 → 将 registry hashCode 登记到 `registriesPostProcessed` → 委托 `processConfigBeanDefinitions(registry)`
- `ConfigurationClassPostProcessor.java:363-469` — **processConfigBeanDefinitions()**: 候选收集(L364-377) → @Order 排序(L385-389) → **do-while 循环**(L414-454: parse→validate→loadBeanDefinitions→搜新增候选→循环直到无新增)
- `ConfigurationClassPostProcessor.java:189-192` — **getOrder() 返回 LOWEST_PRECEDENCE**: 虽然实现 PriorityOrdered 但优先级最低 — 确保在所有其他 PriorityOrdered BFPP 之后执行(但仍在普通 Ordered BFPP 之前) — "CCPP 等大家都准备好了再出手"

关键设计: **Why identityHashCode 防重入？** 一个容器可能被多个地方调用 postProcessBeanDefinitionRegistry — 如果重复处理，配置类会被解析两次，@Bean 方法注册的 BeanDefinition 重复。identityHashCode 比 equals 检测更准确(不同 registry 实例即使 equals 相同也必须各自处理)。

数据流: refresh() Step 5→`invokeBeanFactoryPostProcessors()`→CCPP.postProcessBeanDefinitionRegistry()→processConfigBeanDefinitions()→①遍历 beanDefinitionMap 逐个 `checkConfigurationClassCandidate`(ConfigurationClassUtils)→收集候选 Set ②`@Order` 排序→③**do-while 循环**: `parser.parse(candidates)`→`parser.validate()`→`reader.loadBeanDefinitions(configClasses)`→检查 registry 是否有新 BeanDefinition→若未解析则加入下一轮 candidates→循环直到无新增→④注册 ImportRegistry 为单例 bean(`IMPORT_REGISTRY_BEAN_NAME` L457-459)→⑤清理 MetadataReader 缓存

### 2. Parser 入口三路分发 — 三类配置类元数据路径

场景: CCPP 调用 `parser.parse(candidates)`(L419) — 候选集合中的 BeanDefinition 来源不同(注解扫描 / XML 导入 / 编程式注册) — Parser 需要分别处理三种元数据路径。

源码路径:
- `ConfigurationClassParser.java:166-199` — **parse(Set\<BeanDefinitionHolder\>)**: 三类分发: ①`AnnotatedBeanDefinition`→直接取 metadata(L171) ②`AbstractBeanDefinition.hasBeanClass()`→从 Class 反射获取 metadata(L174) ③最终 fallback→从 className 通过 MetadataReader(ASM)获取 metadata(L176-178)
- `ConfigurationClassParser.java:182-187` — **lite 降级**: 标记为 FULL(注解含 @Configuration)但无实例级 @Bean 方法 → `ConfigurationClassUtils.CONFIGURATION_CLASS_LITE`
- `ConfigurationClassParser.java:198` — **延迟处理**: `deferredImportSelectorHandler.process()` — 所有解析完成后统一处理 DeferredImportSelector

关键设计: **Why 三路分发而非统一？** `AnnotatedBeanDefinition`(ComponentScan 产物)已经带 ASM 解析好的 AnnotationMetadata — 无需重复读取 class 文件。`AbstractBeanDefinition.hasBeanClass()`(已加载 Class 的外部注册 Bean)直接反射读取 — 跳过 ASM。只有前两条路都不通时才兜底 MetadataReader — 最少 IO 的渐进式策略。

数据流: L171 candidate instanceof AnnotatedBeanDefinition → metadata = ((AnnotatedBeanDefinition) bd).getMetadata()(直接复用ASM结果) → L174 candidate instanceof AbstractBeanDefinition && hasBeanClass() → metadata = AnnotationMetadata.introspect(beanClass)(反射路径) → L176 fallback: metadata = reader.getMetadataReader(className).getAnnotationMetadata()(ASM路径) → L182-187 FULL但无实例@Bean → 降级为LITE → L198 deferredImportSelectorHandler.process()(延迟处理统一执行)

### 3. processConfigurationClass — 递归入口和"三胜"去重策略

场景: Parser.parse() 对每个候选调用 `processConfigurationClass(configClass, filter)` — 解析整个类层级链(含父类和接口)。但如果同一个类被多个来源注册(imported + scanned + explicit)，选谁？

源码路径:
- `ConfigurationClassParser.java:246-291` — **processConfigurationClass()**: ①@Conditional 先导检查(L247-249): `ConfigurationPhase.PARSE_CONFIGURATION` 条件不满足→直接 return ②**三胜去重**(L252-273): 优先级 **explicit > imported > scanned** — 新 imported 遇到已有非 imported 类→忽略新类(L253-258, "existing non-imported class overrides it"); 新 scanned 遇到已有 explicit import→移除其 beanDefinition 后返回(L260-266, "An implicitly scanned bean definition should not override an explicit import"); 新 explicit→移除旧的并替换(L268-273) ③do-while 递归(L280-283): 每轮 `doProcessConfigurationClass` 返回父类→继续处理直到无父类
- `ConfigurationClassParser.java:280-283` — **do-while 递归父类**: `do { sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter); } while (sourceClass != null)` — ConfigurationClassParser 自动跟踪 `knownSuperclasses` 去重

关键设计: **"三胜"去重策略的 Why？** explicit(AppConfig 显式注册)优先级最高 — 用户意图最明确。imported(@Import 引入)次之 — 显式 import 压过隐式扫描(源码 :265 注释明言 "An implicitly scanned bean definition should not override an explicit import" — 扫描到的新类遇到已有 import 时直接被忽略, 其 BeanDefinition 也被移除)。scanned(@ComponentScan 发现)最低 — 自动化产物最弱。这是处理"同一个类作为配置候选出现在多个入口"的优先级策略。

数据流: CCPP.processConfigBeanDefinitions()→candidates={AppConfig, AppConfig$DBConfig(内部扫描发现)}→parser.parse()→遍历 candidates→AppConfig: processConfigurationClass → @Conditional: skip? no → 存入 configurationClasses → doProcessConfigurationClass → 返回父类(若有)→AppConfig$DBConfig: processConfigurationClass → 同样是 import entry → 递归处理

### 4. doProcessConfigurationClass 8步注解流水线概览

场景: processConfigurationClass 内部 do-while 循环的每一步 — doProcessConfigurationClass 按固定顺序处理 8 类注解/结构 — 顺序有严格必要性(如 @ComponentScan 必须在 @Bean 之前 — 扫描到的类也可能有 @Bean 方法)。

源码路径:
- `ConfigurationClassParser.java:302-402` — **doProcessConfigurationClass()**: ①L306-308 成员(嵌套)类 ②L312-322 @PropertySource ③L325-358 @ComponentScan ④L360-361 @Import ⑤L364-373 @ImportResource ⑥L376-382 @Bean 方法 ⑦L385 接口默认方法 ⑧L388-398 检查父类(非 java.**) → 返回父类 SourceClass
- `ConfigurationClassParser.java:376-382` — **@Bean 提取**: `retrieveBeanMethodMetadata(sourceClass)` → 过滤 `kotlin.jvm.JvmStatic` → `configClass.addBeanMethod()`
- `ConfigurationClassParser.java:456-492` — **retrieveBeanMethodMetadata()**: ASM 声明顺序修正 — JVM 反射方法顺序不稳定(L461-462) → 当 >1 @Bean 且反射路径时 ASM 重读 class 文件 → 按声明顺序返回

关键设计: **8步顺序的 Why？** ①成员类在前 — 嵌套 @Configuration 类先被递归处理(封闭类的 scope 决定嵌套类的处理) ②@ComponentScan 在 @Import 前 — 扫描的类可能在 @Import 中被再次引用(去重可以合并) ③@Import 在 @Bean 前 — ImportSelector 可能返回@Bean 方法无法覆盖的类(ImportSelector 有更高的灵活性) ④@Bean 在接口方法前 — 类自己的 @Bean 优先于接口默认方法。

数据流: sourceClass={AppConfig} → Step ① processMemberClasses: 遍历嵌套类(如AppConfig$DBConfig)→ 每个递归processConfigurationClass → Step ② processPropertySource: 注册到Environment → Step ③ processComponentScans: getAnnotationAttributes(ComponentScan)→ componentScanParser.parse("com.example") → 扫描结果递归parse→ Step ④ processImports: getImports(sourceClass)→ collectImports递归元标注→ processImports三路分发 → Step ⑤ processImportResources: XML路径替换占位符 → Step ⑥ @Bean: retrieveBeanMethodMetadata→ ASM声明顺序修正→ addBeanMethod → Step ⑦ processInterfaces: 递归接口default方法 → Step ⑧ 返回superclass SourceClass供外层do-while继续

→ 引出篇2: @ComponentScan 的递归扫描 + @Import 的三路分发。
