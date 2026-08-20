# NC-7 安全+监控+限流/加密 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 NC-7 核心类含 AuthManager/PermissionManager/UserManager | **09 审计: 三者不存在于 3.0.3** — auth 重构为 ProtocolAuthService 体系 (Abstract/Grpc/Http) + ResourceParser 族 — 大纲按实测重写 |
| 2 | 规划 "MetricsMonitor 监控指标" 单处 | 09 审计: **三处** (client/config/core) — 大纲 §5 分层精确化 |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "Limiter 是自定义限流" | 实测: **Guava RateLimiter + CacheBuilder 缓存 (1000/1min) + 默认 5 QPS + limitTime 可配** — 第三方库复用精确化 |

## 审 3: 文件名/目录名推断 — 1 修正

| # | 推断 | 实测 |
|:--:|:--|:--|
| 1 | 规划把 SecurityProxy 归 "client/security" | 实测确认 + **clientAuthService SPI 抽象 + LoginIdentityContext 注入面 (L95-97) 补齐** |

## 审 4: 跨项目概念转移 — 0

## 审 5: 覆盖率 — 0 缺漏 (认证/代理/限流/加密/监控 6 面全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过
