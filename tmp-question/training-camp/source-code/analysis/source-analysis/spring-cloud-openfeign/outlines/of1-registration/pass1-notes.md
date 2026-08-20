# OF-1 注册机制 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (FeignClientsRegistrar 503 / EnableFeignClients / FeignClient / FeignClientSpecification)
> 09 域级审计: OPENFEIGN-PLAN OF-1 (新增域, issue F-1 补) — 断言 "FeignClientsRegistrar 503 行 + ClassPathScanning 扫描" 已 grep 验证

## 入口展开 (Level-1~2, 已读源码)

### Level-1: @EnableFeignClients 注解属性

```
@EnableFeignClients (EnableFeignClients.java):
├── value()/basePackages() (L50/61) — 包扫描
├── basePackageClasses() (L71) — 类所在包
├── defaultConfiguration() (L81) — 默认配置类
└── clients() (L88) — 显式指定客户端接口 (跳过扫描!)
```

### Level-2: FeignClientsRegistrar.registerBeanDefinitions (L152-155)

```
implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware (L71)
registerBeanDefinitions (L152-155):
├── registerDefaultConfiguration (L157-169):
│   └── defaultConfiguration 属性 → registerClientConfiguration(registry, "default.类名", "default", 配置类)
│       ← 注册全局默认配置 BeanDefinition (FeignClientSpecification)
└── registerFeignClients (L172+):
    ├── clients 属性非空 → 直接用 (跳过扫描)
    ├── 否则扫描: getScanner() + AnnotationTypeFilter(FeignClient.class)
    │   + getBasePackages(metadata) (L393+):
    │   ├── value → basePackages → basePackageClasses 三级
    │   └── 全空 → 兜底: ClassUtils.getPackageName(importingClass)
    │   + scanner.findCandidateComponents(basePackage) (L181)
    ├── 每候选: 懒/急注册分流 (L210-215):
    │   └── spring.cloud.openfeign.lazy-attributes-resolution (默认 false)
    │       ├── false → eagerlyRegisterFeignClientBeanDefinition:
    │       │   validate(attributes) (L319-324: fallback/fallbackFactory 校验)
    │       │   + BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class)
    │       │   + 属性: url/path/name/contextId/type/dismiss404/fallback/fallbackFactory...
    │       └── true → lazilyRegisterFeignClientBeanDefinition (延迟属性解析)
    └── registerClientConfiguration (L202): 每客户端独立配置 BeanDefinition (FeignClientSpecification)
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-1 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "FeignClientsRegistrar 503 行" | wc -l = 503 | 接受 |
| "@EnableFeignClients 触发 Registrar" | ImportBeanDefinitionRegistrar (L71) + registerBeanDefinitions (L152) | 接受 |
| "ClassPathScanningCandidateComponentProvider 扫描" | getScanner + AnnotationTypeFilter(FeignClient) + findCandidateComponents (L177-181) | 接受 |
| "registerClientConfiguration 注册每个客户端配置" | L202 + FeignClientSpecification | 接受 |
| 执行计划/issue 未提: 懒/急注册双模式 | lazy-attributes-resolution (L212-215) | 补锚 |
| 执行计划/issue 未提: getBasePackages 四级兜底 | value→basePackages→basePackageClasses→importingClass 包 (L393+) | 补锚 |
| 执行计划/issue 未提: validateFallback 校验 | L83-87, L319-324 (fallback/fallbackFactory 类校验) | 补锚 |

## 待展开 (下一层)

1. eagerlyRegister 完整属性清单 (BeanDefinitionBuilder 属性)
2. lazilyRegister 延迟解析机制 (FeignClientFactoryBean 后处理?)
3. registerClientConfiguration → FeignClientSpecification 结构
4. getScanner 细节 (类型过滤器注册)
5. registerFeignClient 的 name/contextId 解析 (getName/getContextId)
