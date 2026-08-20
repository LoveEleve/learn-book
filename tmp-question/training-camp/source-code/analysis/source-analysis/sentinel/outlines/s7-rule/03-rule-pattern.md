# 规则管理的一致性

> S-7 下篇。本文横向对比五种 RuleManager，提炼它们统一的骨架与各自的差异，并打通规则动态更新的完整链路。

## 悬念

Flow、Degrade、Authority、System、ParamFlow 五种规则，各自的 Manager 代码风格几乎一样。这不是巧合，而是同一个“骨架 + 差异”模式的刻意收敛。

## 一、五种 RuleManager 的统一骨架

以 `AuthorityRuleManager`、`SystemRuleManager`、`FlowRuleManager`、`DegradeRuleManager`、`ParamFlowRuleManager` 对比，它们共享同一套骨架：

```text
1. RuleManager<R> 实例          —— 存规则
2. RulePropertyListener         —— 实现 PropertyListener
3. SentinelProperty<List<R>>    —— 默认 DynamicSentinelProperty
4. 静态块 addListener            —— 注册 listener
5. register2Property            —— 换 property
6. loadRules                    —— updateValue(rules)
```

以 `AuthorityRuleManager` 的静态块为例：

```java
static {
    currentProperty.addListener(LISTENER);
}
```

`register2Property` 换 property 时，先移除旧 listener，再加新 listener，再替换 currentProperty：

```java
synchronized (LISTENER) {
    if (currentProperty != null) {
        currentProperty.removeListener(LISTENER);
    }
    property.addListener(LISTENER);
    currentProperty = property;
}
```

这个骨架的核心思想：**规则不直接写进 Manager，而是必须经 property 事件流进入，listener 再把规则编译成 Manager 需要的结构。**

## 二、差异点:各 Manager 的独特逻辑

统一骨架之外，各 Manager 有各自的差异：

- `SystemRuleManager`：额外维护系统状态采样线程（`SystemStatusListener` 每秒采样）
- `DegradeRuleManager`：额外维护 `RuleManager<CircuitBreaker>` 与 `RuleManager<DegradeRule>` 双容器
- `AuthorityRuleManager`：强制“一资源一规则”
- `ParamFlowRuleManager`：额外清理参数统计 metric

这些差异都在 listener 的 `configUpdate`/`configLoad` 里体现。骨架负责“规则如何进入”，差异负责“进入后如何编译”。

## 三、规则动态更新的完整链路

一次规则动态更新的完整链路：

```text
外部数据源 (Nacos/ZK/Console)
  -> SentinelProperty.updateValue(newRules)
  -> 值变化?
  -> 通知所有 PropertyListener
  -> RuleManager.listener.configUpdate(newRules)
  -> 校验 + 编译 + RuleManager.updateRules
  -> 槽下次查询时读到新规则
```

以 `SystemRuleManager` 为例：

```java
public static void loadRules(List<SystemRule> rules) {
    currentProperty.updateValue(rules);
}
```

`updateValue` 先判断值是否变化（`isEqual`），只有变化才通知 listener。listener 的 `configUpdate` 调 `restoreSetting()` 重置所有阈值，再逐条 `loadSystemConf(rule)` 重新设置。

## 四、规则管理作为数据源推送入口

规则管理不只是内存容器，它是**外部数据源推送的入口**。

`register2Property` 允许外部数据源接入：

```java
public static void register2Property(SentinelProperty<List<AuthorityRule>> property) {
    ...
}
```

数据源（如 NacosDataSource）在底层实现 `SentinelProperty`，配置变化时调 `updateValue`，从而触发规则更新。这是 Sentinel 与配置中心联动的基础。

`NoOpSentinelProperty` 提供空实现，用于不需要动态更新的场景。`SimplePropertyListener` 把 `configLoad` 委托给 `configUpdate`，简化只关心更新的 listener。

## 五、一致性带来的可维护性

统一骨架的价值在于可维护性：

- 新增一种规则类型时，照着骨架写一个 Manager + Rule + Checker + Listener 即可
- 规则更新链路全局一致，排查问题只需定位 listener 的编译逻辑
- 数据源接入方式统一，所有规则共享同一套 property 机制

## 悬念回收

Sentinel 规则管理的“一致性”来自刻意设计的统一骨架：

- `RuleManager` 容器统一存储
- `SentinelProperty` 统一发布-订阅
- listener 统一编译规则
- 差异只在各规则的编译逻辑

这是一套高度可复用的基础设施，让五种规则（未来更多）都能用同一种方式被配置、加载、更新、查询。

## 锚点

- `AuthorityRuleManager.java:48-67`
- `AuthorityRuleManager.java:69-70`
- `AuthorityRuleManager.java:96-107`
- `SystemRuleManager.java:95-99`
- `SystemRuleManager.java:108-124`
- `SystemRuleManager.java:186-211`
- `ParamFlowRuleManager.java:101-125`
- `NoOpSentinelProperty.java:32-33`
- `SimplePropertyListener.java:21-23`
