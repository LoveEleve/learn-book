# 03. 业务代码喊了一声,监听器怎么听到? — Spy 回调分发链

> 🔴 Deep | 37 KP 中的 6 个(SpyAPI/SpyImpl/AdviceListenerManager/AdviceListenerAdapter/Advice/ThreadLocalWatch)
> 读者处境: 字节码已织入——业务方法每次执行都会调 `SpyAPI.atEnter(...)`。现在的问题是: 这一声"喊",怎么精准送到你那条 `watch` 命令的监听器上?

### 1. "薄薄的转发层" — SpyAPI: 静态方法 + volatile 实例

场景: 增强代码调的是 `SpyAPI.atEnter`——但 SpyAPI 只是转发,真正的逻辑在哪?

- 结构(spy/src/main/java/java/arthas/SpyAPI.java:24-27): `public static final AbstractSpy NOPSPY` + `private static volatile AbstractSpy spyInstance` + `INITED` 标志
- 7 个静态转发方法(:58-87): atEnter/atExit/atExceptionExit/atBeforeInvoke/atAfterInvoke/atInvokeException/atLine——每个都是 `spyInstance.atXxx(...)`
- 安装: `Enhancer` 静态块 `SpyAPI.setSpy(new SpyImpl())`(Enhancer.java:95-99)——**core 一加载就装好**
- 生命周期: `init()`(INITED=true,AR-1 bind 最后一步)/`setNopSpy()`/`destroy()`(INITED=false,AR-1 destroy)

关键设计: [模式: 门面+策略(SpyAPI 静态转发,NOPSPY 空对象兜底)+ 注册表(AdviceListenerManager 分发索引)+ 适配器(AdviceListenerAdapter 签名桥)] **转发层与实现分离**: 目标 JVM 里的增强代码只依赖 bootstrap 上的 SpyAPI(AR-1 注入),SpyImpl 在 ArthasClassloader 里(可卸载)。`NOPSPY` 兜底 = arthas 停止后,业务字节码里的 SpyAPI 调用依然安全(空实现,零开销)——"增强代码不清理也能跑"的最后保证。

### 2. "分发核心" — SpyImpl → AdviceListenerManager

场景: 一次 atEnter 回调,怎么知道要通知哪几个监听器?

- `SpyImpl.atEnter`(core/advisor/SpyImpl.java:28-50): `StringUtils.splitMethodInfo(methodInfo)`(:31)拆出 `方法名|方法描述` → **`AdviceListenerManager.queryAdviceListeners(classLoader, clazz.getName(), methodName, methodDesc)`(:35)** → 逐个 listener: `skipAdviceListener`(:40,实现 :204-217: ProcessAware 且进程已 TERMINATED/STOPPED 则跳过)→ `adviceListener.before(clazz, methodName, methodDesc, target, args)`(:43)
- **索引结构**(AdviceListenerManager.java:101): `ConcurrentWeakKeyHashMap<ClassLoader, ClassLoaderAdviceListenerManager>`——**按 ClassLoader 分桶**;桶内 key = `类名+方法名+方法描述`(trace 追加 owner,:110-112;line 追加行号,:114-116)
- 注册时机: Enhancer.transform 织入时同步注册(Enhancer.java:335-336)——增强与索引**原子完成**
- 为什么弱引用 key: 业务 ClassLoader 被回收时索引自动消失,不泄漏
- [Java: ConcurrentWeakKeyHashMap——并发安全 + key 弱引用,避免"类卸载了索引还挂着"导致的内存泄漏]

关键设计: **分发的核心维度是 ClassLoader**: 同名类被不同 CL 加载(多版本),回调必须回到"增强时那个 CL 下的监听器"——所以查询第一维度就是 classLoader。`splitMethodInfo` 拆的是织入时拼的 `"方法名|方法描述"` 字符串——增强与分发共享同一编码协议(AR-2 篇 2 的 MethodInfo @Binding)。

### 3. "接口适配" — AdviceListenerAdapter: 包装 ArthasMethod

场景: Spy 层回调签名是 `before(Class, String, String, Object, Object[])`,但命令监听器要 `ClassLoader` 和 `ArthasMethod`——怎么对接?

- `AdviceListenerAdapter`(core/advisor/AdviceListenerAdapter.java:18): **final 包装方法**(:49-52)`: before(clazz.getClassLoader(), clazz, new ArthasMethod(clazz, methodName, methodDesc), target, args)`——补充 loader + 构造 ArthasMethod → 调用抽象方法 `before(ClassLoader, Class, ArthasMethod, Object, Object[])`(:85)
- 命令监听器继承 Adapter 实现抽象方法(WatchAdviceListener 等)
- 同时持有 `ProcessAware` 的 process(:40-46)——供 SpyImpl 的 skipAdviceListener 判断进程是否已结束
- 条件与限次辅助: `isConditionMet`(:132-135)/`getExpressionResult`(:137-139)/`isLimitExceeded`(:148)/`abortProcess`(:158)

关键设计: **适配层解决签名演化**: 增强代码(Spy 层)签名冻结,命令层签名自由演化——加 loader/ArthasMethod 不改字节码。`ArthasMethod` 的懒加载反射桥(ASM 描述符→java.lang.reflect)只在需要时(tt replay)才解析,平时零反射开销。

### 4. "调用现场" — Advice 数据模型 + ThreadLocalWatch

场景: 回调拿到原始参数,怎么组织成 watch 输出的"调用现场"?

- `Advice`(core/advisor/Advice.java:12-27): loader/clazz/method(ArthasMethod)/target/params/returnObj/throwExp/lineNumber + **场景标志位**(`AccessPoint`: BEFORE(1)/AFTER_RETURNING(2)/AFTER_THROWING(4)/LINE(8),:147-150 按位与)
- 工厂: `newForBefore`(:153)/`newForAfterReturning`(:170)/`newForAfterThrowing`(:188)/`newForLine`(:207)——每个工厂设对应场景位
- 局部变量: `normalizeLocalVariables`(:247-274,剔除 "this")+ `buildLocalVarMap`(:232-245,名字→值 Map 供 OGNL 用)
- **耗时**: `ThreadLocalWatch`(core/util/ThreadLocalWatch.java:9-94)——**固定 4097 的 ring stack**: `start()` push nanoTime(:24-28),`costInMillis()` pop 求差(:34-36);Listener 在 before 时 start、返回/异常时 costInMillis
- 为什么 ring stack 而不是一个 long: **方法可以嵌套**(A 方法被增强,内部又调被增强的 B)——栈结构保存每层起点;固定数组 + ThreadLocal = 无 ArthasClassLoader 泄漏

关键设计: **位标志的场景语义**: 一次方法调用最多触发 4 种回调,但 watch 只关心"这次是哪个时机"——`advice.isBefore()/isThrow()/isReturn()` 判断(对应 watch 的 -b/-e/-f 参数,AR-0 篇 4);Advice 本身**不含时间戳**,cost 由监听器用 ThreadLocalWatch 算——模型关注"是什么现场",时机归调用方管。

### 5. 全链路回放

```
业务方法执行到织入点
  → SpyAPI.atEnter(clazz, "方法名|描述", target, args)     [bootstrap 上,内联直调]
  → SpyImpl.atEnter: splitMethodInfo → queryAdviceListeners(CL,类,方法,描述)
  → [命中 1..N 个监听器,跳过已终止进程的]
  → adviceListenerAdapter.before(CL, clazz, new ArthasMethod, target, args)
  → WatchAdviceListener.before: threadLocalWatch.start()
  → (业务继续)→ atExit → afterReturning → Advice.newForAfterReturning
  → watching(): cost + isConditionMet(OGNL) → getExpressionResult → WatchModel
  → process.appendResult → View 渲染 → 终端输出
```

关键设计: 全程**无反射、无锁**(除索引查询的并发安全)——这是诊断工具的性能底线: 增强后方法每次调用只多一次静态方法调用 + 一次索引查找,开销纳秒级。

---

跨域桥: SpyAPI 注入 = AR-1 篇 2;织入的调用点 = AR-2 篇 2(10 拦截器);Advice 是 AR-5 的 OGNL 表达式环境;watching 输出细节 = 下一篇。

---

**OpenJDK 关联**:  [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — transformer 回调语义;**另见** [OpenJDK 域 07 ClassFile & ClassLoader — outlines/07-classfile-classloader/] — 按 ClassLoader 分桶的索引依赖加载模型。

### 核心悬念

**"回调到了监听器——'调用现场'怎么变成你屏幕上那行输出?"** — 条件过滤、耗时计算、限次停止,全在一个 watching() 里。而 trace 更复杂: 它要画一棵树——树的节点是"追踪桩"在方法内每个子调用点插出来的。

> → [04-watch-trace-tt.md](04-watch-trace-tt.md)