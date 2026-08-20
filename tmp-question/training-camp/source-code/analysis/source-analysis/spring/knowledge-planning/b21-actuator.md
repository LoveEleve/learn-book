# S-21 Actuator 端点 — @Endpoint 注解 → EndpointDiscoverer → WebMvcEndpointHandlerMapping

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | Endpoint.java(90行)+ReadOperation.java(70行)+ExposableEndpoint.java(60行)+EndpointDiscoverer.java(280行)+WebEndpointDiscoverer.java(130行)+WebEndpointAutoConfiguration.java(160行)+AbstractWebMvcEndpointHandlerMapping.java(280行)+WebMvcEndpointHandlerMapping.java(130行)+HealthEndpoint.java(90行)
> 基线: BOOT-PLAN-v2 S-21 — 端点注册/暴露/健康检查; 前置: **C-12 拦截器 + C-13 异常 (Web MVC 内核 — 复用) + C-4 排序** — 展开端点抽象与 Web 暴露

---

## §0.8

- 🔴 Deep，1篇 — 端点抽象(@Endpoint 注解: id+defaultAccess + @ReadOperation/@WriteOperation/@DeleteOperation 操作注解 + ExposableEndpoint.getOperations) → 发现机制(EndpointDiscoverer: createEndpointBeans 找 @Endpoint 容器 Bean → WebEndpointDiscoverer 转 ExposableWebEndpoint, WebEndpointAutoConfiguration @Bean 注册) → Web 暴露(WebMvcEndpointHandlerMapping extends RequestMappingInfoHandlerMapping: initHandlerMethods 遍历端点/操作 → registerMapping 建 RequestMappingInfo 映射到 /actuator/{id}) → 健康检查示例(HealthEndpoint: @Endpoint(id="health") + HealthIndicator 聚合)
- 设计模式: [模式: 注解驱动]—@Endpoint/@ReadOperation; [模式: 工厂/发现器]—EndpointDiscoverer; [模式: 适配到 MVC]—WebMvcEndpointHandlerMapping 复用 RequestMappingInfoHandlerMapping

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Endpoint.java:57,64,79 | 注解 | **public @interface Endpoint(L57)**: id()(L64)+defaultAccess()(L79, 默认 UNRESTRICTED) | High |
| ExposableEndpoint.java:29,57 | 抽象 | **接口 L29**: getOperations()(L57) — 端点=id+一组 Operation | High |
| EndpointDiscoverer.java:72,151,157 | 发现 | **类 L72**: discoverEndpoints(L151)→createEndpointBeans(L157) 找 @Endpoint 容器 Bean → 建 ExposableEndpoint | High |
| WebEndpointDiscoverer.java:53 | Web 发现 | **extends EndpointDiscoverer<ExposableWebEndpoint,WebOperation>(L53)**: createEndpoint 转 Web 端点 | High |
| WebEndpointAutoConfiguration.java:91,92,98 | 注册 | **@ConditionalOnMissingBean(L91)+webEndpointDiscoverer(L92)→new WebEndpointDiscoverer(L98)** | High |
| AbstractWebMvcEndpointHandlerMapping.java:90,154,156,170 | Web 暴露 | **extends RequestMappingInfoHandlerMapping(L90)**: initHandlerMethods(L154) 遍历端点/操作→registerMappingForOperation(L156/170)→registerMapping 建 RequestMappingInfo | High |
| HealthEndpoint.java:41,64 | 示例 | **@Endpoint(id="health")(L41)+@ReadOperation(L64)**: 用 HealthIndicator 聚合健康 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 端点机制是一条线(注解→发现→Web 暴露), 3 块耦合 — 1篇 (~52行) 按"端点抽象与注解 → 发现机制 → Web 暴露与健康示例"展开; C-12/C-13 的 MVC 内核复用, 本域讲端点怎么生成 MVC 映射。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @Endpoint 注解模型 (id/defaultAccess + 操作注解) | 🔴 | **为什么🔴**: 端点怎么声明 |
| P1-2 | EndpointDiscoverer 发现机制 (@Endpoint Bean → ExposableEndpoint) | 🔴 | **为什么🔴**: 注解怎么变成端点对象 |
| P1-3 | Web 暴露 (WebMvcEndpointHandlerMapping → /actuator) | 🔴 | **为什么🔴**: 端点怎么成 HTTP 接口 |
| P2-1 | @ReadOperation 操作注解 (方法 → Operation) | 🟡 | **为什么🟡**: 操作注解语义 |
| P2-2 | Health 端点示例 (HealthIndicator 聚合) | 🟡 | **为什么🟡**: 内置端点代表 |
| P3-1 | 与 C-12/C-13 边界 (MVC 内核复用) | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **端点抽象** | 🔴 | 端点怎么声明 |
| B | **发现机制** | 🔴 | 注解→对象 |
| C | **Web 暴露与健康** | 🔴 | 端点→HTTP |

> **Cluster A (§1)**: @Endpoint/@ReadOperation + ExposableEndpoint(Operation 集合)
> **Cluster B (§2)**: EndpointDiscoverer(createEndpointBeans 找 @Endpoint Bean) + WebEndpointDiscoverer + WebEndpointAutoConfiguration 注册
> **Cluster C (§3)**: WebMvcEndpointHandlerMapping(initHandlerMethods→registerMapping→/actuator) + HealthEndpoint 示例

→ 引出 S-22: 测试自动配置 — Actuator 之后: @SpringBootTest/@WebMvcTest 切片自动配置(前置 C-16~C-19)
