# OF-9 动态刷新 — Pass 2 闭环 Q3: 懒加载面 (PropertyBasedTarget)

> 核心: PropertyBasedTarget | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 配置属性 URL 怎么懒加载? 与 AOT 的关系?**

## 机制链 (已实证)

```
PropertyBasedTarget extends Target.HardCodedTarget<T> (L31):
├── 注释 (L22/25): "resolves url from properties when..." — spring.cloud.openfeign.client.config.[clientId].url
├── 持有 FeignClientConfiguration config (L35) + path
├── 构造: super(type, name, config.getUrl()) (L41/47)
└── **url() 覆写** (L50-62):
    ├── url == null → **url = config.getUrl() + path** (L53-55) — 懒计算
    └── 返回缓存 url (L)
    ← 懒加载: 首次调用才拼 URL; AOT 场景 (编译期无运行时配置)
```

## 关键设计 (why)

1. **懒计算 + 缓存**: url 首次调用构建, 后续复用 — 性能
2. **配置属性源**: config[clientId].url — 与 FeignClientProperties 联动 (OF-7)
3. **AOT 场景**: 编译期无运行时配置 → 懒加载让 native 镜像可用 (注释实证)
4. **与 Refreshable 对比**: PropertyBased (配置懒加载) vs Refreshable (动态刷新) — 两种动态面

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| extends HardCodedTarget | PropertyBasedTarget.java:31 |
| 注释 (配置属性 URL) | PropertyBasedTarget.java:22-25 |
| url() 懒计算 | 同上 L50-62 |

## 负面空间 (Q3 面)

- 不做 URL 刷新 (懒加载非动态)
- 不线程安全双检 (简单赋值)
- 不做配置热读 (缓存后固定)
