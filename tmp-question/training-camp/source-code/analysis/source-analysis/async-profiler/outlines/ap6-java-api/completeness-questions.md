# 域 AP-6: Java API 与外部集成 — 全视角提问验证

> 9 KP / 🔴3 + 🟡3 + 🟢2 | ~11 文件 ~1000 行 | 拆 2 篇文章
> 验证方法: 逐题检查 2 篇大纲是否覆盖该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-java-api.md` | 单例/五级加载策略/execute 家族/MXBean |
| 2 | `02-helper-closure.md` | Instrument/LockTracer/JfrSync/Recording/Span/全链闭环 |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | getInstance 怎么找到 .so? | ✅ 篇 1 §2 — 五级加载策略 |
| D2 | 内嵌库解压后为什么删掉? | ✅ 篇 1 §2 — 不留垃圾 |
| D3 | 插桩注入的 recordEntry 是谁实现的? | ✅ 篇 2 §1 — Instrument helper |
| D4 | 业务代码怎么往 JFR 写自定义事件? | ✅ 篇 2 §3 — Recording/Span |

## 维度 2: 架构师 (Architecture)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| A1 | 为什么 API 只有 execute 字符串没有参数对象? | ✅ 篇 1 §3 — 协议投影设计 |
| A2 | 远程管理采样器? | ✅ 篇 1 §3 — MXBean JMX |

## 维度 3: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| I1 | "Arthas execute 的字符串最后到哪?" | ✅ 篇 1 §3 — execute0 → parse |
| I2 | "为什么不用 JNIEXPORT 而用 RegisterNatives?" | ✅ 篇 2 §2 — trusted context |
| I3 | "async-profiler 整个项目怎么串起来?" | ✅ 篇 2 §4 — 全链闭环 |

## 维度 4: 源码学习者 (跨域过渡)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| S1 | 五级加载与 Arthas native 库处理的异同? | ✅ 篇 1 §2 — 临时文件同哲学 |
| S2 | JfrSync 的 Java 侧在哪? | ✅ 篇 2 §3 — helper(指向 AP-5) |
| S3 | Instrument 与 BytecodeRewriter 的关系? | ✅ 篇 2 §1 — 织入+收件一对(指向 AP-3) |

---

## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
|------|:--:|:--:|:--:|
| 开发者 | 4 | 4 | ✅ |
| 架构师 | 2 | 2 | ✅ |
| 面试者 | 3 | 3 | ✅ |
| 源码学习者 | 3 | 3 | ✅ |
| **合计** | **12** | **12** | **✅ 全覆盖** |

---

## 覆盖检查

- 9 KP 全部在 2 篇大纲中落地 ✅
- 4 身份视角 ✅(12 问——API 域较小,5 身份中的"性能/运维"视角并入架构师)
- 全链闭环: 与 AP-1~AP-5 和 Arthas AR-6 的衔接完整 ✅
- 防 #3 编造: 全部机制 grep 验证(五级加载/execute0/Instrument.recordExit/LockTracer 注释/emitSpan)✅
