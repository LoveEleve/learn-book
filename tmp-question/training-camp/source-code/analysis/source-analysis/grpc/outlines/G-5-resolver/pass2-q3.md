# 闭环笔记 Q3 — DnsNameResolver: 查询 + 30s 缓存 + TXT service config

假设: DNS 解析器三件事: 地址查询 (JdkAddressResolver) + 缓存 (TTL 30s 可配) + TXT 记录带 service config (A2 提案)。

验证过程:
- **地址查询** (DnsNameResolver.java:212-216): `addressResolver.resolveAddress(host)` (L213) → **每地址一个 EAG** (L216-218, "Each address forms an EAG") — 与 G-4 PickFirstLeaf 每地址一 Subchannel 呼应
- **缓存 TTL** (L436-458): Android 固定 DEFAULT (L438-441); 否则 `System.getProperty("networkaddress.cache.ttl")` (L445-446, 可配) → **DEFAULT_NETWORK_CACHE_TTL_SECONDS = 30** (L108); 非法数值回退默认 (L450-454); cacheTtl > 0 才缓存 (L457)
- **service config via TXT** (L75-86): `SERVICE_CONFIG_PREFIX = "grpc_config="` (L75) + `_grpc_config.` 名称前缀 (L86); enableTxt 开关 (L115)
- **选择逻辑** (L466-470+): maybeChooseServiceConfig — A2 提案 (DnsNameResolver.java:462-465 注释 "Service Config in DNS")
- resolveServiceConfig (L223-242): TXT 记录 → 解析 → serviceConfigParser.parseServiceConfig (L242) — **G-6 retryPolicy 的来源链**
- JdkAddressResolver (L595): InetAddress.getAllByName 封装

代码类型: Implementation (DNS 客户端)

结论: DNS 解析器 = 查询 (JDK API) + **缓存** (30s 默认, 防每 RPC 打 DNS) + **TXT 服务配置** (grpc_config= — 重试/负载均衡策略从 DNS 下发, A2 提案)。**被放弃的方案: 无缓存每次查询** — DNS 查询昂贵且易被限频; 缓存 TTL 让"配置下发"与"地址刷新"统一节奏。 [跨域: G-6 retryPolicy 来源链 (service config → RetriableStream); G-4 每地址 EAG] [标准: DNS TXT/A2 提案] (DnsNameResolver.java:75-86,108,212-242,436-458)
