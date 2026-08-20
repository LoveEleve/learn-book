# S2-8 AppContext 三大实现 — AnnotationConfig / ClassPathXml / GenericWeb

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 5文件/~1630行
> 基线: S2-1 refresh() — 现在讲 refresh() 之前发生了什么 → 构造器→注册→refresh 三部曲

---

## §0.8

- 🟡 Working，1篇 — GenericApplicationContext 内置 BeanFactory → AnnotationConfig/ClassPathXml/GenericWeb 三大路径 → AnnotatedBeanDefinitionReader 注册管线
- 设计模式: [模式: 模板方法] GenericApplicationContext 模板 + 子类自定义 reader; [模式: 建造者模式] AnnotatedBeanDefinitionReader 逐步构建 BeanDefinition
- pre-refresh 做什么: 创建BeanFactory→创建reader→register(annotatedClasses)→scan(packages)→loadBeanDefinitions(xml)→refresh()

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| GenericApplicationContext.java:105-121 | 构造器 | **内置 DefaultListableBeanFactory**: 不通过 obtainFreshBeanFactory 获取 — 在构造器中直接创建 L121 `this.beanFactory = new DefaultListableBeanFactory()` — registerSingleton 可工作 | High |
| GenericApplicationContext.java:refreshBeanFactory() | 空实现 | 父类 AbstractApplicationContext.refresh() Step 2 调 obtainFreshBeanFactory→refreshBeanFactory — Generic 版本为空(beanFactory 已在构造器创建) | High |
| AnnotationConfigApplicationContext.java:56-92 | 构造器 | **三步构造**: ①new AnnotatedBeanDefinitionReader(this)→②new ClassPathBeanDefinitionScanner(this)→③register(componentClasses)/scan(packages)→④refresh() — ③把注解类转BeanDefinition→④激活容器 | High |
| AnnotatedBeanDefinitionReader.java:146-147 | registerBean→doRegisterBean | **注解类→BeanDefinition**: 创建AnnotatedGenericBeanDefinition→处理@Scope/@Lazy/@Primary/@DependsOn→BeanDefinitionHolder→registry.registerBeanDefinition | High |
| ClassPathXmlApplicationContext.java:configLocations | XML路径 | configLocations→new XmlBeanDefinitionReader(this)→loadBeanDefinitions(configLocations)→refresh() | High |
| GenericWebApplicationContext.java:setServletContext | Web集成 | setServletContext→init WebApplicationContext→注册 servletContext/scopes→refresh() — Spring Boot WebServer 创建后的容器上下文 | High |

---

## 02-04 聚合+分类+聚类

### 聚合

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | GenericApplicationContext 内置 BeanFactory + refreshBeanFactory 空实现 | 🔴 | **为什么🔴**: 与父类 AbstractApplicationContext 的 obtrainFreshBeanFactory 不同 — Generic 的 BeanFactory 在构造器创建而非 refresh 时创建 — 这就是为什么 S2-1 refresh() Step 2 在 Generic 下是空操作 |
| P1-2 | AnnotationConfigApplicationContext 构造器: reader+scanner→register→refresh 三部曲 | 🔴 | **为什么🔴**: Spring Boot 最常用的 ApplicationContext — register(annotatedClass)→reader.doRegisterBean→创建BeanDefinition 是 refresh() 之前的"加载阶段" — 理解这个时序才能理解 Spring Boot 启动 |
| P1-3 | AnnotatedBeanDefinitionReader.doRegisterBean — 注解类→AnnotatedGenericBeanDefinition 全流程 | 🔴 | **为什么🔴**: 回答"注册一个注解类时发生了什么" — @Scope/@Lazy/@Primary/@DependsOn/@Description/@Role 的提取和设置 — 这是 BeanDefinition 加载的所有注解处理 |

**P2 支持 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | ClassPathXmlApplicationContext — XmlBeanDefinitionReader 加载XML | 🟡 | **为什么🟡**: 历史遗留路径 — 现代 Spring Boot 不使用XML — 但理解它有助于理解 BeanDefinitionReader 的抽象(注解Reader和XMLReader是同一接口的不同实现) |
| P2-2 | GenericWebApplicationContext — ServletContext 集成 + Web Scopes 注册 | 🟡 | **为什么🟡**: Web容器集成 — setServletContext→init scopes→attribute→setParent — 这是 Spring MVC/Spring Boot Web 的基础 |

### 聚类 (1篇)

**1篇理由**: ~1630行/5文件 — Generic 骨架(636行)+注解路径(201+305)+XML路径(212行)+Web路径(276行) — 四条路径的核心差异=BeanDefinition 如何被注册(db/XML/scan)。1篇(~47行)覆盖 before refresh → register → refresh 三部曲。

**单篇结构**: §1 GenericApplicationContext 骨架(内置BeanFactory) → §2 AnnotationConfig路径(reader+register 三部曲) → §3 ClassPathXml/Web 路径(对比 + GenericWeb)
