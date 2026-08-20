# OF-9 动态刷新 — Pass 2 闭环 Q2: FactoryBean 面

> 核心: RefreshableUrlFactoryBean | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: RefreshableUrl 怎么从配置构建? 刷新怎么生效?**

## 机制链 (已实证)

```
RefreshableUrlFactoryBean implements FactoryBean<RefreshableUrl>, ApplicationContextAware (L34):
├── getObjectType → RefreshableUrl.class (L43)
├── setApplicationContext (L45-48)
├── **getObject()** (L53-68):
│   ├── refreshableUrl 缓存命中 → 返回 (L54-56)
│   ├── FeignClientProperties.config 获取 (L57-59)
│   ├── config null / 无 url → **new RefreshableUrl(null)** (L61-66) — 容错!
│   └── **new RefreshableUrl(FeignClientsRegistrar.getUrl(configuration.getUrl()))** (L67)
└── setContextId (L) — 按客户端名
→ **@RefreshScope**: 配置刷新 → FactoryBean 重建 → getObject 重新构建 → 新 RefreshableUrl
```

## 关键设计 (why)

1. **FactoryBean 模式**: 配置→RefreshableUrl 的工厂 — 容器管理生命周期
2. **@RefreshScope 联动**: 刷新事件 → Bean 重建 → 新 URL (commons C-2 交叉)
3. **null 容错**: 无配置/无 url → RefreshableUrl(null) — 不炸
4. **FeignClientsRegistrar.getUrl 复用**: 属性解析逻辑统一 (OF-1 联动)
5. **缓存语义**: refreshableUrl 字段缓存 — 非刷新期不重复构建

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| FactoryBean + ApplicationContextAware | RefreshableUrlFactoryBean.java:34 |
| getObject 缓存 | 同上 L53-56 |
| null 容错 | 同上 L61-66 |
| getUrl 构建 | 同上 L67 |

## 负面空间 (Q2 面)

- 不 URL 热校验 (重建时)
- 不刷新传播到 in-flight 请求 (下次调用生效)
- 不做 URL 模板 (静态值)
