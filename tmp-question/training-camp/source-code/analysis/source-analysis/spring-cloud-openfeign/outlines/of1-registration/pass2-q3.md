# OF-1 注册机制 — Pass 2 闭环 Q3: 注册面 (懒/急双模式 + BeanDefinition)

> 核心: eagerly/lazilyRegister + BeanDefinition 属性 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 扫描到的接口怎么变成 BeanDefinition? 懒/急两种注册差在哪?**

## 机制链 (已实证)

```
懒/急分流 (L210-215):
└── spring.cloud.openfeign.lazy-attributes-resolution (默认 false)
    ├── false → eagerlyRegisterFeignClientBeanDefinition (L222):
    │   ├── validate(attributes) (L319-324):
    │   │   ├── validateFallback (L83-87): fallback 类校验 (需实现接口?)
    │   │   └── validateFallbackFactory (L87+)
    │   ├── BeanDefinitionBuilder.genericBeanDefinition(FeignClientFactoryBean.class) (L225)
    │   └── 10 个属性 (L226-):
    │       url/path/name/contextId/type(接口类名)/dismiss404/fallback/fallbackFactory/
    │       **refreshableClient (isClientRefreshEnabled — OF-9 开关!)/qualifiers**
    └── true → lazilyRegisterFeignClientBeanDefinition (L218): 延迟属性解析
        ← 属性不从注解立即解析, 延迟到 Bean 创建
```

## 关键设计 (why)

1. **FeignClientFactoryBean 作为 BeanDefinition 工厂**: genericBeanDefinition(FeignClientFactoryBean.class) — 实际代理创建在 getObject() (OF-2)
2. **懒/急可配**: lazy-attributes-resolution — 大项目扫描慢时可延迟解析 (启动优化)
3. **validate 前置**: fallback/fallbackFactory 类校验在注册期 — 配置错误早暴露
4. **refreshableClient 属性**: 与 OF-9 动态刷新联动 (刷新开关注册期决定)
5. **qualifiers**: 限定符支持 (@Qualifier 面)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 懒/急分流 | FeignClientsRegistrar.java:210-218 |
| validate + 双校验 | FeignClientsRegistrar.java:83-87,319-324 |
| BeanDefinitionBuilder + 10 属性 | FeignClientsRegistrar.java:225-245 |

## 负面空间 (Q3 面)

- 不做 BeanDefinition 合并 (每客户端独立)
- 不做属性默认值注入 (缺省由 FactoryBean 处理)
- 不做代理预创建 (注册期只建 BeanDefinition)
