# 闭环笔记 Q5 — 分层策略族: CDS → ClusterManager → ClusterImpl → 叶子

假设: xds 负载均衡是四层嵌套: Cds (集群发现) → ClusterManager (按子集群分发) → ClusterImpl (EDS 消费) → 叶子策略 (RingHash/WRR/LeastRequest)。

验证过程:
- **CdsLoadBalancer2** (CdsLoadBalancer2.java:77-81): "One instance per top-level cluster. The top-level cluster may be a plain EDS/logical-DNS cluster or an aggregate cluster formed by a group of sub-clusters in a tree hierarchy" — **CDS = 集群发现入口**; 动态集群订阅 (L120-125, "The dynamic cluster must not have loaded yet")
- **ClusterManagerLoadBalancer** (ClusterManagerLoadBalancer.java:51-73): **extends MultiChildLoadBalancer** (util 基类, G-4 q5) — 覆写 createChildLbState (L73)/createChildAddressesMap (L78); 子集群删除有 deletionTimer (L126-127, 优雅延迟删除)
- **ClusterImplLoadBalancer** (ClusterImplLoadBalancer.java:78): EDS 消费 — edsServiceName (L99) + **drop 统计** (L144: addClusterDropStats — LRS 负载报告) + locality 传播 (L334)
- **叶子**: RingHash (q3)/WRR (q4)/LeastRequest — 全部 MultiChild 继承 + 默认子策略 PickFirst (G-4)
- **分层组装**: xds 配置树 (cluster → sub-clusters → eds → leaf policy) 由 XdsConfig 描述, LB 工厂逐层实例化

代码类型: Implementation (策略组合树)

结论: xds 把负载均衡组织成**组合树**: CDS (发现集群) → ClusterManager (子集群路由) → ClusterImpl (EDS 端点) → 叶子 (具体算法); 每层都是独立 LoadBalancer (SPI, G-4), MultiChild 基类提供子策略生命周期骨架。**被放弃的方案: 单层巨策略** — 控制面资源 (CDS/EDS 分离) 天然是分层数据, 分层 LB 一一对应, 每层可独立替换。 [跨域: G-4 MultiChild/PickFirst 复用; G-8 RLS 经 ClusterSpecifierPlugin 挂到 ClusterManager] [架构: 组合树] (CdsLoadBalancer2.java:77-125; ClusterManagerLoadBalancer.java:51-127; ClusterImplLoadBalancer.java:78-144)
