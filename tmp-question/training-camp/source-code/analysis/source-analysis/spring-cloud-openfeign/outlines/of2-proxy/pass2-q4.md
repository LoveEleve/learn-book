# OF-2 代理创建与装配 — Pass 2 闭环 Q4: Target 解析与 Targeter

> 核心: resolveTarget 三分支 + DefaultTargeter | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Target 怎么解析? 有几种? Targeter 怎么收尾?**

## 机制链 (已实证)

```
resolveTarget (L524-542) — Target 三分支:
├── 有 url → new HardCodedTarget(type, name, url + cleanPath()) (L525-527)
├── 无 url 但 RefreshableUrl 存在 → **RefreshableHardCodedTarget** (L530-533)
│   └── RefreshableUrl = context.getInstance("RefreshableUrl-" + contextId) — OF-9 联动!
└── 无 url 无 Refreshable → **PropertyBasedTarget** (L542)
    └── 懒加载: url 从配置属性解析 (spring.cloud.openfeign.client.config.[clientId].url)

cleanPath (L508-522): path 规范化 — 补 "/" 前缀/去尾部 "/"

DefaultTargeter (L25-28 implements Targeter):
└── target(factory, builder, context, target) — 直接 feign.target(target) 构建代理
    ← OF-6: FeignCircuitBreakerTargeter 三分支在此替换 (fallback/fallbackFactory)
```

## 关键设计 (why)

1. **Target 三分支 = URL 来源三态**: 注解直连 (HardCoded) / 动态刷新 (Refreshable, OF-9) / 配置懒加载 (PropertyBased, AOT)
2. **RefreshableUrl 从子上下文拿**: 按 contextId 命名获取 — 与 OF-9 的 RefreshableUrlFactoryBean 对接
3. **cleanPath 规范化**: path 补 "/" 前后处理 — URL 拼接正确性
4. **Targeter 可替换**: DefaultTargeter (普通) vs FeignCircuitBreakerTargeter (熔断, OF-6) — 策略注入点

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| resolveTarget 三分支 | FeignClientFactoryBean.java:524-542 |
| RefreshableHardCodedTarget (OF-9) | FeignClientFactoryBean.java:530-533 |
| PropertyBasedTarget | FeignClientFactoryBean.java:542 |
| cleanPath | FeignClientFactoryBean.java:508-522 |
| DefaultTargeter | DefaultTargeter.java:25-28 |

## 负面空间 (Q4 面)

- 不 URL 合法性校验 (前缀补全后直连)
- 不 Target 缓存 (每次 getTarget 重建)
- 不自动选择 Targeter (容器注入)
