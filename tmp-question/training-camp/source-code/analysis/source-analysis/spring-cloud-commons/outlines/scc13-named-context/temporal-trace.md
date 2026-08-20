# SCC-13 NamedContextFactory — 时空溯源 (issue 注释锚 + 消费方实证, git shallow)

> git shallow (单提交) 无法考古; 溯源以 github issue 注释 + 消费方代码为锚。

## 演进链 (代码痕迹实证)

| 阶段 | 事件 | 证据 |
|:--:|:--|:--|
| 早期 | **NamedContextFactory 诞生** (Spring Cloud Netflix/Feign 时期): 每客户端子上下文隔离 | NamedContextFactory.java:60 (抽象工厂) |
| issue#3101 (netflix) | **类加载器问题**: 子上下文 ClassLoader 错误导致 Bean 加载失败 | NamedContextFactory.java:162 注释 |
| issue#475 (openfeign) | **类加载器问题延续**: Feign 客户端子上下文 CL 修复 | NamedContextFactory.java:163 注释 |
| 修复 | **buildContext 用 parent 的 BeanClassLoader** (非线程上下文 CL) | L169-173 + testBadThreadContextClassLoader 回归测试 (Tests:63-77) |
| 3.x | **AOT 支持**: AotDetector.useGeneratedArtifacts 分支 (GenericApplicationContext vs AnnotationConfig) | L174-181 |
| 当前 | **applicationContextInitializers 支持** (AOT 初始化器 + LoadBalancerChildContextInitializer 消费) | L63/L133-137 |

## 消费方实证 (跨域)

- **LoadBalancerClientFactory extends NamedContextFactory** (loadbalancer/support/LoadBalancerClientFactory.java:46) — SCC-6 面
- **FeignClientFactory** (spring-cloud-openfeign 仓库, 阶段 5.6) — Feign 每客户端隔离
- **LoadBalancerClientSpecification implements Specification** (loadbalancer/annotation/) — 声明契约实现

## 版本相关性结论

- **子上下文隔离是 Spring Cloud 配置隔离的基石** — LoadBalancer/Feign 两个最大消费方都基于它
- **类加载器修复 (netflix#3101/openfeign#475) 是长期痛点** — 从 Netflix 到 OpenFeign 两代仓库都踩过, 修复后加回归测试
- **AOT 支持是 3.x 新增** — GraalVM 原生镜像场景需要 GenericApplicationContext (无法用 AnnotationConfig 反射)
- **Specification 契约稳定** — getName/getConfiguration 从诞生至今未变 (消费方多)
