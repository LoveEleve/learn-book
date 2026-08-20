# 开机自检、日志体系与资源键

> 依赖: 02 SPI(装配机制)| 来源: init 3 文件 + log 14 + slots/logger 2 + config 2 | 后续: S-2 入口(链的消费)
> 覆盖笔记: Q5, Q8, Q11

## 1. 链已经能搭了——但谁先把它"点着"?

前两篇讲完了链的骨架和装配:链长什么样、槽怎么排序、SPI 怎么加载。但还有一个"鸡生蛋"的问题:链是被请求**第一次用到某个资源时**现搭的(CtSph 双检锁里 newSlotChain,见上篇第 6 节),可 Sentinel 有不少组件需要**在收到第一个请求之前**就绪——比如命令端口(S-10)、心跳上报(S-10)、集群初始化(S-9)。谁在启动时把这些拉起来?

答案是 `Env` 类的静态块:

```java
// Env.java:32-36
public class Env {
    public static final Sph sph = new CtSph();
    static {
        InitExecutor.doInit();   // 类加载时触发
    }
}
```

Java 的类加载时机决定了一切:任何代码第一次触碰 Sentinel(几乎都是从 `SphU.entry` 或 `Sph` 开始),都会触发 Env 的类初始化,静态块里跑 `InitExecutor.doInit()`。**Sentinel 的"启动"不是一个显式的 start(),而是首次触碰时的隐式初始化**——这对用户透明,你不需要记得调用任何初始化方法。

## 2. InitExecutor — 双层排序、一次执行

`InitExecutor.doInit()`(InitExecutor.java:42-52)干两件事:找出所有 `InitFunc` 实现,按顺序执行。

```java
// InitExecutor.java:42-52,逻辑节选
if (!initialized.compareAndSet(false, true)) { return; }   // CAS:全局只初始化一次
List<InitFunc> initFuncs = SpiLoader.of(InitFunc.class).loadInstanceListSorted();  // 第一层: @Spi order
// 第二层: insertSorted 按 @InitOrder 重新插入
for (OrderWrapper w : initList) { w.func.init(); }
```

**双层排序**值得展开:先按 `@Spi order`(上篇那套)取列表,再按 `@InitOrder` 排一遍。为什么两套顺序?`@Spi order` 是"加载优先级",`@InitOrder` 是"初始化优先级"——两者语义不同,不能混用。内置的 8 个 InitFunc 分布在各模块:核心 1 个(MetricCallbackInit)、extension 3 个(ParamFlowStatisticSlotCallbackInit、MetricExporterInit、PromExporterInit)、transport 2 个(CommandCenterInitFunc、HeartbeatSenderInitFunc)、cluster 2 个(DefaultClusterClientInitFunc、DefaultClusterServerInitFunc)。其中传输层的两个标了 `@InitOrder(-1)`(CommandCenterInitFunc.java:27)——**命令端口必须最先起**,不然其他初始化想上报/通信都无门。

`initialized` 的 CAS 保证全 JVM 只初始化一次。还有个双触发点:`ClusterStateManager` 静态块也调 doInit(ClusterStateManager.java:51)——集群模块可能比 Env 先被碰到(比如你在应用里先用了集群 API),CAS 保证谁先谁执行,后到的直接跳过。

**一个文档与代码打架的地方**:Env.java 的注释写 "If init fails, the process will exit",但 `doInit` 的实际实现是 catch Exception/Error 后 `ex.printStackTrace()`(InitExecutor.java:57-63)——**不退出,只是打堆栈**。哪个对?以代码为准:单个 InitFunc 抛异常,初始化中断,后续 InitFunc 不再执行,但进程继续跑。后果是"部分初始化"——比如命令端口没起来,但业务还能走。SRE 排查时记住:启动日志里看到 "[InitExecutor] WARN: Initialization failed" 就意味着有组件没就绪,别被"进程还活着"骗了。

## 3. 三套日志 — 为什么 block 日志不走 RecordLog?

链上 LogSlot 记 block 事件走的是 `EagleEyeLogUtil`(上篇第 5 节)。它的背后不是 RecordLog,而是一套独立的日志引擎。Sentinel 的日志体系一共三层:

| 层 | 文件 | 职责 | 实现 |
|---|---|---|---|
| **事件日志** | log/ 包(14 文件) | 启动信息、警告、异常等**低频事件** | `RecordLog` → Logger SPI(可替换)→ 默认 JUL |
| **统计日志** | eagleeye/ 包(15 文件) | 高频统计数据(block 事件等) | 自研批量 + 滚动引擎 |
| **桥接** | slots/logger/ 包 | LogSlot ↔ EagleEye | `EagleEyeLogUtil`("sentinel-block-log") |

**事件日志**这层很朴素:接口 `Logger`,`RecordLog` 在类初始化时先找用户自定义实现(LoggerSpiProvider,SPI 风格),找不到就用 JDK 自带 JUL 的适配器(RecordLog.java:35-39)。低频、量小,标准做法。

**统计日志**这层是重点。block 事件是**每请求都可能触发**的高频数据(一次拒绝就是一条),量级比事件日志高几个数量级。EagleEye(自研引擎)为此设计:批量统计(StatLogger/StatEntry)、滚动文件 Appender(EagleEyeRollingFileAppender)、后台 daemon 刷新(EagleEyeLogDaemon),连自检日志都有令牌桶限流(TokenBucket,10 秒 10 次,EagleEye.java:46)——一套为"高吞吐写入 + 滚动落盘"而生的自研迷你框架。

**为什么自研而不是 SLF4J/Logback?** SLF4J 解决的是"应用日志的桥接与格式化",它主要面向**低频事件日志**的语义;block 日志是统计面——需要批量聚合、需要按时间滚动、需要在高频下不掉性能,还要与指标体系共享格式。用通用日志框架逐条写 block 记录,等于把"统计流水"当"日志"写——格式、频率、性能模型全不对。自研的代价是维护成本,换来的是**统计语义的掌控力**。

顺带 SRE 排障时最常用的一问:block 日志在哪个文件?`EagleEyeLogUtil.statLoggerBuilder("sentinel-block-log")`(EagleEyeLogUtil.java:32)建的 logger,输出到名为 `sentinel-block-log` 的滚动文件。还有日志级别:全局日志级别用 `-Dcsp.sentinel.log.level=DEBUG` 调(LogBase.java:44,默认 INFO)——注意这是 Sentinel 自己的配置,不经过 JUL 的 logger.properties。

## 4. 资源键 — 为什么 chainMap 只比资源名?

上篇第 6 节提到每条业务资源对应一条链,缓存键是 `ResourceWrapper`。看它的 equals/hashCode(ResourceWrapper.java:82-93):

```java
// ResourceWrapper.java:82-93,逻辑节选
public int hashCode() { return getName().hashCode(); }          // 只有名字
public boolean equals(Object obj) { return rw.getName().equals(getName()); }
```

注释原文:"Only getName() is considered"。这意味着 `StringResourceWrapper("getUserById")` 和 `MethodResourceWrapper(方法对象)` 只要名字相同,**判定为同一个键,共享同一条链**(CtSph.java:195-210)。两个包装器类存在,但键只看名字。

为什么只比名字?因为**资源名是规则的统一作用域**:FlowRule 的 resource 字段是字符串、热点规则/熔断规则的 resource 也是字符串——规则绑的是名字,链绑的也必须是名字,不然"同名规则"和"同名链"就对不上了。如果 equals 把类型也算进去,同一个业务资源用 String 包装注册一次、用 Method 包装再注册一次,就会生成两条链、两套规则互不相干——这是 Sentinel 明确不想要的。

推论:一个资源被多个入口注册(比如 Web 适配器注册 "GET:/api/user",业务代码又 SphU.entry("GET:/api/user")),它们共享同一条链、同一份规则集——规则对所有入口一视同仁(规则的存储与下发在 S-7,链只是检查的执行者)。这也是为什么 S-7 规则管理可以按名字精确下发——链的粒度就是名字的粒度。

顺带一个监控面:链的总数可以用 `CtSph.entrySize()`(CtSph.java:223-225)查——上篇的 6000 上限不是黑盒,是公开 API。SRE 可以定时采样 entrySize,接近 6000 就该警觉了。

## 5. 收束 — S-1 的最后一根线

至此 S-1 三篇讲完了一条完整的链的**前半生**:骨架(容器 + 遍历 + 排序)、装配(自有 SPI 的四个能力)、前置条件(启动初始化)+ 运行时辅助(日志分层、资源键)。

把三篇压缩成一张图:

```
首次触碰 Env → InitExecutor.doInit()(双层排序,一次执行)
                    ↓
首个请求 → CtSph.entryWithPriority → lookProcessChain(双检锁,COW 建链)
                    ↓
        DefaultSlotChainBuilder.build() → SpiLoader 加载槽(@Spi order 排序,单例/原型分治)
                    ↓
        chain.entry → 头节点 → 10 个槽逐个 fireEntry(递归,LogSlot 一个 catch 记全部 block)
                    ↓
        超出 6000 条链 / 上下文超限 / 开关关闭 → 静默放行(宁可放行,不可崩溃)
```

下一站 S-2 入口篇:这一整套机制的**消费方**——SphU.entry 怎么进入 Env、Context 怎么创建、NullContext 降级、entry 与 exit 的配对——链在这里被真正"用起来"。资源名这根线也会在那里继续:ContextUtil 的 context 名、origin 的语义,把"名字即作用域"贯彻到底。