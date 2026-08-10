# T-7 Spring Boot 集成 — TomcatServletWebServerFactory 全链路

> 项目: Spring Boot 3.5.x + Tomcat 10.1.x | 🟡B 域 / 2 篇 | TomcatServletWebServerFactory(1032行)
> 基线: T-1~T-6 全部 Tomcat 内核 — T-7 回答 "Spring Boot 如何把 Tomcat 组装起来"

---

## §0.8 域审核

- 这是 **跨框架集成域** — 不是纯 Tomcat 源码域 — 含 Spring Boot 源码(1032行 Factory)
- 之前分析覆盖: 09-springboot-integration.md(46K字) — 确认此域不冗余，但需压缩为我们 v5 的大纲密度
- 淘汰检查: 嵌入式入口已经是当代主流 — 独立 XML 配置方式不提

---

## 01 提取 + 02-04 聚合/分类/聚类

### TomcatServletWebServerFactory.java (1032 行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| TomcatServletWebServerFactory.java:196 | **getWebServer()**: 创建 Tomcat → Connector → Context → 装配全组件 | High |
| TomcatServletWebServerFactory.java:235 | **prepareContext()**: 设置 Context(classLoader/docBase/sessionTimeout) | High |
| TomcatServletWebServerFactory.java:269 | **configureContext()**: 注册 ServletContextInitializer | High |

### 聚类

**§1: TomcatServletWebServerFactory — Spring Boot 的总装车间**
- getWebServer(): Tomcat tomcat = new Tomcat() → Connector → Service → Engine → Host → Context
- prepareContext: Context 配置(classLoader/delegate/docBase)
- configureContext: TomcatStarter 桥接 Spring 和 Servlet 规范
- 对应之前分析的 T-1~T-6 全部域 — 在这里串联为一条调用链

**§2: Customizer 三层 + 配置映射**
- WebServerFactoryCustomizer: 程序化定制(pipeline.addValve/connectorCustomizers)
- server.tomcat.* → TomcatServletWebServerFactory 属性的自动映射
- GracefulShutdown: Spring Boot 3.x 的优雅关闭

> → Spring Stage 1 全部完成: Netty 13章 + Tomcat 7域。Stage 2: Spring Framework 核心容器。
