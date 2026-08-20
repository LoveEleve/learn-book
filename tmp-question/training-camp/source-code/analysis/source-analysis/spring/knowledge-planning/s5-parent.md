# S2-5 父子容器 — HierarchicalBeanFactory 的 parent 委托链

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 3文件/~140行核心代码
> 基线: S2-4 MessageSource — getMessageFromParent 的 parent 委托 → 现在解释 parent 本身如何建立

---

## §0.8

- 🟡 Working，1篇 — setParent 建立父子关系→getBean 委托→containsLocalBean vs containsBean→Spring Boot parent-child 模式
- 设计模式: [模式: 责任链] self→parent→parent.parent→...; [模式: 组合模式] ApplicationContext 同时是 BeanFactory 和 parent BeanFactory 的容器
- 关联 S2-4: getMessageFromParent 委托到此解释 — parent 是怎么设置的

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HierarchicalBeanFactory.java:40 | getParentBeanFactory() | **父容器接口**: 返回 parent BeanFactory — null=顶层容器 | High |
| HierarchicalBeanFactory.java:51 | containsLocalBean(name) | **本地检查**: 只在当前容器查 — 不同于 containsBean(会查parent) | High |
| AbstractApplicationContext.java:538 | setParent(ApplicationContext) | 设置父容器 → 同时同步到内部的 BeanFactory.parentBeanFactory + Environment.parent + MessageSource.parent | High |
| AbstractApplicationContext.java:1502 | getParentBeanFactory() | **桥接**: ApplicationContext.getParentBeanFactory → getParent() | High |
| AbstractApplicationContext.java:1517 | getInternalParentBeanFactory() | **优化**: parent是ConfigurableApplicationContext→直接getBeanFactory(跳过getParent()中间层) | High |
| AbstractBeanFactory.java:191-192 | AbstractBeanFactory(parentBeanFactory) | 构造时设置 parentBeanFactory — 注入 parent BeanFactory 引用 | High |
| AbstractBeanFactory.java:272-288 | getBean parent delegation | **核心委托**: getBean→!containsBeanDefinition(本地没)→parentBeanFactory.getBean(name) → parent是AbstractBeanFactory→直调doGetBean(跳过外层) | High |
| AbstractBeanFactory.java:437-438 | containsBean parent delegation | containsBean→parentBeanFactory.containsBean(不检查本地) → 检测parent中的bean | High |
| AbstractBeanFactory.java:456-459 | isSingleton parent delegation | isSingleton→!containsBeanDefinition(本地没)→parent.isSingleton → 检查parent中是否为singleton | High |

---

## 02-04 聚合+分类+聚类

### 聚合 (P1≥5 / P2 2-4 / P3 1)

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | AbstractBeanFactory.getBean parent 委托 (L272-288) | 🔴 | **为什么🔴**: 父子容器的核心行为 — getBean时本地找不到→委托parent — 这是 containsBean vs containsLocalBean 差异的来源 |
| P1-2 | AbstractApplicationContext.setParent (L538) — 建立父子关系 | 🔴 | **为什么🔴**: 怎么建立父子关系 — setParent 同步三个维度: BeanFactory.parent + Environment.parent + MessageSource.parent — 确保所有子系统的parent一致 |
| P1-3 | HierarchicalBeanFactory.containsLocalBean vs BeanFactory.containsBean | 🔴 | **为什么🔴**: 理解"本地"vs"全局"的关键 — containsLocalBean只在当前容器查 — containsBean会递归查parent → 两个方法的语义差异是最容易混淆的点 |

**P2 支持 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | getInternalParentBeanFactory 优化 (L1517) — parent是ConfigurableApplicationContext→直调getBeanFactory | 🟡 | **为什么🟡**: 性能优化 — 跳过getParent()中间层直接取BeanFactory → 非关键路径但展示Spring性能意识 |
| P2-2 | AbstractBeanFactory parent delegation 优化 (L276-281) — parent是AbstractBeanFactory→直调doGetBean | 🟡 | **为什么🟡**: 同P2-1 — 跳过外层getBean/类型检查节省~3层调用栈 → pairs的另一个性能优化 |

### 聚类 (1篇)

**1篇理由**: ~140行核心代码/3文件 — parent委托机制简单但影响深远(Spring Boot/MVC/父子容器分离)。1篇文章3个KP + 2个P2支持 = 正好一个完整叙事。不拆更多。

**单篇结构**: §1 setParent 建立关系(含BeanFactory/Environment/MessageSource同步) → §2 getBean parent 委托(含containsLocalBean语义) → §3 性能优化(paths优化) + Spring Boot 父子容器实例
