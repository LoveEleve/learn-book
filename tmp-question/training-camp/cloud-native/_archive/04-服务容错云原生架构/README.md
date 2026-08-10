# 04 服务容错云原生架构

> 不是教你怎么配 Sentinel/Resilience4j，而是先讲 **Java 框架扩展点体系**（JDBC代理/MyBatis拦截器/JPA Listener/Dubbo Filter/Spring Interceptor），理解"在哪个层面做容错"比选什么工具更重要。

## 讲什么

### 基础设施

| 维度 | 内容 |
|------|------|
| Java | Java 框架扩展点全景：JDBC DataSource 代理 / MyBatis Interceptor / JPA EntityListener / Dubbo Filter / Spring MVC Interceptor |
| Java | **Alibaba Sentinel** 核心原理：滑动窗口 / 令牌桶 / 漏桶 / 规则持久化 |
| Java | Microsphere Sentinel 扩展：Dashboard 优化、推模式持久化 |
| Java | **Resilience4j** 核心：断路器 / 舱壁 / 限流 / 重试 / 超时 |
| Java | Microsphere Resilience4j 扩展 |
| GoLang | Alibaba Sentinel GoLang（跨语言容错） |
| Service Mesh | **Istio**：无需改代码的熔断/限流/超时 |

### 应用服务端

| 维度 | 内容 |
|------|------|
| Java | Microsphere Sentinel 应用整合（JDBC/MyBatis/JPA/RPC 各层面） |
| Java | Microsphere Resilience4j 应用整合 |

### 应用管理端

| 维度 | 内容 |
|------|------|
| Java | Microsphere Sentinel Dashboard：DataSource 优化、服务发现、Metrics 扩展 |
| UI | DevOps UI 整合 Sentinel Dashboard、Resilience4j 运维面板（SQL/MyBatis/JPA/Web/RPC 容错状态可视管控） |

## 关键设计思路

**Sentinel vs Resilience4j 不是二选一，而是分层使用**：
- Sentinel 做**流量控制**（入口层限流/降级，适合网关和粗粒度）
- Resilience4j 做**实例级容错**（细粒度断路器/重试/超时，适合 RPC 调用）

## 涉及的代码仓库

- [microsphere-sentinel](https://github.com/microsphere-projects/microsphere-sentinel)
- [microsphere-alibaba-sentinel](https://github.com/microsphere-projects/microsphere-alibaba-sentinel)
- [microsphere-resilience4j](https://github.com/microsphere-projects/microsphere-resilience4j)
- [microsphere-devops](https://github.com/microsphere-projects/microsphere-devops)
