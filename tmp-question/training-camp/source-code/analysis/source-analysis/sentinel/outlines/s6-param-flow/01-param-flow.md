# 为什么要有独立的热点限流

> S-6 上篇。本文回答：热点参数限流为什么是独立模块，它与普通流控有什么本质区别，参数值从哪来。

## 悬念

Sentinel 已经有 FlowSlot 做流控了，为什么还要一个 ParamFlowSlot？答案是：FlowSlot 只统计“资源维度”，而热点限流要统计“某个参数值维度”。

## 一、资源维度 vs 参数值维度

普通流控看的是“某个资源最近一秒来了多少请求”。但热点限流要回答的是“这个资源里，参数值 = `user123` 的请求最近来了多少”。

这是两个完全不同的统计坐标系：

- FlowSlot：`resource -> passQps/threadNum`
- ParamFlowSlot：`resource -> 参数值 -> passQps/threadNum`

因为参数值维度无法用 `StatisticNode` 表达，所以 Sentinel 没有把它塞进 core，而是做成一个 extension，用 SPI 织入。

## 二、SPI 织入:order = -3000

`ParamFlowSlot` 的声明很简洁：

```java
@Spi(order = -3000)
public class ParamFlowSlot extends AbstractLinkedProcessorSlot<DefaultNode> {
```

对照 S-1 实证的槽序，-3000 让它正好落在 System(-5000) 之后、Flow(-2000) 之前。也就是说，一个请求会先经过热点限流，再经过普通流控。

这个顺序不是偶然：热点限流比普通流控更“精确”，应该优先拦截；如果热点限流已经拒绝了，就不需要再走普通流控。

## 三、参数值从哪来

`ParamFlowSlot.checkFlow` 遍历当前资源的规则，对每条规则做 `applyRealParamIdx` 处理负数下标，然后取参数值：

```java
Object value = args[rule.getParamIdx()];
if (value instanceof ParamFlowArgument) {
    value = ((ParamFlowArgument) value).paramFlowKey();
}
```

这里有两个关键点：

1. `paramIdx` 支持负数（倒数），例如 `-1` 表示最后一个参数。
2. 如果参数值实现了 `ParamFlowArgument`，就调用 `paramFlowKey()` 得到真正的统计 key。

`ParamFlowArgument` 是自定义热点 key 的扩展点。默认情况下参数值直接 `toString`，但如果参数是一个复杂对象，业务可以自己实现 `paramFlowKey()` 来指定用哪个字段做 key。

## 四、无参数就放行

`checkFlow` 的几个 early-return 都体现了“不干扰无参数场景”：

```java
if (args == null) {
    return;
}
if (!ParamFlowRuleManager.hasRules(resourceWrapper.getName())) {
    return;
}
```

- 没有参数 → 放行
- 资源没有热点规则 → 放行
- 参数值为 null → 放行

这是热点限流的边界哲学：它只处理“确实有参数值且配置了规则”的场景，其他情况完全不参与。

## 五、ParamFlowRule 的三要素

`ParamFlowRule` 的核心字段：

- `paramIdx`：取第几个参数
- `grade`：QPS 还是线程数
- `controlBehavior`：默认 / 匀速排队
- `count`：阈值
- `paramFlowItemList` / `hotItems`：特殊热点项，可设独立阈值

`paramFlowItemList` 允许给特定的热点参数值单独设阈值。例如“默认所有参数值 QPS 100，但 `admin` 这个值 QPS 1000”。解析后存到内部 `hotItems` map 里。

## 六、判定与统计的分离

`ParamFlowSlot` 只负责取参数值、调 checker、抛 `ParamFlowException`。真正的统计在 `ParameterMetric` 里，由 callback 织入 `StatisticSlot` 完成（中篇详述）。

这种分离和 FlowSlot 一样：槽是入口，checker 是判定，统计是另一套。但热点限流因为统计维度不同，统计部分整个挪到了 extension。

## 悬念回收

热点限流的“独立”有两层含义：

1. 独立模块：因为参数值维度统计不属于 core 的通用统计。
2. 独立槽：`@Spi(order=-3000)` 让它先于 FlowSlot 拦截。

参数值的来源是 `args[paramIdx]`，配合 `ParamFlowArgument` 可自定义 key。无参数、无规则、null 值都放行。

## 锚点

- `ParamFlowSlot.java:34`
- `ParamFlowSlot.java:49-58`
- `ParamFlowSlot.java:61-88`
- `ParamFlowRule.java:45-83`
- `ParamFlowArgument.java:26`
