# 07 服务文档化云原生架构

> 让文档自动生成，而非手动维护。从标准 Doclet → REST Docs → Swagger → Dubbo 文档全覆盖。

## 讲什么

### 基础设施

| 维度 | 内容 |
|------|------|
| Java | 标准 Java API 文档：基于 JDK 原生 Doclet API 自动生成 |
| Java | Spring REST API 文档：Spring HATEOAS + Spring REST Docs（测试即文档） |
| Java | **Swagger** OpenAPI 设计：注解驱动 + UI 展示 |
| Java | Microsphere API Docs：让 Dubbo 服务也能生成 Swagger 文档 |

### 应用服务端

| 维度 | 内容 |
|------|------|
| Java | Microsphere API Docs Doclet 特性扩展 |
| UI | Swagger UI 单机/集群部署方案 |

### 应用管理端

> 在 DevOps UI 中直接查看各服务 API 文档，与运维平台整合。

## 关键价值

Dubbo 服务的文档化是个痛点——Swagger 天然支持 REST，但对 RPC 协议束手无策。Microsphere API Docs 通过扩展 Dubbo Metadata + Swagger 注解桥接，让 Dubbo 服务也能自动出文档。

## 涉及的代码仓库

- [microsphere-apidocs](https://github.com/microsphere-projects/microsphere-apidocs)
- [microsphere-dubbo](https://github.com/microsphere-projects/microsphere-dubbo)
