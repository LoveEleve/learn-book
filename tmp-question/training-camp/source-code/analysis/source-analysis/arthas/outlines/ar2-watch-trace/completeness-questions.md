# 域 AR-2: Watch/Trace 与字节码增强 — 全视角提问验证

> > 37 KP / 🔴8 + 🟡4 + 🟢4 | core ~35 文件 + ByteKit 130 文件 | 拆 4 篇文章
> 验证方法: 逐题检查 4 篇大纲是否覆盖了该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-command-system.md` | 注解式命令/注册 49 命令/分词/Job/查找/CLIConfigurator 注入/管道 |
| 2 | `02-bytekit-enhancer.md` | EnhancerCommand 模板/enhance/transform/ByteKit/10 拦截器/inline 防重复 |
| 3 | `03-spy-dispatch.md` | SpyAPI 转发/SpyImpl 分发/AdviceListenerManager/Adapter/Advice/ThreadLocalWatch |
| 4 | `04-watch-trace-tt.md` | watch 监听器/trace 树/stack 裁剪/tt ring+replay |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | `@Name("watch")` 注解的完整作用——名字从哪来、参数怎么被解析注入? | ✅ 篇 1 §1/§3 |
| D2 | 新增一个命令需要改哪些地方?(模板方法模式) | ✅ 篇 2 §1 |
| D3 | 同一个方法被 watch 两次会重复织入吗?怎么防? | ✅ 篇 2 §3 — GroupLocationFilter |
| D4 | `AdviceListenerManager` 为什么按 ClassLoader 分桶?弱引用 key 解决了什么? | ✅ 篇 3 §2 |
| D5 | `Advice` 的场景位标志(before/return/throw/line)怎么用? | ✅ 篇 3 §4 |
| D6 | trace 的节点为什么"合并"而不爆炸? | ✅ 篇 4 §2 — findChild |
| D7 | tt 重放为什么能还原参数类型? | ✅ 篇 4 §4 — ASM 描述符 |
| D8 | watch `-E` 正则与通配分别用什么匹配器? | ✅ 篇 1 §1 — RegexMatcher vs WildcardMatcher |
| D9 | 织入字节码时,ASM 算公共父类为什么可能 NoClassDefFoundError?怎么解决? | ✅ 篇 2 §4 — ClassLoaderAwareClassWriter.getCommonSuperClass |
| D10 | monitor 的调用统计存在哪?增强字节码参与统计吗? | ✅ 篇 4 §5 — Listener 内部 ConcurrentHashMap,增强零改动 |

## 维度 2: JVM/字节码工程师 (Bytecode)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| B1 | `retransformClasses` 对已加载类的语义——正在执行的方法会怎样? | ✅ 篇 2 §2 |
| B2 | inline=true 与反射 invoke 的字节码差异?防重复扫描依赖什么? | ✅ 篇 2 §4 |
| B3 | `MethodProcessor.process()` 的织入流程——插入点如何确定? | ✅ 篇 2 §3 |
| B4 | 为什么复用 ClassReader 常量池? | ✅ 篇 2 §3 — metaspace OOM |
| B5 | 类版本 <49 为什么要提升? | ✅ 篇 2 §3 |
| B6 | 防递归: trace 拦截器的 excludes 排除什么?为什么? | ✅ 篇 2 §4 |

## 维度 3: 性能工程师 (Performance)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| P1 | 增强后方法每次调用的额外开销量级?设计上如何保证? | ✅ 篇 3 §5 — 无反射无锁 |
| P2 | `ThreadLocalWatch` 为什么用固定 ring stack 而不是单个 long? | ✅ 篇 3 §4 — 嵌套 + 防泄漏 |
| P3 | trace 在循环调 100 次的场景如何不爆? | ✅ 篇 4 §2 — findChild 合并 |
| P4 | `-n` 限次和条件过滤在代码里如何落地(abortProcess)? | ✅ 篇 4 §1 — watching 唯一出口 |

## 维度 4: 运维/生产安全 (Operations)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| O1 | watch 高频方法为什么会把线上搞挂?源码里哪一层在保护? | ✅ 篇 4 §1 — -n/条件过滤/isLimitExceeded |
| O2 | 进程被 stop 后,为什么还在跑的增强回调不会报错? | ✅ 篇 3 §1 — NOPSPY 兜底 + skipAdviceListener |
| O3 | 匹配类太多时会发生什么?(maxNumOfMatchedClass) | ✅ 篇 2 §2 — overLimit |
| O4 | 类还没加载(watch 未来才加载的类)怎么增强? | ✅ 篇 2 §2 — 懒加载 addLazyTransformer |

## 维度 5: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| I1 | "讲一下 arthas watch 的原理" — 从命令到字节码到回调完整链路 | ✅ 篇 1→2→3→4 全链 |
| I2 | "为什么增强后调用栈还是干净的?" | ✅ 篇 2 §4 + 篇 4 §3 |
| I3 | "trace 为什么能看到方法内部子调用?" | ✅ 篇 2 §4(AtInvoke)+ 篇 4 §2(追踪桩) |
| I4 | "arthas 停止后,增强的代码怎么办?" | ✅ 篇 3 §1 — NOPSPY |
| I5 | "watch 和 trace 的实现差异是什么?" | ✅ 篇 2 §1 — instanceof InvokeTraceable |
| I6 | "重复 watch 会不会叠加增强?" | ✅ 篇 2 §3 |

---



## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
| 开发者 (Developer) | 10 | 10 | ✅ |
| JVM/字节码工程师 (Bytecode) | 6 | 6 | ✅ |
| 性能工程师 (Performance) | 4 | 4 | ✅ |
| 运维/生产安全 (Operations) | 4 | 4 | ✅ |
| 面试者 (Interview) | 6 | 6 | ✅ |
| **合计** | **30** | **30** | **✅ 全覆盖** |

---

## 覆盖检查

- 37 KP 全部在 4 篇大纲中落地 ✅
- 5 个身份视角 × ≥2 问 ✅(共 28 问)
- 全链路(命令→增强→分发→输出)在 4 篇中线性铺开,每篇结尾跨域桥衔接 ✅
- 与 AR-0(使用规范)/AR-1(SpyAPI 注入)/AR-5(OGNL)/AR-3/AR-4(ThreadUtil 复用)交叉引用完整 ✅