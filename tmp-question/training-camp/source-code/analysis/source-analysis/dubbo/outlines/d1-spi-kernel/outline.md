# D-1 SPI 微内核 — 加载/创建/自适应/激活

> 前置: 无 (一切基础) | 引出: [[D-2-服务导出]] ... [[D-7-集群容错]] (全框架) | 对照: Java SPI + Spring IoC
> 🔴 A | 8 KP | [模式: SPI 微内核 + URL 总线]
> Pass 2 闭环: q1(加载面) q2(创建面) q3(自适应面) q4(激活面)

**读者处境**: Dubbo 所有扩展点怎么被加载/装配/选择? URL 总线是什么? 这篇拆 ExtensionLoader (1522) + 4 注解 + 动态代码生成。

### 1. 加载面 — @SPI + LoadingStrategy

场景: 扩展怎么被发现?
源码路径:
- **getExtensionLoader** (L242): **接口 + @SPI 双检查** — 否则 IllegalArgumentException; ⚠ **@SPI scope 参数** (SPI.java:61-64, 默认 application 作用域)
- **cacheDefaultExtensionName**: @SPI value() → 默认名 (无 name 时用)
- **多 LoadingStrategy** (L988-998): **3 目录三级优先级** — internal=**MAX** / dubbo=**NORMAL** / services=**MIN** (内部优先覆盖); 3.x **ExtensionDirector 作用域** (ScopeModel + destroyed)
- **loadResource** (L1139): "name=类名" 逐行解析 + @Deprecated 标记
- **懒加载**: 首次访问加载 (cachedClasses 双检锁)
关键设计 (q1): **@SPI 接口契约 + 3 目录策略扫描 + 默认名**。[模式: 加载面]

### 2. 创建面 — 单例 + DI + Wrapper 链

场景: 扩展实例怎么装配?
源码路径:
- **单例缓存** (L216-221): extensionInstances CHM + putIfAbsent
- **injectExtension** (L280+): **setter 扫描 + @DisableInject 跳过 → 自适应对注入** (getAdaptiveExtension)
- **Wrapper 链** (L226-246): **WrapperComparator 排序 (⚠ 永不为 0 — HashSet 集合安全注释锚 L54-55) + reverse + @Wrapper matches/mismatches/order** → **构造器注入逐层包装** — **包装器 = AOP**
- **initExtension**: Lifecycle initialize; **unacceptableExceptions**: 失败扩展缓存
关键设计 (q2): **单例 + setter DI + 包装器链 (AOP)**。[模式: 创建面]

### 3. 自适应面 — @Adaptive + 动态生成

场景: 运行时怎么选扩展?
源码路径:
- **getAdaptiveExtension** (L610-): 双检锁 + 错误缓存
- **getAdaptiveExtensionClass**: **@Adaptive 注解类优先** (手工) / 无 → **AdaptiveClassCodeGenerator 动态生成 + 编译** (L1467); ⚠ **生成五步**: URL 参数定位 → null 检查 → @Adaptive value 键名 (**多键回退 key1→key2**, Adaptive.java:44-47) → **extName = url.getParameter(键)** → getExtensionLoader(...).getExtension(extName) (模板 L75)
- **URL 总线**: 自适应方法含 URL 参数 — **URL 参数选扩展** (getExtension(url.getParameter(key)))
关键设计 (q3): **URL 驱动选择 + 动态代码生成**。[模式: 自适应面]

### 4. 激活面 — @Activate 条件激活

场景: 过滤链怎么装配?
源码路径:
- **getActivateExtension(url, group)** (L344-440): group 筛选 + **ActivateComparator 四层权重** (before/after 双向 → order → **类名兜底** 注释锚 L114-117) + isActive
- **@Activate**: **group / value ("key:value" 条件对) / order / before / after** — 排序三机制
- **isActive** (L471-495): value 条件对 → URL 参数匹配 — **OR 语义 (任一满足即激活) + realValue 空 → getAnyMethodParameter 方法参数兜底**
- **历史修复注释** (L346): wrapper NPE bug
关键设计 (q4): **group + 条件对 + 三机制排序** — Filter 链装配机制。[模式: 激活面]

## 代码类型
Architecture (微内核)

## 负面空间 — SPI 微内核刻意不做的事

- **不做动态卸载**: 扩展加载后常驻 (无热卸载)
- **不做跨 classloader 隔离**: 单 loader 命名空间 (模块隔离靠 scope)
- **不做注解扫描**: 仅配置文件声明 (无 @Component 式自动注册)
- **不做循环依赖处理**: 注入循环 → 构造失败 (对照 Spring 三级缓存)
- **不做编译缓存**: 自适应类每次生成编译 (首次后缓存)
- **不做多实例隔离**: 默认全局单例 (scope 面除外)

→ 引出: 服务导出 → [[D-2-服务导出]]
