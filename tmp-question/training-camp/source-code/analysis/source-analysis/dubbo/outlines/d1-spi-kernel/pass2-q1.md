# 闭环笔记 q1: 加载面 — @SPI + LoadingStrategy

## 假设
扩展加载 = 接口 @SPI 检查 + 目录扫描 + 默认名缓存。

## 验证过程
- **getExtensionLoader** (L242): type 必须是**接口** + 必须有 **@SPI 注解** — 否则 IllegalArgumentException ("Extension type must be an interface" / "must be annotated with @SPI")
- **cacheDefaultExtensionName** (loadExtensionClasses 首步): @SPI value() → cachedDefaultName — 无 name 的 getExtension 用默认
- **多 LoadingStrategy** (L988-998): **strategies 遍历 → loadDirectory** — LoadingStrategy SPI (dubbo-internal/dubbo-external/dubbo) — **3 目录**: META-INF/dubbo/internal/ + META-INF/dubbo/ + META-INF/services/
- **loadResource** (L1139): 逐行解析 "name=类名" + **废弃类检查** (@Deprecated → cachedDeprecatedExtensions)
- **懒加载**: 首次访问 getExtensionClasses 才加载 (cachedClasses + 双检锁)
- **兼容面**: ExtensionFactory (旧名) → ExtensionInjector (新架构) (L993-998)

## 代码类型
Implementation (加载面)

## 跨域关联
- D-2~D-7: 全部扩展经本域加载 (微内核基础)
- 对照: Java SPI (META-INF/services) — Dubbo 增强 (name 键 + 默认名)

## 结论
加载 = @SPI 接口检查 + 3 目录策略扫描 + name=类 解析 + 默认名缓存 + 懒加载。
源码位置: ExtensionLoader.java:242,955-994,1005-1139
