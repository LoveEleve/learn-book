# SCC-3 服务发现抽象 — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 跨域一致性)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | 遗漏 | **Composite 覆写 probe()** (L50-56) — 遍历所有子客户端探活, 非接口 default | 已修 §2 |
| 7 | 语义补强 | **sort 原地修改传入 List** (L41) — 不可变列表抛异常 (harness H1 实证); getDiscoveryClients 访问器 (L58-60) | 已修 §2 |
| 8 | 语义补强 | **reactiveProbe() default = getServices().then()** (L94-95, Mono\<Void\>) — probe @Deprecated(forRemoval) L75-81 带 LOG.warn | 已修 §5 |
| 9 | **属性消费链** | autoRegister=false 注入的属性被 **AutoServiceRegistrationAutoConfiguration (L30) + AutoServiceRegistrationConfiguration (L28)** 的 @ConditionalOnProperty 消费 — 注册装配双关 | 已修 §4 |
| 10 | **健康检查语义** | 非"能列服务即 UP"那么简单: **discoveryInitialized 门控** (L55-59, InstanceRegisteredEvent 触发) + **双模式** (useServicesQuery=true→getServices / false→probe) + catch → down(e) (L83-84) | 已修 §6 |
| 11 | 验证通过 | 负面空间"不做实例缓存" (Simple 的 Map 是配置数据非缓存 L45) / 跨域 SCC-3→SCC-4 属性联动闭环 | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | DiscoveryClient 类 **L32** (写 L36-79) + DEFAULT_ORDER **L37** + description **L43** + getInstances **L50** + getServices **L55** + probe **L66-68** | 已修 |
| 2 | 锚点漂移 | Composite **L36** (写 L27-60) + 短路返回 **L51-56** (写 L44-49) + LinkedHashSet **L65** (写 L51-60) | 已修 |
| 3 | 锚点漂移 | SimpleDiscoveryClient **L34** (写 L27-42) + getInstances **L48-51** + getServices **L59-60** + getOrder **L63-65** | 已修 |
| 4 | 锚点漂移 | EnableDiscoveryClient **L38** (写 L22-28) + autoRegister **L44** + isEnabled **L68-69** | 已修 |
| 5 | 验证通过 | ImportSelector L37/L40-66 / ReactiveDiscoveryClient probe L75-79 / HealthIndicator L37/48/74 / HeartbeatMonitor L27/29/35 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniDiscovery 16/16 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | 可变性 | List.of 不可变列表 sort 抛 UnsupportedOperationException | 改用 ArrayList — 实证"构造排序需要可变列表" |
| H2 | 验证通过 | 排序短路语义: 空实例跳过继续查 / order 小优先 / 合并去重 — 全部实证 | 16/16 |

## 锚点密度统计

- file:line 锚点数: **25+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (客户端内建/实例缓存/负载均衡/故障转移/推送订阅/实例排序)
- [x] 每条有对照物 (Nacos 快照/长轮询/SCC-6 LoadBalancer)

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (实例查询/换注册中心不改代码/组合顺序/故障暴露)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (注解 → ImportSelector → 实现 → Composite → 消费)
- [x] 边界交代: 短路 vs 合并/autoRegister 双分支/probe 演进

## 方法论教训

- **harness 短路语义实证** — "空实例跳过继续查"是 Composite 的核心行为, 用空客户端测试证明
- **List.of 可变性陷阱** — 真实 Composite 构造接收 List 后 sort, 调用方传不可变列表会崩 (源码无防御) — 微缩版复现了真实行为
