# T-6 ClassLoader 20 问 (A 机制理解 5 / B 源码实证 6 / C 推理深挖 5 / D 跨域扩展 4)

> 格式升级: 旧 3 问 → 新 A/B/C/D 四节 20 问 (2026-08-17 查漏补缺)

### A. 机制理解 (5)
1. delegate=false 和 delegate=true 有什么区别? 哪个先加载本地类?
2. 双亲委派是什么? Web 应用为什么要打破它 (本地优先)?
3. 为什么 WebappClassLoader 对 jakarta.servlet.* 永远先从父加载? 应用能覆盖 Servlet API 吗?
4. Tomcat 的类加载器层级 (common/catalina/shared/webapp) 各加载什么? 谁是谁的父?
5. 类加载器泄漏 (ClassLoader leak) 是什么? 热部署为什么要解决它?

### B. 源码实证 (6)
6. delegate 字段声明与默认值? (grep catalina/loader/WebappClassLoaderBase.java:281)
7. jakarta 前缀的过滤逻辑位置? (grep WebappClassLoaderBase.java:2481-2497)
8. findClassInternal 的本地查找入口? (grep WebappClassLoaderBase.java:152)
9. loadClass 的委派逻辑 (delegate 分支)? (grep WebappClassLoaderBase.java loadClass)
10. 缓存机制 (notFoundClassResources/resourceEntries)? (grep WebappClassLoaderBase.java:807/919)
11. WebappClassLoaderBase 的类声明与继承? (grep catalina/loader/WebappClassLoaderBase.java)

### C. 推理深挖 (5)
12. 为什么 Filter 名单不允许应用覆盖 jakarta.servlet.*? 覆盖了会出什么问题?
13. JSP 类的加载 — 为什么需要独立的 JspClassLoader? 与 WebappClassLoader 什么关系?
14. 热部署时旧类加载器怎么释放? 什么情况下会泄漏 (static 持有)?
15. 并行能力 (parallelCapable) — Tomcat 的 WebappClassLoader 支持并行加载吗? 为什么?
16. delegate=true 与 false 在 Spring Boot fat jar 场景下的取舍?

### D. 跨域扩展 (4)
17. 本域 vs openjdk: JDK 9+ 的模块化类加载 vs 双亲委派 — Tomcat 怎么适配?
18. 本域 vs Nacos: Nacos 的 SPI 类加载 vs Tomcat WebappClassLoader — 都是自定义加载策略?
19. 本域 vs Spring Boot: Spring Boot 的 LaunchedURLClassLoader vs Tomcat WebappClassLoader — fat jar 加载差异?
20. 本域 vs T-3 Filter: 类加载顺序对 Filter 链实例化的影响 — 过滤器类由谁加载?