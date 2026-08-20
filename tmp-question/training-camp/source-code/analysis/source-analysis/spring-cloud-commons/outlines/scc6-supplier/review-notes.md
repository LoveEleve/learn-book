# SCC-6 ServiceInstanceListSupplier 体系 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | **消费方补全** | SelectedInstanceCallback **消费方是 LoadBalancer**: RoundRobinLoadBalancer:93 / RandomLoadBalancer:74 选中后调用; SameInstancePreference:94-95 覆写 (SCC-12 联动) | 已修 §2 |
| 7 | 语义补强 | CacheFlux **cache 不存在 → log.error + 走 miss 路径** (L58-61, 退化场景每请求打底层) + andWriteWith 写回 (L71-78) | 已修 §4 |
| 8 | **共享语义** | aliveInstancesReplay: **replay(1).refCount(1)** (L77-79) — 多消费者共享健康流 + **afterPropertiesSet 主动订阅** (L81-87, 启动即开始) | 已修 §5 |
| 9 | 语义精确化 | build() 顺序: **baseCreator 建基底 → for(creator) 依次包外层** (后注册在外层, L443-449) | 已修 §6 |
| 10 | 验证通过 | 负面空间"不做实例排序/选择" (Supplier 包零 choose) / 跨域 SCC-6→SCC-7 (RoundRobin 持有 SingletonSupplier L51) | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | Supplier 接口 **L33** (写 L31) + getServiceId **L35** + get(Request) **L37-39** + builder **L41-43** | 已修 |
| 2 | 锚点漂移 | Delegating **L32** (写 L27) + delegate **L35** + getServiceId **L47-48** + selectedServiceInstance **L52-55** | 已修 |
| 3 | 锚点漂移 | Discovery timeout 属性 **L51** + 默认 **L55** (写 L49/55 区间) | 已修 |
| 4 | 语义补全 | HealthCheck **Repeat 双驱动** (L91-92): repeatHealthCheck 周期独立于 refetchInstances 周期 | 已补 |
| 5 | 验证通过 | Caching L40/47/55/57/70 / HealthCheck L48/68/71-75 / Builder L74-352 (20+ with) | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniSupplier 11/11 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | 断言时序 | "二次查仍实时"断言前只调了一次 get (断言时机 bug 非实现 bug) | 补第二次 get 调用 — 实证基底无缓存语义 |
| H2 | 验证通过 | 缓存 miss/命中计数 / 健康过滤 / 链序 (缓存包健康过滤) / delegate null 校验 / getServiceId 沿链 — 全部实证 | 11/11 |

## 锚点密度统计

- file:line 锚点数: **30+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (实例排序/多注册中心聚合/权重内建/健康协议内建/缓存淘汰内建/故障转移)
- [x] 每条有对照物 (SCC-7 选择/SCC-3 Composite/WeightFunction)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (实例列表来源/链式加工/响应式缓存/周期健康检查/自定义链)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (Builder 装配 → 基底 → 缓存 → 健康过滤 → ReactorLoadBalancer 消费)
- [x] 边界交代: CacheFlux miss 回填/超时 30s/Repeat 双驱动/delegate null 校验

## 方法论教训

- **harness 断言时序 bug 与实现 bug 要区分** — "二次查"断言前只查了一次是测试问题, 修正后实证基底无缓存语义
- **CacheFlux 是"响应式缓存查找"** — onCacheMissResume 是核心 (miss 才打底层), 不是简单 put/get
- **Repeat 双驱动** — refetchInstances (重拉) 与 repeatHealthCheck (检查) 是两个独立周期, 大纲最初只写了一个
