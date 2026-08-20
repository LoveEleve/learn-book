# S2-1 refresh() — Spring 容器启动的 12 步

> 项目: Spring Framework 6.x | 🔴 Deep / 1 篇 | AbstractApplicationContext.java(588行)
> 基线: S1-1~S1-7 spring-beans 全部 7 域 — refresh() 是这些域的指挥者

---

## §0.8

- 🔴 Deep，1篇 — refresh() 12 步 + 每步与 S1-1~S1-7 的映射
- 设计模式: [模式: Template Method] — 12 步固定流程，每步可被子类覆写

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AbstractApplicationContext.java:588 | — | **refresh()** 入口，synchronized via startupShutdownLock | High |
| L596 | 1 | **prepareRefresh()** — set active flag, init PropertySources | High |
| L599 | 2 | **obtainFreshBeanFactory()** — create or refresh BeanFactory, load BeanDefinitions | High |
| L602 | 3 | **prepareBeanFactory(beanFactory)** — set ClassLoader, add BPPs (ApplicationContextAwareProcessor), register default beans | High |
| L606 | 4 | **postProcessBeanFactory(beanFactory)** — subclasses override (Spring Boot adds WebApplicationContext support) | High |
| L610 | 5 | **invokeBeanFactoryPostProcessors(beanFactory)** — invoke all BFPPs, including @Configuration parsing | High |
| L612 | 6 | **registerBeanPostProcessors(beanFactory)** — register BPPs (don't instantiate beans yet) | High |
| L616 | 7 | **initMessageSource()** — i18n | High |
| L619 | 8 | **initApplicationEventMulticaster()** — event system | High |
| L622 | 9 | **onRefresh()** — subclass hook (Spring Boot creates Tomcat WebServer here) | High |
| L625 | 10 | **registerListeners()** — register ApplicationListeners from BeanFactory | High |
| L628 | 11 | **finishBeanFactoryInitialization(beanFactory)** — **instantiate ALL non-lazy singleton beans** | High |
| L631 | 12 | **finishRefresh()** — publish ContextRefreshedEvent, start Lifecycle beans | High |

---

## 02-04 聚合+分类+聚类

### 聚合 (P1≥5 / P2 2-4 / P3 1)

**P1 核心机制 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | refresh() 12 步完整序列 (L588-631) — Template Method 骨架 | 🔴 | **为什么🔴**: Spring 容器的"启动按钮" — 12步顺序固定、每步可被子类覆写 — 理解 Spring Boot ServletWebServerApplicationContext.onRefresh() 创建Tomcat的关键 |
| P1-2 | Step 5(BFPP) vs Step 6(BPP) 的时机差异 — 先定义后加工 | 🔴 | **为什么🔴**: BFPP 修改BeanDefinition(定义阶段)必须在BPP处理Bean实例(创建阶段)之前 — 顺序颠倒会导致新BeanDefinition未被BPP预处理 — @Autowired注入失败 |
| P1-3 | Step 11 finishBeanFactoryInitialization — 触发 S1-1~S1-7 全部机制 | 🔴 | **为什么🔴**: 这是spring-beans所有域的实际运行时刻 — preInstantiateSingletons()→getBean()→doCreateBean()→S1-3 13步生命周期 — refresh()是"总指挥" |

**P2 支持机制 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | Step 9 onRefresh() — 子类扩展hook (Spring Boot创建WebServer) | 🟡 | **为什么🟡**: 关键扩展点但不是refresh()主线 — 理解Spring Boot集成重要但独立于"容器启动"叙事 |
| P2-2 | Step 7-8 initMessageSource/initApplicationEventMulticaster — 国际化+事件基础设施 | 🟡 | **为什么🟡**: 两个子系统初始化 — 但在S2-4(MessageSource)和S2-3(Event)中有独立域深入讲解 |

**P3 辅助 (1):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P3-1 | Step 3 prepareBeanFactory 默认BPP注册(ContextApplicationListenerDetector等) | 🟢 | **为什么🟢**: 框架基础设施 — 理解全貌有帮助但不影响refresh()核心逻辑理解 |

### 聚类决策 (1篇)

**1篇理由**: refresh() 588行/12步 — 作为"容器启动总览"域，1篇够覆盖所有12步+2个核心对比(BFPP vs BPP + Step 11触发S1-1~S1-7)。不拆多篇 — refresh() 是一个连贯的流程 — 拆开会打断"12步交响乐"的完整性。

**单篇结构**: §1 12步完整概览(Template Method + 子类覆写) → §2 Step 5(BFPP) vs Step 6(BPP)时机差异 → §3 Step 11 触发 S1-1~S1-7 全部机制

> spring-context 层开始。引出 @Configuration 解析 — refresh() 的 Step 5 invokeBeanFactoryPostProcessors 是 @Configuration 类被解析成 BeanDefinition 的时机
