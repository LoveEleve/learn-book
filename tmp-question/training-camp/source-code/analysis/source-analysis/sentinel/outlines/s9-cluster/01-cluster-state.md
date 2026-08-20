# 集群状态与组件

> S-9 上篇。本文讲集群限流的全局状态管理和组件抽象：模式如何切换，client/server 如何通过 SPI 加载，token 请求的结果模型是什么。

## 悬念

Sentinel 集群限流让多个节点共享一个 token server 做统一流控。问题：节点怎么知道自己是 client 还是 server？切换模式时会发生什么？

## 一、三种模式

`ClusterStateManager` 定义集群的三种状态：

```java
public static final int CLUSTER_CLIENT = 0;
public static final int CLUSTER_SERVER = 1;
public static final int CLUSTER_NOT_STARTED = -1;
```

`mode` 是 `volatile int`，默认 `CLUSTER_NOT_STARTED`（集群未启动）。`isClient()` / `isServer()` 分别判断当前模式。

## 二、模式切换:互斥与启停

`setToClient()` / `setToServer()` 切换模式：

```java
public static boolean setToClient() {
    if (mode == CLUSTER_CLIENT) {
        return true;          // 已是目标模式
    }
    mode = CLUSTER_CLIENT;
    sleepIfNeeded();          // 限制切换间隔
    lastModified = TimeUtil.currentTimeMillis();
    return startClient();
}
```

关键点：

- 已是目标模式 → 直接返回，不重复启停
- 切换前 `sleepIfNeeded()`：两次切换间隔不小于 `MIN_INTERVAL`，防止频繁切换
- 启动新组件前会**先停掉另一模式**：

```java
private static boolean startClient() {
    EmbeddedClusterTokenServer server = EmbeddedClusterTokenServerProvider.getServer();
    if (server != null) {
        server.stop();        // 先停 server
    }
    ClusterTokenClient tokenClient = TokenClientProvider.getClient();
    if (tokenClient != null) {
        tokenClient.start();  // 再启 client
        return true;
    }
    return false;
}
```

client 和 server 是互斥的：节点同一时刻只能是 client 或 server，不能同时扮演两个角色。

模式切换也通过 `SentinelProperty<Integer>` 响应外部配置变化。静态块里 `InitExecutor.doInit()` + `stateProperty.addListener(PROPERTY_LISTENER)`，listener 的 `configUpdate` 调 `applyStateInternal` 应用新模式。

## 三、SPI provider 加载 client/server

client 和 server 实例都不是直接 new，而是通过 SPI provider 惰性加载。

`TokenClientProvider`：

```java
static {
    resolveTokenClientInstance();
}

private static void resolveTokenClientInstance() {
    ClusterTokenClient resolvedClient = SpiLoader.of(ClusterTokenClient.class).loadFirstInstance();
    ...
}
```

`EmbeddedClusterTokenServerProvider` 同理，用 `SpiLoader.loadFirstInstance()` 加载第一个 SPI 实现。

这两个 provider 的意义：Sentinel core 不依赖具体的 Netty 实现。core 只定义 `ClusterTokenClient` / `EmbeddedClusterTokenServer` 接口，真正的实现（`DefaultClusterTokenClient` / `DefaultEmbeddedTokenServer`）在 `sentinel-cluster-*-default` 模块里通过 SPI 提供。

如果没找到 SPI 实现，provider 记 warning，集群模式不会被激活——这就是“core 无集群也能跑”的原因。

## 四、TokenService 接口与结果模型

`TokenService` 定义集群 token 服务的接口：

```java
TokenResult requestToken(Long ruleId, int acquireCount, boolean prioritized);
TokenResult requestParamToken(Long ruleId, int acquireCount, Collection<Object> params);
TokenResult requestConcurrentToken(String clientAddress, Long ruleId, int acquireCount);
void releaseConcurrentToken(Long tokenId);
```

- `requestToken`：普通流控
- `requestParamToken`：参数流控
- `requestConcurrentToken` / `releaseConcurrentToken`：并发流控（占用/释放）

`TokenResult` 是结果载体：

```java
private Integer status;
private int remaining;    // 剩余额度
private int waitInMs;     // 需要等待的毫秒数
private long tokenId;     // 并发 token 的唯一 ID
private Map<String, String> attachments;
```

`TokenResultStatus` 用整数编码所有结果：

- 成功：`OK=0`
- 拒绝：`BLOCKED=1` / `SHOULD_WAIT=2`
- 错误：`BAD_REQUEST=-4` / `TOO_MANY_REQUEST=-2` / `FAIL=-1`
- 无规则：`NO_RULE_EXISTS=3` / `NO_REF_RULE_EXISTS=4`
- 释放：`RELEASE_OK=6` / `ALREADY_RELEASE=7`

## 五、checker 如何消费 TokenResult

`FlowRuleChecker.applyTokenResult` 消费结果：

```java
switch (result.getStatus()) {
    case OK: return true;
    case SHOULD_WAIT: Thread.sleep(result.getWaitInMs()); return true;
    case NO_RULE_EXISTS:
    case BAD_REQUEST:
    case FAIL:
    case TOO_MANY_REQUEST:
        return fallbackToLocalOrPass(...);
    case BLOCKED:
    default:
        return false;
}
```

核心三态（OK / SHOULD_WAIT / BLOCKED）直接决定放行/等待/拒绝，其余状态走 fallback 策略——集群失败不直接阻塞流量。

## 悬念回收

集群限流的基础架构：

1. `ClusterStateManager` 用 volatile 管理 client/server/未启动三种模式
2. 模式切换互斥：切换前先停另一模式，且限制切换频率
3. client/server 实例通过 SPI provider 惰性加载，core 不依赖具体实现
4. `TokenService` 定义三类 token 请求，`TokenResult`/`TokenResultStatus` 编码结果
5. checker 只认核心三态，其余状态走 fallback

## 锚点

- `ClusterStateManager.java:40-44`
- `ClusterStateManager.java:69-73`
- `ClusterStateManager.java:82-101`
- `ClusterStateManager.java:136-155`
- `TokenClientProvider.java:41`
- `EmbeddedClusterTokenServerProvider.java:34`
- `TokenService.java:26-62`
- `TokenResult.java:28-31`
- `TokenResultStatus.java:40-49`
