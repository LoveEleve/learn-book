# OF-9 动态刷新 — Pass 2 闭环 Q4: 联动面 (getUrl + 消费链)

> 核心: FeignClientsRegistrar.getUrl + OF-2 消费 | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: URL 解析怎么统一? 动态面怎么被消费?**

## 机制链 (已实证)

```
FeignClientsRegistrar.getUrl (L115-127) — 统一 URL 规范化:
├── **SpEL 排除** (L116): "#{" 开头且含 "}" → 原样返回 (运行时表达式)
├── 前缀补全 (L117-119): 无 "://" → "http://"
├── 尾部 "/" 去除 (L120-121)
└── **URI 合法性校验** (L124-127): malformed → IllegalArgumentException

消费链 (跨域):
├── OF-1: FeignClientsRegistrar 注册时 url 属性 (L115 同款)
├── OF-2: **resolveTarget 三分支** (L528-535): RefreshableUrl =
│   context.getInstance(contextId, "RefreshableUrl-" + contextId, RefreshableUrl.class)
│   → RefreshableHardCodedTarget (动态) / PropertyBasedTarget (懒加载)
├── OF-7: FeignClientProperties.config[contextId].url 配置源
└── SCC C-2: @RefreshScope 触发 FactoryBean 重建 (scc1-bootstrap 交叉)
```

## 关键设计 (why)

1. **URL 解析统一**: getUrl 一处规范化 (SpEL/前缀/校验) — OF-1/OF-9 复用
2. **按名获取**: "RefreshableUrl-" + contextId — 子上下文规范命名 (OF-7)
3. **三分支消费**: 直连 (HardCoded) / 动态 (Refreshable) / 懒加载 (PropertyBased) — URL 来源三态完整
4. **RefreshScope 联动**: 配置刷新 → Bean 重建 → 新 URL — 运行时动态

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| getUrl 规范化 | FeignClientsRegistrar.java:115-127 |
| SpEL 排除 | 同上 L116 |
| OF-2 消费 | FeignClientFactoryBean.java:528-535 |
| RefreshableUrl 按名 | 同上 L530-531 |

## 负面空间 (Q4 面)

- 不 URL 刷新传播 in-flight
- 不做多个 RefreshableUrl 并存 (按 contextId 唯一)
- 不做 SpEL 求值 (留运行时)
