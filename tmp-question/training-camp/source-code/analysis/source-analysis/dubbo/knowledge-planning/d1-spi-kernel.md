# D-1 SPI 微内核 — 加载/创建/自适应/激活 四面与 URL 总线

> 项目: Dubbo | 🔴 Deep / 1 篇 | ExtensionLoader(1522)+SPI/Adaptive/Activate/Wrapper 注解+AdaptiveClassCodeGenerator+ExtensionDirector
> 基线: DUBBO-PLAN D-1 (内核域, 一切基础) — 前置: **无** — 展开 加载面→创建面→自适应面→激活面

---

## §0.8

- 🔴 Deep，1篇 — 入口(**getExtensionLoader L242: 接口+@SPI 双检查, 否则 IllegalArgumentException; @SPI scope 参数 SPI.java:61-64**) → 加载面(**3 目录三级优先级 L988-998: internal=MAX/dubbo=NORMAL/services=MIN; loadResource L1139 "name=类名" 逐行+@Deprecated 标记; 懒加载双检锁**) → 创建面(**单例缓存 L216-221; injectExtension setter DI+@DisableInject 跳过+自适应对注入; Wrapper 链 L226-246[WrapperComparator 永不为 0 排序+reverse+@Wrapper matches/mismatches/order→构造器逐层包装=AOP]; initExtension Lifecycle; unacceptableExceptions 失败缓存**) → 自适应面(**getAdaptiveExtension 双检锁 L610; 无 @Adaptive 类→AdaptiveClassCodeGenerator 动态生成五步 L1467[URL 定位→null 检查→多键回退→extName→getExtension]; 注入器本身自适应 L219-221**) → 激活面(**getActivateExtension L344-440: group 筛选+ActivateComparator 四层权重[before/after→order→类名兜底 L114-117]+isActive OR 语义+getAnyMethodParameter 兜底 L471-495**) → 3.x 架构(**ExtensionDirector scoped loader manager L28: ScopeModel+destroyed**)
- 设计模式: [模式: SPI 微内核+单例+装饰器链(AOP)+动态代码生成+URL 总线驱动]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ExtensionLoader.java:242 | 双检查 | **getExtensionLoader: 接口+@SPI 双检查** — 无 @SPI 抛 IllegalArgumentException; @SPI scope 参数 (SPI.java:61-64) 作用域声明 | High |
| ExtensionLoader.java:988-998 | 目录优先级 | **3 加载目录三级优先级**: internal=MAX / dubbo=NORMAL / services=MIN — 内部扩展优先覆盖 | High |
| ExtensionLoader.java:1139 | 解析 | **loadResource: "name=类名" 逐行解析 + @Deprecated 标记** (废弃扩展缓存) | High |
| ExtensionLoader.java:216-221 | 单例 | **extensionInstances CHM + putIfAbsent** — 单例缓存 | High |
| ExtensionLoader.java:280+ | DI | **injectExtension: setter 扫描 + @DisableInject 跳过 → 自适应对注入** (getAdaptiveExtension); 注入器本身自适应 (L219-221) | High |
| ExtensionLoader.java:226-246 | Wrapper | **Wrapper 链: WrapperComparator 排序永不为 0 (L54-55 注释集合安全) + reverse + @Wrapper matches/mismatches/order → 构造器逐层包装 = AOP** | High |
| ExtensionLoader.java:248 | 生命周期 | **initExtension: Lifecycle initialize** — 启动钩子 | High |
| ExtensionLoader.java:610+ | 自适应 | **getAdaptiveExtension 双检锁 + 错误缓存**; 无 @Adaptive 类 → 动态生成 | High |
| AdaptiveClassCodeGenerator.java:1467 | 生成 | **动态生成五步**: URL 参数定位 → null 检查 → @Adaptive value 多键回退 (Adaptive.java:44-47) → extName 赋值 → getExtensionLoader().getExtension(extName) (模板 L75) | High |
| ExtensionLoader.java:344-440 | 激活 | **getActivateExtension: group 筛选 + ActivateComparator 四层权重** (before/after 双向→order→类名兜底 L114-117) | High |
| ExtensionLoader.java:471-495 | 条件 | **isActive: value "key:value" 条件对 OR 语义 + realValue 空 → getAnyMethodParameter 方法参数兜底** | High |
| ExtensionDirector.java:28 | 作用域 | **ExtensionDirector: scoped extension loader manager + ScopeModel + destroyed** — 3.x 作用域管理 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: SPI 微内核是单机制四面 (加载/创建/自适应/激活) — 1篇按四面展开; 全框架扩展点都经此装配 (导航 D-2~D-7), URL 总线贯穿 (导航各域)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 3 加载目录三级优先级 (internal 覆盖) | 🔴 | **为什么🔴**: 扩展发现语义 |
| P1-2 | 单例 + setter DI + 自适应对注入 | 🔴 | **为什么🔴**: 装配核心 |
| P1-3 | Wrapper 链 (排序确定性 + AOP) | 🔴 | **为什么🔴**: 全框架横切面机制 |
| P1-4 | 自适应动态代码生成五步 | 🔴 | **为什么🔴**: URL 总线驱动核心 |
| P1-5 | 激活 group/条件对/四层权重 | 🔴 | **为什么🔴**: Filter 链装配机制 |
| P2-1 | ExtensionDirector/ScopeModel 3.x 作用域 | 🟡 | **为什么🟡**: 3.x 架构面 |
| P2-2 | Lifecycle init + @Deprecated 标记 + 失败缓存 | 🟡 | **为什么🟡**: 生命周期/容错面 |
| P3-1 | 注入器自举 (注入器本身自适应) | 🟢 | **为什么🟢**: 自举细节 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **加载面** (双检查/3 目录/懒加载) | 🔴 | 发现语义 |
| B | **创建面** (单例/DI/Wrapper/Lifecycle) | 🔴 | 装配核心 |
| C | **自适应面** (动态生成/URL 驱动) | 🔴 | 运行时选择 |
| D | **激活面** (group/条件/权重) | 🔴 | Filter 装配 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 加载面 | 接口+@SPI 双检查保证类型安全; 3 目录 internal 优先实现"内部扩展覆盖用户扩展"; 懒加载双检锁避免重复加载; @Deprecated 标记兼容提示 | ExtensionLoader.java:242,988-998,1139 |
| q2 | 创建面 | putIfAbsent 单例; setter DI 注入的是自适应实例 (依赖本身动态选择); Wrapper 排序永不为 0 保证 HashSet 集合安全, reverse 后构造器逐层包装 = 责任链 AOP; matches/mismatches 按扩展名精准包装 | ExtensionLoader.java:216-246,280+ |
| q3 | 自适应面 | 无 @Adaptive 注解类 → 运行时生成适配类代码并编译; 生成五步: URL 参数定位→null 检查→多键回退 (key1→key2)→extName 赋值→getExtension — URL 参数决定扩展选择 (URL 总线) | ExtensionLoader.java:610,1467; Adaptive.java:44-47 |
| q4 | 激活面 | group 筛选消费/服务端; ActivateComparator 四层权重 (before/after 双向→order→类名兜底) 保证确定性; isActive 条件对 OR 语义 (任一满足激活), realValue 空时方法参数兜底 | ExtensionLoader.java:344-440,471-495 |
| q5 | 3.x 作用域 | ExtensionDirector = scoped extension loader manager + ScopeModel + destroyed — 多应用/多模块扩展隔离 | ExtensionDirector.java:28 |

→ 引出 D-2~D-7: Protocol/Registry/Cluster/LoadBalance/Filter 全部经此装配 — URL 总线 (getParameter 选扩展) 贯穿全框架。
