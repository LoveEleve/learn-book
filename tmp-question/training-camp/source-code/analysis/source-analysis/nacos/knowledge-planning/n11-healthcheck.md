# N-11 健康检查 — 知识规划 (KP)

> 🔴 A | 模块: naming/healthcheck (36 文件 2,987 行) | 版本: 3.0.3

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | 自续任务 | HealthCheckTaskV2:43/110-128 | doHealthCheck → 重新调度 |
| 2 | 处理器注册表 | HealthCheckProcessorV2Delegate:38/49-55 | type Map 分派 |
| 3 | 四处理器 | Tcp:421/Http:178/Mysql:207/None | 检查方式族 |
| 4 | 公共判定 | HealthCheckCommonV2:209 | 状态判定/上报 |
| 5 | SPI 扩展 | HealthCheckExtendProvider | 自定义处理器 |
| 6 | 拦截器链 | HealthCheckInterceptorChain | Enable/Responsible |
| 7 | 心跳任务 | BeatCheckTask + ClientBeatCheckTaskV2 | 5s 周期 |
| 8 | 心跳三维 | InstanceBeatChecker/Unhealthy/Expired | 分治判定 |
| 9 | 状态同步 | HealthStatusSynchronizer 双实现 | 临时/持久 |
| 10 | 心跳载体 | RsInfo:164 | 1.x 遗留 |

## 02 高频坑

1. TcpSuperSenseProcessor 不存在 (1.x) — 3.x 处理器族
2. 心跳 (heartbeat/) 与主动检查 (v2/processor) 是两个面
3. Delegate 按 getType 注册 — 未知 type 无处理器
4. 自续调度 — 任务永不终止
5. Responsible 拦截器决定哪个节点检查
6. 检查结果按临时/持久分型同步
7. None 处理器在无检查配置时启用

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 任务 | HealthCheckTaskV2 / 自续环 / 调度 |
| 处理器 | Tcp/Http/Mysql/None / Delegate / SPI |
| 拦截 | Enable / Responsible / 包装器 |
| 心跳 | BeatCheckTask / ClientBeatProcessorV2 / 三维检查器 |
| 状态 | HealthCheckStatus / Synchronizer 双实现 |
| 遗留 | RsInfo / 1.x 对照 |

## 04 跨域桥接

- ← N-10: 客户端状态消费
- ← NC-6: HealthCheckReactor 调度面
- → ALI-A5: 客户端心跳对照
- → 面试: "Nacos 健康检查" — 自续任务 + 处理器族 + 心跳三维
