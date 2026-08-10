# 08 性能实战（Week 15-17 · 9课时）

## 学完能干什么
在真实电商项目 Shopizer 上完成一轮完整的性能优化。

## 项目定位

Shopizer（`cloud-native-code/stage-3/shopizer/`）是一个成熟的 Spring Boot 2.5.12 + Java 11 电商平台，5 个 Maven 模块：

```
sm-core-model  → JPA Entity (15+ 核心实体)
sm-core        → 业务 Service + Repository (60+ Service)
sm-core-modules→ 模块化扩展 (支付/物流/CMS SPI)
sm-shop-model  → DTO/VO 视图模型
sm-shop        → Web 层 (40+ Controller + 6层安全链)
```

### Shopizer 架构模式

**Facade 模式**：Controller 不直接调用 Service，而是通过 Facade 桥接（ProductFacade→ProductService）

**AOP + XML 声明式事务**：
```xml
<!-- get*/list*/search* = 只读事务，其余 = ServiceException 回滚 -->
<tx:method name="get*" read-only="true"/>
<tx:method name="*" rollback-for="ServiceException"/>
```

**6 层安全链**：

| Order | URL | 认证 | 用户 |
|:-----:|-----|------|------|
| 1 | /shop/** | HTTP Basic | 客户 |
| 2 | /services/** | HTTP Basic+Form | 管理员 |
| 5 | /api/v*/private/** | JWT Bearer | 管理员 |
| 6 | /api/v*/auth/** | JWT Bearer | 客户 |

**Entity 关系亮点**：
- Category 自引用树形结构（parent/children + lineage/depth）
- Product @ManyToMany Category + @OneToMany ProductDescription/ProductAvailability
- Order @OneToMany OrderProduct/OrderTotal/OrderStatusHistory
- MerchantStore 多租户核心，几乎所有 Entity 都有 @ManyToOne

**技术栈**：HibernateJpaVendorAdapter + Ehcache 二级缓存 + Infinispan + Elasticsearch 7.5.2 + Drools 7.32.0 + MapStruct + Jackson

## 优化路径

### 第 1-2 步：建基线 + 监控接入
- Shopizer 项目结构熟悉（参考上面的架构分析）
- Micrometer + Prometheus + Grafana 接入
- Sentinel Dashboard 接入
- **JFR 零开销诊断** + JMH 基准测试

### 第 3 步：JVM GC 调优 ⭐ 训练营最强文档
`stage-3/docs/05.`（395 行）— **实测数据**：

| GC | TPS | 适用 |
|----|----:|------|
| ZGC + 500线程 + 80连接池 | **100** | 低延迟 Web 服务 |
| G1 + 500线程 + 80连接池 | 13.4 | 均衡方案 |
| ZGC 参数：-XX:+UseZGC -XX:ConcGCThreads=N -XX:+UseLargePages |

### 第 4 步：微服务化 → 协议升级
- 单体 Shopizer → Nacos 注册发现
- HTTP 升级：Servlet 3.0→3.1→4.0(HTTP/2)
- RPC 升级：gRPC + Dubbo Triple 协议

### 第 5 步：数据层优化
- MyBatis 替换 JPA（N+1 查询优化）
- MySQL MGR 高可用（Paxos 复制）
- ShardingSphere 分库分表

### 第 6 步：异步化 + Spring 运行时优化
- Kafka 分布式事件 + WebFlux Reactive
- **AOP 静态代理替换**（DecoratingUserRegistrationService 装饰器模式）
- **@RefreshScope 替换**（ConfigurationPropertiesRebinder 自动触发）

### 第 7 步：Gateway 性能优化 ⭐
`stage-3/docs/20.`（331 行）— **CachingFilteringWebHandler**：
- 问题定位：FilteringWebHandler 每次请求创建 ArrayList（数组复制+排序+Annotation 反射）、DefaultGatewayFilterChain 每次新建对象
- 优化方案：MethodHandle 反射缓存 + 缓存合并后的 GatewayFilter + volatile HashMap 替换 + RefreshRoutesResultEvent 监听更新

### 额外：Dubbo 10 层架构
`stage-3/docs/23.`（403 行）— Config→Proxy→Registry→Cluster→Monitor→Protocol→Exchange→Transport→Serialize 十层完整剖析

### 额外：JDK 9-21 变迁
`stage-3/docs/33.`（657 行）— 按"语言-API-JVM"三维分类，每个版本标 JEP 编号

**动手**：
```bash
cd cloud-native-code/stage-3/shopizer && mvn compile
```

> 跳过：Stage3 #25（Nacos 概念 215 行无代码）、#30（12 行）、#31-32（3行/17行占位）
