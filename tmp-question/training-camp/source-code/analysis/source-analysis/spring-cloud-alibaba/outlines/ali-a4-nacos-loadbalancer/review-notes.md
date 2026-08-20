# ALI-A4 NacosLoadBalancer 权重 — 深审

> 深审标准: 缺陷档案 #1~#15 (方案 B 六层)

## 审 1: 事实错误 — 2 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 规划称 "NacosBalancer.getHostByRandomWeight() 按权重随机选择" — 未提继承关系 | 大纲补: NacosBalancer **extends nacos-client 的 Balancer** (L38), getHostByRandomWeight2/3 是桥接方法 |
| 2 | 规划未提 IPv6 双栈逻辑 | 大纲 §3 补 filterInstanceByIpType 双栈 (L86-112) + convertIPv4ToIPv6 (L88-95) |

## 审 2: API/实现路径编造 — 1 修正

| # | 问题 | 修正 |
|:--:|:--|:--|
| 1 | 初稿假设 "权重读取自 NacosDiscoveryProperties.getWeight()" | 实测选择面权重从 **ServiceInstance.metadata("nacos.weight")** 读 (NacosBalancer:69) — 注册面才用 properties.getWeight() (A3) — 双向分离精确化 |

## 审 3: 文件名/目录名推断 — 0 (loadbalancer/balancer 包均读源码)

## 审 4: 跨项目概念转移 — 1 修正

| # | 误判 | 实测 |
|:--:|:--|:--|
| 1 | "NacosLoadBalancer 直接调 NacosBalancer" | 实测中间隔 LoadBalancerAlgorithm 插槽 (DefaultLoadBalancerAlgorithm 才调 NacosBalancer) — 可插拔架构精确化 |

## 审 5: 覆盖率 — 0 缺漏 (算法插槽/权重随机/流水线/装配 4 大机制全覆盖)

## 审 6: 跨层一致性 — 0 (无 harness, 数字锚点重 grep 一致)

## 结论: 4 项修正, 全部落盘大纲; 锚点重 grep 验证通过

## 二轮 REVIEW (2026-08-16)

| # | 维度 | 发现 | 处置 |
|:--:|:--|:--|:--|
| 1 | 锚点复验 | LB:136-139 (EmptyResponse) / 149-150 (nacos.cluster) / 170-176 (算法路由+默认兜底) / Balancer:38 (extends) + 69-70 (metadata 回读) / DefaultAlgo:36+46 (DEFAULT_SERVICE_ID+LOWEST) 全精确 | 通过 ✅ |
| 2 | 跨域一致 | nacos.weight/healthy 回读 ↔ A3 hostToServiceInstance 写入方闭环; 算法插槽 ↔ SCC-7/12 对照 | 通过 ✅ |
| 3 | 数字自洽 | 三关过滤/两选路由/2021.1 版本锚全文一致 | 通过 ✅ |

## 四轮 REVIEW (2026-08-16, 全量脚本复验)

文字锚 (#7) 命中修复: 本文档域引用类补行号 (grep 实证, 见 HANDOFF §六 第 6 行); 锚点范围修正见全局记录。
