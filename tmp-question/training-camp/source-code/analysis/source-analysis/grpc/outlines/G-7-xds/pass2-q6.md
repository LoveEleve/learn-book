# 闭环笔记 Q6 — WrrLocality: 同 Zone 优先 (执行计划 G-4 "优先同Zone" 的落点)

假设: WrrLocality 把地址按 locality (Zone) 分组, 用 locality 权重组合成加权子策略 — "同 Zone 优先" 是权重倾斜, 不是硬路由。

验证过程:
- **类结构** (WrrLocalityLoadBalancer.java:44-54): extends LoadBalancer + **GracefulSwitchLoadBalancer** (L48, util 家族 — 优雅切换) + LoadBalancerRegistry
- **locality 权重提取** (L68-80): "The configuration with the child policy is combined with the locality weights it gets from an attribute in... A map of locality weights is built up from the locality weight attributes in each address" (L68-74) — 地址属性读 **ATTR_LOCALITY_NAME** (L77) + **ATTR_LOCALITY_WEIGHT** (L78, XdsAttributes) → localityWeights Map
- **组合**: locality 权重 → 加权 target LB 配置 (L69: "combined with the locality weights to produce the weighted target LB config") — 子策略 = WeightedTarget (每 locality 一个目标, 权重来自控制面)
- **"优先"是权重语义**: 同 Zone 的 locality 权重高 → 被选中概率高; 不是硬性"只连同 Zone"
- 测试: (xds 测试目录, WrrLocality 相关)

代码类型: Implementation (权重组合)

结论: "同 Zone 优先" = **权重倾斜而非硬路由**: 控制面下发 locality 权重 (地址属性), WrrLocality 按 locality 分组 + 权重组合 (WeightedTarget 子策略); 同 Zone 地址权重高 → 流量倾向本地。**被放弃的方案: 硬性同 Zone 路由 (只连本 Zone)** — 本 Zone 全挂时无兜底; 权重方案自然降级。执行计划 G-4 原主题 "优先同Zone" 的真实落点在此 (xds 模块)。 [跨域: G-4 执行计划主题落点; G-7 WeightedTarget 子策略] [分布式: 同 Zone 亲和] (WrrLocalityLoadBalancer.java:41-80)
