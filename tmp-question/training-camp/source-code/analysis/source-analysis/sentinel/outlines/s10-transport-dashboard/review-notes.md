# S-10 传输与 Dashboard 域 — 审查记录

## Pass 1 / Pass 2

- 确认 transport-common 只定义 SPI/handler/init/request-response 抽象，三种 transport 模块才提供协议实现。
- 确认 `CommandCenterProvider` / `HeartbeatSenderProvider` 用 `SpiLoader.loadHighestPriorityInstance()` 解析默认实现。
- 确认 `CommandCenterInitFunc` 和 `HeartbeatSenderInitFunc` 在 `@InitOrder(-1)` 阶段启动命令端口与心跳任务。
- 确认 `CommandHandlerProvider` 是“SPI + @CommandMapping + interceptor 包装”的命令注册机制。
- 确认 Dashboard 的规则链路：controller → repository → provider/publisher → `SentinelApiClient` → client command API。
- 确认三条控制面数据流：heartbeat、command、metric。

## 深审修正

1. 上篇锚点核对：provider 的 `loadHighestPriorityInstance`、handler map 的 `namedHandlers`、心跳调度位置。
2. 中篇核对：`FlowControllerV2` 查询/新增/修改/删除 + `publishRules(app)` 链路；`FlowRuleApiProvider` 选最近心跳的健康机器，`FlowRuleApiPublisher` fan-out 推送到所有健康机器。
3. 下篇确认：`CommandCenter` 是纯接口，`MetricController` 依赖 `MetricsRepository` 聚合展示；控制面三条数据流边界清晰。

## 三篇正文

- `01-command-heartbeat.md`：CommandCenter/HeartbeatSender SPI、handler 注册、三种 transport 实现
- `02-dashboard-rule.md`：Dashboard controller/repository/provider/publisher 规则链路
- `03-control-plane.md`：heartbeat/command/metric 三条数据流与 Dashboard 控制面边界

## 遗留

1. 具体 17 个 CommandHandler 没有逐个展开，只抽象出注册与分发机制。
2. `SentinelApiClient` 的 HTTP 命令协议细节未单独展开。
3. `MetricController` 的聚合逻辑和 repository 存储细节未深入。
4. cluster dashboard controller（client/server 分配）的具体流程未逐 controller 拆开。
