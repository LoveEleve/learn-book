# Phase 5：架构设计能力 + my-xhs 架构升级（第 33-40 周）

## 目标
从"实现者"到"设计者"。掌握系统设计方法论，同时完成 my-xhs 最重要的技术升级：Java 21 + Virtual Threads + GraalVM。

---

## Week 33：系统设计方法论

### 标准答题模板（7 步法）
1. **需求澄清**：功能需求 + 非功能需求（QPS/延迟/一致性/可用性）
2. **容量估算**：DAU → QPS → 存储 → 带宽
3. **接口设计**：REST API 或 RPC 定义
4. **数据模型**：表结构、索引、分片策略
5. **架构选型**：微服务拆分、中间件选择，每个选择说明 WHY
6. **详细设计**：核心流程时序图、容错/降级/监控
7. **边界处理**：极端场景（热点/数据倾斜/网络分区/宕机恢复）

### 本周练习：2 道系统设计题

**题目 1：设计一个短链服务（TinyURL）**
- 要求：支持 1 亿日活，短链长度 ≤ 8 个字符
- 涉及：Base62 编码、预生成策略、布隆过滤器、缓存层

**题目 2：设计一个分布式限流器**
- 要求：支持全局限流 + 按用户限流，P99 < 1ms
- 涉及：滑动窗口算法、Redis Lua 原子操作、令牌桶 vs 漏桶

### 实践任务
每道题输出：架构图（ASCII/流程图）+ 伪代码（核心接口）+ 容量估算表

---

## Week 34：ID 生成器与限流器深度设计

### 必读文件
```
my-xhs-common/src/main/java/com/myxhs/common/id/
├── SegmentIdGenerator.java          // CosId 号段模式
└── IdGeneratorUtil.java             // ID 工具类
```

### CosId 号段模式深度走读
- **双 Buffer 设计**：当前号段用完时，异步预加载下一个号段
- **号段大小**：`cosid.segment.share.biz-name.user.step=1000`
- **安全性**：号段用完时同步等待（防止预加载未完成）
- **对比 Snowflake**：
  - Snowflake：时钟回拨问题、WorkerId 分配、趋势递增
  - 号段模式：强递增、无时钟依赖、依赖 DB（单点风险）

### 实践任务
1. 手写滑动窗口限流器（`SlidingWindowRateLimiter.java`，~200 行）：
   - 基于 Redis Lua 脚本
   - Java 封装 `RateLimiterClient`
   - JMH 压测对比 Guava RateLimiter / Sentinel
2. 对比 my-xhs 的 `@RateLimit` 实现与自己手写的差异

---

## Week 35：海量数据系统设计

### 题目：短链系统完整设计方案
输出标准设计文档：
- API 设计：`POST /shorten` / `GET /{shortCode}`
- DB 设计：短链映射表（short_code → long_url → user_id → created_at）
- 分片策略：short_code hash → 256 分片
- 缓存层：Redis（short_code → long_url）+ 布隆过滤器
- 热点处理：热门短链本地缓存（Caffeine）
- 容量估算：1 亿链接 → 每条 512B → ~50GB → 16 分片 × ~3GB/片

### Feed 流推拉结合（对比 my-xhs-home）
- **推模式**（fan-out on write）：大 V 发布 → 写入所有粉丝收件箱
- **拉模式**（fan-in on read）：用户刷新 → 聚合关注者最新内容
- **推拉结合**：粉丝数 > 阈值用推，否则用拉
- **my-xhs 实践**：看 `FeedService` 如何实现 2 层并行聚合

---

## Week 36：搜索引擎与推荐系统

### 搜索优化
- ES 拼音分析器：`analysis-pinyin` 插件
- 同义词词典：`synonyms/fashion.txt`
- Completion Suggester：前缀匹配，多字段索引
- 搜索权重：标题 > 标签 > 内容

### 推荐 Pipeline（四阶段）
```
召回（5 个策略） → 粗排（CTR 预估） → 精排（深度学习） → 重排（MMR 多样性）
```
- 协同过滤：UserCF（用户相似度）、ItemCF（物品相似度）
- 内容推荐：基于标签/分类的向量相似度
- 热度推荐：时间衰减的加权分数
- MMR 重排：平衡相关性与多样性

---

## Week 37：多活架构设计

### 必读文件
```
my-xhs-common/src/main/java/com/myxhs/common/zone/
├── ZoneContext.java                  // Zone 上下文
├── ZonePreferenceFilter.java         // Zone 优先过滤器（核心 10 步算法）
└── ZoneLocator.java                  // Zone 定位器接口
```

### ZonePreferenceFilter 10 步决策算法
1. 实体数 ≤ 1 → 直接返回
2. 未启用 → 直接返回
3. 当前 Zone 为空/DEFAULT → 忽略 Zone
4. 过滤禁用 Zone
5. 统计同 Zone 实体数
6. 计算同 Zone 比例 = 同 Zone 数 / 总数
7. 同 Zone 比例 ≥ zone-ready-percentage(80%) → 返回同 Zone
8. 同 Zone 数 ≥ min-available → 返回同 Zone
9. 否则返回全部（跨 Zone 降级）
10. 可选：返回备选 Zone（按优先级排序）

### my-xhs 两机房多活拓扑设计
```
GeoDNS → GSLB → Zone-A Gateway (active) → Zone-A Services → Zone-A DB(主) / Redis
              → Zone-B Gateway (standby)→ Zone-B Services → Zone-B DB(从) / Redis
数据复制：MySQL 主主双向复制 + ES CCR 跨集群复制 + Redis 多活复制器
```

### 实践任务
1. 深度走读 `ZonePreferenceFilter` 全部 10 步代码
2. 设计 my-xhs 的完整多活部署方案（含网络拓扑、数据复制、流量调度、故障切换 SOP）
3. 输出：《my-xhs 多活架构设计方案》

---

## Week 38：Java 21 升级 + Virtual Threads + GraalVM Native Image

> **这是整个训练计划中最重要的改造周！**

### 步骤 1：Java 版本升级（预计 2 天）
修改所有 `pom.xml`：
```xml
<java.version>21</java.version>
<spring-boot.version>3.3.5</spring-boot.version>
<spring-cloud.version>2023.0.3</spring-cloud.version>
<spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
```

受影响文件清单：根 POM + 15 个微服务 POM + BOM POM = 约 18 个文件

### 步骤 2：javax → jakarta 迁移
```bash
# 批量替换脚本
find . -name "*.java" -exec sed -i \
  -e 's/javax\.servlet/jakarta.servlet/g' \
  -e 's/javax\.persistence/jakarta.persistence/g' \
  -e 's/javax\.validation/jakarta.validation/g' \
  -e 's/javax\.annotation/jakarta.annotation/g' {} \;
```

### 步骤 3：Virtual Threads 改造
```yaml
# application.yml
spring.threads.virtual.enabled: true  # Tomcat 使用虚拟线程
```

```java
// 异步任务线程池改为虚拟线程
@Bean
public Executor taskExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor();
}

// @Async 方法自动使用虚拟线程
@Async
public CompletableFuture<Result> asyncProcess() { ... }
```

### 步骤 4：压测对比
| 并发线程 | 平台线程 P99 | 虚拟线程 P99 | 平台线程内存 | 虚拟线程内存 |
|---------|------------|------------|------------|------------|
| 100 | 50ms | 52ms | 500MB | 200MB |
| 1,000 | 230ms | 180ms | 2.5GB | 350MB |
| 10,000 | OOM | 1,200ms | N/A | 800MB |

### 步骤 5：GraalVM Native Image 试点
选取 2 个无状态微服务（gateway + common）做 Native 编译：
```bash
mvn -Pnative native:compile
```
对比：启动时间（2s vs 0.05s）、内存（500MB vs 50MB）、编译时间、不兼容库清单

### 实践任务
1. Java 17 → 21 完整升级 PR（18 个文件）
2. Virtual Threads 改造 + 压测对比报告
3. GraalVM Native Image 试点 + 兼容性报告

---

## Week 39：Service Mesh 接入

### Istio 接入 my-xhs
- VirtualService：根据 Header `X-Gray-Tag` 分流到灰度版本
- DestinationRule：定义灰度版本（v2）和稳定版本（v1）的流量权重
- EnvoyFilter：Sentinel 网关限流移交 Istio 承担部分职责

### 三方对比
| 维度 | 传统微服务 | Istio Sidecar | Proxyless Mesh (Dubbo Triple) |
|------|----------|--------------|-----|
| 侵入性 | 代码入侵 | Sidecar 注入 | 无侵入 |
| 性能开销 | 无额外 | +10% CPU | +5% CPU |
| 运维复杂度 | 各服务独立配置 | 统一控制面 | 简单 |
| 适用场景 | 小规模 | 大规模多语言 | 仅 Java/Dubbo |

### 实践任务
1. my-xhs 接入 Istio（VirtualService + DestinationRule）
2. 配置灰度发布：v1 90% + v2 10% 流量
3. 对比 Sentinel Dashboard 限流 vs Istio EnvoyFilter 限流

---

## Week 40：Phase 5 阶段交付

### 交付物清单
- [ ] 《my-xhs V2 架构设计文档》（含 Java 21 升级、Virtual Threads、Native Image 报告）
- [ ] Java 21 升级 PR（18 个文件修改）
- [ ] Virtual Threads 改造 + 压测对比报告
- [ ] GraalVM Native Image 试点报告
- [ ] 系统设计题库 50+ 题（含答案框架）
- [ ] my-xhs 多活架构设计方案
- [ ] Service Mesh 接入方案

---

## Phase 5 检验标准
- [ ] 能在一小时内完成一个系统设计题的完整方案（含架构图 + 容量估算 + 接口设计）
- [ ] my-xhs 成功升级到 Java 21 + Spring Boot 3.3，0 编译错误
- [ ] Virtual Threads 在 10,000 并发下 P99 ≤ 1.5s
- [ ] GraalVM Native Image 成功编译 gateway 服务，启动 < 100ms
- [ ] K8s 部署 + HPA 自动扩缩容
- [ ] 多活架构切换可在 30 秒内完成
