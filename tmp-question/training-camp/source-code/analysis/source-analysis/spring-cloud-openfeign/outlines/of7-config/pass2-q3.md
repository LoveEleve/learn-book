# OF-7 配置隔离 — Pass 2 闭环 Q3: 配置面 (FeignClientProperties)

> 核心: FeignClientProperties + configureUsingProperties | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Properties 怎么配? 注解组件与 Properties 谁覆盖谁?**

## 机制链 (已实证)

```
FeignClientProperties (L30+):
├── **defaultConfig = "default"** (L31) — 默认配置键
├── **config = Map<String, FeignClientConfiguration>** (L33) — 每客户端
└── FeignClientConfiguration 8 字段 (L131-151): loggerLevel/connectTimeout/readTimeout/
    retryer (Class<Retryer>)/errorDecoder/requestInterceptors/defaultRequestHeaders/dismiss404

configureFeign (L166-190) — ⚠ 覆盖顺序开关:
├── **inheritParentContext 从 FeignClientConfigurer.inheritParentConfiguration() 设置** (L172-173)
├── **isDefaultToProperties=true** (L174-177): configureUsingConfiguration 先 → Properties 后
│   ← Properties 覆盖注解组件 (Properties 优先!)
└── false (L178-182): Properties 先 → Configuration 后 (注解组件优先)

configureUsingProperties 应用 (L260-300): dismiss404 → logLevel →
**options (!refreshableClient 时才设!)** → retryer → errorDecoder → requestInterceptors...
```

## 关键设计 (why)

1. **双级配置**: defaultConfig (全局) + config[contextId] (客户端覆盖) — 继承结构
2. **覆盖顺序可配**: isDefaultToProperties — 注解组件 vs Properties 优先级可切换 (配置哲学选择)
3. **inheritParentContext 联动**: 从 FeignClientConfigurer 读 — 与 OF-2 继承开关一致
4. **refreshableClient 联动**: options 只在非刷新时设 — OF-9 前置

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| defaultConfig + config Map | FeignClientProperties.java:31-33 |
| 8 字段 | FeignClientProperties.java:131-151 |
| isDefaultToProperties 分支 | FeignClientFactoryBean.java:174-188 |
| 应用清单 | FeignClientFactoryBean.java:260-300 |

## 负面空间 (Q3 面)

- 不配置热更新 (启动读取)
- 不 Properties 校验 (非法值运行时暴露)
- 不配置继承深度 (仅两层)
