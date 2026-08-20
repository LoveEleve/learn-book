# 命令与心跳的 SPI 骨架

> S-10 上篇。本文讲 transport-common 的统一骨架：命令中心、心跳发送器、handler 注册与三种 transport 实现的边界。

## 悬念

Sentinel 客户端为什么既要暴露一个命令端口，又要周期性发送心跳？这不是重复通信，而是控制面的两条不同通道：命令负责“Dashboard → client”，心跳负责“client → Dashboard”。

## 一、CommandCenter 与 HeartbeatSender 的角色分离

- `CommandCenter`：在客户端开启命令服务端口，接收 Dashboard 的查询/修改请求
- `HeartbeatSender`：客户端周期性上报机器存活与基础信息到 Dashboard

它们都是 SPI 接口：transport-common 只定义抽象和启动逻辑，真正的协议实现位于 `sentinel-transport-netty-http`、`sentinel-transport-simple-http`、`sentinel-transport-spring-mvc`。

## 二、SPI 加载与 InitFunc 启动

`CommandCenterProvider` 和 `HeartbeatSenderProvider` 都在静态块里通过 `SpiLoader.loadHighestPriorityInstance()` 解析最高优先级实现：

```java
CommandCenter resolveCommandCenter = SpiLoader.of(CommandCenter.class).loadHighestPriorityInstance();
HeartbeatSender resolved = SpiLoader.of(HeartbeatSender.class).loadHighestPriorityInstance();
```

如果没有找到 SPI 实现，只记录 warning，不让 core 崩溃。

启动则由两个全局 `InitFunc` 完成：

- `CommandCenterInitFunc`：`beforeStart()` → `start()`
- `HeartbeatSenderInitFunc`：初始化 scheduler → 解析心跳间隔 → 周期调用 `sendHeartbeat()`

心跳任务的周期优先取 `TransportConfig.HEARTBEAT_INTERVAL_MS`，没有配置则用 sender 自带默认值。

## 三、CommandHandler 的注册与分发

命令不是硬编码 switch，而是“SPI + 注解名”的组合。

`CommandHandlerProvider.namedHandlers()` 的过程：

1. SPI 加载全部 `CommandHandler`
2. 读取每个 handler 类上的 `@CommandMapping`，拿到命令名
3. SPI 加载全部 `CommandHandlerInterceptor`
4. 对需要拦截的命令用 `InterceptingCommandHandler` 包裹
5. 构建 `Map<String, CommandHandler>`

因此新增一个命令只要：

- 实现 `CommandHandler`
- 标注 `@CommandMapping(name = "xxx")`
- 通过 SPI 暴露

transport 实现本身只做“请求解析 → handler map 分发”，不关心具体业务。

## 四、三种 transport 实现的边界

transport-common 只定义 SPI/handler；具体协议实现有三种：

- `NettyHttpCommandCenter` / `HttpHeartbeatSender`
- `SimpleHttpCommandCenter` / `SimpleHttpHeartbeatSender`
- `SpringMvcHttpCommandCenter` / `SpringMvcHttpHeartbeatSender`

也就是说：

- 公共层定义控制面抽象
- 协议层决定“命令端口和心跳具体怎么走 HTTP/Netty/SpringMVC”

这就是为什么 transport-common 不内置协议实现：协议选择是可插拔的。

## 五、心跳的调度与边界

`HeartbeatSender` 本身只定义：

```java
boolean sendHeartbeat() throws Exception;
long intervalMs();
```

它不自己起线程，不管理重试，不负责周期调度；这些都由 `HeartbeatSenderInitFunc` 的 `ScheduledThreadPoolExecutor` 统一处理。

因此 sender 只关心“单次如何发一次心跳”，调度属于 transport-common 的全局启动逻辑。

## 悬念回收

传输层的骨架很清楚：

- CommandCenter：Dashboard 下行控制通道
- HeartbeatSender：客户端上行注册/保活通道
- CommandHandler：SPI + 注解命令分发
- 具体 transport：协议实现层
- InitFunc：统一启动与调度

控制面的抽象和协议细节被清晰分开，SPI 决定默认实现，client 只要接入其中一种 transport 就能被 Dashboard 管理。

## 锚点

- `CommandCenterProvider.java:23-39`
- `HeartbeatSenderProvider.java:22-38`
- `CommandCenterInitFunc.java:18-33`
- `HeartbeatSenderInitFunc.java:31-88`
- `CommandHandlerProvider.java:34-55`
- `CommandRequestExecution.java:18-28`
