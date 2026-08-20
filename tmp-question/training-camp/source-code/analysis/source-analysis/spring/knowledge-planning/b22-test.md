# S-22 测试自动配置 — @WebMvcTest 切片 (@OverrideAutoConfiguration + @TypeExcludeFilters + @ImportAutoConfiguration)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | WebMvcTest.java(110行)+OverrideAutoConfigurationContextCustomizerFactory.java(73行)+TypeExcludeFiltersContextCustomizer.java(100行)+WebMvcTypeExcludeFilter.java(100行)+StandardAnnotationCustomizableTypeExcludeFilter.java(110行)+ImportAutoConfiguration.java(90行, boot-autoconfigure)
> 基线: BOOT-PLAN-v2 S-22 — 切片自动配置; 前置: **C-16~C-19(test 层, MockMvc/TestContext/MockBean/SQL — 复用) + S-2(自动装配)** — 展开切片机制

---

## §0.8

- 🟡 Working，1篇 — 切片注解组合(@WebMvcTest 复合注解: @BootstrapWith + @OverrideAutoConfiguration(enabled=false) + @TypeExcludeFilters(WebMvcTypeExcludeFilter) + @AutoConfigureMockMvc + @ImportAutoConfiguration) → 关闭全量自动装配(@OverrideAutoConfiguration→OverrideAutoConfigurationContextCustomizerFactory→DisableAutoConfigurationContextCustomizer: spring.boot.enableautoconfiguration=false, 只跑切片自动装配) → 类型过滤(@TypeExcludeFilters→TypeExcludeFiltersContextCustomizer 注册 TypeExcludeFilter, WebMvcTypeExcludeFilter 只留 Controller/ControllerAdvice/Filter 等) → 切片自动装配导入(@ImportAutoConfiguration→ImportAutoConfigurationImportSelector 导入切片专属配置)
- 设计模式: [模式: 复合注解]—@WebMvcTest; [模式: ContextCustomizer]—test 层钩子; [模式: 类型过滤]—TypeExcludeFilter; [模式: 选择性导入]—@ImportAutoConfiguration

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| WebMvcTest.java:101-109 | 切片注解 | **@BootstrapWith(L101)+@OverrideAutoConfiguration(enabled=false)(L103)+@TypeExcludeFilters(L104)+@AutoConfigureMockMvc(L107)+@ImportAutoConfiguration(L108)** | High |
| OverrideAutoConfigurationContextCustomizerFactory.java:40,47,54,58 | 关闭全量 | **createContextCustomizer(L40)**: enabled(L47)→DisableAutoConfigurationContextCustomizer(L54)→customizeContext(L58): spring.boot.enableautoconfiguration=false | High |
| TypeExcludeFiltersContextCustomizer.java:84,87 | 类型过滤 | **customizeContext(L84)**: registerSingleton TypeExcludeFilter(L87) | High |
| WebMvcTypeExcludeFilter.java:50,63,67,91 | 过滤内容 | **extends Standard...(L50)**: includes ControllerAdvice(L63)/Filter(L67)/Controller(L91) | High |
| ImportAutoConfiguration.java:50,51 | 导入 | **@Import(ImportAutoConfigurationImportSelector)(L50)+@interface(L51)**: classes()(L70) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 切片机制是一条线(注解→关全量→过滤→导入), 3 块耦合 — 1篇 (~44行) 按"切片注解 → 关闭全量+类型过滤 → 自动装配导入"展开; C-16 MockMvc 执行机制复用, 本域讲切片怎么搭上下文。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 切片注解组合 (@WebMvcTest 复合注解) | 🔴 | **为什么🔴**: 一个注解怎么定义整个切片 |
| P1-2 | 关闭全量自动装配 (OverrideAutoConfiguration→enableautoconfiguration=false) | 🔴 | **为什么🔴**: 切片为何关全量只跑局部 |
| P1-3 | 类型过滤 (TypeExcludeFilters + WebMvcTypeExcludeFilter) | 🔴 | **为什么🔴**: 只保留切片的 Bean |
| P2-1 | 切片自动装配导入 (@ImportAutoConfiguration) | 🟡 | **为什么🟡**: 切片专属配置怎么装 |
| P2-2 | 与 @SpringBootTest 对照 (切片 vs 全量) | 🟡 | **为什么🟡**: 差异 |
| P3-1 | 与 C-16 边界 (MockMvc 机制复用) | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **切片注解** | 🔴 | 一个注解定义切片 |
| B | **关全量+过滤** | 🔴 | 只留切片 Bean |
| C | **自动装配导入** | 🟡 | 切片配置怎么装 |

> **Cluster A (§1)**: @WebMvcTest 复合注解(meta-annotations)
> **Cluster B (§2)**: @OverrideAutoConfiguration→DisableAutoConfigurationContextCustomizer + @TypeExcludeFilters→WebMvcTypeExcludeFilter
> **Cluster C (§3)**: @ImportAutoConfiguration→ImportAutoConfigurationImportSelector + 与 @SpringBootTest 对照 + C-16 边界

→ 引出 S-23: Validation — 测试之后: ValidationAutoConfiguration 自动注册校验器(前置 C-22)
