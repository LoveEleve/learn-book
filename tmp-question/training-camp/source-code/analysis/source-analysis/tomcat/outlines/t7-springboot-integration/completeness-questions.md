# T-7 Spring Boot 集成 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

> 格式升级: 旧 5 问 → 新 A/B/C/D 四节 20 问 (2026-08-17 查漏补缺)
> 锚点源: spring-boot 仓库 TomcatServletWebServerFactory.java (1032 行)

### A. 机制理解 (5)
1. getWebServer() 一行代码创建了哪些组件? 调用链?
2. `server.tomcat.threads.max=200` 怎么到达 AbstractEndpoint.setMaxThreads()? 中间经过了哪几层?
3. Customizer 为什么设计成三层 (properties/Customizer/subclass)? 各层职责?
4. TomcatStarter 如何绕过 SCI 的 @HandlesTypes 扫描? 为什么需要绕过?
5. GracefulShutdown 是怎样的关闭流程? 和 Tomcat 原生关闭有什么区别?

### B. 源码实证 (6)
6. getWebServer 方法签名? (grep TomcatServletWebServerFactory.java:196)
7. connector 的 addConnector 调用点? (grep TomcatServletWebServerFactory.java:208)
8. TomcatStarter 的创建位置? (grep TomcatServletWebServerFactory.java:404)
9. configureContext 方法签名与调用点? (grep TomcatServletWebServerFactory.java:269/403)
10. addConnectorCustomizers/addAdditionalTomcatConnectors 方法位置? (grep TomcatServletWebServerFactory.java:687/742)
11. TomcatServletWebServerFactory 的类声明与继承链? (grep TomcatServletWebServerFactory.java:116-117)

### C. 推理深挖 (5)
12. Spring Boot 为什么不用 Tomcat 的 server.xml? 编程式配置的利弊?
13. WebServerFactoryCustomizer 的执行时机 — 在 getWebServer() 之前还是之后? 多个 Customizer 顺序?
14. 嵌入式 Tomcat vs 独立 Tomcat — 生命周期谁管理? 关闭钩子怎么注册?
15. GracefulShutdown 的等待超时 — 超时后强制关闭? 长请求怎么处理?
16. 端口 0 启动 (随机端口) — 测试场景怎么拿实际端口?

### D. 跨域扩展 (4)
17. 本域 vs T-1 Lifecycle: 嵌入式 Tomcat 的 Lifecycle 由谁触发? Spring 容器 vs Tomcat 自身?
18. 本域 vs T-4 线程模型: server.tomcat.threads.max 与 AbstractEndpoint 的绑定链?
19. 本域 vs Nacos: Nacos 内置 Web 服务器 (Jetty) 的集成 vs Spring Boot Tomcat 集成对照?
20. 本域 vs openjdk: Spring Boot 的自动装配 (AutoConfiguration) 机制 vs 条件注解 — 是设计模式还是框架约定?