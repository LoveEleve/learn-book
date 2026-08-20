# 控制面的整体边界

> S-10 下篇。本文把 heartbeat、command、metric 三条数据流与 Dashboard 的 cluster/machine 管理放到一起，看 Sentinel 的控制面如何包裹 core 而不侵入 core。

## 悬念

为什么 Sentinel 要把心跳、命令、指标拆成三条数据流？因为它们分别回答三个不同问题：机器在不在、规则怎么改、指标怎么看。

## 一、三条数据流

### 1. Heartbeat: client → dashboard

Heartbeat 只负责机器发现与存活感知。`HeartbeatSender` 周期性把机器信息发给 Dashboard，Dashboard 侧的 `MachineRegistryController` / `AppManagement` 维护机器列表和健康状态。

### 2. Command: dashboard → client

CommandCenter 暴露命令端口，Dashboard 通过 `SentinelApiClient` 调用 handler：

- 拉规则
- 推规则
- 查树/查 origin/查 cluster node
- on/off 开关
- 版本/状态查询

### 3. Metric: client → dashboard 仓库 → 页面

指标不是通过 command 逐次查询，而是由 client 上报、dashboard 存储，再由 `MetricController`/`MetricsRepository` 聚合展示。

这三条流向不同、节奏不同，强行合成一条会让控制面复杂且脆弱。

## 二、MachineRegistry 与 Cluster Controller

Dashboard 还承担机器管理和集群分配：

- `MachineRegistryController`：机器注册与查询
- `ClusterAssignController`：client/server 分配
- `ClusterConfigController`：集群配置

这些 controller 并不直接改 core 规则；它们修改的是 transport/cluster 的控制面配置，最终通过 command API 或集群数据源让 client/server 感知。

## 三、transport-common 与协议实现的分层理由

transport-common 只定义：

- 命令/心跳 SPI
- handler 抽象
- init 启动逻辑
- request/response 抽象

具体协议实现（Netty HTTP / Simple HTTP / Spring MVC）放到独立模块。这样做有两个好处：

1. core 不被任何具体网络栈绑死
2. command/heartbeat 的协议实现可替换、可裁剪

这是与 S-8 适配器同样的边界哲学：外围协议细节属于 transport 模块，不应泄漏到 core 的 entry/rule/statistic 语义。

## 四、Dashboard 为什么还需要本地 repository

Dashboard 不是纯代理，它还需要：

- 本地维护规则副本（repository）
- 聚合多机器的视图
- 在页面编辑时先修改副本
- 再由 publisher 批量 fan-out 推送

因此 Dashboard 是“控制面协调者”，而不是“HTTP 透传代理”。

## 五、控制面如何包裹 core

把全局结构画出来：

```text
client core (entry/rule/statistic)
  <- 适配器/transport/cluster/heartbeat
  -> command port / heartbeat / metric
  -> dashboard controller/repository/provider/publisher
  -> 页面与运维动作
```

core 只暴露可管理面（command、metric、heartbeat），transport-common 和 dashboard 把这些可管理面组织成完整的控制平面。

## 悬念回收

S-10 的核心不是某个 handler 或 controller，而是控制面分层：

- Heartbeat 负责“机器是否在线”
- Command 负责“规则与状态如何交互”
- Metric 负责“运行指标如何展示”
- Dashboard 把这些能力收敛成统一的运维入口

这层控制面建立在 core 之上，但不改变 core 的判定语义。

## 锚点

- `HeartbeatSender.java:17-36`
- `CommandCenter.java:17-38`
- `MetricController.java:47-57`
- `MachineRegistryController.java:36`
- `ClusterAssignController.java:43`
- `ClusterConfigController.java:58`
- `FlowControllerV2.java:176-179`
