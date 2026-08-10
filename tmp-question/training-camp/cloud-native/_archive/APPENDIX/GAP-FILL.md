# 训练营缺失内容的补充方案

> 训练营没讲、没写完、或只有空仓库的部分。基于我自己的知识补充。
> 这不是"他讲了什么"，而是"他没讲但你该会的"。

---

## 一、Kafka 多活（Stage 4 #20-21 缺失）

训练营规划了这个专题但文档未交付。以下是 Kafka 多活的核心设计思路：

### 1.1 架构模型

```
  城市A (Kafka Cluster A)                  城市B (Kafka Cluster B)
  ┌──────────────────────┐         ┌──────────────────────┐
  │  Producer (本城)      │         │  Producer (本城)      │
  │       ↓               │         │       ↓               │
  │  Topic: orders-a      │ ←───→  │  Topic: orders-b      │
  │       ↓               │ Mirror  │       ↓               │
  │  Consumer (本城)      │ Maker 2 │  Consumer (本城)      │
  └──────────────────────┘         └──────────────────────┘
```

### 1.2 三种方案

| 方案 | 原理 | 延迟 | 适用场景 |
|------|------|:----:|---------|
| **MirrorMaker 2** | Kafka 官方工具，消费源集群 → 生产到目标集群 | 秒级 | 跨机房异步复制 |
| **双写** | Producer 同时写两个集群，通过 transaction 保证原子性 | 实时 | 要求强一致时 |
| **单元化路由** | 每个城市只处理本城数据，不做跨城同步 | 无 | 真正的多活架构 |

### 1.3 关键问题

1. **Offset 同步**：MirrorMaker 通过 `RemoteClusterUtils` 自动翻译 Offset
2. **Topic 路由**：Consumer 优先消费本城 Topic，故障时切到异地
3. **数据一致性**：双写模式需要分布式事务保证
4. **延迟容忍**：跨城复制通常有 50-200ms 延迟

### 1.4 如果自己实现

参考 Microsphere 的 Redis Replicator 模式：
```
Producer 发送 → Interceptor 拦截 → publishEvent → Kafka Mirror Topic → 异地 Consumer → 本地 produce
```

---

## 二、分布式配置中心实现（distributed-config-project 仅 POM）

训练营的 `distributed-config-project` 只有 POM 骨架，没有 Java 代码。以下是完整的实现思路：

### 2.1 参考 microsphere-configuration 的四源统一抽象

```
@PropertySourceExtension (元注解)
    ├── @ApolloPropertySource  → ApolloPropertySourceBeanDefinitionRegistrar
    ├── @EtcdPropertySource    → EtcdPropertySourceLoader (extends PropertySourceExtensionLoader)
    ├── @NacosPropertySource   → NacosPropertySourceLoader
    └── @ZookeeperPropertySource → ZookeeperPropertySourceLoader
```

**关键设计模式**：模板方法——`PropertySourceExtensionLoader.loadPropertySource()` 定义流程，子类实现 `resolveResources()`。

### 2.2 如果自己从零实现一个配置中心

```
Server 端:
├── ConfigController (/config/{app}/{profile}/{label})
├── ConfigRepository (MySQL Storage)
├── NotifyService (长轮询 / WebSocket)
└── ConfigVersionService (乐观锁版本控制)

Client 端:
├── ConfigClient (HTTP 拉取)
├── ConfigCache (本地文件缓存，容灾)
├── ConfigListener (长轮询 / Watch)
└── Spring PropertySource 集成
```

### 2.3 核心要点

1. **长轮询 vs Watch**：Apollo 用长轮询（兼容性好），etcd/Nacos 用 Watch/gRPC（性能好）
2. **本地缓存**：配置中心挂了也要能启动，靠本地文件兜底
3. **灰度发布**：配置按 IP/AppId/版本号灰度下发
4. **回滚**：每次配置变更加快照，支持一键回滚

---

## 三、测试策略（全文缺失）

120 节课没有系统讲测试。以下是生产级测试体系：

### 3.1 测试金字塔

```
       /\
      /E2E\          少量端到端测试（关键业务流程）
     /------\
    /集成测试\        中等数量（数据库/Redis/Kafka 用 Testcontainers）
   /----------\
  /  单元测试  \      大量（纯逻辑，Mock 外部依赖）
 /--------------\
```

### 3.2 关键工具

| 层 | 工具 | 用法 |
|----|------|------|
| 单元测试 | JUnit 5 + Mockito | `@Mock` Service，`@InjectMocks` Controller |
| 集成测试 | Testcontainers | `new MySQLContainer<>("mysql:8")` 真实数据库 |
| API 测试 | MockMvc | `mockMvc.perform(get("/api/user/1")).andExpect(status().isOk())` |
| 契约测试 | Spring Cloud Contract | Provider 定义契约 → Consumer 验证 |
| E2E | Selenium / Playwright | 关键路径自动化 |

### 3.3 训练营对应示例（segfault-lessons lesson-19 已有）

lesson-19 提供了 7 种测试风格，可直接参考：
- 纯单元：`PersonTest`
- @SpringBootTest：`PersonSpringBootTest`
- @WebMvcTest + MockMvc：`PersonControllerSpringBootTest`
- 自定义 TestExecutionListener：`PersonIntegrationTestListener`
- @TestPropertySource：覆盖测试配置

---

## 四、OAuth2 / JWT 认证（训练营只有 RBAC）

### 4.1 JWT 认证流程

```
Client                    Gateway                  Auth Service
  │                          │                          │
  │  POST /login             │                          │
  │  {username, password}    │                          │
  │ ─────────────────────────→                          │
  │                          │ ────────────────────────→│
  │                          │                          │ 验证密码
  │                          │                          │ 签发 JWT
  │                          │ ←────────────────────────│
  │  ← 200 {access_token,    │                          │
  │         refresh_token}   │                          │
  │                          │                          │
  │  GET /api/orders         │                          │
  │  Authorization: Bearer   │                          │
  │  <access_token>          │                          │
  │ ─────────────────────────→                          │
  │                          │ 验证 JWT (公钥解密)      │
  │                          │ 提取 userId/roles        │
  │                          │ ────────────────────────→│
  │                          │ ←────────────────────────│
  │  ← 200 orders[]          │                          │
```

### 4.2 Spring Security 配置要点

```java
// Resource Server 配置（微服务侧）
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    return http
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/api/admin/**").hasRole("ADMIN")
            .anyRequest().authenticated()
        )
        .build();
}
```

### 4.3 关键安全设计

1. **Access Token 短期**（15min），Refresh Token 长期（7天）
2. **Token 黑名单**：登出后把 JTI 加入 Redis 黑名单
3. **Gateway 统一验签**：只在 Gateway 验证 JWT，后端服务不再验证
4. **密钥轮换**：定期更换签名密钥，支持多密钥并存

---

## 五、部署策略（全文缺失）

### 5.1 K8s 滚动更新

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1         # 最多超出 1 个 Pod
      maxUnavailable: 0   # 不能有不可用的 Pod
```

### 5.2 金丝雀发布（Istio VirtualService）

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
spec:
  http:
  - route:
    - destination:
        host: my-service
        subset: v1        # 稳定版
      weight: 90
    - destination:
        host: my-service
        subset: v2        # 金丝雀版
      weight: 10          # 10% 流量
```

### 5.3 部署检查清单

- [ ] 健康检查（Liveness: 进程是否活着，Readiness: 是否可以接流量）
- [ ] 优雅关闭（`terminationGracePeriodSeconds` > 业务最长请求时间）
- [ ] 资源限制（`resources.limits.cpu/memory`）
- [ ] 回滚测试（`kubectl rollout undo`）
- [ ] 监控告警（部署前后对比 TPS/RT/错误率）

---

## 六、系统设计面试速查（全文缺失）

### 6.1 常见题型与训练营知识映射

| 面试题 | 用到哪个阶段的知识 |
|--------|------------------|
| "设计一个 URL 缩短服务" | 阶段一（REST API）+ 阶段五（数据架构） |
| "设计一个分布式 ID 生成器" | 阶段二（共识算法：为什么不用 UUID？Snowflake 怎么保证全局唯一？） |
| "设计一个限流器" | 阶段七（滑动窗口/令牌桶算法）+ 阶段五（Redis 实战） |
| "设计一个消息队列" | 阶段三（RPC + 序列化）+ 阶段八（Kafka 异步） |
| "设计一个分布式锁" | 阶段五（Redisson RLock）+ 阶段二（ZK 临时顺序节点） |
| "设计支持异地多活的秒杀系统" | 阶段十（多活全部）+ 阶段四（分布式事务）+ 阶段七（限流降级） |

### 6.2 答题框架

1. **需求澄清**（2分钟）：QPS？延迟要求？一致性要求？数据量？
2. **高层设计**（5分钟）：画系统架构图，标注关键组件
3. **深入细节**（10分钟）：挑 2-3 个关键点深入（如"怎么保证分布式一致性"）
4. **总结**（3分钟）：回顾设计决策，指出可能的瓶颈和改进方向

---

## 七、ADR 模板（架构决策记录，全文缺失）

```markdown
# ADR-001: 选择 Nacos 替代 Eureka 作为注册中心

## 状态
已采纳（2024-06-15）

## 上下文
- Eureka 2.0 已停止开发
- 需要同时支持服务注册发现和配置管理
- 团队对 Spring Cloud Alibaba 生态有使用经验

## 决策
使用 Nacos 替代 Eureka，配置管理使用 Nacos CP 模式（Raft），服务发现使用 AP 模式（Distro）

## 后果
- 正面：统一了注册中心和配置中心的技术栈，运维成本降低
- 负面：需要将现有的 Eureka Client 迁移到 Nacos Client
- 风险：Nacos 社区活跃度低于 Kubernetes 原生方案，长期需要关注
```

---

## 总结：25 个缺失项完整清单

| # | 缺失内容 | 重要性 | 本节是否覆盖 |
|---|---------|:----:|:----------:|
| 1 | Kafka 多活 | 高 | ✅ |
| 2 | 分布式配置中心实现 | 中 | ✅ |
| 3 | 单元测试策略 | 高 | ✅ |
| 4 | 集成测试（Testcontainers） | 高 | ✅ |
| 5 | 契约测试 | 中 | ✅ |
| 6 | OAuth2/JWT 认证 | 高 | ✅ |
| 7 | 金丝雀发布 | 中 | ✅ |
| 8 | K8s 滚动更新 | 中 | ✅ |
| 9 | 系统设计面试 | 中 | ✅ |
| 10 | ADR 模板 | 低 | ✅ |
| 11 | 数据库迁移（Flyway） | 中 | 缺 |
| 12 | OpenTelemetry | 高 | 缺 |
| 13 | SLO/SLI/Error Budget | 中 | 缺 |
| 14 | 混沌工程 | 低 | 缺 |
| 15 | 容器安全 | 低 | 缺 |
| 16 | API 网关高级模式 | 中 | 缺 |
| 17 | 事件溯源/CQRS | 低 | 缺 |
| 18 | DDD 战术设计 | 低 | 缺 |
| 19 | GraalVM Native Image 实战 | 中 | 缺 |
| 20 | 缓存一致性方案 | 中 | 缺 |
| 21 | Saga 长事务 | 中 | 缺 |
| 22 | Helm Chart | 中 | 缺 |
| 23 | GitOps（ArgoCD） | 中 | 缺 |
| 24 | 链路追踪（Jaeger/Tempo） | 中 | 缺 |
| 25 | 性能调优方法论 | 中 | 部分（Stage 3 GC 对比数据好，但缺其他层） |
