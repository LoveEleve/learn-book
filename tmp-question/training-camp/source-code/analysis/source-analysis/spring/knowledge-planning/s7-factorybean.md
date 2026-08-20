# S1-7 FactoryBean — Spring 的"Bean 工厂代理"机制

> 项目: Spring Framework 6.x | 🟡B / 1 篇 | FactoryBean(149行)+AbstractBeanFactory(1857行)
> 基线: S1-6 BPP全景 — BPP 是"加工Bean" — FactoryBean 是"创建Bean" — MyBatis Mapper/Dubbo Reference 都是 FactoryBean

---

## §0.8

- 🟡B，1 篇 — FactoryBean 接口 + `&` 前缀 + MyBatis/Dubbo 实例

---

## 01 提取

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| FactoryBean.java:96 | **getObject()** — 返回创建的 Bean(MyBatis MapperProxy/Dubbo RPC Proxy) | High |
| FactoryBean.java:118 | **getObjectType()** — 声明的返回类型(用于 @Autowired 类型匹配) | High |
| FactoryBean.java:122 | **isSingleton()** — 决定创建的 Bean 是否单例 | High |
| AbstractBeanFactory.java:1857 | **getObjectForBeanInstance()** — doGetBean 中处理 FactoryBean 的核心方法 | High |
| AbstractBeanFactory.java:246 | **transformedBeanName()** — 剥离/检测 `&` 前缀 | High |

---

## 02-04

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | FactoryBean 契约 (getObject/getObjectType/isSingleton) + 产物缓存 | 🔴 | **为什么🔴**: "特殊 Bean 来源"机制 — MyBatis/Dubbo 基础设施 |
| P1-2 | getObjectForBeanInstance 三段逻辑 + & 前缀 | 🔴 | **为什么🔴**: 产物 vs 工厂的分界 — & 前缀的 escape hatch |
| P2-1 | MyBatis MapperFactoryBean 应用 (JDK 动态代理产物) | 🟡 | **为什么🟡**: FactoryBean 的真实工业用例 |
| P3-1 | transformedBeanName/isFactoryDereference 细节 | 🟢 | **为什么🟢**: 名称解析的实现细节 |

### 深度分类

**单篇**: FactoryBean 接口 → `&` 前缀机制 → `getObjectForBeanInstance` 的三段逻辑 → MyBatis MapperFactoryBean 实例 → Dubbo ReferenceBean 实例 — 🟡: 理解"特殊 Bean"来源机制 (MyBatis/Dubbo 基础设施)

> → spring-beans 层全部 7 域完成。引出 spring-context 层: ApplicationContext / refresh() / @Configuration
