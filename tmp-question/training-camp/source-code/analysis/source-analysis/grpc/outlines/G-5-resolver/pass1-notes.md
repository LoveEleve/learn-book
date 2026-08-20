# G-5 Pass 1 扫描笔记 — 命名解析

> 日期: 2026-08-16 | 版本: 1.83.1 | 🟡 B | 模块: api/NameResolver (1040) + api/Uri (1184) + core/DnsNameResolver (709) + netty/UdsNameResolver + RetryingNameResolver + Provider

## 继承树/调用图

```
ManagedChannelImpl.exitIdleMode → nameResolver.start(listener) (G-3)
  ├── DnsNameResolver (65, extends NameResolver): start (L199)/refresh (L207)
  │     ├── ResolutionTask → resolveInternal → DNS 查询
  │     ├── 缓存: cacheTtlNanos (L182, getNetworkAddressCacheTtlNanos L436)
  │     ├── JdkAddressResolver (L595) / NetworkAddressResolver
  │     └── serviceConfig 解析 (L223, TXT 记录)
  ├── UdsNameResolver (netty, Unix Domain Socket)
  └── RetryingNameResolver (G-6 退避复用)
NameResolver (62, api):
  ├── getServiceAuthority (L74)/start/shutdown/refresh (L132)
  ├── Listener2 (L256) — onResult2/onError
  └── Factory/Provider SPI
Uri (1184, api) — target 解析: scheme://authority + query
```

## 基本元素分解

1. **NameResolver API**: start/refresh/shutdown 生命周期 + Listener 回调 (onResult2/onError)
2. **DnsNameResolver**: DNS 查询实现 — 缓存 (TTL)/JdkAddressResolver/TXT service config
3. **Uri**: gRPC target 解析 (dns:///host:port?param=value)
4. **Provider/Registry**: 按 scheme 找解析器 (dns/uds)
5. **RetryingNameResolver**: 解析失败退避重试 (G-6 关联)

## 标记问题 (6)

1. **Q1 NameResolver API**: start/refresh/shutdown 生命周期 + Listener2 回调契约; 与 G-3 的 nameResolver.start(listener) 集成点
2. **Q2 Uri target 解析**: "dns:///host:port" 格式 — Uri (1184) 的 scheme/authority/query 解析; 非法 target 错误
3. **Q3 DNS 查询与缓存**: JdkAddressResolver (L595); 缓存 TTL (L436, Android 差异?); neverCache 测试 (L224)
4. **Q4 TXT service config**: resolveServiceConfig (L223) — DNS TXT 记录带 service config (grpc_config)
5. **Q5 Provider 注册**: DnsNameResolverProvider (L51) — scheme 注册/优先级
6. **Q6 RetryingNameResolver**: 解析失败重试 (G-6 退避复用)

## 已读测试

- DnsNameResolverTest: nullDnsName (L204)/invalidDnsName_containsUnderscore (L214)/resolve_neverCache (L224)/testExecutor_default (L255)
- ManagedChannelImplGetNameResolverTest 存在
