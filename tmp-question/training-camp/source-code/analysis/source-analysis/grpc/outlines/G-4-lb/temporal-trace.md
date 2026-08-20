# G-4 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| Helper @since 1.22.0 | LoadBalancer.java:1052 | 早期 API 无 Helper 抽象 (旧版本直接 Subchannel 数组) |
| PickFirstLoadBalancerConfig @since | PickFirstLoadBalancer.java:185 | shuffle 是后加配置 (负载分散需求) |
| PickFirstLeafLoadBalancer (915 行) | 新版每地址一 Subchannel | **1.83 的核心演进**: 连接粒度从"列表"到"地址" + Happy Eyeballs |
| GRPC_EXPERIMENTAL_PF_WEIGHTED_SHUFFLING | PickFirstLeafLoadBalancer.java:66-67 | 实验标志 → 默认开 (true) |
| MultiChildLoadBalancer @since 2023 (Copyright) | MultiChildLoadBalancer.java:2 | **2023 年统一多子策略基类** — xds 家族重构的骨架 |

## 写书建议

呈现"连接粒度演进": 旧版 (列表级 Subchannel) → 新版 (地址级 Subchannel + Happy Eyeballs) + 策略家族统一基类 (MultiChild, 2023) — 体现 gRPC 从"够用"到"精细控制"的演进。
