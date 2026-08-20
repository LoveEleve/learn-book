# 域 AR-5: OGNL 表达式 — 全视角提问验证

> > 10 KP / 🔴5 + 🟡3 + 🟢2 | ~10 文件 | 拆 2 篇文章
> 验证方法: 逐题检查 2 篇大纲是否覆盖了该视角的关注点

**大纲文件对照**:

| 篇 | 文件 | 覆盖范围 |
|:--:|------|------|
| 1 | `01-express-engine.md` | 弱引用池/OgnlExpress/DefaultMemberAccess/ClassLoaderClassResolver |
| 2 | `02-express-usage.md` | isConditionMet/getExpressionResult/Advice 环境/ognl 命令/tt 搜索/全链路 |

---

## 维度 1: 开发者 (Developer)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| D1 | 为什么 ThreadLocal 里不能直接存表达式对象?WeakReference 怎么打断链路? | ✅ 篇 1 §1 — 源码注释原文 |
| D2 | `is()` 和 `get()` 的语义差异?空条件为什么零开销? | ✅ 篇 2 §1 |
| D3 | 表达式变量(params/cost 等)从哪里来?怎么加新变量? | ✅ 篇 2 §2 — Advice 字段 |
| D4 | `@类@静态成员` 的类名怎么解析?多版本怎么办? | ✅ 篇 1 §4 — ClassResolver |
| D5 | threadLocalExpress 和 unpooledExpress 的选择依据? | ✅ 篇 2 §3 |
| D6 | 表达式 `String` 这个类名怎么解析到 java.lang.String? | ✅ 篇 1 §4 — CustomClassResolver java.lang 兜底 |
| D7 | ognl 能修改线上对象的属性吗?代码层面怎么拦? | ✅ 篇 1 §3 — strict 模式 setPossibleProperty 抛错 |

## 维度 2: 安全/架构师 (Security & Architecture)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| A1 | DefaultMemberAccess(true) 全放开——安全边界在哪一层? | ✅ 篇 1 §3 |
| A2 | 表达式的方法调用会真实执行——风险与纪律? | ✅ 篇 1 §3 + AR-0 篇 6 |
| A3 | 泄漏防御的完整链条(业务线程→ThreadLocal→类加载器)? | ✅ 篇 1 §1 |

## 维度 3: 性能工程师 (Performance)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| P1 | 高频 watch 场景,表达式求值的开销在哪?如何最小化? | ✅ 篇 2 §1 — 空短路 |
| P2 | 池复用 vs 每次新建的开销差异? | ✅ 篇 2 §3 |

## 维度 4: 面试者 (Interview)

| # | 问题 | 大纲覆盖? |
|:--:|------:&--:|
| I1 | "watch 的条件表达式在哪执行?" | ✅ 篇 2 §1 |
| I2 | "arthas 为什么能读 private 字段?" | ✅ 篇 1 §3 |
| I3 | "为什么说 arthas 停掉不会有内存泄漏?" | ✅ 篇 1 §1 |
| I4 | "OGNL 和 SpEL 的区别?" | ⚠️ 补充: 篇 1 §2 — OGNL 上下文机制(可展开对比) |

## 维度 5: 源码学习者 (跨域过渡)

| # | 问题 | 大纲覆盖? |
|:--:|------|:--:|
| S1 | 表达式执行点与分发链的关系? | ✅ 篇 2 §4 — 最后一层闸门 |
| S2 | tt 的搜索与 watch 的条件用同一套引擎吗? | ✅ 篇 2 §3 |
| S3 | cost 变量的绑定机制? | ✅ 篇 2 §1 — bind(String, Object) |

---



## 审查结论

| 维度 | 问题数 | 全覆盖 | 状态 |
| 开发者 (Developer) | 7 | 7 | ✅ |
| 安全/架构师 (Security & Architecture) | 3 | 3 | ✅ |
| 性能工程师 (Performance) | 2 | 2 | ✅ |
| 面试者 (Interview) | 4 | 4 | ✅ |
| 源码学习者 (跨域过渡) | 3 | 3 | ✅ |
| **合计** | **19** | **19** | **✅ 全覆盖** |

---

## 覆盖检查

- 10 KP 全部在 2 篇大纲中落地 ✅
- 5 个身份视角 ✅(共 17 问)
- 每篇场景句 + 关键设计 + 跨域桥 ✅
- 注意: I4(OGNL vs SpEL)标注为补充点——撰写文章时可展开 OGNL 上下文/类解析机制对比