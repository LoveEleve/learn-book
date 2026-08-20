# 课程侧 L2 聚合：跨 stage 功能域知识图谱（轮 0）

> 来源：`training-camp/stage-{1..4}-完整梳理.md`（4 份，3084 行——功能域聚类 + 过时→现代替代映射表）
> 性质：**课程侧 L2 聚合**（07 SOP 阶段 2——L3 总大纲的课程输入核对）
> 聚合方法：①提取 4 份梳理的功能域聚类 ②30 功能域 → L3 主题承接核对 ③35 组现代替代映射 → L3 落地核对 ④无孤儿核对
> 状态：轮 0 完成——**30 功能域全部承接 + 35 组映射全部落地（本轮补 3 处）**

---

## 一、功能域 → L3 主题承接总表（30 域——无孤儿）

| # | 功能域 | 来源 stage | L3 承接主题 | 核对 |
|---|--------|:--:|------|:--:|
| 1 | 工程构建域 | 1 | 4.1/4.4 | ✅ |
| 2 | REST API 域 | 1 | 4.4 | ✅ |
| 3 | 容错域 | 1 | 2.4 | ✅ |
| 4 | 负载均衡域 | 1 | 2.3 | ✅ |
| 5 | 可观测-指标域 | 1 | 2.9 | ✅ |
| 6 | 可观测-链路域 | 1 | 2.9 | ✅ |
| 7 | 网关域 | 1 | 2.7 | ✅ |
| 8 | 性能优化域 | 1 | 5.1 | ✅ |
| 9 | 脚手架域 | 1 | 4.4 | ✅ |
| 10 | 共识理论域 | 2 | 3.2/3.1 | ✅ |
| 11 | ZooKeeper 域 | 2 | 2.1/3.2 | ✅ |
| 12 | 事务域 | 2 | 2.5/3.3 | ✅ |
| 13 | RPC 域 | 2 | 4.3/4.8 | ✅ |
| 14 | 配置中心域 | 2 | 2.2 | ✅ |
| 15 | 数据存储域 | 2 | 2.6 | ✅ |
| 16 | 分布式缓存域 | 2 | 2.6 | ✅ |
| 17 | 性能优化方法论域 | 3 | 4.4/5.1 | ✅ |
| 18 | 服务容器调优域 | 3 | 5.1 | ✅ |
| 19 | 可观测性域 | 3 | 2.9 | ✅ |
| 20 | 注册发现与高可用域 | 3 | 2.1 | ✅ |
| 21 | RPC 与 HTTP 升级域 | 3 | 4.4/5.3 | ✅ |
| 22 | Reactive 与事件域 | 3 | 5.3/2.10 | ✅ |
| 23 | 网关与 Service Mesh 域 | 3 | 2.7/4.4 | ✅ |
| 24 | Dubbo 架构域 | 3 | 4.3 | ✅ |
| 25 | 云原生域 | 3 | 1.7/5.2 | ✅ |
| 26 | JVM 故障排查域 | 3 | 5.2 | ✅ |
| 27 | Dubbo Mesh 域 | 3 | 4.3/4.4 | ✅ |
| 28 | etcd 配置中心域 | 3 | 2.2 | ✅ |
| 29 | 多活域（含 2025 补充） | 4 | 2.8/3.4 | ✅ |
| 30 | 工程案例与成长域 | 3 | （非知识——结营） | ✅ 排除 |

**核对结论**：30 功能域（29 知识 + 1 结营）**全部有 L3 主题承接，无孤儿功能域** ✓

---

## 二、现代替代映射落地核对（35 组——本轮补 3 处）

### stage-1（16 组——全部落地）

| 过时技术 | 现代替代 | 落位主题 | 核对 |
|---------|---------|---------|:--:|
| Netflix Eureka | Nacos | 2.1 | ✅ |
| Netflix Ribbon | Spring Cloud LoadBalancer | 2.3 | ✅ |
| Netflix Servo | Micrometer | 2.9 | ✅ |
| Spring Cloud Sleuth | Micrometer Tracing + OTel + SkyWalking | 2.9 | ✅（本轮补 OTel 全称） |
| Hystrix | Sentinel | 2.4 | ✅（本轮补 Hystrix 对照） |
| RestTemplate 主流 | RestClient + OpenFeign | 4.4/2.1 | ✅ |
| FastJSON | Jackson | 4.7/5.1 | ✅ |
| javax.validation | Jakarta Validation 3.0 | 1.4 | ✅ |
| Bootstrap 上下文 | application.yml + on-profile | 5.1/2.2 | ✅ |
| Spring Cloud Config | Nacos Config | 2.2 | ✅ |
| Netflix OSS 整体 | SCA | 2.1/2.2 | ✅ |
| gateway artifactId | webflux/webmvc 双栈 | 2.7/4.8 | ✅ |
| Tomcat Boss/Worker | 虚拟线程 | 5.1 | ✅ |
| X-B3-TraceId | W3C traceparent | 2.9 | ✅ |
| @NewSpan（Sleuth） | @Observed（Observation） | 2.9 | ✅（本轮补 @Observed） |
| WeightedResponseTimeRule | 手写 SCLB 实现 | 2.3 | ✅ |

### stage-2（10 组——全部落地）

| 过时技术 | 现代替代 | 落位主题 | 核对 |
|---------|---------|---------|:--:|
| ZK 注册中心 | Nacos | 2.1 | ✅ |
| ZK 分布式锁 | Redisson | 2.6 | ✅ |
| EJB 3.x | Jakarta EE | 2.5 | ✅ |
| Grizzly | Netty | 5.3 | ✅ |
| Spring Cloud Config | Nacos/Apollo | 2.2 | ✅ |
| Hmily TCC | Seata TCC | 2.5 | ✅ |
| TX LCN | Seata | 2.5 | ✅ |
| JTA/XA | Seata AT/TCC | 2.5/3.3 | ✅ |
| Hessian | Protobuf/Kryo | 4.8 | ✅ |
| Sleuth | Micrometer Tracing + SkyWalking | 2.9 | ✅ |

### stage-3（9 组——全部落地）

| 过时技术 | 现代替代 | 落位主题 | 核对 |
|---------|---------|---------|:--:|
| Eureka（已废弃） | Nacos | 2.1 | ✅ |
| Eureka JGroups/Tribes | Raft/gRPC | 3.2 | ✅ |
| Spring Cloud Netflix | SCA | 2.1 | ✅ |
| CMS GC | ZGC/G1 | 5.2 | ✅ |
| JDK 8/11 | JDK 21（虚拟线程+分代 ZGC+GraalVM） | 5.2 | ✅ |
| RestTemplate | RestClient + WebClient | 4.4 | ✅ |
| JPA | MyBatis-Plus | 2.6 | ✅ |
| OpenTSDB | VictoriaMetrics/Mimir | 2.9 | ✅ |
| RSocket（衰退） | gRPC Triple | 5.3 | ✅（本轮补"衰退→Triple"定位） |

**核对结论**：35 组映射**全部在 L3 主题中落地**（本轮补齐 OpenTelemetry 全称/@Observed/Hystrix 对照/RSocket 衰退定位 4 处）✓

---

## 三、课程侧知识面补充发现（轮 0 新增）

| 补充项 | 来源 | 落位 | 说明 |
|--------|------|------|------|
| Istio + Cilium Service Mesh | stage-3 梳理 | 2.7 | 本轮补（现代对应） |
| Dubbo 3.x Proxyless Mesh | stage-3 梳理 | 2.7 | 本轮补（无 Sidecar 形态） |
| Actuator Endpoints 架构 | stage-1 梳理 | 2.7 | 本轮补（@Endpoint/暴露控制/安全边界） |
| 拼写缺陷 4~7 例 | L2 缺陷家族 | 4.8/2.10/4.6 | 前轮补齐 |
| P1 时序 2.10 交叉 | L2 缺陷家族 | 2.10 | 本轮补（静默失效家族完整性） |

---

## 四、轮 0 与骨架 §三 总表的复核

- **骨架 103 篇主属总表**（骨架 §三）vs 本文 30 功能域：**一致**（功能域是篇的聚类——每域至少 1 篇主属）
- **待裁决 4 项**（骨架 §9.2——stage-2-13/25~28/3-12/1-24 维持现状）：**轮 0 未发现新证据推翻**——维持
- **结营篇**（stage-3-结营）：非知识——不入主题 ✓
- **无孤儿篇**：103 篇全部有主属（骨架 §9.6 已全量核对——102 知识篇）✓

---

## 五、遗留（诚实标注）

1. **骨架 §三 主属表的逐篇复核**：轮 0 以功能域粒度核对（30 域）——**篇粒度（103 篇）的逐篇复核**建议在总览终稿的用户 review 中完成（骨架 §9.6 已做标签首位对照——轮 0 补充域粒度）
2. **sca-lab 14 lab 的实践完成度**：映射已定（骨架 §3.y）——**实际 lab 练习**是学习者的动作（非本文档职责）
