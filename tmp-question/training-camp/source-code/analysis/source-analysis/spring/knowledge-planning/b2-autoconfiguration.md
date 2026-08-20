# S-2 自动装配加载机制 — AutoConfigurationImportSelector (imports→过滤→排序→注册)

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | AutoConfigurationImportSelector(约520行)+ImportCandidates+OnClassCondition+OnBeanCondition+OnPropertyCondition+ConfigurationClassFilter
> 基线: BOOT-PLAN-v2 S-2 ⭐新增 — Boot 灵魂 — @EnableAutoConfiguration 的 @Import 指向它; 前置: s23 @Import/DeferredImportSelector (机制复用 s23, 内容全展开)

---

## §0.8

- 🔴 Deep，1篇 — 时机(DeferredImportSelector: 所有 @Configuration 解析后统一处理 → getImportGroup→AutoConfigurationGroup.process) → 读取(getCandidateConfigurations: ImportCandidates.load 读 META-INF/spring/...AutoConfiguration.imports) → 过滤(getConfigurationClassFilter: OnClassCondition/OnBeanCondition/OnPropertyCondition 先按元数据粗筛 → AutoConfigurationImportEvent 条件评估精筛 + exclude) → 排序(sortAutoConfigurations: @AutoConfigureBefore/After/Order via 自动装配元数据) → 注册(返回配置类名列表→作为 @Configuration 导入)
- 设计模式: [模式: DeferredImportSelector]—延迟到业务配置之后; [模式: 责任链]—过滤链; [模式: 元数据驱动]—spring-autoconfigure-metadata.properties 免加载类粗筛

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AutoConfigurationImportSelector.java:77,153 | 入口 | **DeferredImportSelector**: L153 getImportGroup→AutoConfigurationGroup(L423) — 在用户 @Configuration 全部解析后执行(此时可评估 @ConditionalOnMissingBean) | High |
| AutoConfigurationImportSelector.java:195 | 读取 | **getCandidateConfigurations L195**: ImportCandidates.load(org.springframework.boot.autoconfigure.AutoConfiguration.imports) → getCandidates — 各 jar 的 imports 文件合并 | High |
| AutoConfigurationImportSelector.java:137,147 | 主流程 | **getAutoConfigurationEntry L137**: L142 读取 → L147 filter → L148 fireAutoConfigurationImportEvents(发布条件评估事件) | High |
| AutoConfigurationImportSelector.java:270,274 | 过滤链 | **getConfigurationClassFilter L274**: SpringFactoriesLoader 加载 AutoConfigurationImportFilter(OnClassCondition/OnBeanCondition/OnPropertyCondition) | High |
| AutoConfigurationImportSelector.java:491,503 | 排序 | **sortAutoConfigurations L491/503**: 用自动装配元数据(spring-autoconfigure-metadata.properties) 按 @AutoConfigureBefore/After/Order 排序 | High |
| OnClassCondition.java:44,47 | 条件 | **粗筛**: getOutcomes L47 — 按元数据(类存在性)批量判定, 线程化(L53) — 不加载类 | High |
| AutoConfigurationImportEvent(事件) | 精筛 | **精筛**: 过滤器粗筛后的候选发布事件 → 条件评估器按元数据+类加载判定 → 不匹配移除 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 加载器+过滤器+排序约 900 行 — 知识单线: "deferred 时机→读 imports→过滤→排序→注册". 1篇 (🔴 ~50行) 按"时机→读取→过滤→排序注册"展开; 这是 Boot 一切自动装配的总管线, 必须 🔴。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | DeferredImportSelector 时机 (业务配置后执行的意义) | 🔴 | **为什么🔴**: 时序决定正确性 — 自动装配晚于用户配置, @ConditionalOnMissingBean 才有效 |
| P1-2 | getCandidateConfigurations (imports 文件读取) | 🔴 | **为什么🔴**: 自动装配清单从哪来 — 各 jar 的 imports 文件是注册表 |
| P1-3 | 两级过滤 (元数据粗筛 OnClassCondition → 事件精筛条件评估) | 🔴 | **为什么🔴**: 性能+正确性 — 免加载类先筛, 再按完整条件精筛 |
| P2-1 | sortAutoConfigurations (@AutoConfigureBefore/After/Order) | 🟡 | **为什么🟡**: 自动装配顺序控制 — 依赖关系的声明式表达 |
| P2-2 | exclude/excludeName (排除机制) | 🟡 | **为什么🟡**: 关闭特定自动装配的标准方式 |
| P3-1 | 注册为 @Configuration (导入列表→配置类) | 🟢 | **为什么🟢**: 与 s9 @Configuration 汇合 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **时机与读取** (Deferred + imports) | 🔴 | 何时/从哪读自动装配清单 |
| B | **过滤链** (两级过滤) | 🔴 | 条件评估 — 性能与正确性 |
| C | **排序与注册** (Before/After/Order + 导入) | 🟡 | 顺序控制与落地 |

> **Cluster A (§1)**: DeferredImportSelector 时机 + ImportCandidates 读取 imports 文件
> **Cluster B (§2)**: getAutoConfigurationEntry 主流程(读取→过滤→事件) + OnClassCondition 元数据粗筛 + 条件评估精筛 + exclude
> **Cluster C (§3)**: sortAutoConfigurations 排序 + 注册为 @Configuration + 全管线收束

→ 引出 S-3: 条件注解 — 过滤链用的 OnClassCondition/OnBeanCondition/OnPropertyCondition 就是 @ConditionalOnXxx 的底层实现 — 展开条件评估细节

(End of file - total 61 lines)
