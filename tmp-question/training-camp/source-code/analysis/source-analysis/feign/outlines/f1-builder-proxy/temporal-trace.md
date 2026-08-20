# F-1 Builder 门面 + 动态代理 — 时空溯源 (CHANGELOG 实证, git shallow)
> **ℹ 8-16 增强版 (备查)**: 本文件为 F-1 的 8-16 深化版, 独有内容 (configKey 规则/verify 四检查/equals 语义/MethodHandle 三路 Lookup/代理三分派) 已并入权威版 f1-builder (§5/§6) 与 f3-proxy (§1)。保留作增强参考, 非权威。

> git shallow (1 commit) 无法 checkout 早期版本; 溯源以 CHANGELOG.md + 代码内注释为锚 (方法论铁律 4)。

## 版本演进链 (CHANGELOG.md 实证)

| 版本 | 事件 | 证据 |
|:--:|:--|:--|
| 10.0 | "Removed @Deprecated methods marked for removal on feign 10" — Builder 面大清理 | CHANGELOG.md:29 |
| 10.4 | "Adding support for JDK Proxy (#1045)" — 代理面增强 | CHANGELOG.md:17 |
| 10.5 | "Declarative contracts (#1060)" — Contract 重构间接影响 Builder (contract 字段类型) | CHANGELOG.md:21 |
| 10.8 | "async feign variant (#1174)" — **AsyncBuilder 复用 BaseBuilder** 的前提 (CRTP 泛型化) | CHANGELOG.md:10 |

## 代码内历史锚

- **BaseBuilder CRTP 泛型** (BaseBuilder.java:41): `<B extends BaseBuilder<B, T>, T>` — 10.8 异步引入时从 `Feign.Builder` 单类抽取为共享基类 (AsyncBuilder 同继承)
- **@Deprecated 内部类**: InvocationHandlerFactory.Default → DefaultInvocationHandlerFactory (28 行独立类); Client.Default → DefaultClient; Encoder.Default → DefaultEncoder — 13.x "抽取独立类"模式
- **DefaultMethodHandler 历史注释** (DefaultMethodHandler.java:28-37): "Uses Java 7 MethodHandle... When Feign upgrades to Java 7, remove the @IgnoreJRERequirement annotation" — **注释本身已过时** (Java 8+ 多年), 但 readLookup 三路 (safeReadLookup/androidLookup/legacyReadLookup L54-62) 是 JDK 9 模块封装的演进痕迹
- **newInstance Javadoc** (Feign.java:88-89): "You should cache this result" — 反射开销的显式提示 (长期不变)

## 版本相关性结论

- **Builder 面分水岭 = 10.8**: 异步客户端 (#1174) 迫使 Builder 泛型化 (CRTP BaseBuilder) — 同步/异步共享装配逻辑, 差异只在 internalBuild
- **代理面 10.4 增强**: JDK Proxy 支持 (#1045) 与 default 方法 MethodHandle 直调并行演化 — "注解方法走 dispatch + default 方法走 MethodHandle" 双路径定型
- **13.x 抽取模式**: 所有内部 @Deprecated Default 类 → 独立类 (DefaultClient/DefaultEncoder/DefaultInvocationHandlerFactory) — 稳定面持续重构
- **FeignInvocationHandler 三分派自远古定型**: equals/hashCode/toString 不触发 HTTP 的设计从 1.x 至今未变 (面试高频常识的源码锚)
