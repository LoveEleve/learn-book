# D-1 SPI 微内核 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: ExtensionLoader + @SPI/@Adaptive/@Activate + Wrapper + AdaptiveClassCodeGenerator; injectExtension (setter DI) |
| 2.7.x | LoadingStrategy SPI (internal/external 目录); @Activate 扩展 (group/value/order) |
| 3.x | **ExtensionDirector/ExtensionInjector 新架构** (ExtensionFactory 兼容面 L993-998); ScopeModel (scope 隔离); DisableInject 注解; 历史 NPE 修复注释 (L346) |
| 3.3.x | ExtensionLoader 1522 行稳定 (策略目录 3 个) |

## 痕迹证据

- ExtensionLoader.java:346: "solve the bug of using @SPI's wrapper method to report a null pointer exception" (历史修复锚)
- ExtensionLoader.java:993-998: ExtensionFactory → ExtensionInjector 兼容 (3.x 锚)
- injectExtension: DisableInject 注释 (3.x 锚)
- AdaptiveClassCodeGenerator.java: 动态生成 (2.x 锚)

## 推断标注

- "2.x 骨架" — Dubbo 2.x 公知版本线 (标注)
- "3.x 新架构" — ExtensionDirector 存在性实证 (实证)
- "3.3.x 稳定" — 行数实证 (实证)
- git 多 commit 可考古 (c91027d) — 本域以注释锚 + 类存在性为主

## 对照线 (阶段 3/4 已交付)

- Java SPI: META-INF/services vs Dubbo 3 目录 + name 键 + 默认名
- Spring IoC: setter 注入 vs Dubbo injector; @Conditional vs @Activate
- Seata (4.4): EnhancedServiceLoader — 同为 SPI 但无 @Adaptive/动态生成 (规模对照)
