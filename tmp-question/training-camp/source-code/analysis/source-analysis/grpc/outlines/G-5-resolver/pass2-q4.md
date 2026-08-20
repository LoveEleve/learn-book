# 闭环笔记 Q4 — Provider 注册与 RetryingNameResolver: 错误→退避→refresh 闭环

假设: Provider 按 scheme 注册 (dns/uds); RetryingNameResolver 是"解析失败自动退避重试"的包装 — G-6 退避复用。

验证过程:
- **Provider** (DnsNameResolverProvider.java:51-53): `SCHEME = "dns"` (L53); getDefaultScheme (L98)/isAvailable (L103) — NameResolverRegistry 按 scheme 匹配
- **target 格式** (L44-47 注释): `dns:///foo.googleapis.com:8080` (默认 DNS) / `dns://8.8.8.8/...` (指定 DNS 服务器, 注释: "not implemented") / 无端口 → 默认端口 (L47)
- **RetryingNameResolver** (RetryingNameResolver.java:30-35): extends ForwardingNameResolver + **BackoffPolicyRetryScheduler(new ExponentialBackoffPolicy.Provider())** (L35-36, **G-6 退避复用**)
- **start 包装** (L60-63): super.start(new RetryingListener(listener)) — 拦截 listener
- **RetryingListener** (L96-119): onResult2 → `delegateListener.onResult2` → 成功 → `retryScheduler.reset()` (L102-103, 重置退避); 失败 → `retryScheduler.schedule(new DelayedNameResolverRefresh())` (L105); **onError → schedule refresh** (L116-118) — 错误后自动退避重试 (q1 的 "Listener 负责重试" 的默认实现)
- shutdown → retryScheduler.reset (L65-69)

代码类型: Implementation (重试包装)

结论: **解析失败闭环**: onError/onResult 非 OK → BackoffPolicyRetryScheduler (1.6x 退避, G-6) → DelayedNameResolverRefresh → refresh() 重解析; 成功则 reset 退避。**被放弃的方案: 解析器内部 try-catch 重试** — 包装器模式 (ForwardingNameResolver) 让任意解析器自动获得重试能力, 解析器自身零改动。 [跨域: G-6 退避复用 (同 ExponentialBackoffPolicy); G-3 通道侧集成] [模式: 装饰器] (RetryingNameResolver.java:30-119; DnsNameResolverProvider.java:51-53)
