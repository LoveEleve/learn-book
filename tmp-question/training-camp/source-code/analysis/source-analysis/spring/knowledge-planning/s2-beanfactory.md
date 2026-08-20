# S1-2 BeanFactory — IoC 容器的核心接口与实现

> 项目: Spring Framework 6.x | 类型: **纯框架** | 🔴 Deep / 2 篇
> 核心文件: BeanFactory(404)/HierarchicalBF(53)/ListableBF(423)/ConfigurableBF(436)/DefaultListableBF(2771) | ~4087 行
> 基线: S1-1 BeanDefinition — BeanDefinition 定义了"什么 Bean" — BeanFactory 是"怎么管理和获取 Bean 的容器"

---

## §0.8 域审核

- 核心类: 6 个，~4087 行 → 🔴 Deep，2-3 篇
- 淘汰检查: BeanFactory 是 Spring 核心，无淘汰项
- 设计模式: [模式: Factory Method] (getBean) / [模式: Composite] (HierarchicalBF父子容器) / [模式: Template Method] (AbstractBF.getBean模板)

---

## 01 提取

### BeanFactory.java (404 行 — 根接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| BeanFactory.java:153 | **getBean(String name)** — 按名获取 Bean | High |

### DefaultListableBeanFactory.java (2771 行 — 核心实现)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultListableBeanFactory.java:195 | **beanDefinitionMap = ConcurrentHashMap** — 所有 BeanDefinition 存储于此 | High |
| DefaultListableBeanFactory.java:385 | **getBean()** — 入口：查缓存→无缓存→创建→存储 singletonObjects | High |

### HierarchicalBeanFactory.java (53 行 — 父子容器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| HierarchicalBeanFactory | **getParentBeanFactory()** — 当前容器找不到 Bean→向上查父容器 | High |

---

## 02-04 聚合+分类+聚类

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | BeanFactory 5 层接口继承树 (ISP + 能力叠加) | 🔴 | **为什么🔴**: 所有 getBean 调用的入口 — 理解接口分层才能读懂容器能力边界 |
| P1-2 | doGetBean 8 步 + 三级缓存 + 循环依赖 | 🔴 | **为什么🔴**: IoC 最核心路径 — 三级缓存+循环依赖是高频面试+实战核心 |
| P2-1 | 父子容器 parent 查找链 (HierarchicalBeanFactory) | 🟡 | **为什么🟡**: MVC 双层容器模型的基础 — 就近优先+父容器兜底 |
| P2-2 | FactoryBean & 前缀 + getObjectForBeanInstance | 🟡 | **为什么🟡**: 特殊 Bean 获取机制 — 与 S1-7 联动 |
| P3-1 | adaptBeanInstance 类型校验 | 🟢 | **为什么🟢**: requiredType 检查的收尾细节 |

### 深度分类

**§1 继承树**: BeanFactory→Hierarchical→Listable→Configurable→DefaultListable — 5 层接口继承，每层增加一种能力 — 🔴: BeanFactory 是最基础接口，理解继承链才能读懂后续所有 getBean 调用

**§2 getBean() 内部**: singletonObjects 缓存→parentFactory 回退→createBean→三级缓存处理循环依赖 — 🔴: 三级缓存是 Spring IoC 最核心的机制之一，循环依赖是高频面试+实战问题

> → 引出 S1-3 Bean 生命周期 — getBean() 创建 Bean 的完整 13 步
