# S-7 MVC 自动装配 — WebMvcAutoConfiguration (DispatcherServlet 家族的装配)

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | WebMvcAutoConfiguration(约550行)+EnableWebMvcConfiguration+DelegatingWebMvcConfiguration(spring-webmvc)+WebMvcProperties
> 基线: BOOT-PLAN-v2 S-7 — starter-web 触发; 前置: **W-1 DispatcherServlet/W-2 HandlerMapping/W-3 Adapter/W-4 Resolver/W-5 MessageConverter (全部复用 — 机制已讲, 本域讲"装配")**

---

## §0.8

- 🔴 Deep，1篇 — 条件与入口(@AutoConfiguration(after=DispatcherServlet) + @ConditionalOnClass + @ConditionalOnMissingBean(WebMvcConfigurationSupport): 用户自定义 MVC 则跳过) → 装配核心(EnableWebMvcConfiguration extends DelegatingWebMvcConfiguration: 创建 W-1~W-4 的全部核心 Bean + 聚合 WebMvcConfigurer) → 适配器(WebMvcAutoConfigurationAdapter: WebMvcProperties 属性 + 转换器/拦截器/静态资源定制) → 与 W 系列复用
- 设计模式: [模式: 自动装配+条件]—S-2/S-3 机制复用; [模式: 委托配置]—DelegatingWebMvcConfiguration 聚合 WebMvcConfigurer; [模式: 门面]—WebMvcAutoConfigurationAdapter

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| WebMvcAutoConfiguration.java:144,147,148,151 | 条件与顺序 | **装配声明**: @AutoConfiguration(after=DispatcherServletAutoConfiguration 等, L144) + @ConditionalOnClass(Servlet/DispatcherServlet/WebMvcConfigurer, L147) + @ConditionalOnMissingBean(WebMvcConfigurationSupport, L148 — **用户自定义 MVC 则整体跳过**) | High |
| WebMvcAutoConfiguration.java:182,185 | 适配器 | **WebMvcAutoConfigurationAdapter L185**: @Import(EnableWebMvcConfiguration.class)(L182) + @EnableConfigurationProperties(WebMvcProperties/WebProperties)(L183) — implements WebMvcConfigurer | High |
| WebMvcAutoConfiguration.java:386,412 | 装配核心 | **EnableWebMvcConfiguration L386** extends DelegatingWebMvcConfiguration — createRequestMappingHandlerAdapter L412(覆写: 响应结果/同步异步) — 创建 W-1~W-4 全部 Bean | High |
| DelegatingWebMvcConfiguration(spring-webmvc) | 委托 | **DelegatingWebMvcConfiguration**: 收集容器全部 WebMvcConfigurer bean → 创建 RequestMappingHandlerMapping/Adapter/ArgumentResolvers/MessageConverters 等核心 Bean — W 系列机制的 Bean 来源 | High |
| WebMvcAutoConfiguration.java:195,225 | 转换器 | **messageConvertersProvider L195**: HttpMessageConverters(用户定制)+默认 → configureMessageConverters L225 — W-5 的转换器装配 | High |
| WebMvcProperties.java | 属性 | **WebMvcProperties**: spring.mvc.* 配置(视图/路径匹配/格式化等) — S-5 绑定 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 自动装配类+委托配置约 700 行 — 知识主线: "WebMvcAutoConfiguration 条件激活 → EnableWebMvcConfiguration 装配 W 系列 Bean → 适配器定制". 1篇 (🔴 ~50行) 按"条件→装配→定制"展开; **W-1~W-5 机制全部复用(06 §2.5), 本域只讲"这些 Bean 怎么被自动装配"**。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 装配条件与顺序 (@ConditionalOnMissingBean(WebMvcConfigurationSupport) 跳过语义) | 🔴 | **为什么🔴**: "用户自定义则跳过"是自动装配的核心原则 — @EnableWebMvc 关闭自动装配 |
| P1-2 | EnableWebMvcConfiguration/DelegatingWebMvcConfiguration (W 系列 Bean 的装配) | 🔴 | **为什么🔴**: W-1~W-4 的 DispatcherServlet/HandlerMapping/Adapter/Resolver Bean 从哪来 — 机制与装配的分界 |
| P1-3 | WebMvcAutoConfigurationAdapter (属性+转换器+定制入口) | 🔴 | **为什么🔴**: spring.mvc.* 属性怎么生效、WebMvcConfigurer 怎么接入 |
| P2-1 | HttpMessageConverters 装配 (用户定制+默认合并) | 🟡 | **为什么🟡**: W-5 转换器链的 Boot 装配 |
| P2-2 | 与 @EnableWebMvc 对照 (自动 vs 手动) | 🟡 | **为什么🟡**: 两条路径的选择 |
| P3-1 | 静态资源/视图配置 (WebMvcProperties) | 🟢 | **为什么🟢**: spring.mvc 属性面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **条件与入口** (自动装配声明) | 🔴 | 何时激活/跳过 |
| B | **核心装配** (EnableWebMvcConfiguration) | 🔴 | W 系列 Bean 来源 |
| C | **定制与属性** (Adapter + Properties) | 🟡 | 用户接入点 |

> **Cluster A (§1)**: @AutoConfiguration(after=DispatcherServlet) + 双条件(OnClass/OnMissingBean) — 用户自定义则跳过
> **Cluster B (§2)**: EnableWebMvcConfiguration(createRequestMappingHandlerAdapter L412) + DelegatingWebMvcConfiguration(聚合 WebMvcConfigurer, 建 W 系列 Bean)
> **Cluster C (§3)**: WebMvcAutoConfigurationAdapter(WebMvcProperties/转换器/拦截器) + @EnableWebMvc 对照

→ 引出 S-8: 嵌入式容器 — WebMvcAutoConfiguration 依赖 DispatcherServletAutoConfiguration, 而它又依赖嵌入式服务器 — ServletWebServerFactory 与 refresh 衔接 (复用 t7)

(End of file - total 61 lines)
