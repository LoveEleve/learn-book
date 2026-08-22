# Spring Boot 源码学习范围规划（缺陷修复版）

> 基准：Spring Boot 3.5.16 + Spring Framework 6.2.17
> 目标：在既有 24 个机制域基础上，重构成可连续阅读、可逐篇 review 的 `vol-spring-boot` 完整卷路线。
> 重要边界：不重复 `vol-spring` 已讲过的 Framework 原理；Boot 负责“如何把这些能力装配成可运行应用”。

## 一、修复结论

原规划已经完成了 Boot 核心包和自动配置包扫描，但存在三个结构问题：

1. 24 个机制域被压缩成“17 篇文章”，7 个重要域没有获得独立的阅读位置。
2. 规划缺少“为什么有了 Spring 还要 Spring Boot”的总开篇，读者只能直接进入类和方法。
3. 主干、集成、诊断、测试、补深主题虽然已经出现，但没有形成一条卷级阅读路径。

本修复版将路线调整为：

- 1 篇总开篇
- 14 篇主干层
- 5 篇诊断与生产层
- 2 篇测试层
- 5 篇补深层

共 **27 篇候选正文**。其中 B-8 HTTP 客户端与消息转换器、B-21 Actuator 可在后续 review 中继续拆分，但不能因为文件多就压缩成一篇泛讲。

## 二、卷级路线

### A. 总开篇

A-1. 为什么有了 Spring，还要 Spring Boot

回答：Spring Framework 已经解决了容器、AOP、事务、MVC，Boot 到底新增了什么价值？重点建立“能力框架”与“应用装配系统”的区别，并桥接到 `SpringApplication.run()`。

### B. 主干层

B-1. `@SpringBootApplication` 与应用定义入口
B-2. 自动配置导入总链：`@EnableAutoConfiguration` 与 `AutoConfigurationImportSelector`
B-3. Boot 条件注解体系
B-4. `SpringApplication.run()` 启动流程
B-5. `@ConfigurationProperties` 与类型安全绑定
B-6. Starter 与自动配置发布机制
B-7. Web MVC 自动装配
B-8. 嵌入式 Servlet 容器自动装配
B-9. HTTP 客户端与消息转换器自动装配
B-10. DataSource/JDBC 自动配置
B-11. Redis 自动配置
B-12. 事务与持久化异常翻译自动配置
B-13. 缓存自动配置
B-14. TaskExecutor / TaskScheduler 自动配置
B-15. AOT / Native Image 启动处理

### C. 诊断与生产层

C-1. FailureAnalyzers 启动失败诊断
C-2. ConfigData 与 `EnvironmentPostProcessor`
C-3. 日志系统自动配置
C-4. ApplicationAvailability、Liveness 与 Readiness
C-5. Actuator 端点、Health 与 Metrics

### D. 测试层

D-1. Spring Boot 测试上下文与 `@SpringBootTest`
D-2. 测试切片、MockBean、MockMvc 与自动配置裁剪

Validation 自动配置作为 Web / 测试交叉主题保留，不能从路线中丢失；必要时单独拆为 D-3。

### E. 补深层

E-1. 虚拟线程与 `OnThreadingCondition`
E-2. WebFlux / Reactive 自动配置
E-3. Elasticsearch 自动配置
E-4. Boot Validation 自动配置深挖
E-5. Boot 上下文扩展：BootstrapRegistry、Initializer、Builder、系统资源与 Banner

## 三、和 `vol-spring` 的边界

`vol-spring` 负责：

- `refresh()` 容器激活骨架
- 配置类解析与 `@Import`
- `@Conditional` 基础接口和评估阶段
- `DispatcherServlet.doDispatch()`
- AOP、事务、缓存、TaskExecutor 的 Framework 原理
- Servlet / Tomcat 的基础规范与容器实现

`vol-spring-boot` 负责：

- 启动入口如何选择并创建上下文
- 自动配置候选如何发现、排序、按条件裁剪
- 配置属性如何绑定成类型安全对象
- Framework 能力如何接入 Tomcat、HikariCP、Lettuce/Jedis 等设施
- 失败诊断、健康状态、指标、测试切片如何形成应用级默认能力

## 四、执行规则

1. 先 review 原规划、既有 `knowledge-planning` 和 `outlines`，再写正文。
2. 每篇正文必须明确属于哪个卷级层次，不能把 24 个域平铺成专题笔记。
3. 第一篇固定为 A-1，不直接从 `run()` 开始。
4. A-1 之后进入 B-1、B-2、B-3，再进入 B-4 启动流程；这样先立“为什么”，再讲“入口是什么”，最后讲“入口怎么跑”。
5. 先保持与 `vol-spring` 一致的机制叙事体；两卷铺完后统一补真实源码块、精确 `file:line` 和证据解释。
6. 每写完一篇先 review、修补，再继续下一篇；每完成 2~3 篇同步 README。

## 五、推荐首轮顺序

```text
A-1 为什么有了 Spring，还要 Spring Boot
  -> B-1 @SpringBootApplication 与应用定义入口
  -> B-2 自动配置导入总链
  -> B-3 Boot 条件注解体系
  -> B-4 SpringApplication.run() 启动流程
  -> B-5 ConfigurationProperties
  -> B-6 Starter
  -> B-7/B-8/B-9 Web 与 HTTP 装配
  -> B-10~B-14 数据、事务、缓存、异步
  -> B-15 AOT
  -> C/D/E 层
```
