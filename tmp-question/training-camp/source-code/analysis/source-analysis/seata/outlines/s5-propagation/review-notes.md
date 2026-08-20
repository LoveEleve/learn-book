# S-5 事务传播 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "6种Propagation" — **6 值全实证** (Propagation:57-176); 无 NESTED (对照 Spring 7) — S-1 消费面与本域定义面一致 | 大纲 §1 |
| 2 | **补充锚点** | **suspend 顺序注释**: "In order to associate the following logs with XID, first get and then unbind" (L213) — 先取 xid 再解绑 (日志关联面) | 大纲 §2 |
| 3 | **表述精确化** | "挂起" 需精确: **clean=true 是事务结束解绑 (不返回 holder)** vs clean=false 是临时挂起 (返回 holder) — 两种语义区分 | 大纲 §2 |
| 4 | **补充锚点** | **bind 空值转 unbind**: RootContext.bind("") → unbind (L127 注释) — 空 xid 防御面 | 大纲 §2 |
| 5 | **补充锚点** | **ContextCore SPI 双实现**: ThreadLocal vs **FastThreadLocal (Netty 事件循环场景)** — 上下文存储可插拔 (ContextCoreLoader:27-41) | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (Propagation 57-176 / SuspendedResourcesHolder 31-36 / DefaultGlobalTransaction 207-240 / RootContext 90-176 / ContextCoreLoader 27-41 / TransactionInfo 27-84) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 决策矩阵 (harness A)
- 挂起恢复 (harness B)
- 嵌套场景 (harness C)
- clean 语义 (harness D)

### 维度2 性能
- ThreadLocal O(1)
- FastThreadLocal 优化 (Netty)

### 维度3 内存
- holder 单 xid
- 嵌套挂起栈

### 维度4 一致性
- 每层 finally resume
- MDC 同步
- 挂起恢复对称

### 维度5 负面空间 (已写入大纲 6 条)
- 不 NESTED/不传播超时继承/不子线程传播/不异步传播/不日志追踪/不多 xid 并存

## 结论
S-5 锚点 ~25 处验证, 8 闭环完成, harness 20/20 (A-D 4 面), **数字穷举 1 + 表述精确化 1 + 补充锚点 3**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1/S-4 ✅; 引出 S-9 ✅; 对照 Spring ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (枚举/挂起/嵌套/SPI) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §3 未提 **commit/rollback 的 suspend(true) 是传播解除的对称面**: 事务结束解绑 (L154-156,197-199) — 与挂起恢复构成完整生命周期 | 大纲 §3 补注 |
| 9 | 通过项 | 其余 ~22 句机制描述逐句对源码一致 ✅ (枚举语义/挂起/嵌套/SPI) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (结束解绑对称面 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 矩阵完整性 | 12 决策点全定义 (harness A9) ✅ | 通过 |
| V2 | 挂起恢复对称 | unbind/bind 配对 — 无泄漏 ✅ | 通过 |
| V3 | 栈式安全 | LIFO 恢复 (harness C3-C4) ✅ | 通过 |
| V4 | 环境清空 | NOT_SUPPORTED → MANDATORY 抛 — 语义一致 ✅ | 通过 |
| V5 | 结束解绑 | suspend(true) → 上下文归零 ✅ | 通过 |
| V6 | SPI 可替换 | ContextCoreLoader Optional — 无实现安全 ✅ | 通过 |
| V7 | MDC 一致 | bind/unbind 同步 MDC — 日志无残留 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **MDC.remove 面**: unbind 时 MDC.remove(MDC_KEY_XID) (RootContext:172-174) — 日志上下文清理闭环 (S-1 MDC 残留面的本域对策) | 大纲 §2 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (MDC.remove), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (SUPPORTS 边界/NEVER 与 MANDATORY 对称/挂起后 TIMEOUT/GlobalLockConfig/异步场景)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | SUPPORTS 边界? | 无事务直跑 (L136) — 不创建不挂起 — 最轻传播 ✅ | 通过 (验证) |
| T2 | NEVER/MANDATORY 对称? | NEVER 有抛 / MANDATORY 无抛 — **互为镜像** (L156,176) ✅ | 通过 (验证) |
| T3 | 挂起后 TIMEOUT? | suspend **不改变 createTime** (挂起不冻结计时) — 挂起期间继续计时; isTimeout 基于 createTime (TransactionalTemplate:162-165) — 长挂起可能触发超时 | 发现 11 (补锚) |
| T4 | GlobalLockConfig? | replaceGlobalLockConfig (TransactionalTemplate:175-181, S-1) — 挂起/恢复不影响锁配置 (resumeGlobalLockConfig finally) | 通过 (验证) |
| T5 | 异步场景? | ThreadLocal 不跨线程 — 需显式传递 xid 或换 ContextCore (响应式) — 负面空间已写 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 镜像对称 | NEVER/MANDATORY 条件互补 ✅ | 通过 |
| V2 | 计时语义 | 挂起不冻结 — 总时长含挂起 ✅ | 通过 |
| V3 | 锁配置隔离 | finally 恢复 — 不泄漏 ✅ | 通过 |
| V4 | 最轻路径 | SUPPORTS 无事务零开销 ✅ | 通过 |
| V5 | 对称生命周期 | bind/unbind/MDC 三同步 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **挂起不冻结超时计时** (T3): suspend 不改变 createTime — 挂起期间继续计时, 长挂起可能触发客户端超时转回滚 (isTimeout 基于 createTime, TransactionalTemplate:162-165) | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 传播枚举 (6 值/无 NESTED/TransactionInfo) — 可写 ✅
- §2 挂起恢复 (suspend/resume/clean/MDC) — 可写 ✅
- §3 嵌套矩阵 (6×2/栈式/环境清空/结束解绑) — 可写 ✅
- §4 上下文 SPI (ContextCore 双实现/RootContext) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); 新发现 **1 处** (挂起不冻结计时)。核心认知: **6 传播 = 参与/挂起/拒绝三族 + NEVER/MANDATORY 镜像对称** + **挂起不冻结计时** + **clean 区分挂起/结束** + **ContextCore SPI 可插拔**。harness 20/20 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 定义面穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (rollbackOn 实现/RollbackRule 深度匹配/角色枚举/REQUIRES_NEW 完整流/响应式 ContextCore)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | rollbackOn 实现? | **Spring RollbackRule 算法移植** (TransactionInfo:65-83): rollbackRules 列表 + 默认 NoRollbackRule.DEFAULT + 最深规则胜出 + !(winner instanceof NoRollbackRule) — S-1 消费面的底层实现 | 发现 12 (补锚) |
| T2 | RollbackRule 深度? | getDepth: **异常类继承链递归匹配** (getDepth(exceptionClass, 0) 父类递归, RollbackRule:49-53) — 深度 = 继承距离 | 发现 12 (并入) |
| T3 | 角色枚举? | GlobalTransactionRole: **Launcher/Participant 2 值** (L23-35) — S-1 消费面定义实证 | 通过 (验证) |
| T4 | REQUIRES_NEW 完整流? | suspend → **tx = GlobalTransactionContext.createNew() → break → beginTransaction** (TransactionalTemplate:74-81) — tx 替换发生在 begin 前 | 通过 (验证) |
| T5 | 响应式 ContextCore? | **spring 模块无 reactive/WebFlux 引用** — ContextCore 仅 2 内置实现 (ThreadLocal/FastThreadLocal); "响应式可替换" 为**推断面** (SPI 机制支持自定义, 无内置) | 发现 13 (表述修正) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | rollbackOn 闭环 | rollbackFor 匹配 → RollbackRule → 回滚; noRollbackFor → NoRollbackRule → 提交 ✅ | 通过 |
| V2 | 继承深度正确 | 子类异常匹配父类规则 — 深度最小者胜 ✅ | 通过 |
| V3 | 角色契约 | Launcher 驱动 / Participant join — 全流程一致 ✅ | 通过 |
| V4 | REQUIRES_NEW 替换 | createNew 后 begin — 新 tx 独立生命周期 ✅ | 通过 |
| V5 | SPI 可扩展 | 无内置响应式但机制支持自定义 — 推断面成立 ✅ | 通过 |

## 新发现问题 (2 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **rollbackOn = Spring RollbackRule 移植** (T1/T2): rollbackRules + getDepth 继承深度 + NoRollbackRule 默认 — S-1 "rollbackOn ? rollback : commit" 的底层规则引擎 | 大纲 §1 注 |
| 13 | **表述修正** | **响应式面修正** (T5): ContextCore 仅 2 内置实现, spring 无 reactive 引用 — "响应式可替换" 标注为推断面 (SPI 支持自定义) | 大纲 §4 注修正 |

## 反写测试 (只读大纲能否写文章)

- §1 传播枚举 (6 值/无 NESTED/TransactionInfo/rollbackOn 规则) — 可写 ✅
- §2 挂起恢复 (suspend/resume/clean/MDC) — 可写 ✅
- §3 嵌套矩阵 (6×2/栈式/环境清空/不冻结计时) — 可写 ✅
- §4 上下文 SPI (双实现/推断面标注) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 2 处全部修复** (RollbackRule 移植/响应式推断面修正)。核心认知: **rollbackOn 是 Spring RollbackRule 算法移植** (继承深度匹配) + **"响应式可替换"是推断面非实证** (无内置实现) + **REQUIRES_NEW = suspend→createNew→begin 替换流**。大纲经修复后反写测试全过。
