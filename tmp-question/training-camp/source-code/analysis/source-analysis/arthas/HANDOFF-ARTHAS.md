# HANDOFF — Arthas 源码分析知识规划(完结交接)

> **状态**: ✅ 知识规划阶段完结(2026-08-10) → ✅ 正式写作已启动并完成 23/23 篇(2026-08-18 收敛)
> **当前阶段**: `openjdk-book/docs/openjdk/vol-arthas/` 已成文 23 篇,并完成多轮深度 REVIEW 收敛;**下一项目**: async-profiler(见 HANDOFF-ASYNC-PROFILER.md)
> 接收者: 新 AI

---

## 一、成果总览

| 交付物 | 数量 | 位置 |
|---|---|---|
| 范围规划 | 1(v3.1) | `issue/Arthas源码学习范围规划.md` |
| KP(四章+依赖图+教学顺序+文章内机制顺序+悬念设计) | 7 | `knowledge-planning/ar{0-6}-*.md` |
| 大纲(v5 标准) | 23 | `outlines/ar{0-6}-*/0*.md` |
| 正式文章 | 23 | `openjdk-book/docs/openjdk/vol-arthas/ch01.md` ~ `ch23.md` |
| completeness-questions(5 身份/162 问/审查结论表) | 7 | `outlines/ar{0-6}-*/completeness-questions.md` |
| 执行计划 | 1 | `issue/源码分析执行计划.md` 6.5 节(7 域) |
| ByteKit 源码(外部依赖) | 130 文件 | `/data/workspace/source-code/code/spring/bytekit/src/` |

**域清单**: AR-0 使用与生产实践(🟢) / AR-1 Agent 注入与 SpyAPI(🔴) / AR-2 Watch·Trace 与字节码增强(🔴,最核心 37 机制) / AR-3 Thread(🔴) / AR-4 Dashboard(🟡) / AR-5 OGNL(🟡) / AR-6 Profiler(🟡) — 共 **141 机制**。

---

## 二、方法论执行清单(十轮深审后的最终要求)

### 每篇大纲 v5 四要素 + 五维检查表

```
### N. "悬念标题" — 机制名
场景: [真实生产场景一句话]
[技术描述 + File.java:行号 + 方法名]
关键设计: [为什么——设计决策/tradeoff] + [模式: XXX] 显式标注
[Java:/JVMTI:/ASM:] 跨层标注
(篇末) 跨域桥 + **OpenJDK 关联** + **核心悬念**(悬念句 + → 下一篇桥)
```

### 每份 KP 四章

```
01 逐源提取: 源文件 → 机制 + Confidence | 02 聚合: P1(≥5文件)/P2/P3
03 深度分类: 🔴Deep/🟡Working/🟢Surface(每项含"为什么",必须全覆盖 01 表)
04 聚类: 依赖图 + 教学顺序 + 文章拆分 + 文章内机制顺序 + 关键悬念设计
```

### 十轮深审抓到的缺陷类型(写新项目必查)

> 已升级为通用档案: `issue/源码分析深审缺陷档案.md`(13 类,含 async-profiler 新增的 #3 文件名推断编造/#4 跨项目概念转移)

| # | 缺陷类型 | 检测方法 |
|:--:|---|---|
| 1 | 事实错误(类名/路径/数量) | grep 源码逐项验证 |
| 2 | 文字锚(有函数名无行号) | `rg -P '\.java(?!:\d)'` |
| 3 | 跨层不一致(KP 数与头部不同步) | 脚本比对 KP 实际数 ↔ 大纲/questions 头部 |
| 4 | API/实现路径编造 | 逐字核对源码(抓到一个编造 issue #196) |
| 5 | 分类覆盖率不足(03 < 01) | 语义映射核对(初版仅 35-92%→补到 100%) |
| 6 | 设计模式标注缺失 | `grep "\[模式:"`(Arthas 纯框架必须显式标注) |
| 7 | 数字自洽 | 关键数字(49 命令/141 机制等)全文一致性扫描 |
| 8 | 篇节标注失效 | questions 的"篇 X §Y"逐一验证(166/166) |
| 9 | 格式不统一 | 标题 `? —` 空格、章节编号连续、表格外行 |

---

## 三、已验证的事实档案(新 AI 写文章时可直接引用,不必重复验证)

### 关键行号锚点(已全部语义验证)

| 机制 | 位置 |
|---|---|
| AgentBootstrap.agentmain | agent/agent334/AgentBootstrap.java:67 |
| attach 参数 `;` 切分 | AgentBootstrap.java:110-119 |
| ArthasClassloader child-first | agent/ArthasClassloader.java:11-30 |
| ArthasBootstrap 构造 7 步 | core/server/ArthasBootstrap.java:149-196 |
| initSpy(appendToBootstrapClassLoaderSearch) | :209-232(注入点 :227) |
| enhanceClassLoader(默认关闭) | :234-262 + ClassLoader_Instrument.java:13-23 |
| bind()(端口/密码/隧道/ShellServer) | :366-518(密码 :415-426, SpyAPI.init :507) |
| destroy()(setNopSpy :944-945, resetCL :951-953) | :838-888 |
| Configure 18 字段(无默认值纪律) | core/config/Configure.java:22-90 |
| 三主体认证 | core/security/SecurityAuthenticatorImpl.java:19-80 |
| Enhancer.enhance / transform | core/advisor/Enhancer.java:639-705 / 149-369 |
| 防重复增强(GroupLocationFilter) | Enhancer.java:253-258 |
| SpyImpl 分发(splitMethodInfo) | core/advisor/SpyImpl.java:28-50 |
| AdviceListenerManager 分桶索引 | core/advisor/AdviceListenerManager.java:101 |
| Advice 位标志(AccessPoint) | core/advisor/Advice.java:12-27, :147-150 |
| ThreadLocalWatch ring 4097 | core/util/ThreadLocalWatch.java:9-94 |
| ThreadSampler CPU% 公式 | core/command/monitor200/ThreadSampler.java:121 |
| findMostBlockingLock | core/util/ThreadUtil.java:99-159(红字 :238) |
| Dashboard Timer/tick | core/command/monitor200/DashboardCommand.java:79/108/218-270 |
| Tomcat 轮询(QPS/RT) | DashboardCommand.java:159-216(8006 端口) |
| tt replay(ArthasMethod.invoke) | TimeTunnelCommand.java:502-563 / ArthasMethod.java:155-164 |
| ExpressFactory 弱引用池 | core/command/express/ExpressFactory.java:19-39 |
| strict 写保护 | ArthasObjectPropertyAccessor.java:13-17 |
| ProfilerCommand(15 动作/35+ 参数) | monitor200/ProfilerCommand.java:595-604 / 302-560 |
| native 库临时文件复制 | ProfilerCommand.java:562-575 |

### Issue 号溯源(6 真 1 假)

真实: #195(:144) #986(:273) #1512(InstrumentationUtils:27) #1596(:244) #1661(:199) #1817(AbstractTraceAdviceListener:87)。**#196 为编造,已从文档删除**。

### ByteKit(外部依赖 0.1.7)

- 源码: `/data/workspace/source-code/code/spring/bytekit/src/`(130 文件,阿里云 Maven sources jar)
- 关键类: MethodProcessor(:117-125 locationFilter)/DefaultInterceptorClassParser/ClassLoaderAwareClassWriter(:34 getCommonSuperClass)/InliningAdapter

---

## 四、OpenJDK 跨项目关联(19 强相关域,已挂)

| Arthas 机制 | OpenJDK 域(outlines/) |
|---|---|
| attach/loadAgent | 36-attach、47-instrumentation |
| SpyAPI/ClassLoader | 07-classfile-classloader |
| 字节码织入 | 44-class-verification、28-jvmti、10-metaspace |
| 线程/锁 | 17-threads、33-jmx-management、19-synchronization |
| 面板/MXBean/GC | 39-runtime-monitoring、25-gc-framework、26-g1-gc、09-memory-core |
| heapdump | 37-heap-dumper |
| 命令体系 | 35-dcmd |
| profiler | 32-jfr、18-safepoint、27-jni |
| 栈帧 | 24-frame-stack |

---

## 五、正式写作阶段状态(已完成)

1. 已按大纲完成 `openjdk-book/docs/openjdk/vol-arthas/` **23/23 篇** 正式文章: `ch01.md` ~ `ch23.md`
2. 全文沿用统一结构: 场景句 + 源码锚点 + 关键设计 + 跨层标注 + 模式标注 + 收束/后续
3. 已执行全卷审计: **23 篇 / 283 个源码引用 / 本地章节链接 / 方法论结构标签** 全量核对
4. 多轮 REVIEW 中修复的代表性问题包括: `heapdump --live` 参数纠正、Dashboard CPU 窗口口径边界、`tt -w` 与 `-i` 语义分离、`redefine`/`reset` 职责边界、`classloader -t`/`-c` 选项区分
5. 当前状态: `vol-arthas` 全卷已收敛,后续若继续扩展,应以现有 23 篇为基线做增量 REVIEW

---

## 六、移交 async-profiler(见 HANDOFF-ASYNC-PROFILER.md)

- 源码: `/data/workspace/source-code/code/spring/async-profiler/`(github.com/async-profiler/async-profiler)
- 与 Arthas 的关系: Arthas AR-6 只学了 ProfilerCommand 命令调用层;async-profiler 项目是 **native 实现本体**(76 C++ + 168 Java)
- 方法论切换: 跨层标注 [Java:/JVMTI:/ASM:] → **[C++:/Linux:/perf_events:/x86:]**(与 openjdk-book 一致)
