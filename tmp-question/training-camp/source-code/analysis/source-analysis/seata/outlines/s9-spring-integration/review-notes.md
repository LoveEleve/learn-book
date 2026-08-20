# S-9 Spring 集成 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **认知修正** | 执行计划 "Spring 集成 — @GlobalTransactional→GlobalTransactionScanner→TransactionalTemplate" 正确但需补: **拦截器在 2.x 已移入 integration-tx-api 模块** (GlobalTransactionalInterceptorHandler), Scanner 只做织入 (L291-297 注释锚) | 大纲 §3 |
| 2 | **语义标注** | **超时重置语义**: aspectTransactional.timeout <= 0 **\|\| == 60000** → 用 defaultGlobalTransactionTimeout (L208-211) — **注解未配置时跟随全局动态默认** (非固定 60000) | 大纲 §1 |
| 3 | **补充锚点** | **rollbackRules 注解源闭环**: rollbackFor/noRollbackFor (Class+String 双形式) → RollbackRule/NoRollbackRule (L216-241) — **S-5 rollbackOn 规则引擎的注解源实证** | 大纲 §1 |
| 4 | **补充锚点** | **ExecutionException 状态化**: Participant → 抛原异常; RollbackDone+isTimeoutException → 抛 cause; Begin/Commit/RollbackFailure → failureHandler 钩子 (L245-260+) | 大纲 §3 |
| 5 | **补充锚点** | **ScannerChecker 族 3 个**: ConfigBeans/Package/Scope — 可插拔扫描检查 | 大纲 §2 |
| 6 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (GlobalTransactionScanner 87-560 / InterceptorHandler 78-260 / GlobalTransactional 注解 / SeataAutoConfiguration 47-59 / DefaultValues 285) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 注解参数桥
- 扫描织入
- 双 handler 分发
- 异常状态化

### 维度2 性能
- NEED_ENHANCE 预收集
- PROXYED 去重
- BeanDefinition 扫描 (一次性)

### 维度3 内存
- 双集合 (PROXYED/NEED_ENHANCE)
- ScannerChecker 注册表

### 维度4 一致性
- 超时重置语义
- 已代理按序织入
- disable 动态开关

### 维度5 负面空间 (已写入大纲 6 条)
- 不注解织入拦截器类/不方法级重试/不表达式 pointcut/不事务名自动生成/不 @Transactional 冲突处理/不多数据源路由

## 结论
S-9 锚点 ~25 处验证, 8 闭环完成, **认知修正 1 + 语义标注 1 + 补充锚点 3**。🟡 B 无 harness。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 S-1/S-4/S-5 ✅; 对照 Spring @Transactional ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (注解/扫描/拦截/配置) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §2 未提 **AspectJ 注解织入面**: Scanner 注释 (L300-305) LocalTCC/TwoPhaseBusinessAction/RemotingParser — TCC 模式远程服务解析 (S-13 交叉) | 大纲 §2 补注 |
| 9 | 通过项 | 其余 ~22 句机制描述逐句对源码一致 ✅ (注解/扫描/拦截/配置) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (TCC 织入面 #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 织入闭环 | Scanner → Advisor → 方法调用拦截 ✅ | 通过 |
| V2 | 超时动态 | 未配置跟随全局 — 配置变更生效 ✅ | 通过 |
| V3 | 异常不吞 | 状态化处理 — 业务感知 ✅ | 通过 |
| V4 | 共存安全 | 已代理按序织入 — 不破坏既有 AOP ✅ | 通过 |
| V5 | 去重幂等 | PROXYED_SET — 重复增强防护 ✅ | 通过 |
| V6 | 规则完整 | Class+String 双形式 — 注解兼容 ✅ | 通过 |
| V7 | disable 生效 | 配置监听 — 动态关闭 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **isTimeoutException 判定**: ExecutionException 的 RollbackDone+timeout → 抛 cause (L248-253) — 超时回滚与业务异常区分 | 大纲 §3 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (isTimeoutException), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (disable 动态/初始化时序/Order 语义/GlobalLock handler 路径/自动配置参数)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | disable 动态? | afterPropertiesSet: disable → 仅注册配置监听 (L526-533) — 动态开启需重启? 监听是 CachedConfigurationChangeListener | 通过 (验证) |
| T2 | 初始化时序? | initialized CAS + initClient + findBusinessBeanNamesNeededEnhancement (L535-539) — 先客户端后扫描目标 | 通过 (验证) |
| T3 | ORDER_NUM=1024? | L96: scanner advisor 顺序基准 — 与 Spring 事务拦截器排序 (SPRING_TRANSACTION_INTERCEPTOR_CLASS_NAME L102) | 通过 (验证) |
| T4 | GlobalLock 路径? | handleGlobalLock (L297 注释) — S-12 全局锁拦截 (S-4 globalLockRequire 注解源) | 通过 (验证) |
| T5 | 自动配置参数? | SeataAutoConfiguration: applicationId (spring.application.name) + txServiceGroup 配置注入 (L59+) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 关闭安全 | disable 不 initClient — 无副作用 ✅ | 通过 |
| V2 | 时序正确 | 客户端先于扫描 — 依赖就绪 ✅ | 通过 |
| V3 | 排序共存 | 1024 基准 — 与 Spring 事务共存 ✅ | 通过 |
| V4 | 锁拦截 | GlobalLock → handleGlobalLock → S-4 ✅ | 通过 |
| V5 | 配置注入 | Boot 属性绑定 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **ORDER_NUM=1024 与 Spring 事务共存**: SPRING_TRANSACTION_INTERCEPTOR_CLASS_NAME 常量 (L102) — Advisor 排序面 (全局事务 vs 本地事务嵌套) | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 注解 (8+ 参数/超时重置/规则源) — 可写 ✅
- §2 扫描 (AOP 基础/双集合/检查器/排序) — 可写 ✅
- §3 拦截 (双 handler/状态化异常/FailureHandler) — 可写 ✅
- §4 配置 (两 bean/disable/initClient) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (Order 共存)。核心认知: **拦截器 2.x 模块化** (integration-tx-api 双 handler) + **超时重置语义** (跟随全局默认) + **rollbackRules 注解源闭环** (S-5) + **已代理按序织入**。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 解析/排序/初始化穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (AspectTransactional 完整字段/解析器责任链/advisor 相对定位/initClient/默认组)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | AspectTransactional 字段? | **9 字段** (L29-78): timeoutMills(60000)/name/rollbackFor×2/noRollbackFor×2/propagation(REQUIRED)/**lockRetryInterval(0)/lockRetryTimes(-1!)**/lockStrategyMode — **lockRetryTimes 默认 -1** (S-12 交叉: 负数=跟随全局?) | 发现 12 (补锚) |
| T2 | 解析器责任链? | **DefaultInterfaceParser 链式组合** (L73-97): SPI 加载 ALL_INTERFACE_PARSERS → 逐 parser → **同类型重复 → RuntimeException** ("there is already an annotation of type...") → 按 order 排序 → **setNextProxyInvocationHandler 链式串联** — 多注解共存责任链 | 发现 13 (补锚) |
| T3 | Advisor 相对定位? | **SeataInterceptorPosition 3 值** (Any/AfterTransaction/BeforeTransaction, L400-463): **相对 Spring TransactionInterceptor 定位 + Order 自动重排** (After: seataOrder >= txOrder → 重设 lower; Before: <= → higher, L447-470) | 发现 14 (补锚) |
| T4 | initClient? | L236-260: **DEFAULT_TX_GROUP_OLD 警告 (1.5 变更!)** + applicationId/txServiceGroup 非空校验 → **TMClient.init + RmClient.init** 双客户端 | 发现 15 (补锚) |
| T5 | 默认组? | DEFAULT_TX_GROUP_OLD vs DEFAULT_TX_GROUP (1.5 起变更) — 配置迁移面 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 责任链顺序 | order 排序 — TM 与 GlobalLock 相对顺序可配 ✅ | 通过 |
| V2 | 重复防护 | 同类型抛错 — 防双注解歧义 ✅ | 通过 |
| V3 | 相对定位保证 | After/Before 自动重排 — 与 Spring 事务嵌套语义正确 ✅ | 通过 |
| V4 | 客户端就绪 | TM/RM 双 init — 依赖齐备 ✅ | 通过 |
| V5 | 配置迁移 | 1.5 默认组警告 — 迁移提示 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **lockRetryTimes 默认 -1** (T1, AspectTransactional:73) — S-12 交叉 (负数语义) | 大纲 §1 注 |
| 13 | **补充锚点** | **解析器责任链** (T2): SPI parser 族 + 同类型重复 Runtime + order 链式串联 (L73-97) — 多注解共存 | 大纲 §2 注 |
| 14 | **语义标注** | **Advisor 相对定位自动重排** (T3): SeataInterceptorPosition (After/Before) + Order 重设 (L447-470) — 与 Spring 事务相对位置保证 | 大纲 §2 注 |
| 15 | **补充锚点** | **1.5 默认组警告 + 双客户端** (T4): DEFAULT_TX_GROUP_OLD 警告 + TMClient/RmClient init (L236-260) | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 注解 (9 参数/超时重置/规则源/lockRetryTimes -1) — 可写 ✅
- §2 扫描 (AOP 基础/双集合/责任链 parser/相对定位) — 可写 ✅
- §3 拦截 (双 handler/状态化异常/FailureHandler) — 可写 ✅
- §4 配置 (两 bean/disable/双客户端/1.5 警告) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复** (lockRetryTimes -1/责任链 parser/相对定位重排/1.5 默认组)。核心认知: **DefaultInterfaceParser 责任链** (SPI parser 族 + 链式串联) + **Advisor 相对 Spring 事务自动重排** (After/Before) + **lockRetryTimes 默认 -1**。大纲经修复后反写测试全过。
