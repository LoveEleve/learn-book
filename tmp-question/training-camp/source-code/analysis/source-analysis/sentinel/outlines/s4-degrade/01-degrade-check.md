# 一条规则如何变成一次熔断

> S-4 上篇。本文只讲入口与规则层：熔断规则如何从配置变成一次 `DegradeException` 判定。

## 悬念

一条熔断规则写进配置后，到底是哪个槽在拦截请求？为什么源码里同时有 `DegradeSlot` 和 `DefaultCircuitBreakerSlot`？

## 一、两个槽,两条规则来源

`DegradeSlot` 消费的是用户显式配置的 `DegradeRule`，从 `DegradeRuleManager.getCircuitBreakers(resourceName)` 取断路器：

```java
List<CircuitBreaker> circuitBreakers = DegradeRuleManager.getCircuitBreakers(r.getName());
for (CircuitBreaker cb : circuitBreakers) {
    if (!cb.tryPass(context)) {
        throw new DegradeException(cb.getRule().getLimitApp(), cb.getRule());
    }
}
```

而 `DefaultCircuitBreakerSlot` 是后来加入的默认断路器槽，`@Spi(order = ORDER_DEFAULT_CIRCUIT_BREAKER_SLOT)`，源码标注 `@since 2.0.0`。它的入口先做一次优先级判断：

```java
if (DegradeRuleManager.hasConfig(r.getName())) {
    return;
}
```

也就是说：如果这个资源已经有显式熔断规则，默认槽直接让路，避免同一资源被两套断路器同时拦截。

所以答案不是“两个槽都在跑同一件事”，而是“显式规则优先，默认规则兜底”。

## 二、一个 entry 判定的完整路径

`DegradeSlot.entry(...)` 只做两步：

```java
performChecking(context, resourceWrapper);
fireEntry(context, resourceWrapper, node, count, prioritized, args);
```

`performChecking` 取当前资源的断路器列表，逐个 `tryPass(context)`；只要有一个返回 false，就抛 `DegradeException`，这个请求被判为降级拦截。

`DefaultCircuitBreakerSlot.entry(...)` 结构相同，只是取的是默认断路器，并且有 `hasConfig` 短路。

## 三、DegradeRule 的三种 grade

`DegradeRule` 的 `grade` 决定构造哪种断路器：

- `DEGRADE_GRADE_RT`：慢调用比例 → `ResponseTimeCircuitBreaker`
- `DEGRADE_GRADE_EXCEPTION_RATIO`：异常比例 → `ExceptionCircuitBreaker`
- `DEGRADE_GRADE_EXCEPTION_COUNT`：异常数 → `ExceptionCircuitBreaker`

`DegradeRuleManager.newCircuitBreakerFrom(...)` 就是这个 switch：

```java
switch (rule.getGrade()) {
    case DEGRADE_GRADE_RT:
        return new ResponseTimeCircuitBreaker(rule);
    case DEGRADE_GRADE_EXCEPTION_RATIO:
    case DEGRADE_GRADE_EXCEPTION_COUNT:
        return new ExceptionCircuitBreaker(rule);
    default:
        return null;
}
```

所以“熔断策略”不是槽决定的，而是规则 grade + 断路器实现决定的。

## 四、规则校验:不合法的规则不生效

`DegradeRuleManager.isValidRule(...)` 在规则加载时逐条过滤，不合法的直接忽略并打警告。校验包括：

- resource 非空、count >= 0、timeWindow > 0
- minRequestAmount > 0、statIntervalMs > 0
- RT 策略要求 slowRatioThreshold 在 [0, 1]
- 异常比例策略要求 count <= 1

这些校验决定了“什么规则能被真正建成断路器”。默认规则还有额外要求：resource 必须是 `"*"`(`DefaultCircuitBreakerRuleManager.isValidDefaultRule`)。

## 五、规则加载与断路器复用

规则不是每次都新建断路器。`DegradeRuleManager` 的 listener 收到新规则后，会逐条 `getExistingSameCbOrNew(rule)`：

```java
for (CircuitBreaker cb : cbs) {
    if (rule.equals(cb.getRule())) {
        return cb;   // 规则不变,复用旧断路器
    }
}
return newCircuitBreakerFrom(rule);
```

规则完全相同时复用旧断路器，保留其 CLOSED/OPEN 状态和统计窗口；规则变了才新建断路器，状态从 CLOSED 重新开始。这是“规则身份决定状态连续性”的关键。

默认规则的复用语义略有不同：默认规则资源名是 `"*"`，reload 时为已缓存的具体 resource 重建断路器列表。所以默认规则不能简单套用“按 resource 精确复用”的结论。

## 六、被拦之后,exit 侧统计

`DegradeSlot.exit(...)` 会先检查当前 entry 是否已经 block：

```java
if (curEntry.getBlockError() != null) {
    fireExit(context, r, count, args);
    return;
}
```

只有真正通过、没有被拦的请求，才遍历断路器调用 `onRequestComplete(context)`，把异常/慢调用统计喂给断路器。默认槽的 exit 侧逻辑同构，只是多了一层 `hasConfig` 短路。

这条路径很重要：熔断的统计不是靠定时采样，而是靠每次真正完成的请求在 exit 侧反哺。

## 悬念回收

熔断规则到拦截的完整链路是：

```text
property 事件
  -> listener 校验 + 复用/新建断路器
  -> DegradeSlot / DefaultCircuitBreakerSlot
  -> tryPass 逐个断路器
  -> false 抛 DegradeException(拦截)
  -> 通过后 exit 侧 onRequestComplete 反哺统计
```

显式规则走 `DegradeSlot`，默认规则走 `DefaultCircuitBreakerSlot`，二者通过 `hasConfig` 保证显式优先。

## 锚点

- `DegradeSlot.java:43-56`
- `DegradeSlot.java:62-80`
- `DefaultCircuitBreakerSlot.java:30-58`
- `DegradeRuleManager.java:145-157`
- `DegradeRuleManager.java:165-175`
- `DegradeRuleManager.java:177-200`
- `DefaultCircuitBreakerRuleManager.java:158-168`
