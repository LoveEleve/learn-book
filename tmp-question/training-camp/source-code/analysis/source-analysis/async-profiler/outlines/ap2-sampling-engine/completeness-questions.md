# 域 AP-2: 采样引擎与事件 — 全视角提问验证

> 17 KP / 🔴5 + 🟡4 + 🟢4 | ~20 文件 ~6000 行 | 拆 4 篇文章
> 验证方法: 逐题检查 4 篇大纲是否覆盖该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-sampling-core.md` | recordSample 主路径/限流/并发锁/ASGCT/JVMTI 回退/信号注册 |
| 2 | `02-cpu-engines.md` | perf_event_open+fdtransfer+RingBuffer/CpuEngine/ITimer/紧急停机 |
| 3 | `03-allocation-events.md` | AllocTracer hook/objectSampler/mallocTracer/双轨策略 |
| 4 | `04-lock-wall-events.md` | lockTracer(parkBlocker)/wallClock 睡眠感知/rateLimit/processSampler |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | recordSample 的三道防线是什么? | ✅ 篇 1 §1 — 节流/并发锁/重置 |
| D2 | 信号处理器里为什么不能 malloc?怎么绕? | ✅ 篇 1 §1 + AP-4 预告 |
| D3 | ASGCT 和 JVMTI GetStackTrace 的取舍? | ✅ 篇 1 §2 — 信号安全 vs 兜底 |
| D4 | perf 每线程一个 fd 的代价与保护? | ✅ 篇 2 §1 — 资源限制紧急停机 |
| D5 | alloc 事件为什么有两条采集轨? | ✅ 篇 3 §2 — hook vs 插桩 |

## 维度 2: 性能工程师 (Performance)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| P1 | perf_events 与 itimer 的精度/权限差异? | ✅ 篇 2 §3 — 硬件 vs 软件定时器 |
| P2 | 采样事件风暴怎么防?为什么是平滑限流? | ✅ 篇 4 §3 — 预算+结转 |
| P3 | lock 采样为什么是事件驱动而非周期采样? | ✅ 篇 4 §1 — 低频高价值 |

## 维度 3: 运维/SRE (Operations)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| O1 | 容器无 perf 权限,CPU 采样怎么办? | ✅ 篇 2 §3 — itimer 降级 |
| O2 | 线程很多时 fd 会耗尽吗?怎么处理? | ✅ 篇 2 §1 — EMFILE 紧急停机 |

## 维度 4: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| I1 | "async-profiler 怎么在信号处理器里安全采样?" | ✅ 篇 1 §2 — ASGCT + JDK-8132510 |
| I2 | "alloc 采样原理?" | ✅ 篇 3 §1 — AllocTracer 符号 hook |
| I3 | "CPU 采样的两种引擎?" | ✅ 篇 2 — perf/itimer |
| I4 | "锁等待火焰图怎么来的?" | ✅ 篇 4 §1 — parkBlocker + TLS |

## 维度 5: 源码学习者 (跨域过渡)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| S1 | fdtransfer 的消费端在哪? | ✅ 篇 2 §1 — requestPerfFd(指向 AP-1) |
| S2 | objectSampler 的插桩是谁写的? | ✅ 篇 3 §2 — instrument.cpp(指向 AP-3) |
| S3 | 栈采集后的帧怎么变成名字? | ✅ 篇 1 §2 — 指向 AP-4 |

---

## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
|------|:--:|:--:|:--:|
| 开发者 | 5 | 5 | ✅ |
| 性能工程师 | 3 | 3 | ✅ |
| 运维/SRE | 2 | 2 | ✅ |
| 面试者 | 4 | 4 | ✅ |
| 源码学习者 | 3 | 3 | ✅ |
| **合计** | **17** | **17** | **✅ 全覆盖** |

---

## 覆盖检查

- 17 KP 全部在 4 篇大纲中落地 ✅
- 5 身份视角 ✅(17 问)
- 跨域桥: AP-1(fdtransfer)/AP-3(instrument)/AP-4(栈行走)/OpenJDK 域 19(锁)✅
- 与 Arthas 对照: thread -b(锁热力)vs -e lock(等待时长)✅
