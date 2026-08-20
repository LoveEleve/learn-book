# 06. 直接操作线上对象,画出 CPU 火焰图 — ognl 与 profiler

> 🟢 使用域 | 覆盖: ognl/profiler
> 读者处境: watch 只能看方法调用,你想"直接读"线上某个对象的状态;或 CPU 高但线程栈看不出热点,想要一张火焰图。

### 1. "线上对象随手读" — ognl

场景: 内存里有个 Map 一直变大,你想看它现在的内容/大小/持有者。

- 调静态方法: `ognl '@java.lang.System@out.println("hi")'`(klass100/OgnlCommand.java:31 `@Name("ognl")` "Execute ognl expression")
- 读静态字段: `ognl '@com.example.Config@INSTANCE.timeout'`
- 指定类加载器: `ognl -c <classLoaderHash> '...'`(多版本共存时必须)
- 拿到实例读成员: 先 `watch -b` 拿 target 的引用 id,再 `ognl -x 2 'target.fieldName'`
- 表达式环境(在 watch 里也一样): `params[0]`/`target`/`returnObj`/`throwExp`/`clazz`/`method`/`cost`(command/Constants.java:12-18)

关键设计: ognl 直接 `Ognl.getValue`(源码 AR-5: OgnlExpress)——[Java: OGNL 表达式在指定 ClassResolver 下解析 `@类@成员` 语法;Arthas 用 DefaultMemberAccess 放开 private 访问,不走反射 setAccessible]——所以线上能读 private 字段;但这也意味着表达式是**强执行**:表达式里的方法调用会真实执行,写错表达式可能调出副作用。

生产注意: 只读不写是纪律;`-x` 限制输出深度防刷屏;ognl 前先 `sc -d` 确认 classLoaderHash,避免操作到错误的类版本。

### 2. "CPU 热点长什么样" — profiler 火焰图

场景: CPU 高但 `thread -n 3` 的栈看不出名堂(比如并发场景多线程轮流跑),需要聚合视角。

- `profiler start`: 开始采样(monitor200/ProfilerCommand.java:48 `@Name("profiler")` "Async Profiler")
- `profiler start --event alloc`: 采样分配(找谁在 new 对象,排查内存增长)
- `profiler stop --format html /tmp/flame.html`: 结束并输出火焰图
- 支持事件: cpu(默认)/alloc(分配)/lock(锁竞争)/wall-clock
- 打开 html: 火焰图 x 轴=采样占比(越宽越热),y 轴=调用栈深度;自上而下看调用链,自下而上最宽的是热点

关键设计: 火焰图是**采样式**(源码 AR-6: ProfilerCommand → async-profiler 的 native 采样),不是插桩——对业务零侵入、可长时间采样;`--event lock` 还能直接画出**锁竞争**火焰图,这是排查"接口偶发慢"的利器。

生产注意: profiler 采样期间有轻微开销,建议 30s-2min 内;火焰图 top-down 第一眼找"宽平"的栈(自己的代码,不是库代码)。

### 3. "三招组合拳" — 完整排查套路

场景: 一次完整的线上 CPU 高排查。

```
1. thread -n 3         → 谁在烧 CPU(线程名+栈)
2. jad 定位到代码行     → 是不是业务代码
3. trace 方法内部       → 慢在哪个调用点
4. profiler 30s 火焰图  → 聚合确认热点(多线程场景)
5. 修复后 reset/stop   → 清理现场
```

关键设计: thread(瞬时采样)→ trace(单点插桩)→ profiler(聚合采样)是**三种互补视角**——瞬时/逐调用/统计,排查步骤从粗到细,每一步都在缩小范围。

生产注意: 每步都要限次/限时,别让诊断工具本身成为新故障源。

---

跨域桥: ognl 表达式引擎 = AR-5(ExpressFactory/OgnlExpress/DefaultMemberAccess);profiler 命令层 = AR-6(ProfilerCommand → async-profiler execute)。

---

**OpenJDK 关联**:  [OpenJDK 域 32 JFR — outlines/32-jfr/] — profiler 与 JFR 同为采样型观测,JFR 是 JDK 内置的采样框架。

### 核心悬念

**"params[0]>100 到底是哪一行代码在求值?火焰图是采样还是插桩?"** — 一个藏在回调链的最底层(表达式是最后一层闸门),一个根本不是 arthas 实现的(委托给 async-profiler)。两句"为什么"带你进源码。

> → [AR-5 篇 2](../ar5-ognl/02-express-usage.md) + [AR-6 篇 2](../ar6-profiler/02-profiler-boundary.md)
