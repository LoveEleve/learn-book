# S-10 传输与 Dashboard 域 — 大纲

## 上篇: 命令与心跳的 SPI 骨架 — 01-command-heartbeat.md

1. CommandCenter/HeartbeatSender 的 SPI 加载与 InitFunc 启动
2. CommandHandler 的注解注册、interceptor 包装与分发
3. 三种 transport 实现的边界
4. 心跳任务的调度与 MachineInfo 注册

## 中篇: Dashboard 的规则链路 — 02-dashboard-rule.md

1. controller → repository → provider/publisher
2. 查询机器规则与推送规则
3. SentinelApiClient 作为 command API 客户端
4. InMemoryRuleRepositoryAdapter 的角色

## 下篇: 指标与控制面的整体边界 — 03-control-plane.md

1. Heartbeat / Command / Metric 三条数据流
2. Dashboard 的 cluster controller 与 machine registry
3. transport-common 与具体协议实现的分层理由
4. 传输与控制面如何包裹 core 而不侵入 core
