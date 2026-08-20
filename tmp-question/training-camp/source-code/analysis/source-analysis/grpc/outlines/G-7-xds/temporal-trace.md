# G-7 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹 + 命名 (LoadBalancer2 后缀)。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| CdsLoadBalancer**2** | CdsLoadBalancer2.java:81 | **第二版重写痕迹**: 早期 CdsLoadBalancer (v1) 被替换 — 代码命名保留代数 |
| XdsClientImplV3Test | xds/src/test/.../GrpcXdsClientImplV3Test | **xDS v3 协议** (v2 时代遗留测试可能已删) |
| GRPC_EXPERIMENTAL_PF_WEIGHTED_SHUFFLING | CdsLoadBalancer2.java:82 | 实验标志迁移 |
| grpclb (GrpclbState 1281) | grpclb 模块 | **xDS 前身**: 旧控制面 LB 协议 — 时空对照材料 |
| MultiChildLoadBalancer @2023 | util (G-4 溯源) | 策略家族统一基类 (2023) — xds 全部多子策略受益 |

## 写书建议

呈现"控制面协议两代演进": grpclb (早期专用协议) → xDS v2 → xDS v3 (1.83 当前) + 策略家族代数命名 (LoadBalancer2) — 体现 xDS 生态从"专用"到"通用控制面"的收敛。
