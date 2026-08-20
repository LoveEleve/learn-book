# 闭环笔记 Q4 — WeightedRoundRobin: 加权随机 + 带外负载报告驱动权重

假设: xds 的 WRR 不是固定权重轮询 — 权重来自端点的带外负载报告 (orca), 按利用率动态调整; 选择算法是"累积权重扫描"加权随机。

验证过程:
- **配置面** (WeightedRoundRobinLoadBalancer.java:70-75 注释): `enableOobLoadReport: true` / `oobReportingPeriod: 10s` / `weightExpirationPeriod: 180s` / `weightUpdatePeriod: 1s` / `errorUtilizationPenalty: 1.0` — **权重是动态的: 端点周期性上报负载**
- **带外报告消费**: OrcaReportListener (L365-381): `report.getEps() > 0 && errorUtilizationPenalty > 0` (L381) → 按错误率/利用率惩罚权重 — 权重随负载波动
- **加权随机选择** (WeightedRandomPicker.java:113-140): totalWeight=0 → 均匀随机 (L116-119); 否则 `rand = random.nextLong(totalWeight)` → **累积权重线性扫描** (L121-130: "Find the first idx such that rand < accumulatedWeights[idx]. Not using Arrays.binarySearch for better readability.")
- **失效兜底**: "with valid weight, which caused the WRR policy to fall back to RR behavior" (L119 注释) — 权重不可用/过期 (endpoint_weight_stale, L133) → 回退普通轮询
- **调度**: weightUpdateTimer (L103) 周期 (weightUpdatePeriod 1s) 更新权重
- 测试: wrrLifeCycle (WeightedRoundRobinLoadBalancerTest.java:260)

代码类型: Algorithmic (加权随机 + 反馈控制)

结论: xds WRR = **闭环加权**: 端点经 orca 带外通道周期性上报利用率 → 权重按 (1 - errorUtilizationPenalty × 错误率) 动态调整 → 累积权重扫描选端点 (O(n), 注释明说为可读性放弃二分); 权重过期回退 RR。**被放弃的方案: 固定权重轮询 (Envoy 早期)** — 无法感知负载不均; 权重动态化让流量自然流向空闲端点。 [跨域: G-7 orca 模块 (带外报告协议); G-4 MultiChild 基类; G-6 过期/回退思想] [算法: 加权随机/反馈控制] (WeightedRoundRobinLoadBalancer.java:70-75,103,365-381; WeightedRandomPicker.java:113-140)
