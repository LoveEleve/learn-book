# D-3 服务引用 — 深审 REVIEW 记录 (六层深审 + 推理验证)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "ReferenceConfig.get()—RegistryProtocol.refer—Cluster 容错" — **3.x 签名修正**: get(boolean check) L228-248 (带参); 引用链实证: get→init→createProxy→createInvoker→Cluster.join | 大纲 §1-§3 |
| 2 | **补充锚点** | **group 合并集群**: group="a,b"/"*" → MERGEABLE_CLUSTER (CommonConstants.java:292) — 分组合并面, 执行计划未提 | 大纲 §2 |
| 3 | **补充锚点** | **MigrationInvoker 3.x 迁移**: ServiceDiscoveryMigrationInvoker + 迁移规则三态 (MigrationStep.java: FORCE_INTERFACE/APPLICATION_FIRST/FORCE_APPLICATION, 实证 L205/246/287) — 接口级→应用级迁移面, 执行计划未提 | 大纲 §2 |
| 4 | **补充锚点** | **injvm 精确判定**: InjvmProtocol.isInjvmRefer (L85-101) — scope=local 判定 + **generic 例外 + broadcast 例外 + exporterMap 存在才算**; 兜底 URL 只是入口 | 大纲 §1 |
| 5 | **语义标注** | **ZoneAwareCluster 双默认**: 多注册中心 CLUSTER_KEY 默认 ZoneAwareCluster.NAME (L697) vs 直连默认 Cluster.DEFAULT (L709) — 两种场景默认不同 | 大纲 §3 |
| 6 | 行号验证 | 全函数 ~25 锚点 + 跨文件 grep (ReferenceConfig 228-248,332-404,431,489-523,605,633-663,668-714,716-767,854 / RegistryProtocol 555-595,601-609 / MigrationInvoker 81-115,314-323,472-497 / StaticDirectory 57-69,113-120 / Cluster.java:48 / ProxyFactory.java:29 / InvokerInvocationHandler 34-100 / AbstractConfig.java:718) | 记录 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 链完整 | 配置→URL→invoker→集群→代理 5 层渐进 ✅ | 通过 |
| V2 | 本地通道 | injvm port 0 — 同 JVM 引用 (isInjvmRefer 判定) ✅ | 通过 |
| V3 | 集群接入 | StaticDirectory + Cluster.join 装饰链 ✅ | 通过 |
| V4 | 迁移平滑 | 双 invoker + 规则三态切换 ✅ | 通过 |
| V5 | 代理门面 | InvokerInvocationHandler → RpcInvocation → InvocationUtil.invoke (D-4 桥) ✅ | 通过 |
| V6 | 启动检查 | checkInvokerAvailable 轮询 fail-fast ✅ | 通过 |
| V7 | group 聚合 | mergeable cluster — 多组服务合并 ✅ | 通过 |

## 反写测试 (只读大纲能否写文章)

- §1 装配面 (get→init 9 步/双路径/injvm 判定/启动检查) — 可写 ✅
- §2 注册中心引用 (group 合并/MigrationInvoker 三态/consumerUrl) — 可写 ✅
- §3 集群接入 (三分支/StaticDirectory/包装序) — 可写 ✅
- §4 代理与检查 (SPI 家族/Object 拦截/RpcInvocation/D-4 桥) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 深审汇总

锚点 ~25 处验证, 4 闭环完成 (q1-q4), **数字穷举 1 + 语义标注 1 + 补充锚点 3**。核心认知: **引用双路径 (直连/注册中心) + injvm 兜底对称 (与 D-2 导出) + MigrationInvoker 3.x 迁移面 + StaticDirectory 静态目录 (对照 D-5 动态订阅) + 代理门面桥接 D-4**。待二次 REVIEW (按 07 五维度轮换 + 反写测试)。

---

# 二次深度 REVIEW (2026-08-16, 07 五维度 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 D-1/D-2 ✅ (Protocol SPI/URL/injvm 对称均消费); 引出 D-4/D-5/D-7/D-11 ✅; 对照 Spring/Feign/ZK ✅; 读者处境场景化 ✅; 锚点 ~25 ✅; 负面空间 6 条 ✅; 横切 (装配/注册/集群/代理) 全覆盖 ✅; 性能 (双检锁/懒加载 get) ✅; 内存 (ConsumerModel/双 invoker 持有) ✅; 一致性 (双路径决策/injvm 判定例外) ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | §1 装配面 ~10 句机制描述逐句对源码一致 ✅ (get 9 步链/双路径/injvm/isInjvmRefer 例外/check 轮询) | 记录 |
| 9 | 通过项 | §2 注册中心引用: refer 分支 (group 合并/默认) + MigrationInvoker 三态 (实证 L205/246/287) + consumerUrl 构建 ✅ | 记录 |
| 10 | 通过项 | §3 集群接入: 三分支 + StaticDirectory (BitList) + buildFilterChain 语义 + 包装序注释 ✅ | 记录 |
| 11 | 通过项 | §4 代理面: ProxyFactory SPI 家族 (jdk/javassist/stub wrapper/nativestub) + Object 拦截 + RpcInvocation 构建 + D-4 桥 ✅ | 记录 |
| 12 | **harness** | MiniServiceRefer 编译运行 **5/5 PASS** (A 双路径+B 迁移+C 集群+D 代理, 自抓 0) | 记录 |

## 二次 REVIEW 汇总

共 **0 处修复** (一次 REVIEW 已修复 4 处), 反写测试结论: 大纲机制面完整可支撑写作, harness 5/5 全过。**D-3 可进入写作阶段**。

---

# 三次深度 REVIEW (2026-08-16, 09 §3 "重审也可能错" — 存疑面穷举 + 逐锚点 re-grep)

> 动机: 对 outline 全部锚点重新 grep, 穷举六个存疑面 (protocolSPI 注入/mesh 面/parseUrl 直连/迁移默认规则/interceptInvoker 语义/shouldCheck 默认)。

## 追查过程 (六个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | protocolSPI 注入? | L188: `getExtensionLoader(Protocol.class).getAdaptiveExtension()` (L129 字段) — 自适应注入 (D-1 消费) | 通过 (验证) |
| T2 | shouldCheck 默认? | ReferenceConfigBase.java:108-121: check → consumer.check → **默认 true** (注释 "// default true") | 通过 (验证) |
| T3 | mesh 面? | **meshModeHandleUrl (L493 调用, L530-605)**: mesh.enable → **triple://providedBy.namespace.svc.cluster.local:80** (POD_NAMESPACE 环境变量 + K8s Service 名 + cluster.local 域名) — **Envoy 路由直连** | **发现 13 (补锚)** |
| T4 | parseUrl 直连细节? | **L605-632**: 分号多 URL; **registry URL → REFER_KEY / peer-to-peer → ClusterUtils.mergeUrl + PEER_KEY 标记** | **发现 14 (补锚)** |
| T5 | 迁移默认规则? | 配置中心未配置 → **INIT 规则** (RegistryConstants.INIT="INIT", MigrationRuleListener.java:261); getStep 兜底 **"initial step: APPLICATION_FIRST"** (MigrationRule.java:167 注释实证) | **发现 15 (补锚)** |
| T6 | interceptInvoker 语义? | **RegistryProtocol.java:623-640: findRegistryProtocolListeners → listener.onRefer 回调 — 是 SPI 监听器集合, 非拦截器链!** | **发现 16 (事实错误修正)** |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 注入自适应 | protocolSPI 也是 SPI 自适应 — 协议可切 ✅ | 通过 |
| V2 | 检查默认开 | shouldCheck 默认 true — fail-fast 是默认行为 ✅ | 通过 |
| V3 | mesh 直连 | mesh.enable → triple URL 直连 Envoy — 3.x 网格面 ✅ | 通过 |
| V4 | 直连标记 | PEER_KEY — 直连 invoker 可识别 ✅ | 通过 |
| V5 | 迁移默认 | INIT → APPLICATION_FIRST — 双订阅共存默认 ✅ | 通过 |
| V6 | 监听器模式 | RegistryProtocolListener.onRefer — 扩展点而非拦截 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 13 | **补充锚点** | **mesh 网格模式** (T3): mesh.enable → triple://providedBy.namespace.svc.cluster.local:80 (POD_NAMESPACE + providedBy + cluster.local, L530-605) — K8s/Envoy 直连面 | 大纲 §1 + pass2-q1 注 |
| 14 | **补充锚点** | **parseUrl 直连分支** (T4): 分号多 URL + registry vs peer-to-peer (**PEER_KEY 标记 + mergeUrl**) | 大纲 §1 注 |
| 15 | **补充锚点** | **迁移默认规则** (T5): 无配置中心 → INIT 规则 → APPLICATION_FIRST 兜底 (MigrationRule.java:167) | 大纲 §2 + pass2-q2 注 |
| 16 | **事实错误修正** | **interceptInvoker 语义** (T6): 首版写 "ClusterInterceptor 拦截链" — 实为 **RegistryProtocolListener.onRefer 监听器集合** (L623-640) | 大纲 §2 + pass2-q2 修正 |

## 三次 REVIEW 汇总

六存疑面全实证 (T1-T6); 推理验证 6 项全过 (V1-V6); **新发现 4 处全部修复 (含 1 处事实错误 #16 — interceptInvoker 语义)**。核心认知: **mesh 网格直连面 (3.x) + parseUrl PEER_KEY 直连标记 + 迁移默认 APPLICATION_FIRST + 注册监听器模式**。大纲经修复后再次反写测试可支撑写作。

---

# 四次深度 REVIEW (2026-08-16, 07 维度1 叙事桥一致性 + 跨文档一致性)

> 维度轮换: 三次 REVIEW 聚焦域内锚点; 本轮跨域/跨文档检查 (07 维度1: 叙事桥 + 结构完整性)。

## R1 跨域桥接检查 (07 维度1)

| # | 桥 | 检查 | 结论 |
|:--:|:--|:--|:--:|
| 17 | IN 桥 | D-3 前置 D-1/D-2 — D-2 OUT 桥 "引出: D-3-服务引用 (对称面)" ✅; D-1 OUT 桥 "引出 D-2~D-7 (全框架)" 含 D-3 ✅ | 通过 |
| 18 | OUT 桥 | D-3 引出 D-4/D-5/D-7/D-11 — 与 PLAN §六 拓扑 (D-4/D-5/D-6/D-7/D-8a...D-11 均在 D-3 后) 一致 ✅; 无前向引用 (11 > 3) ✅ | 通过 |
| 19 | 对称面 | D-2 导出 (scope 三态/本地始终可导) vs D-3 引用 (injvm 兜底/本地始终可引) — 对称叙事成立 ✅ | 通过 |

## R2 跨文档一致性

| # | 检查项 | 结果 |
|:--:|:--|:--|
| 20 | PLAN §三 D-3 行 (get L230→refer L557→Cluster) vs outline 4 节 — 一致 ✅ | 通过 |
| 21 | interceptInvoker 修正 (#16) 三处同步: outline §2 / pass2-q2 机制链 / pass2-q2 关键设计 — 全部修正 ✅ | 通过 |
| 22 | pass1 待展开 5 项全部完成并标记 ✅ | 通过 |
| 23 | harness 默认 APPLICATION_FIRST vs 源码实证 (MigrationRule.java:167) — 一致 ✅ | 通过 |
| 24 | HANDOFF §零 D-3 状态行 vs PLAN 进度行 vs review-notes 汇总 — 一致 ✅ | 通过 |

## 四次 REVIEW 汇总

跨域桥 3 项 (17-19) + 跨文档 5 项 (20-24) 全过, **0 处新修复**。收敛信号: 连续两轮零结构性发现 (二次 REVIEW 0 修复 + 本轮 0 修复), 且维度不同 (07 五维度 → 存疑面穷举 → 叙事桥)。**D-3 深审收敛, 可进入写作阶段**。
