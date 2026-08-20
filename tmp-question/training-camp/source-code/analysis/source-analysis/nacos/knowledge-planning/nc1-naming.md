# NC-1 NamingService 注册发现 — 知识规划 (KP)

> 🔴 A | 模块: client/naming (120 文件之核心) | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 重载漏斗 | NacosNamingService:140-175 | 6 重载收敛到 registerInstance(s,g,i) |
| 2 | 双代理路由 | Delegate:197-203 | ephemeral\|\|ability → grpc |
| 3 | 订阅三连 | NamingGrpcClientProxy:392-410 | cacheRedo → doSubscribe → registered |
| 4 | 服务端列表变更 | NamingGrpcClientProxy:139-146 | onServerListChange 重连 |
| 5 | 推送处理四步 | ServiceInfoHolder:124-164 | 忽略/put/差异/事件+落盘 |
| 6 | 差异计算 | InstancesDiffer | added/modified/removed + 过期忽略 |
| 7 | 发现三路 | NacosNamingService:351-376 | failover > 缓存 > 直查 |
| 8 | 过滤 | NacosNamingService:331-345 | healthy/enabled/weight |
| 9 | 订阅门面 | NacosNamingService:506-539 | 本地注册先行 + 无监听才 unsubscribe |
| 10 | 保护+兜底 | ProtectMode:27 / ServiceInfoUpdateService:102-116 | 0.8 阈值 / 双检定时查询 |

## 02 高频坑

1. 持久实例也走 gRPC (能力路由不是"持久=HTTP")
2. subscribe 先 cacheRedo 后发送 — 顺序不能反
3. 空推送被忽略 (pushEmptyProtection 可关, 默认 false 时 hosts==null 仍忽略)
4. 无监听器才真正 unsubscribe
5. doDiff 过期数据 (lastRefTime 旧) 静默忽略
6. 发现默认走缓存, 非订阅才直查
7. batch 强制 gRPC

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 门面 | 重载漏斗 / init 组装 (NotifyCenter+Holder+Proxy) |
| 路由 | ephemeral\|\|ability / batch 强制 / 订阅面强制 |
| 通信 | RpcClient 工厂 / PushRequestHandler / redo 前置 |
| 缓存 | ConcurrentMap / 差异事件 / DiskCache 落盘 / 启动加载 |
| 数据源 | failover > 缓存 > 直查 / 过滤语义 |
| 订阅 | changeNotifier 本地注册 / isSubscribed 门控 |
| 保护 | ProtectMode 0.8 / ServiceInfoUpdateService 双检 |

## 04 跨域桥接

- ← ALI-A3: NamingService 消费方 (registerInstance/getInstances/subscribe)
- → NC-3: redo 前置/连接监听 (NamingGrpcRedoService)
- → NC-4: FailoverReactor/getFailoverServiceInfo 面
- → 面试: "Nacos 注册发现客户端怎么工作" — 漏斗 + 双代理 + 订阅 + 差异缓存
