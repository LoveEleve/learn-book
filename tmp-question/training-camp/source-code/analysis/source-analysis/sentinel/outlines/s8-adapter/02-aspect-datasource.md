# 注解、异常与配置推送

> S-8 中篇。本文讲两个看似不同、实际都属于“翻译层”的扩展：`SentinelResourceAspect` 把注解翻译成 entry，DataSource 把外部配置翻译成 property 更新。

## 悬念

一个 `@SentinelResource` 方法被拦时，blockHandler、fallback、Tracer 谁先执行？一个 Nacos 配置变了，又是怎么一路到达 RuleManager 的？

## 一、SentinelResourceAspect:注解到 SphU

AspectJ 切面先解析目标方法和 `SentinelResource`：

```java
String resourceName = getResourceName(annotation.value(), originMethod);
EntryType entryType = annotation.entryType();
int resourceType = annotation.resourceType();
```

资源名优先使用 annotation value，没有则按方法名解析。然后：

```java
entry = SphU.entry(resourceName, resourceType, entryType, pjp.getArgs());
return pjp.proceed();
```

这一步把 annotation 的资源类型、流量方向、方法参数全部传入 S-2 的统一 entry 模型。

## 二、BlockException: blockHandler 优先

如果 SphU entry 抛出 `BlockException`，切面进入 `handleBlockException`：

1. 如果配置了 `blockHandler`，反射找到并调用它
2. 没有 blockHandler，则进入普通 fallback
3. 两者都没有，异常继续抛出

blockHandler 的参数是原方法参数后追加 `BlockException`。方法可以位于目标类，也可以通过 `blockHandlerClass` 指定静态处理类。

这条链专门处理“规则拒绝”，不把 BlockException 当业务异常。

## 三、业务异常:ignore → trace → fallback

如果 `pjp.proceed()` 抛业务异常，切面顺序是：

```text
exceptionsToIgnore 命中?
  -> 原样抛出，不 trace，不 fallback
否则 exceptionsToTrace 命中?
  -> Tracer.trace
  -> handleFallback
否则
  -> 原样抛出
```

`exceptionsToIgnore` 优先级高于 `exceptionsToTrace`。fallback 支持：

- 与原方法参数完全一致
- 原方法参数后追加 Throwable
- `defaultFallback`
- `fallbackClass` 中的静态方法

`ResourceMetadataRegistry` 缓存解析出的 Method，避免每次异常都重新反射查找。

## 四、finally 保证 exit

无论业务正常、被 block、fallback 成功还是业务异常，只要 Entry 已经创建，finally 都会调用：

```java
if (entry != null) {
    entry.exit(1, pjp.getArgs());
}
```

这与 S-2 的生命周期约束完全一致：entry 创建成功，就必须 exit；block 发生在 entry 创建阶段时 entry 可能为空，因此不会重复 exit。

## 五、AbstractDataSource:外部配置到 property

`AbstractDataSource<S,T>` 持有两样东西：

```java
protected final Converter<S, T> parser;
protected final SentinelProperty<T> property;
```

`loadConfig()` 只负责读取并转换：

```text
readSource()
  -> parser.convert(conf)
  -> T rules
```

具体的 Nacos/Apollo/Consul/Etcd/Redis/ZK 数据源负责读取外部系统并调用 `getProperty().updateValue(newValue)` 触发刷新；Converter 负责把字符串/配置对象转换成目标规则类型。

`getProperty()` 把 property 暴露给 RuleManager 的 `register2Property`：

```text
Nacos/Apollo/... DataSource
  -> Converter
  -> 具体 DataSource 调 SentinelProperty.updateValue
  -> RuleManager PropertyListener
  -> 运行时规则
```

DataSource 不直接调用 FlowRuleManager 或 AuthorityRuleManager，它只生产 property。这使数据源和规则类型解耦。

## 悬念回收

两个扩展的共同点是“翻译”：

- AspectJ：注解/方法异常 → SphU entry/Tracer/fallback
- DataSource：外部配置 → Converter → SentinelProperty → RuleManager

它们都不改变 core 的限流/统计语义，只把外围框架或配置中心的生命周期接入已有抽象。

## 锚点

- `SentinelResourceAspect.java:37-49`
- `SentinelResourceAspect.java:50-73`
- `AbstractSentinelAspectSupport.java:92-113`
- `AbstractSentinelAspectSupport.java:124-160`
- `AbstractDataSource.java:24-48`
