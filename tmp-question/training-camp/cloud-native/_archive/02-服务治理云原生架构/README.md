# 02 服务治理云原生架构

> 整个训练营的**地基模块**，篇幅最大、讲得最细。不是教你怎么用 Spring Cloud，而是教你从注册中心选型 → 多语言客户端 → K8s 服务发现 → CI/CD 全链路打通。

## 讲什么

### 2.2.1 基础设施

| 维度 | 内容 |
|------|------|
| **Java** | Spring Cloud 服务注册发现（Eureka→Consul→Nacos→K8s Service） |
| **Java** | Microsphere Spring Cloud 增强：多注册中心混合订阅、同区域优先路由、实例元信息扩展、优雅上下线 |
| **GoLang** | Go 基础（语法+并发模型）→ **从零手写 Consul GO API Client**：HTTP 封装 → 健康检查 → 服务注册 → 服务发现 → Watch |
| **容器化** | Consul/etcd Dockerfile 定制（非 pull 镜像，是教你写 Dockerfile） |
| **CI/CD** | Jenkins 部署 + Docker 镜像仓库 + Maven 私服搭建 |
| **K8s** | 基础三件套 Pod/Deployment/Service/Ingress + Spring Cloud Kubernetes |

### 2.2.2 应用服务端

| 维度 | 内容 |
|------|------|
| Java | Microsphere Spring Cloud 应用整合 |
| CI/CD | Java Artifacts 发布 → Docker 镜像构建 → K8s 部署，GitHub WebHooks 自动触发 |
| K8s | Kubernetes 服务注册（替代传统注册中心） |

### 2.2.3 应用管理端

| 维度 | 内容 |
|------|------|
| Java | Microsphere Spring Boot 运维特性：Endpoints 聚合、缓存优化、实例元信息、优雅上下线 |
| Java | Microsphere DevOps 服务管理：服务列表、依赖关系拓扑、上下线按钮 |
| UI | DevOps UI 整合服务注册发现，可视化运维 |
| K8s | Kuboard 可视化管理 |

## 为什么用 GoLang？

因为云原生生态的核心组件（Consul、etcd、Prometheus）客户端基本都是 Go 写的，Java 工程师需要**能看懂甚至改这些东西**。

## 涉及的代码仓库

- [microsphere-spring-cloud](https://github.com/microsphere-projects/microsphere-spring-cloud)
- [microsphere-spring-boot](https://github.com/microsphere-projects/microsphere-spring-boot)
- [microsphere-devops](https://github.com/microsphere-projects/microsphere-devops)
- [shopizer](https://github.com/shopizer-ecommerce/shopizer)
