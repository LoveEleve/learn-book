# 从"一条链"说起 — ProcessorSlot 责任链的骨架

> 依赖: 无(S-1 首篇)| 来源: slotchain 11 文件 + DefaultSlotChainBuilderTest | 后续: 02 自有 SPI(链的装配来源)
> 覆盖笔记: Q10, Q1, Q6, Q7

## 1. 一个请求进来,它面对的是什么?

假设你在接口里写了这么一行:

```java
Entry entry = SphU.entry("getUserById");   // [伪代码] 真正写法见 S-2
```

Sentinel 要检查的东西很多:这个接口允许多少并发?每秒能过多少流量?要不要熔断?来源 IP 在白名单里吗?系统整体负载扛得住吗?这些检查如果每次都在业务代码里串一遍,业务代码早被塞满。Sentinel 的做法是:把检查组织成一条**责任链**,每个环节一个槽(slot),业务代码只需要碰链的头。

于是问题变成三个:链长什么样?请求怎么沿链走?链出了状况怎么办?这一篇把链的骨架讲透;槽"从哪来、按什么顺序排"的装配机制留到中篇。

先给一个参照系:GOF 责任链模式中,链的组成和顺序由**客户端**在运行时拼装,每个 handler 自行决定是否传递;Sentinel 的链恰恰相反——顺序由 SPI 声明(见第 4 节),链在构建期一次性成型,运行期每个槽必须传递(除非自己就是终点)。前者是"客户端组织",后者是"配置声明、框架组织"。记住这个差异,后面读链序就顺了。

## 2. 链的骨架 — 一个匿名头节点撑起整条链

`DefaultProcessorSlotChain`(DefaultProcessorSlotChain.java:11-64)是链的容器实现,结构极简:一个**匿名头节点** `first` + 一个尾指针 `end`。

```java
// 节选自 DefaultProcessorSlotChain.java:11-19(完整签名含 prioritized 参数)
AbstractLinkedProcessorSlot<?> first = new AbstractLinkedProcessorSlot<Object>() {
    @Override
    public void entry(...) { super.fireEntry(context, resourceWrapper, t, count, args); }
    @Override
    public void exit(...)  { super.fireExit(context, resourceWrapper, count, args); }
};
AbstractLinkedProcessorSlot<?> end = first;
```

头节点自己不干任何检查,它的 entry 只做一件事——`super.fireEntry`,也就是把事件递给下一个真实槽。`end` 指向链尾,`addLast` 就是"挂在 end 后面并推进 end"。

为什么要这个"空头节点"?因为链有两个插入入口:`addFirst`(插到最前)和 `addLast`(追加到末尾)。没有头节点,`addFirst` 就得特判"链是空的吗"——每次插入都要检查边界。头节点把两种插入统一成同一个操作:`addFirst` 在 first 之后插入,空链时 end 也指向它;`addLast` 在 end 之后插入。**边界条件被折叠进容器结构**,调用方永远不用判断空链(DefaultProcessorSlotChain.java:24-33)。

顺带一提:链容器从 0.1.0 首版 commit(c92fea5d,2018-07-23)到现在**结构分毫未动**——git diff 实证的唯一差异是 1.8.0 起 entry 多了一个 `prioritized` 参数(配合 OccupyTimeout 抢占特性,见 S-3),核心骨架经住了 7 年演进,详见 temporal-trace.md。

## 3. 链怎么走 — fireEntry 递归,entry 和 exit 的双向不对称

槽的抽象在 `AbstractLinkedProcessorSlot`(AbstractLinkedProcessorSlot.java:29-47),两对方法:

```java
// 节选自 AbstractLinkedProcessorSlot.java:16-38,参数已缩写
public void fireEntry(...) { if (next != null) { next.transformEntry(...); } }   // 递给下一个
void transformEntry(...)   { T t = (T) o; entry(context, resourceWrapper, t, ...); }  // 泛型桥
public void fireExit(...)  { if (next != null) { next.exit(...); } }
```

请求从 `DefaultProcessorSlotChain.entry` 进入(first.transformEntry)→ 头节点 fireEntry → 第一个真实槽的 entry → 该槽做完自己的检查,调用 `fireEntry` → 下一个槽……直到链尾。**这不是循环,是递归**——每层调用在调用栈上深一层。

一个耐人寻味的细节:**entry 方向有泛型转换(transformEntry),exit 方向没有**。原因在签名:entry 的第三个参数 `obj` 是泛型 `T`,而链上每个槽的 T 不同(NodeSelectorSlot<DefaultNode>、FlowSlot<DefaultNode>…),Object 沿链传递时必须 cast 成各槽自己的 T;exit 的签名(AbstractLinkedProcessorSlot.java:44-47)没有泛型参数,直接传即可。这个不对称不是设计偏好,是 Java 类型系统强制的。

为什么递归而不是 for 循环?链上每个槽都要包裹自己的异常处理(比如 LogSlot 要 catch 下游的 BlockException,见第 5 节)——递归让每个槽的 try-catch 天然成为"下游全部槽"的 catch 边界;循环的话,异常要么全链共享一个 catch(丧失粒度),要么自己造栈模拟。链深度固定 10 槽左右,递归栈深没有风险,递归是更诚实的选择。

## 4. 链怎么排 — 每个槽自己声明位置

链的容器和遍历都看完了,还有一个基本问题:谁决定"NodeSelector 必须第一个、Degrade 必须最后一个"?

答案在 1.8.1 之前是:硬编码。`DefaultSlotsChainBuilder.build()` 里手写八行 `addLast`(0.1.0 原版,git show 可查),加一个槽就要改源码重新编译。1.8.1 重构(#1383,2021-01-27)后,每个槽用自己的注解声明位置:

```java
@Spi(order = Constants.ORDER_NODE_SELECTOR_SLOT)   // = -10000
public class NodeSelectorSlot extends AbstractLinkedProcessorSlot<DefaultNode>
```

`@Spi(order)` 的数值来自 Constants(Constants.java:75-86),9 个内置槽 **order 全部唯一**,从 -10000 一路到 -1000。全是负数是给用户留的约定:内置槽占负区间,用户自定义槽可以用正数(order 越大越靠后执行)。

- NodeSelectorSlot **-10000** → 建资源节点(为什么它必须第一:后面的统计和检查都要落到这个节点上,节点必须先于一切检查存在)
- ClusterBuilderSlot **-9000** → 建集群节点(同一个资源的统计归并点)
- LogSlot **-8000**(第 5 节)
- StatisticSlot **-7000** → 统计(过了检查才计数,语义见 S-5)
- AuthoritySlot **-6000** → 黑白名单
- SystemSlot **-5000** → 系统自适应保护
- ParamFlowSlot **-3000**(extension 模块,热点参数,见 S-6)
- FlowSlot **-2000** → 流控
- DefaultCircuitBreakerSlot **-1500** → 熔断计数
- DegradeSlot **-1000** → 熔断判定(见 S-4)

注意 ParamFlowSlot 不在核心包里,它靠 SPI 文件注册、order=-3000 恰好插进 System 与 Flow 之间——**链序是"声明出来的",不是"编出来的"**。排序算法本身:比较器只比 order(Integer.compare,升序小值先执行),order 相同时 Java 稳定排序保持 SPI 文件声明顺序——只是内置槽 order 全唯一,这个兜底从没被触发过(SpiLoader.java:415-426)。

还有个有意思的演进事实:Authority 和 System 的顺序在 1.7.0 对调过(0.1.0 是 System→Authority)。合理的解读是"先拒后查"——把便宜的拒绝逻辑(黑白名单)提到昂贵的系统保护检查前面,越便宜的检查越靠前(1.7.0 的 DefaultSlotChainBuilder 源码实证,对调动机 commit 说明待查)。链序不是一次性设计,是演进产物。

## 5. LogSlot — 为什么 10 个工位只有它写日志?

槽序表里有一个"位置即设计"的例子:LogSlot 卡在第三位(-8000),前面只有两个建节点的槽。看它的实现就懂了(LogSlot.java:34-46):

```java
// 节选自 LogSlot.java:34-46,参数已缩写
public void entry(...) throws Throwable {
    try {
        fireEntry(context, resourceWrapper, obj, count, prioritized, args);  // 递出——让下游全部检查执行
    } catch (BlockException e) {
        EagleEyeLogUtil.log(resourceWrapper.getName(), e.getClass().getSimpleName(),
            e.getRuleLimitApp(), context.getOrigin(), e.getRule() != null ? e.getRule().getId() : null, count);
        throw e;   // 记完继续往上抛,不吞异常
    } catch (Throwable e) {
        RecordLog.warn("Unexpected entry exception", e);
    }
}
```

关键在于 `fireEntry` 被包在 try 里:递归的异常会沿调用栈**向上冒泡**,所以 LogSlot 的 catch 块接住的是"链上它之后所有检查槽"抛出的 BlockException——一个 catch 点,覆盖 Authority/System/ParamFlow/Flow/CircuitBreaker/Degrade 全部 6 个检查槽的拒绝事件。这就是 LogSlot 只需要一个实例、却把全部检查槽的 block 都记下来的原因:位置在检查链之前,行为在检查链之后,一条 try 把"全部"折叠了。

它记录 6 个字段:资源名、异常类型名(FlowException/SystemException…)、limitApp(规则限定的应用)、origin(调用来源)、规则 ID、计数——写进 EagleEye 的 `sentinel-block-log`(日志体系的分工见下篇)。顺带:它只 catch BlockException,其他意外异常(比如槽的 bug 抛 NPE)走 RecordLog.warn——block 日志与错误日志严格分流,不污染统计日志。

## 6. 链的降级 — 6000 条链上限,超了怎么办?

每条业务资源对应一条链(CtSph.chainMap 缓存,键的语义见下篇 Q11)。资源数量无限,链不能无限建——`CtSph.lookProcessChain`(CtSph.java:194-215)维护一个上限:

```java
// 节选自 CtSph.java:201-203,完整逻辑含双检锁
if (chainMap.size() >= Constants.MAX_SLOT_CHAIN_SIZE) {   // 6000
    return null;   // 超过:不给链
}
```

`MAX_SLOT_CHAIN_SIZE = 6000`(Constants.java:37)。超过之后,**新资源静默放行**:调用方拿到 null 链,返回一个不带链的 CtEntry,规则检查全部跳过(CtSph.java:136-142)。注意这个设计选型:**降级放行,而不是 LRU 驱逐**。为什么?驱逐意味着"已建链可能失效",一个正在被高频调用的资源如果链被淘汰,下次调用要重建——在流量高峰期,驱逐引发的重建风暴比"少数边缘资源不受保护"更危险。静默放行是资源侧的保守策略:牺牲边缘资源,保住整体稳定。

链表的更新方式也值得一说:不用 ConcurrentHashMap,而是 **copy-on-write**——新建 HashMap,putAll 旧内容,put 新链,然后整体替换 volatile 引用(CtSph.java:206-210)。为什么?链的读取是每请求高频路径,写入只在第一次见到新资源时发生;COW 让读路径完全无锁(volatile 引用读),写路径付出一次全量复制的代价换读取零开销——读多写少的典型换法。这也正是 chainMap 必须用 volatile + COW、双检锁才能并发安全的原因。

三种"放弃保护"的路径由此收敛:链超限(本节的 6000)、上下文超限(NullContext)、全局开关关闭(Constants.ON=false)——后两个在 S-2 入口篇展开,这里先记住一句话:**Sentinel 的降级哲学是"宁可放行,不可崩溃"**。

## 7. 收尾 — 新增一个槽要遵守什么?

把前六节压缩成"加槽契约":

1. 继承 `AbstractLinkedProcessorSlot<T>`(T 通常填 DefaultNode),覆写 entry/exit
2. entry 里做完自己的检查,**必须调用 fireEntry 把事件递给下一个槽**(不递,链就断了);exit 同理
3. 拒绝时 throw BlockException(子类,如 FlowException)——链上 LogSlot 和上层 CtSph 会处理它
4. 类上标 `@Spi(order = 你想要的优先级)`,正数(内置槽全是负数)
5. 把全限定类名写进 `META-INF/services/com.alibaba.csp.sentinel.slotchain.ProcessorSlot`(多 jar 各自声明,加载时合并)
6. 单例问题要分两类看:默认 @Spi isSingleton=true,无状态检查槽(LogSlot/FlowSlot/Degrade…)都是单例复用;**但持有"资源绑定状态"的槽必须声明 isSingleton=false**——NodeSelectorSlot 和 ClusterBuilderSlot 正是如此(NodeSelectorSlot.java:127, ClusterBuilderSlot.java:49):它们持有当前资源的节点引用,而链按资源缓存,每条链需要自己的节点实例,单例共享会让不同资源的链互相覆盖节点。原型槽在每次 build() 时 new,链实例本身按资源缓存
7. 请求级数据无论如何不能写进槽字段——即使原型槽,链实例也跨请求复用,请求级状态要放在 Context/Node 里

约束 7 是最容易被新手踩的坑:槽被所有请求共享,请求级数据绝不能写进槽的字段。这也是 NodeSelector 建节点、Statistic 记统计都往 Node/Context 上放的原因——**槽自己无状态,状态都在被检查的"物"上**。

至此链的骨架讲完:容器(头节点)、遍历(递归 + transformEntry)、排序(@Spi order)、位置即设计(LogSlot)、降级(6000 静默放行)。下一个问题自然浮出来:第 2 节里的 `SpiLoader.of(ProcessorSlot.class).loadInstanceListSorted()` 到底是什么——槽是怎么被"发现"并装进链的?这是中篇的主题。
