# OF-2 代理创建与装配 — Pass 2 闭环 Q3: 超时与定制 (Options + Customizers)

> 核心: OptionsFactoryBean + applyBuildCustomizers | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 超时配置优先级? Builder 定制怎么排序应用?**

## 机制链 (已实证)

```
OptionsFactoryBean (implements FactoryBean<Request.Options>) (L35):
└── createOptionsWithApplicableValues (L60-95) — 超时三级优先级:
    ├── clientConfiguration (FeignClientProperties) 非空 → 用它 (L76-79)
    │   ├── connectTimeout / readTimeout / **followRedirects** 三参数
    │   └── 单参数 null 时 fallback options 对应值 (L77/79 逐项)
    └── 全缺 → 默认 options (feign 默认值)
    → new Request.Options(connect, MILLISECONDS, read, MILLISECONDS, followRedirects) (L82)

applyBuildCustomizers (L153-161) — Builder 定制:
├── customizerMap = context.getInstances(contextId, FeignBuilderCustomizer.class) (L154)
├── sorted(AnnotationAwareOrderComparator.INSTANCE) (L160) — 有序应用
└── forEach(customizer.customize(builder)) (L161)
```

## 关键设计 (why)

1. **超时三级优先级**: Properties 客户端配置 > feign 默认 — 且**逐项 fallback** (connect 配了 read 没配, read 用默认)
2. **followRedirects 可配**: 重定向跟随策略 — 3.x 超时配置扩展
3. **Customizer 有序**: AnnotationAwareOrderComparator (@Order/@Priority) — 多定制器顺序确定
4. **getInstances 按 contextId**: 定制器从客户端子上下文取 — 每客户端独立定制 (OF-7)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| OptionsFactoryBean 三级优先级 | OptionsFactoryBean.java:60-95 |
| followRedirects | OptionsFactoryBean.java:80-82 |
| applyBuildCustomizers 排序 | FeignClientFactoryBean.java:153-161 |
| getInstances(contextId) | FeignClientFactoryBean.java:154 |

## 负面空间 (Q3 面)

- 不做超时自动调优 (静态配置)
- 不做 Customizer 去重 (同序多实例按序应用)
- 不做 options 缓存 (每次创建)
