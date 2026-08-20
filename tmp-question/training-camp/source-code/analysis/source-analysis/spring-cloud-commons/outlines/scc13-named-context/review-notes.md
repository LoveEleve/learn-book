# SCC-13 NamedContextFactory — REVIEW 记录 (2026-08-16)

## 二轮深度 REVIEW (07 换维度: 机制语义 vs 源码对照 + 消费方实证)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 6 | **生产者补全** | default. 前缀的生产者: LoadBalancerClientConfigurationRegistrar 用 `"default." + 类名` (L68/71); **AOT 初始化器排除 default. 配置** (LoadBalancerChildContextInitializer:84) | 已修 §3 |
| 7 | **语义精确化** | **含祖先查找只在 ResolvableType/注解变体**: getInstance(name, Class) 用 context.getBean 不含祖先 (L203); getInstance(name, ResolvableType) 才用 IncludingAncestors (L227) — 大纲原笼统归家族 | 已修 §4 |
| 8 | **类加载器双轨** | BeanFactory 的 BeanClassLoader 用 parent 的 (L169-173) vs **context 自身 setClassLoader 用工厂类的** (L183) — 大纲原只说"用父的" | 已修 §2 |
| 9 | 验证通过 | 消费方实证: LoadBalancerClientSpecification (L29)/FeignClientSpecification (OpenFeign)/LoadBalancerClientFactory extends (L46) + getInstance(serviceId) 委托 (L79-80) | 通过 |
| 10 | 验证通过 | 负面空间: named 包零事件发布 (子上下文无通信) / destroy WARN 注释 L112-113 | 通过 |

## 深审发现 (一轮, 锚点维度 — 全部修正)

| # | 类型 | 发现 | 修正 |
|:--:|:--:|:--|:--|
| 1 | 锚点漂移 | getInstance(name, Class) 实际 **L199-208** (大纲写 L204-210) | 已修 |
| 2 | 锚点漂移 | getInstance(ResolvableType) **L225-234** + beanNamesForTypeIncludingAncestors **L227** (写 L230) | 已修 |
| 3 | 锚点漂移 | getAnnotatedInstance 抛错 **L248** (写 L245-247); getInstances **L253-256**; getLazyProvider **L210-211** (写 L213-214) | 已修 |
| 4 | 锚点漂移 | Specification **L266-270** (写 L261-267) | 已修 |
| 5 | 验证通过 | L60-61/L69/L71/L98-102/L119-126/L130-140/L143-159/L161-192 (issue L162-163/AotDetector L175/setParent L189)/测试 L55-77/L79-117 | 通过 |

## harness 自抓缺陷 (方案 A 强制, MiniContext 14/14 PASS)

| # | 类型 | 发现 | 处理 |
|:--:|:--:|:--|:--|
| H1 | 模型语义 | Bean 注册 key (配置名) vs 查找 key (类型名) 不匹配 — 微缩版把"配置类"和"目标类型"混为一谈 | 统一为类型名 — 实证真实机制 (配置类注册, Bean 按类型查) |
| H2 | **配置时序语义** | 第二次 setConfigurations 加 default 配置, 已创建的上下文**不会重新 registerBeans** — 子上下文构建期固定 (负面空间第 2 条实证!) | 修正测试顺序 — 实证"配置构建期固定" |
| H3 | key 语义 | 基础设施注册 key 需匹配查找类型 (InfraLookup) — 微缩版覆写 registerBeans 补注册 | 修正 — 实证 defaultConfigType 恒注册 |

## 锚点密度统计

- file:line 锚点数: **30+** (🔴A 标准 ≥8 — 大幅超出)
- 全部锚点逐条 sed/grep 重验; 5 项检查 4 处修正

## 负面空间检查 (07 维度5)

- [x] 6 条 "不做" 声明 (缓存淘汰/配置热更新/跨上下文共享/子上下文通信/懒配置校验/动态增删)
- [x] 每条有对照物 (RefreshScope/Spring 父子容器) — **H2 实证"配置构建期固定"**

## 开篇质量检查 (07 维度5)

- [x] 读者处境 4 场景 (Feign 隔离/服务策略隔离/子上下文 vs 父子容器/CL 健壮性)
- [x] 每节有场景句 + 关键设计 + 跨层标注

## 反写测试 (只读大纲能否写文章)

- [x] 6 节 × 四要素完备; 数据流可追溯 (setConfigurations → getContext 双检 → createContext → registerBeans → getInstance)
- [x] 边界交代: default. 前缀/含祖先查找/AOT 分支/类加载器

## 方法论教训

- **harness 的"配置时序"实证最有价值** — H2 用失败证明了"子上下文构建期固定" (负面空间第 2 条), 不是测试 bug 而是真实语义
- **配置类 vs 目标类型是两个概念** — 微缩建模必须区分"注册什么"和"查找什么"
- **github issue 注释是时空溯源锚** — netflix#3101/openfeign#475 直接说明 buildContext 类加载器修复动机
