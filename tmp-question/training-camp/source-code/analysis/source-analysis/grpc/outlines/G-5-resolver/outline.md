# G-5 命名解析 — 从 target 字符串到地址列表: NameResolver 的拉取模型

> 前置: [[G-3-客户端]] (exitIdleMode 触发 start) + [[G-6-流控重试]] (退避复用) | 引出: [[G-4-负载均衡]] (地址消费, 下一域) + [[G-7-xDS]] (XdsNameResolver 第二实现) | 对照: Dubbo Registry (阶段5.2) + java.net.URI
> 🟡 B | 4 KP | [模式: 拉取 + 回调 + 装饰器]
> Pass 2 闭环: q1(生命周期) q2(Uri) q3(DNS) q4(Retrying)

**读者处境**: `ManagedChannelBuilder.forTarget("dns:///api.example.com:8080")` — 一行字符串。通道怎么知道"api.example.com:8080"对应哪些 IP?什么时候去查?查不到怎么办?解析器、DNS、缓存、TXT 记录——这条链路怎么串起来?

### 1. 生命周期 — start 即解析, 错误后谁重试?

场景: 解析器什么时候工作?解析失败后谁负责再试?
源码路径:
- **DnsNameResolver.start** (DnsNameResolver.java:199-204): `checkState(listener == null, "already started")` (L200, 一次性) → executor 池取 (L201) → 记录 listener (L202) → **立即 resolve()** (L203) — start 即首轮解析
- **refresh** (L207-211): "not started" 检查 → resolve()
- **API** (NameResolver.java): start L87/123; **refresh() 默认空实现** (L146); shutdown 抽象 (L132)
- **Listener2.onError** (L303): "The listener is responsible for eventually invoking refresh() to re-attempt resolution"
- **onResult2 只能 syncContext** (L271-273 注释, G-3 模型)
关键设计 (q1): **拉取模型**: start 触发 → 结果/错误经回调 → 错误后由**通道侧决定何时 refresh**; 解析器自己无定时重试 — 重试节奏 (退避) 是策略, 上层管。**被放弃的方案: 解析器内部自动重试** — 职责耦合。**第二实现对照: netty/UdsNameResolver (scheme=uds, Unix Domain Socket)** — 同一 SPI 的不同地址源, 佐证"解析器 = 地址源适配器"。 [跨域: G-3 exitIdleMode; G-6 退避]

### 2. Uri — gRPC 自研 RFC 3986 解析器

场景: "dns:///host:port?param=value" 怎么拆?
源码路径:
- **组件存储** (Uri.java:162-191): "Components are stored percent-encoded, just as originally parsed for transparent parse/toString round-tripping" (L163-164)
- **严格校验** (L185-190): 有 authority → 非空 path 必须 '/' 开头; 无 authority → path 不能 '//' — 构造器统一强制
- **parse** (L197-202): IllegalArgumentException → URISyntaxException
- **create 手写解析** (L208-240): **3.1 Scheme** — "Look for a ':' before '/', '?', or '#'" (L214-229, 缺 scheme 报 "Missing required scheme."); **3.2 Authority** — "//" 后扫到分隔符 (L232-240)
关键设计 (q2): **自研严格解析器**: 组件 percent-encoded 无损往返; 校验规则构造器强制; 手写文法按 RFC 3986 3.1/3.2。**被放弃的方案: java.net.URI** — 其宽松 authority 接受任意字符串 (L119 注释), 与 target 严格性冲突; 缺查询参数 API。 [标准: RFC 3986] [跨域: G-3 Builder target 入口]

### 3. DNS 查询与缓存 — 30 秒的平衡

场景: 每次 RPC 都查 DNS 吗?service config 怎么从 DNS 来?
源码路径:
- **查询** (DnsNameResolver.java:212-216): `addressResolver.resolveAddress(host)` (L213) → **每地址一个 EAG** (L216-218)
- **缓存 TTL** (L436-458): **DEFAULT_NETWORK_CACHE_TTL_SECONDS = 30** (L108); `networkaddress.cache.ttl` 系统属性可配 (L445-446); Android 固定 (L438-441); 非法数值回退默认 (L450-454)
- **service config via TXT** (L75-86): `grpc_config=` (L75) + `_grpc_config.` (L86); A2 提案 (DnsNameResolver.java:462-465 注释); resolveServiceConfig → serviceConfigParser (L223-242)
- JdkAddressResolver (L595)
关键设计 (q3): **30s 缓存平衡**: 不每次查询 (DNS 昂贵) 也不长期缓存 (地址漂移); **TXT 服务配置**: retryPolicy (G-6)/负载均衡策略从 DNS 下发 — 配置与地址同一刷新节奏。**被放弃的方案: 无缓存** — DNS 查询昂贵易限频。 [跨域: G-6 retryPolicy 来源链; G-4 每地址 EAG] [标准: DNS TXT/A2]

### 4. Provider 与重试闭环 — 失败后自动退避再解析

场景: DNS 挂了, 解析失败, 通道怎么办?
源码路径:
- **Provider** (DnsNameResolverProvider.java:51-53): `SCHEME = "dns"`; target 格式 (L44-47): `dns:///foo.googleapis.com:8080` / `dns://8.8.8.8/...` (注释 "not implemented") / 无端口默认
- **RetryingNameResolver** (RetryingNameResolver.java:30-35): **BackoffPolicyRetryScheduler(new ExponentialBackoffPolicy.Provider())** (L35-36, **G-6 退避复用**)
- **RetryingListener** (L96-119): 成功 → `retryScheduler.reset()` (L102-103); 失败/onError → `retryScheduler.schedule(new DelayedNameResolverRefresh())` (L105,116-118) — **错误→退避→refresh 闭环**
关键设计 (q4): 解析失败闭环: onError → 1.6x 退避 (G-6) → 定时 refresh; 成功 reset。**被放弃的方案: 解析器内部 try-catch** — 装饰器 (ForwardingNameResolver) 让任意解析器零改动获得重试。 [跨域: G-6 退避; G-2 的 UDS 解析器 (netty) 同 SPI] [模式: 装饰器]

### 核心悬念

"DNS 只是解析器的一种 — xds:/// 的 XdsNameResolver (1206 行) 比 DnsNameResolver 大近一倍, 它从控制面拉地址。" 下一域 [[G-7-xDS]] 将看到控制面驱动的解析与负载均衡。

### 负面空间 (不做)

1. 不写 DNS 协议细节 (UDP/递归查询) — 只讲 gRPC 封装
2. 不写 NameResolverRegistry 的 SPI 加载机制穷举
3. 不写 UdsNameResolver 细节 (对照面, 一行带过)
4. 不写 service config 校验规则穷举 (支撑面)
5. 不写 java.net.URI 全 API 对照 (只讲差异点)
6. 不写 JNDI/Android 平台差异细节
