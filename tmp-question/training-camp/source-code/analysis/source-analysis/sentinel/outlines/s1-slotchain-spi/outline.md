# S-1 Pass 3 文章规划与大纲:ProcessorSlot 链 + SPI + 启动日志

> 日期: 2026-08-17 | 🔴 A 方案 (Pass 3 强制 + 时空溯源 + harness)
> 依据: pass2-q1~q11 共 11 个闭环笔记 (N≥6 → 拆 3 篇)
> 深度 REVIEW: completeness-questions.md 36 问 (6❌ 12⚠️ 12✅) — 本大纲已回补全部缺口 (v2 版)

## Pass 3 文章规划:11 闭环 → 3 篇

| 篇 | 标题 | 覆盖笔记 | 核心问题 |
|:--:|------|:--:|------|
| 上 | 从"一条链"说起 — ProcessorSlot 责任链的骨架 | Q10, Q1, Q6, Q7 | 10 个槽怎么排成链、请求怎么沿链走、链路失效时为什么直接放行? |
| 中 | 不依赖 JDK 的扩展机制 — Sentinel 自有 SPI | Q2, Q3, Q4, Q9 | 为什么不用 ServiceLoader?order/别名/默认/单例如何支撑"用户自定义优先"? |
| 下 | 开机自检、日志体系与资源键 | Q5, Q8, Q11 | 谁在启动时组装一切?block 日志为什么不走 RecordLog?同名资源为何共享一条链? |

概念依赖: 上(链的运行时)引用中(链的装配来源);下(启动编排)依赖中(SPI 装配);下篇 Q11 收束链域并引向 S-3/S-7(资源名 = 规则作用域)。

---

## 文章大纲一(上):从"一条链"说起 — ProcessorSlot 责任链的骨架

### 概念依赖链
Q10(链遍历/头节点)→ Q1(链序/order)→ Q6(LogSlot 位置)→ Q7(chainMap 上限与降级)→ [新增槽契约] 收尾

### 叙事顺序
1. 问题引入:一个业务请求进入 Sentinel,它面对的是一组什么样的"检查流水线"?(读者视角: 规则那么多,怎么组织?)
2. 链的骨架:DefaultProcessorSlotChain 匿名头节点 + addLast 尾指针 — 为什么需要头节点?(统一 addFirst/addLast 两个入口;first 是"空槽"让链起点与内部一致) [QA#30]
3. 链怎么走:fireEntry → next.transformEntry → entry 的递归;为什么 entry 要泛型转换而 exit 不用?(参数签名差异);为什么递归而非循环?(深度 10 无栈风险,且每槽需要自己的 try-catch 上下文) [QA#13]
4. 链怎么排:Constants.ORDER_*_SLOT 全负值 — 为什么负数?(Constants 约定,负区间留给用户槽正数插入) [QA#27];9 内置 order 全唯一 + ParamFlowSlot(-3000) 插入点 [QA#3]
5. LogSlot 为什么卡在第三位:try-fireEntry-catch 包住全部检查槽 — "一个 catch 点覆盖全部 block 事件" [QA#4]
6. 链的降级:MAX_SLOT_CHAIN_SIZE 6000 — 为什么超限"静默放行"而非 LRU 驱逐?(有界缓存 + 静默降级比淘汰更安全:淘汰会导致已建链失效;放行是资源侧保守策略) [QA#10];为什么 COW 而非 ConcurrentHashMap?(读多写少,COW 保证读路径零锁) [QA#11, #14]
7. 收尾:新增槽契约 — 继承 AbstractLinkedProcessorSlot<T>,覆写 entry/exit,内部必须 fireEntry/fireExit 传递;@Spi(order) 注册 [QA#1, #2]

### 核心悬念
"10 个检查工位,为什么只有一个写日志的?" — 答案:LogSlot 一个 catch 点覆盖全部检查槽,位置即设计。

### 比喻锚点
流水线质检:前两道工位给产品挂上测量点(NodeSelector/ClusterBuilder 建统计节点),后面 8 个工位做各类检查,LogSlot 是握着全流水线异常记录册的工位。链 = 传送带,fireEntry = 递给下一工位。

### Aha Moment
"链的顺序不是写死在代码里的 — 每个槽用 @Spi(order) 声明自己的位置,SPI 加载器负责排序。"

---

## 文章大纲二(中):不依赖 JDK 的扩展机制 — Sentinel 自有 SPI

### 概念依赖链
[SPI 概念引入] → Q2(加载语义/单例/双缓存)→ Q3(别名/硬失败)→ Q4(ClassLoader)→ Q9(扩展点全景)

### 叙事顺序
1. 问题引入:上篇里链搭建一行代码 SpiLoader.of(...).loadInstanceListSorted() — 这是什么?为什么 Sentinel 不用 JDK 的 ServiceLoader? [QA#25]
2. SPI 是什么:服务提供者接口 — 接口 + META-INF/services 文件 + 按名加载;JDK 从 1.6 就有 ServiceLoader [QA#25]
3. 为什么自有实现:JDK ServiceLoader 缺四样 — 无顺序控制(槽顺序是硬需求!)、无别名寻址、无默认实现语义、无单例缓存;Sentinel 需要"顺序优先"的 SPI [QA#8]
4. 加载器全貌:SPI_LOADER_MAP 静态缓存 + classList/sortedClassList 双缓存(未排序/已排序,一次解析两次使用)+ loaded CAS 双检锁 [QA#15]
5. 单例与原型:@Spi isSingleton(默认 true)+ singletonMap 双检锁;原型则每次 new — 链槽都是单例复用 [QA#5]
6. 默认实现语义:loadFirstInstance(文件首个)vs loadFirstInstanceOrDefault(首个非默认,否则默认)vs loadDefaultInstance — "用户自定义优先、内置兜底"模式 [QA#2]
7. 别名与硬失败:classMap(别名 → 类);别名缺省 = 全限定类名;重复别名 = 启动硬失败;重复类 = warn 跳过(多 jar 合并) [QA#6]
8. ClassLoader 策略:默认 service.getClassLoader()(与 JDK 一致),csp.sentinel.spi.classloader=context 才切 TCCL [QA#22]
9. 扩展点全景:加槽(SPI 文件 + @Spi order)→ 换 Builder(SlotChainBuilder SPI,loadFirstInstanceOrDefault 优先用户)→ 别名加载(loadInstance("别名")) — 三种扩展点分别何时用;演进注脚:1.8.1 重构 SpiLoader 类头注释 [QA#21]

### 核心悬念
"默认实现和第一个实现,差在哪里?" — 答案:firstOrDefault 的"非默认优先"语义,是 Sentinel 全部扩展面(槽/Builder/DataSource/Logger)的通用装配规则。

### 比喻锚点
门诊档案室:第一次就诊建档(类缓存),病历按号码排队(order),默认科室套餐(isDefault),别名 = 病历号,重复建档 = 拒绝(硬失败)。

### Aha Moment
"Sentinel 的 SPI 不是'加载机制',而是'装配策略' — order/默认/单例把'谁来、按什么顺序、是单例吗'全部交给配置声明。"

---

## 文章大纲三(下):开机自检、日志体系与资源键

### 概念依赖链
Q5(InitExecutor 双层排序)→ Q8(三套日志分工 + 级别配置)→ Q11(资源键 + 监控面)→ [引向 S-3/S-7]

### 叙事顺序
1. 问题引入:链已经搭好 — 但谁保证链搭建前一切就绪?(CommandCenter/Heartbeat 等启动组件谁在调用?)
2. 启动编排:Env 静态块(类加载时)+ ClusterStateManager 双触发,CAS 单次;SpiLoader 加载 InitFunc(@Spi order)+ insertSorted(@InitOrder)双层排序;失败语义:注释 "process will exit" 与代码 printStackTrace 的偏差(以代码为准) [QA#17]
3. 三套日志分工:RecordLog(事件日志,Logger SPI 可替换,默认 JUL)/ EagleEye(自研滚动 + 批量统计引擎)/ EagleEyeLogUtil(桥接, "sentinel-block-log") — 为什么自研 EagleEye 不用 SLF4J?(高吞吐批量统计 + 滚动文件掌控力) [QA#12, #20]
4. 日志级别配置:csp.sentinel.log.level(默认 INFO) — SRE 排障入口 [QA#18]
5. 资源键:chainMap 键 = 资源名(String/Method 包装器同名等价)— 为什么只比名字?(资源名 = 规则作用域的统一键) [QA#29]
6. 监控面:CtSph.entrySize() 公开 API — 6000 上限的观测手段 [QA#19]
7. 收束:资源名是链缓存与规则集的统一键 → 引向 S-3 流控(S-3 讲 FlowRule.setResource 与链命中)与 S-7 规则管理

### 核心悬念
"block 日志为什么不走 RecordLog?" — 答案:block 是高频统计数据(每请求可能触发),与事件日志频度差几个数量级,EagleEye 的批量滚动引擎为此而生。

### 比喻锚点
剧场开演前:舞台监督(InitExecutor)按节目单(InitOrder)催场;大屏故障记录(EagleEye,高频滚动)与后台通话记录(RecordLog,事件日志)分册保管。

### Aha Moment
"Sentinel 把'频率'刻进了架构 — 高频路径(block 日志/统计)和低频路径(事件日志)从第一天就分开设计。"

---

## 补充要素(🔴 域强制)

- **时空溯源 (temporal-trace.md)**: 1.8.9 vs 1.0.x 骨架 — slotchain 包演进(早期无 SpiLoader, 链构建方式对比);SpiLoader 1.8.1 重构前后
- **极简复现 (harness/MiniSlotChain.java)**: ~200 行 — 匿名头节点链 + @Spi order 排序加载 + fireEntry 递归遍历 + 6000 上限降级
- **审查记录 (review-notes.md)**: 随正文写作同步记录

## 写作顺序

1. outline.md(本文)→ 2. temporal-trace.md → 3. 上篇正文 → 4. 中篇正文 → 5. 下篇正文 → 6. harness/MiniSlotChain.java → 7. review-notes.md(每篇完成后即时追加)

> 每篇正文按 01 Pass 3 产出模板: 问题先行、概念递进、自然叙事、源码锚点 file:line、设计权衡融入、跨域引用自然衔接。禁止 AI 模板标记(## 0. 问题 / ✅❌ / → 细节脚注)。
