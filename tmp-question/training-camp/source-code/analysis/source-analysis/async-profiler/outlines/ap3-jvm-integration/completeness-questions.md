# 域 AP-3: JVM 集成 — 全视角提问验证

> 13 KP / 🔴5 + 🟡4 + 🟢3 | ~8 文件 ~3800 行 | 拆 4 篇文章
> 验证方法: 逐题检查 4 篇大纲是否覆盖该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-agent-jvmti.md` | 双入口/16 回调注册表/能力申请 |
| 2 | `02-bytecode-rewriter.md` | BytecodeRewriter/重定位表/三表修正/latency |
| 3 | `03-vmstructs-stackwalk.md` | vmStructs 偏移/线程桥/三模式栈行走/codeCache |
| 4 | `04-java-api-bridge.md` | execute0/execute1/getSamples/RegisterNatives |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | async-profiler 怎么进 JVM?两种加载路径? | ✅ 篇 1 §1 — Agent_OnLoad/OnAttach |
| D2 | 字节码插入后行号表/栈映射表怎么办? | ✅ 篇 2 §1/§2 — 三表修正+重定位表 |
| D3 | 为什么不用 ASM 库? | ✅ 篇 2 §1 — C++ 零依赖 vs ByteKit 框架 |
| D4 | execute("...") 的 native 落地? | ✅ 篇 4 §1 — execute0 → parse |

## 维度 2: JVM/字节码工程师 (Bytecode)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| B1 | 重定位表的两遍法具体怎么做? | ✅ 篇 2 §2 — 首遍扫描建表,二遍按表改 |
| B2 | rewriteVerificationTypeInfo 修什么? | ✅ 篇 2 §1 — 栈状态变化修正 |
| B3 | vmStructs 怎么做到跨 JDK 版本? | ✅ 篇 3 §1 — 运行时读偏移,不写死 |
| B4 | walkFP/walkDwarf/walkVM 各什么时候用? | ✅ 篇 3 §2 — FP 优先/DWARF 兜底/VM 展开内联 |

## 维度 3: 性能工程师 (Performance)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| P1 | 插桩采样率怎么控制?高频方法怎么办? | ✅ 篇 2 §3 — _calls/shouldRecordSample 节流 |
| P2 | 三种栈行走的性能差异? | ✅ 篇 3 §2 — FP 最快/DWARF 慢/VM 专属 |

## 维度 4: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| I1 | "async-profiler 的 alloc 插桩原理?" | ✅ 篇 2 — BytecodeRewriter |
| I2 | "为什么能跨 JDK 版本工作?" | ✅ 篇 3 §1 — VMStructs 偏移解析 |
| I3 | "火焰图的 [inlined] 帧哪来的?" | ✅ 篇 3 §2 — walkVM scope 展开 |
| I4 | "和 Arthas 的字节码增强什么区别?" | ✅ 篇 2 §1 — 手写 vs ByteKit 框架 |

## 维度 5: 源码学习者 (跨域过渡)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| S1 | 16 个回调里各引擎的挂接点? | ✅ 篇 1 §2 — 回调→引擎映射表(指向 AP-2) |
| S2 | 地址→JIT 方法的映射在哪? | ✅ 篇 3 §3 — CodeCache(指向 AP-4 符号表) |
| S3 | Arthas 的 execute 字符串最后到哪? | ✅ 篇 4 §2 — execute0 → parse(指向 AP-1) |

---

## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
|------|:--:|:--:|:--:|
| 开发者 | 4 | 4 | ✅ |
| JVM/字节码工程师 | 4 | 4 | ✅ |
| 性能工程师 | 2 | 2 | ✅ |
| 面试者 | 4 | 4 | ✅ |
| 源码学习者 | 3 | 3 | ✅ |
| **合计** | **17** | **17** | **✅ 全覆盖** |

---

## 覆盖检查

- 13 KP 全部在 4 篇大纲中落地 ✅
- 5 身份视角 ✅(17 问)
- 与 Arthas 对照: premain/agentmain(AR-1)、ByteKit(AR-2)、反射桥哲学 ✅
- 与 OpenJDK: 域 44(栈映射表)、域 16(Code Cache)、域 28 JVMTI ✅
- 防 #3 编造: 全部机制经 grep 验证(BytecodeRewriter 方法清单/回调表/偏移字段)✅
