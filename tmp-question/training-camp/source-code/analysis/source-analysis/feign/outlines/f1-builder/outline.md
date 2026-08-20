# F-1 Builder 装配 — 一切的起点: CRTP、clone 与 Capability 的魔法

> 前置: [[F-6-模板引擎]] + [[F-2-契约解析]] + [[F-4-编解码]] + [[F-5-拦截器链]] + [[F-3-代理与调用链]] (全组件) | 引出: 阶段5.6 OpenFeign (Spring FactoryBean 消费) | 对照: Spring Boot 自动装配 + Retrofit.Builder
> 🔴 A | 5 KP | [模式: CRTP Builder + 装饰器流水线]
> Pass 2 闭环: q1(CRTP) q2(enrich) q3(装配) q4(Capability)

**读者处境**: `Feign.builder().client(x).encoder(y)...build()` 一行链式调用 — 为什么每个方法都返回 Builder 而不是 Feign? build() 前发生了什么魔法 (clone + Capability)? 所有组件在哪一刻被拼成 ReflectiveFeign?

### 1. CRTP — 链式调用的类型安全

场景: 为什么 Feign.Builder 的 client() 返回 Builder 而不是 BaseBuilder?
源码路径:
- **class BaseBuilder<B extends BaseBuilder<B, T>, T>** (BaseBuilder.java:41) — CRTP 泛型自引用
- Feign.Builder extends BaseBuilder<Builder, Feign> (Feign.java:97)
- 每个配置方法返回 B (自身类型) — 链式不丢类型
关键设计 (q1): **CRTP 让"继承的配置方法"返回子类类型** — 若返回 BaseBuilder, `builder.client().encoder()` 会断链 (encoder 在子类视角丢失); 泛型自引用是 Java 的"自我类型"模拟。 [模式: CRTP]

### 2. 十八个默认值 — 开箱即用的秘密

场景: 什么都不配, Feign 用什么默认组件?
源码路径:
- **18 个 protected 字段** (BaseBuilder.java:43-62): contract=DefaultContract/retryer=DefaultRetryer/encoder=DefaultEncoder/decoder=DefaultDecoder/logger=NoOp/logLevel=NONE/closeAfterDecode=true/queryMapEncoder=FIELD/errorDecoder=DefaultErrorDecoder/propagationPolicy=NONE/三拦截器空列表/capabilities 空
关键设计 (q1): **每个接口都有可用默认** — 零配置可用; 默认全是"安全保守"取向 (NoOp 日志/Default 编解码)。 [模式: 默认值完备]

### 3. enrich — build 前的装饰流水线

场景: build() 做了什么? 为什么需要 clone?
源码路径:
- **enrich()** (BaseBuilder.java:265-389): **clone 副本** (L271) → 逐字段 Capability.enrich (L288-354: 三拦截器列表逐个 + 单组件) → 返回装饰后副本
- **build() final** (L385-386): enrich().internalBuild()
- Capability.enrich 静态 (Capability.java:38-56): **reduce 流水线** — cap1 的结果喂给 cap2
关键设计 (q2): **clone 防污染原 Builder** — 装饰在副本上进行, 原 Builder 可复用; Capability 流水线让多个能力叠加 (监控+缓存+日志)。 [模式: 不可变构建]

### 4. internalBuild — 全组件的汇聚点

场景: 所有组件在哪一刻被拼起来?
源码路径:
- Feign.internalBuild (Feign.java:217-242): **ResponseHandler 组装** (L218-227: decoder/errorDecoder/dismiss404/closeAfterDecode/decodeVoid/responseInterceptorChain) → **SynchronousMethodHandler.Factory** (L228-239: client/retryer/三拦截器/responseHandler/logger/options/RequestTemplateFactoryResolver) → **new ReflectiveFeign** (L240-241)
- RequestTemplateFactoryResolver (40-48): formParams/bodyIndex/alwaysEncodeBody 三选一模板工厂
- 异步变体: AsyncBuilder.internalBuild 覆写此方法产出 AsyncFeign (F-3 M5)
关键设计 (q3): **装配只有两个新对象** — ResponseHandler (响应侧) + MethodHandler.Factory (执行侧), 其余全部是用户配置的直接引用; ReflectiveFeign 是装配终点; internalBuild 是同步/异步的分叉点。 [模式: 组装工厂]

### 5. configKey — 契约-执行-错误三层的共享主键

场景: 日志里的 "GitHub#getUser(String)" 是什么? 为什么格式这么固定?
源码路径:
- **configKey 格式** (Feign.java:69-84): `SimpleName#method(ParamType1,ParamType2)` — `#` 分隔 L73 + Types.resolve 泛型解析 + getRawType().getSimpleName (L75); 无参数方法去尾逗号 deleteCharAt (L78); 结尾 ')' L80
- 生成时机: F-2 BaseContract 初始化时写入 MethodMetadata.configKey (Contract.java:96-98)
- **消费方**: F-3 重试日志 logRetry (SynchronousMethodHandler.java:94-98) / F-4 ErrorDecoder.methodKey (DefaultErrorDecoder L18) / AnnotationErrorDecoder 的 methodKey→handler 表 (F-4 M6)
关键设计 (q4): **configKey 是三层共享的稳定标识** — 契约解析写入、执行层日志、错误层映射全用它关联; 格式必须稳定 (ErrorDecoder Javadoc 显式依赖); 无空格无括号歧义的设计 (Javadoc 注释 L48-63)。 [模式: 全局标识符]

### 6. Capability — 13.x 的横切注入点

场景: 想给所有请求加监控, 改哪个组件? 要改 Builder 源码吗?
源码路径:
- **Capability 全套 default enrich** (Capability.java:76-154): Client/Retryer/Contract/Encoder/Decoder/ErrorDecoder/InvocationHandlerFactory/MethodInfoResolver/三拦截器族
- 实现类只覆写关心的组件 (Metrics5Capability 包装 Client/Decoder 实证); 反射 invoke (L58-74) 支持任意 enrich(X)
- addCapability 注册 (Feign.java:204-206)
- **enrich() 细节** (BaseBuilder.java:265-389): **capabilities 空 → 短路返回 thisB() 不克隆** (L266-268); 克隆 (L271) → **List 字段按泛型 ownerType 逐元素增强** (L288-354) / 非 List 整体增强; **responseInterceptors 空时构造默认拦截器再增强** (L303-310, 让 Capability 可给"不存在的拦截器"注入)
- getFieldsToEnrich 白名单 (L366-383): **8 个排除条件** — synthetic / capabilities / 三拦截器列表 (逐元素+整体增强故整体排除) / executorService (调用方生命周期资源) / primitive / **enum (logLevel+propagationPolicy 两枚举字段 L46/L60)**
关键设计 (q4): **装饰器注入点而非组件替换** — Capability 包住原组件加横切 (计数/耗时/日志); 不用改 Builder 源码就能横切任意组件; 克隆装饰保证原 Builder 可复用。 [模式: 装饰器]

## 代码类型
Architecture (装配层)

## 负面空间 — Feign Builder 刻意不做的事

- **不做运行时修改**: build 后不可变 (clone 副本装饰)
- **不做全局默认**: 每 Builder 独立 (对照 Spring 全局 Bean)
- **不做组装期校验**: 组件组合合法性运行时才暴露
- **不做 Builder 线程安全**: 构建期单线程
- **不做组件自动发现**: 显式装配 (Capability 例外)

→ 引出: 阶段5.6 OpenFeign 怎么用 FactoryBean 包住这个 Builder?
