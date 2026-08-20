# G-5 命名解析 — 知识规划 (KP)

> 域级: 🟡 B | 模块: api/NameResolver (1040) + api/Uri (1184) + core/DnsNameResolver (709) + netty/UdsNameResolver + RetryingNameResolver + Provider
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(生命周期) q2(Uri) q3(DNS 查询+缓存+TXT) q4(Provider+Retrying)

## 一、机制提取 (逐源)

### M1 NameResolver 生命周期 (q1)
- start (DnsNameResolver.java:199-204): 一次性 + **立即 resolve()**; refresh (L207-211)
- API: start L87/123, refresh 默认空 (L146), shutdown (L132) (NameResolver.java)
- **Listener2**: onResult (L294); **onResult2 只能 syncContext** (L271-273); onError (L303, "listener is responsible for eventually invoking refresh()")

### M2 Uri 解析 (q2)
- 组件 percent-encoded 存储 (Uri.java:163-164); 构造器校验 (L185-190, authority/path 规则)
- parse → URISyntaxException (L197-202); create 手写 RFC 3986: scheme (L214-229, "Missing required scheme."), authority (L232-240)
- 对照: java.net.URI 宽松 authority (L119)

### M3 DNS 查询+缓存+TXT (q3)
- resolveAddresses (L212-216): 每地址一个 EAG
- **缓存 TTL 30s 默认** (L108), networkaddress.cache.ttl 可配 (L445-446), Android 固定 (L438-441)
- service config TXT: grpc_config= (L75) + _grpc_config. (L86); A2 提案 (DnsNameResolver.java:462-465 注释)
- JdkAddressResolver (L595)

### M4 Provider + Retrying (q4)
- DnsNameResolverProvider: SCHEME="dns" (L53); target 格式 (L44-47)
- RetryingNameResolver (L30-35): BackoffPolicyRetryScheduler + ExponentialBackoffPolicy (G-6 复用)
- RetryingListener (L96-119): 成功 reset (L102-103)/失败 schedule refresh (L105, L116-118)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 生命周期拉取模型 / M3 DNS 缓存+TXT |
| P2 | M2 Uri 严格解析 / M4 重试闭环 |
| P3 | Provider SPI 细节 / UdsNameResolver (对照) |

## 三、叙事线

场景: `ManagedChannelBuilder.forTarget("dns:///api.example.com:8080")` — 这个字符串怎么变成地址列表?读者疑问链: 谁解析 target (M2) → 解析器怎么被找到/启动 (M1/M4 Provider) → DNS 查什么/缓存多久 (M3) → 失败了怎么办 (M4 Retrying)。
