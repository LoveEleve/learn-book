# D-6 负载均衡 — Pass 2 闭环 Q1: 抽象面 (SPI + 模板 + 权重预热)

> 核心: LoadBalance SPI + AbstractLoadBalance | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 负载均衡抽象出什么? 权重怎么算? 预热怎么影响权重?**

## 机制链 (已实证)

```
LoadBalance @SPI(RandomLoadBalance.NAME)    LoadBalance.java:36 — 默认 random
SPI 注册表 (internal, 6 算法穷举):
├── random / roundrobin / leastactive / consistenthash (2.x 族)
└── shortestresponse / adaptive (3.x 族)

AbstractLoadBalance.select (L51-60) — 模板方法:
├── invokers 空 → null
├── size==1 → 直接返回 (单节点无选择)
└── doSelect(invokers, url, invocation) — 抽象, 各算法实现

getWeight (L72-100) — 权重计算:
├── 方法级权重: getMethodParameter(method, WEIGHT_KEY, DEFAULT_WEIGHT)
├── 预热: TIMESTAMP_KEY (provider 启动时间) → uptime < WARMUP_KEY (默认?) → calculateWarmupWeight
└── return Math.max(weight, 0) — 权重不为负
calculateWarmupWeight (L46-50): ww = uptime / (warmup/weight) — 线性爬坡
├── ww < 1 → 1 (最小权重 1)
└── min(ww, weight) — 不超过满权重
```

## 关键设计 (why)

1. **SPI + 默认 random**: 负载均衡可插拔 (URL 参数 loadbalance=xx 切换), 默认随机
2. **模板方法**: select 公共逻辑 (空/单节点) 统一, doSelect 各算法 — 新算法只写核心
3. **方法级权重**: 权重按方法可配 (WEIGHT_KEY 方法参数) — 细粒度
4. **预热爬坡**: 新启动 provider 权重从 1 线性增长到满权重 — **避免刚启动的节点被压垮** (JIT 预热/缓存冷启动)
5. **权重不为负**: Math.max 保护 — 配置错误不产生负权重

## 锚点清单

| 锚点 | 位置 |
|:--|:--|
| @SPI 默认 random | LoadBalance.java:36 |
| SPI 注册表 6 算法 | dubbo-cluster resources META-INF/dubbo/internal/org.apache.dubbo.rpc.cluster.LoadBalance |
| select 模板 (空/单节点) | AbstractLoadBalance.java:51-60 |
| calculateWarmupWeight | AbstractLoadBalance.java:46-50 |
| getWeight + 预热判断 | AbstractLoadBalance.java:72-100 |

## 负面空间 (Q1 面)

- 不做全局负载状态 (各算法独立, 无共享统计 — 除自适应面)
- 不做预热加速 (线性爬坡, 无指数)
- 不做权重动态下发 (权重静态 URL 参数)
