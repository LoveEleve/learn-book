# S-10 传输与 Dashboard 域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: transport-common + netty-http/simple-http/spring-mvc + dashboard

## 核心骨架

- Command 抽象：`CommandCenter` / `CommandCenterProvider` / `CommandCenterInitFunc`
- Handler 抽象：`CommandHandler` / `CommandHandlerProvider` / `CommandHandlerInterceptor` / 17 个 handler
- Heartbeat 抽象：`HeartbeatSender` / `HeartbeatSenderProvider` / `HeartbeatSenderInitFunc`
- Command 实现：`NettyHttpCommandCenter` / `SimpleHttpCommandCenter` / `SpringMvcHttpCommandCenter`
- Heartbeat 实现：对应三种 Http/SimpleHttp/SpringMvc sender
- Dashboard：controller 17 个、entity 16 个、repository 11 个、rule provider/publisher（数字已与 S-1 计划复核一致）

## Pass 1 观察

- transport-common 定义 SPI/接口/handler，三种 transport 模块只负责协议与服务器实现。
- CommandHandler 通过 `@CommandMapping` 或 Provider 机制暴露命令，命令数量实证为 17 个。
- Dashboard 不是简单 UI：controller → repository → rule provider/publisher → transport command API，形成规则读写与机器通信链。
- Heartbeat 负责 MachineInfo→Dashboard 注册/存活；command 负责 Dashboard→client 查询/修改。

## 标记问题

1. CommandCenter/HeartbeatSender 的 SPI provider 如何加载？
2. CommandHandler 17 个命令如何注册与分发？
3. 三种 CommandCenter 实现的协议差异？
4. HeartbeatSender 如何构造 MachineInfo 并发送？
5. CommandRequest/Response 的异常和状态码如何传播？
6. Dashboard controller/repository/provider/publisher 如何串联？
7. 规则修改如何从 Dashboard 推到 client RuleManager？
8. MetricController 如何读取/聚合 metric？
9. Cluster dashboard controller 如何分配 client/server？
10. 三种 transport 的选择边界与默认实现？
