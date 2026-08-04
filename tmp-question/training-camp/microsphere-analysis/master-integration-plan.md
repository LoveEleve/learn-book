# Microsphere → my-xhs 整合全景图

> **核心命题**：microsphere 有 36 个仓库，my-xhs 有 19 个微服务。哪些能力值得搬？哪些不值得？怎么搬？本文件从完整的技术栈映射、优先级判断、依赖关系、实施路径四个维度给出全景答案。

---

## 第一部分：microsphere 全部模块一览

### Infrastructure 层

| 仓库 | 文件数 | 核心能力 | 对 my-xhs 的价值 |
|------|--------|---------|----------------|
| microsphere-java | 大量 | SPI 基础设施、事件系统、配置属性、工具类、类型转换 | **低** — JDK 17 自带很多了 |
| microsphere-logging | 少量 | 日志抽象 | **极低** — SLF4J + Logback 已足够 |
| microsphere-bom | BOM | 依赖管理 | **低** — my-xhs 有自己 BOM |

### Spring 层

| 仓库 | 文件数 | 核心能力 | 对 my-xhs 的价值 |
|------|--------|---------|----------------|
| microsphere-spring | 大量 | BeanPostProcessor 扩展、事件拦截器、Web 端点注册、HandlerMethod 拦截 | **中** — HandlerMethodInterceptor 值得借鉴 |
| microsphere-spring-boot | 大量 | 条件注解、配置监听、Actuator 扩展、FailuerAnalyzer | **低** — Spring Boot 3.x 已完善 |
| microsphere-spring-cloud | 中等 | 多注册中心、Feign 热更新、服务注册生命周期事件 | **低** — 只用 Nacos，不需要多注册 |

### Data Access 层

| 仓库 | 文件数 | 核心能力 | 对 my-xhs 的价值 |
|------|--------|---------|----------------|
| microsphere-mybatis | 25 | ExecutorFilter SPI、MyBatis 拦截器链 | **中** — my-xhs 用 MyBatis Plus |
| microsphere-alibaba-druid | 20 | AbstractStatementFilter、Druid Filter 扩展 | **高** — Druid 在国内主流 |
| microsphere-hibernate | - | Hibernate EntityCallback | **无** — 不用 Hibernate |
| microsphere-redis | 84 | Redis 命令拦截器、双层代理、Kafka 复制 | **高** — my-xhs 用 Redisson，拦截器模式通用 |

### Fault Tolerance 层

| 仓库 | 文件数 | 核心能力 | 对 my-xhs 的价值 |
|------|--------|---------|----------------|
| microsphere-sentinel | 30 | 扩展点架构、SentinelTemplate、Plugin SPI | **高** — my-xhs 使用 Sentinel 1.8.8 |
| microsphere-resilience4j | 57 | 6 种模式模板、CallbackChain、双向事件桥接 | **低** — 国内用 Sentinel 为主 |

### Integration 层

| 仓库 | 文件数 | 核心能力 | 对 my-xhs 的价值 |
|------|--------|---------|----------------|
| microsphere-multiactive | 31 | ZoneLocator、ZonePreferenceFilter、动态切换 | **高** — 已部分实现，缺动态切换 |
| microsphere-gateway | 28 | Gateway 扩展 | **高** — my-xhs 有 gateway 模块 |
| microsphere-observability | 39 | Micrometer 集成、条件注解 | **中** — 可参考，但 Micrometer 官方文档已完善 |
| microsphere-dynamic | 81 | 动态数据源 | **中** — my-xhs 已有 DynamicDataSource |

### 其他

| 仓库 | 价值 |
|------|------|
| microsphere-i18n | **极低** — 不需要国际化框架 |
| microsphere-netflix | **无** — Netflix OSS 已不维护 |
| microsphere-dubbo | **无** — 用 Spring Cloud，不用 Dubbo |
| microsphere-tomcat | **无** — 空壳项目 |

---

## 第二部分：按 my-xhs 需求反推的优先级

### P0：必须整合

| 能力 | 来源 | my-xhs 现状 | 差距 | 预估工作量 |
|------|------|-----------|------|----------|
| 区域自动定位 + 动态切换 | multiactive | 已有 ZoneContext/ZonePreferenceFilter，缺 ZoneLocator 和 ZoneContextChangedListener | 7 个新文件 | 2 人天 |
| Sentinel 扩展点接入 | sentinel | 依赖 Sentinel 1.8.8，但没有 microsphere 的扩展点集成 | 5-6 个适配器 | 3 人天 |
| Redis 命令拦截器 | redis | 已有 7 个文件实现 | 和 my-xhs 现有实现方向一致，可对比优化 | 1 人天 |
| HandlerMethod 拦截 | spring-web | 没有 | Gateway 层需要 | 0.5 人天 |
| Druid 连接池扩展 | druid | 没有（或默认 Druid 配置） | 防 SQL 注入、慢查询统计 | 1 人天 |

### P1：值得借鉴

| 能力 | 来源 | 说明 | 预估工作量 |
|------|------|------|----------|
| 配置可监听绑定 | spring-boot | 配置变更时自动通知 Bean | 0.5 人天 |
| 动态数据源配置 | dynamic | 和现有 DynamicDataSource 互补 | 建议不直接搬，参考设计 |
| Gateway 扩展 | gateway | 自定义谓词、过滤器工厂模式 | 2 人天 |
| MyBatis ExecutorFilter | mybatis | SQL 拦截、慢查询统计 | 1 人天 |

### P2：暂时不需要

| 能力 | 理由 |
|------|------|
| resilience4j | 国内用 Sentinel 为主，不需要第二个熔断库 |
| 多注册中心 | 只用 Nacos |
| Feign 热更新 | Spring Cloud Alibaba Feign 已有类似能力 |
| 手写 Nacos HTTP 客户端 | Spring Cloud Alibaba Nacos 够用 |
| CallbackChain | 只在有多个独立容错模式组合时才需要 |

---

## 第三部分：架构叠加关系

```
my-xhs 现有技术栈
  ├── Spring Boot 3.2.5 + Spring Cloud 2023.0.1
  ├── Spring Cloud Alibaba 2023.0.1.0 (Nacos + Sentinel)
  ├── MyBatis Plus 3.5.7
  ├── Redisson 3.27.0
  └── Gateway

叠加 microsphere 能力后：
  ├── [P0] ZoneLocator + ZoneContextChangedListener
  │   └── 让 Gateway + LoadBalancer + DataSource 都区域感知
  │
  ├── [P0] SentinelPlugin + Adapter
  │   └── 自动保护 MyBatis、Redis、Gateway、Feign 调用
  │
  ├── [P1] HandlerMethodInterceptor + WebEndpointRegistry
  │   └── Gateway 层统一拦截、监控、审计
  │
  ├── [P1] AbstractStatementFilter + ExecutorFilter
  │   └── Druid + MyBatis 的 SQL 级监控
  │
  └── [P1] RedisCommandInterceptor
      └── Redis 命令级拦截（缓存穿透检测、热点 key 发现）
```

---

## 第四部分：模块间依赖关系

```
必须按顺序实施：

Phase 1: 基础能力（无外部依赖）
  ZoneLocator + ZoneContextChangedListener
  → 不依赖其他 microsphere 模块
  → 只依赖 Spring Cloud Context (EnvironmentChangeEvent)
  → 实施后：Gateway + LoadBalancer + DataSource 区域感知

Phase 2: 接入 Sentinel（依赖 Phase 1？不依赖）
  SentinelTemplate → SentinelPlugin SPI → 6 个 Adapter
  → 不依赖 Phase 1
  → 依赖 Sentinel 1.8.8（已有）
  → 和 Phase 1 可以并行

Phase 3: 数据访问监控（依赖 Phase 2？不依赖）
  Druid AbstractStatementFilter → Druid SQL 监控
  MyBatis ExecutorFilter → MyBatis SQL 监控
  RedisCommandInterceptor → Redis 命令监控
  → 可以独立实施
  → 可以和 Phase 1/2 并行

Phase 4: Gateway 增强（依赖 Phase 3？部分依赖）
  HandlerMethodInterceptor
  → 需要 Spring Web 模块的 WebEndpointRegistry
  → 可以和 Gateway 模块的现有路由配置叠加
  → 可以和 Phase 1 配合（Gateway 区域路由）

结论：3 条并行链路
  Phase 1 (Zone) ─┐
                    ├── 无依赖关系，可并行
  Phase 2 (Sentinel) ─┐
                      ├── 无依赖关系，可并行
  Phase 3 (Data) ─────┘
  Phase 4 (Gateway) ─── 依赖 Phase 3 的某些基础设施，但可以先做基础版本
```

---

## 第五部分：各模块对 my-xhs 现有代码的改动范围

### 改动小的（新增文件，不改现有代码）

| 能力 | 新增文件数 | 需要改现有文件 |
|------|-----------|-------------|
| ZoneLocator | 4 | 1 个（ZoneContextAutoConfiguration） |
| ZoneContextChangedListener | 2 | 0 个 |
| HandlerMethodInterceptor | 3 | 0 个（只要在 Gateway 模块中新增） |
| Druid AbstractStatementFilter | 2 | 0 个 |
| MyBatis ExecutorFilter | 2 | 0 个 |

### 改动大的（需要改现有业务代码）

| 能力 | 说明 |
|------|------|
| Redis 命令拦截器 | my-xhs 已有 7 个文件，和 microsphere 版重合。建议先对比再决定保留哪个版本 |
| Sentinel Adapter | 需要为每个模块（MyBatis、Redis、Gateway、Feign）添加 adapter，业务代码不需要改，但需要确认各模块的拦截点暴露方式 |

### 不需要改动的

| my-xhs 模块 | 说明 |
|------------|------|
| 19 个微服务中的业务代码 | 所有能力都是基础设施层，不侵入业务代码 |
| my-xhs-common 的现有工具类 | 不冲突 |
| Gateway 的路由配置 | HandlerMethodInterceptor 是新增过滤器，不影响现有路由 |

---

## 第六部分：技术栈冲突排查

| microsphere 依赖 | my-xhs 现有 | 冲突？ |
|----------------|-----------|--------|
| Spring Boot 2.x/3.x/4.x | Spring Boot 3.2.5 | 选 3.x 版本即可，无冲突 |
| Spring Cloud 2020.x~2025.x | Spring Cloud 2023.0.1 | 选 2023.x 版本即可 |
| Java 8~17 | Java 17 | 无冲突 |
| Jedis/Lettuce（redis 模块） | Redisson 3.27.0 | **潜在冲突**。microsphere 的 redis 模块基于 Spring Data Redis（Jedis/Lettuce），my-xhs 直接使用 Redisson。需要检查 Redisson 是否兼容 Spring Data Redis 的拦截器链 |
| Nacos Client | Nacos 2.3.0（via Spring Cloud Alibaba） | 无冲突 |
| Sentinel | Sentinel 1.8.8（via Spring Cloud Alibaba） | **潜在冲突**。microsphere 的 sentinel 模块有自己的 SentinelTemplate，和 Spring Cloud Alibaba Sentinel 可能冲突。需要检查 auto-configuration 的顺序 |
| Apache HttpClient（nacos 模块） | 不一定有 | 不冲突，但 nacos 模块不一定要搬 |

### Redisson 兼容性注意

microsphere 的 redis 拦截器链（RedisConnectionInterceptor）基于 Spring Data Redis 的 RedisConnectionFactory。Redisson 有自己的配置方式（RedissonClient），不一定经过 Spring Data Redis 的 RedisTemplate 路径。如果 my-xhs 直接用 RedissonClient 而不是 RedisTemplate，redis 拦截器链可能不生效。

### Sentinel Adapter 注意

microsphere 的 sentinel adapter 在 MyBatis 层面通过 ExecutorFilter 拦截（microsphere-mybatis 的 SPI），Gateway 层面通过 HandlerMethodInterceptor 拦截（microsphere-spring-web 的 SPI）。如果 my-xhs 不引入 microsphere-mybatis 和 microsphere-spring-web 的对应模块，需要直接实现对应的拦截接口（MyBatis Interceptor、Spring MVC HandlerInterceptor）。

---

## 第七部分：实施总路线

### 短跑（2 周）

```
Week 1: ZoneLocator + ZoneContextChangedListener（2 人天）
  目标：配置中心改 zone 秒级切换，DataSource/Redis 跟着切
  文件：7 个新文件 + 改 1 个

Week 2: Sentinel Adapter（3 人天）
  目标：MyBatis + Redis + Gateway 的 Sentinel 自动保护
  文件：5-6 个 Adapter + 配置
```

### 中跑（1 个月）

```
Week 3-4:
  Druid Filter 扩展（1 人天）
  MyBatis ExecutorFilter（1 人天）
  Redis 拦截器链优化（2 人天）
  Gateway HandlerMethodInterceptor（1 人天）
```

### 长跑（2 个月）

```
Observability 集成
  目标：Prometheus + Grafana 全链路监控
  可选：参考 microsphere-observability 的条件注解设计

Gateway 增强
  目标：跨区域路由 + 熔断降级
  可选：参考 microsphere-gateway
```

---

## 第八部分：不确定需要深入探索的模块

以下模块我没有全部分析过，仅根据文件结构判断：

| 模块 | 已知信息 | 需要深入吗？ |
|------|---------|------------|
| microsphere-dynamic | 81 文件，动态数据源 | **建议深入** — 和现有的 DynamicDataSource 互补，可能有更好的子上下文隔离模式 |
| microsphere-gateway | 28 文件 | **建议深入** — my-xhs 有 gateway 模块，可能需要 |
| microsphere-mybatis | 25 文件，ExecutorFilter | **可以浅读** — 核心模式 Sentinel 文章中已经分析过 ExecutorFilter |
| microsphere-druid | 20 文件，AbstractStatementFilter | **可以浅读** — 和 ExecutorFilter 是同一模式 |
| microsphere-observability | 39 文件 | **建议深入** — 如果需要做全链路监控 |
| microsphere-configuration | 11 文件 | **不需要** — 太小 |

如果需要，我可以优先深入探索 dynamic 和 gateway 这两个模块，再做最终整合判断。
