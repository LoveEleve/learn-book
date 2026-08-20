# S1-6 BeanPostProcessor 全景 — doCreateBean 各阶段的 BPP 切入

> 项目: Spring Framework 6.x | 🟡B / 1 篇 | 5 个核心 BPP 实现
> 基线: S1-3 doCreateBean 13步 + S1-5 @Autowired/@Resource — 所有"魔法注解"都是 BPP 在 doCreateBean 的不同阶段切入

---

## 01 提取

| BPP 接口 | doCreateBean 切入时机 | 核心实现 |
|------|:--:|------|
| InstantiationAwareBeanPostProcessor | postProcessBeforeInstantiation(实例化前) / postProcessAfterInstantiation(实例化后) | AbstractAutoProxyCreator |
| SmartInstantiationAwareBeanPostProcessor | predictBeanType(预测类型) / getEarlyBeanReference(早期引用) | AbstractAutoProxyCreator |
| BeanPostProcessor | postProcessBeforeInitialization(初始化前) / After(初始化后) | @PostConstruct/@PreDestroy 处理器 |
| DestructionAwareBeanPostProcessor | postProcessBeforeDestruction(销毁前) | @PreDestroy 处理器 |

---

## 02-04 聚合+分类+聚类

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | BPP 接口继承树 (4 子接口 + 时机语义) | 🔴 | **为什么🔴**: 所有 Spring 魔法注解(AOP/@Async/@Transactional)的载体 |
| P1-2 | doCreateBean 各阶段 BPP 时间线 (6 个切入窗口) | 🔴 | **为什么🔴**: 理解"何时切什么"是 BPP 体系的核心 |
| P2-1 | 内置 BPP 映射 (Autowired/CommonAnnotation/AutoProxy/InitDestroy) | 🟡 | **为什么🟡**: 常用注解对应的 BPP 全览 |
| P3-1 | 自定义 BPP 编写模式 | 🟢 | **为什么🟢**: 扩展点实践 — 了解即可 |

### 深度分类

**单篇**: BPP 接口继承树 + 5 个关键实现在 doCreateBean 的时间线位置 + 自定义 BPP 示例 — 🔴: 所有 Spring 魔法注解(AOP/@Async/@Transactional)的载体

> → 引出 S1-7 FactoryBean — Spring 还有另一种"特殊 Bean"机制 — FactoryBean 不是 BPP — 它是在 getBean 级别工作的"Bean 工厂代理"
