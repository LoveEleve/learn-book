# G-8 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| @ExperimentalApi 指标 | grpc.lb.rls.* 指标 (CachingRlsLbClient.java:142+) | RLS 是较新特性 (数据面治理演进) |
| "EXPERIMENTAL." 前缀 | 各指标描述 | 观测面先行 — 新能力先埋点 |
| RouteLookupServiceClusterSpecifierPlugin | xds 模块 | **RLS 集成是 xds ClusterSpecifierPlugin 生态的后加成员** |
| AdaptiveThrottler DEFAULT 常量 | AdaptiveThrottler.java:45-47 | 节流参数演进 (可配化) |

## 写书建议

RLS 是 gRPC 数据面治理的**最新一环** (相对 DNS/xDS): 呈现为"决策外包的第三种答案" — 与 G-5 (DNS 静态)、G-7 (xDS 配置) 形成"去哪"三答案的收束。
