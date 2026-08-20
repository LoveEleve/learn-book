# G-7 Pass 1 扫描笔记 — xDS 控制面与高级负载均衡

> 日期: 2026-08-16 | 版本: 1.83.1 | 🟡 B | 模块: xds/ (184 文件: 顶层 80 + client 20 + internal 30 + matcher 22 + security 30 + orca 4) + grpclb 对照

## 继承树/调用图

```
xds:///target → XdsNameResolver (96, xds: scheme)
  ├── XdsClientImpl (1102) — 控制面客户端: watch/subscribe 资源
  │     └── ControlPlaneClient (59) — ADS 流: sendDiscoveryRequest (L191)/ACK (L215)/NACK (L231)/version_info (L74)
  ├── ClusterManagerLoadBalancer (263) — 按 cluster 分发
  │     └── ClusterImplLoadBalancer (571) — EDS 子策略
  │           └── 叶子策略: RingHash (575, Ketama)/LeastRequest (348)/WRR (903)/WrrLocality
  │                 全部 extends MultiChildLoadBalancer (util, 414)
  ├── CdsLoadBalancer2 (651) — CDS 集群发现
  └── 过滤器: RouterFilter/FaultFilter/RbacFilter/ExternalProcessorFilter
XdsServerWrapper (960) — xDS 服务端面 (LDS/RDS)
grpclb (对照, GrpclbState 1281) — 旧控制面协议
```

## 基本元素分解

1. **XdsClientImpl/ControlPlaneClient**: ADS 双向流 — 订阅/ACK/NACK/version 协商
2. **XdsNameResolver**: xds:/// target → 控制面资源
3. **策略家族**: RingHash (Ketama)/LeastRequest/WRR/WrrLocality — 全部 MultiChild 继承
4. **分层**: Cds (cluster) → ClusterManager → ClusterImpl (eds) → 叶子
5. **过滤器链**: Router/Fault/RBAC + matcher 22 (路由匹配引擎)
6. **服务端面**: XdsServerWrapper (LDS/RDS)

## 标记问题 (7)

1. **Q1 ADS 协议**: XdsClientImpl 订阅模型 — watch/资源类型 (CDS/EDS/LDS/RDS)/ACK-NACK/version 协商
2. **Q2 XdsNameResolver**: xds:/// target 解析 (1206) — 与 DnsNameResolver 的差异 (控制面拉取)
3. **Q3 RingHash**: Ketama 一致性哈希 (L60-64 注释) — ring 构建/哈希/最小化移动
4. **Q4 WRR**: 加权轮询 (903) — 权重调度算法
5. **Q5 分层策略**: Cds→ClusterManager→ClusterImpl 的分层职责
6. **Q6 WrrLocality**: 同 Zone 优先 — locality 权重 (执行计划 G-4 "优先同Zone" 落点)
7. **Q7 过滤器面**: RouterFilter (默认路由) + FaultFilter (故障注入) — 数据面拦截

## 已读测试

- RingHashLoadBalancerTest: subchannelLazyConnectUntilPicked (L148) — 惰性连接; subchannelNotAutoReconnectAfterReenteringIdle (L182)
- WeightedRoundRobinLoadBalancerTest: pickChildLbTF (L211)/wrrLifeCycle (L260)
- ControlPlaneClientTest/GrpcXdsClientImplV3Test 存在
