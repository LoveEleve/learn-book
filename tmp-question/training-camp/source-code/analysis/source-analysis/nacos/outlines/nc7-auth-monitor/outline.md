# NC-7 安全+监控+限流/加密 — 横切三面

> 前置: [[NC-1~NC-6]] (横切消费) | 对照: 09 审计修正 (AuthManager 等 1.x 类不存在) | 引出: [[Sentinel-5.9]] (限流对照)
> 🟡 B | 方案 B (重要域) | 闭环: q1(认证插件面) q2(客户端代理) q3(限流/加密)

**读者处境**: Nacos 的认证在 3.x 怎么组织? 客户端登录 token 怎么注入请求? 配置监听回调的限流 (QPS 5) 是谁? 加密数据键 (encryptedDataKey) 怎么容灾?

### 1. 认证插件面 — ProtocolAuthService 族 (09 审计修正)

场景: 3.x 的认证体系长什么样? 规划里的 AuthManager 在哪?
源码路径:
- **09 审计**: AuthManager/PermissionManager/UserManager **不存在于 3.0.3** — auth 模块重构为 **ProtocolAuthService 体系** (28 文件)
- **ProtocolAuthService<R>** (auth/ProtocolAuthService.java:33): **initialize** + **isEnable (authEnabled 主开关)** + validateIdentity + validateAuthority — 泛型 (请求类型)
- **GrpcProtocolAuthService** / **HttpProtocolAuthService**: 双协议实现
- **AbstractProtocolAuthService**: 公共基类
- parser/ 子包: **ResourceParser** 族 (AbstractResourceParser/DefaultResourceParser + grpc/http 变体) — 资源解析
- 扩展: NacosAuthConfig (authEnabled) + plugin 机制
关键设计 (q1): **"协议认证 = gRPC/HTTP 双实现 + 插件可扩展"** — 认证核心抽象为 ProtocolAuthService, 双协议各自实现, 用户可插件化 — 与 NC-2 ConfigChangeParser SPI 同构。 [模式: 协议认证族]

### 2. 客户端安全代理 — SecurityProxy

场景: 客户端怎么登录/携带 token?
源码路径:
- **SecurityProxy** (client/security/SecurityProxy.java:45): implements Closeable — **login(Properties)** (L78: clientAuthService.login) + **getLoginIdentityContext** (L95-117: 按资源取登录上下文)
- 注入: header.put(loginIdentityContext 各键) (L96-97) — 请求头携带
- clientAuthService: 认证服务抽象 (SPI)
关键设计 (q2): **"登录上下文 = 请求头注入"** — 客户端维护登录态, 每次请求把登录上下文写 header; 与 NC-1/NC-2 的代理构造串联。 [模式: 上下文注入]

### 3. 回调限流 — Limiter 的 RateLimiter 缓存

场景: 配置监听回调的限流怎么实现?
源码路径:
- **Limiter** (client/config/impl/Limiter.java:33): **Guava RateLimiter 缓存** (L16-20: CacheBuilder initialCapacity 1000 + expireAfterAccess 1min) + **limit 默认 5 (QPS)** (L47-49, "qps 5" 注释) + **limitTime 属性可配** (L27-31)
- **isLimit(accessKeyId)** (L37-45): CACHE.get(key, RateLimiter.create(limit)) + **tryAcquire(1000ms)** — 超时即限流
- 消费方: 配置监听通知前的闸门 (防回调风暴)
关键设计 (q3): **"按 key 限流 + 缓存衰减"** — 每 accessKeyId 一个 RateLimiter, 缓存 1 分钟过期; 默认 5 QPS 防监听回调风暴。 [模式: 按 key 限流]

### 4. 加密数据键 — LocalEncryptedDataKeyProcessor

场景: 加密配置的 encryptedDataKey 怎么容灾?
源码路径:
- **LocalEncryptedDataKeyProcessor** (client/config/impl/LocalEncryptedDataKeyProcessor.java:38): **extends LocalConfigInfoProcessor** — 复用 failover/snapshot 文件机制
- **getEncryptDataKeyFailover** (L60) / **getEncryptDataKeySnapshot** (L79): 键的 failover/snapshot 双容灾
- failover 路径: FAILOVER_CHILD_2 = "failover" / FAILOVER_CHILD_3 = "failover-tenant" (L44-46)
- 消费方: NC-2 getConfigInner 三路各取加密键 (L225/255)
关键设计 (q4): **"键随内容三路容灾"** — encryptedDataKey 与配置内容走同构的 failover/snapshot 文件路, 解密不因服务端不可达而断。 [模式: 键容灾]

### 5. 监控 — MetricsMonitor 三处

场景: 监控指标在哪定义?
源码路径:
- **MetricsMonitor ×3** (09 审计扩充): client/monitor (getServiceInfoMapSizeMonitor — NC-1 ServiceInfoHolder 消费 L146) / config/server/monitor / core/monitor
- **core/monitor/MetricsMonitor** (core/monitor/MetricsMonitor.java:39): **RAFT 指标族** — RAFT_READ_INDEX_FAILED/RAFT_FROM_LEADER/RAFT_APPLY_LOG_TIMER/RAFT_APPLY_READ_TIMER (L64-79) + longConnection gauge (L84)
- NacosMeterRegistryCenter: 指标注册中心
关键设计 (q5): **"指标三处 = 客户端/配置/核心分层"** — 各模块自持监控, 统一注册中心; RAFT 指标反映 CP 面健康。 [模式: 分层监控]

### 6. 测试与行为锚

场景: 安全的边界行为?
源码路径:
- 测试: SecurityProxyTest / LimiterTest (client/src/test)
- 注释锚: "configuration authEnabled in NacosAuthConfig is the main switch" (ProtocolAuthService) — 主开关语义
- "qps 5" (Limiter:26) — 默认限流值
- 日志锚: "access_key_id:{} limited" (Limiter:41)
关键设计 (q1): **"开关 + 默认值注释"** — 认证主开关与限流默认值都写注释, 行为可预期。 [模式: 行为锚]
