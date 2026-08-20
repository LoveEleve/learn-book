# Arthas 源码学习范围规划

> **版本**: v4.0（2026-08-19，按《源码范围规划复盘方法论》重构）
> **仓库**: `/data/workspace/arthas/`
> **定位**: 为后续正文写作准备“知识域骨架”，不是类名清单，也不是目录导览
> **核心问题**: 如何避免“扫过很多文件，却没有建立正确的知识域”

---

## 一、先给结论：旧版规划最大的问题不在“没扫到”，而在“知识域切得还不够彻底”

旧版 v3.1 已经比最初版本强很多：

- 它不再是简单的模块清单；
- 它已经把 `attach`、`watch/trace`、`thread`、`dashboard`、`OGNL`、`profiler` 拆成了 7 个域；
- 它已经意识到 `SpyAPI`、`ByteKit`、`ThreadSampler`、`Dashboard`、`ProfilerCommand` 这些桥接点的重要性。

但如果严格按《源码范围规划复盘方法论》审，它仍然有四个系统性问题：

1. **部分域仍然像“命令族聚类”，不是“读者困惑闭环”**
   - 例如旧版 `AR-2 Watch/Trace 与字节码增强` 塞进了命令体系、ByteKit、EnhancerCommand、SpyAPI 分发链、TraceTree、TimeTunnel、OGNL 条件等一大串内容，域过大、边界不够利。
2. **桥接类虽然被看到了，但还没完全转化成知识域结构**
   - 例如 `AgentBootstrap`、`ArthasClassloader`、`SpyAPI`、`AdviceListenerManager`、`ThreadSampler`、`ProfilerCommand` 都是桥接类，但旧版更多还是“核心类列表”，还不是“桥接问题卡片”。
3. **测试证据没有真正进入域定义**
   - 文档知道测试重要，但没有把“哪些测试负责证明哪个边界”嵌回到每个知识域里。
4. **淘汰/按需边界层级不够清楚**
   - 有些模块被一句“淘汰”带过，但没有分清：是永久排除、当前卷外、平台专项，还是桥接保留但不展开实现。

所以这次重构的目标，不是再加更多类名，而是：

- 把 7 个域都改成“可直接写正文”的问题卡片；
- 把过大的域拆出内部子域；
- 把桥接点、失败路径、测试证据和排除边界全部显式化。

---

## 二、规划原则：扫描可以按目录，知识域必须按问题

目录结构当然有帮助，但目录边界不等于知识边界。

对 Arthas 来说，真正应该优先回答的问题不是：

- `core/` 里有什么类；
- `monitor200/` 里有多少命令；
- `agent334/` 里有哪些入口。

而是：

- 不重启应用，Arthas 到底怎么进 JVM；
- 进来之后，它怎么既不污染业务依赖，又让所有增强代码都能找到同一个入口；
- 一次 `watch` 命令，为什么能从字符串一路走到方法现场；
- 同一个 Advice 回调，为什么最后会长成 watch / trace / stack / tt 四种不同观察模型；
- 线程、锁、GC、内存、类加载、字节码、Profiler 这些能力，生产上应该怎样组合，而不是互相替代。

所以本规划改用下面两级结构：

1. **一级知识域（AR-0 ~ AR-6）**：对齐整体教学路径；
2. **域内五要素卡片**：每个域都必须补齐：
   - 读者问题
   - 入口类
   - 状态核心
   - 失败路径
   - 与其他域的连接点

只要五要素缺一，这个域就还不算真正规划完成。

---

## 三、重构后的 7 个知识域

### 🟢 AR-0 使用与生产排查实践

**读者问题**

- 一个没用过 Arthas 的人，怎样在 10 分钟内把它挂进 JVM 并完成第一次有效排查？
- 线程、类、内存、表达式、火焰图这些命令，生产上到底该怎么组合，而不是乱打一通？

**入口类 / 入口对象**

- `bin/as.sh`
- 典型命令：`thread`、`dashboard`、`memory`、`jvm`、`sc`、`jad`、`trace`、`watch`、`tt`、`ognl`、`profiler`

**状态核心**

- 这里不追源码状态机，重点是“问题类型 → 命令路径”的决策树

**失败路径**

- 一上来全量 dump
- 一上来就 heapdump
- 没缩小目标就直接 trace/watch
- 把对象问题和热点问题混成一类

**跨域连接点**

- AR-1 解释“怎么进门”
- AR-2 解释“方法现场怎么被织出来”
- AR-3 解释“thread` / `-b` / `%CPU` 背后原理”
- AR-4 解释 Dashboard / jvm / memory 背后的数据来源
- AR-5 解释 OGNL 表达式边界
- AR-6 解释 Profiler 委托 async-profiler 的边界

**正文候选篇**

- 安装与 Attach
- thread 实战
- JVM 与内存排查
- ognl + profiler 组合排查
- 类与字节码 / 慢接口现场排查

---

### 🔴 AR-1 Agent 注入、ClassLoader 隔离与 SpyAPI 全局入口

**读者问题**

- Arthas 不重启应用，怎么进 JVM？
- 进来之后为什么不会和业务依赖打架？
- 增强后的业务代码凭什么都能找到同一个 `SpyAPI` 入口？

**入口类**

- `Arthas.java`
- `AgentBootstrap`
- `ArthasClassloader`
- `ArthasBootstrap`
- `SpyAPI`
- `ArthasAgent`（starter 自 attach）

**状态核心**

- attach / self-attach 两条入口
- `loadAgent` 字符串协议
- ArthasClassloader 隔离加载 core
- `ArthasBootstrap` 单例装配
- `SpyAPI` 提升到 Bootstrap 搜索路径
- bind / destroy 生命周期（服务端真正开门与撤走）

**失败路径**

- 把 Arthas 整个混进应用类路径
- 让两条入口各自跑一套服务端
- 绑定完成前就暴露真实 Spy
- stop 后遗留 Spy / ClassLoader / Transformer 引用

**跨域连接点**

- AR-2：后续所有增强链都依赖 `SpyAPI`
- AR-4：Dashboard 会话服务依赖 `ArthasBootstrap.bind()`
- AR-6：Profiler 命令层复用同一 attach / 类加载 / 生命周期边界

**测试证据**

- 需要系统补充 `agent`、`attach`、`classloader`、`stop/reset` 相关测试作为边界证明

**正文候选篇**

- 安装与 Attach
- AgentBootstrap / ArthasClassloader / SpyAPI
- 外部 attach vs 自 attach
- bind / destroy 生命周期

---

### 🔴 AR-2 方法级观察链：命令入口、字节码增强、Spy 分发、观察模型

> 旧版最大问题就在这里：域太大、内容太多、把多个问题压成一个“增强大章”。
> 本次重构后，AR-2 仍是一个一级域，但内部明确拆为 4 个子域，后续写作时**不能再混写**。

#### AR-2a 命令入口与执行链

**读者问题**

- 一条回车为什么不能直接等于一次方法调用？

**入口类**

- `BuiltinCommandPack`
- `ShellLineHandler`
- `JobControllerImpl`
- `ProcessImpl`
- `AnnotatedCommandImpl`

**状态核心**

- 命令元数据注册
- Shell / Job / Process 分层
- CLI 参数解析
- 原型式命令实例注入

**失败路径**

- 巨大 if-else 命令分发器
- 每个命令自己解析字符串参数
- Shell 内建命令与业务命令混表

**连接点**

- 连接 AR-2b 的 EnhancerCommand
- 连接 AR-5 的表达式参数
- 连接 AR-6 的 ProfilerCommand

#### AR-2b 增强链与 ByteKit

**读者问题**

- 方法都已经在跑了，Arthas 凭什么还能临时钻进去？

**入口类**

- `EnhancerCommand`
- `Enhancer`
- ByteKit / ASM 相关模板与拦截器

**状态核心**

- 已加载类搜索
- Transformer 注册
- `retransformClasses()`
- 内联 SpyAPI 调用
- 防重复增强
- 写回安全与 reset 缓存

**失败路径**

- 把方法观察当成“重编译”问题
- 每次命令都重复物理插入 Spy 调用
- 在错误的 ClassLoader 上下文里写回字节码

**连接点**

- 连接 AR-1 的 `SpyAPI`
- 连接 AR-2c 的回调分发
- 连接 AR-2d 的 watch / trace / tt 模型

#### AR-2c Spy 分发与 Advice 模型

**读者问题**

- 业务代码只喊一声，为什么正确 listener 能听到？

**入口类**

- `SpyAPI`
- `SpyImpl`
- `AdviceListenerManager`
- `AdviceListenerAdapter`
- `Advice`
- `ThreadLocalWatch`

**状态核心**

- 稳定门面与可替换实现
- ClassLoader 分桶索引
- 方法签名 / owner / line 的分发键
- 现场快照 vs 耗时模型分离

**失败路径**

- 业务字节码直接依赖具体 listener
- 只按类名分发
- 把 Advice 和耗时模型混成一个对象

**连接点**

- 连接 AR-2d 的 watch / trace / stack / tt 模型
- 连接 AR-5 的 Advice 根对象和 `cost` 变量

#### AR-2d 方法现场观察模型

**读者问题**

- 同一批 Advice 事件，为什么会长成 watch / trace / stack / tt 四种不同观察模型？

**入口类**

- `WatchAdviceListener`
- `TraceAdviceListener`
- `StackAdviceListener`
- `TimeTunnelAdviceListener`
- `MonitorAdviceListener`
- line 相关 listener

**状态核心**

- watch：单次调用现场
- trace：内部调用树
- stack：来源栈
- tt：历史现场与重放
- monitor / line：同一事件源下的变体分支

**失败路径**

- 用 watch 看耗时结构
- 用 trace 看参数现场
- 偶发问题一直靠实时命令等
- 把 tt 当成无限历史数据库或无副作用录像

**连接点**

- 连接 AR-5 的 OGNL 表达式
- 连接 AR-6 的 profiler 路线（对象观察 vs 采样观察）

---

### 🟡 AR-3 线程、CPU 与锁诊断链

**读者问题**

- CPU 高时，为什么先看线程，不是先看类或对象？
- 阻塞线程多时，为什么 `%CPU` 最高的线程不一定是真堵点？

**入口类**

- `ThreadCommand`
- `ThreadUtil`
- `ThreadSampler`

**状态核心**

- ThreadGroup 树递归枚举
- `ThreadVO` 快照
- CPU 双采样差值
- `thread -b` 的等待/持有两张表
- `DEADLOCK-COUNT` 的死锁确认
- 实战篇：thread 作为生产入口

**失败路径**

- 假设有现成线程总表
- 把一次采样当 `%CPU`
- 把等待多直接叫死锁
- 一上来全量深度 dump

**连接点**

- 连接 AR-4：Dashboard 线程/CPU 区域复用同一数据源
- 连接 AR-0：thread 实战排查顺序
- 连接 AR-6：profiler 往往在 thread 之后接入

**测试证据**

- 建议专门收集线程状态、CPU 排序、死锁/争用路径相关测试与 demo 作为规划证据

---

### 🟡 AR-4 Dashboard、JVM 与内存消费层

**读者问题**

- 为什么 Dashboard 不是一个 `while(true)`？
- 为什么 `jvm`、`memory` 和 Dashboard 要并存？
- 线上内存或 GC 异常时，为什么不能一上来 heapdump？

**入口类**

- `DashboardCommand`
- `DashboardTimerTask`
- `DashboardView`
- `MemoryCommand`
- `JvmCommand`

**状态核心**

- 会话级 Timer / fixedRate 刷新链
- `DashboardModel` 快照替换
- 线程/CPU、内存、GC、Runtime、Tomcat 的异构来源聚合
- `jvm` 的一次性完整盘点
- `memory` 的专项内存结构视图
- 生产路径：趋势 → 结构 → 背景 → 配置 → heapdump

**失败路径**

- 把 Dashboard 当成完整监控系统
- 把 `jvm` 当成 Dashboard 实时版
- 只看 heap 总量
- 一上来 heapdump 或开 DEBUG / 改 vmoption

**连接点**

- 连接 AR-3 的线程/CPU/锁数据
- 连接 AR-0 的 JVM/内存实战
- 连接 AR-5 的对象和配置现场排查

---

### 🟡 AR-5 OGNL：表达式引擎与对象现场

**读者问题**

- 为什么一行表达式会牵出可见性、副作用和类加载器卸载边界？
- 同一套 OGNL 引擎为什么在 watch、ognl、tt 里承担不同职责？
- 线上对象状态问题和 CPU 热点问题为什么不能混用一套工具？

**入口类**

- `ExpressFactory`
- `OgnlExpress`
- `DefaultMemberAccess`
- `ClassLoaderClassResolver`
- `CustomClassResolver`
- `OgnlCommand`

**状态核心**

- `ThreadLocal<WeakReference<Express>>`
- Advice 根对象 + `cost` 绑定
- ClassLoader 感知解析
- watch 守卫 / 投影
- ognl 独立执行器
- tt 批量筛选 / 单条取值
- ognl / profiler 实践分流

**失败路径**

- 每次回调都 new 表达式执行器
- ThreadLocal 强引用泄漏 ArthasClassLoader
- 把 `ognl` 当成无副作用只读器
- 把对象问题和热点问题混成同一类

**连接点**

- 连接 AR-2d 的 Advice / watch / tt 模型
- 连接 AR-6：对象观察 vs 采样观察的实践分流

---

### 🟡 AR-6 Profiler：命令翻译层与采样观察边界

**读者问题**

- 为什么 Arthas profiler 只是翻译器，而不是采样引擎本体？
- 为什么插桩观察和采样观察不是同一种问题，也不该由同一种工具回答？

**入口类**

- `ProfilerCommand`
- `ProfilerAction`
- `AsyncProfiler` Java API（桥接）

**状态核心**

- 命令动作分派
- `executeArgs()` 协议翻译
- `AsyncProfiler.execute()` 桥接 native 引擎
- `--timeout` / `--duration` / Markdown 的命令层增值边界
- 采样 vs 插桩的观察边界

**失败路径**

- 在 Arthas 里再造一套采样引擎
- 以为 15 个 action 就是 15 套实现
- 只用 profiler 看单次参数/返回值
- 把火焰图横轴当时间线

**连接点**

- 连接 AR-0 / AR-3 / AR-5 的实践分流
- 卷外连接 async-profiler 专卷（native 细节单独展开）

---

## 四、桥接类清单（单独标出，后续正文必须优先解释）

这些类不是因为“代码最多”而重要，而是因为它们负责把两个世界接起来：

- `AgentBootstrap`：Attach API → Arthas 内部系统
- `ArthasClassloader`：业务类世界 → Arthas core 隔离世界
- `SpyAPI`：增强字节码 → Arthas 回调体系
- `AdviceListenerManager`：ClassLoader / 方法签名 → listener 分发
- `ThreadSampler`：ThreadMXBean 累计值 → `%CPU` 窗口差值
- `DashboardCommand` / `DashboardTimerTask`：一次性采集器 → 周期性面板
- `ProfilerCommand`：Arthas CLI → async-profiler 协议
- `OgnlCommand` / `ExpressFactory`：表达式文本 → 运行时对象观察

后续任何正文，如果绕开这些桥接类去直接讲局部实现，几乎都会重新退回“源码结构文”。

---

## 五、测试证据要求（旧版最缺的地方之一）

旧版虽然知道测试重要，但没有把测试真正编进知识域卡片。后续必须补：

1. **AR-1**：attach / stop / reset / classloader 隔离相关测试
2. **AR-2**：watch / trace / tt / line / monitor 的边界测试
3. **AR-3**：线程状态、CPU 排序、锁争用和死锁测试
4. **AR-4**：Dashboard / memory / jvm 输出口径和异常路径测试
5. **AR-5**：OGNL 私有访问、副作用、ClassLoader 绑定、WeakReference 卸载测试
6. **AR-6**：ProfilerCommand 参数翻译、duration/timeout、Markdown 路径测试

测试在这里不是“补充阅读”，而是：

- 帮你证明某个知识域真的闭环；
- 帮你识别失败路径和边界条件；
- 帮你给正文里的“为什么必须这样”补证据。

---

## 六、范围之外内容，必须分层记录

旧版“淘汰清单”还是太平了。后续统一改为下面几层：

### A. 当前主线

- `core/`
- `agent/`
- `spy/`
- `arthas-agent-attach/`
- `common/`
- `arthas-model/`
- `arthas-spring-boot-starter/`（作为 AR-1 子线）

### B. 已探索但暂缓实现细节

- `async-profiler/` native 内部实现（命令层在 AR-6 保留，native 本体转 async-profiler 专卷）
- `arthas-vmtool/` native JVMTI 细节（命令层可提，实现层暂缓）
- `arthas-mcp-server/` 协议实现细节（只保留依赖和启动边界）

### C. 协议/部署专项

- `tunnel-client/` / `tunnel-common/`：作为 AR-1 / AR-4 的连接面专项
- `tunnel-server/`：部署侧专项，不列入当前主线正文

### D. 示例 / 实验 / 练手材料

- `math-game/`
- `arthas-demo-*`
- `labs/`
- `testcase/`

### E. 明确排除

- `client/` UI 侧交互细节
- `web-ui/` 前端实现细节

每一项都要附带“为什么暂缓/排除、何时重新纳入”的理由，而不是一句“淘汰”。

---

## 七、重构后的学习/写作顺序

```text
AR-0 使用与生产实践（先会用，得到场景）
  → AR-1 Agent 注入、ClassLoader 隔离与 SpyAPI（怎么进来、怎么住下）
    → AR-2 方法级观察链（命令入口 / 增强链 / 分发链 / 观察模型）
      → AR-3 线程、CPU 与锁诊断链
        → AR-4 Dashboard、JVM 与内存消费层
          → AR-5 OGNL：表达式引擎与对象现场
            → AR-6 Profiler：命令翻译层与采样观察边界
```

这里的关键不只是“从 0 到 6”，而是：

- 先有使用场景；
- 再有寄生与增强原理；
- 再有线程/面板/表达式/采样这些诊断分支；
- 最后才有卷外 native 深挖。

---

## 八、当前可直接进入正文的优先级

### 已经足够写正文的域

- AR-1
- AR-2a / 2b / 2c / 2d
- AR-3
- AR-4
- AR-5
- AR-6

### 仍需补强的地方

- 每个域对应的测试证据索引
- 某些“已探索但暂缓”模块的复核条件
- AR-0 使用域的命令输出样本和场景记录

---

## 九、最终判断

这次重构后，Arthas 规划不再是：

- “看了哪些模块”
- “列了多少核心类”
- “一共有几个域”

而是变成：

- **读者会卡在哪些问题上**
- **这些问题对应哪条机制链**
- **每条机制链的入口、状态核心、失败路径、桥接点是什么**
- **哪些内容属于当前主线，哪些只是卷外或后备内容**

如果后续正文仍然写成源码说明文，那不是因为规划不够细，而是因为写作阶段没有继续遵守这份规划骨架。
