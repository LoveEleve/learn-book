# S-1 @SpringBootApplication — 组合注解拆解 (配置+自动装配+扫描 三层)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | SpringBootApplication(156行)+SpringBootConfiguration(76行)+EnableAutoConfiguration(111行)+AutoConfigurationPackage(65行)+AutoConfigurationPackages(224行)+AutoConfigurationExcludeFilter(72行)+TypeExcludeFilter(约120行)
> 基线: BOOT-PLAN-v2 S-1 — Boot 启动的一切从主类注解开始; 前置: C-5 注解元数据/s9 @Configuration/s23 @Import/C-6 条件 — 组合注解机制复用 C-5, 三层职责展开

---

## §0.8

- 🟡 Working，1篇 — 组合拆解(@SpringBootApplication = @SpringBootConfiguration(@Configuration+@Indexed) + @EnableAutoConfiguration(@AutoConfigurationPackage+@Import(ImportSelector)) + @ComponentScan(excludeFilters 双过滤器)) → 包注册(@AutoConfigurationPackage→AutoConfigurationPackages.Registrar: 记录主类包供自动装配扫描) → 扫描定制(exclude/scanBasePackages/TypeExcludeFilter/AutoConfigurationExcludeFilter) → 参数转发(注解属性→三层)
- 设计模式: [模式: 组合注解]—C-5 元注解机制; [模式: ImportSelector 桥]—@EnableAutoConfiguration 引向 S-2; [模式: 排除过滤器]—TypeFilter 定制扫描

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SpringBootApplication.java:50,58 | 注解 | **组合**: L50-53 元注解(@Target/@Retention/@Documented/@Inherited) + L54 @SpringBootConfiguration + L55 @EnableAutoConfiguration + L56-57 @ComponentScan(excludeFilters) — 一键替代三个注解 | High |
| SpringBootApplication.java:71,95 | 参数 | **属性转发**: exclude(L71)/excludeName(L80)/scanBasePackages(L95)/scanBasePackageClasses — 转发给 @EnableAutoConfiguration/@ComponentScan | High |
| SpringBootConfiguration.java:47,49 | 配置层 | **@SpringBootConfiguration**: @Configuration(L47)+@Indexed(L48) — 主配置类; @Indexed 让组件索引(C-10)可发现 | High |
| EnableAutoConfiguration.java:81,83 | 自动装配层 | **@EnableAutoConfiguration**: @AutoConfigurationPackage(L81) + @Import(AutoConfigurationImportSelector.class)(L82) — **S-2 入口** | High |
| AutoConfigurationPackage.java:41,42 | 包注册 | **@AutoConfigurationPackage**: @Import(AutoConfigurationPackages.Registrar.class) — 记录主类所在包 | High |
| AutoConfigurationPackages.java:93,128 | 注册/读取 | **Registrar.register(L128)→register(L93)**: 存 BasePackages bean; get(L73) 供自动装配读主包 | High |
| AutoConfigurationExcludeFilter.java:36,48 | 排除 | **AutoConfigurationExcludeFilter**: TypeFilter — match L48 排除自动配置类(防重复扫描) | High |
| TypeExcludeFilter.java:50,62 | 排除 | **TypeExcludeFilter**: match L62 委托 delegates — 用户可注册自定义排除器 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 主注解+三个组成注解+注册器+过滤器约 900 行 — 知识单线: "@SpringBootApplication 拆成三层 → 每层做什么 → 参数怎么转发". 1篇 (~46行) 按"组合拆解→自动装配层→扫描定制"展开; S-2 展开 ImportSelector 机制本身。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @SpringBootApplication 组合拆解 (配置/自动装配/扫描 三层) | 🔴 | **为什么🔴**: Boot 主类的唯一入口 — 三层的职责划分是理解一切的基础 |
| P1-2 | @EnableAutoConfiguration (@AutoConfigurationPackage 包注册 + @Import(ImportSelector)) | 🔴 | **为什么🔴**: 自动装配的启动开关 — 主类包怎么被记录、ImportSelector 怎么引入 S-2 |
| P1-3 | @ComponentScan 定制 (excludeFilters 双过滤器) | 🔴 | **为什么🔴**: 扫描的排除机制 — TypeExcludeFilter 扩展点 + AutoConfigurationExcludeFilter 防重复 |
| P2-1 | @SpringBootConfiguration (@Configuration+@Indexed) | 🟡 | **为什么🟡**: 与普通 @Configuration 差异 — 主配置类可被索引发现 |
| P2-2 | 属性转发 (exclude/scanBasePackages → 各层) | 🟡 | **为什么🟡**: 注解参数怎么到三层 |
| P3-1 | AutoConfigurationPackages (register/get bean) | 🟢 | **为什么🟢**: 主包记录机制 — 供 S-2 自动装配扫描 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **组合拆解** (三层职责) | 🔴 | 主注解的结构理解 |
| B | **自动装配开关** (@EnableAutoConfiguration 包注册+导入) | 🔴 | S-2 的入口 |
| C | **扫描定制** (excludeFilters/属性转发) | 🟡 | 扫描细节 |

> **Cluster A (§1)**: @SpringBootApplication 元注解组合 + 属性转发
> **Cluster B (§2)**: @SpringBootConfiguration + @EnableAutoConfiguration(@AutoConfigurationPackage/ImportSelector) — 引 S-2
> **Cluster C (§3)**: @ComponentScan excludeFilters(双过滤器) + 与普通 @Configuration 应用差异

→ 引出 S-2: 自动装配加载机制 — @EnableAutoConfiguration 的 @Import(AutoConfigurationImportSelector.class) 到底做了什么: imports 文件读取/过滤/排序/条件评估

(End of file - total 61 lines)
