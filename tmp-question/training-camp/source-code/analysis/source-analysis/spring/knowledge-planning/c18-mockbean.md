# C-18 @MockBean — 测试替身注入 (注解 → MockitoPostProcessor → 替换 Bean)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | MockBean(约130行)+MockitoPostProcessor(约340行)+MockDefinition(约170行)+DefinitionsParser+MockitoBeans
> 基线: C-17 TestContext — 依赖注入监听器注入真实 Bean, @MockBean 用 Mockito mock 替换目标 Bean; 原始执行计划 8-A

---

## §0.8

- 🟡 Working，1篇 — 声明(@MockBean 注解: value 指定类型, 标在字段上) → 收集(MockitoPostProcessor.postProcessBeanFactory: DefinitionsParser 扫描配置类/@MockBean 字段) → 替换(register: 定位同类型 bean→用 Mockito.mock 的 RootBeanDefinition 覆盖其 beanName) → 创建(MockDefinition.createMock→Mockito.mock, RETURNS_DEFAULTS) → 与 @SpyBean/TestContext 对照
- 设计模式: [模式: BeanDefinitionRegistryPostProcessor]—启动期改 BeanDefinition; [模式: 代理/替身]—Mockito mock 替换真实 bean; [模式: 组合]—MockitoBeans 收集所有 mock

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MockBean.java:103,119 | 注解 | **@MockBean**: value() 指定要 mock 的类型(按类型替换) — 标在测试类字段上 | High |
| MockitoPostProcessor.java:86,132 | 后处理器 | **postProcessBeanFactory L132**: L137 收集定义→register(MockitoBeans 单例 L138 + DefinitionsParser 解析配置类/@MockBean 字段) | High |
| MockitoPostProcessor.java:179 | registerMock | **替换**: L190 definition.createMock → 建 RootBeanDefinition(mockType L200) | High |
| MockitoPostProcessor.java:209 | 定位 beanName | **替换目标**: @MockBean(name)→用 name; 否则 getExistingBeans(同类型)→单个→替换其 beanName; 多个→primary; 无→新生成 | High |
| MockDefinition.java:62,144,149 | mock 创建 | **createMock**: L149 Mockito.mock(mockType, settings); answer 默认 RETURNS_DEFAULTS(L62) | High |
| MockitoBeans.java | 收集 | **mock 集合**: 注册为单例, 保存所有 mock bean(供断言/清理) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注解+后处理器+定义约 700 行 — 知识单线: "@MockBean 声明 → 启动期用 mock BeanDefinition 替换真实 Bean". 1篇 (~45行) 按"声明→替换→创建→对照"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | MockitoPostProcessor 替换机制 (postProcessBeanFactory→register→覆盖 beanName) | 🔴 | **为什么🔴**: @MockBean 的核心 — 启动期用 mock 的 BeanDefinition 替换真实 bean, 依赖注入自动拿 mock |
| P1-2 | beanName 定位策略 (name/同类型/primary/生成) | 🔴 | **为什么🔴**: 替换哪个 bean 的决定逻辑 — 接口多实现时选 primary |
| P1-3 | MockDefinition.createMock (Mockito.mock + RETURNS_DEFAULTS) | 🔴 | **为什么🔴**: mock 怎么生成 — 行为未 stubbing 时返回默认值 |
| P2-1 | DefinitionsParser 扫描 (@MockBean 字段/配置类收集) | 🟡 | **为什么🟡**: 声明怎么被发现 |
| P2-2 | 与 @SpyBean 对照 (mock vs spy 半真) | 🟡 | **为什么🟡**: 选择: 全替身 vs 部分真实 |
| P3-1 | 与 TestContext 缓存交互 (不同 mock 组合→不同缓存键) | 🟢 | **为什么🟢**: @MockBean 组合不同会重建容器 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **替换机制** (后处理器 + 定位 + 覆盖) | 🔴 | @MockBean 核心 — 启动期替身 |
| B | **创建与扫描** (MockDefinition + DefinitionsParser) | 🟡 | mock 生成与声明收集 |
| C | **对照与交互** (@SpyBean + TestContext 缓存) | 🟡 | 选择与容器重建 |

> **Cluster A (§1)**: @MockBean 注解 + MockitoPostProcessor(收集)
> **Cluster B (§2)**: register/registerMock(定位 beanName + 用 mock BeanDefinition 替换) + MockDefinition.createMock
> **Cluster C (§3)**: @SpyBean 对照 + 与 TestContext 缓存(不同 mock→不同容器) + 使用建议

→ 引出 8-B: @Sql — 替换了 Bean 后还要准备数据: @Sql 在测试前后执行 SQL(建表/插数据/清理)

(End of file - total 61 lines)
