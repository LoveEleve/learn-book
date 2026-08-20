# 04. 慢接口和幽灵参数 — watch/trace/stack/tt

> 🟢 使用域 | 覆盖: watch/trace/stack/tt
> 读者处境: 一个接口偶发超时,参数有时不对,异常只出现一次——日志不够用,你要"看"方法调用现场。

### 1. "方法每次调用的入参返回值" — watch

场景: 怀疑某个方法的入参/返回值有问题。

- `watch org.example.Service doBiz`: 每次调用输出入参+返回值+耗时(monitor200/WatchCommand.java:22 `@Name("watch")`)
- `watch -x 3`: 对象展开 3 层(默认 1 层,复杂对象要加深)
- `watch -n 5`: 只输出 5 次(默认 100,高频方法必须限次)
- `watch -b/-f/-e/-s`: 只看 before/afterReturning/exception/success 时机
- 条件过滤: `watch -e 'params[0] > 100'` 只观察满足条件的调用
- 命令定义: WatchCommand.java:69 `@Argument(index=3, argName="condition-express")`

关键设计: watch 是**字节码增强**实现的(源码 AR-2: SpyAPI 织入方法前后)——所以 `-x` 展开本质是对象图序列化,`-e` 条件是 OGNL 表达式(AR-5)。性能注意: 增强是有开销的,`-n` 限次、条件过滤是必选项,否则生产方法被高频 watch 反而制造问题。

生产注意: 高危操作是 `watch` 高频方法 + 无限次——先 `-n 10` 试跑;`-x` 别开太大(对象图爆炸)。

### 2. "时间都花在哪了" — trace

场景: 接口 2 秒才返回,不知道慢在内部哪一步。

- `trace org.example.Service doBiz`: 方法内部调用树,每节点显示耗时+占比+次数(monitor200/TraceCommand.java:31 `@Name("trace")`)
- 耗时列解读: 单次显示 `[xx.xx% costms]`,多次显示 `[min=,max=,total=,count=]`
- 定位到最慢子方法后,可以**级联 trace**: 再 trace 那个子方法,逐层下钻
- `trace -n 5`: 输出 5 次后自动停
- 条件: `trace -e 'cost > 1000'` 只看超过 1s 的调用

关键设计: trace 的下钻能力来自 `@AtInvoke` 拦截器——[ASM: 织入的是方法体内每个方法调用点的 MethodInsnNode 前/后桩]——它织入的是**方法体内每个方法调用点**(源码 AR-2: SpyTraceInterceptor + invoke 追踪桩),所以能看到"方法内部哪一行调了什么、花了多久",这是 jstack 做不到的运行时视角。

生产注意: trace 的增强比 watch 更重(每个调用点都插桩),`-n` 限次必填;trace 结果里 `[0.00%]` 的节点通常是 JDK 内部调用(被 `--skipJDKTrace` 排除)。

### 3. "这个方法被谁调用" — stack

场景: 想知道某个方法的调用来源(谁在调它、什么链路)。

- `stack org.example.Service doBiz`: 方法命中时打印调用栈(monitor200/StackCommand.java:22 `@Name("stack")`),输出 `@类.方法()` + 逐帧 `at 类.方法(文件:行号)`
- 含 trace_id/rpc_id(EagleEye 链路标识)
- 条件过滤: `stack -n 3 'params[0]=="pay"`

关键设计: stack 输出会**裁剪掉 Spy/Arthas 自身栈帧**(源码 AR-2: `findTheSpyAPIDepth`,ThreadUtil.java:381)——你看到的是业务调用链,不含 arthas 自己的污染帧。

生产注意: 配合 trace_id 使用能直接定位到具体一次请求的完整链路。

### 4. "这个方法正常吗" — monitor 与 line

场景: 不想手动盯 watch,想看"这个方法每分钟调几次、成功率多少"。

- `monitor org.example.Service doBiz`: 按方法聚合统计,定时刷新(monitor200/MonitorCommand.java:22 `@Name("monitor")`)— 输出调用次数/成功率/耗时
- `line org.example.Service doBiz`: 显示方法执行到**哪一行**+当前局部变量(monitor200/LineCommand.java:32)——"走到哪一步停了"的定位神器
- 两者与 watch/trace 同一套增强引擎(EnhancerCommand 7 子类),差异只在监听器

生产注意: monitor 常驻有持续开销,和 dashboard 一样用完即停。

### 5. "偶发异常抓现场" — tt 时间隧道

场景: 问题每天出现一两次,watch 守着也错过——把调用**录下来**,事后重放。

- `tt -t org.example.Service doBiz`: 开始录制(monitor200/TimeTunnelCommand.java:39 `@Name("tt")`)
- `tt -l`: 列表,`tt -i 1000`: 看某次调用的完整现场(参数/返回值/异常/耗时)
- `tt -s 'method.name=="doBiz" && params[0]==123'`: 表达式搜索
- `tt -p -i 1000`: **重放**那次调用(replay,用当时的参数再调一次)
- 注意: tt 有容量上限,老记录被覆盖

关键设计: tt 录的是**方法调用的完整参数/返回值快照**(源码 AR-2: TimeFragment + 512 ring 栈存真实入参)——配合 `-p` 重放,可以"复现"过去的一次调用,这是其他诊断工具没有的能力。

生产注意: tt 只对增强的类方法生效,用完 `reset` 清理;重放有副作用(真实调用业务方法),`-p` 前想清楚。

---

跨域桥: watch/trace/stack/tt 全部 = AR-2 EnhancerCommand 模板 7 子类 + AdviceListener 回调;条件表达式 = AR-5 OGNL;tt replay = AR-2 ArthasMethod 反射桥。

---

**OpenJDK 关联**:  [OpenJDK 域 47 Instrumentation — outlines/47-instrumentation/] — ClassFileLoadHook 是 watch/trace 织入的 JDK 侧入口。

### 核心悬念

**"watch 是怎么'看'到方法调用的?"** — 不是 AOP 代理,不是反射——是字节码级织入 + 回调分发。你敲的每个参数(-x/-n/-e)最后都变成监听器里的一行判断。

> → [AR-2 篇 2](../ar2-watch-trace/02-bytekit-enhancer.md) + [AR-2 篇 4](../ar2-watch-trace/04-watch-trace-tt.md)
