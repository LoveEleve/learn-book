# S-11 Redis 自动装配 — RedisAutoConfiguration (接线与客户端选择)

> 依赖 S-5 (属性绑定) | 🟡 Working | 6 KP | [模式: 条件装配 + 用户优先]

**读者处境**: 引 starter-data-redis 就有 RedisTemplate — 怎么激活?Lettuce/Jedis 怎么选?自定义模板怎么覆盖? — 连接/协议深入在阶段3, 本域只讲接线。

### 1. 条件激活与模板装配

场景: 自动装配激活条件 + RedisTemplate/StringRedisTemplate 的装配。

源码路径:
- `RedisAutoConfiguration.java:48,52` — **条件**: @ConditionalOnClass(RedisOperations)(L49 — spring-data-redis 在 classpath) — 引 starter-data-redis 即有
- `RedisAutoConfiguration.java:61,64` — **模板**: redisTemplate L64: @ConditionalOnMissingBean(name="redisTemplate")(L62, 用户自定义则跳过) + @ConditionalOnSingleCandidate(RedisConnectionFactory)(L63, 连接工厂唯一才配模板) — StringRedisTemplate 同样

关键设计: **Why @ConditionalOnSingleCandidate？** RedisTemplate 依赖 RedisConnectionFactory — 若有多个连接工厂(多 Redis 实例), 模板不知道该用哪个, 条件不满足不自动配 — 避免歧义(与 C-2 依赖注入的歧义处理同思路)。[模式: 条件装配 + 单候选]

数据流: 引 starter-data-redis → @ConditionalOnClass(RedisOperations)→激活 → 容器有唯一 RedisConnectionFactory(§2)→ redisTemplate(L64) 装配 → @Autowired RedisTemplate 可用。用户自定义 redisTemplate bean → @ConditionalOnMissingBean 跳过。

### 2. 客户端选择 — Lettuce vs Jedis

场景: 两个 Redis 客户端库(Lettuce 默认/Jedis 可选)— 怎么选?

源码路径:
- `LettuceConnectionConfiguration.java:67,69` — **Lettuce**: @ConditionalOnClass(RedisClient)(L67 — Lettuce 的 RedisClient 类在 classpath) → lettuceConnectionFactory L89(@ConditionalOnMissingBean(RedisConnectionFactory) — 用户自定义工厂优先)
- `JedisConnectionConfiguration` — **Jedis**: @ConditionalOnClass(Jedis)(对应类)— 引 Jedis 依赖则走它
- 默认: starter-data-redis 默认引入 **Lettuce** — 换 Jedis = 换依赖

关键设计: **Why 依赖驱动选择？** 与 S-8 容器选择同模式: classpath 有谁的客户端类用谁; 默认 Lettuce(线程安全/响应式支持), Jedis 是替代。**Why @ConditionalOnMissingBean？** 用户自定义 RedisConnectionFactory(如连集群)优先。[模式: 条件装配]

数据流: starter-data-redis 默认带 lettuce-core → @ConditionalOnClass(RedisClient)→Lettuce 配置激活 → lettuceConnectionFactory: new LettuceConnectionFactory(standalone 配置/集群) → RedisConnectionFactory bean。换 jedis 依赖(排除 lettuce)→ Jedis 配置激活。

### 3. 配置接线与阶段3 分工

场景: spring.data.redis.* 怎么到连接工厂?为什么本域只讲接线?

源码路径:
- `RedisConnectionConfiguration.java:48,183` — **基类**: getConnectionDetails L183 — 封装 spring.data.redis.* 属性(host/port/password 等, S-5 绑定) → 各客户端配置类用它构造连接工厂(standalone/sentinel/cluster 配置)
- 分工: 本域只讲"自动装配怎么接线"(条件/模板/客户端/属性→工厂); **连接池细节/协议/数据结构/序列化深入在阶段3 Redis(18域)**

关键设计: **Why 只讲接线？** BOOT-PLAN-v2 降级原则: Redis 自动装配是"胶水"(接线), 深挖价值在阶段3 的 Redis 本身(连接池/持久化/集群)— 本域标注边界, 避免与阶段3 重复(06 §2.5 同源: 不重复讲)。[模式: 边界声明]

数据流: spring.data.redis.host=localhost/port=6379 → S-5 绑定到 RedisProperties → RedisConnectionConfiguration.getConnectionDetails(L183) → LettuceConnectionFactory(host/port) → RedisConnectionFactory → RedisTemplate 使用 → 序列化/操作深入在阶段3。

→ 引出 S-12: 事务自动配置 — 数据访问延续: DataSourceTransactionManagerAutoConfiguration — 事务管理器自动装配(s29-s33 事务链机制复用)。
