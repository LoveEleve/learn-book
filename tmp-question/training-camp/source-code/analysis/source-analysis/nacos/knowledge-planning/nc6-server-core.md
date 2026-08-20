# NC-6 服务端核心 — 知识规划 (KP)

> 🟡 B | 模块: naming (271) + config/server (243) | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 启动入口 | NamingApp | scanBasePackages naming+core |
| 2 | 注册入口 | InstanceController:127 | → InstanceOperator |
| 3 | 客户端模型 | InstanceOperatorClientImpl:106-113 | clientOperationService + clientId |
| 4 | 心跳检查 | HealthCheckReactor:56-64 | futureMap 去重 + 5s 周期 |
| 5 | v2 健康 | HealthCheckReactor:48-54 | HealthCheckTaskV2 + 拦截包装 |
| 6 | 双推送 | UdpPushService + push/v2 | UDP 遗留 + gRPC 新面 |
| 7 | 订阅服务 | NamingSubscriberService | Local/Aggregation 双实现 |
| 8 | 双存储路由 | consistency/ephemeral+persistent | Distro (AP) / JRaft (CP) |
| 9 | 配置入口 | ConfigController | publishConfig/getConfig |
| 10 | trace 事件 | InstanceController:155 | DeregisterInstanceTraceEvent |

## 02 高频坑

1. TcpSuperSenseProcessor 已不存在 (1.x) — 3.x 用 HealthCheckReactor
2. UDP 推送是遗留, 主线 gRPC
3. ephemeral→Distro / persistent→JRaft 双落点
4. 3.x 以 clientId 为核心 (长连接绑定)
5. BeatCheckTask 5 秒周期, futureMap 防重复
6. config/server 控制器与存储分离
7. scanBasePackages 跨 naming+core

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 入口 | Controller → Operator → ClientService 三层 / v2/v3 变体 |
| 健康 | HealthCheckReactor / BeatCheckTask / v2 任务 / 拦截器 |
| 推送 | UDP 遗留 / gRPC v2 / 本地-聚合订阅 |
| 存储 | ephemeral/persistent 路由 / Datum/KeyBuilder |
| 配置 | ConfigController / CommunicationController / 存储服务 |
| 可观测 | trace 事件 / [CANCEL-CHECK] 日志 |

## 04 跨域桥接

- ← NC-1/NC-2: 客户端两端闭合 (注册链/配置链)
- ← NC-5: 一致性双轨落点实证
- → NC-7: 安全/监控横切
- → 面试: "Nacos 服务端架构" — 三层入口 + 双推送 + 双存储
