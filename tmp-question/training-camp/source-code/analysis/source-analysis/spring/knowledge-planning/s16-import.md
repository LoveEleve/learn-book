# S2-16 @Import/@EnableXxx — ImportSelector/DeferredImportSelector + @EnableAutoConfiguration 终结

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 4接口/~300行
> 基线: S2-2 @Configuration — @Import在doProcessConfigurationClass Step ④被处理

---

## §0.8

- 🟡 Working，1篇 — @Import三路分发: ImportSelector/DeferredImportSelector/ImportBeanDefinitionRegistrar → @EnableXxx模式总结 → Spring Boot @EnableAutoConfiguration
- 设计模式: [模式: 策略模式]—@Import的value是三路处理器; [模式: 模板方法]—@EnableXxx=@Import+Selector+Registrar 三步

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Import.java:56 | @Import | value=Class<?>[]—三种类型: ①ImportSelector(selectImports→类名数组) ②ImportBeanDefinitionRegistrar(registerBeanDefinitions) ③@Configuration类(递归processConfigurationClass) | High |
| ImportSelector.java:61 | selectImports() | `selectImports(AnnotationMetadata)`→返回类名字符串数组→每个类被递归processConfigurationClass→作为@Import的下一步 | High |
| DeferredImportSelector.java:38 | @Order+Group | 继承ImportSelector—`getImportGroup()`→返回Class<? extends Group>—Group批量处理多个selector→selectImports聚合过滤去重→最后统一process | High |
| ImportBeanDefinitionRegistrar.java:61 | registerBeanDefinitions | `registerBeanDefinitions(AnnotationMetadata, BeanDefinitionRegistry)`—在BeanDefinitionReader阶段调用—直接操作registry→注册任何BeanDefinition→最灵活但最底层 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**P1 核心 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @Import三路分发机制 — ImportSelector(selectImports→类名)/DeferredImportSelector(延迟聚合→排序分组→最后process)/ImportBeanDefinitionRegistrar(直接操作registry) | 🟡 | **为什么🟡**: 三种处理器覆盖不同的扩展需求——ImportSelector="根据注解元数据决定哪些类需要导入"(如@EnableAsync的AsyncConfigurationSelector)→DeferredImportSelector="延迟处理+聚合"(如@EnableAutoConfiguration)→Registrar="直接操作registry"(如@EnableAspectJAutoProxy注册AnnotationAwareAspectJAutoProxyCreator) |
| P1-2 | @EnableXxx模式 — @Import+Selector/Registrar 三步创建自己的Enable注解 | 🟡 | **为什么🟡**: @EnableXxx是Spring扩展的核心模式——@EnableAsync=@Import(AsyncConfigurationSelector)→selector返回ProxyAsyncConfiguration→@Configuration中的@Bean定义BeanPostProcessor→自定义注解在容器启动前注入自定义BPP/Bean |

**1篇理由**: ~300行/4接口 — 大部分 @Import 处理已在 S2-2 covered。此域聚焦"@EnableXxx 如何创建"的设计模式 + @Import三路分发的最后总结。

**单篇结构**: §1 @Import 三路回顾(Selector/Deferred/Registrar→已在S2-2详解层次→本域重点三路选择决策) → §2 @EnableXxx 模式——如何用@Import+Selector创建自己的Enable注解(@EnableAsync/@EnableScheduling/@EnableCaching 三个实例对比)
