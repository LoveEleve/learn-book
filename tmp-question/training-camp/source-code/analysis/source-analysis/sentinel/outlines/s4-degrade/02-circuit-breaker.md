# 状态机与探测请求

> S-4 中篇。本文只讲断路器状态机：CLOSED / OPEN / HALF_OPEN 如何转换，探测请求如何只放一个。

## 悬念

熔断器要“熔断后过一段时间尝试恢复”，但恢复不能突然全放——否则刚恢复又被压垮。Sentinel 用 HALF_OPEN 状态放一个探测请求。问题是：并发下怎么保证只放一个？

## 一、三个状态,一条主线

`CircuitBreaker.State` 只有三个值：CLOSED、OPEN、HALF_OPEN。

`AbstractCircuitBreaker` 用 `AtomicReference<State> currentState` 保存当前状态，初始 CLOSED，还有一个 `volatile long nextRetryTimestamp` 保存下次允许探测的时间。

`tryPass(context)` 是状态机的入口：

```java
if (currentState.get() == State.CLOSED) {
    return true;
}
if (currentState.get() == State.OPEN) {
    return retryTimeoutArrived() && fromOpenToHalfOpen(context);
}
return false;
```

- CLOSED：放行
- OPEN：先判断恢复时间是否到达，再尝试转 HALF_OPEN
- HALF_OPEN：不通过新请求（只有一个探测请求被放过，其余都返回 false）

## 二、CLOSED → OPEN:统计触发

断路器不主动检测，而是由 `onRequestComplete` 在 exit 侧统计后触发 `transformToOpen`。基类里这条转换是：

```java
protected boolean fromCloseToOpen(double snapshotValue) {
    State prev = State.CLOSED;
    if (currentState.compareAndSet(prev, State.OPEN)) {
        updateNextRetryTimestamp();
        notifyObservers(prev, State.OPEN, snapshotValue);
        return true;
    }
    return false;
}
```

只有 CAS 成功的那个调用才真正完成转换并通知观察者；其他并发调用 CAS 失败，什么都不做。

## 三、恢复窗口:时间到了才允许探测

`updateNextRetryTimestamp()` 把 `nextRetryTimestamp` 设为 `now + recoveryTimeoutMs`，其中 `recoveryTimeoutMs = rule.getTimeWindow() * 1000`。

`retryTimeoutArrived()` 判断当前时间是否已经到达这个时间点。所以 OPEN 之后，在 `timeWindow` 秒内，即使大量请求进来，也全部返回 false，不会进入探测。

## 四、OPEN → HALF_OPEN:一个探测请求

恢复时间到达后，请求进入 `fromOpenToHalfOpen(context)`：

```java
if (currentState.compareAndSet(State.OPEN, State.HALF_OPEN)) {
    notifyObservers(State.OPEN, State.HALF_OPEN, null);
    Entry entry = context.getCurEntry();
    entry.whenTerminate(...);
    return true;
}
return false;
```

关键在这里：只有 CAS 成功的那个请求返回 true（被放过作为探测请求），其他并发请求 CAS 失败返回 false。这就是“只放一个探测请求”的实现——不是计数器，而是 `AtomicReference` 的 CAS。

## 五、探测请求的兜底

探测请求进入 HALF_OPEN 时，断路器给它注册了一个 `whenTerminate` 回调：

```java
entry.whenTerminate(new BiConsumer<Context, Entry>() {
    public void accept(Context context, Entry entry) {
        if (entry.getBlockError() != null) {
            currentState.compareAndSet(State.HALF_OPEN, State.OPEN);
            notifyObservers(State.HALF_OPEN, State.OPEN, 1.0d);
        }
    }
});
```

这段是历史修复的兜底：探测请求可能穿过熔断槽，却被后续规则（如流控）拦下。如果没有这个回调，断路器会一直停在 HALF_OPEN 无法恢复。回调在 entry 终止时，如果发现该请求已被 block，就把状态拉回 OPEN。

注意一个边界：这里 `compareAndSet` 的返回值没有被检查，但代码仍然无条件 `notifyObservers`。这意味着这个兜底路径与主转换路径存在不一致——即使 CAS 失败，也会广播一个 HALF_OPEN → OPEN 事件。正文如实记录这个不一致，不粉饰。

## 六、探测结果:CLOSED 或 OPEN

真正决定探测成功/失败的逻辑在子类 `onRequestComplete` 里。基类提供两个转换方法：

```java
protected boolean fromHalfOpenToClose() {
    if (currentState.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
        resetStat();
        notifyObservers(State.HALF_OPEN, State.CLOSED, null);
        return true;
    }
    return false;
}

protected boolean fromHalfOpenToOpen(double snapshotValue) {
    if (currentState.compareAndSet(State.HALF_OPEN, State.OPEN)) {
        updateNextRetryTimestamp();
        notifyObservers(State.HALF_OPEN, State.OPEN, snapshotValue);
        return true;
    }
    return false;
}
```

- 探测成功（无异常/不快）：`fromHalfOpenToClose`，重置统计，回 CLOSED
- 探测失败（异常/慢）：`fromHalfOpenToOpen`，重设恢复时间，回 OPEN

## 七、观察者通知:转换之后

观察者由 `EventObserverRegistry` 管理，按名字注册。`notifyObservers` 遍历当前所有观察者，回调 `onStateChange(prevState, newState, rule, snapshotValue)`。

`snapshotValue` 只在触发 OPEN 时携带触发指标（异常比例/异常数/慢调用比例）；进入 HALF_OPEN 或 CLOSED 时传 null。

通知发生在 CAS 成功之后，是状态机成功转换的副作用，不是转换前的拦截。

## 悬念回收

熔断恢复不是“等时间到了就全放”，而是：

1. OPEN 期间全部拒绝
2. 时间到后，第一个 CAS 成功的请求成为唯一探测请求
3. 探测成功回 CLOSED，失败回 OPEN
4. 探测请求若被后续规则拦下，由 `whenTerminate` 兜底拉回 OPEN

并发安全靠 `AtomicReference.compareAndSet`，不靠锁或计数器。

## 锚点

- `AbstractCircuitBreaker.java:68-77`
- `AbstractCircuitBreaker.java:93-101`
- `AbstractCircuitBreaker.java:104-112`
- `AbstractCircuitBreaker.java:132-147`
