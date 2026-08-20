# N-12 推送面 — 知识规划 (KP)

> 🔴 A | 模块: naming/push (31 文件 2,771 行) | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 执行器族 | PushExecutorDelegate:36-42 | rpc+udp 双持有 |
| 2 | SPI 优先 | PushExecutorDelegate:66-71 | 自定义执行器优先 |
| 3 | gRPC 推送 | PushExecutorRpcImpl | 主线 |
| 4 | UDP 遗留 | PushExecutorUdpImpl | 兼容 |
| 5 | 延迟合并 | PushDelayTaskExecuteEngine | 变更风暴削峰 |
| 6 | 执行任务 | PushExecuteTask:42/57-60 | 遍历 clientManager |
| 7 | 订阅三实现 | Local/Aggregation/V2 | 本地/集群/事件 |
| 8 | 模糊族 | FuzzyWatch* 12 文件 | 独立通知链 |
| 9 | 结果钩子 | PushResultHookHolder | 可观测 |
| 10 | 无需重试 | NoRequiredRetryException | 客户端已断 |

## 02 高频坑

1. SPI 推送优先于默认 (rpc/udp)
2. 延迟窗口内多次变更合并一次 (最新数据)
3. 推送执行遍历 clientManager (clientId 维度)
4. 模糊订阅推送是独立任务链
5. NoRequiredRetryException 区分无需重试
6. UdpPushService 是 1.x 遗留
7. 回调钩子供监控 (NacosMonitorPushResultHook)

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 执行 | Delegate / Rpc / Udp / SPI |
| 任务 | 延迟引擎 / PushExecuteTask / 回调 |
| 订阅 | Local / Aggregation / V2 事件 |
| 模糊 | notifier / delay / callback 族 |
| 观测 | PushResultHook / Monitor / PushConfig |
| 遗留 | UdpPushService / ClientInfo |

## 04 跨域桥接

- ← N-10: clientId 订阅管理
- → NC-1: 客户端 NamingPushRequestHandler 接收端
- → 面试: "Nacos 服务端推送" — 双执行器 + 延迟合并 + 订阅三实现
