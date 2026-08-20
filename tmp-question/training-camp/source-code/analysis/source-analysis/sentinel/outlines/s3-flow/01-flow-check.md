# 规则如何变成一次判定

> S-3 上篇。本文只追踪 FlowSlot 从规则加载到一次放行/拒绝的路径。

## 悬念

一条 `FlowRule` 不是写进内存就自动生效的。它要经过 property、listener、resource 分组，最后在 `FlowSlot` 里变成一次具体判定。问题是：多条规则如何组合？当前请求到底拿哪一个节点来比较？

## 一、FlowSlot 只是入口壳

`FlowSlot.entry(...)` 的核心只有两句：

```java
checkFlow(resourceWrapper, context, node, count, prioritized);
fireEntry(context, resourceWrapper, node, count, prioritized, args);
```

第一句做流控判定，第二句在通过后继续传播。`checkFlow(...)` 又只是把现场参数交给 `FlowRuleChecker` (`FlowSlot.java:154-165`)。

所以 FlowSlot 不保存规则，也不实现四种限流算法。它只把当前资源的现场数据送进 checker，再决定是否允许继续。

## 二、规则不是直接塞进 Map

`FlowRuleManager` 使用的是：

```java
RuleManager<FlowRule> flowRules;
SentinelProperty<List<FlowRule>> currentProperty;
FlowPropertyListener LISTENER;
```

静态初始化时，manager 把 listener 注册到默认 `DynamicSentinelProperty` (`FlowRuleManager.java:46-58`)。

外部数据源接入走 `register2Property(...)`：旧 property 移除 listener，新 property 添加 listener，最后替换 currentProperty (`FlowRuleManager.java:82-90`)。

直接加载规则也不绕过这条链：

```java
public static void loadRules(List<FlowRule> rules) {
    currentProperty.updateValue(rules);
}
```

真正更新规则的是 listener：它把 List 交给 `FlowRuleUtil.buildFlowRuleMap(...)` 按 resource 分组，再调用 `flowRules.updateRules(...)` (`FlowRuleManager.java:127-139`)。

这条链的价值在于：外部数据源只负责发布规则，FlowRuleManager 只负责把规则转换成运行时索引；两者之间没有直接共享可变 Map。

## 三、Checker 不是挑一条,而是全量过规则

`FlowRuleChecker.checkFlow(...)` 先按 resource 名取规则集合，然后顺序遍历：

```java
for (FlowRule rule : rules) {
    if (!canPassCheck(rule, context, node, count, prioritized)) {
        throw new FlowException(rule.getLimitApp(), rule);
    }
}
```

这意味着多条 FlowRule 是 **AND 关系**：每一条都必须通过；任意一条失败，整个 entry 被 `FlowException` 拒绝 (`FlowRuleChecker.java:42-53`)。

Checker 不是从多条规则里选“最严格的一条”，也没有一个总阈值合并器；它只是让每条规则分别对当前请求做自己的判定。

## 四、当前请求到底拿哪个 Node

本地判定路径是：

```java
Node selectedNode = selectNodeByRequesterAndStrategy(rule, context, node);
if (selectedNode == null) {
    return true;
}
return rule.getRater().canPass(selectedNode, acquireCount, prioritized);
```

`selectedNode` 的选择由两个字段共同决定：`limitApp` 与 `strategy`。

### 1. 指定 origin + DIRECT

如果 `limitApp` 等于当前 context 的 origin，且 origin 不是 `default`/`other`，DIRECT 策略会选 `context.getOriginNode()`。这就是“只限制某个调用方”。

### 2. default + DIRECT

`limitApp=default` 时，DIRECT 直接选当前资源的 `node.getClusterNode()`，也就是跨 context 的资源总量。

### 3. other + DIRECT

`limitApp=other` 且 `FlowRuleManager.isOtherOrigin(...)` 成立时，选当前 origin node。这是把未被显式列出的调用方归到 other 桶。

### 4. RELATE

`strategy=RELATE` 时，checker 按 `refResource` 找另一个资源的 `ClusterNode` (`FlowRuleChecker.java:87-106`)。当前规则限制的是“关联资源”的统计量。

### 5. CHAIN

`strategy=CHAIN` 时，只有 `refResource` 等于当前 context 名才生效，节点使用当前 `DefaultNode`。它表达的是“只在某条调用入口链上限流”。

## 五、local 与 cluster: 判定节点之外的第二条分叉

如果 `rule.isClusterMode()` 为 false，走上面的 local 路径。

如果是 cluster mode，checker 会根据 `ClusterStateManager` 选择：

- client → `TokenClientProvider.getClient()`
- server → `EmbeddedClusterTokenServerProvider.getServer()`
- 其他状态 → 没有 cluster service

拿到 token service 后，请求 cluster token；结果为 OK 就通过，BLOCKED 就拒绝。`NO_RULE_EXISTS`、`BAD_REQUEST`、`FAIL`、`TOO_MANY_REQUEST` 等状态会按 `fallbackToLocalWhenFail` 决定回退本地还是直接放行 (`FlowRuleChecker.java:138-208`)。

所以 cluster mode 不是另一个 FlowSlot，而是 checker 内部的一条 token 判定分支。

## 悬念回收

一次 Flow 判定的完整路径是：

```text
外部 property
  -> FlowPropertyListener
  -> FlowRuleUtil 按 resource 分组
  -> RuleManager
  -> FlowSlot
  -> FlowRuleChecker 遍历全部规则
  -> 按 limitApp/strategy 选 Node
  -> local controller 或 cluster token
  -> 通过继续 fireEntry / 失败抛 FlowException
```

规则模型负责描述“限制谁、看什么、怎么控”；manager 负责把规则送到运行时；checker 负责把规则翻译成这一次请求的具体判定。

## 锚点

- `FlowSlot.java:154-165`
- `FlowRuleManager.java:46-58`
- `FlowRuleManager.java:82-90`
- `FlowRuleManager.java:104-106`
- `FlowRuleManager.java:127-139`
- `FlowRuleChecker.java:42-53`
- `FlowRuleChecker.java:73-81`
- `FlowRuleChecker.java:87-136`
- `FlowRuleChecker.java:138-208`
