# D-1 SPI 微内核 — Pass 1 探索笔记

> 域: D-1 SPI 微内核 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: dubbo-common/extension/ (ExtensionLoader 1522 + @SPI + @Adaptive + @Activate + Wrapper + AdaptiveClassCodeGenerator) | Dubbo 3.3.7-SNAPSHOT

## 调用图

```
getExtensionLoader(type) (L242) → 缓存 loader (TYPE 检查: 接口 + @SPI)
getExtension(name) → getExtension(name, true):
  createExtension (L216-):
    getExtensionClasses (L955-994): cacheDefaultExtensionName (@SPI 默认)
      → 多 LoadingStrategy 加载 (META-INF/dubbo/internal|external|services)
    extensionInstances 单例缓存 (CHM + putIfAbsent)
    → createExtensionInstance → injectExtension (setter 依赖注入) → Wrapper 链 (排序+matches/mismatches) → initExtension
getAdaptiveExtension (L610-): 双检锁 → createAdaptiveExtension
  → getAdaptiveExtensionClass: cachedAdaptiveClass (@Adaptive 注解) / 无 → AdaptiveClassCodeGenerator 动态生成 + 编译 (L1467)
getActivateExtension(url, group) (L344-440): @Activate group/value/order/before/after → TreeMap 排序 + isActive 条件
```

## 基本元素分解

1. **加载面**: @SPI + LoadingStrategy 目录扫描 + cacheDefaultExtensionName
2. **创建面**: 单例缓存 + injectExtension (DI) + Wrapper 链 (AOP) + initExtension
3. **自适应面**: @Adaptive 注解类 / AdaptiveClassCodeGenerator 动态生成
4. **激活面**: @Activate (group/value/order/before/after)
5. **URL 总线**: URL 作为方法参数传递扩展选择

## 标记问题 (20 问)

1. getExtensionLoader? (接口 + @SPI 检查)
2. @SPI 默认名? (cacheDefaultExtensionName)
3. LoadingStrategy? (3 目录)
4. 单例缓存? (CHM)
5. injectExtension? (setter DI + DisableInject)
6. Wrapper 链? (排序 + matches/mismatches)
7. @Adaptive? (注解类 vs 动态生成)
8. AdaptiveClassCodeGenerator? (代码生成 + 编译)
9. @Activate? (group/value/order/before/after)
10. URL 总线? (URL 参数选扩展)
11. 异常缓存? (unacceptableExceptions)
12. initExtension? (Lifecycle)
13. 双检锁? (cachedAdaptiveInstance)
14. 对照 Java SPI? (增强面)
15. WrapperComparator? (包装排序)
16. 注入面? (ExtensionInjector 自适应)
17. 懒加载? (首次访问加载)
18. 多策略? (LoadingStrategy SPI)
19. 对照 Spring? (Bean 注入对照)
20. 销毁? (checkDestroyed)

## 时空溯源 (代码内注释锚)

- ExtensionLoader:346 "solve the bug of using @SPI's wrapper method to report a null pointer exception" (历史修复注释)
- AdaptiveClassCodeGenerator (L1467): 动态生成 (1.x 锚)
- injectExtension: DisableInject 注释 (2.x 锚)
- ExtensionInjector/ExtensionDirector (3.x 新架构锚)

## 大域拆分判断

D-1 = SPI 微内核 (加载/创建/自适应/激活 4 面); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "ExtensionLoader/@SPI/@Adaptive/@Activate/Wrapper—URL 总线" | 全实证 (ExtensionLoader 1522 + 4 注解 + AdaptiveClassCodeGenerator) | **接受** ✅ |
| 数字: LoadingStrategy | 3 目录 (internal/external/services) | **补充** ✅ |
| 数字: Wrapper 排序 | WrapperComparator + reverse (L216-218) | **补充** ✅ |
| 3.x 新架构 | ExtensionDirector/ExtensionInjector — 执行计划未提 | **补充** ✅ |
