# S1-1 BeanDefinition — Spring IoC 的元数据蓝图

> 项目: Spring Framework 6.x | 类型: **纯框架** (非规范参考实现)
> 核心文件: BeanDefinition(388行)/AbstractBeanDefinition(1405行)/RootBeanDefinition(682行)/GenericBeanDefinition(102行) | 🔴 Deep
> 基线: Tomcat T-7 — Spring 不是 Servlet 容器，是"Bean 工厂"

---

## §0.8 域审核

- 核心类: 4 个，~2577 行 → 🔴 Deep，2-3 篇
- 淘汰检查: BeanDefinition 是 Spring 核心，无淘汰项
- 设计模式: [模式: Builder] (BeanDefinitionBuilder) / [模式: Prototype] (GenericBD→RootBD 克隆)
- 框架类型: 纯框架 — 不涉及规范对应

---

## 01 提取

### BeanDefinition.java (388 行 — Bean 元数据蓝图接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| BeanDefinition.java:49,57 | **SCOPE_SINGLETON/SCOPE_PROTOTYPE** — 两个内置作用域常量 | High |
| BeanDefinition.java:64,75,83 | **ROLE_APPLICATION(0)/SUPPORT(1)/INFRASTRUCTURE(2)** — Bean 角色三层 | High |
| BeanDefinition.java:122 | **getBeanClassName()** — 目标类的全限定名 | High |
| BeanDefinition.java:129,136 | **setScope/getScope** — singleton/prototype/request/session | High |
| BeanDefinition.java:143 | **setLazyInit** — 是否延迟初始化 | High |
| BeanDefinition.java:164 | **getDependsOn()** — 依赖其他 Bean 必须先创建 | High |
| BeanDefinition.java:222,226 | **factoryBeanName/factoryMethodName** — @Bean 注解对应的工厂方法元数据 | High |

### AbstractBeanDefinition.java (1405 行 — 具体字段持有者)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractBeanDefinition.java:172 | **scope=SCOPE_DEFAULT** — 默认 singleton | High |
| AbstractBeanDefinition.java:179 | **lazyInit** — 是否延迟初始化 | High |
| AbstractBeanDefinition.java:181 | **autowireMode=AUTOWIRE_NO** — 自动装配模式(0/1/2) | High |
| AbstractBeanDefinition.java:215 | **propertyValues** — DI 时填充的属性值 | High |
| AbstractBeanDefinition.java:220,223 | **initMethodNames/destroyMethodNames** — @PostConstruct/@PreDestroy 对应的字符串 | High |
| AbstractBeanDefinition.java:231 | **role=ROLE_APPLICATION** — 默认应用级 | High |

### RootBeanDefinition.java (682 行 — 合并后)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| RootBeanDefinition | **merged bean definition** — 合并了父 BD + 当前 BD + post-process 后的最终形态 | High |
| RootBeanDefinition | **targetType** — 已解析的 Class 引用(ResolvableType) | High |

### GenericBeanDefinition.java (102 行 — 合并前)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| GenericBeanDefinition | **raw bean definition** — 用户定义的原始形态(未合并父定义) | High |

---

## 02-04 聚合+分类+聚类

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | BeanDefinition 接口设计 (SCOPE/ROLE/lazyInit/dependsOn/init/destroy/factoryBean/factoryMethod) | 🔴 | **为什么🔴**: Spring IoC 的元数据基石 — 不了解 BD 就不了解 Bean 如何定义 — 所有配置源(XML/注解/@Bean)统一于此 |
| P1-2 | GenericBD → RootBD 合并流程 (getMergedBeanDefinition) | 🔴 | **为什么🔴**: 每个 Bean 创建前必经的合并 — 无 parent 也统一转 RootBD — 容器处理只面对一种 BD 形态 |
| P2-1 | parent/child 继承合并 (overrideFrom + 深拷贝) | 🟡 | **为什么🟡**: XML parent 继承的机制 — 理解深拷贝+覆盖的合并语义 |
| P3-1 | mergedBeanDefinitions 缓存与 stale 失效 | 🟢 | **为什么🟢**: 合并结果的缓存策略 — 细节优化, 非核心机制 |

### 深度分类

| Cluster | KP | 级别 | 理由 |
|:--:|------|:--:|------|
| A | **BeanDefinition 接口设计** (SCOPE/ROLE/lazyInit/dependsOn) | 🔴 | Spring IoC 的元数据基石 — 不了解 BD 就不了解 Bean 如何定义 |
| B | **RootBD vs GenericBD** (合并前 vs 合并后) | 🟡 | parent BD + child BD → merge → RootBD — 理解 Spring 的"继承"概念 |

> **Cluster A (§1)**: BeanDefinition 作为 IoC 元数据蓝图 — scope/role/lazyInit/init/destroy/factoryBean<br>
> **Cluster B (§2)**: RootBeanDefinition vs GenericBeanDefinition — 合并前 vs 合并后 + 为什么需要两种定义

→ 引出 S1-2 BeanFactory — BeanDefinition 是"定义什么 Bean"，BeanFactory 是"怎么创建和管理 Bean 的容器"。
