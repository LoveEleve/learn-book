# N-14 命名集群与监控 — 状态机 + TPS 监控 + gRPC 服务端处理器

> 前置: [[NC-6-服务端核心]] + [[N-11-健康检查]] (状态面) | 对照: 集群状态与监控的命名专属面
> 🟡 B | 方案 B (重要域) | 闭环: q1(集群状态) q2(监控面) q3(远端处理器)

**读者处境**: 命名服务的集群状态 (UP/DOWN/READY_ONLY) 怎么维护? TPS 监控怎么统计? gRPC 服务端处理器 (ServerRequestHandler) 怎么注册?

### 1. 集群状态 — ServerStatusManager

场景: 服务端状态怎么维护?
源码路径:
- **ServerStatusManager** (cluster/ServerStatusManager.java:37): 状态管理 — UP/DOWN/READY_ONLY 流转
- **ServerStatus** (cluster/ServerStatus.java:25): 状态枚举
- **NamingReadinessCheckService** (cluster/NamingReadinessCheckService.java:30): 就绪检查
- **cluster/transport** + **cluster/remote**: 集群传输与远端 (NC-6 对照)
关键设计 (q1): **"状态机 = 服务就绪语义"** — READY_ONLY 表示只读 (订阅恢复中); 与客户端 getServerStatus 呼应。 [模式: 状态机]

### 2. 监控面 — MetricsMonitor 与 TPS 族

场景: 命名服务监控怎么统计?
源码路径:
- **MetricsMonitor** (monitor/MetricsMonitor.java:28): 命名指标 (NC-7 三处之一)
- **NamingTpsMonitor** (monitor/NamingTpsMonitor.java:28) + **TpsMonitorItem** (monitor/TpsMonitorItem.java:24): TPS 统计
- **NamingDynamicMeterRefreshService** (monitor/NamingDynamicMeterRefreshService.java:36): 动态指标刷新
- **ServiceTopNCounter** (monitor/ServiceTopNCounter.java:29): TopN 服务统计
- **PerformanceLoggerThread** (monitor/PerformanceLoggerThread.java:39): 性能日志
- **collector/** (3): 采集器
关键设计 (q2): **"TPS + TopN + 性能日志"** — 实时 TPS 与 TopN 服务观测; 性能日志定期输出。 [模式: 多维监控]

### 3. gRPC 远端处理器 — remote/rpc/handler

场景: 命名 gRPC 请求怎么处理?
源码路径:
- **remote/rpc/handler/** (naming/remote/rpc/handler): ServerRequestHandler 实现族 (InstanceRequestHandler/SubscribeServiceRequestHandler 等)
- **remote/udp/**: UDP 面 (1.x 遗留)
- 注册: NC-3 rpcClient.registerServerRequestHandler 的服务端对应
关键设计 (q3): **"处理器族 = 请求类型分派"** — 每个 gRPC 请求类型一个 handler; 与 api/remote 请求族对应。 [模式: 处理器族]

### 4. 测试与行为锚

场景: 状态/监控边界?
源码路径:
- 测试: ServerStatusManagerTest (naming test)
- 锚: ServerStatus 枚举三态
关键设计 (q1): **"枚举即状态契约"**。 [模式: 枚举契约]
