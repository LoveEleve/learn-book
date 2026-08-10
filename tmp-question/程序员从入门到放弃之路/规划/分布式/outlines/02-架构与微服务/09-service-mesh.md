# 每个服务都内嵌一个Sentinel来做限流, 100个服务=100份配置 — Service Mesh承诺"一次配置, 全局生效"

> Cluster C: 10 KPs | 依赖: 08-resilience-patterns, 07-microservices-design | 读者基线: 用过微服务框架(Spring Cloud/Dubbo), 对K8s有基本了解

---

### 1. 从内嵌SDK到基础设施层代理 — 为什么要把限流从应用代码中剥离?
  你团队每个Java服务都加了Hystrix→代码耦合(Sentinel代码散落在业务逻辑中)+运维复杂(每个服务各自Sentinel控制台)+语言绑定(Go/Python服务无法复用)
  - B1 Ch8 §4: Service Mesh起源 — 云原生时代服务数暴增→SDK嵌入模式(每个服务各自框架)暴露出更新/升级/多语言支持的困境 — 把流量治理能力抽象到独立进程(Sidecar)中, 应用不感知
  - 核心变换: SDK模式(应用内部→Java限定→每次应用升级→全流程测试) → Sidecar模式(独立进程→任何语言→基础设施独立→应用不修改) [理论: Service Mesh=把7层网络功能从单体应用拆分为独立服务, 用代理模式实现透明化]
  - B4 Ch8 §2: 为什么需要Service Mesh — 微服务间通信逻辑(负载均衡/重试/超时/熔断)与业务逻辑耦合→多语言栈无法复用→Sidecar模式将通信逻辑下沉为基础设施层的独立代理
  - 关键设计: Sidecar模式本质=把流量治理变成"OS级别"的服务 — 就像TCP/IP对应用透明, Service Mesh对微服务透明

### 2. Envoy + Istio — C++高性能代理 + 控制面的组合
  Envoy是数据面(实际处理每个TCP/HTTP包), Istio是控制面(定义规则→转换为Envoy配置)
  - B1 Ch8 §4.2: Envoy — C++编写/7层代理/动态配置API/可观测性(Metrics+Logging+Tracing) / 支持Filter Chain(L4/L7过滤器链)
  - B1 Ch8 §4.3: Istio架构 — Pilot(服务发现+流量配置→转Envoy配置), Mixer(策略(Telmetry→Prometheus) + 遥测 + 配额检查→已废弃用WasmPlugin替代), Citadel(安全, mTLS自动证书轮换) [案例: Istio最初使用Mixer集中做策略检查导致API延迟显著增加, v1.5重构为WasmPlugin嵌入Envoy消除网络跳]
  - B4 Ch8 §2.3: Istio流量管理 — VirtualService(路由规则, v1:v2=90:10灰度), DestinationRule(服务子集定义/负载均衡策略/连接池配置), Gateway(边缘入口) (B4 Ch8 §2.4)
  - 关键设计: 为什么需要控制面? — 100个Envoy同时改配置=灾难, 控制面统一生成配置并分发到所有Envoy, 运营改Istio配置→Pilot转Envoy配置→热更新到所有Sidecar

### 3. 可观测性 — Metrics/Tracing/Logging, 三根支柱缺一根都"看不清"
  你第一线上"用户说页面慢", 但没有数据的你真的只能猜 — 是订单服务? 还是库存服务? 还是MQLAG?
  - B4 Ch3 §5: 监控(Metrics) — Prometheus(Pull模式+PromQL+AlertManager→Grafana面板), 指标: 黄金四指标(延迟/流量/错误/饱和度) (B4 Ch3 §5.4-5.5)
  - 链路追踪(Tracing) — OpenTelemetry: Trace→Span→SpanContext, Jaeger/Zipkin存储+可视化; 采样率(全量太贵, 0.1%抽样→常见但可能丢异常, 强制100%错误+随机正常) (B4 Ch3 §5)
  - Logging — ELK(Elasticsearch+Logstash+Kibana), 结构化日志(JSON, requestId串联所有Span), 日志收集(Filebeat/Fluentd→Kafka→ES) (B4 Ch3 §5)
  - 关键设计: 分布式追踪的关键=全链路一个traceId — 入口生成traceId→每个服务继承+生成自己的spanId→最后Jaeger把整个调用树拼出来

### 4. 灰度发布与流量切分 — Service Mesh的最佳实践场景
  发布v2版本 → 先5%流量到v2 → 观察错误率(QPS/RT) → 如果正常→50%→最终100%, 如果异常→立刻15%→5%→0
  - B4 Ch4 §2.3: 灰度/金丝雀 — 从最小影响开始, 持续监控, 发现异常→立即回滚(改流量权重回0%)
  - Istio实现: VirtualService weight=v1(95):v2(5) → 调整weight→v2(100), 无需应用改代码, 完全基础设施层控制 (B4 Ch8 §2.4)
  - B1 Ch8 §4.4: Envoy Sidecar注入 — K8s Mutating Admission Webhook, 创建Pod时自动注入envoy容器→拦截所有入站/出站流量
  - 关键设计: 灰度发布=发布+监控+回滚三合一 — 如果没有监控(Metrics)的灰度发布就是闭眼开车, Service Mesh的灰度发布也依赖Prometheus/Grafana

### 5. 收束 — 回到统一治理的承诺
  - Service Mesh让"应用不关心流量治理" — 但代价是额外的Sidecar开销(CPU+内存+延迟) + 整个控制面运维复杂度
  - 如果你的团队规模小(<20服务) + 语言统一(Java), Spring Cloud/Dubbo + Sentinel足够了 — Service Mesh在>50服务+多语言时才显价值
  - 可观测性是Service Mesh的核心收益 — 无缝的Metrics+Tracing+Logging, 比限流/熔断更值得采用

---

### 核心悬念
**"Service Mesh解决了应用层的治理, 但你的服务还跑在物理机上——怎么做到'今天10个服务扩容到明天100个'而不被人肉运维拉垮?"**

→ 引出 容器与编排: Docker → Kubernetes(Pod/Service/Deployment) — 把微服务部署从人肉运维变为声明式基础设施 (10-container-orchestration)
