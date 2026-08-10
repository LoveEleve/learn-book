# 08 服务网关云原生架构

> Microsphere Gateway 不是通用网关，而是**四合一分层网关**，每层解决不同问题。

## 讲什么

### 基础设施 — 四合一网关

| 网关层 | 职责 |
|--------|------|
| **服务发现网关** | 动态发现后端实例，替代硬编码 upstream |
| **分布式配置网关** | 统一配置入口，所有服务配置经网关代理 |
| **服务 API 网关** | HTTP → 微服务，类似 Spring Cloud Gateway |
| **服务 RPC 网关** | HTTP → Dubbo 泛化调用，HTTP 请求直接透传 Dubbo |

### 应用管理端

| 维度 | 内容 |
|------|------|
| Java | Microsphere DevOps 整合 Gateway：路由管理/限流/安全策略统一管控 |
| UI | DevOps UI 可视化管理所有网关策略 |

## 设计亮点

RPC 网关是核心创新——传统方案是 HTTP → Gateway → REST → Dubbo，多一跳还有序列化损耗。Microsphere RPC 网关直接做 **HTTP → Dubbo 泛化调用**，省掉 REST 层。

## 涉及的代码仓库

- [microsphere-gateway](https://github.com/microsphere-projects/microsphere-gateway)
- [microsphere-devops](https://github.com/microsphere-projects/microsphere-devops)
- [microsphere-dubbo](https://github.com/microsphere-projects/microsphere-dubbo)
