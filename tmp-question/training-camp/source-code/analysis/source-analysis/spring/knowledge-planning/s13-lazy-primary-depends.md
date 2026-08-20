# S2-13 @Lazy/@Primary/@DependsOn — Bean 注册控制三元组

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 3注解/~400行
> 基线: S2-1 refresh() Step 11 + S2-5 DI注入 — @Lazy影响eager创建/@Primary影响DI选择/@DependsOn影响创建顺序

---

## §0.8

- 🟡 Working，1篇 — @Lazy(延迟创建) + @Primary(优先选择) + @DependsOn(依赖排序) — 三个小注解深刻影响Bean生命周期
- 设计模式: [模式: 策略模式]—@Primary在DI多候选时作为优先级策略

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| @Lazy.java:75行 | @Lazy | value=true→Bean推迟到首次getBean创建 / false→eager(Singleton在Step 11创建) | High |
| AbstractBeanFactory.java:641 | isLazyInit() | preInstantiateSingletons→`!mbd.isLazyInit()` → 跳过lazy Bean → 只有首次getBean时才创建 | High |
| @Primary.java:96行 | @Primary | 标记为"首选"Bean — DI时多个同类型候选→@Primary标记的胜出 → 从S1-5 DI注入中determinePrimaryCandidate | High |
| DefaultListableBeanFactory.java:2049 | determinePrimaryCandidate | DI解析多候选→遍历candidates→`isPrimary()`→找到一个返回/未找到返回null→继续下一个tiebreaker(beanName/@Qualifier/@Priority) | High |
| @DependsOn.java:56行 | @DependsOn | value=依赖的bean名称数组→AbstractBeanFactory.getBean→`mbd.getDependsOn()`(L306)→先创建依赖Bean→再创建当前Bean | High |

---

## 02-04 聚合+分类+聚类

### 聚合 — P1 核心 (3)

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @Lazy — isLazyInit 检查 + preInstantiateSingletons 跳过 | 🔴 | **为什么🔴**: 影响refresh() Step 11的Bean创建顺序——非Lazy singleton在Step 11全部创建——@Lazy singleton推迟到首次getBean——影响应用启动性能和故障检测时机 |
| P1-2 | @Primary — determinePrimaryCandidate + DI解析优先级 | 🔴 | **为什么🔴**: DI注入多候选时的冲突解决——@Primary是四层tiebreaker的第一层(@Primary→beanName→@Qualifier→@Priority)——如果不设置@Primary而有两个同类型Bean→NoUniqueBeanDefinitionException |
| P1-3 | @DependsOn — getDependsOn + 先创建依赖Bean + 循环检测 | 🔴 | **为什么🔴**: Bean创建顺序控制——A dependsOn B→创建A前先创建B——容器检测循环dependsOn关系→抛BeanCreationException——与autowired依赖不同(autowired隐式排序/DependsOn显式排序) |

### 聚类 (1篇)

**1篇理由**: 三个小注解(~400行)，虽功能独立但共享共同主题"Bean注册控制"—都在doRegisterBean阶段设置BeanDefinition属性—影响后续getBean/DI行为。1篇(~42行)覆盖三个注解。

**单篇结构**: §1 @Lazy — eager/lazy创建时机 → §2 @Primary — DI多候选优先级 → §3 @DependsOn — Bean创建顺序控制
