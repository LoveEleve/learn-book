# OF-1 注册机制 — Pass 2 闭环 Q4: 配置注册面 (FeignClientSpecification)

> 核心: registerClientConfiguration + FeignClientSpecification | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 每客户端配置怎么注册? Specification 是什么?**

## 机制链 (已实证)

```
registerClientConfiguration (L463-470):
├── BeanDefinitionBuilder.genericBeanDefinition(FeignClientSpecification.class) (L465)
├── 3 构造参数: name + className + configuration (L466-468)
└── registerBeanDefinition(name + ".FeignClientSpecification") (L469)
    ← 每客户端一个规范 BeanDefinition

FeignClientSpecification implements **NamedContextFactory.Specification** (L29) — SCC C-13 关联!
├── getName() (L46)
└── getConfiguration() (L62: Class<?>[]) — 配置类数组

两种来源:
├── 默认: registerDefaultConfiguration → "default.类名" (OF-1 q1)
└── 每客户端: registerFeignClients 内 L202 → name.FeignClientSpecification
```

## 关键设计 (why)

1. **NamedContextFactory.Specification**: 直接对接 SCC C-13 的命名子上下文机制 — OF-7 子上下文创建的输入
2. **构造参数即规范**: name/className/configuration 三要素 — 子上下文按此创建
3. **"name.FeignClientSpecification" 命名**: BeanDefinition 名可预测 — FeignClientFactory (OF-7) 按名获取
4. **默认 + 每客户端双注册**: 全局默认配置 + 客户端覆盖 — 配置继承结构 (OF-7 深入)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| registerClientConfiguration 三参 | FeignClientsRegistrar.java:463-470 |
| FeignClientSpecification implements Specification | FeignClientSpecification.java:29 |
| getName/getConfiguration | FeignClientSpecification.java:46,62 |

## 负面空间 (Q4 面)

- 不配置合并 (默认/客户端各自独立 BeanDefinition, 覆盖在子上下文)
- 不做配置类实例化 (仅 Class 引用, 子上下文时实例化)
- 不做规范去重 (同名覆盖由容器)

## 域归属说明

- FeignClientFactory (NamedContextFactory) 子上下文创建 → **OF-7 配置隔离** 深入
- FeignClientFactoryBean.getObject → **OF-2 代理创建** (OF-1 注册的 BeanDefinition 触发)
