# G-5 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| Listener2 @since 1.21.0 | NameResolver.java:294 | **onResult2/ResolutionResult 是 1.21 演进** — 旧 Listener.onAddresses 已 @Deprecated + @InlineMe (L262-278) |
| "This will be removed in 1.22.0" | NameResolver.java:262 | 老 API 移除计划 (实际保留更久 — 兼容面) |
| Helper @since 1.22.0 (G-4) | 同代 | 1.21-1.22 是 API 现代化窗口 |
| networkaddress.cache.ttl | DnsNameResolver.java:105 | 缓存 TTL 配置演进 (早期固定) |
| JNDI TXT property | DnsNameResolver.java:92 | service config via DNS 是后加能力 |

## 写书建议

呈现"解析 API 演进": 老 Listener.onAddresses (已废弃) → Listener2.onResult2 (1.21, ResolutionResult 统一地址+属性+service config) — 现代 API 收敛为"一次回调给全量结果"。
