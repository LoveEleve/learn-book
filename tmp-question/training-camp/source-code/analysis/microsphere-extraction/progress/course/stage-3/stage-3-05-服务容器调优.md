# stage-3 · 第 05 节：第三节：高并发、高性能服务容器调优 — 知识点提取

> 课程：stage-3 三高架构 第 05 节（实操组第一篇）
> 来源 docs：`/data/workspace/java-training-camp/stage-3/docs/05. 第三节：高并发、高性能服务容器调优.md`
> 提取时间：2026-08-12 | 权重：核心（Web/JVM 双面调优 + GC 算法全景——实操组开篇）
> 案例载体：my-xhs（决策 B，2026-08-12）

---

## 一、本节概览

- **技术域**：Tomcat 线程池/队列/网络参数、GC 算法选型与调优（Serial/Parallel/CMS/G1/ZGC）、Spring Boot 最小化
- **维度**：`[性能优化]` 主导（容器/JVM）
- **核心命题**：**容器调优三面**——Web 服务（Tomcat 并发参数）、JVM（GC 选型与目标）、Spring Boot（装配裁剪）；docs 的 GC 算法部分为 JDK8-15 时代全景，CMS 已移除、ZGC 已转正（JDK15 JEP 377）
- **知识点数**：9 个
- **前置**：02 篇 KP-10（GC 演进主线）、04 篇（JFR 诊断）、stage-1 第 7 节（Tomcat 容错）、第 21/22 节（Spring Web/Cloud 性能）

## 前置条件清单
读者需先掌握：
1. **GC 演进主线**（02 篇 KP-10：G1 默认/CMS 移除/ZGC）
2. **Tomcat 架构**（stage-1 07：连接器/容器/线程池）
3. **性能测试与指标**（02 篇 KP-05/06：TPS 口径）
4. **JFR 诊断**（04 篇：GC 事件）
未达前置者，先补：02/04 篇 + stage-1 07

## 掌握度
目标读者：**本人（读源码多，Spring 熟，Maven/工程化工具弱）** — 沿用已确认画像
讲解策略：
- **源码强**：GC 存在性/参数用 JDK11/17 源码验证（gcConfig/z_globals）
- **实例锚定**：Tomcat 参数对照 my-xhs application.yml（差异化线程数注释实证）
- **docs 场景 vs my-xhs**：docs 测试数据（Shopizer G1/ZGC TPS 对比）口径不全；my-xhs 用 G1+200ms 目标

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 容器调优三面总览（Web/JVM/Spring Boot）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **来源**：docs §主要内容（3 条）
- **需求**：容器调优的三条主线——进程外（Tomcat）→ 进程内（JVM）→ 框架层（Spring Boot）
- **自主实现**：若我设计——先 Web 并发参数（线程池/队列/网络），再 JVM（堆/代/GC），最后框架裁剪（装配/依赖）
- **参考实现**（docs）：**①Web 服务调优**——动态调整 Tomcat 线程池、请求队列、网络参数（并发与性能）**②JVM 调优**——Heap 大小、新老生代比率、GC 算法选择（性能与吞吐平衡）**③Spring Boot 优化**——最小化自动装配组件，降内存足迹与 CPU 计算
- **对比取舍**：**三面顺序**——容器参数见效快（TPS 立涨）、JVM 影响深（稳定/GC 停顿）、框架裁剪长期（启动/内存）；docs 测试数据显示三变量联动（KP-08）
- **测试佐证**：docs §主要内容 + §测试数据（Tomcat/GC 组合矩阵）

### KP-02 GC 选型指导原则（Serial/Parallel/CMS/G1/ZGC 场景矩阵）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[部分过时→G1 默认/ZGC 转正；CMS 已移除]` | **置信度**：High
- **前置**：02 篇 KP-10
- **来源**：docs §GC 算法（Serial/Parallel/CMS 指导原则）+ JDK11/17 gcConfig 验证
- **需求**：按应用特征选 GC——**小数据/停顿不敏感/吞吐优先/响应优先** 四类场景
- **自主实现**：若我设计——按"数据规模 × 处理器数 × 停顿敏感度"三轴选型
- **参考实现**（docs + 源码验证）：**①Serial**（`-XX:+UseSerialGC`）——小规模（~100MB）、单处理器、停顿不敏感；**②Parallel**（`-XX:+UseParallelGC`）——性能敏感但停顿要求不高（>1s 可接受）——吞吐优先；**③CMS/G1/ZGC**——**响应时间比吞吐量更重要**时选用；**源码验证（存在性）**——JDK11 `gcConfig.cpp:67-77` SupportedGCs：CMS/Epsilon/G1/Parallel/Serial/Shenandoah/ZGC 全在；**JDK17 `gcConfig.cpp` IncludedGCs：CMS 已不在**（JDK14 JEP 363 移除实证）——**CMS 从选型矩阵删除，现代矩阵 = Serial（小/单核）/Parallel（吞吐）/G1（默认均衡）/ZGC/Shenandoah（超低延迟）**
- **对比取舍**：**吞吐 vs 响应**——Parallel 吞吐最大化（停顿可 >1s）、CMS/G1/ZGC 响应优先；矩阵选型是 docs 核心指导原则（时间无关），但成员已随 JDK 换代
- **测试佐证**：docs §GC 算法指导原则 + JDK11 gcConfig.cpp:67-77 + JDK17 IncludedGC 列表（无 CMS）

### KP-03 Parallel GC 线程数与目标调优（线程公式/停顿/吞吐/足迹三目标）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：KP-02
- **来源**：docs §Parallel Garbage Collector + JDK 验证 + my-xhs 实证
- **需求**：理解"目标导向"的 GC 调优——**停顿时间/吞吐量/内存足迹三目标与优先级**
- **自主实现**：若我设计——设 MaxGCPauseMillis（停顿目标）→ GCTimeRatio（吞吐目标）→ Xmx/Xms/Xmn（足迹）；先满足停顿再权衡吞吐
- **参考实现**（docs + 实例）：**并行线程数**——NP>8 时约 5/8*NP、NP≤8 时 =NP（`-XX:ParallelGCThreads=<N>` 设置）；**注意事项**——单处理器 Parallel 不如 Serial（并行同步开销）、双处理器中型堆可能 Serial 更合适；**停顿目标**——`-XX:MaxGCPauseMillis=<N>`（默认不限，可能超出，调后吞吐可能降）；**吞吐目标**——`-XX:GCTimeRatio=<N>`（GC 时间 = 1/(N+1)，N=99 默认 → GC 占 1/100）；**足迹**——`-Xmx/-Xms/-Xmn`；**优先次序**——停顿 > 吞吐 > 足迹；**实例锚定**——my-xhs `start-all.sh:16-20` `-XX:+UseG1GC -XX:MaxGCPauseMillis=200`（**目标导向：显式停顿目标 + G1**，02/03 篇已锚）
- **对比取舍**：**三目标不可能全优**——停顿/吞吐/足迹互斥，按业务优先级调（docs 标优先次序：停顿>吞吐>足迹）
- **测试佐证**：docs §Parallel 指导原则 + my-xhs start-all.sh:16-20

### KP-04 分代空间自适应调整（扩容/缩容比率与默认值）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效→主要适用于 Parallel（自适应调节）]` | **置信度**：High
- **前置**：分代 GC 概念
- **来源**：docs §分代内存空间调整 + §Heap 默认空间大小
- **需求**：理解 GC 自适应调节（扩容/缩容比率）与默认堆规划——**先知道默认再调**
- **自主实现**：若我设计——收集器按统计信息自适应调代大小（扩容快缩容慢）
- **参考实现**（docs）：**自适应调节**——每次 GC 后更新统计（平均停顿等），代空间按**固定增量比率**扩缩容：**扩容 20%、缩容 5%**（默认）；`-XX:YoungGenerationSizeIncrement=<Y>`/`-XX:TenuredGenerationSizeIncrement=<T>` 调扩容、`-XX:AdaptiveSizeDecrementScaleFactor=<D>` 调缩容（例：扩容 10%、D=2 → 缩容 5%）；**默认堆规划**——未设 Xms/Xmx 时：**最大=物理内存 1/4、初始=1/16、新生代最大=堆 1/3**；**校验**——`-XX:+PrintFlagsFinal` 中 `MaxHeapSize`（docs 明确校验手段）
- **对比取舍**：**自适应 vs 手工固定**——Parallel 自适应（`-XX:+UseAdaptiveSizePolicy` 默认 true，**JDK11 源码实证 `gc_globals.hpp:357`**）省心但难预期；固定代比（`-XX:NewRatio/-Xmn`）可预期
- **测试佐证**：docs §分代内存空间调整 + §Heap 默认空间大小（1/4、1/16、1/3 + PrintFlagsFinal 校验）

### KP-05 CMS 机制提取（并发周期/浮动垃圾/CMF/两次停顿）【过时→G1/ZGC】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[过时→G1/ZGC（JDK14 JEP 363 移除，JDK17 源码实证无 CMS）]` | **置信度**：High
- **前置**：02 篇 KP-10
- **来源**：docs §CMS（特征/问题/浮动垃圾/停顿时机/周期起始/调度）+ JDK17 源码验证
- **需求**：**提取 CMS 的底层机制模式**（并发收集的普遍问题）——即使收集器已移除，机制教训仍在 G1/ZGC 复现
- **自主实现**：若我设计——并发收集器要处理：并发期对象变化（浮动垃圾）、老年代耗尽（CMF）、增量标记与重标记
- **参考实现**（docs + 源码验证）：**特征**——分代/并发/少停顿/不压缩/STW 停顿；**浮动垃圾（Floating Garbage）**——并发收集期间应用线程与 GC 并行，跟踪过的对象收集结束前可能变不可达 → 未被回收，留待下周期——**并发收集器的固有代价**；**并发模式失效（CMF）**——老年代填满前并发清扫未完成 → 应用停顿到收集结束——**代价昂贵，需调参避免**；**两次停顿**——initial mark（标记 roots 存活对象）+ remark（并发跟踪结束，补被错过的对象）；**周期起始**——基于历史评估 + 老年代占用超阈值启动（默认 ~92%，`-XX:CMSInitiatingOccupancyFraction=<N>` 调）；**调度**——避免新生代/老年代收集重叠长停顿（remark 调度）；**过时标注**——JDK11 仍可用（gcConfig.cpp:68 CMSGC_ONLY_ARG）但弃用，**JDK17 IncludedGC 列表已无 CMS**（JDK14 移除）→ 机制提取、工具标注 `[过时→G1/ZGC]`
- **对比取舍**：**CMS 机制教训的现代映射**——浮动垃圾/CMF 思想在 G1（并发标记）与 ZGC（并发）同样存在；"老年代阈值触发" = G1 的 IHOP（Initiating Heap Occupancy Percent）同类概念（**JDK11 源码实证：`g1_globals.hpp:48-50` G1UseAdaptiveIHOP=true + `g1Policy.cpp:742/748` Adaptive/StaticIHOPControl**）
- **测试佐证**：docs §CMS 全文 + JDK11 gcConfig.cpp:68 + JDK17 IncludedGC（无 CMS）

### KP-06 G1 GC（空节标注 + 衔接）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]`（JDK9+ 默认） | **置信度**：High
- **前置**：02 篇 KP-10
- **来源**：docs §G1（**空节：仅标题，无正文**）+ 架构师衔接
- **需求**：docs 未写 G1 内容——按空节处理（08 §2），G1 机制在 02 篇 KP-10 已提（默认 GC、分区、暂停目标）
- **自主实现**：若我设计——Region 分区 + 可预测暂停（MaxGCPauseMillis 目标驱动回收）
- **参考实现**（docs 空节 + 衔接）：**docs 空节标注**——§G1 仅标题无正文；**衔接 02 篇 KP-10**——JDK9+ 默认（JEP 248）、分区分代、目标驱动（`MaxGCPauseMillis`）、CMS 的替代；**实例**——my-xhs 全部服务 `-XX:+UseG1GC -XX:MaxGCPauseMillis=200`（start-all.sh:16-20，G1 显式选型实证）
- **对比取舍**：**G1 是默认基线**——docs 空节反而不影响掌握（G1 细节在 GCTuning 指南/源码 gc/g1/）
- **测试佐证**：docs §G1（空节）+ 02 篇 KP-10 + my-xhs start-all.sh:16-20

### KP-07 ZGC 机制与调优（彩色指针/负载屏障/内存归还/大页面/NUMA）【JDK15 转正】
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效→JDK15（JEP 377）production ready，不再需 UnlockExperimentalVMOptions]` | **置信度**：High
- **前置**：02 篇 KP-10（ZGC 提及）
- **来源**：docs §ZGC（全文）+ JDK17 z_globals 验证
- **需求**：掌握超低延迟收集器——亚毫秒暂停、彩色指针、调参面（堆/线程/内存归还/大页面/NUMA）
- **自主实现**：若我设计——并发 + Region + 彩色指针（指针存 GC 元数据）+ 负载屏障（并发标记不 STW）
- **参考实现**（docs + 源码验证）：**目标**——亚毫秒最大暂停、暂停不随堆/活动集增长、堆 8MB-16TB；**特点**——并发/Region/压缩/支持 NUMA/**彩色指针**/**负载屏障**；**激活**——`-XX:+UseZGC`（**docs 用 `-XX:+UnlockExperimentalVMOptions`，JDK15 JEP 377 转正后不再需要** `[过时→JDK15+]`）；**调优要点**——①**-Xmx 是 ZGC 最重要的调优**（堆须容纳活动集 + 净空给并发分配）②**ConcGCThreads**（给 GC 的 CPU 时间；JDK17 起动态增减，一般不需调）③**CPU 利用率永不超 70%**（docs 明确：低延迟应用不要过度配置）④**ZUncommit 默认 true**（未用内存归还 OS；`-Xms=-Xmx` 隐式禁用；`ZUncommitDelay` 默认 300s）——**JDK17 源码实证 `z_globals.hpp:51`（ZUncommit=true）、`:54`（ZUncommitDelay=5*60=300s）**、`:45`（ZCollectionInterval=0）、`:35`（ZAllocationSpikeTolerance=2.0）；⑤大页面（hugetlbfs 2MB/THP madvise 模式，延迟敏感慎用 THP）⑥NUMA（默认启用，单节点自动禁用）⑦GC 日志——**`-Xlog:gc:gc.log` / `-Xlog:gc*:gc.log`（JDK9+ unified logging）**；**演进**——JDK11 初始（实验，不支持类卸载）→ 14 平台扩展 → 15 转正（JEP 377，CDS/类指针压缩支持）→ 16 并发线程栈扫描（JEP 376）→ 17 动态 GC 线程数 → 18 字符串去重
- **对比取舍**：**ZGC vs G1**——亚毫秒暂停（10ms 内）vs 均衡默认；代价是并发簿记与净空要求（堆需更大）；**彩色指针/负载屏障 vs 写屏障**——GC 元数据进指针（load barrier）换并发安全
- **测试佐证**：docs §ZGC 全文 + JDK17 `z_globals.hpp:35/45/51/54` + gcConfig（JDK11:77 ZGC_ONLY_ARG / JDK17 IncludedGC ZGC）

### KP-08 容器调优测试数据解读（Tomcat/连接池/GC 三变量组合）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[有效]`（数据口径不全） | **置信度**：Medium
- **前置**：02 篇 KP-06（TPS 口径）
- **来源**：docs §测试数据（5 组）+ 架构师解读
- **需求**：从组合实验理解**变量联动**——单变量 vs 多变量对 TPS 的影响
- **自主实现**：若我设计——控制变量逐组实验：固定其余只变一个（GC 或线程或池）
- **参考实现**（docs 数据 + 解读）：**5 组数据**——①Tomcat 500/Hikari 8/G1 → **TPS 11.2** ②Tomcat 500/Hikari 80/G1 → **13.38**（池 8→80，+19%）③Tomcat 200/Hikari 8/ZGC → **59.0**（换 GC，+427%）④Tomcat 500/Hikari 8/ZGC → **64.2**（线程 200→500，+9%）⑤Tomcat 500/Hikari 80/ZGC → **100.0**（池+线程+GC 全拉满）——**解读**：GC 选型（G1→ZGC）是最大变量（11→64），连接池/线程数在小幅增益；**口径警示**——Shopizer 单机小数据、TPS 定义/工具未注明（02 篇 KP-11 已警示）`[待验证]`——**结论方向可参考（ZGC 低延迟场景吞吐优势），绝对值不可比较**；**my-xhs 对照**——my-xhs 用 G1（延迟目标导向）非 ZGC，`[诚实标注：无同口径对比数据]`
- **对比取舍**：**解读教训**——优化要"控制变量"，不能五组全变归因单一因素；docs 数据实际混合了三个变量（③④⑤逐步叠加）
- **测试佐证**：docs §测试数据 5 组 + 02 篇 KP-11（口径警示交叉引用）

### KP-09 Tomcat 线程池/队列/网络参数（my-xhs 差异化实证）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[有效]` | **置信度**：High
- **前置**：stage-1 07（Tomcat）
- **来源**：docs §Web 服务调优（关联标题）+ my-xhs application.yml 实证 + 架构师发散
- **需求**：掌握 Tomcat 并发参数面——**线程池/请求队列（accept-count）/连接数/Keep-Alive/压缩**，并按服务特性差异化
- **自主实现**：若我设计——按服务 IO 特征定线程数（入口多/锁竞争少）+ 队列兜底 + 连接超时管理
- **参考实现**（docs 标题 + my-xhs 实证 + 发散）：**docs 关联**——"动态调整 Tomcat 线程池、请求队列、网络参数"（关联 stage-1 第 7 节 Tomcat 容错——已提取，交叉引用）；**my-xhs 实证（user application.yml:4-16）**——`server.tomcat.threads.max=150`（**注释实证差异化策略："网关(入口,IO密集)=300, 库存(锁竞争)=100, 支付(等回调)=100, 其他=150"**，gateway 实际 300:4-7）、`min-spare=15/30`、`max-connections=8192`（默认）、`accept-count=100`（**请求队列长度**）、`connection-timeout=30000`、`keep-alive-timeout=60000`、`max-keep-alive-requests=200`（**单连接 200 次后关闭——防连接泄漏**）、`compression.enabled`（gzip：json/xml/html）+ `min-response-size=1024`、`http2.enabled=true`、`server.shutdown=graceful`（**优雅停机**）；**架构师发散**——参数联动：threads.max=并发执行上限、accept-count=排队上限（超限拒绝）、max-connections=连接层、keep-alive=连接复用；**顺序**——先按服务特性定 threads（差异化），再队列/网络兜底
- **对比取舍**：**差异化线程数 vs 统一配置**——my-xhs 按"IO 密集/锁竞争/回调等待"三特征分类（注释实证）；**连接池同理**——docs 测试数据中 Hikari 池 8→80 有增益（KP-08）
- **测试佐证**：docs §Web 服务调优 + my-xhs `my-xhs-user/src/main/resources/application.yml:4-16` + `my-xhs-gateway/src/main/resources/application.yml:4-14`（300 线程实证）
- **待验证**：gateway 为 **WebFlux/Reactive 应用**（`pom.xml:22` `spring-cloud-starter-gateway` + `CachingFilteringWebHandler implements WebHandler` 实证）——`server.tomcat.*` 配置在 reactive 栈**大概率不生效**（实际走 Netty）`[待验证：reactive 下 server.tomcat 块是否被忽略]`

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 过时 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|:---:|
| 容器调优三面总览 | 性能优化 | 核心 | P1 | 🟡 | 时间无关 | High |
| GC 选型指导原则（场景矩阵） | 性能优化 | 核心 | P1 | 🔴 | 部分过时→G1/ZGC | High |
| Parallel GC 目标调优（三目标） | 性能优化 | 核心 | P1 | 🔴 | 有效 | High |
| 分代自适应调整与默认值 | 性能优化 | 支撑 | P2 | 🟡 | 有效(Parallel) | High |
| CMS 机制提取（浮动垃圾/CMF） | 性能优化 | 核心 | P1 | 🔴 | 过时→G1/ZGC | High |
| G1（空节标注 + 衔接） | 性能优化 | 支撑 | P2 | 🟡 | 有效 | High |
| ZGC 机制与调优 | 性能优化 | 核心 | P1 | 🔴 | 有效→JDK15 转正 | High |
| 测试数据解读（三变量联动） | 性能优化 | 支撑 | P2 | 🟡 | 有效(口径不全) | Medium |
| Tomcat 线程池/队列/网络参数 | 性能优化 | 核心 | P1 | 🔴 | 有效 | High |

---

## 四、与 microsphere 的关联（参考实现验证）

- **本地源码可验证（源码优先，08）**：JDK11/17（GC）+ my-xhs（Tomcat 实例）
- **关键源码**（本次实证）：
  - JDK11 `gcConfig.cpp:67-77`（SupportedGCs：CMS/Epsilon/G1/Parallel/Serial/Shenandoah/ZGC）
  - **JDK17 `gcConfig.cpp` IncludedGCs（CMS 已移除——JDK14 JEP 363 实证）**
  - JDK17 `z_globals.hpp:35/45/51/54`（ZAllocationSpikeTolerance=2.0/ZCollectionInterval=0/ZUncommit=true/ZUncommitDelay=300s——docs 的 300s 与源码一致）
  - my-xhs `my-xhs-user/src/main/resources/application.yml:4-16`（tomcat 差异化线程配置注释实证）+ `my-xhs-gateway/src/main/resources/application.yml:4-14`（300）
  - my-xhs `start-all.sh:16-20`（G1 + MaxGCPauseMillis=200）
- **诚实标注**：docs §G1 为**空节**（仅标题）→ KP-06 衔接处理；docs ZGC 参数为 JDK15 前版本（UnlockExperimentalVMOptions）→ 过时标注；docs 测试数据口径不全（TPS 定义/工具未注明）→ 置信度 Medium；gateway 为 reactive 栈（spring-cloud-starter-gateway + WebHandler 实证）——`server.tomcat` 配置块生效性 `[待验证]`
- **关联标注**：衔接 stage-1 07（Tomcat 容错）、21/22（Spring Web/Cloud 性能——docs 的"Spring Boot 优化"主题在 stage-1 21/22 已提取，**交叉引用不重复提取**）、02 篇 KP-10（GC 演进）；18 节（JVM 故障实战）

---

## 五、本节小结（三层次视角）

**需求**：容器调优三面——Tomcat 并发参数、GC 选型与目标、Spring Boot 裁剪。

**自主实现核心**：若我设计——①Tomcat 按服务特性差异化（threads/队列/连接/keep-alive）②GC 按"停顿目标优先"选型（G1 基线/ZGC 超低延迟）③三目标（停顿>吞吐>足迹）④控制变量做组合实验。

**参考实现**：docs（GC 全景 + 测试数据）+ **JDK11/17 源码实证**（CMS 移除、ZGC 参数）+ **my-xhs 实例**（Tomcat 差异化注释、G1+200ms）。**已源码验证，非只看 docs**。

**对比取舍**：知识本体是"**目标导向的容器调优**"。核心洞察：①GC 选型矩阵时间无关但成员换代（CMS 移除/ZGC 转正）②docs 测试数据最大变量是 GC（G1→ZGC 11→64 TPS）③my-xhs 的"按服务特性差异化线程数"是现代工程实践。

**待验证汇总**：
- gateway（reactive）的 server.tomcat 配置是否生效（netty 栈）
- my-xhs Hikari 连接池显式配置位置（Nacos 或默认值）
- docs 测试数据口径（TPS 定义/压测工具）
- my-xhs 若有 ZGC 迁移需求时的实测对比（当前 G1）

---

## 六、架构师视角补全（防井底之蛙）

> **来源标注**：docs 为 GC 全景手册（JDK8-15 时代）+ 空节（G1）+ 测试数据；GC 存在性与参数已 JDK11/17 源码验证；my-xhs Tomcat 配置实证；"docs 明确内容"vs"架构师发散"如下。

### 完整认知：容器调优的完整架构该讲什么

docs 覆盖 GC 算法与测试数据。完整还该包含：

1. **三目标不可能三角**（docs 明确 + 发散）：停顿/吞吐/足迹互斥——**调优必须先定目标优先级**（docs 标优先次序：停顿>吞吐>足迹），否则参数自相矛盾
2. **GC 选型是"延迟目标"驱动的决策**（docs 明确 + 发散）：my-xhs `MaxGCPauseMillis=200`（G1 可达成）；需要 <10ms 才上 ZGC（docs 数据显示 ZGC 吞吐也高——Shopizer 场景）；**CPU<70% 是低延迟铁律**（docs ZGC 明确、02 篇排队理论呼应）
3. **容器参数是"分层"的**（架构师发散）：连接层（max-connections）→ 线程层（threads.max）→ 队列层（accept-count）→ 应用层——**瓶颈逐层下移**；队列填满是"慢"的信号（配合 03 篇监控）
4. **差异化的线程配置**（my-xhs 实证示范）：网关 IO 密集=300、库存锁竞争=100、支付等回调=100、其他=150——**按服务 IO 特征定参而非一刀切**——这是 docs"动态调整"的现代工程答案
5. **Spring Boot 最小化**（docs 第 3 条 + 交叉引用）：stage-1 21/22 已提取（排除自动装配/依赖裁剪）——本篇交叉引用即可；my-xhs 的 common 27 包是"装配面"的另一面（依赖裁剪空间 `[待验证]`）
6. **ZGC 的"内存归还 OS"哲学**（docs 明确 + 源码实证）：ZUncommit 默认开（300s 延迟，z_globals:54 实证）——容器化环境多实例共存时省内存；**-Xms=-Xmx + AlwaysPreTouch** 是极低延迟反模式提醒（docs 明确）
7. **大页面是"免费午餐但有配置成本"**（docs 明确）：hugetlbfs 需 root 配置（nr_hugepages）、THP 延迟敏感慎用——生产实施前要评估

### 关键决策与权衡

| 决策 | 权衡 |
|------|------|
| GC：Serial/Parallel/G1/ZGC | 数据规模×处理器×停顿敏感度（docs 矩阵） |
| 停顿 vs 吞吐 vs 足迹 | 不可能三角，按优先级调（docs 标序） |
| MaxGCPauseMillis 目标 | 停顿可控 vs 吞吐下降（docs 明确"调后吞吐可能减少"） |
| G1（默认）vs ZGC | 均衡 vs 亚毫秒（堆净空成本） |
| ZUncommit 归还内存 | 省内存（容器共存） vs 提交/取消延迟（极低延迟场景关闭） |
| 线程差异化 vs 统一 | 贴合服务特征 vs 管理复杂度（my-xhs 注释实证） |
| 大页面 vs THP | 稳定收益 vs 延迟峰值风险 |

### 常见坑/反模式

1. **照搬商业时代/旧版本参数**（本篇最大坑）：`UnlockExperimentalVMOptions`（JDK15+ 不需要）、CMS 参数 `-XX:+UseConcMarkSweepGC`（JDK14+ 直接报错——JDK17 源码实证已从 IncludedGCs 移除）
2. **只看 TPS 均值不控变量**：docs 测试数据五组混了三变量——生产压测必须控制变量归因
3. **CPU 打满还谈低延迟**：>70% 利用率 RT 非线性爆炸（02 篇排队理论）——ZGC 也救不了
4. **堆设太小没有净空**：ZGC 并发收集需净空，`-Xmx` 必须容纳活动集+分配净空（docs 明确）
5. **keep-alive 不设上限**：连接永不释放泄漏——my-xhs `max-keep-alive-requests=200` 示范
6. **一刀切线程数**：所有服务一个 threads.max——my-xhs 差异化注释示范反例
7. **自动装配不裁剪**：Spring Boot 最小化（docs 第 3 条）——内存足迹/启动时间/CPU 计算三方面（stage-1 21/22 展开）

### 生态位置

- **stage-3 教学主线**：05 是**实操组第一篇**——容器/服务组（05-10）开篇：本篇=进程内（GC）+ 进程外（Tomcat）基线 → 06 微服务升级 → 07/08 Eureka → 09 HTTP → 10 RPC
- **前后篇衔接**：02 篇 KP-10（GC 演进）→ 本篇（选型实操 + 测试数据）→ 18 节（JVM 故障实战，GC 日志解读）；stage-1 07（Tomcat 容错）→ 本篇（Tomcat 性能参数）；stage-1 21/22（Spring Web/Cloud 性能）→ 本篇 Spring Boot 优化交叉引用
- **与源码提取的关系**：JDK gc 源码（gcConfig/z_globals/gc/g1）为机制验证源；my-xhs application.yml 为工程实例；18 节将用 JFR/GC 日志实战

**架构师视角结论**：本篇不是背 GC 参数表，而是掌握**"目标导向的容器调优"**——先定三目标优先级（停顿>吞吐>足迹）→ 按场景矩阵选 GC（现代矩阵 G1 默认/ZGC 超低延迟）→ Tomcat 按服务特性差异化 → 控制变量做组合实验验证；docs 的 CMS/ZGC-实验参数是"版本迁移≠机制"的又一次示范（CMS 移除/UnlockExperimentalVMOptions 退役），机制本体（并发收集的浮动垃圾/CMF、目标调优三角）在 JDK11/17 源码中完整存在并已验证。
