# 06 服务通讯云原生架构

> 从 Dubbo 增强到 GoLang 互通到 Native 编译，核心是多语言微服务的通信方案。

## 讲什么

### 基础设施

| 维度 | 内容 |
|------|------|
| Java | Microsphere Dubbo 扩展：Metadata 增强 + Actuator Endpoints（运维端点） |
| GoLang | **Apache Dubbo Go**：Java 和 Go 微服务通过 Dubbo 协议互通——多语言微服务的关键 |

### 应用服务端

| 维度 | 内容 |
|------|------|
| Java | Microsphere Dubbo 应用整合 |
| Java | Microsphere Dubbo Cloud 整合（Spring Cloud + Dubbo 混合） |
| 容器化 | **Dubbo Native**：GraalVM 编译原生镜像，冷启动从秒级 → 毫秒级 |

### 应用管理端

| 维度 | 内容 |
|------|------|
| Java | Microsphere DevOps 整合 Dubbo：服务管理、在线测试调用、路由管理 |
| UI | DevOps UI 可视化管理 Dubbo 服务 |

## 重点思路

为什么需要 Dubbo Go？因为云原生场景下：
- 网关/LB 用 Go 写（性能好）
- 业务逻辑用 Java 写（生态丰富）
- 两者通过 Dubbo 协议通信，不用 REST 转一层

## 涉及的代码仓库

- [microsphere-dubbo](https://github.com/microsphere-projects/microsphere-dubbo)
- [microsphere-devops](https://github.com/microsphere-projects/microsphere-devops)
- [apache/dubbo](https://github.com/apache/dubbo)
- [apache/dubbo-go](https://github.com/apache/dubbo-go)
