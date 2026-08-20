# ALI-A5 Nacos 容错+心跳+优雅关闭 — 知识规划 (KP)

> 🟡 B | 模块: discovery/ + registry/ (5 文件, 542 行) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 订阅变更 | NacosWatch:75-102 | subscribe + ip/port 自我过滤 |
| 2 | metadata 回写 | NacosWatch:108-112 | resetIfNeeded properties.setMetadata |
| 3 | 心跳打点 | NacosDiscoveryHeartBeatPublisher:102-105 | HeartbeatEvent + index 递增 |
| 4 | 心跳三条件 | NacosDiscoveryHeartBeatConfiguration:33-58 | AnyNestedCondition 生态驱动 |
| 5 | 优雅关闭 | NacosGracefulShutdownDelegate:56-81 | 反注册 → sleep → 放行 |
| 6 | 子上下文过滤 | NacosGracefulShutdownDelegate:56-62 | context equals 才执行 |
| 7 | 同步等待 | NacosGracefulShutdownDelegate:83-87 | supportsAsyncExecution=false |
| 8 | 健康镜像 | NacosDiscoveryHealthIndicator:59-70 | getServerStatus 三态 |
| 9 | 变更事件 | NacosDiscoveryInfoChangedEvent | 事件驱动重注册 (A3 restart) |
| 10 | 变更检测 | NacosServiceManager:66-73 | properties equals 比较 |

## 02 高频坑

1. 心跳默认关闭 — 普通服务无 HeartbeatEvent
2. NacosWatch 只回写自身 metadata, 不更新 ServiceCache
3. 优雅关闭先 stop() 再 sleep — 顺序反了摘流量就晚
4. 子上下文关闭不触发优雅关闭
5. 健康状态 unknown 兜底 (非 UP/DOWN)
6. watchDelay 是心跳间隔配置 (非 watch 本身)

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 生命周期 | SmartLifecycle (Watch/HeartBeat) / DisposableBean / ContextClosedEvent |
| 事件 | HeartbeatEvent / NacosDiscoveryInfoChangedEvent / NamingEvent |
| 并发 | running CAS / listenerMap computeIfAbsent / AtomicLong index |
| 条件 | AnyNestedCondition 三态 OR / @ConditionalOnBean SBA |
| 健康 | AbstractHealthIndicator / getServerStatus 镜像 |
| 容灾 | 反注册先行 / sleep 窗口 / catch 不阻断 |

## 04 跨域桥接

- ← ALI-A3: ServiceCache/注册链/restart 消费
- ← SCC-3: HeartbeatEvent 契约 (HeartbeatMonitor 消费)
- → Nacos 5.8: subscribe/HeartBeatReactor 内核
- → 面试: "Nacos 优雅停机/心跳/变更推送" 三连
