# S-8 嵌入式容器 — ServletWebServerFactory (容器选择与生命周期衔接)

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | ServletWebServerFactory+ServletWebServerFactoryAutoConfiguration+ServletWebServerFactoryConfiguration+ServletWebServerApplicationContext+Tomcat/Jetty/UndertowServletWebServerFactory
> 基线: BOOT-PLAN-v2 S-8 — **复用≠省略典型**: 引用 **Tomcat t7 (getWebServer 组装/Starter/配置映射已深度覆盖)** — 本域展开增量: 工厂抽象层次/三容器对照/生命周期衔接

---

## §0.8

- 🔴 Deep，1篇 — 工厂抽象(WebServerFactory→ServletWebServerFactory: getWebServer(initializers)) → 容器选择(ServletWebServerFactoryConfiguration: @ConditionalOnClass 三选一 Tomcat/Jetty/Undertow) → 自动装配(ServletWebServerFactoryAutoConfiguration: @Import 激活 + ServletWebServerFactoryCustomizer 应用 ServerProperties) → 生命周期(ServletWebServerApplicationContext.onRefresh→createWebServer→factory.getWebServer — **组装细节引用 t7**) → 三容器对照(增量)
- 设计模式: [模式: 工厂]—WebServerFactory 层次; [模式: 条件装配]—按 classpath 选容器; [模式: 定制器]—Customizer 应用配置

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ServletWebServerFactory.java:31,43 | 接口 | **工厂抽象**: getWebServer(ServletContextInitializer...)(L43) — 返回 WebServer 抽象(TomcatWebServer 等) | High |
| ServletWebServerFactoryConfiguration.java:61,64,69 | 容器选择 | **三选一**: Tomcat(@ConditionalOnClass(Tomcat/UpgradeProtocol) L64→@Bean L69)/Jetty(L86)/Undertow — classpath 决定容器 | High |
| ServletWebServerFactoryAutoConfiguration.java:74,77 | 自动装配 | **@AutoConfiguration L74**: @Import(BeanPostProcessorsRegistrar+FactoryConfiguration) → servletWebServerFactoryCustomizer L77(ServerProperties→工厂定制) | High |
| ServletWebServerApplicationContext.java:164,167 | 生命周期 | **onRefresh L164 → createWebServer L167**: refresh 时创建 WebServer — 工厂组装细节见 t7 | High |
| TomcatServletWebServerFactoryCustomizer.java:89 | Tomcat 定制 | **TomcatCustomizer L89**: @ConditionalOnClass(Tomcat) — server.tomcat.* 应用(优雅关闭等) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 工厂抽象+选择+装配+生命周期约 600 行核心 — 知识主线: "工厂抽象 → 按 classpath 选容器 → 自动装配激活 → refresh 衔接 → 三容器对照". 1篇 (🔴 ~50行) 按"抽象→选择→装配衔接→对照"展开; **Tomcat 组装细节引用 t7(06 §2.5), Jetty/Undertow 对照是本域增量**。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ServletWebServerFactory 抽象 (getWebServer→WebServer) | 🔴 | **为什么🔴**: 容器可替换的基础 — WebServer 统一抽象 |
| P1-2 | 三容器条件选择 (@ConditionalOnClass 三选一) | 🔴 | **为什么🔴**: 换容器=换依赖 — classpath 驱动的装配 |
| P1-3 | 生命周期衔接 (onRefresh→createWebServer→getWebServer) | 🔴 | **为什么🔴**: 容器在 refresh 何时创建 — 与 t7 组装衔接点 |
| P2-1 | ServletWebServerFactoryAutoConfiguration (激活+Customizer) | 🟡 | **为什么🟡**: 自动装配入口 + ServerProperties 应用 |
| P2-2 | 三容器对照 (Tomcat vs Jetty vs Undertow) | 🟡 | **为什么🟡**: 本域增量 — t7 未覆盖 |
| P3-1 | ServerProperties (server.port/server.tomcat.*) | 🟢 | **为什么🟢**: 配置面(S-5 绑定) |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **工厂抽象与选择** (接口 + 三条件) | 🔴 | 容器怎么抽象/选 |
| B | **装配与生命周期** (AutoConfiguration + onRefresh) | 🔴 | 何时/如何激活创建 |
| C | **对照与配置** (三容器 + ServerProperties) | 🟡 | 选型与定制 |

> **Cluster A (§1)**: ServletWebServerFactory 接口(getWebServer) + ServletWebServerFactoryConfiguration(三条件选容器)
> **Cluster B (§2)**: ServletWebServerFactoryAutoConfiguration(@Import+Customizer) + ServletWebServerApplicationContext.onRefresh→createWebServer — **组装细节引用 t7**
> **Cluster C (§3)**: 三容器工厂对照(线程模型/配置面差异) + ServerProperties + 切换机制(换 starter 依赖)

→ 引出 S-9: HTTP客户端+消息转换 — 容器就绪后, RestClient/HttpMessageConverters/JacksonAutoConfiguration — Web 层最后一块

(End of file - total 61 lines)
