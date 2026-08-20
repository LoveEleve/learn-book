# D-1 SPI 微内核 — 深审 REVIEW 记录 (六层深审 + 07 五维度)

## 六层深审 (Pass 1-3 全程)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 1 | **数字穷举** | 执行计划 "ExtensionLoader/@SPI/@Adaptive/@Activate/Wrapper—URL 总线" — 全实证; **3 加载目录** (internal/external/services) + **Wrapper 排序 + reverse** (L216-218) | 大纲 §1/§2 |
| 2 | **补充锚点** | **3.x 新架构**: ExtensionDirector/ExtensionInjector (ExtensionFactory 兼容面 L993-998) — 执行计划未提 | 大纲 §2 |
| 3 | **语义标注** | **injectExtension 自适应对注入**: setter 依赖注入的是 getAdaptiveExtension() (自适应实例) — 依赖本身也是动态选择 | 大纲 §2 |
| 4 | **补充锚点** | **AdaptiveClassCodeGenerator 动态生成**: 无 @Adaptive 注解类 → **运行时生成适配类代码 + 编译** (L1467) | 大纲 §3 |
| 5 | **补充锚点** | **@Activate value "key:value" 条件对** (L33-37) + order/before/after 三机制排序 | 大纲 §4 |
| 6 | 行号验证 | 全函数 ~30 锚点 + 跨文件 grep (ExtensionLoader 216-320,344-471,610-660,955-1139,1467 / Activate 45-93 / AdaptiveClassCodeGenerator) | 记录 |

## 07 五维度

### 维度1 功能正确性
- 加载面 (harness A)
- 创建面 (harness B)
- 自适应 (harness C)
- 激活 (harness D)

### 维度2 性能
- 懒加载
- 单例缓存
- 双检锁

### 维度3 内存
- cachedClasses/instances
- 异常缓存 (unacceptableExceptions)

### 维度4 一致性
- 接口 + @SPI 双检查
- Wrapper 排序确定性
- URL 驱动选择

### 维度5 负面空间 (已写入大纲 6 条)
- 不动态卸载/不跨 classloader 隔离/不注解扫描/不循环依赖处理/不编译缓存/不多实例隔离

## 结论
D-1 锚点 ~30 处验证, 8 闭环完成, harness 10/10 (A-D 4 面, 自抓 1 处), **数字穷举 1 + 语义标注 1 + 补充锚点 3**。待二次 REVIEW。

---

# 二次深度 REVIEW (2026-08-15, 07 五维度轮换 + 内容深度)

## R1-R5 五维度

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 7 | 通过项 | 前置 无 ✅; 引出 D-2~D-7 ✅; 对照 Java SPI/Spring ✅; 读者处境场景化 ✅; 锚点 ~30 ✅; 负面空间 6 条 ✅; 横切 (加载/创建/自适应/激活) 全覆盖 ✅ | 记录 |

## R6 内容深度: 反写测试 + 机制逐句对源码

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 8 | **反写测试发现** | 大纲 §2 未提 **initExtension (Lifecycle)**: createExtension 末步 Lifecycle initialize (L248) — 启动钩子面 | 大纲 §2 补注 |
| 9 | 通过项 | 其余 ~26 句机制描述逐句对源码一致 ✅ (加载/创建/自适应/激活) | 记录 |

## 二次 REVIEW 汇总
共 **1 处修复** (Lifecycle init #8), 反写测试结论: 大纲机制面完整可支撑写作。

---

# 三次深度 REVIEW (2026-08-15, 逐句对源码 + 推理验证)

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 契约完整 | 接口 + @SPI — 类型安全 ✅ | 通过 |
| V2 | 单例正确 | putIfAbsent — 无重复实例 ✅ | 通过 |
| V3 | 包装确定性 | 排序 + reverse — 顺序稳定 ✅ | 通过 |
| V4 | URL 驱动 | 参数选择 — 运行时切换 ✅ | 通过 |
| V5 | 激活可配 | group/条件/order — 装配灵活 ✅ | 通过 |
| V6 | 失败隔离 | 异常缓存 — 不重复尝试 ✅ | 通过 |
| V7 | 生命周期 | init — 启动钩子 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 10 | **补充锚点** | **废弃扩展标记**: @Deprecated 类 → cachedDeprecatedExtensions (loadResource) — 兼容提示面 | 大纲 §1 注 |

## 三次 REVIEW 汇总
推理验证 7 项全过 (V1-V7); 新发现 **1 处** (废弃标记), 修复。大纲现可支撑写作。

---

# 四次深度 REVIEW (2026-08-15, 用户要求深度 REVIEW — 逐锚点 re-grep + 边沿穷举)

> 动机: 对 outline 全部锚点重新 grep, 穷举五个存疑面 (Wrapper 匹配语义/注入绕过面/自适应 URL 键/激活默认组/策略优先级)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | Wrapper matches? | @Wrapper matches/mismatches 数组 (L230-235) — 按扩展名匹配包装 | 通过 (验证) |
| T2 | 注入绕过? | @DisableInject + ScopeModelAware + 原始类型跳过 (injectExtension) | 通过 (验证) |
| T3 | 自适应 URL 键? | @Adaptive 注解 value → URL 键名 (AdaptiveClassCodeGenerator) | 通过 (验证) |
| T4 | 激活默认组? | group 空 → 所有组? (cachedActivateGroups) | 通过 (验证) |
| T5 | 策略优先级? | LoadingStrategy SPI 顺序 — 目录覆盖语义 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 包装精准 | matches/mismatches — 按名控制 ✅ | 通过 |
| V2 | 注入安全 | 跳过面 — 无副作用 ✅ | 通过 |
| V3 | 键可配 | @Adaptive value — 灵活 ✅ | 通过 |
| V4 | 组语义 | group 筛选 — 装配面 ✅ | 通过 |
| V5 | 策略有序 | SPI 顺序 — 覆盖可控 ✅ | 通过 |

## 新发现问题 (1 处, 已修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 11 | **补充锚点** | **Wrapper matches/mismatches 按名匹配** (T1, L230-235): 包装器可选择性应用 — AOP 精准面 | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 加载面 (双检查/3 目录/默认名/废弃标记) — 可写 ✅
- §2 创建面 (单例/注入/Wrapper 匹配/Lifecycle) — 可写 ✅
- §3 自适应面 (URL 驱动/动态生成/键可配) — 可写 ✅
- §4 激活面 (group/条件对/三机制) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 四次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 1 处** (Wrapper matches)。核心认知: **SPI 微内核四面** (加载/创建/自适应/激活) + **URL 总线驱动** + **Wrapper AOP 按名匹配** + **3.x ExtensionDirector 新架构**。harness 10/10 全过 (自抓 1 处)。大纲经修复后反写测试全过。

---

# 五次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 策略/排序/生成/架构穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查五个存疑面 (LoadingStrategy 优先级/ActivateComparator 权重/代码生成逻辑/ExtensionDirector/双模式)。

## 追查过程 (五个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | LoadingStrategy 优先级? | **3 策略优先级**: DubboInternalLoadingStrategy → **MAX_PRIORITY** / DubboLoadingStrategy → **NORMAL_PRIORITY** / ServicesLoadingStrategy → **MIN_PRIORITY** — 内部优先覆盖 | 发现 12 (补锚) |
| T2 | ActivateComparator 权重? | support/ActivateComparator: **before/after 双向比较** → order 比较 → **order 相同时按类名兜底** (注释 L114-117) — 排序确定性 | 发现 13 (补锚) |
| T3 | 代码生成逻辑? | AdaptiveClassCodeGenerator: **getUrlTypeIndex** → URL null 检查 → **getMethodAdaptiveValue** → generateExtNameAssignment → **模板 L75: scopeModel.getExtensionLoader(...).getExtension(extName)** | 发现 14 (补锚) |
| T4 | ExtensionDirector? | **"scoped extension loader manager"** (L28): parent + **ExtensionScope** + **ScopeModel** + destroyed AtomicBoolean — **作用域管理** (3.x 架构核心) | 发现 15 (补锚) |
| T5 | 双模式? | **getExtension(name, wrap)** (L558): wrap 控制是否包装 — createExtension wrap=false 场景 | 通过 (验证) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 优先级覆盖 | MAX > NORMAL > MIN — 内部扩展优先 ✅ | 通过 |
| V2 | 排序确定 | before/after → order → 类名 — 无歧义 ✅ | 通过 |
| V3 | 生成正确 | URL 键 → extName → getExtension — 动态选择闭环 ✅ | 通过 |
| V4 | 作用域隔离 | ScopeModel + parent — 模块隔离 ✅ | 通过 |
| V5 | 包装可控 | wrap 参数 — 场景可选 ✅ | 通过 |

## 新发现问题 (4 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 12 | **补充锚点** | **LoadingStrategy 三级优先级** (T1): internal=MAX / dubbo=NORMAL / services=MIN — 覆盖语义 | 大纲 §1 注 |
| 13 | **补充锚点** | **ActivateComparator 四层权重** (T2): before/after → order → 类名兜底 — 确定性排序 | 大纲 §4 注 |
| 14 | **补充锚点** | **代码生成五步** (T3): URL 定位 → null 检查 → @Adaptive 键 → extName 赋值 → getExtension | 大纲 §3 注 |
| 15 | **补充锚点** | **ExtensionDirector 作用域** (T4): scoped loader manager + ScopeModel + destroyed | 大纲 §2 注 |

## 反写测试 (只读大纲能否写文章)

- §1 加载面 (双检查/3 目录优先级/默认名/废弃标记) — 可写 ✅
- §2 创建面 (单例/注入/Wrapper/Lifecycle/作用域) — 可写 ✅
- §3 自适应面 (URL 驱动/生成五步/键可配) — 可写 ✅
- §4 激活面 (group/条件对/四层权重) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 五次 REVIEW 汇总

五存疑面全实证 (T1-T5); 推理验证 5 项全过 (V1-V5); **新发现 4 处全部修复** (优先级/排序权重/生成五步/作用域)。核心认知: **3 目录三级优先级** (internal 覆盖) + **ActivateComparator 四层确定性排序** + **自适应代码生成五步** + **ExtensionDirector 作用域架构** (3.x)。大纲经修复后反写测试全过。

---

# 六次深度 REVIEW (2026-08-15, 用户要求按方法论深度 REVIEW — 逐锚点 re-grep + 比较器/注解/URL/条件穷举)

> 动机: 对 outline 全部锚点重新 grep; 追查六个存疑面 (WrapperComparator 排序安全/四注解参数全景/URL 结构/注入器路径/isActive 条件语义)。

## 追查过程 (六个存疑面全部实证)

| # | 存疑面 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| T1 | WrapperComparator? | support/WrapperComparator (L31-60): **parseOrder** + **"never return 0 even if n1 equals n2..."** (L54-55 注释) — 包装排序永不为 0 (集合安全) | 发现 16 (语义标注) |
| T2 | 四注解全景? | **@SPI: value + scope 参数** (SPI.java:61-64) / **@Adaptive: value 数组多键回退** (L44-47) / @Wrapper: matches/mismatches + **order** | 发现 17 (补锚) |
| T3 | URL 结构? | **protocol/host/port/path/parameters + URLParam.parse** (URL.java:162-199) + getParameter(key, default) + getMethodParameter 族 | 通过 (验证) |
| T4 | 注入器路径? | **injector = extensionDirector.getExtensionLoader(ExtensionInjector).getAdaptiveExtension()** (L219-221) — 注入器本身也是自适应扩展 | 通过 (验证) |
| T5 | isActive 条件语义? | **OR 语义** + **realValue 空 → getAnyMethodParameter 兜底** (L471-495) | 发现 18 (补锚) |

## 推理验证 (全部反推通过)

| # | 项 | 验证过程 | 结论 |
|:--:|:--|:--|:--:|
| V1 | 包装排序安全 | 永不为 0 — HashSet 不覆盖 ✅ | 通过 |
| V2 | scope 可配 | @SPI scope — 作用域灵活 ✅ | 通过 |
| V3 | 键回退 | value 数组依次尝试 — 兼容 ✅ | 通过 |
| V4 | 注入器自适应 | 注入器本身 SPI — 自举 ✅ | 通过 |
| V5 | 条件 OR | 任一满足激活 — 灵活面 ✅ | 通过 |

## 新发现问题 (3 处, 全部修复)

| # | 类型 | 问题 | 修复 |
|:--:|:--:|:--|:--|
| 16 | **语义标注** | **WrapperComparator 永不为 0** (T1): 排序确定性 + HashSet 集合安全 (注释锚 L54-55) | 大纲 §2 注 |
| 17 | **补充锚点** | **@SPI scope + @Adaptive 多键回退** (T2): 作用域参数 + key1→key2 依次尝试 | 大纲 §1/§3 注 |
| 18 | **补充锚点** | **isActive OR 语义 + getAnyMethodParameter 兜底** (T5): 任一条件对激活; URL 参数空时方法参数兜底 | 大纲 §4 注 |

## 反写测试 (只读大纲能否写文章)

- §1 加载面 (双检查/3 目录优先级/默认名/@SPI scope) — 可写 ✅
- §2 创建面 (单例/注入/Wrapper 排序安全/Lifecycle/作用域) — 可写 ✅
- §3 自适应面 (URL 驱动/生成五步/多键回退) — 可写 ✅
- §4 激活面 (group/条件对 OR/四层权重/方法参数兜底) — 可写 ✅
- 负面空间 6 条 — 完整 ✅

## 六次 REVIEW 汇总

六存疑面全实证 (T1-T6); 推理验证 5 项全过 (V1-V5); **新发现 3 处全部修复** (包装排序永不为 0/@SPI scope+多键回退/isActive OR+方法参数兜底)。核心认知: **包装排序确定性** (永不为 0 集合安全) + **@Adaptive 多键回退** (兼容面) + **激活条件 OR + 方法参数兜底**。大纲经修复后反写测试全过。
