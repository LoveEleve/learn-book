# F-1 Builder 装配 — 知识规划 (KP)

> 域级: 🔴 A (收束 Hub: 依赖全部组件) | 模块: Feign.java (260) + BaseBuilder (402) + Capability (154) + RequestTemplateFactoryResolver
> 日期: 2026-08-15 | 版本: 13.14-SNAPSHOT

## 一、机制提取 (逐源)

### M1 CRTP 泛型自引用 Builder (BaseBuilder 402)
- **class BaseBuilder<B extends BaseBuilder<B, T>, T>** (L41): CRTP — 子类链式调用返回自身类型
- Feign.Builder extends BaseBuilder<Builder, Feign> (Feign.java:97)
- **18 个 protected 配置字段** (BaseBuilder.java:43-62): requestInterceptors/responseInterceptors/methodInterceptors (三拦截器列表) + logLevel (默认 NONE L46)/contract (默认 DefaultContract L47)/retryer (默认 DefaultRetryer L48)/logger (NoOp L49)/encoder (DefaultEncoder L50)/decoder (DefaultDecoder L51)/closeAfterDecode (true L52)/decodeVoid (false L53)/queryMapEncoder (FIELD L54)/errorDecoder (DefaultErrorDecoder L55)/options (L56)/invocationHandlerFactory (L57-58)/dismiss404 (L59)/propagationPolicy (NONE L60)/capabilities (L61)

### M2 enrich — Capability 装饰流水线 (L265-389)
- enrich() (L265): **clone 副本** (L271) → 对每字段 Capability.enrich (L288-354: 三拦截器列表逐个 + 单组件) → 返回装饰后 Builder
- **build() final** (L385-386): enrich().internalBuild(); internalBuild 抽象 (L389)
- Capability.enrich 静态 (Capability.java:38-56): **reduce 流水线** — cap1 结果喂 cap2; invoke 反射 (L58-74) 找 "enrich" 方法

### M3 internalBuild 装配 (Feign.java:217-242)
- ResponseHandler 组装 (L218-227): logLevel/logger/decoder/errorDecoder/dismiss404/closeAfterDecode/decodeVoid/responseInterceptorChain
- **MethodHandler.Factory = SynchronousMethodHandler.Factory** (L228-239): client/retryer/三拦截器/responseHandler/logger/options/RequestTemplateFactoryResolver(encoder, queryMapEncoder)
- new ReflectiveFeign (L240-241)
- RequestTemplateFactoryResolver (40-48): formParams/bodyIndex/alwaysEncodeBody 三选一工厂

### M4 Capability 能力扩展 (154)
- 全套 default enrich: Client/Retryer/Contract/Encoder/Decoder/ErrorDecoder/InvocationHandlerFactory/MethodInfoResolver/三拦截器族 (L76-154)
- **装饰器注入点**: 实现类只覆写关心的组件, 把原组件包成带横切能力的版本 (Metrics5Capability 包装 Client/Decoder 实证)

### M5 目标方法族
- target(Class, url) (Feign.java:208-210) → HardCodedTarget; target(Target) (L212-214): build().newInstance
- configKey 生成 (L69-81): 类名#方法(参数类型,)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M2 enrich 流水线 | P1 | 13.x 装配核心 |
| M3 internalBuild | P1 | 全组件汇聚点 |
| M1 CRTP | P1 | 类型安全链式 |
| M4 Capability | P2 | 扩展面 |
| M5 target | P2 | 入口 |

## 三、负面空间

- **不做运行时 Builder 修改**: build 后不可变 (clone 副本装饰)
- **不做全局默认配置**: 每个 Builder 独立
- **不做配置校验**: 组装期不校验组件组合合法性 (运行时才暴露)
- **不做 Builder 线程安全**: 构建期单线程假设
- **不做组件自动发现**: 全部显式装配 (Capability 例外)
