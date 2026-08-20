# S-11 Redis 自动装配 — RedisAutoConfiguration (接线与客户端选择)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | RedisAutoConfiguration+RedisConnectionConfiguration+LettuceConnectionConfiguration+JedisConnectionConfiguration
> 基线: BOOT-PLAN-v2 S-11 — **只讲接线**(降级项): 连接/协议/数据结构深入在阶段3 Redis(18域) — 前置: S-5 属性绑定(spring.data.redis.*)

---

## §0.8

- 🟡 Working，1篇 — 条件激活(@ConditionalOnClass(RedisOperations)) → 模板装配(RedisTemplate L64: @ConditionalOnMissingBean(name)+@ConditionalOnSingleCandidate(RedisConnectionFactory) + StringRedisTemplate) → 客户端选择(Lettuce @ConditionalOnClass(RedisClient) 默认 / Jedis 条件) → 连接工厂(RedisConnectionConfiguration 基类: spring.data.redis.* → RedisConnectionFactory) → 阶段3 分工
- 设计模式: [模式: 条件装配]—客户端选择; [模式: 用户优先]—@ConditionalOnMissingBean; [模式: 接线]—只装配不深入

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RedisAutoConfiguration.java:48,52 | 条件 | **@ConditionalOnClass(RedisOperations)(L49)**: spring-data-redis 在 classpath 才激活 | High |
| RedisAutoConfiguration.java:61,64 | 模板 | **redisTemplate L64**: @ConditionalOnMissingBean(name="redisTemplate")(L62) + @ConditionalOnSingleCandidate(RedisConnectionFactory)(L63) — 用户模板优先; StringRedisTemplate 同样 | High |
| LettuceConnectionConfiguration.java:67,69 | 客户端 | **Lettuce 配置**: @ConditionalOnClass(RedisClient)(L67 — Lettuce 客户端类在 classpath) — lettuceConnectionFactory L89(@ConditionalOnMissingBean(RedisConnectionFactory)) | High |
| RedisConnectionConfiguration.java:48,183 | 基类 | **连接基类**: getConnectionDetails L183(封装 spring.data.redis.* 属性) → 各客户端配置用它建工厂 | High |
| (阶段3 分工) | 边界 | **深入在阶段3**: 连接池/协议(Redis 18域) — 本域只讲"自动装配接线" | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 自动装配+客户端配置约 400 行 — 知识主线: "条件激活 → 模板装配 → 客户端选择". 1篇 (~44行) 按"激活→模板→客户端→分工"展开; **连接/协议深入标注阶段3 (BOOT-PLAN-v2 降级项)**。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | RedisAutoConfiguration (条件激活 + 模板装配) | 🔴 | **为什么🔴**: RedisTemplate/StringRedisTemplate 的自动装配 — 使用入口 |
| P1-2 | 客户端选择 (Lettuce 默认 vs Jedis) | 🔴 | **为什么🔴**: 两个客户端怎么选 — 依赖驱动 |
| P1-3 | @ConditionalOnSingleCandidate (连接工厂唯一) | 🔴 | **为什么🔴**: 模板装配的前置 — 单一连接工厂 |
| P2-1 | RedisConnectionConfiguration (spring.data.redis.* → 工厂) | 🟡 | **为什么🟡**: 配置接线(S-5 绑定) |
| P2-2 | 与阶段3 Redis 分工 (只讲接线) | 🟡 | **为什么🟡**: 边界声明 — 深入在阶段3 |
| P3-1 | 用户自定义 RedisTemplate/ConnectionFactory | 🟢 | **为什么🟢**: 覆盖方式 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **条件与模板** (激活 + RedisTemplate) | 🔴 | 使用入口装配 |
| B | **客户端选择** (Lettuce/Jedis) | 🔴 | 客户端怎么选 |
| C | **配置与分工** (spring.data.redis.* + 阶段3) | 🟡 | 接线与边界 |

> **Cluster A (§1)**: RedisAutoConfiguration 条件 + RedisTemplate/StringRedisTemplate 装配
> **Cluster B (§2)**: LettuceConnectionConfiguration(默认) vs Jedis — @ConditionalOnClass 选择 + 连接工厂
> **Cluster C (§3)**: RedisConnectionConfiguration(spring.data.redis.* → 工厂) + 与阶段3 分工(连接池/协议深入在阶段3)

→ 引出 S-12: 事务自动配置 — 数据访问延续: DataSourceTransactionManagerAutoConfiguration — 事务管理器自动装配(s29-s33 机制复用)

(End of file - total 61 lines)
