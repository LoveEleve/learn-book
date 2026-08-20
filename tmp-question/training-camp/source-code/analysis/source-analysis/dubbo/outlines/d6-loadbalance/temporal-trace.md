# D-6 负载均衡 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: LoadBalance SPI (默认 random) + AbstractLoadBalance (select 模板 + getWeight 预热) + Random/RoundRobin/LeastActive/ConsistentHash 四算法 |
| 2.7.x | 权重/预热完善; 方法级权重 (WEIGHT_KEY); RoundRobin RECYCLE_PERIOD 回收 |
| 3.x | **ShortestResponseLoadBalance (滑动窗口响应)** + **AdaptiveLoadBalance (P2C 自适应)**; AbstractClusterInvoker select 重选逻辑完善 |
| 3.3.x | loadbalance/ 7 文件 (Abstract + 6) 稳定 |

## 痕迹证据

- LoadBalance.java:36: @SPI(RandomLoadBalance.NAME) — 默认 random (2.x 锚)
- AbstractLoadBalance.java:46-50: calculateWarmupWeight 线性爬坡 (2.x 锚)
- RoundRobinLoadBalance.java:38: RECYCLE_PERIOD = 60000 (2.7+ 锚)
- ConsistentHashLoadBalance.java:84: replicaNumber = HASH_NODES 默认 160 (2.x 锚)
- ShortestResponseLoadBalance.java:75-76: SucceededResponseTimeWindow offset (3.x 锚)
- AdaptiveLoadBalance.java:41: attachmentKey "mem,load" + selectByP2C (L62) (3.x 锚)
- AbstractClusterInvoker.java:140-141: "a) Firstly, select an invoker using loadbalance. If this invoker is in previously selected list..." (重选注释锚)

## 推断标注

- "2.x 四算法" — Dubbo 2.x 公知版本线 (标注)
- "2.7.x 回收" — RECYCLE_PERIOD 存在性推断 (标注)
- "3.x ShortestResponse/Adaptive" — 类名/注释实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- Nginx 平滑加权轮询: 同源算法 (WRR current 递增/扣减) — 算法对照
- Ribbon: 客户端负载均衡 vs Dubbo 集群负载 — 框架对照
- Cassandra/Envoy P2C: Adaptive 同思路 — 算法对照
