# OF-9 动态刷新 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (RefreshableHardCodedTarget / RefreshableUrlFactoryBean / RefreshableUrl / PropertyBasedTarget)
> 09 域级审计: OPENFEIGN-PLAN OF-9 (新增域, issue F-9 补) — 断言 "RefreshableUrl 动态 URL + PropertyBasedTarget 懒加载" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: RefreshableHardCodedTarget (L28-70)

```
extends Target.HardCodedTarget<T> (L28):
├── 持有 **RefreshableUrl** (L30) + cleanPath (L31)
├── 2 构造器 (L36-46): super(type, name, refreshableUrl.getUrl())
└── **url() 覆写** (L49-51): refreshableUrl.getUrl() + cleanPath
    ← 每次调用从 RefreshableUrl 取最新 URL!
```

### Level-2: RefreshableUrlFactoryBean (L34-90)

```
implements FactoryBean<RefreshableUrl>, ApplicationContextAware (L34):
├── getObjectType → RefreshableUrl.class (L43)
├── **getObject()** (L53-68): refreshableUrl 缓存 → 首次:
│   ├── FeignClientProperties.config[contextId] 获取 (L)
│   ├── 空/无 url → ? (L)
│   └── **refreshableUrl = new RefreshableUrl(FeignClientsRegistrar.getUrl(configuration.getUrl()))** (L67)
│       ← 配置刷新 → FactoryBean 重建 → 新 RefreshableUrl (动态!)
└── (setUrl 等?)
```

### Level-3: PropertyBasedTarget (L25-46)

```
extends Target.HardCodedTarget<T> (L31):
├── 注释 (L25): "spring.cloud.openfeign.client.config.[clientId].url"
├── 持有 **FeignClientConfiguration config** (L35)
└── super(type, name, config.getUrl()) (L41) — 配置属性懒加载 (AOT 场景)
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-9 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "RefreshableHardCodedTarget extends HardCodedTarget" | L28 | 接受 |
| "RefreshableUrl 动态 URL" | url() = refreshableUrl.getUrl() + cleanPath (L49-51) | 接受 |
| "RefreshableUrlFactoryBean" | FactoryBean + getObject 配置构建 (L34-68) | 接受 |
| "PropertyBasedTarget 懒加载" | config 持有 + super(config.getUrl()) (L25-41) | 接受 |
| 补锚: url() 每次调用取最新 | L49-51 (动态覆写) | 补锚 |
| 补锚: 配置来源 | FeignClientProperties.config[contextId].url (L67) | 补锚 (OF-7 联动) |
| 补锚: FeignClientsRegistrar.getUrl 复用 | L67 | 补锚 (OF-1 联动) |

## 待展开 (下一层)

1. RefreshableUrlFactoryBean 的 setUrl/刷新机制 (@RefreshScope 联动细节)
2. PropertyBasedTarget.url() 覆写 (懒加载语义)
3. FeignClientsRegistrar.getUrl 完整 (SpEL/属性解析)
4. OF-2 resolveTarget 三分支的消费 (RefreshableUrl 获取)
5. 与 @RefreshScope 的关系 (commons C-2 交叉)
