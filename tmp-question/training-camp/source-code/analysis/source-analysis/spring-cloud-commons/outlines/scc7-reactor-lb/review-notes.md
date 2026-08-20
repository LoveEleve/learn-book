# SCC-7 ReactorLoadBalancer 策略 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | 测试实证 | **shouldOrderEnforcedWhenPositiveOverflow** (Tests:56-58) — & MAX_VALUE 溢出场景仍有序; **shouldNotMovePositionIfOnlyOneInstance** (L61-73) — choose 两次 position 仍 0 | 已补 §4 引用 |
| 7 | **回调链实证** | **shouldCallSelectedServiceInstanceIfSupplierOrItsDelegateIsInstanceOf** (Tests:75-85) — 回调经 Delegating 传递到 delegate (RetryAware 包装实证) — 大纲补 "Supplier 或其 Delegate" | 已修 §3 |
| 8 | **工厂语义补全** | LoadBalancerClientFactory **implements ReactiveLoadBalancer.Factory** (L47) + **NAMESPACE/PROPERTY_NAME** (L55-58) + **defaultConfigType = LoadBalancerClientConfiguration** (L63) + **默认 RoundRobin** (Configuration:74-75) + **默认 Supplier 链 withDiscoveryClient().withCaching()** (L89) | 已修 §6 |
| 9 | 验证通过 | 负面空间"不做多策略切换" (无 switch 机制) / 跨域 SCC-7→SCC-11 (BlockingLoadBalancerClient:163 Mono.from().block()) | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | ReactorLoadBalancer 类 **L31** (写 L28-44) + choose **L39** + choose() default **L41-43** | 已修 |
| 2 | 锚点漂移 | RoundRobin 类 **L43** (写 L45-58) + SingletonSupplier **L64-66** (写 L53-55) + seed **L60** (写 L51) | 已修 |
| 3 | 锚点漂移 | choose **L82-86** (写 L55-61) + getInstanceResponse **L98-116** (写 L72-85 严重偏移) + & MAX **L114** + 取模 L116 | 已修 |
| 4 | 锚点漂移 | Random 类 **L41** (写 L35) + ThreadLocalRandom **L86** (写 L76-77) | 已修 |
| 5 | 验证通过 | LoadBalancerClientFactory L46/L79-80 / ocelli 注释 L80-81 / 测试断言 L46-75 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniReactorLB 12/12 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | 编译缺陷 | StaticSupplier 匿名类 @Override 不存在的接口方法 — 微缩建模错误 (StaticSupplier 应实现 SelectedInstanceCallback) | 修正 StaticSupplier implements 双接口 — 实证回调机制 |
| H2 | 验证通过 | 轮询数学 (seed/& MAX/取模) / 单实例不转 / 空 EmptyResponse / 回调 / 每服务隔离 — 全部实证 | 12/12 |

## 锚点密度统计

- file:line 锚点数: **20+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (加权轮询内建/粘性会话/重试/实例排序/健康检查/多策略切换)
- [x] 每条有对照物 (WeightedSupplier SCC-12/RetryLoadBalancerInterceptor SCC-11)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (轮询均匀/& MAX 语义/单实例/随机种子/每服务隔离)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (choose → supplier.get → next → process → 回调)
- [x] 边界交代: 三态/& MAX 循环/seed 防惊群/Noop 兜底

## 方法论教训

- **"& Integer.MAX_VALUE 忽略符号位"是位运算循环** — 防 AtomicInteger 溢出负值, 面试点
- **随机种子防惊群是设计决策** — `new Random().nextInt(1000)` 不是随意, 是"多实例冷启动不同步"
- **单实例不转位置的注释是关键** — "especially some suppliers have already filtered" 说明与 SCC-6 健康过滤的联动
