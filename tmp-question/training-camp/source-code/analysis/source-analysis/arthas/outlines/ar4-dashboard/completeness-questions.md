# 域 AR-4: Dashboard 面板 — 全视角提问验证

> > 12 KP / 🔴4 + 🟡3 + 🟢2 | ~10 文件 | 拆 3 篇文章
> 验证方法: 逐题检查 3 篇大纲是否覆盖了该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-dashboard-engine.md` | Timer 引擎/scheduleAtFixedRate/生命周期/Ctrl-C 清理 |
| 2 | `02-dashboard-data.md` | sampler 跨 tick 复用/MemoryCommand/GcInfoVO/Tomcat HTTP 轮询 |
| 3 | `03-jvm-memory-commands.md` | jvm 9 数据块/DEADLOCK-COUNT/memory 内存池/快照 vs 面板 |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | `scheduleAtFixedRate` 和 `schedule` 的区别?面板为什么用前者? | ✅ 篇 1 §1 |
| D2 | Timer 为什么不能复用?suspend/resume 怎么实现? | ✅ 篇 1 §2 — cancel 后新建 |
| D3 | dashboard 的 %CPU 和 thread 命令的是同一算法吗?窗口期一样吗? | ✅ 篇 2 §1 — 200ms vs 5s |
| D4 | Tomcat 的 QPS/RT 具体来自哪个端口哪个路径? | ✅ 篇 2 §3 — :8006 connector/stats |
| D5 | `jvm` 的 9 块分别对应哪些 MXBean? | ✅ 篇 3 §1 |
| D6 | MemoryPoolMXBean 的 committed vs used 语义? | ✅ 篇 3 §2 |
| D7 | 面板的 QPS 具体怎么算出来的? | ✅ 篇 2 §3 — SumRateCounter 增量/窗口平均 |
| D8 | 终端窗口很矮时,面板怎么布局? | ✅ 篇 1 §3 — 三分之一+保底 12 行 |

## 维度 2: 性能工程师 (Performance)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| P1 | 面板每 5 秒全量采样的开销来源?为什么可接受? | ✅ 篇 1 §3 |
| P2 | Tomcat 探测失败会怎样?轮询超时设计? | ✅ 篇 2 §3 — 静默降级 + 1s/3s 超时 |
| P3 | 1000 线程场景下 dashboard 的采样成本? | ✅ 篇 1 §3 + 篇 2 §1 |

## 维度 3: 运维/SRE (Operations)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| O1 | Ctrl-C 退出 dashboard 为什么不会留后台刷新线程? | ✅ 篇 1 §2 |
| O2 | 面板里 QPS 是 0 或没有 Tomcat 块,代表什么? | ✅ 篇 2 §3 — 探测失败降级 |
| O3 | `jvm` 的 DEADLOCK-COUNT 什么时候看? | ✅ 篇 3 §1 — 真死锁确诊(配 AR-3 篇 3) |

## 维度 4: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| I1 | "dashboard 的实现原理?" | ✅ 篇 1 + 篇 2 |
| I2 | "dashboard 的 QPS/RT 是怎么来的?"(纠正"基于 Instrument 计数器"的误解) | ✅ 篇 2 §3 |
| I3 | "面板和 thread 命令的 CPU 有什么区别?" | ✅ 篇 2 §1 |
| I4 | "为什么面板刷新不用 ScheduledExecutorService?" | ✅ 篇 1 §1 — Timer fixedRate 语义 |

## 维度 5: 源码学习者 (跨域过渡)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| S1 | dashboard 复用了哪些 AR-3 的组件? | ✅ 篇 2 §1 — ThreadUtil/ThreadSampler/ViewRenderUtil |
| S2 | 采集逻辑与呈现如何分离?新增指标怎么加? | ✅ 篇 2 §4 + 篇 3 §3 |
| S3 | jvm 命令与 dashboard 的 addXxx 方法关系? | ✅ 篇 3 §1 — subset vs 全量 |

---



## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
| 开发者 (Developer) | 8 | 8 | ✅ |
| 性能工程师 (Performance) | 3 | 3 | ✅ |
| 运维/SRE (Operations) | 3 | 3 | ✅ |
| 面试者 (Interview) | 4 | 4 | ✅ |
| 源码学习者 (跨域过渡) | 3 | 3 | ✅ |
| **合计** | **21** | **21** | **✅ 全覆盖** |

---

## 覆盖检查

- 12 KP 全部在 3 篇大纲中落地 ✅
- 5 个身份视角 ✅(共 18 问)
- 每篇场景句 + 关键设计 + 跨域桥 ✅
- v1 规划的错误点("Instrument 计数器")在篇 2 §3 显式纠正 ✅