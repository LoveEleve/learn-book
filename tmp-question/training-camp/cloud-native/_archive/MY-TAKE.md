# 训练营之外的补充：我自己的判断 v3（最终版）

> 基于 6 轮、24 个 agent 的深度探索（120+ 节课全文通读、29 个仓库 + segfault-lessons 源码逐行分析）
> 不是"他说了什么"，而是"你应该怎么学、缺了什么、哪些不用学"。

---

## 一、训练营最值得学的东西（最终排序）

### 第一梯队：不学会后悔

| 内容 | 位置 | 评分 | 为什么 |
|------|------|:----:|--------|
| **自研 RPC 框架** | stage-2/rpc-project | 10/10 | 52 文件，Netty+JRaft+Protobuf+3 种服务发现。**学完 Spring Cloud 就是透明壳。** 7 个已知 Bug（RoundRobin 取模、Channel 泄漏等）是你练手的绝佳机会 |
| **共识算法全套** | stage-2 #01-13 + 10 篇论文 | 9/10 | Paxos/Raft/ZAB 原文 + SOFAJRaft 源码。面试大厂必考 |
| **多活架构** | stage-4 + 6 个仓库 | 9/10 | **国内极少有课讲。** ZonePreferenceFilter 7 步过滤、Redis Kafka 跨区域命令复制、DynamicJdbcConfig JSON 驱动切换、UnionDiscoveryClient 多注册中心合并 |
| **JVM GC 对比数据** | stage-3 #05 | 9/10 | ZGC 100 TPS vs G1 11.2 TPS，实测不是编的 |
| **Feign 动态刷新** | microsphere-spring-cloud-openfeign | 8/10 | EnvironmentChangeEvent → FeignClientConfigurationChangedListener → FeignComponentRegistry.refresh → DecoratedFeignComponent 完整 8 步数据流 |

### 第二梯队：值得学但不急

| 内容 | 位置 | 评分 | 为什么 |
|------|------|:----:|--------|
| Seata 源码 | stage-2 #20-21 | 9/10 | ConnectionProxy commit 流程 + TccActionInterceptor 200+行 |
| EventDispatcher | microsphere-java | 8/10 | Direct/Parallel 双模式 + ConditionalEventListener + SPI 加载，**纯 Java 事件系统的最佳实践** |
| Converter SPI | microsphere-java | 8/10 | 30+ 转换器 + 类型继承深度优先级算法，**SPI 设计的教科书** |
| CachingFilteringWebHandler | Stage 3 #20 | 8/10 | 源码级性能瓶颈定位 + MethodHandle 缓存优化 |
| segfault-lessons 自定义 Stream Binder | ActiveMQ 实现 | 8/10 | Binder 接口 bindConsumer/bindProducer 完整实现 |
| segfault-lessons JMX 三种风格 | Boot lesson-17 | 8/10 | Spring @ManagedResource / 标准 MBean / 动态 MBean 全覆盖 |

### 第三梯队：了解即可

- Resilience4j 5 种容错模板（ChainableResilience4jFacade 责任链设计很精巧，但 2026 年大部分场景被 Istio 接管）
- ExecutorFilterChain（position 索引 + process() 递归的责任链实现）
- InterceptingHandlerMethodProcessor（同时实现 4 接口，预计算参数解析器）
- ParallelPreInstantiationSingleton（依赖图分析 → 并行 Bean 初始化）

---

## 二、训练营完全没讲、但你应该会的东西

### 生产必备（零覆盖）
1. **测试策略** — 120 节课零测试。Boot lesson-19 是唯一有测试代码的地方（7 种风格）
2. **数据库迁移** — Flyway/Liquibase
3. **OpenTelemetry** — 替代 Sleuth（概念训练营讲了，实现层要换）
4. **OAuth2/JWT** — 安全模块只有 RBAC 和 CSP/CORS

### 架构师基本功（零覆盖）
5. **ADR** — Architecture Decision Record
6. **部署策略** — 金丝雀/蓝绿/滚动更新
7. **SLO/SLI/Error Budget**

---

## 三、内容质量红黑榜

### 红榜
| 内容 | 评分 |
|------|:----:|
| rpc-project | 10/10 |
| 共识算法论文合集 | 9/10 |
| ZonePreferenceFilter 源码 | 9/10 |
| JVM GC 对比数据 | 9/10 |
| Seata 源码分析 | 9/10 |
| EventDispatcher + Converter SPI | 8/10 |
| CachingFilteringWebHandler | 8/10 |
| Feign 动态刷新 | 8/10 |
| segfault-lessons 自定义 Stream Binder / APT 处理器 | 8/10 |

### 黑榜
| 内容 | 评分 | 理由 |
|------|:----:|------|
| Stage 3 #31-32 Native | 1/10 | 各 3 行/17 行，占位符 |
| Stage 3 #25 Nacos | 2/10 | 215 行未完成草稿，在"配置服务端设计"处中断 |
| Stage 4 #02-06 Eureka 多活 | 2/10 | 6 节基于停更产品 |
| distributed-config-project | 1/10 | 仅 POM 骨架 |
| microsphere-devops/dubbo/apidocs | 0/10 | 空仓库/仅配置 |

---

## 四、学习顺序最终建议

```
第一优先级（地基）：
  自研 RPC → 共识算法 → JVM GC

第二优先级（核心能力）：
  分布式事务 → 多活架构 → EventDispatcher/Converter → 可观测性

第三优先级（实战整合）：
  Shopizer 优化 → 云原生部署 → 平台工程

最后（视野拓展）：
  segfault-lessons 自定义 Binder/APT/Starter
  Feign 动态刷新完整数据流
  CachingFilteringWebHandler 性能优化案例
  补充训练营缺失的 10 项内容
```
