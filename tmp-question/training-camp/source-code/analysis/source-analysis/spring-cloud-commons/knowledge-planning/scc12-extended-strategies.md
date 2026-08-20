# SCC-12 LoadBalancer 扩展策略 — 知识规划 (KP)

> 域: SCC-12 | 级别: 🟡 | 方案: B | 大纲: outlines/scc12-extended-strategies/outline.md (6 节)

## §01 域定位

LoadBalancer 扩展策略 = 实例列表的六种"滤镜"。加权 (元数据 weight) / 区域 (zone 过滤+回退) / hint (请求级过滤) / 粘性 (cookie) / 子集 (分桶降采样) / 同实例偏好 (选择记忆)。

## §02 源文件清单

| 文件 | 行数 | 职责 | 归属节 |
|:--|:--:|:--|:--:|
| core/WeightedServiceInstanceListSupplier.java | 128 | 权重展开 | 1 |
| core/ZonePreferenceServiceInstanceListSupplier.java | 107 | 区域过滤 | 2 |
| core/HintBasedServiceInstanceListSupplier.java | 103 | hint 过滤 | 3 |
| core/RequestBasedStickySessionServiceInstanceListSupplier.java | 102 | cookie 粘性 | 4 |
| core/SubsetServiceInstanceListSupplier.java | 99 | 分桶降采样 | 5 |
| core/SameInstancePreferenceServiceInstanceListSupplier.java | 101 | 同实例偏好 | 6 |
| core/WeightFunction.java | — | 权重函数 SPI | 1 |

## §05 闭环要点 (Pass 2 内化)

### q1 加权
METADATA_WEIGHT_KEY="weight" (L41) + 默认 metadataWeightFunction (L50) + weightFunction.apply (L91)。

### q2 区域
ZONE 键 (L42) + spring.cloud.loadbalancer.zone + 回退全部 (Javadoc L31-34)。

### q3 hint/粘性
getHint 双来源 (L64-71) / instanceIdCookieName cookie (L60-68) + 回退。

### q4 子集/同实例
size 分桶 (L71-78) / selectedServiceInstance 覆写 (L94-95)。

## §06 负面空间 (6 条)

不做动态权重 / 不做多区域感知 / 不做粘性失效清理 / 不做子集动态重算 / 不做同实例过载保护 / 不做组合策略编排

## §07 交叉引用

- ← SCC-6 Supplier (链扩展) + SCC-7 ReactorLoadBalancer (策略消费)
- 另见: Ribbon WeightedResponseTimeRule/ZoneAwareLoadBalancer / Netflix subsetting
