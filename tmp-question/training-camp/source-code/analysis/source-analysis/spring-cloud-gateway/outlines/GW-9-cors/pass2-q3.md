# 闭环笔记 GW-9-q3 — 安全头: SecureHeaders 默认值 + withDefaults 合并

假设: SecureHeaders 工厂给响应加 8+ 安全头 — 路由级配置优先, 全局属性 (SecureHeadersProperties) 兜底 (withDefaults); 默认值有意见 (opinionated)。

验证过程:
- **工厂** (SecureHeadersGatewayFilterFactory.java:43-136): 7 个头常量 (L49-74, 来自 SecureHeadersProperties); apply → assembleHeaders (L105) + **withDefaults** (L107) → applySecurityHeaders 逐头 addHeaderIfEnabled (L127-136)
- **withDefaults** (L226-250): 路由级值拷贝 → **null 时用全局属性 fallback** (L243-248: config.xssProtectionHeaderValue == null → properties.getXssProtectionHeader())
- **默认值 (SecureHeadersProperties)** (L41-91): **X-XSS-Protection "1 ; mode=block"** (L41) / **Strict-Transport-Security "max-age=631138519"** (L51) / **X-Frame-Options "DENY"** (L61) / **Content-Security-Policy 完整策略** (L91, "default-src 'self' https:; ... object-src 'none'; script-src https:")
- **opt-in**: Permissions-Policy (L163, 默认可选)
- 文档引用 (L36-39): "opinionated defaults... sensible defaults are applied"

代码类型: Implementation (响应头安全)

结论: 安全头 = **意见默认值 + 双层配置**: 默认值直接可用 (X-Frame DENY/CSP 全策略); 路由级 > 全局 > 默认三级 fallback。**被放弃的方案: 无默认值 (全手动)** — 安全头默认值防"忘了配" (OWASP 建议)。 [安全: OWASP 头] [跨域: GW-2 过滤器工厂体系] (SecureHeadersGatewayFilterFactory.java:43-136,226-250; SecureHeadersProperties.java:41-163)
