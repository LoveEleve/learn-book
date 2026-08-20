# 04. watch 输出一行,trace 画一棵树,tt 重放一次过去 — 追踪命令实现

> 🔴 Deep | 37 KP 中的 6 个(watch/trace/stack/tt 四命令)
> 读者处境: 分发链已通——现在看四个命令各自的"个性": watch 怎么过滤输出、trace 的树怎么长出来、stack 怎么裁剪栈、tt 怎么重放。

### 1. "watch 的过滤与输出" — WatchAdviceListener

场景: `watch -x 3 -e 'params[0] > 100' -n 5 com.example.Service doBiz`——每个参数都对应监听器里的一个动作。

- 参数映射(WatchCommand.java:22 起): `-x` expand(默认1,对象展开深度)/`-n` numberOfLimit(默认100)/`-b` before/`-f` finish(默认)/`-e` exception/`-s` success/`-M` sizeLimit/`-E` regex
- `WatchAdviceListener`(monitor200/WatchAdviceListener.java:20):
  - `before`(:38-45): `threadLocalWatch.start()`(:41)开计时;`-b` 时才输出 `watching(Advice.newForBefore(...))`(:43)
  - `afterReturning`(:48-56): `newForAfterReturning`(:50);`-s` 时输出;`finishing(advice)`(:55)
  - `afterThrowing`(:59-67): `newForAfterThrowing`(:61);`-e` 时输出;finishing
  - `watching`(:76-116): `cost = threadLocalWatch.costInMillis()`(:79)→ **`isConditionMet(conditionExpress, advice, cost)`(:80)** → 命中 → `getExpressionResult(express, advice, cost)`(:87)→ 组装 `WatchModel`(ts/cost/`new ObjectVO(value, expand)`/sizeLimit/类名/方法名/`AccessPoint` 位,:89-102)→ `process.appendResult(model)`(:104)→ `times().incrementAndGet()` → 超 `-n` `abortProcess`(:106-108)
- `isFinish`(:33-35): `-f/-e/-s` 都没给时默认输出所有时机(返回+异常)

关键设计: [模式: 模板方法(四回调汇聚 watching)+ 组合树+节点合并(享元式 TraceTree)+ 环形缓冲(ThreadLocalWatch/tt ring 栈)] **watching 是唯一出口**: 四个回调全部汇到 `watching`,条件过滤/限次/组装只写一次;`-x` 的展开是 `ObjectVO`(arthas-model/ObjectVO.java:12-33——`expand` 字段 + `array()` 工厂 + `expandOrDefault()` :32,注意它在 **arthas-model 模块**而非 core)递归(防循环引用+sizeLimit 截断)——这就是 watch 复杂对象不爆栈的原因。

### 2. "trace 的树怎么长出来的" — 追踪桩 + TraceTree

场景: `trace` 能看到方法内部每个子调用(含行号)——它怎么知道?

- 两套节点来源:
  - **方法入口/出口**: `AbstractTraceAdviceListener.before`(:52)→ `tree.begin(clazz.getName(), method.getName(), -1, false)`(**isInvoking=false**)+ `deep++`;afterReturning(:59)→ `tree.end()`
  - **方法内子调用**: `TraceAdviceListener.invokeBeforeTracing`(:23-27)→ `tree.begin(tracingClassName, tracingMethodName, lineNumber, true)`(**isInvoking=true**)——这些回调来自织入的 SpyTraceInterceptor(@AtInvoke 追踪桩,AR-2 篇 2)
- **树的合并**: `TraceTree.begin`(model/TraceTree.java:30-39): `findChild(current, 类, 方法, 行号)`(:41)——**相同调用点合并**: 循环调 100 次,树上只有一个节点,但 `MethodNode` 累计 min/max/total/times
- **深层计数**: `deep==0` 才是整棵调用树结束(AbstractTraceAdviceListener.java:88)→ `cost = threadLocalWatch.costInMillis()`(:91)→ isConditionMet → `traceEntity.getModel()`(:101: `tree.trim()` + TraceModel)→ appendResult
- **输出**: `TraceView` 树形绘制(`recursive`,view/TraceView.java:151-171,`+---`/`|` 前缀拼树)+ `renderCost`(:118-146,占比=子cost/父totalCost)+ `findMaxCostNode`(:42)最大耗时节点红高亮(:69-71)
- `-p path`: `PathTraceAdviceListener`(空子类)——类匹配扩为 OR、方法全匹配、**不插 invoke 桩**,树靠 before/after 的 deep 嵌套搭出来,行号恒为 -1

关键设计: **两套树来源,一棵树**——[Java: TraceTree 是"前缀树+合并"——findChild 按 (类,方法,行号) 三元组查子节点,命中则复用节点累计统计,这是循环调用不爆炸的关键]: 入口节点(方法级)与子调用节点(调用点级)都挂到同一棵树——`isInvoking` 标记区分类型;findChild 合并是 trace 在循环场景不爆炸的关键(100 次循环 = 1 节点 100 次统计)。`deep` 计数解决嵌套增强的深度归零问题(#1817 防止 deep 为负数)。

### 3. "stack 的栈帧裁剪" — StackAdviceListener

场景: `stack` 输出的栈是干净的业务栈——arthas 自己的帧去哪了?

- `StackAdviceListener`(monitor200/StackAdviceListener.java:19): before 只 start 计时(:33-37);afterReturning/afterThrowing 构造 Advice 后 `finishing`(:53-76): cost + isConditionMet → `ThreadUtil.getThreadStackModel(advice.getLoader(), Thread.currentThread())`(:63)
- **裁剪**: `ThreadUtil.getThreadStackModel`(ThreadUtil.java:402-420): `findTheSpyAPIDepth(stackTrace)`(:381-395,找 SpyAPI 帧的位置)→ **System.arraycopy 切掉 Spy/Arthas 以下的所有帧**(:414-417)
- 链路信息: `getEagleeyeTraceInfo`(ThreadUtil.java:465-497,反射调 EagleEye 取 trace_id/rpc_id)
- 输出: `ThreadUtil.getThreadTitle`(:438-452,`thread_name=...;id=...;TCCL=...`)+ `@类.方法()` + 逐帧 `at 类.方法(文件:行号)`(StackView.java:19-37)

关键设计: **裁剪策略**: 栈里从 `SpyAPI.atEnter` 往下的帧(SpyImpl→AdviceListener→Listener→ThreadUtil)全是 arthas 的,往上才是业务——找到 SpyAPI 帧即切断。内联织入(AR-2 篇 2)让业务帧之上**只有一帧 SpyAPI.atEnter**,裁剪干净利落;若用反射 invoke,这里要多裁 Method.invoke 等 3-4 帧,这就是 inline 设计的连锁收益。

### 4. "tt 的时间隧道" — ring 栈录参 + 反射重放

场景: `tt -t` 录制后,`tt -p -i 1000` 用**当时的参数**重放一次调用——参数怎么存、怎么还原?

- **真实入参保护**: `TimeTunnelAdviceListener`(monitor200/TimeTunnelAdviceListener.java:22): `before` `pushArgs(args)`(:59)存进**固定 512 的 ring stack**(:34-39,注释: 方法执行中 args 可能被业务代码修改,先拷贝);afterReturning `popArgs()`(:67-70)取回
- 存储: `new TimeFragment(advice, LocalDateTime.now(), cost)`(TimeTunnelAdviceListener.java:123)→ `command.putTimeTunnel`(TimeTunnelCommand.java:278-282)→ 全局 `LinkedHashMap<Integer, TimeFragment>` + AtomicInteger 序号(:55-57)
- 查询: `tt -i` 详情(processShow :345-367)/`tt -s` OGNL 搜索(processSearch :398-440)/`tt -w` watch 表达式(ExpressFactory.unpooledExpress(loader).bind(advice).get(...) :381)
- **重放**: `processPlay`(TimeTunnelCommand.java:502-563): `advice.getMethod()`(ArthasMethod,:508-509)→ 不可访问则 `setAccessible(true)`(:513)→ 循环 `method.invoke(advice.getTarget(), advice.getParams())`(:536,ArthasMethod.invoke :155-164)——**ASM 描述符还原参数类型**(ArthasMethod.java:26-102 `Type.getMethodType(methodDesc)` + getDeclaredMethod)→ 结果组装 TimeTunnelModel 输出

关键设计: **重放的诚实性**: 用录制时的 target + params 原样再调一次——所以 `-p` 是有副作用的真实调用(AR-0 篇 4 强调的坑)。ring 栈(512 固定数组)防递归+防泄漏;ArthasMethod 的 ASM 描述符→反射还原,让重放不依赖编译时类型。

---

### 5. "同引擎的两个变体" — monitor 统计与 line 行号

场景: 你想看"这个方法每分钟调用几次、成功率多少"(monitor),或"方法执行到哪一行了"(line)——它们复用同一套增强链。

- **monitor**: `MonitorAdviceListener`(monitor200/MonitorAdviceListener.java:67)——`ConcurrentHashMap<Key, AtomicReference<MonitorData>>`(:72)按方法聚合调用次数/耗时/异常 → `MonitorTimer`(:186)定时输出快照;`isConditionMet`(:141)同样走 OGNL 条件——与 watch 同引擎、不同输出
- **line**: `LineCommandAdviceListener` 消费 atLine 回调——对应 SpyLineInterceptor 的 `@AtLine` 织入(AR-2 篇 2);输出"当前执行到哪一行+局部变量"
- 两者都是 `EnhancerCommand` 的 7 子类之一(AR-2 篇 2 §1)——模板方法的收益: 新增命令只写 Listener

关键设计: **统计状态在监听器里**: monitor 的计数器放在 listener 字段(ConcurrentHashMap),不碰增强字节码——增强只管"喊",统计是监听器的内部状态。这与 watch(每次输出)的差异只在 Listener,增强层零改动。

跨域桥: isConditionMet/getExpressionResult = AR-5(OGNL 引擎);-n 限次 = 本链路的 abortProcess(AR-0 篇 4 的安全规范在此落地);ObjectVO 展开 = AR-5 表达式输出的同一机制;tree 渲染的占比算法 = AR-0 篇 4 的耗时解读。

---

**OpenJDK 关联**:  [OpenJDK 域 24 Frame & Stack — outlines/24-frame-stack/] — stack 命令的栈帧裁剪与 JVM 栈帧结构的对应。

### 核心悬念

**"输出有了——但 'params[0]>100' 是谁在执行?cost 从哪来?"** — 表达式引擎藏在 AdviceListenerAdapter 里,每次回调都从 ThreadLocal 弱引用池借一个 OGNL 求值器——这个池子,藏着 arthas 最深的防泄漏设计。

> → [AR-5 篇 1](../ar5-ognl/01-express-engine.md)
