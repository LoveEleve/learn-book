# S1-4 循环依赖 — 三级缓存 + getEarlyBeanReference 的协同机制

> 项目: Spring Framework 6.x | 🔴 Deep / 1 篇 | DefaultSingletonBeanRegistry
> 基线: S1-2 getBean 内部(三级缓存) + S1-3 BeanPostProcessor(getEarlyBeanReference)

---

## §0.8

- 🔴 Deep，1篇 — 聚焦三级缓存在循环依赖场景下的协作

---

## 01 提取

| Source | Confidence |
|--------|------------|
| DefaultSingletonBeanRegistry.java:85,94,88 — 三级缓存字段 | High |
| DefaultSingletonBeanRegistry.java:210 — getSingleton(beanName, true) 三级查找 | High |
| AbstractAutowireCapableBeanFactory.java:600 — addSingletonFactory(lambda) | High |
| AbstractAutowireCapableBeanFactory.java:975 — getEarlyBeanReference() | High |

---

## 02-04

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 循环依赖解决机制 (三级缓存时序 + 构造器 vs setter) | 🔴 | **为什么🔴**: 高频面试题 + 理解三级缓存设计的关键 |
| P2-1 | getEarlyBeanReference 与 AOP 代理的协作 (early proxy 语义) | 🟡 | **为什么🟡**: 循环依赖+AOP 的交互 — early proxy 与最终代理的关系 |
| P3-1 | earlyBeanReferences.remove 判定逻辑 (二次包装防重) | 🟢 | **为什么🟢**: remove!=bean 的判定细节 — 防重复包装的实现 |

### 深度分类

**单篇**: 循环依赖的完整时间线 — A↔B 循环依赖时三级缓存的精确协作流程，包含构造器注入(不可解决)vs setter注入(可解决)的根本原因 — 🔴: 高频面试题 + 理解 AOP 与循环依赖协作的关键

> → 引出 S1-5 DI注入三机制 — 循环依赖解决了 A 和 B 的引用问题，但 @Autowired 怎么知道 A 需要注入哪个 B？@Qualifier 怎么区分同类型多实例？
