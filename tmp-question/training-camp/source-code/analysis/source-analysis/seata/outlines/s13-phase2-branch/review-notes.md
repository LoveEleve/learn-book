# S-13 Phase2 分支通知 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "getCore(branchType) 多态" — 实证 **RM 侧对称面**: 处理器族 **5 实现** (AT/XA/TCC/Saga/**SagaAnnotation**) — 执行计划未提 SagaAnnotation | 大纲 §1 |
| 2 | **语义标注** | **AT 提交异步化**: RMHandlerAT.doBranchCommit → AsyncWorker 入队**立即返回 PhaseTwo_Committed** (S-11:81-85) — Phase2 提交非阻塞 (最终一致) | 大纲 §3 |
| 3 | **补充锚点** | **双面多态**: TC getCore (S-1) / RM getRMHandler (本域) — BranchType 全链路贯穿 | 大纲 §1/§4 |
| 4 | **补充锚点** | **UndoLogDeleteRequest 基类空实现** (issue #2226, L84-87) — RMHandlerAT 覆盖 (S-11 清理) | 大纲 §3 |
| 5 | **补充锚点** | **exceptionHandleTemplate 双侧**: AbstractRMHandler + AbstractTCInboundHandler 同模板 (S-3 交叉) | 大纲 §2 |
| 6 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (DefaultRMHandler 40-82 / AbstractRMHandler 42-120 / RmBranchCommitProcessor 42-63 / RMHandlerAT 39-128) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 处理器族 5 (harness A)
- 接收回传 (harness B)
- 双路径 (harness C)
- 全链闭环 (harness D)

### 维度2 性能
- 提交异步 (Phase2 非阻塞)
- 逐分支通知 (无批量)

### 维度3 内存
- allRMHandlersMap (5 单例)
- 协议对象

### 维度4 一致性
- 状态回传驱动决策
- 双面多态
- 异常统一模板

### 维度5 负面空间 (已写入大纲 6 条)
- 不同步两阶段/不分支级事务/不通知重试确认/不分支优先级/不批量通知/不端到端追踪

## 结论
S-13 锚点 ~25 处验证, 8 闭环完成, harness 10/10 (A-D 4 面), **数字穷举 1 + 语义标注 1 + 补充锚点 3**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 13 域全部 ✅; 对照 RocketMQ RM-12 ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (处理器族/接收端/收束/闭环) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §1 未提 **DefaultRMHandler 单例模式**: SingletonHolder (L94-100) — 分发器全局单例 | 大纲 §1 补注 |
| 9 | 通过项 | 其余 ~22 句机制描述逐句对源码一致 ✅ (处理器族/接收端/收束/闭环) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (单例 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 多态闭环 | BranchType → getCore/getRMHandler — 全链路一致 ✅ | 通过 |
| V2 | 提交异步安全 | AsyncWorker 缓冲 — 不阻塞 TC ✅ | 通过 |
| V3 | 回滚补偿完整 | undo 校验+重试 — 数据一致 ✅ | 通过 |
| V4 | 回传驱动 | BranchStatus → removeBranch/重试 — 决策正确 ✅ | 通过 |
| V5 | 异常统一 | 双侧模板 — 错误码一致 ✅ | 通过 |
| V6 | 单例安全 | SingletonHolder — 无重复注册 ✅ | 通过 |
| V7 | 清理面 | UndoLogDelete 覆盖 — 孤儿清理 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **分支删除通知面**: 终态后 TC 删除分支 — doBranchDelete (S-1:148-219) 与 RM 处理器联动面 | 大纲 §4 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (分支删除联动), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 收官穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (处理器注册时机/协议版本/响应时序/异步回调/收官完整性)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 处理器注册时机? | DefaultRMHandler 构造 initRMHandlers (L46-47) — 首次访问即注册 (SPI 懒加载) | 通过 (验证) |
| T2 | 协议版本? | BranchCommitRequest 带 version? — AbstractTransactionRequest (协议基类) | 通过 (验证) |
| T3 | 响应时序? | RM 执行后回传 — TC 同步等待 vs 异步 (S-1 异步提交面) | 通过 (验证) |
| T4 | 异步回调? | AsyncWorker 消费后无回调 — 状态靠 TC 侧查询/重试 (S-7) | 通过 (验证) |
| T5 | 收官完整性? | 13/13 域 — 全部交付 (大纲 857 行/REVIEW 187/harness 11/11) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 懒加载安全 | 首次访问注册 — 无启动耦合 ✅ | 通过 |
| V2 | 协议基类 | 继承 AbstractTransactionRequest — 统一 ✅ | 通过 |
| V3 | 异步一致 | 无回调靠状态 — 最终一致 ✅ | 通过 |
| V4 | 收束完整 | 13 域全闭 ✅ | 通过 |
| V5 | 双面一致 | TC/RM 多态对称 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **无异步回调面**: AsyncWorker 消费后无回调 — TC 靠状态查询/重试 (S-7) 保证最终一致 — 异步提交通知边界 | 大纲 §3 注 |

## 反写测试 (只读大纲能否写文章)

- §1 处理器族 (5 实现/SPI/单例) — 可写 ✅
- §2 接收端 (处理器/协议/回传) — 可写 ✅
- §3 AT 收束 (异步/补偿/异常模板) — 可写 ✅
- §4 完整闭环 (13 域链路/分支删除) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (无异步回调面)。核心认知: **双面多态收束** (getCore/getRMHandler) + **提交异步回滚补偿** + **状态回传驱动决策** + **13 域完整闭环**。harness 10/10 全过。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 资源管理器/异常模板/RMClient 穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (资源管理器分发表/异常模板细节/回滚处理器对称/RMClient 装配/TCC 处理器面)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 资源管理器分发表? | **DefaultResourceManager: resourceManagers CHM + EnhancedServiceLoader.loadAll(ResourceManager) → put(getBranchType)** (L43-73) — **branchCommit/branchRollback/lockQuery 全多态** (L82-115) — RM 侧资源管理器双面 | 发现 12 (补锚) |
| T2 | 异常模板细节? | **AbstractExceptionHandler (L95-135)**: onSuccess → ResultCode.Success; onTransactionException → **setTransactionExceptionCode + Failed + "TransactionException[...]"**; onException → Failed + "RuntimeException[...]"; **LockKeyConflict 重试提示日志** | 发现 13 (补锚) |
| T3 | 回滚处理器对称? | **RmBranchRollbackProcessor: handleBranchRollback → handler.onRequest(BranchRollbackRequest)** (L58-64) — 与 commit 同构 | 发现 14 (补锚) |
| T4 | RMClient 装配? | **RMClient.init (L31-45)**: RmNettyRemotingClient → **setResourceManager(DefaultResourceManager.get())** + **setTransactionMessageHandler(DefaultRMHandler.get())** → init — RM 两件套 | 发现 15 (补锚) |
| T5 | TCC 处理器面? | RMHandlerTCC (tcc 模块) — TCC 门面调用 (独立实现, 未深挖属 S-13 边界) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 资源双面 | getResourceManager (RM) / getCore (TC) — 全链路多态 ✅ | 通过 |
| V2 | 错误码回填 | TransactionException → 码回传 — 客户端可识别 ✅ | 通过 |
| V3 | 回滚对称 | 同构处理器 — 提交/回滚一致 ✅ | 通过 |
| V4 | 装配完整 | 两件套注入 — 客户端就绪 ✅ | 通过 |
| V5 | 冲突提示 | LockKeyConflict 日志引导配置 — 可运维 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **资源管理器双面分发表** (T1): DefaultResourceManager CHM + SPI — 分支操作全多态 | 大纲 §1 注 |
| 13 | **补充锚点** | **异常模板错误码回填** (T2): Success/TransactionExceptionCode/RuntimeException 三态 + LockKeyConflict 提示 | 大纲 §2 注 |
| 14 | **补充锚点** | **回滚处理器对称** (T3): RmBranchRollbackProcessor 与 commit 同构 | 大纲 §2 注 |
| 15 | **补充锚点** | **RMClient 装配两件套** (T4): setResourceManager + setTransactionMessageHandler (L31-45) | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 处理器族 (5 实现/SPI/单例/资源双面) — 可写 ✅
- §2 接收端 (处理器/协议/回传/异常码) — 可写 ✅
- §3 AT 收束 (异步/补偿/异常模板) — 可写 ✅
- §4 完整闭环 (13 域链路/RMClient 装配) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复** (资源双面/异常码回填/回滚对称/RMClient 装配)。核心认知: **资源管理器与处理器双面多态** (DefaultResourceManager/DefaultRMHandler) + **异常模板三态错误码** (Success/TransactionExceptionCode/RuntimeException) + **RMClient 两件套装配**。大纲经修复后反写测试全过。
