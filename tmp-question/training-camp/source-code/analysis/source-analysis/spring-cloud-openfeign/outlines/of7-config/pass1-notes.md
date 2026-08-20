# OF-7 配置隔离 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (FeignClientFactory / FeignClientsConfiguration 260 / FeignClientProperties 397 / AOT 2)
> 09 域级审计: OPENFEIGN-PLAN OF-7 (合并 F-5 + 指标并入) — 断言 "NamedContextFactory 子上下文 + 14 @Bean + Properties" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: FeignClientFactory — NamedContextFactory 子类 (L39-62)

```
public class FeignClientFactory extends NamedContextFactory<FeignClientSpecification> (L39):
├── 2 构造器 (L41-48): 无参 → (initializers Map)
├── super(FeignClientsConfiguration.class, "spring.cloud.openfeign",
│   "spring.cloud.openfeign.client.name", initializers) (L47-48)
│   ← 配置类 + 属性前缀 + 上下文名属性
└── 3 个实例获取方法:
    ├── getInstanceWithoutAncestors(name, type) (L52-58): BeanFactoryUtils.beanOfType(getContext(name))
    ├── getInstancesWithoutAncestors(name, type) (L61-63): getBeansOfType
    └── getInstance(contextName, beanName, type) (L65-67): getBean (按名)
    ← OF-2 inheritParentContext 开关消费 (false → WithoutAncestors)
```

### Level-2: NamedContextFactory 基类 (SCC spring-cloud-context L60)

```
abstract class NamedContextFactory<C extends Specification> (L60):
├── **getContext(name)** (L119): 惰性创建子上下文 (GenericApplicationContext)
│   ├── registerConfigurations (配置注册)
│   └── **applicationContextInitializers** (L132-134: AOT 初始化器)
├── getContextNames (L104) / getInstances (L253) / registerConfiguration
└── Specification 族 (OF-1 注册的 FeignClientSpecification)
→ SCC C-13 NamedContextFactory 域 (scc13-named-context 已规划)
```

### Level-3: FeignClientProperties (L30+)

```
public class FeignClientProperties:
├── **defaultConfig = "default"** (L31) — 默认配置键
├── **config = Map<String, FeignClientConfiguration>** (L33) — 每客户端配置
└── FeignClientConfiguration (L131-151) — 字段:
    loggerLevel / connectTimeout / readTimeout / **retryer (Class<Retryer>)** /
    errorDecoder / **requestInterceptors (List<Class>)** / defaultRequestHeaders /
    dismiss404
    ← OF-2 configureUsingProperties 消费 (defaultConfig + contextId 合并)
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-7 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "FeignClientFactory extends NamedContextFactory" | L39 | 接受 |
| "每 @FeignClient 独立子上下文" | NamedContextFactory.getContext (L119 惰性创建) | 接受 |
| "14 @Bean 默认" | FeignClientsConfiguration (OF-2 穷举) | 接受 |
| "FeignClientProperties 配置" | defaultConfig + config Map + 8 字段 | 接受 |
| 补锚: 3 个实例获取方法 | WithoutAncestors ×2 + getBean (L52-67) | 补锚 (OF-2 继承开关消费) |
| 补锚: 属性前缀 | "spring.cloud.openfeign.client.name" (L47-48) | 补锚 |
| 补锚: AOT 初始化器 | applicationContextInitializers (NamedContextFactory L132-134) | 补锚 |

## 待展开 (下一层)

1. NamedContextFactory.getContext 完整 (惰性创建/配置注册流程)
2. FeignClientProperties 配置合并逻辑 (defaultConfig + contextId 覆盖优先级)
3. AOT 处理器 (FeignClientBeanFactoryInitializationAotProcessor 236)
4. 指标条件装配 (Micrometer 2 Bean — OF-2 已穷举, 本域归属)
5. FeignChildContextInitializer (AOT 子上下文)
