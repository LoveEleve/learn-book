# OF-9 动态刷新 — Pass 2 闭环 Q1: Target 刷新面

> 核心: RefreshableHardCodedTarget | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 刷新后的 URL 怎么让 Target 动态拿到?**

## 机制链 (已实证)

```
RefreshableHardCodedTarget extends Target.HardCodedTarget<T> (L28):
├── 持有 RefreshableUrl (L30) + cleanPath (L31)
├── 2 构造器 (L36-46): super(type, name, refreshableUrl.getUrl()) — 构造时取当前 URL
└── **url() 覆写** (L49-51): refreshableUrl.getUrl() + cleanPath
    ← 每次调用 (如请求构建时) 从 RefreshableUrl 取最新值!
    ← 刷新 = 换 RefreshableUrl 内部值 → 下次调用即用新 URL
```

## 关键设计 (why)

1. **动态覆写**: HardCodedTarget.url() 是每次调用的 — 覆写后天然动态
2. **值对象持有**: RefreshableUrl 是简单值对象 — FactoryBean 重建即换值
3. **cleanPath 分离**: 路径规范化独立于 URL — 拼接正确性
4. **与普通 HardCodedTarget 对比**: 静态 (final) vs 动态 (每次取) — 语义差异

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| extends HardCodedTarget | RefreshableHardCodedTarget.java:28 |
| RefreshableUrl 持有 | 同上 L30 |
| url() 覆写 | 同上 L49-51 |

## 负面空间 (Q1 面)

- 不 URL 缓存 (每次取)
- 不线程安全特殊处理 (volatile 值对象)
- 不做 URL 校验 (构造时)
