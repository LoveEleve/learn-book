# 闭环笔记 q2: 创建面 — 单例 + DI + Wrapper 链

## 假设
扩展实例 = 单例缓存 + 依赖注入 + 包装器链 (AOP)。

## 验证过程
- **单例缓存** (L216-221): extensionInstances (CHM) + **putIfAbsent** — 同扩展全局单例
- **injectExtension** (L280+): **setter 扫描** → isSetter + **@DisableInject 跳过** + ScopeModelAware/ExtensionAccessorAware 跳过 + 原始类型跳过 → **getExtensionLoader(pt).getAdaptiveExtension() 注入** (自适应对依赖!)
- **Wrapper 链** (L226-246): cachedWrapperClasses → **WrapperComparator 排序 + reverse** → 逐包装: **@Wrapper matches/mismatches 匹配** → **构造器注入 (wrapperClass(type).newInstance(instance))** → 逐层 postProcess + inject — **包装器 = AOP** (ProtocolListenerWrapper/ProtocolFilterWrapper 等)
- **initExtension** (L248): Lifecycle initialize — 启动钩子
- **异常缓存** (L220): unacceptableExceptions — 已失败扩展名缓存 (不重复尝试)

## 代码类型
Implementation (创建面)

## 跨域关联
- D-4: ProtocolFilterWrapper (Filter 链包装, 消费面)
- 对照: Spring IoC — setter 注入 vs Dubbo injector 注入

## 结论
创建 = 单例缓存 + 自适应对依赖注入 + Wrapper 链 (排序/matches) + Lifecycle init + 异常缓存。
源码位置: ExtensionLoader.java:216-248,280-320
