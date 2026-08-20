# S2-2-2 @ComponentScan + @Import — 扫描引擎和导入的三路分发

> 依赖 S2-2-1 | 🔴 Deep | 4 KP | [模式: 策略模式]

**读者处境**: 篇1讲了 CCPP→Parser 主线——但现在卡在"doProcessConfigurationClass 8步里的第③④步(@ComponentScan 和 @Import)到底是什么？"——这两个是 Spring 自动配置的核心引擎。

### 1. @ComponentScan — 递归扫描的"种子→森林"

场景: `@ComponentScan("com.example")` 在 AppConfig 上 — Parser 扫描 `com.example` 包 → 找到 `UserService`, `OrderService` — 每个作为扫描到的 BeanDefinition 注册到 registry → 然后 **每个再作为配置候选递归 parse** — 扫描到的类可能自身带 @Bean 方法。

源码路径:
- `ConfigurationClassParser.java:325-358` — **doProcessConfigurationClass 的 Step ③**: `Set<AnnotationAttributes> componentScans` — 先收集直接声明(`AnnotationConfigUtils.attributesForRepeatable(..., isDirectlyPresent)`, L325-327)→仅当直接声明为空(`componentScans.isEmpty()`, L331)才收集元标注(`...isMetaPresent`, L332-334, 如 @SpringBootApplication 元标注 @ComponentScan)
- `ComponentScanAnnotationParser.java:parse(componentScan, className)` — 使用 `ClassPathBeanDefinitionScanner` 扫描包 → 结果为 `ScannedGenericBeanDefinition` 集合
- `ConfigurationClassParser.java:345-356` — 每个扫描到的 BeanDefinition → `parse(beanDef, beanName)` → 递归 processConfigurationClass(可能再触发自身的 @ComponentScan/@Bean)

关键设计: **Why 直接声明优先于元标注？** `@SpringBootApplication` 元标注了 `@ComponentScan` — 但用户可能自己在 AppConfig 上也加 `@ComponentScan(basePackages = "my.custom")` — **两条路径是互斥的先后关系而非竞争关系**: 源码 :331 只有 `componentScans.isEmpty()`(直接声明不存在)时才回退收集元标注 — 直接声明存在时元标注的 @ComponentScan 根本不收集。

数据流: @ComponentScan("com.example")→ComponentScanAnnotationParser→ClassPathBeanDefinitionScanner.scan("com.example")→读取 classpath 资源→用 ASM MetadataReader 读 `com/example/UserService.class`→创建 ScannedGenericBeanDefinition(带 metadata)→注册到 registry → parser.parse(userServiceDef, "userService")→processConfigurationClass→递归 doProcessConfigurationClass → 若 UserService 有 @Bean 方法→继续注册

### 2. @Import 三路分发 — 策略模式的极致演示

场景: `@Import({DataSourceConfig.class, MyImportSelector.class, MyRegistrar.class})` — 三种类型的导入需要完全不同的处理路径。DataSourceConfig 是普通 @Configuration 类(递归 parse) — MyImportSelector 返回类名字符串列表(递归处理每个类名) — MyRegistrar 是延迟注册器(调用 registerBeanDefinitions)。

源码路径:
- `ConfigurationClassParser.java:571-633` — **processImports()**: 遍历每个 import candidate → 判断类型 → ①ImportSelector(L585-601): 实例化(L588 `ParserStrategyUtils.instantiateClass()`)→ 若为 DeferredImportSelector(L594-596) → `deferredImportSelectorHandler.handle(configClass, selector)`(入队延迟) — 普通 ImportSelector → `selectImports(metadata)`(L598) → `asSourceClasses(importClassNames, filter)`(L599) → 递归 `processImports`(L600) ②ImportBeanDefinitionRegistrar(L603-611): 实例化 → `configClass.addImportBeanDefinitionRegistrar(registrar, metadata)` — 不立即注册(等到 BeanDefinitionReader 阶段) ③普通 @Configuration 类(L612-618): `processConfigurationClass(candidate.asConfigClass(configClass))`(L617) — 递归解析
- `ConfigurationClassParser.java:635-647` — **isChainedImportOnStack()**: 沿 import chain 检测 A→B→A 循环 → 有循环则抛 CircularImportProblem

关键设计: **Why ImportSelector → DeferredImportSelector 分离？** 普通 ImportSelector 的结果当即被 parse — 它的结果**影响下一轮候选收集**(parse 后立即搜新增 BeanDefinition)。DeferredImportSelector(如 Spring Boot 的 `@EnableAutoConfiguration` 下的 `AutoConfigurationImportSelector`)的 selectImports 依赖**所有其他配置类都已解析完**——只有全部解析后才能做条件匹配(`@ConditionalOnClass` 等)。这就是 DeferredImportSelectorHandler 的状态机设计: 解析阶段收集→process() 时排序+分组+统一处理。

数据流: @Import(MySelector.class, MyRegistrar.class, ConfigC.class) → L571 processImports入口 → checkForCircularImports(L578 isChainedImportOnStack) → L585 candidate.isAssignable(ImportSelector) → ParserStrategyUtils.instantiateClass(MySelector)(L588)→ L598 selectImports(AnnotationMetadata)返回["ConfigD","ConfigE"]→ L599 asSourceClasses(className, filter)→ L600 processImports递归 → L594 DeferredImportSelector检测→ handle入队 vs 直接processImports → L603 candidate.isAssignable(ImportBeanDefinitionRegistrar) → 实例化(L607-609)→ configClass.addImportBeanDefinitionRegistrar(registrar, importingMetadata)(L610) → L612 普通类 → L617 processConfigurationClass(candidate.asConfigClass(configClass))

### 3. DeferredImportSelectorHandler — 延迟收集+排序+分组+处理

场景: Spring Boot `@SpringBootApplication` → `@EnableAutoConfiguration` → `AutoConfigurationImportSelector`(DeferredImportSelector) — 它需要读取 `spring.factories` 中所有自动配置类，按 `@Conditional` 判断生效。这些判断依赖"所有其他配置类都已解析"。

源码路径:
- `ConfigurationClassParser.java:784-823` — **DeferredImportSelectorHandler**: ①`handle()(L797-807)`: 若 deferredImportSelectors == null(正在 process 中)→ 不直接调 processImports, 而是 `new DeferredImportSelectorGroupingHandler()` + `handler.register(holder)` + `handler.processGroupImports()`(L799-803)立即按 Group 处理(防止遗漏) — 否则入队(L805) ②`process()(L809-823)`: 排序(DEFERRED_IMPORT_COMPARATOR 按 @Order, L815)→分组(L816 `handler::register` 按 Group 类)→`processGroupImports()`(L817) 处理每个组
- `ConfigurationClassParser.java:827-873` — **DeferredImportSelectorGroupingHandler**: Group 概念 — 按 `Group` 类分组(L833-841: group != null ? group : selector 自身为 key)→`grouping.getImports()`(L915-921): 组内 selector 逐次调 `group.process(metadata, selector)`(L916-919)→最后 `group.selectImports()`(L920) 返回聚合结果

关键设计: **Group 设计的 Why？** AutoConfigurationImportSelector 和另一个自定义 DeferredImportSelector 可能需要**聚合处理**——所有 selector 的 selectImports 在一个 Group 内统一排序和去重。默认 `DefaultDeferredImportSelectorGroup` 简单拼接——Spring Boot 可以覆盖为自定义 Group 做去重+条件过滤。

数据流: doProcessConfigurationClass循环内→ 每次遇到DeferredImportSelector → handle(configClass, selector) → L799 deferredImportSelectors==null? 直接: new DeferredImportSelectorGroupingHandler + register(holder) + processGroupImports()(处理中) → 否则 L805 deferredImportSelectors.add(holder)入队 → 全部类解析完成后 L198 parse() 末尾调用 deferredImportSelectorHandler.process() → L815 ①DEFERRED_IMPORT_COMPARATOR按@Order排序 → L816 ②groupingHandler.register(holder)按Group类分组 → L817 ③processGroupImports → grouping.getImports()(L915) → group.process(metadata, selector)(L916-919)→ group.selectImports()(L920) → 逐结果processImports()

### 4. ImportStack — DFS 栈+导入注册表的双用途

场景: A @Import B, B @Import C, C 检测 A 是否在链条上 → ImportStack 同时做循环检测(栈)和导入关系查询(注册表)。

源码路径:
- `ConfigurationClassParser.java:737-781` — **ImportStack**: 继承 `ArrayDeque<ConfigurationClass>` + 实现 `ImportRegistry` — `registerImport(importingClass L741)`: 记录 "谁导入了谁" → `getImportingClassFor(importedClass L747)`: 查询最后一次导入 → `removeImportingClass(importingClass L752)`: 移除导入类 + 调 `removeKnownSuperclass`
- `ConfigurationClassParser.java:498-533` — **removeKnownSuperclass()**: 移除导入类对父类层级的引用 — `replace=true` 时取其他子类重新处理父类(防止父类 @Bean/@Import 丢失)

关键设计: **ImportRegistry 和 ImportStack 为什么要合在一起？** 自然生命周期一致 — import 链入栈时注册导入关系、出栈时无需额外 API。CCPP.processConfigBeanDefinitions() 在 do-while 结束后调用 `registry.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry())`(L457-459)——ImportRegistry 成为容器单例，后续 `ImportAwareBeanPostProcessor` 用它查找谁导入了谁。

数据流: processImports处理@Import → importStack.push(configClass)(入栈) → L741 registerImport(importingClassName, importedClassMetadata)(同时记录导入关系) → 递归 processConfigurationClass → 若遇到@Import的子类再次进入processImports → L578 isChainedImportOnStack检查循环: L635-647沿栈反向查找 importingClass → 有循环→ CircularImportProblem → 无循环→ 继续递归 → 出栈 importStack.pop() → do-while结束后 L457-459 registerSingleton(getImportRegistry())→ ImportRegistry成为容器单例 → ImportAwareBeanPostProcessor.postProcessBeforeInitialization(L572-581)读取registry查找导入元数据

→ 引出篇3: 解析结果如何变成 BeanDefinition + CGLIB 如何保证 @Bean 单例语义。
