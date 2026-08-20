# D-7 集群容错 — 深审 REVIEW 记录 (六层深审 + 推理验证)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "8 实现" — **实测 9** (+ZoneAware) ✓ PLAN 已修正; **SPI 注册表 11 项穷举** (9 + mock/scope 装饰); **默认值穷举**: DEFAULT_RETRIES=2 (CommonConstants:419) / DEFAULT_CLUSTER_STICKY=false (Constants:75) / reselectCount 限制 | 大纲 §1-§4 |
| 2 | **补充锚点** | **join 三层包装**: ClusterFilterInvoker (D-4 集群链钩子兑现!) + buildInterceptorInvoker (3.x 拦截器) + 2.7 兼容开关 | 大纲 §1 + pass2-q1 |
| 3 | **补充锚点** | **重试前刷新目录**: i>0 时 list(invocation) — 地址变化在重试窗口生效 | 大纲 §2 + pass2-q2 |
| 4 | **补充锚点** | **业务异常不重试**: e.isBiz() 直接抛 (L104-107) | 大纲 §2 + pass2-q2 |
| 5 | **补充锚点** | **reselect 三段式 + reselectCount 限制** (防大集群挂起 L268-271) | 大纲 §3 + pass2-q3 |
| 6 | **补充锚点** | **ZoneAware 三级优先**: preferred → zone 附件/ZoneDetector → 同 zone (L60-110) | 大纲 §4 + pass2-q4 |
| 7 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (Cluster 34 / AbstractCluster 45-108 / FailoverClusterInvoker 57-142 / AbstractClusterInvoker 140-185,254-328,402-412,452 / ZoneAwareClusterInvoker 46-110 / MockClusterWrapper 28-38) | 记录 |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 三层分离 | doJoin 策略 + 集群链 + 拦截器 — 职责清晰 ✅ | 通过 |
| V2 | 重试换节点 | select 带 invoked — 不重复 ✅ | 通过 |
| V3 | 目录刷新 | 重试前 list — 地址变化生效 ✅ | 通过 |
| V4 | 业务不重试 | isBiz — 无意义重试消除 ✅ | 通过 |
| V5 | 重选不撞车 | selected > available 三段式 ✅ | 通过 |
| V6 | 区域亲缘 | preferred → zone → 同 zone 三级 ✅ | 通过 |
| V7 | 降级不侵入 | Mock wrapper — 策略无感知 ✅ | 通过 |

## 反写测试 (只读大纲能否写文章)

- §1 装配面 (SPI 11 项/join 三层/装饰) — 可写 ✅
- §2 重试面 (RETRIES 默认/目录刷新/isBiz/错误信息) — 可写 ✅
- §3 选择面 (sticky 默认 false/reselect 三段式) — 可写 ✅
- §4 策略族 (9 策略/ZoneAware 三级/路由/Mock) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 深审汇总

锚点 ~30 处验证, 4 闭环完成 (q1-q4), **数字穷举 1 + 补充锚点 5**。核心认知: **join 三层包装 (策略+集群链+拦截器) + Failover 换节点重试 (目录刷新/isBiz) + sticky 粘滞 + reselect 三段式 + ZoneAware 三级优先 + Mock 不侵入**。待二次 REVIEW (07 五维度 + 反写测试)。

---

# 二次深度 REVIEW (2026-08-16, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | 通过项 | 前置 D-1 (SPI/Wrapper) / D-3 (Cluster 黑盒→深潜) / D-5 (预路由钩子) / D-6 (selected 钩子) ✅ 全部兑现; 引出 D-8a ✅; 对照 Sentinel/Ribbon ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (装配/重试/选择/策略族) 全覆盖 ✅; 性能 (reselectCount 限制/粘滞缓存) ✅; 内存 (invoked/providers 收集) ✅; 一致性 (isBiz 不重试/selected 规则) ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | 通过项 | §1 装配面 ~8 句逐句对源码一致 ✅ (SPI 11 项/join 三层/2.7 兼容) | 记录 |
| 10 | 通过项 | §2 重试面: RETRIES 默认 2/目录刷新/isBiz/失败收集 ✅ | 记录 |
| 11 | 通过项 | §3 选择面: sticky 默认 false/三段式 reselect/reselectCount ✅ | 记录 |
| 12 | 通过项 | §4 策略族: 9 策略/ZoneAware 三级/路由两段式/Mock wrapper ✅ | 记录 |
| 13 | **harness** | MiniCluster 编译运行 **5/5 PASS** (A 装配+B 重试换节点/业务不重试+C 粘滞+D ZoneAware; 断言修正: bizException 显式设置) | 记录 |

## 二次 REVIEW 汇总

共 **0 处机制修复** (一次 REVIEW 已修复 6 处), harness 5/5 全过, 反写测试结论: 大纲机制面完整可支撑写作。**D-7 可进入写作阶段**。

---

# 三次深度 REVIEW (2026-08-16, 09 §3 "重审也可能错" — 存疑面穷举 T1-T7)

> 动机: 对 outline 全部锚点重新 grep, 穷举七个存疑面 (Forking 并行/Broadcast 失败阈值/Mergeable 合并/Failback 异步/路由执行点/上下文传播/目录检查)。

## 追查过程 (存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | 上下文传播? | invokeWithContext (L402-412): setContext → Profiler → setRemote → invoker.invoke | 通过 (验证) |
| T2 | Forking 并行? | **FORKS_KEY (默认 DEFAULT_FORKS) 并行; forks<=0/>=size → 全部; invokeWithContextAsync 先到先得** (L76-80) | **发现 9 (补锚)** |
| T3 | Broadcast 失败策略? | **broadcast.fail.percent 0~100 (默认 100=最后才抛; 0=任一失败即抛)** — PR #7174 注释实证 (L58-74) | **发现 10 (补锚)** |
| T4 | Mergeable 合并? | **MERGER_KEY 方法级 + MergerFactory** (L64-65); 无 merger → 只调一个 group | **发现 11 (补锚)** |
| T5 | Failback 异步? | **RetryTimerTask + failTimer.newTimeout(RETRY_FAILED_PERIOD)** (L103-106) + "Asynchronous call method must be used here" 注释 (L124) | **发现 12 (补锚)** |
| T6 | 路由执行点? | AbstractClusterInvoker.list → directory.list (L452) — RegistryDirectory 预路由缓存兑现 | 通过 (验证) |
| T7 | 目录检查? | checkInvokers + checkWhetherDestroyed (重试循环内) | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 并行不重复 | select 去重 — Forking 不重复选同节点 ✅ | 通过 |
| V2 | 广播阈值可配 | fail.percent 0~100 — 失败容忍度可控 ✅ | 通过 |
| V3 | 合并可配 | MERGER_KEY + MergerFactory — 合并器可插拔 ✅ | 通过 |
| V4 | 异步不阻塞 | RetryTimerTask — 失败不阻塞主调用 ✅ | 通过 |
| V5 | 上下文完整 | setContext/setRemote — 三件套传递 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 9 | **补充锚点** | Forking 细节: FORKS_KEY 默认值 + invokeWithContextAsync 并行先到先得 (L76-80) | 大纲 §4 + pass2-q4 |
| 10 | **补充锚点** | **Broadcast 失败阈值**: broadcast.fail.percent 0~100, 默认 100 (L58-74, PR #7174) — 非"任一失败即抛" | 大纲 §4 + pass2-q4 |
| 11 | **补充锚点** | Mergeable: MERGER_KEY + MergerFactory (L64-65), 无 merger 只调一组 | 大纲 §4 + pass2-q4 |
| 12 | **补充锚点** | Failback: RetryTimerTask + RETRY_FAILED_PERIOD 异步重试 (L103-106) | 大纲 §4 + pass2-q4 |

## 三次 REVIEW 汇总

七存疑面全实证 (T1-T7); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复**。核心认知: **Forking 并行先到先得 + Broadcast 失败阈值可配 (PR #7174) + Mergeable MergerFactory + Failback RetryTimerTask** — 策略族细节补全。大纲经修复后再次反写测试可支撑写作。

---

# 四次深度 REVIEW (2026-08-16, 07 维度1 叙事桥一致性 + 跨文档一致性)

## R1 跨域桥接检查 (07 维度1)

| # | 桥 | 检查 | 结论 |
|:--:|:--|:--|:--:|
| 13 | IN 桥 | D-7 前置 D-1/D-3/D-5/D-6 — D-3 OUT (Cluster 黑盒) ✅; D-5 OUT (预路由) ✅; D-6 OUT (selected 重选) ✅ | 通过 |
| 14 | **三钩子兑现** | D-4 OUT "doInvoke 黑盒" → D-7 §2 重试循环 (list/select/invoked) ✅; D-5 OUT "预路由" → D-7 §4 路由执行点 (directory.list L452) ✅; D-6 OUT "selected 重选" → D-7 §3 reselect ✅ | 通过 (闭环) |
| 15 | OUT 桥 | D-7 引出 D-8a (ExchangeClient 网络层) — PLAN 拓扑 (8a > 7) ✅; 无前向引用 ✅ | 通过 |

## R2 跨文档一致性

| # | 检查项 | 结果 |
|:--:|:--|:--|
| 16 | PLAN §三 D-7 行 (9 实现+Mock/ClusterInvoker) vs outline — 覆盖 ✅; 数字 8→9 已修正 ✅ | 通过 |
| 17 | 三次 REVIEW 修正 (#9-#12) 同步: outline §4 + pass2-q4 — 全部修正 ✅ | 通过 |
| 18 | pass1 待展开 5 项: calculateInvokeTimes (q2)/reselect (q3)/Router 链 (q4)/Mock (q4)/invokeWithContext (q1) — 全部完成 ✅ | 通过 |
| 19 | harness (5 断言) vs 大纲机制: 装配/重试/biz 不重试/粘滞/ZoneAware 一致 ✅ | 通过 |
| 20 | HANDOFF §零 D-7 状态行 vs PLAN 进度行 vs review-notes — 一致 ✅ | 通过 |
| 21 | 锚点行号复查: Forking L76-80 / Broadcast L58-74 / Mergeable L64-65 / Failback L103-106 — 全部吻合 ✅ | 通过 |

## 四次 REVIEW 汇总

跨域桥 3 项 (13-15, 含三钩子兑现闭环) + 跨文档 6 项 (16-21) 全过, **0 处新修复**。收敛信号: 连续两轮零结构性发现 (二次 REVIEW 0 修复 + 本轮 0 修复), 且维度不同 (07 五维度 → 存疑面穷举 → 叙事桥)。**D-7 深审收敛, 可进入写作阶段**。
