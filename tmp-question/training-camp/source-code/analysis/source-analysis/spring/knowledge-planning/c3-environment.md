# C-3 Environment — 配置属性体系 (PropertyResolver → PropertySources → 占位符解析)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | PropertyResolver(114行)+Environment(152行)+AbstractEnvironment(601行)+StandardEnvironment(103行)+MutablePropertySources(232行)+PropertySourcesPropertyResolver(126行)+PropertySource(260行)+PlaceholderParser(564行)+PropertyPlaceholderHelper(138行)
> 基线: C-2 类型转换 — @Value("${...}") 先取到 String 再转类型 — 本域展开上游: ${} 占位符谁解析、值从哪些来源按什么优先级取; 原始执行计划 0-3

---

## §0.8

- 🟡 Working，1篇 — 接口层次(PropertyResolver 取值 → Environment 加 profile → ConfigurableEnvironment 可变) → 来源容器(MutablePropertySources 有序列表: addFirst/addLast, CopyOnWriteArrayList) → 查找链(PropertySourcesPropertyResolver: 遍历首个非 null → 嵌套占位符递归 → 类型转换) → 默认来源(StandardEnvironment: systemProperties + systemEnvironment) → 占位符(PlaceholderParser: ${} 解析/转义/递归/默认值)
- 设计模式: [模式: 责任链]—propertySources 按优先级逐个查找首个非 null; [模式: 组合]—MutablePropertySources 聚合多个 PropertySource; [模式: 模板方法]—customizePropertySources 钩子

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| PropertyResolver.java:30,47,67 | 接口 | **取值三方法**: getProperty(key) / getProperty(key, default) / getProperty(key, Class)(带类型转换 — 衔接 C-2) + containsProperty(L36) | High |
| Environment.java:72,87 | Environment 接口 | **加 profile**: extends PropertyResolver — getActiveProfiles — "属性+环境"统一接口 | High |
| StandardEnvironment.java:55,96 | customizePropertySources() | **默认两个来源**: L97-98 addLast(PropertiesPropertySource(systemProperties)) + L99-100 addLast(SystemEnvironmentPropertySource(systemEnvironment)) — Java 系统属性优先于 OS 环境变量 | High |
| MutablePropertySources.java:41,43,104 | 有序容器 | **来源列表**: propertySourceList=CopyOnWriteArrayList — addFirst(L104 最高优先级)/addLast — 迭代顺序=优先级顺序 | High |
| PropertySourcesPropertyResolver.java:32,78 | getProperty() | **查找链**: L80 遍历 propertySources → 首个 getProperty 非 null 即返回 → resolveNestedPlaceholders(嵌套 ${}) → convertValueIfNecessary(C-2 转换) — 找不到返回 null(非抛错) | High |
| AbstractPropertyResolver.java:279 | resolveNestedPlaceholders() | **嵌套解析入口**: resolvePlaceholders(宽松) / resolveRequiredPlaceholders(严格, 未解析抛 IllegalArgumentException) | High |
| PlaceholderParser.java:62,122 | 解析器 | **占位符解析**: replacePlaceholders: parse(切分 Part 列表: 文本/占位符, 支持转义 \\${) → ParsedValue.resolve(PartResolutionContext) → 递归解析(L154) — Spring 6.2 重构的解析器 | High |
| PropertySource.java:62 | 抽象 | **来源抽象**: getName+getProperty(key)→value — 每种来源一种子类(Properties/SystemEnvironment/Resource 等) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+容器+查找+默认来源+解析器约 2300 行 — 但使用路径单线: "@Value → PropertyResolver.getProperty → PropertySources 遍历 → PlaceholderParser 递归". 1篇 (~47行) 按"接口→来源→查找→解析"展开; 若分 2 篇则"来源组织"与"查找算法"割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | PropertySourcesPropertyResolver.getProperty 查找链 (遍历首个非 null → 嵌套占位符 → 转换) | 🔴 | **为什么🔴**: 所有 @Value/${} 取值的核心算法 — "首个命中即返回"的优先级语义决定配置覆盖 |
| P1-2 | MutablePropertySources 有序容器 (CopyOnWriteArrayList + addFirst/addLast 优先级) | 🔴 | **为什么🔴**: "多来源+优先级"的模型 — Boot 外部化配置的 17 级优先级就是多个 addFirst/addLast 的堆积 |
| P1-3 | PlaceholderParser (${} 切分/转义/递归/默认值) | 🔴 | **为什么🔴**: 占位符语法的完整语义 — 嵌套/转义/默认值是配置解析的高频坑 |
| P2-1 | StandardEnvironment 默认来源 (systemProperties 优先于 systemEnvironment) | 🟡 | **为什么🟡**: 开箱的两个来源及顺序 — 解释"-D 参数为何压过 OS 环境变量" |
| P2-2 | PropertyResolver/Environment 接口层次 (取值→+profile) | 🟡 | **为什么🟡**: 接口演进 — getProperty(key, Class) 与 C-2 转换衔接 |
| P3-1 | PropertySource 抽象 (name+getProperty 契约) | 🟢 | **为什么🟢**: 自定义来源(如配置中心)的扩展点 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **来源与优先级** (PropertySource 抽象 + MutablePropertySources + 默认来源) | 🔴 | 配置"从哪来、谁优先"的模型 |
| B | **查找与解析** (getProperty 链 + PlaceholderParser) | 🔴 | 取值的算法核心 — 遍历+递归 |
| C | **接口层次** (PropertyResolver→Environment) | 🟡 | 面向客户的接口演进 |

> **Cluster A (§1)**: 接口层次(PropertyResolver/Environment/ConfigurableEnvironment) + PropertySource 抽象 + MutablePropertySources 容器(addFirst/addLast)
> **Cluster B (§2)**: PropertySourcesPropertyResolver.getProperty 查找链 + StandardEnvironment 默认来源 + 嵌套占位符
> **Cluster C (§3)**: PlaceholderParser 解析语义(转义/递归/默认值) + 与 @Value 的衔接 (context 侧 PropertySourcesPlaceholderConfigurer)

→ 引出 0-4: Ordered — 上节 addFirst/addLast 只是手动排优先级 — @Order/Ordered/PriorityOrdered 与 AnnotationAwareOrderComparator 是框架级统一排序

(End of file - total 61 lines)
