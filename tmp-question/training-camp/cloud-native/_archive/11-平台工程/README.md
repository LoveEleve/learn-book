# 11 平台工程（Week 24 · 5课时）

## 学完能干什么
理解网关四合一设计、安全 RBAC + CSRF/CSP、JMX 三种注册方式、JVM 生产排障。

## 课时 60：Gateway 四合一

**📖 读**: `stage-1/docs/19.` + `20.`

**四种网关**：服务发现网关 / 分布式配置网关 / API 网关 / RPC 网关

**Spring Cloud Gateway 核心**：Route = ID + URI + Predicates（匹配）+ Filters（拦截）

**RPC 网关创新**：HTTP → Dubbo 泛化调用，省掉 REST 中间层

## 课时 61：安全架构

**📖 读**: segfault-lessons/spring-boot/lesson-15/（WebSecurityConfiguration 完整源码）

**CSRF**: CookieCsrfTokenRepository + POST only
**CSP**: contentSecurityPolicy("script-src https://code.jquery.com/")
**X-Frame-Options**: AllowFromStrategy 白名单
**XSS**: xssProtection().block(false)

**OAuth2/JWT 补充**（`APPENDIX/GAP-FILL.md`）：
```java
.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
```

## 课时 62：JMX 三种风格

**📖 读**: segfault-lessons/spring-boot/lesson-17/

| 风格 | 方式 | 场景 |
|------|------|------|
| @ManagedResource | Spring 注解，最简单 | Spring 托管 Bean |
| 标准 MBean | 实现 XxxMBean 接口 | 类型安全，编译期校验 |
| 动态 MBean | 实现 DynamicMBean 接口 | 运行时动态暴露 |

**标准 MBean 通知机制**：`AttributeChangeNotification` + `NotificationBroadcasterSupport`

## 课时 63：API 文档自动化

**Doclet**（JDK 原生）→ **Spring REST Docs**（测试即文档）→ **Swagger/OpenAPI**（注解驱动）

**Dubbo 文档化痛点**：Swagger 天然支持 REST，对 RPC 协议无用。microsphere API Docs 通过 Dubbo Metadata + Swagger 桥接解决。

## 课时 64：JVM 生产排障

**📖 读**: `public/docs/生产环境JVM故障分析/`（3 篇实战案例）

**案例一**：ThreadPoolExecutor 未 shutdown → core thread 永久 WAITING → OOM Killer
**案例二**：SimpleAsyncTaskExecutor 创建 15万+线程 → Logback ReentrantLock 竞争 → JVM Crash
**案例三**：线程池拒绝策略（CallerRunsPolicy vs AbortPolicy）

**四件套**：JMap (jmap -histo:live) → JStack (fastthread.io) → JFR (-XX:StartFlightRecording) → MAT (Dominator Tree)

## 本阶段自检清单
- [ ] 能区分四种网关的职责
- [ ] 能配置 CSRF/CSP/X-Frame-Options/XSS 防护
- [ ] 知道 JMX 三种风格的适用场景
- [ ] 能用 JStack 定位一次死锁
