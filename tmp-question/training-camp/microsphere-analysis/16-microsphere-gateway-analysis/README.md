# 16 - microsphere-gateway 深度分析

## 源码信息

- **本地路径**: `/data/workspace/java-training-camp/cloud-native-code/stage-4/microsphere-gateway`
- **GitHub**: https://github.com/microsphere-projects/microsphere-gateway
- **Stars**: 4
- **规模**: 28 个主 Java 文件 + 2 个父 POM，5 个 Maven 模块（parent / dependencies / commons / webflux / webmvc）
- **验证基础**: SCG 4.1.2 源码（`/data/tmp/opencode/spring-cloud-gateway-src`）+ 本机 jar 字节码 + git 全历史 352 提交 + 依赖模块（03/04/05）源码

## 文章索引

| 编号 | 文章 | 内容 |
|------|------|------|
| 16-01 | [项目定位与模块结构](16-01-project-analysis.md) | 定义（端点级路由）、模块结构、依赖链、SPI 三通道、开关体系、与官方 SCG 九维对比、项目活性 |
| 16-02 | [演进史](16-02-evolution-history.md) | git 全历史；2024 原始设计（自研 LB/谓词插件/context-path）vs 2025-10-31 重构；6 项能力丢失清单 |
| 16-03 | [WebFlux 请求链路](16-03-webflux-request-flow.md) | 过滤器序 10149、10 步处理、双缓存、两级匹配、P1-P5 问题、静默 200 行为（NettyRoutingFilter 源码） |
| 16-04 | [MVC 请求链路](16-04-webmvc-request-flow.md) | HandlerSupplier SPI 机制、lb() 委托、双守卫、与 WebFlux 八维对比、excludes 前缀差异 |
| 16-05 | [刷新机制与事件链](16-05-refresh-event-chain.md) | RouteRefreshListener 5 类事件、拦截器精确范围、启动时序链（通）、双通道、实例变更断链、四风险 |
| 16-06 | [配置绑定](16-06-config-binding.md) | 三层机制（Initializer/Advisor/onFinish）、自动生效前提（Boot 字节码验证）、默认值陷阱、静默失败缺口 |
| 16-07 | [跨进程元数据协议](16-07-metadata-protocol.md) | 九字段 JSON 协议、URL 编码、actuator 剔除、id 语义、协议隐患 H1-H3、与 03-07 边界 |
| 16-08 | [集成关系与缺口总表](16-08-integration-and-gaps.md) | 与 15/17/18 集成、Zone 真相（17 全局配置）、适用场景与部署、G1-G15 统一缺口表 |

## 核心结论（引用时直接用）

- **交接文档假设不成立**：16 没有网关层 Zone 路由（全历史仅 2 处 `TODO support ZonePreferenceFilter`）；网关 Zone 优先由 **17 的全局 LoadBalancer 配置**提供（需 4 个条件：引入 17 + `customized=true` + `optimized-zone-preference` + `ZoneContext.preferenceEnabled=true`）。
- 16 是 **we:// 端点级路由**：服务把端点集序列化进注册中心 metadata，网关自动发现、六维匹配、精确转发——官方 discovery.locator 只到服务级。
- **实例变更 → 路由刷新断链（G1）**：`ServiceInstancesChangedEvent` 全工作区无发布者 + 心跳被禁；测试都需手动 publish；与 17 的配置改 zone 断链同构。
- **三条高危（G1-G3）是生产前置条件**：发布者缺失、WebFlux 阻塞选实例（事件循环 `toFuture().get()`）、context-path 丢失（buildPath 死代码）。
- 学习价值：两代架构对比（自研 LB → SCG 生态，6 项能力丢失）、WebFlux/MVC 双实现对比、与官方 SCG 九维对比。
