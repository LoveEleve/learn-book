# D-7 集群容错 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.x | 骨架: Cluster SPI (默认 failover) + 8 策略 (Failover/Failfast/Failsafe/Failback/Forking/Broadcast/Available/Mergeable) + AbstractClusterInvoker (select + reselect + sticky) + MockClusterWrapper |
| 2.7.x | 路由链完善 (condition/script/tag); Directory 抽象 (AbstractDirectory); 集群 Filter 链雏形 |
| 3.x | **ZoneAwareCluster (区域亲缘)** + **InvocationInterceptorBuilder 拦截器面** + ClusterFilterInvoker (集群级 Filter 链正式化) + CLUSTER_INTERCEPTOR_COMPATIBLE 兼容开关; router/ 扩至 7 族 (affinity/mesh) |
| 3.3.x | support/wrapper/AbstractCluster 稳定; FailoverClusterInvoker 重试注释完善 |

## 痕迹证据

- Cluster.java:34: @SPI(Cluster.DEFAULT) — 默认 failover (2.x 锚)
- AbstractClusterInvoker.java:140-143: "a) Firstly, select an invoker using loadbalance... b) Reselection, the validation rule for reselection: selected > available" (2.x 重选注释锚)
- AbstractClusterInvoker.java:155-185: sticky 粘滞 (2.x 锚)
- ZoneAwareClusterInvoker.java:46: "1. registry marked as 'preferred=true' has the highest priority" (3.x 锚)
- AbstractCluster.java:45-64: buildClusterInterceptors + ClusterFilterInvoker + CLUSTER_INTERCEPTOR_COMPATIBLE_KEY (3.x 锚)
- MockClusterWrapper.java:38: cluster.join 包装 (2.x 锚)

## 推断标注

- "2.x 8 策略" — Dubbo 2.x 公知版本线 (标注)
- "2.7.x 路由链" — router/ 包结构推断 (标注)
- "3.x ZoneAware/拦截器" — 类名/注释实证 (实证)
- **源码 grafted (浅克隆, 单 commit)**: git log 时空考古受限 — 以注释锚 + 类结构为主 (降级说明)

## 对照线 (已交付/待交付)

- Sentinel (ST-1 流量控制/ST-2 熔断): 规则驱动降级 vs Dubbo Mock 配置驱动 — 容错对照
- Ribbon 重试: 客户端重试 vs Dubbo Failover — 框架对照
- ZK 阶段 (4.3): Watcher 通知 vs 路由链更新 — 机制对照
