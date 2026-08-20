# S-6 TransactionHook — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "7个生命周期钩子(beforeBegin→afterBegin→beforeCommit/rollback→afterCompletion)" — 接口 7 方法实证 (TransactionHook:19-55); **计划漏列 afterCommit/afterRollback** (总数 7 对) | 大纲 §1 |
| 2 | **补充锚点** | **getHooks 每次新建只读包装** (TransactionHookManager:42-46): 非缓存 — 对照 TccHookManager **CACHED_UNMODIFIABLE_HOOKS (volatile 缓存)** — harness B2 自抓 | 大纲 §2 |
| 3 | **表述精确化** | "钩子异常不中断" 精确: 每钩子 **独立 try/catch** (L322-328) — 非整体包裹 — 一个钩子失败不阻止后续钩子 | 大纲 §3 |
| 4 | **补充锚点** | **Participant 不清钩子**: cleanUp **Launcher 才 clear** (L399-401) — Participant 共享外层钩子 (嵌套场景钩子不丢) | 大纲 §3 |
| 5 | **补充锚点** | **TCC 独立体系**: TccHookManager CopyOnWriteArrayList 全局 + 缓存视图 (L33-35) — 门面拦截 vs 生命周期观察 | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (TransactionHook 19-55 / TransactionHookManager 32-66 / TransactionalTemplate 223-402 / DefaultSagaTransactionalTemplate 125-160 / TccHookManager 27-62) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 7 钩子顺序 (harness A)
- 管理器语义 (harness B)
- 异常不中断 (harness C)
- Launcher 守卫 (harness D)

### 维度2 性能
- ThreadLocal O(1)
- 只读包装开销 (每次新建)

### 维度3 内存
- ThreadLocal 列表 (remove 防泄漏)
- TCC 全局列表 (常驻)

### 维度4 一致性
- 注册序触发
- Launcher 守卫
- 异常隔离

### 维度5 负面空间 (已写入大纲 6 条)
- 不过滤器链/不异步钩子/不钩子重试/不跨线程/不优先级/不事件总线

## 结论
S-6 锚点 ~25 处验证, 8 闭环完成, harness 11/11 (A-D 4 面, 自抓 1 处), **数字穷举 1 + 表述精确化 1 + 补充锚点 3**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1/S-5 ✅; 引出 S-9 ✅; 对照 Spring TransactionSynchronization ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (接口/管理/编排/复用) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §3 未提 **afterCompletion 的异常吞掉语义**: triggerAfterCompletion 也是 catch 不中断 (L381-391) — 收尾钩子失败不影响 finally 后续 (resumeGlobalLockConfig/cleanUp) | 大纲 §3 补注 |
| 9 | 通过项 | 其余 ~22 句机制描述逐句对源码一致 ✅ (接口/管理/编排/复用) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (afterCompletion 异常语义 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 顺序正确 | before 先于动作 after 后于 — 观察点对齐 ✅ | 通过 |
| V2 | 异常隔离 | 独立 try/catch — 钩子失败不影响主流程 ✅ | 通过 |
| V3 | 无泄漏 | clear remove — ThreadLocal 回收 ✅ | 通过 |
| V4 | 嵌套共享 | Participant 不清 — 外层钩子保留 ✅ | 通过 |
| V5 | 只读安全 | unmodifiableList — 触发方不改列表 ✅ | 通过 |
| V6 | 模式无关 | Saga 同模板 — 机制复用 ✅ | 通过 |
| V7 | 收尾完整 | afterCompletion finally — 成功失败都触发 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **Saga 钩子的 Launcher 守卫粒度**: DefaultSagaTransactionalTemplate 每个 trigger 方法内判 Launcher (L129-131) — 与 AT 的"外层方法守卫"不同实现同语义 | 大纲 §4 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (Saga 守卫粒度), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (Saga 触发面完整/钩子与挂起顺序/多钩子并发/afterCompletion 与 cleanUp 顺序/TCC 钩子面)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | Saga 触发面? | triggerBeforeBegin/AfterBegin/BeforeRollback 实证 (L125-160) — 是否缺 beforeCommit? Saga 模板 commit 路径不同 (branchReport 面) | 通过 (验证) |
| T2 | 钩子与挂起顺序? | TransactionalTemplate: 传播挂起 (L147-152 外层 finally) vs 钩子在 begin 周期 — **钩子触发在挂起决策之后** (挂起后新事务的 begin 钩子) | 通过 (验证) |
| T3 | 多钩子并发? | ThreadLocal 单线程串行 — 无并发 (单线程模型) | 通过 (验证) |
| T4 | afterCompletion 与 cleanUp 顺序? | finally: resumeGlobalLockConfig → **triggerAfterCompletion** → cleanUp (L141-146) — 钩子先于清除 | 通过 (验证) |
| T5 | TCC 钩子面? | TccHookManager registerHook + getHooks (L41-62) — 全局注册, prepare/commit/rollback 前后 (门面) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 钩子先于清除 | afterCompletion → cleanUp — 钩子内可访问完整上下文 ✅ | 通过 |
| V2 | 挂起后钩子 | 新事务 begin 钩子在挂起后 — 语义正确 ✅ | 通过 |
| V3 | 串行安全 | 单线程触发 — 无并发问题 ✅ | 通过 |
| V4 | Saga 语义对齐 | Launcher 守卫同语义不同实现 ✅ | 通过 |
| V5 | TCC 独立 | 全局注册常驻 — 应用级钩子 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **finally 顺序**: resumeGlobalLockConfig → afterCompletion → cleanUp (L141-146) — 钩子执行时锁配置已恢复 (S-1 交叉面) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 钩子接口 (7 方法/3×2+1/afterCompletion) — 可写 ✅
- §2 管理器 (ThreadLocal/只读包装/register/clear) — 可写 ✅
- §3 触发编排 (7 点/守卫/异常/顺序) — 可写 ✅
- §4 跨模式 (Saga 复用/TCC 独立) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (finally 顺序)。核心认知: **钩子机制 = 模式无关观察者** (AT/Saga 同模板, TCC 独立) + **getHooks 每次新建只读包装** (对照 TCC 缓存) + **finally 顺序: 锁配置恢复 → 钩子 → 清除**。harness 11/11 全过 (自抓 1 处)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 跨模板穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (Saga 全钩子面/TccHook 接口/afterCompletion 空分支/registerHook 调用者/Spring 集成钩子)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | Saga 全钩子面? | **Saga 模板完整 7 钩子**: beforeBegin(L130)/afterBegin(L142)/beforeRollback(L154)/afterRollback(L166)/beforeCommit(L178)/afterCommit(L190)/afterCompletion(L203) — 我 outline 仅列 3 钩子**表述不完整** | 发现 12 (表述修正) |
| T2 | TccHook 接口? | **before/after × prepare/commit/rollback = 6+ 方法** (TccHook:3-28) — 门面 3 周期拦截 | 发现 13 (补锚) |
| T3 | afterCompletion 空分支? | **`tx == null \|\| Launcher`** (TransactionalTemplate:382) — tx 为 null 也触发 (异常早期路径) — S-1 未提 | 发现 14 (补锚) |
| T4 | registerHook 调用者? | **spring 模块无 registerHook 调用** — 钩子无内置调用者 (业务/集成自定义注册) | 发现 15 (补锚) |
| T5 | Spring 集成钩子? | spring 模块无 TransactionHook 引用 — 钩子机制独立于 Spring 集成 (纯 tm 面) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | Saga 语义对齐 | 全 7 钩子 — 与 AT 模板一致 (模式无关实证强化) ✅ | 通过 |
| V2 | TCC 周期 | prepare/commit/rollback 三周期 — 门面语义 ✅ | 通过 |
| V3 | null 安全 | tx==null 触发 — 早期失败路径也收尾 ✅ | 通过 |
| V4 | 无内置调用者 | 纯 SPI 风格 — 集成层接入 ✅ | 通过 |
| V5 | 机制独立 | tm 模块自足 — 不依赖 Spring ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **表述修正** | **Saga 全 7 钩子** (T1): 之前大纲仅列 3 — 实证 Saga 模板完整 7 钩子 (L130-203), 与 AT 同构 — "模式无关"强化 | 大纲 §4 修正 |
| 13 | **补充锚点** | **TccHook 6+ 方法** (T2): before/after × prepare/commit/rollback — 门面 3 周期 (TccHook:3-28) | 大纲 §4 注 |
| 14 | **补充锚点** | **afterCompletion tx==null 分支** (T3): `tx == null \|\| Launcher` (L382) — null 也触发收尾 | 大纲 §3 注 |
| 15 | **补充锚点** | **无内置调用者** (T4): registerHook 无 spring 集成调用 — 纯 SPI 风格 | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 钩子接口 (7 方法/3×2+1/afterCompletion) — 可写 ✅
- §2 管理器 (ThreadLocal/只读包装/register/clear/无内置调用者) — 可写 ✅
- §3 触发编排 (7 点/守卫/异常/null 分支) — 可写 ✅
- §4 跨模式 (Saga 全 7 钩子/TCC 6+ 方法) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复** (Saga 全 7 钩子修正/TccHook 6+/afterCompletion null 分支/无内置调用者)。核心认知: **钩子机制 = 纯 tm 面 SPI** (无内置调用者) + **Saga 与 AT 全 7 钩子同构** (模式无关强化) + **TCC 门面 3 周期独立体系**。大纲经修复后反写测试全过。
