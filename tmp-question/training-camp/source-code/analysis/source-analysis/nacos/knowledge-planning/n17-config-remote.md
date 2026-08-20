# N-17 配置长轮询与 gRPC 通信 — 知识规划 (KP)

> 🔴 A | 模块: config/server/service/LongPollingService (455) + config/server/remote (15) | 版本: 3.0.3

## 01 核心机制提取
| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 长轮询入口 | LongPollingService:63/172-216 | compareMd5 → 挂起 |
| 2 | 提前响应 | LongPollingService:210-212 | max(10s, 请求-500ms) |
| 3 | 订阅者 | ClientLongPolling:264/271 | async 挂起 |
| 4 | 广播 | LongPollingService:78-91 | allSubs 遍历 |
| 5 | gRPC 查询 | ConfigQueryRequestHandler:60 | RequestHandler 泛型 |
| 6 | gRPC 监听 | ConfigChangeBatchListenRequestHandler | 3.x 主线 |
| 7 | 通知 | RpcConfigChangeNotifier | 按连接推送 |
| 8 | 上下文 | ConfigChangeListenContext | 监听状态 |
| 9 | 集群同步 | ConfigClusterRpcClientProxy | 变更传播 |
| 10 | 模糊族 | ConfigFuzzyWatch* 5 | 模糊监听 gRPC 面 |

## 02 高频坑
1. **ClientLongPolling 存在 (config 侧)** — 09 审计只在命名侧找 (表述已修正)
2. timeout = max(10s, 请求值-500ms) 非固定 30s
3. compareMd5 有变化立即响应 (instant)
4. no-hang-up 头短路
5. AsyncContext.setTimeout(0) 自控
6. 批量监听是 3.x 主线
7. 通知按连接推送

## 03 深度分类
| 类别 | 条目 |
|:--|:--|
| 长轮询 | 三出口 (instant/nohangup/hang) / 提前响应 / 超限 503 |
| gRPC | handler 族 / 批量监听 / 上下文 |
| 通知 | RpcConfigChangeNotifier / 按连接 / 模糊族 |
| 集群 | ConfigClusterRpcClientProxy |

## 04 跨域桥接
- ← NC-2: ConfigBatchListenRequest 两端闭合
- ← N-15: 缓存变更消费
- → 面试: "配置变更怎么到客户端" — 长轮询遗留 + gRPC 批量监听
