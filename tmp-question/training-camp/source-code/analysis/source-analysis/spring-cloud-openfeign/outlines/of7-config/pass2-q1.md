# OF-7 配置隔离 — Pass 2 闭环 Q1: 子上下文工厂面

> 核心: FeignClientFactory + NamedContextFactory | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 每个 @FeignClient 的子上下文怎么创建? 组件怎么取?**

## 机制链 (已实证)

```
FeignClientFactory extends NamedContextFactory<FeignClientSpecification> (L39):
├── 构造 super(FeignClientsConfiguration.class, "spring.cloud.openfeign",
│   "spring.cloud.openfeign.client.name", initializers) (L47-48)
│   ← 配置类 + 属性前缀 + 上下文名属性
└── 3 实例获取:
    getInstanceWithoutAncestors (L52-58: beanOfType)
    getInstancesWithoutAncestors (L61-63: getBeansOfType)
    getInstance(name, beanName, type) (L65-67: getBean)
    ← OF-2 inheritParentContext 开关消费!

NamedContextFactory.getContext (SCC L119):
├── contexts.put(name, **createContext(name)**) — 惰性创建 (首次访问)
├── createContext: buildContext + **AOT 初始化器** (L132-134) + **context.refresh()**
└── registerBeans (L): configurations.get(name).getConfiguration() 循环注册
    ← OF-1 注册的 FeignClientSpecification 是输入!
```

## 关键设计 (why)

1. **子上下文隔离**: 每 @FeignClient 独立 GenericApplicationContext — 组件互不污染 (SCC C-13 核心价值)
2. **惰性创建**: 首次 getContext 才建 — 不使用的客户端不消耗
3. **配置注册**: FeignClientSpecification（OF-1）→ registerBeans — 规范驱动上下文装配
4. **继承开关**: WithoutAncestors 变体 — 父容器组件可选 (OF-2 联动)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| FeignClientFactory extends NamedContextFactory | FeignClientFactory.java:39 |
| 构造 (配置类+前缀) | FeignClientFactory.java:47-48 |
| 3 实例获取 | FeignClientFactory.java:52-67 |
| getContext 惰性创建 | NamedContextFactory.java:119 |
| registerBeans | NamedContextFactory.java |

## 负面空间 (Q1 面)

- 不子上下文复用 (每客户端独立)
- 不上下文销毁复用 (客户端销毁即删)
- 不做父容器自动回退 (显式继承开关)
