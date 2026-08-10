# 05 服务可观测性云原生架构

> 三大支柱（Logging / Metrics / Tracing）全部落地到生产级别。不是教你怎么在本地跑个 Prometheus，而是教你怎么搭建一个**生产可用的可观测性平台**。

## 讲什么

### 基础设施

| 支柱 | 内容 |
|------|------|
| **Logging** | Microsphere Logging：日志过滤器（敏感信息脱敏）、JMX 动态改日志级别、ELK 全套搭建 |
| **Metrics** | Micrometer 扩展：JDBC 指标（慢SQL统计）、系统指标（CPU/内存/GC）、JMX 指标；Sentinel 整合 Micrometer |
| **Tracing** | Skywalking Java Agent **镜像重构**（不是拿来就用，是教你定制 Skywalking Docker 镜像） |

### 应用服务端

| 维度 | 内容 |
|------|------|
| Java | Microsphere Observability 应用整合：镜像构建、JMX 扩展、Gradle 插件 |
| K8s | Kubernetes 平台监控（宿主机、组件、核心指标全覆盖） |

### 应用管理端

| 维度 | 内容 |
|------|------|
| Java | Prometheus HTTP 服务发现 |
| GoLang | **Prometheus 生产级集群搭建**：集群部署、监控告警规则、联邦机制 |

## 非典型内容

- **Skywalking 镜像重构**：官方镜像有 JVM 参数不合理、插件版本绑定等问题，教你重新打镜像
- **Prometheus 联邦**：多集群场景下，联邦节点汇总各集群指标，避免单点瓶颈

## 涉及的代码仓库

- [microsphere-observability](https://github.com/microsphere-projects/microsphere-observability)
- [microsphere-logging](https://github.com/microsphere-projects/microsphere-logging)
- [apache/skywalking](https://github.com/apache/skywalking)
