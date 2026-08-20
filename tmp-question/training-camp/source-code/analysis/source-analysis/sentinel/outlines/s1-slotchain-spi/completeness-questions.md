# 全视角提问验证:S-1(链骨架 + SPI + 启动日志)

> 日期: 2026-08-17 | 普通域(35 文件 ≈ 1100 行 < 10000)→ 最少 30 问 / 5 身份
> 身份集调整(中间件核心机制域): 开发者 / 架构师 / 性能工程师 / SRE / 扩展开发者 / 学生
> 覆盖对象: Pass 3 文章规划 v1(上=链骨架 Q1,Q6,Q10,Q7 / 中=SPI Q2,Q3,Q4,Q9 / 下=启动日志 Q5,Q8,Q11)

## 提问矩阵

| # | 身份 | 子主题 | 问题 | 大纲覆盖 |
|:--:|------|------|------|:--:|
| 1 | 开发者 | 链遍历 | 我加一个自定义槽,要覆写哪些方法?泛型 T 填什么? | ⚠️ Q10 有机制无"新增槽契约" |
| 2 | 开发者 | 链遍历 | 槽里怎么访问后续槽?(直接 next 还是 fireEntry?) | ⚠️ 仅提 transformEntry,未讲"必须 fireEntry"约束 |
| 3 | 开发者 | 链序 | 我改 order 会破坏谁?(NodeSelector 必须最先的硬约束) | ✅ 上篇 Q1 |
| 4 | 开发者 | BlockException | 我的槽 throw BlockException,谁处理它? | ✅ 上篇 Q6(LogSlot catch) |
| 5 | 开发者 | 单例 | 我的槽/实现要单例还是每次 new? | ❌ 中篇未提 isSingleton/双检锁 |
| 6 | 开发者 | 别名 | loadInstance("别名") 找不到会怎样? | ✅ 中篇 Q3(fail 硬错) |
| 7 | 开发者 | Builder | 自定义 SlotChainBuilder 的非 AbstractLinkedProcessorSlot 槽会怎样? | ✅ 中篇 Q9(demo removeIf + warn) |
| 8 | 架构师 | SPI | 为什么不用 JDK ServiceLoader? | ✅ 中篇核心(修正: 非"每查必加载",是 order/别名/默认/单例) |
| 9 | 架构师 | 链式 | 这是 GOF 责任链吗?差异在哪? | ⚠️ 无标准责任链对比 |
| 10 | 架构师 | 6000 | 超限为什么"静默放行"而不是驱逐 LRU? | ⚠️ Q7 有行为无 tradeoff |
| 11 | 架构师 | COW | 为什么 copy-on-write 而不是 ConcurrentHashMap? | ⚠️ Q7 有实现无权衡 |
| 12 | 架构师 | 日志 | 为什么自研 EagleEye 而非 SLF4J? | ⚠️ Q8 有分工无取舍 |
| 13 | 性能 | 遍历开销 | 递归 10 层 vs 循环,为什么选递归? | ⚠️ 可补一句(浅栈无风险) |
| 14 | 性能 | COW 写 | 第 6001 个资源创建时 COW 全量复制的代价? | ⚠️ 与 #11 合并补 |
| 15 | 性能 | 单例缓存 | SpiLoader 的类缓存避免重复解析? | ⚠️ 中篇可显式化(classList/sortedClassList 双缓存) |
| 16 | 性能 | block 日志 | 高频 block 日志为什么需要批量/滚动引擎? | ✅ 下篇 Q8(eagleeye) |
| 17 | SRE | 初始化 | 初始化失败日志在哪?怎么排查? | ✅ 下篇 Q5(printStackTrace 偏差) |
| 18 | SRE | 日志级别 | 日志级别怎么调?配置键? | ❌ 未提 csp.sentinel.log.level(默认 INFO) |
| 19 | SRE | 6000 监控 | 链数怎么监控? | ❌ 未提 CtSph.entrySize() 公开 API |
| 20 | SRE | block 定位 | 线上被 block,查哪个文件? | ✅ 下篇 Q8(sentinel-block-log) |
| 21 | 扩展开发者 | 扩展全景 | 加槽 vs 换 Builder vs 别名加载,三种扩展点何时用哪个? | ⚠️ 分散 Q2/Q9,无全景总结 |
| 22 | 扩展开发者 | ClassLoader | 多 jar 适配器场景,SPI 文件怎么合并?TCCL 何时开? | ✅ 中篇 Q4 |
| 23 | 扩展开发者 | InitFunc | 我要加启动钩子,SPI 文件放哪?顺序怎么排? | ✅ 下篇 Q5(双层排序) |
| 24 | 扩展开发者 | Logger | 自定义日志实现怎么接入? | ⚠️ Q8 有 SPI 无"怎么写自定义 Logger" |
| 25 | 学生 | SPI 概念 | 什么是 SPI?为什么叫"服务提供者接口"? | ❌ 中篇直接对比,无从零引入 |
| 26 | 学生 | 命名 | slot/chain 为什么叫这个名字? | ⚠️ 可加名词来源 |
| 27 | 学生 | 负数 order | 为什么 order 是负数? | ⚠️ 可加一句(Constants 约定) |
| 28 | 学生 | 首槽 | 为什么 NodeSelectorSlot 必须第一? | ✅ 上篇 Q1 |
| 29 | 学生 | 链键 | 同名资源的两个包装器为什么共享一条链? | ✅ 下篇 Q11 |
| 30 | 学生 | 头节点 | DefaultProcessorSlotChain 为什么要匿名头节点? | ⚠️ Q10 有实现无"为什么"(统一 addFirst/addLast) |

## 覆盖统计

- ✅ 12 项 / ⚠️ 12 项 / ❌ 6 项(36 问, 6 身份)
- ❌ 缺口(必须回补): #5 单例机制 / #18 日志级别键 / #19 entrySize 监控 / #25 SPI 概念引入
- ⚠️ 部分覆盖(优先补): #1 新增槽契约 / #2 fireEntry 约束 / #9 GOF 对比 / #10 静默放行 tradeoff / #11 COW tradeoff / #12 EagleEye tradeoff / #15 双缓存 / #21 扩展点全景 / #24 自定义 Logger / #26 名词 / #27 负数 order / #30 头节点动机

## 回补计划

1. 中篇开头: "SPI 是什么"从零引入(服务提供者接口,JDK 早有 ServiceLoader,区别在能力)
2. 中篇: 单例/原型双模式(@Spi isSingleton + singletonMap 双检锁)+ classList/sortedClassList 双缓存 → 并到 Q2 叙事
3. 上篇: "新增槽契约"收尾段(继承 AbstractLinkedProcessorSlot<T>,覆写 entry/exit,必须 fireEntry/exit,加 @Spi)——QA #1/#2
4. 上篇 Q7: tradeoff 两则 — 静默放行 vs 驱逐;COW vs CHM(读多写少)
5. 下篇: 日志级别 csp.sentinel.log.level(默认 INFO)+ entrySize() 监控面 → 并入 Q8/Q7 叙事
6. 全局: GOF 责任链对比一句;EagleEye vs SLF4J 取舍一句;名词来源一句;负数 order 一句;头节点动机并入 Q10
