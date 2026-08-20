# S1-3 Bean 生命周期 — doCreateBean 的 13 步

> 项目: Spring Framework 6.x | 🔴 Deep / 2-3 篇 | AbstractAutowireCapableBeanFactory(2073行)
> 基线: S1-2 getBean内部 — getBean() 调用了 createBean → doCreateBean — 本篇展开 createBean 内部的完整 13 步

---

## §0.8

- 核心类: AbstractAutowireCapableBeanFactory(2073) + BeanPostProcessor(123) + InitializingBean/DisposableBean
- 🔴 Deep，2 篇

---

## 01 提取

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractAutowireCapableBeanFactory.java:560 | **doCreateBean()** — 生命周期的入口: 实例化→填充→初始化 | High |
| AbstractAutowireCapableBeanFactory.java:1405 | **populateBean()** — @Autowired/@Resource 依赖注入 | High |
| AbstractAutowireCapableBeanFactory.java:1813 | **initializeBean()** — @PostConstruct→afterPropertiesSet→initMethod 三段式 | High |
| AbstractAutowireCapableBeanFactory.java:424 | **applyBeanPostProcessorsBeforeInitialization** — 初始化前置回调 | High |
| AbstractAutowireCapableBeanFactory.java:440 | **applyBeanPostProcessorsAfterInitialization** — AOP 代理在此生成 | High |
| AbstractAutowireCapableBeanFactory.java:1166 | **applyBeanPostProcessorsBeforeInstantiation** — 可返回代理跳过实例化 | High |
| BeanPostProcessor.java:82,108 | **postProcessBefore/After Initialization** — @PostConstruct→afterPropertiesSet→initMethod 的三段式 | High |

---

## 02-04 聚合+分类+聚类

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | doCreateBean 13 步完整流程 (实例化→填充→初始化→代理→注册销毁) | 🔴 | **为什么🔴**: 每个 Bean 创建的必经路径 — @Autowired/@PostConstruct/AOP 全在此链上 |
| P1-2 | BPP 三层干预 (InstantiationAware/BPP/DestructionAware) | 🔴 | **为什么🔴**: 所有"魔法"(AOP/@Async/@Transactional) 的载体 |
| P2-1 | 构造器 vs setter 注入分支 (createBeanInstance) | 🟡 | **为什么🟡**: 两种注入方式的底层差异 — 官方推荐构造器注入的原因 |
| P2-2 | predictBeanType 类型预测 (proxyTypes 缓存) | 🟡 | **为什么🟡**: @Autowired 类型匹配在实例化前的预测机制 |
| P3-1 | earlyBeanReference 与二次包装检查 | 🟢 | **为什么🟢**: 循环依赖+AOP 协作的细节 — 与 S1-4 联动 |

### 深度分类

**§1 (§1)**: doCreateBean 13 步完整流程 — 实例化→三级缓存→属性填充→初始化→代理→注册DisposableBean — 🔴: 每个 Bean 创建的必经路径，@Autowired/@PostConstruct/AOP 都在此链上

**§2 (§2)**: BeanPostProcessor 干预点 — InstantiationAwareBPP/BPP/DestructionAwareBPP 三层 — 🔴: 理解所有"魔法"(AOP/@Async/@Transactional) 的入口

> → 引出 S1-4 循环依赖 — doCreateBean 中 addSingletonFactory 放入三级缓存解决 A↔B 循环引用
