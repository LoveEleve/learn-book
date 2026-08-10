# 03 分布式配置云原生架构

> 四种配置方案逐一对比和实战：Spring Cloud Config → Consul KV → K8s ConfigMaps → Apollo。核心是 Microsphere Configuration 做了**统一抽象**——底层换，API 不变。

## 讲什么

### 基础设施

| 维度 | 内容 |
|------|------|
| Java | Spring Cloud Config 基础（Spring 配置模型 + Consul KV + K8s ConfigMaps） |
| Java | Microsphere Configuration 核心特性：统一抽象、多后端适配 |
| Java | **Apollo 配置中心**（携程开源，集群管理） |
| GoLang | Consul KV API（配置维度） |
| 容器化 | Docker 网络部署 |
| K8s | ConfigMaps + Spring Cloud Kubernetes |

### 应用服务端

| 维度 | 内容 |
|------|------|
| Java | Microsphere Configuration 应用整合，动态刷新 |
| 容器化 | Docker Compose + Docker Network 编排 |
| K8s | ConfigMaps 在实际应用中的使用 |

### 应用管理端

| 维度 | 内容 |
|------|------|
| Java | DevOps UI 整合 Microsphere Configuration（单机配置在线改、推送、刷新） |
| Java | Apollo 配置管理（集群级配置治理） |
| UI | Kuboard 管理 K8s ConfigMaps |

## 关键点

为什么不直接用 Spring Cloud Config，还要自己搞 Microsphere Configuration？因为**运维面需要可视化管控**——上线后运营人员不可能 ssh 进服务器改配置文件。

## 涉及的代码仓库

- [microsphere-configuration](https://github.com/microsphere-projects/microsphere-configuration)
- [microsphere-devops](https://github.com/microsphere-projects/microsphere-devops)
- [apolloconfig/apollo](https://github.com/apolloconfig/apollo)
