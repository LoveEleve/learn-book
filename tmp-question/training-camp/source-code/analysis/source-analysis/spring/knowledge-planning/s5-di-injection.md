# S1-5 DI 注入三机制 — @Autowired/@Resource/@Qualifier

> 项目: Spring Framework 6.x | 🔴 Deep / 2 篇 | AutowiredAnnotationBPP(1113行)+CommonAnnotationBPP(955行)
> 基线: S1-1 BeanDefinition(元数据) → S1-3 populateBean(注入时机) → 本篇展开"populateBean 内部怎么决定注入什么"

---

## 01 提取

| Source | Method | Confidence |
|--------|--------|------------|
| AutowiredAnnotationBPP.java:506 | **postProcessProperties()** — @Autowired 字段/方法注入入口 | High |
| AutowiredAnnotationBPP.java:543 | **findAutowiringMetadata()** — scan @Autowired/@Value on fields+methods | High |
| QualifierAnnotationAutowireCandidateResolver.java:156 | **isAutowireCandidate()** — @Qualifier 过滤同类型多 Bean | High |

---

## 02-04

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @Autowired byType 完整链路 (BPP→metadata→resolveDependency) | 🔴 | **为什么🔴**: 最常用注解的内部机制 — 从注解扫描到字段赋值的全链 |
| P1-2 | determineAutowireCandidate 选择链 (@Primary→beanName→@Qualifier→@Priority) | 🔴 | **为什么🔴**: 多候选时的决策顺序 — 面试高频 |
| P2-1 | @Resource byName 语义 (JSR-250) vs @Autowired byType | 🟡 | **为什么🟡**: 两种 DI 哲学的对比 (按名 vs 按类型) |
| P2-2 | @Qualifier 过滤时机 (isAutowireCandidate 在 findAutowireCandidates 内) | 🟡 | **为什么🟡**: Qualifier 在候选收集阶段即过滤 — 时机决定行为 |
| P3-1 | injectionMetadataCache 缓存与 needsRefresh | 🟢 | **为什么🟢**: 反射扫描只做一次的缓存优化 |

### 深度分类

**§1 @Autowired**: postProcessProperties → findAutowiringMetadata → AutowiredFieldElement.inject → beanFactory.resolveDependency → DefaultListableBeanFactory.doResolveDependency(byType/byName/@Qualifier/@Primary fallback) — 🔴: 最常用注解的内部机制

**§2 @Resource + @Qualifier**: @Resource = byName(先)→byType(退), JSR-250标准; @Qualifier = byType 后附加过滤器 — 🟡: 与 @Autowired 对照理解

> → 引出 S1-6 BeanPostProcessor — @Autowired/@Resource 都是 BPP 实现。还有哪些 BPP？@CommonAnnotationBeanPostProcessor 处理 @PostConstruct/@PreDestroy、AsyncAnnotationBeanPostProcessor 处理 @Async...
