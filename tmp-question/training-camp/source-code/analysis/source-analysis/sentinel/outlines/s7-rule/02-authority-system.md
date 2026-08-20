# 黑白名单与系统保护

> S-7 中篇。本文讲两个具体的规则槽：Authority 黑白名单与 System 系统保护。

## 悬念

Authority 和 System 是两种完全不同的保护：一个按调用方(origin)黑白名单，一个按全局系统指标。它们各自怎么判定？

## 一、AuthorityRule:黑白名单

`AuthorityRule` 只有两个核心字段：`strategy`(白名单/黑名单) 和 `limitApp`(允许/拒绝的调用方列表)。

`AuthorityRuleChecker.passCheck` 的判定：

```java
String requester = context.getOrigin();
if (StringUtil.isEmpty(requester) || StringUtil.isEmpty(rule.getLimitApp())) {
    return true;
}
int pos = rule.getLimitApp().indexOf(requester);
boolean contain = pos > -1;
// 按逗号拆分做精确匹配
if (contain) {
    boolean exactlyMatch = false;
    String[] appArray = rule.getLimitApp().split(",");
    for (String app : appArray) {
        if (requester.equals(app)) {
            exactlyMatch = true;
            break;
        }
    }
    contain = exactlyMatch;
}
int strategy = rule.getStrategy();
if (strategy == AUTHORITY_BLACK && contain) {
    return false;
}
if (strategy == AUTHORITY_WHITE && !contain) {
    return false;
}
return true;
```

关键点：

1. origin 为空或 limitApp 为空 → 放行
2. 先用 `indexOf` 粗匹配，再按逗号拆分做精确匹配，避免 `app1` 误匹配 `app10`
3. 黑名单 + 命中 → 拒绝
4. 白名单 + 未命中 → 拒绝

`AuthorityRuleManager` 在加载时强制“一个资源最多一条 authority 规则”，冗余规则被忽略。这是为了避免多条黑白名单语义冲突。

## 二、SystemRuleManager:系统状态采样

`SystemRuleManager` 用一组 volatile 阈值字段保存系统规则，并有一组 `*IsSet` 标志标记用户是否设置过。

静态块启动一个 1 秒周期的 `SystemStatusListener` 采样任务：

```java
scheduler.scheduleAtFixedRate(statusListener, 0, 1, TimeUnit.SECONDS);
```

`SystemStatusListener` 每秒采样系统负载和 CPU 使用率，供 `checkSystem` 读取。

`loadSystemConf` 对每条规则取各指标的最小值，任一指标被设置就打开 `checkSystemStatus` 开关。

## 三、SystemSlot:IN 流量判定

`SystemSlot.entry` 调用 `SystemRuleManager.checkSystem`：

```java
SystemRuleManager.checkSystem(resourceWrapper, count);
fireEntry(context, resourceWrapper, node, count, prioritized, args);
```

`checkSystem` 的判定顺序：

1. 开关关闭 → 放行
2. 非 IN 流量 → 放行（系统保护只针对入站）
3. 总 QPS 超限 → 拒绝
4. 总线程超限 → 拒绝
5. 平均 RT 超限 → 拒绝
6. 系统负载超限 → 用 BBR 算法二次判断
7. CPU 使用率超限 → 拒绝

系统保护用的是 `Constants.ENTRY_NODE` 的全局统计，而不是单个资源的统计。这是“全局入站流量”维度的保护。

## 四、BBR 算法:负载的二次确认

系统负载高不一定是真的过载。`checkBbr` 用 BBR 思想二次判断：

```java
private static boolean checkBbr(int currentThread) {
    if (currentThread > 1 &&
        currentThread > Constants.ENTRY_NODE.maxSuccessQps() * Constants.ENTRY_NODE.minRt() / 1000) {
        return false;
    }
    return true;
}
```

`maxSuccessQps * minRt / 1000` 估算的是“管道容量”（inflight 上限）。如果当前线程数超过这个容量，说明真的过载，返回 false 表示应该拒绝；否则即使负载高，也认为系统还能承受，放行。

这是 BBR 的核心思想：负载高但吞吐未饱和时，不误拒。

## 五、阈值取最小值

`loadSystemConf` 对每条规则取各指标的最小值：

```java
highestSystemLoad = Math.min(highestSystemLoad, rule.getHighestSystemLoad());
maxRt = Math.min(maxRt, rule.getAvgRt());
```

多条系统规则时，取最严格的值。任一指标被设置就打开检查开关。

## 悬念回收

Authority 和 System 是两种不同维度的保护：

- Authority：按 origin 黑白名单，精确匹配，一资源一规则
- System：按全局入站指标，QPS/线程/RT 直接比较，负载用 BBR 二次确认

它们都通过 `RuleManager` + property/listener 管理，但判定逻辑完全不同。

## 锚点

- `AuthorityRuleChecker.java:25-52`
- `AuthorityRuleManager.java:96-120`
- `SystemRuleManager.java:70-76`
- `SystemRuleManager.java:190-230`
- `SystemRuleManager.java:240-285`
- `SystemRuleManager.java:287-293`
- `SystemSlot.java:30-38`
