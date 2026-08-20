# OF-1 注册机制 — Pass 2 闭环 Q1: 注解与入口面

> 核心: @EnableFeignClients + registerBeanDefinitions | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: @EnableFeignClients 怎么触发注册? 注册了什么?**

## 机制链 (已实证)

```
@EnableFeignClients (EnableFeignClients.java) — 5 组属性:
├── value()/basePackages() (L50/61) — 扫描包
├── basePackageClasses() (L71) — 类所在包 (包扫描)
├── defaultConfiguration() (L81) — 全局默认配置类
└── clients() (L88) — 显式指定客户端 (跳过扫描!)

FeignClientsRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware (L71)
registerBeanDefinitions (L152-155) — Import 触发:
├── registerDefaultConfiguration (L157-169): defaultConfiguration → registerClientConfiguration("default.类名")
└── registerFeignClients (L172+): 扫描/显式 → 每候选注册 FeignClientFactoryBean BeanDefinition
```

## 关键设计 (why)

1. **ImportBeanDefinitionRegistrar**: @EnableFeignClients 通过 @Import 触发 — 与 Spring Boot 自动装配同机制 (非 BeanPostProcessor)
2. **clients() 显式路径**: 不扫描直接指定 — 精确控制/扫描失效兜底
3. **默认配置独立注册**: defaultConfiguration 注册为 "default.类名" 规范 — 子上下文共享 (OF-7 前置)
4. **ResourceLoaderAware/EnvironmentAware**: 扫描需要资源加载器 + 环境 (懒/急判定)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 5 组注解属性 | EnableFeignClients.java:50-88 |
| ImportBeanDefinitionRegistrar | FeignClientsRegistrar.java:71 |
| registerBeanDefinitions 双注册 | FeignClientsRegistrar.java:152-155 |
| registerDefaultConfiguration | FeignClientsRegistrar.java:157-169 |

## 负面空间 (Q1 面)

- 不做运行时扫描 (启动时一次性注册)
- 不做 @FeignClient 条件过滤 (仅类型过滤器, 无自定义条件)
- 不做重复注册防护 (扫描结果唯一性由 Spring 容器)
