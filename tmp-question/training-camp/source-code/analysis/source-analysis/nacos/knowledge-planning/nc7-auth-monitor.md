# NC-7 安全+监控+限流/加密 — 知识规划 (KP)

> 🟡 B | 模块: auth (28) + client/security + client/config/impl + 三处 monitor | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 协议认证 | ProtocolAuthService | initialize/isEnable/validate |
| 2 | 双协议实现 | Grpc/HttpProtocolAuthService | gRPC/HTTP 各自认证 |
| 3 | 资源解析 | ResourceParser 族 | grpc/http 变体 |
| 4 | 客户端代理 | SecurityProxy:45/78/95 | login + 上下文注入 header |
| 5 | 回调限流 | Limiter:16-20/47-49 | Guava RateLimiter 缓存 + 5 QPS |
| 6 | 加密键容灾 | LocalEncryptedDataKeyProcessor:38/60/79 | extends LocalConfigInfoProcessor |
| 7 | 核心监控 | core/monitor/MetricsMonitor:64-84 | RAFT 指标族 + longConnection |
| 8 | 客户端监控 | client/monitor/MetricsMonitor | serviceInfoMapSize |
| 9 | 配置监控 | config/server/monitor | 配置面指标 |
| 10 | 注册中心 | NacosMeterRegistryCenter | 统一指标注册 |

## 02 高频坑

1. AuthManager/PermissionManager/UserManager 不存在 (1.x) — 3.x 是 ProtocolAuthService
2. authEnabled 是认证主开关
3. Limiter 默认 5 QPS, limitTime 可配
4. encryptedDataKey 独立容灾 (failover/snapshot)
5. 三处 MetricsMonitor 分层 (client/config/core)
6. SecurityProxy 登录上下文按请求注入
7. ResourceParser 分 grpc/http

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 认证 | ProtocolAuthService / 双协议实现 / 主开关 / 插件面 |
| 代理 | SecurityProxy / login / LoginIdentityContext 注入 |
| 限流 | RateLimiter 缓存 / 5 QPS / tryAcquire 1000ms |
| 加密 | encryptedDataKey failover/snapshot / 路径结构 |
| 监控 | 三处分层 / RAFT 指标 / 统一注册中心 |
| 扩展 | ResourceParser SPI / clientAuthService SPI |

## 04 跨域桥接

- ← NC-1/NC-2: SecurityProxy 注入头 / encryptedDataKey 消费 (三路)
- → Sentinel 5.9: 限流框架对照
- → 面试: "Nacos 安全/限流/加密" — 协议认证 + 回调限流 + 键容灾
