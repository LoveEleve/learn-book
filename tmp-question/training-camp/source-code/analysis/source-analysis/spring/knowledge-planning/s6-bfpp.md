# S2-6 BFPP 全景 — BeanFactoryPostProcessor + ${} 占位符解析

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 5文件/1346行
> 基线: S2-2 @Configuration — CCPP 是 BFPP 的子类型；S2-1 refresh() Step 5 invokeBeanFactoryPostProcessors

---

## §0.8

- 🟡 Working，1篇 — BFPP 接口 → PriorityOrdered/Ordered 排序 → PropertySourcesPlaceholderConfigurer → PropertyPlaceholderHelper 解析引擎
- 设计模式: [模式: 模板方法] parseStringValue 递归解析嵌套占位符; [模式: 策略模式] PlaceholderResolver 不同来源(Environment/Properties)
- refresh() Step 5: invokeBeanFactoryPostProcessors → PostProcessorRegistrationDelegate 三阶段调用

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| BeanFactoryPostProcessor.java:80 | postProcessBeanFactory() | **单方法接口**: 在 BeanDefinition 加载后/Bean 实例化前修改 BeanDefinition — 与 BPP(Bean 级别)不同 | High |
| PostProcessorRegistrationDelegate.java:68 | invokeBeanFactoryPostProcessors() | **三阶段调用**: ①PriorityOrdered BDRPP ②Ordered BDRPP ③non-ordered BDRPP → ①PriorityOrdered BFPP ②Ordered BFPP ③non-ordered BFPP | High |
| PostProcessorRegistrationDelegate.java:104-119 | PriorityOrdered 阶段 | 先 invokeBeanDefinitionRegistryPostProcessors(PriorityOrdered)→sortPostProcessors→invoke → registryProcessors 累积 | High |
| PostProcessorRegistrationDelegate.java:121-131 | Ordered 阶段 | 判断未处理过的(processedBeans不含)→Ordered类型的BDRPP → sortPostProcessors→invoke | High |
| PostProcessorRegistrationDelegate.java:145-153 | Non-ordered 常规阶段 | while 循环: 逐轮取所有未处理的BDRPP→invoke→循环直到无新增(新注册的BDRPP在下一轮处理) | High |
| PropertySourcesPlaceholderConfigurer.java:132-157 | postProcessBeanFactory() | 核心入口: Environment 已注入(L68 implements EnvironmentAware)→propertySources取自Environment→createPropertyResolver→processProperties→doProcessProperties遍历所有BD替换${} | High |
| PropertySourcesPlaceholderConfigurer.java:175-193 | processProperties() | createPropertyResolver→MutablePropertySources(组合remote+local+system)→doProcessProperties: 遍历beanDefinitionNames→每个BD中replace ${} | High |
| PropertyPlaceholderHelper.java:100-114 | replacePlaceholders() | **核心解析**: 递归解析 ${server.port:8080} 格式 — prefix="${" suffix="}" → parseStringValue 处理嵌套+默认值 | High |
| PropertyPlaceholderHelper.java:37 | prefix/suffix | 默认 "${" + "}" — 可自定义为 "@{" 或其他 | High |

---

## 02-04 聚合+分类+聚类

### 聚合 (P1≥5 / P2 2-4 / P3 1)

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | PostProcessorRegistrationDelegate.invokeBeanFactoryPostProcessors() 三阶段排序+调用 | 🔴 | **为什么🔴**: 所有BFPP的调度中心 — PriorityOrdered→Ordered→no-priority 三阶段确保CCPP(配置解析)在所有其他BFPP之前执行 — 如果阶段错乱，Configuration没有解析完就开始处理${}占位符 |
| P1-2 | BeanFactoryPostProcessor.postProcessBeanFactory() — 在BeanDefinition加载后/Bean实例化前修改 | 🔴 | **为什么🔴**: BFPP的语义边界 — 与BeanPostProcessor的区别(BPP处理Bean实例) — 理解"定义扩展"vs"实例加工"的关键 |
| P1-3 | PropertySourcesPlaceholderConfigurer.postProcessBeanFactory — Environment驱动的${}解析 | 🔴 | **为什么🔴**: Spring Boot占位符解析的实际实现 — ${server.port}→Environment.getProperty — 从Environment(Servlet\Properties\System Properties)三重来源取值 |

**P2 支持 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | PropertyPlaceholderHelper.parseStringValue 递归解析 | 🟡 | **为什么🟡**: 内核解析引擎 — 支持嵌套${a${b}}、默认值${key:default} — 是PropertySourcesPlaceholderConfigurer的核心工具但不改变BFPP调度逻辑 |
| P2-2 | BDRPP vs BFPP 双接口分离 — invokeBeanDefinitionRegistryPostProcessors 有一个额外的 do-while 循环 | 🟡 | **为什么🟡**: 设计意图—BDRPP可以注册新BD(ConfigClassPostProcessor)→需要循环直到无新增→普通BFPP只需单次调用 |

### 聚类 (1篇)

**1篇理由**: 1346行/5文件 — BFPP接口(82行)+调度引擎(544行)+实现(312行)+解析引擎(138行) — 与S2-2的3649行相比规模小3×。1篇文章(~47行)覆盖接口→调度→实现→解析 四层主线。不拆更多—BFPP机制简单(一个方法 + 三阶段排序)，概念比代码复杂。

**单篇结构**: §1 BFPP 接口 + PriorityOrdered/Ordered 排序机制 → §2 PropertySourcesPlaceholderConfigurer — Environment驱动的${}解析 → §3 PropertyPlaceholderHelper — 递归嵌套+默认值解析引擎
