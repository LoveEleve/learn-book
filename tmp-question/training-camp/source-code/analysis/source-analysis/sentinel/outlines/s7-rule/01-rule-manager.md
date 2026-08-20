# 规则如何被管理

> S-7 上篇。本文讲 Sentinel 规则管理的统一底座：`RuleManager` 容器、`SentinelProperty` 发布-订阅，以及各 RuleManager 的一致骨架。

## 悬念

Sentinel 有 FlowRule、DegradeRule、AuthorityRule、SystemRule、ParamFlowRule 五种规则。它们各自有 Manager，但骨架惊人地一致。这个一致性从哪来？

## 一、RuleManager:统一规则容器

`RuleManager<R>` 是泛型规则容器，核心是四张 map：

```java
Map<String, List<R>> originalRules;      // 原始规则
Map<Pattern, List<R>> regexRules;        // 正则规则
Map<String, List<R>> regexCacheRules;    // 正则匹配缓存
Map<String, List<R>> simpleRules;       // 简单规则
```

`updateRules` 把每条规则按 `predicate`（是否 `AbstractRule.isRegex()`）分成简单/正则两类。正则规则编译成 `Pattern` 存入 `regexRules`，简单规则按 resource 名存入 `simpleRules`。

## 二、正则规则缓存:性能关键

`getRules(resource)` 的查询逻辑：

```java
List<R> result = new ArrayList<>(simpleRules.getOrDefault(resource, Collections.emptyList()));
if (regexRules.isEmpty()) {
    return result;
}
if (regexCacheRules.containsKey(resource)) {
    result.addAll(regexCacheRules.get(resource));
    return result;
}
synchronized (this) {
    // 锁内做正则匹配,写入缓存
}
```

简单规则直接按 resource 查；正则规则先查缓存，未命中才在锁内做正则匹配并缓存结果。

这个缓存的意义：正则匹配是昂贵的，如果每次请求都对所有正则规则跑一遍 `Pattern.matcher`，性能会崩。缓存让“同一个 resource 的正则匹配结果”只算一次。

`setRules` 在规则更新时重建正则缓存，避免发布规则时性能损耗。

## 三、SentinelProperty:发布-订阅底座

`SentinelProperty<T>` 是规则动态更新的统一接口：

```java
void addListener(PropertyListener<T> listener);
void removeListener(PropertyListener<T> listener);
boolean updateValue(T newValue);
```

`updateValue` 只在值变化时通知 listener。`DynamicSentinelProperty` 是默认实现，用 `CopyOnWriteArraySet` 存 listener，`addListener` 时立即 `configLoad` 当前值。

`NoOpSentinelProperty` 是空实现，`updateValue` 恒返回 true，用于不需要动态更新的场景。`SimplePropertyListener` 把 `configLoad` 委托给 `configUpdate`，简化只关心更新的 listener。

## 四、各 RuleManager 的一致骨架

对比五种 Manager，它们共享同一套骨架：

1. 一个 `RuleManager<R>` 实例存规则
2. 一个 `RulePropertyListener` 实现 `PropertyListener`
3. 一个 `SentinelProperty<List<R>> currentProperty`（默认 `DynamicSentinelProperty`）
4. 静态块里 `currentProperty.addListener(LISTENER)`
5. `register2Property` 换 property 时先移除旧 listener 再加新 listener
6. `loadRules` 走 `currentProperty.updateValue(rules)`

以 `AuthorityRuleManager` 为例：

```java
private static volatile RuleManager<AuthorityRule> authorityRules = new RuleManager<>();
private static final RulePropertyListener LISTENER = new RulePropertyListener();
private static SentinelProperty<List<AuthorityRule>> currentProperty = new DynamicSentinelProperty<>();

static {
    currentProperty.addListener(LISTENER);
}
```

这个骨架是刻意设计的：规则不直接写进 Manager，而是必须经 property 事件流进入，listener 再把规则编译成 Manager 需要的结构。

## 五、RuleConstant:语义字典

`RuleConstant` 集中定义所有规则类型/策略/行为的数值常量：

- grade：`FLOW_GRADE_THREAD=0` / `FLOW_GRADE_QPS=1`
- degrade：`DEGRADE_GRADE_RT=0` / `EXCEPTION_RATIO=1` / `EXCEPTION_COUNT=2`
- authority：`AUTHORITY_WHITE=0` / `AUTHORITY_BLACK=1`
- strategy：`STRATEGY_DIRECT=0` / `RELATE=1` / `CHAIN=2`
- controlBehavior：`DEFAULT=0` / `WARM_UP=1` / `RATE_LIMITER=2` / `WARM_UP_RATE_LIMITER=3`
- limitApp：`default` / `other`

这些常量是各 RuleManager 与 checker 共享的语义字典，保证“数值 1 代表 QPS”在整条链上一致。

## 悬念回收

Sentinel 规则管理的一致性来自三件事：

1. `RuleManager` 统一容器，内置正则缓存优化
2. `SentinelProperty` 发布-订阅底座，规则经事件流进入
3. 各 Manager 复用同一骨架，差异只在规则编译逻辑

规则不是直接塞进内存，而是“数据源 → property → listener → RuleManager”的完整链路。

## 锚点

- `RuleManager.java:31-35`
- `RuleManager.java:54-75`
- `RuleManager.java:87-105`
- `RuleManager.java:172-190`
- `SentinelProperty.java:24-45`
- `DynamicSentinelProperty.java:37-55`
- `AuthorityRuleManager.java:43-46`
- `RuleConstant.java:26-63`
