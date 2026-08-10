# 09 云原生部署（Week 18-20 · 6课时）

## 学完能干什么
能把 Java 应用从本地一路推到 K8s + Istio，理解 etcd 集群部署和 Nacos 服务发现。

> ⚠️ 训练营最薄弱的模块——云原生训练营只有语雀大纲，无实际文档。以下基于训练营现有内容 + 建议自补方向。

---

## 课时 42：Docker 全链路

**训练营有**：
- `stage-3/docs/26.` — etcd 单机 Docker + bitnami 镜像 + 3 节点集群完整 YAML
- `stage-4/docs/09.` — MongoDB Docker Compose → K8s Secret/ConfigMap/Deployment/Service/LoadBalancer 完整 YAML

**自己补**：
- Dockerfile 多阶段构建（maven:3-openjdk-17 构建 → openjdk:17-slim 运行）
- 非 root 用户运行（`USER 1000`）
- 健康检查（`HEALTHCHECK`）

---

## 课时 43：K8s 核心对象

**训练营有**：
- `stage-4/docs/09.` — 完整 K8s 组件介绍（Control Plane + Node + 插件）

**必须掌握的 5 个对象**：

| 对象 | 作用 | YAML 示例 |
|------|------|---------|
| Pod | 最小调度单元 | `spec.containers[].image` |
| Deployment | 声明式副本 + 滚动更新 | `spec.replicas: 3`, `strategy: RollingUpdate` |
| Service | 负载均衡 | `ClusterIP`(内部)/`NodePort`/`LoadBalancer` |
| ConfigMaps | 配置分离 | `data.database_url: mongodb-service` |
| Ingress | 外部流量入口 | `rules[].http.paths[]` |

**训练营提供的完整 YAML**（`stage-4/docs/09.`）：
- MongoDB Secret（base64 编码的用户名/密码）
- MongoDB ConfigMap
- MongoDB Deployment + Service
- Mongo Express Deployment + Service（LoadBalancer, nodePort: 30000）

---

## 课时 44：Nacos 替代 Eureka

**为什么换**：Eureka 2.0 跳票，社区转向 Nacos/Consul/K8s Service。

**训练营有**：
- `cloud-native-code/projects/microsphere-nacos/` — 130+ Java 文件，完整 OpenAPI v1/v2 客户端 + Spring Cloud DiscoveryClient

**🔍 看什么代码**：
```bash
cd cloud-native-code/projects/microsphere-nacos && mvn compile
```
- `OpenApiNacosClient` → 聚合认证/配置/服务/实例/命名空间/Raft 全部功能
- `OpenApiConfigClient` → 配置中心 CRUD + 变更监听
- `NacosDiscoveryClient` → 实现 Spring Cloud DiscoveryClient
- `NacosDiscoveryConfiguration` → Spring @Configuration 自动配置

---

## 课时 45：Jenkins CI/CD

**训练营有**：仅大纲提及

**自己补**：
```
Git push → WebHook → Jenkins 拉代码 → Maven 编译 → 单元测试
→ Docker build → push Harbor → kubectl apply → 滚动更新
```

---

## 课时 46：Istio Service Mesh

**📖 读什么**
- `stage-3/docs/21. 第十五节：Istio.md`（全文）
- `stage-3/docs/22. 加餐四：Istio（续）.md`

**Istio 帮你做什么（不需要改代码）**：

| 功能 | 配置 | 效果 |
|------|------|------|
| 金丝雀发布 | `VirtualService` 权重 90/10 | 10% 流量到新版 |
| 熔断 | `DestinationRule` connectionPool | 连接数超限自动熔断 |
| 可观测 | Envoy Sidecar 自动注入 | 收集所有 HTTP/gRPC 流量指标 |
| 安全 | mTLS | 自动加密服务间通信 |

---

## 课时 47：etcd 配置中心

**📖 读什么**
- `stage-3/docs/26. 第十八节：配置中心 - etcd.md`（全文）

**3 节点集群 Docker Compose**（训练营提供完整 YAML）：
```yaml
services:
  node1: ipv4_address: 172.16.238.100
  node2: ipv4_address: 172.16.238.101
  node3: ipv4_address: 172.16.238.102
```

**Spring PropertySource 整合**：
- `@PropertySource` 的 4 个设计缺陷（不能扩展/不支持自动刷新/不支持排序/不能元注解复用）
- microsphere-spring-config 解决方案：`@ResourcePropertySource` + `AnnotatedPropertySourceLoader` 层次
- etcd Watch 机制：`watchClient.watch(key, callback)` 实现配置变更实时推送

**🔍 看什么代码**：
```bash
cd cloud-native-code/projects/microsphere-etcd && mvn compile
```

---

## 现代等价对照

### Eureka → Nacos / K8s Service

| Eureka 概念 | Nacos 等价 | K8s 等价 |
|-----------|-----------|---------|
| `eureka.client.serviceUrl` | `spring.cloud.nacos.discovery.server-addr` | K8s Service DNS |
| 心跳续约 30s | 临时实例心跳 5s | Pod readinessProbe |
| 自我保护模式 | Distro AP 协议 | - |
| Peer 同步 | Raft CP 协议 | etcd |

### Config Server → Apollo / Nacos Config / K8s ConfigMaps

| Config Server | 2026 等价 |
|--------------|---------|
| Git 后端 | Apollo 配置中心（携程） |
| 手动刷新 | Nacos Config 自动推送 |
| 无版本管理 | K8s ConfigMaps + Helm |

---

## 本阶段自检清单
- [ ] 能写一个生产级 Dockerfile（多阶段 + 非 root）
- [ ] 能把应用部署到 K8s 并通过 Service 暴露
- [ ] 了解 etcd 3 节点集群的部署方式
- [ ] 知道 Nacos 和 K8s Service 各自适合什么场景
