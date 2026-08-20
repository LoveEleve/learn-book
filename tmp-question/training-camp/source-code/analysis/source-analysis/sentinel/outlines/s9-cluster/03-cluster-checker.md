# server 侧流控判定

> S-9 下篇。本文讲集群 token server 如何做流控判定：三类请求如何分派，普通流控的全局阈值算法，并发流控的占用/释放，以及 envoy-rls 如何复用这套判定。

## 悬念

client 把 token 请求发给 server 后，server 到底怎么决定放行还是拒绝？它没有本地那套滑动窗口，那用什么统计？

答案是：server 侧维护了集群维度的 metric，用“全局阈值 - 当前 PASS 事件”算剩余额度。

## 一、DefaultTokenService:三类请求分派

`DefaultTokenService` 实现 `TokenService`，由 `TokenServiceProvider` 通过 `SpiLoader.loadFirstInstanceOrDefault()` 解析。它把三类请求分派给三个专用 checker：

```java
public TokenResult requestToken(Long ruleId, int acquireCount, boolean prioritized) {
    // 按 flowId 取 FlowRule
    return ClusterFlowChecker.acquireClusterToken(rule, acquireCount, prioritized);
}

public TokenResult requestConcurrentToken(String clientAddress, Long ruleId, int acquireCount) {
    // 按 ruleId 取规则
    return ConcurrentClusterFlowChecker.acquireConcurrentToken(clientAddress, rule, acquireCount);
}

public void releaseConcurrentToken(Long tokenId) {
    ConcurrentClusterFlowChecker.releaseConcurrentToken(tokenId);
}
```

- 普通流控 → `ClusterFlowChecker`
- 参数流控 → 参数 checker
- 并发流控 → `ConcurrentClusterFlowChecker`

`DefaultEmbeddedTokenServer` 只是 Netty server 的薄封装，真正的判定在 `cluster/flow` 下的 checker。

## 二、ClusterFlowChecker:全局阈值判定

`acquireClusterToken` 是普通流控的核心：

```java
if (!allowProceed(id)) {
    return new TokenResult(TokenResultStatus.TOO_MANY_REQUEST);
}
ClusterMetric metric = ClusterMetricStatistics.getMetric(id);
if (metric == null) {
    return new TokenResult(TokenResultStatus.FAIL);
}
double latestQps = metric.getAvg(ClusterFlowEvent.PASS);
double globalThreshold = calcGlobalThreshold(rule) * ClusterServerConfigManager.getExceedCount();
double nextRemaining = globalThreshold - latestQps - acquireCount;
if (nextRemaining >= 0) {
    metric.add(ClusterFlowEvent.PASS, acquireCount);
    metric.add(ClusterFlowEvent.PASS_REQUEST, 1);
    if (prioritized) {
        metric.add(ClusterFlowEvent.OCCUPIED_PASS, acquireCount);
    }
    return new TokenResult(TokenResultStatus.OK).setRemaining((int) nextRemaining);
} else {
    // prioritized 尝试借未来窗口,否则 BLOCK
    ...
    return blockedResult();
}
```

步骤拆解：

1. `allowProceed(id)`：用 `GlobalRequestLimiter.tryPass(namespace)` 做全局请求限流，防止 server 过载
2. 取 `ClusterMetric`，拿不到返回 `FAIL`
3. `latestQps = metric.getAvg(PASS)`：集群维度的当前 PASS 速率
4. `globalThreshold = calcGlobalThreshold(rule) * exceedCount`：全局阈值
5. `nextRemaining = globalThreshold - latestQps - acquireCount`：剩余额度
6. 够则记 PASS 事件，prioritized 记 OCCUPIED_PASS，返回 OK
7. 不够则 prioritized 尝试 `metric.tryOccupyNext` 借未来窗口，成功返回 SHOULD_WAIT，失败记 BLOCK 返回 BLOCKED

## 三、全局阈值的两种类型

`calcGlobalThreshold` 决定阈值：

```java
private static double calcGlobalThreshold(FlowRule rule) {
    double count = rule.getCount();
    switch (rule.getClusterConfig().getThresholdType()) {
        case FLOW_THRESHOLD_GLOBAL:
            return count;
        case FLOW_THRESHOLD_AVG_LOCAL:
        default:
            int connectedCount = ClusterFlowRuleManager.getConnectedCount(...);
            return count * connectedCount;
    }
}
```

- `FLOW_THRESHOLD_GLOBAL`：全局总量，直接用规则 count
- `FLOW_THRESHOLD_AVG_LOCAL`：按连接数均摊，count × 连接的节点数

`exceedCount` 是 server 配置的“超发系数”，允许阈值适度放大。

## 四、并发流控:ConcurrentClusterFlowChecker

并发流控管理“占用中的 token 数量”。`acquireConcurrentToken` 请求占用，`releaseConcurrentToken` 释放。

它维护一个当前占用数，请求进来时判断是否超过上限，超过则拒绝并返回 tokenId。释放时按 tokenId 归还占用额度。

这对应 `TokenService.requestConcurrentToken` 需要 `clientAddress` 参数——按调用方区分占用。

## 五、envoy-rls:复用集群判定

`sentinel-cluster-server-envoy-rls` 是 Envoy RateLimitService(RLS) 的集成，通过 gRPC 暴露集群限流能力。核心：

- `SentinelRlsGrpcServer`：gRPC 服务端
- `SimpleClusterFlowChecker`：复用 core 的 `ClusterMetric`/`ClusterMetricStatistics` 做判定
- `SentinelEnvoyRlsServer`：集成入口

`SimpleClusterFlowChecker.acquireClusterToken` 和 `ClusterFlowChecker` 类似，用 `ClusterFlowEvent` 事件和 `ClusterMetricStatistics` 统计：

```java
public static TokenResult acquireClusterToken(FlowRule rule, int acquireCount) {
    // 用 ClusterMetric 判定,记 PASS/BLOCK 事件
    ClusterServerStatLogUtil.log("flow|pass|" + id, acquireCount);
}
```

envoy-rls 没有另起一套流控，而是**复用同一套 `ClusterMetric` 统计和判定逻辑**，只是把接入方式从 Netty 换成 gRPC RLS 协议。

## 悬念回收

server 侧流控判定是：

1. `DefaultTokenService` 分派三类请求到专用 checker
2. `ClusterFlowChecker` 用 `全局阈值 - 当前PASS` 算剩余额度，够则记 PASS 放行，不够则借未来窗口或 BLOCK
3. 阈值类型分全局/均摊，可乘 exceedCount 放大
4. `ConcurrentClusterFlowChecker` 管理并发占用/释放
5. envoy-rls 复用同一套 ClusterMetric 判定，只是换成 gRPC 接入

server 不依赖 client 的本地滑窗，而是自己维护集群维度的 metric，这是“统一限流”的关键。

## 锚点

- `DefaultTokenService.java:39-49`
- `DefaultTokenService.java:67-85`
- `ClusterFlowChecker.java:38-46`
- `ClusterFlowChecker.java:55-81`
- `SimpleClusterFlowChecker.java:33`
