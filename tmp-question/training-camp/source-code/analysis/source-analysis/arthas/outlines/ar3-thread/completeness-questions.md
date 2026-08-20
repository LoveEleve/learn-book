# 域 AR-3: Thread 线程诊断 — 全视角提问验证

> 19 KP / 🔴4 + 🟡4 + 🟢2 | ~10 文件 | 拆 3 篇文章
> 验证方法: 逐题检查 3 篇大纲是否覆盖了该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-thread-enumeration.md` | ThreadGroup 枚举/ThreadVO 快照/状态统计/表格 |
| 2 | `02-cpu-sampling.md` | 两次采样差值/getInternalThreadCpuTimes/TopN/深度 getThreadInfo |
| 3 | `03-blocking-deadlock.md` | findMostBlockingLock/dumpAllThreads 深度/红字渲染/-b vs jvm 分工 |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | 没有全局线程表 API,Arthas 怎么枚举"所有线程"? | ✅ 篇 1 §1 — ThreadGroup 爬树 + enumerate 翻倍重试 |
| D2 | `ThreadVO` 为什么是快照拷贝而不是 Thread 引用? | ✅ 篇 1 §2 |
| D3 | `getThreadCpuTime` 返回什么?瞬时 CPU% 为什么必须两次读? | ✅ 篇 2 §1 |
| D4 | `getThreadInfo(id, lockedMonitors, lockedSynchronizers)` 比普通版多什么? | ✅ 篇 2 §3 |
| D5 | 锁的"身份"用什么标识?为什么不是 equals? | ✅ 篇 3 §1 — identityHashCode |
| D6 | `lockedMonitors`/`lockedSynchronizers` 参数控制什么? | ✅ 篇 1 §1 — getThreadInfo 锁深度 |
| D7 | 内部线程采样失败会怎样?为什么不会崩? | ✅ 篇 2 §2 — hotspotThreadMBeanEnable 降级 |
| D8 | thread -b 没有阻塞锁时输出什么? | ✅ 篇 3 §1 — "No most blocking thread found!" |

## 维度 2: 性能工程师 (Performance)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| P1 | 采样间隔 200ms 是怎么平衡的?更短/更长会怎样? | ✅ 篇 2 §1 — 噪声 vs 平滑 |
| P2 | `thread -n 3` 为什么不先深度 getThreadInfo 再排序? | ✅ 篇 2 §3 — 先采样截断再深度读 |
| P3 | 业务线程都不忙但 CPU 高——表格里能看到谁? | ✅ 篇 2 §2 — 内部线程 id=-1 |
| P4 | `dumpAllThreads(true, true)` 的开销量级?为什么只在 -b 用? | ✅ 篇 3 §1 |

## 维度 3: 运维/SRE (Operations)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| O1 | `thread -b` 红字的含义——它告诉运维什么? | ✅ 篇 3 §2 |
| O2 | "假死锁"(池子耗尽)与真死锁怎么区分?用哪个命令? | ✅ 篇 3 §3 |
| O3 | 状态统计行的作用?(Threads Total 与各状态数) | ✅ 篇 1 §3 |

## 维度 4: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| I1 | "thread 命令的 %CPU 是怎么算的?" | ✅ 篇 2 §1 |
| I2 | "thread -b 和 jstack 找死锁的区别?" | ✅ 篇 3 §1/§3 |
| I3 | "thread -n 3 为什么是两次采样?" | ✅ 篇 2 §1/§3 |
| I4 | "getThreadInfo 深度参数的作用?" | ✅ 篇 2 §3 |

## 维度 5: 源码学习者 (跨域过渡)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| S1 | dashboard 的 %CPU 和 thread 的是同一套吗? | ✅ 篇 2 §1 — 复用同一 ThreadSampler(指向 AR-4) |
| S2 | stack 命令的栈裁剪与 thread 的栈渲染共享什么? | ✅ 篇 1 跨域桥 — ThreadUtil |
| S3 | `findDeadlockedThreads` 在哪个命令里用? | ✅ 篇 3 §3 — JvmCommand(指向 AR-4) |

---



## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
| 开发者 (Developer) | 8 | 8 | ✅ |
| 性能工程师 (Performance) | 4 | 4 | ✅ |
| 运维/SRE (Operations) | 3 | 3 | ✅ |
| 面试者 (Interview) | 4 | 4 | ✅ |
| 源码学习者 (跨域过渡) | 3 | 3 | ✅ |
| **合计** | **22** | **22** | **✅ 全覆盖** |

---

## 覆盖检查

- 19 KP 全部在 3 篇大纲中落地 ✅
- 5 个身份视角 ✅(共 17 问)
- 每篇有场景句 + 关键设计(为什么)+ 跨域桥 ✅
- 与 AR-0 篇 2(使用体验)双向印证: 现象(等 1-2 秒/红字)在源码层得到解释 ✅