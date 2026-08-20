# OF-7 配置隔离 — 子上下文与配置体系

> 前置: [[OF-1-注册机制]] [[OF-2-代理创建与装配]] | 引出: [[OF-9-动态刷新]] | 对照: SCC (C-13 NamedContextFactory) + Spring @Configuration
> 🔴 A | 8 KP | [模式: 命名子上下文 + 双级配置 + AOT]
> Pass 2 闭环: q1(子上下文) q2(默认组件) q3(配置面) q4(AOT/隔离语义)

**读者处境**: 每个 @FeignClient 的配置怎么隔离? 默认组件从哪来? Properties 与注解谁优先? 这篇拆 FeignClientFactory + FeignClientsConfiguration (260) + FeignClientProperties (397)。

### 1. 子上下文工厂面 — FeignClientFactory + NamedContextFactory

场景: 子上下文怎么建?
源码路径:
- **FeignClientFactory extends NamedContextFactory** (L39) + 构造 (配置类+属性前缀 L47-48)
- **3 实例获取**: WithoutAncestors ×2 + getBean (L52-67) — OF-2 继承开关消费
- **NamedContextFactory.getContext** (SCC L119): **惰性创建** + AOT 初始化器 + refresh; registerBeans (规范驱动); ⚠ **setConfigurations 按名注册** (L98-100) + **destroy → context.close()** (L109-114, DisposableBean)
关键设计 (q1): **每客户端独立上下文 + 惰性创建 + 规范注册 (OF-1 输入)**。[模式: 子上下文面]

### 2. 默认组件面 — FeignClientsConfiguration 14 @Bean

场景: 子上下文默认提供什么?
源码路径:
- **9 功能 Bean** (L104-174): decoder/encoder/contract/conversionService/retryer/loggerFactory...
- **3 Builder 变体** (L210-232): 普通/默认/**circuitBreaker (OF-6)** — prototype; ⚠ **circuitBreakerFeignBuilder 需 @ConditionalOnBean(CircuitBreakerFactory.class)** (L230-232)
- **2 Micrometer Capability** (L247-254) — 指标面并入本域; ⚠ **micrometer.enabled 默认开** (matchIfMissing=true L)
关键设计 (q2): **默认组件齐全 + Builder 三态 + 指标条件装配**。[模式: 组件面]

### 3. 配置面 — FeignClientProperties

场景: Properties 怎么配? 谁覆盖谁?
源码路径:
- **defaultConfig="default" + config Map** (L31-33) — 双级配置; ⚠ **decodeSlash=true (默认开) + removeTrailingSlash (默认 false)** (L62-67) — **OF-3 SpringMvcContract 构造参数来源!**
- **FeignClientConfiguration 8 字段** (L131-151): 超时/retryer/errorDecoder/拦截器/dismiss404
- ⚠ **isDefaultToProperties 覆盖顺序开关** (L174-188): true (⚠ **默认 true** L52) → Properties 覆盖注解组件 / false → 反之
- **inheritParentContext 从 FeignClientConfigurer 读** (L172-173) + 应用清单 (L260-300: options 仅非刷新时)
关键设计 (q3): **双级配置 + 覆盖顺序可配 + 继承开关联动 (OF-2/OF-9)**。[模式: 配置面]

### 4. AOT 与隔离语义面 — 双处理器 + Configurer

场景: 子上下文怎么支持 native? 开关从哪来?
源码路径:
- **AOT 双处理器**: FeignClientBeanFactoryInitializationAotProcessor (L68-102: 子上下文注册) + **FeignChildContextInitializer** (L51: **ApplicationContextAotGenerator 生成初始化器类** L107 + per-contextId Map L118-122)
- **FeignClientConfigurer**: primary() 默认 true + **inheritParentConfiguration() 默认 true** — OF-2 继承开关来源链
关键设计 (q4): **AOT 编译期生成 + 隔离语义开关一链贯通**。[模式: AOT 面]

## 代码类型
Architecture (上下文隔离) + Spring 容器机制

## 负面空间 (OF-7, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不子上下文复用 | 每客户端独立 (q1) |
| 不组件自动发现 | 显式 @Bean (q2) |
| 不配置热更新 | 启动读取 (q3) |
| 不 Properties 校验 | 运行时暴露 (q3) |
| 不 AOT 全量支持 | 核心面生成 (q4) |
| 不 native 反射全量注册 | 按需 (q4) |

## 结尾桥 OUTBOUND

- → [[OF-9-动态刷新]]: refreshableClient 联动 (options 条件)
- → 对照: SCC C-13 NamedContextFactory (scc13-named-context) / Spring @Configuration
