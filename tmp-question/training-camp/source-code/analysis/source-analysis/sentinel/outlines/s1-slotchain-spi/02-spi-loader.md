# 不依赖 JDK 的扩展机制 — Sentinel 自有 SPI

> 依赖: 01 链骨架(第 2 节的装配行)| 来源: spi 3 文件 + SpiLoaderTest | 后续: 03 启动与日志
> 覆盖笔记: Q2, Q3, Q4, Q9

## 1. 链是搭出来的,槽是"发现"出来的

上篇第 2 节里有一行被一笔带过的代码,它是整个链的装配起点:

```java
// 节选自 DefaultSlotChainBuilder.build()(DefaultSlotChainBuilder.java:39-42)
List<ProcessorSlot> sortedSlotList = SpiLoader.of(ProcessorSlot.class).loadInstanceListSorted();
```

`SpiLoader` 不是 JDK 的东西,是 Sentinel 自己的类。一个成熟的 Java 框架想要"让外部代码扩展自己",通常的做法是 JDK 的 `ServiceLoader`——接口 + `META-INF/services/接口全名` 配置文件 + 运行时按名加载。Sentinel 偏偏自己写了一个 542 行的 `SpiLoader`,还带一堆 JDK 版没有的概念:order、别名、默认实现、单例缓存。

为什么?这一篇回答两件事:SPI 是什么;以及 Sentinel 为什么非要自己的。

## 2. 先回到最朴素的问题:SPI 是什么?

Service Provider Interface,服务提供者接口。它的玩法是"接口与实现分离,实现由外部提供":

1. 框架定义一个接口(比如 `SlotChainBuilder`)
2. 外部实现它,把全限定类名写进 `META-INF/services/com.alibaba.csp.sentinel.slotchain.SlotChainBuilder`
3. 框架运行时读这个文件,按名加载实现类,new 出来用

代码里看不到"new 谁",看到的只有接口名——**实现的选择权从编译期挪到了运行期,从代码挪到了配置**。这是插件化、扩展性的基石,Java 1.6 起标准库就有 `ServiceLoader`。

那为什么不直接用?看需求清单就明白了。

## 3. 需求清单:JDK ServiceLoader 缺了什么

JDK `ServiceLoader` 的能力:加载所有实现、按声明顺序遍历、`findFirst` 取第一个。Sentinel 需要的四样它都没有:

| 需求 | 为什么必需 | JDK ServiceLoader |
|---|---|---|
| **顺序控制** | 槽的执行序是核心语义(NodeSelector 必须先于一切),顺序必须可声明、可排序 | ❌ 只有文件声明序,无法注解排序 |
| **默认实现** | "用户自定义优先,没有就用内置"是扩展的基本模式 | ❌ 无法标记"默认" |
| **别名寻址** | 运行时按名字定向取某个实现 | ❌ 只能全量加载 |
| **单例管理** | 槽/Builder 是共享组件,应当单例复用,不能每次 new | ❌ 每次迭代都 new 新实例 |

其中最要命的是**顺序**。JDK ServiceLoader 的顺序 = 配置文件里的书写顺序,加一个槽改一次文件顺序,还无法表达优先级。而 Sentinel 的槽序是"内建语义"——上篇讲过,9 个内置槽 order 全唯一,ParamFlowSlot 靠一个 -3000 精准插入 System 与 Flow 之间。这种"注解声明优先级"的需求,ServiceLoader 给不了。

顺带一个时间注脚:Sentinel 也不是一步到位的。0.1.0 时代链构建是硬编码(改源码才能加槽);2018-09-12(#145)引入 SlotChainBuilder 接口,用的还是 JDK ServiceLoader——那时只解决"能不能换 Builder";直到 1.8.1(#1383,2021-01-27,"Refactor SpiLoader and enhance SPI mechanism")才自研 SpiLoader,把槽也纳入 SPI。**先换 Builder、后换槽,两次只解决一个需求层次**——这个演进顺序本身就在说:顺序/默认/别名这些能力是逐步被需要的(详见 temporal-trace.md)。

## 4. 加载器全貌 — 缓存、双检锁、两套列表

`SpiLoader` 是 final 类(SpiLoader.java:73),每个 SPI 接口一个实例,结构上六块:

```java
// SpiLoader.java:79-101(字段声明,已节选)
private static final ConcurrentHashMap<String, SpiLoader> SPI_LOADER_MAP;  // 接口 → 加载器,全局缓存
private final List<Class<? extends S>> classList;        // 解析序(未排序,文件声明序)
private final List<Class<? extends S>> sortedClassList;  // 排序序(按 @Spi order)
private final ConcurrentHashMap<String, Class<? extends S>> classMap;  // 别名 → 类
private final ConcurrentHashMap<String, S> singletonMap;               // 类名 → 单例
private final AtomicBoolean loaded = new AtomicBoolean(false);
```

加载入口 `load()`(SpiLoader.java:313-427),整体一次性的:

```java
// SpiLoader.java:314-316 + 328-330,逻辑节选
if (!loaded.compareAndSet(false, true)) { return; }   // CAS:整个解析只做一次
Enumeration<URL> urls = classLoader.getResources(fullFileName);  // 多 jar 同名文件 → 全部 URL
```

几个值得展开的点:

**双缓存**:`classList`(未排序)和 `sortedClassList`(排序后)各存一份。为什么?`loadFirstInstance`/`loadFirstInstanceOrDefault` 需要**声明序**(第 6 节会看到这语义多重要),`loadInstanceListSorted` 需要排序序。一次解析,两种视图,各取所需——排序只在首次加载时做一次(Collections.sort,SpiLoader.java:414-426),之后全走缓存。

**多 jar 合并**:`getResources` 返回的是**所有** classpath 上的同名文件(而不是 getResource 的第一个)。核心 jar 声明 9 个槽、参数流控 jar 声明 ParamFlowSlot——加载器把它们全部读进来合在一起。这就是扩展能"插进链中间"的物理基础:每个 jar 自己声明,合并时按 order 排序。

**CAS 单次语义**:`loaded.compareAndSet(false, true)` 保证整个解析过程全 JVM 只执行一次。注意这个开关在**加载器实例**上,而加载器实例又被 `SPI_LOADER_MAP` 缓存——所以是"每个接口一生一次"。这个单次语义很重要:它把"解析配置文件"这个 IO 开销彻底移出了热路径。

## 5. 单例还是原型 — 一个注解的事

无状态检查槽被所有请求共享(上篇第 7 节的契约),所以加载器默认按单例管理。`createInstance`(SpiLoader.java:459-473)的分支:

```java
// SpiLoader.java:459-473,逻辑节选
if (singleton) {
    instance = singletonMap.get(clazz.getName());
    if (instance == null) {
        synchronized (this) {
            instance = singletonMap.get(clazz.getName());   // 双检锁
            if (instance == null) {
                instance = service.cast(clazz.newInstance());
                singletonMap.put(clazz.getName(), instance);
            }
        }
    }
} else {
    instance = service.cast(clazz.newInstance());   // 原型:每次 new
}
```

默认 `isSingleton = true`(Spi.java:39,注解缺省);需要原型就在实现类上写 `@Spi(isSingleton = false)`。核心代码里的原型消费者有两个——**恰恰是持有资源绑定状态的那两个槽**:NodeSelectorSlot 和 ClusterBuilderSlot(NodeSelectorSlot.java:127, ClusterBuilderSlot.java:49)。它们持有当前资源的节点引用,而链按资源缓存、每条链需要自己的节点实例——若单例共享,不同资源的链会互相覆盖节点。原型的语义是"每链一新":每次 build() 时新建一个,链实例本身持久缓存。其余 7 个无状态检查槽(LogSlot/FlowSlot/Degrade…)全是单例。`singletonMap` 是单例侧"零重复创建"的保证;原型侧不走 singletonMap,直接 new。

## 6. 默认实现 — 三种取法,一个语义

加载器提供三把"取货钥匙",语义容易混,值得放一起看(SpiLoader.java:211-254):

| 方法 | 取谁 | 空时 |
|---|---|---|
| `loadFirstInstance()` | 文件声明的第一个 | null |
| `loadFirstInstanceOrDefault()` | **第一个非默认**;全默认则默认实现 | 默认实现(实现列表全空时为 null) |
| `loadDefaultInstance()` | @Spi(isDefault=true) 那个 | null |

`loadFirstInstanceOrDefault` 是主力:SlotChainProvider 用它取 Builder——你自定义了 Builder,用你的;没自定义,用内置的 `DefaultSlotChainBuilder`(@Spi(isDefault=true))。**"用户自定义优先、内置兜底"** 这个模式贯穿 Sentinel 多个扩展面(CommandHandlerProvider 同样经 SpiLoader 体系,CommandHandlerProvider.java:32-42)。值得注意的是 Logger 是例外:LoggerSpiProvider 注释明说"不能用 SpiLoader,因为它依赖 RecordLog"(LoggerSpiProvider.java:53)——加载器自身要打日志,打日志的组件不能用加载器,循环依赖用"绕过"解决,这个细节很能说明 SPI 体系的分层。

还有个细节:`loadFirstInstance` 和 `firstOrDefault` 遍历的都是**未排序**的 `classList`(声明序),不是 order 序——"第一个"永远是"文件里写的第一个",别和 `loadInstanceListSorted` 的"order 最小"搞混。

## 7. 别名与硬失败 — 有冲突就启动失败

`classMap` 的 key 是**别名**,来自 `@Spi.value()`:

```java
// SpiLoader.java:384-391,逻辑节选
Spi spi = clazz.getAnnotation(Spi.class);
String aliasName = spi == null || "".equals(spi.value()) ? clazz.getName() : spi.value();
if (classMap.containsKey(aliasName)) {
    fail("Found repeat alias name for " + clazz.getName() + " and " + existClass.getName());
}
classMap.put(aliasName, clazz);
```

规则:注解没写 value,别名就是全限定类名;两个类抢同一个别名 → **启动直接 fail**(硬错误)。为什么这么狠?别名是定向寻址的键(loadInstance("别名")),键冲突意味着寻址歧义——运行时才炸不如启动就炸。与之对比,同一个类被多个 jar 重复声明是 warn 跳过(SpiLoader.java:374-376)——因为"合并加载"场景下重复声明无害,只是冗余。

这个 fail 语义值得记住:Sentinel 的配置/加载错误大多是"启动期硬失败",宁可不可用,不可错着用。这与第 6 节的"用户自定义优先"形成一对:扩展冲突启动即暴露,配置缺省静默兜底。

## 8. ClassLoader 策略 — 默认不用 TCCL,这是有意的

加载类用的 ClassLoader 由 `SentinelConfig.shouldUseContextClassloader()` 决定(SpiLoader.java:319-327):

```java
// SpiLoader.java:319-324,逻辑节选
if (SentinelConfig.shouldUseContextClassloader()) {
    classLoader = Thread.currentThread().getContextClassLoader();
} else {
    classLoader = service.getClassLoader();
}
```

默认分支是 `service.getClassLoader()`——**SPI 接口的加载器**,和 JDK ServiceLoader 行为一致。为什么默认不用 TCCL(线程上下文类加载器)?TCCL 的语义是"当前线程的类加载环境",在容器/热部署场景更灵活,但也更不可预测(线程在不同 jar 环境下切换,加载结果跟着变)。Sentinel 选择默认跟接口走——稳定、可复现;想要 TCCL,配 `csp.sentinel.spi.classloader=context` 显式开启(SentinelConfig.java:47,62,339-343)。注意别被名字骗了:`shouldUseContextClassloader()` 返回的是"配置了 context 吗",默认 false。

多 jar 场景(上篇第 4 节 ParamFlowSlot 那种)还有个推论:SPI 文件是 `getResources` 多 URL 合并的,但**类的加载器只有一个**(接口的加载器)——所以扩展 jar 必须能被接口的加载器看到,否则类加载失败。这是"扩展 jar 必须在 classpath 上"的底层原因。

## 9. 扩展点全景 — 三种姿势,一个原则

把第 2~8 节收拢成"扩展 Sentinel 的三种姿势":

1. **加槽**(最常用):实现 ProcessorSlot,标 @Spi(order),注册进 SPI 文件 → 自动按 order 插进链(上篇契约)
2. **换 Builder**:实现 SlotChainBuilder,注册进 `META-INF/services/...SlotChainBuilder` → `loadFirstInstanceOrDefault` 优先用你的(可过滤槽,官方 demo 的 removeIf DegradeSlot 就是这么玩的)
3. **别名定向加载**:`SpiLoader.of(X.class).loadInstance("别名")` → 运行期按名取实现(核心内部零消费,给扩展方用)

一个原则贯穿: **声明式装配**——实现类标注解、写文件,框架在启动时一次解析、按语义排序,热路径零开销。这就是中篇开头那个问题的答案:为什么自研 SPI?因为 JDK ServiceLoader 只解决了"能加载",Sentinel 需要的是"能装配"——顺序、默认、别名、单例,四个词,一个 542 行的加载器。

至此链的"骨架"(上篇)和"装配"(本篇)都齐了。还剩最后一个问题:这套机制在**启动时**是怎么被拉起来的?以及链运行时依赖的日志、资源键这些辅助设施——下篇讲。