# my-xhs 三高优化规划（stage-3 B2 模式汇总）

> **定位**：把 stage-3 32 篇提取文档中的"现状核对差距清单"汇总为**一份可执行的 my-xhs 优化规划**。
> 依据：docs 三高优化方法论（stage-3 教学主线）+ my-xhs 实际代码实证（每项附位置）
> 时间：2026-08-12 | 来源：progress/course/stage-3/ 32 篇现状核对小节 + HANDOVER-session004
> 使用方式：按 P1→P2→P3 逐项执行；每项完成后更新本表状态

---

## 一、总览

| 优先级 | 项数 | 定位 |
|:---:|:---:|------|
| P1 | 2 | 立即执行（docs 核心动作缺失 + 已实现但未完成） |
| P2 | 8 | 短期（1-2 周内）——配置补全/链路确认 |
| P3 | ~15 | 演进项（触发条件驱动，决策待定） |

**优化方法论依据**（02 篇 6 步循环）：评估定基线 → 改前测量 → 找瓶颈 → 改 → 改后测量 → 好则留坏则回滚——**所有优化必须配套"改前基线 + 改后对比"**（当前最大缺口即此，见 P1-2）。

---

## 二、P1 优化项（立即执行）

### P1-1 灰度负载均衡补全（"看起来在做≠真的实现"现场）
- **现状**（21 篇实证）：`GrayRouteFilter.java:15` 已按 `X-Gray-Tag` Header 路由到灰度实例；但 `:43` 注释"**GrayLoadBalancer（需后续实现）**"——**灰度 LB 未实现**；`:47` 灰度比例依赖上游（CDN/前端）设置 header
- **差距**：docs 21"基于版本、流量分配的流量控制"——my-xhs 只有 header 路由，无**权重切分**与**灰度负载均衡**
- **优化动作**：
  1. 实现 `GrayLoadBalancer`（扩展 `ReactorServiceInstanceLoadBalancer`——参照 `LeastConnectionsLoadBalancer.java:53` 的扩展模式）：根据实例 metadata（gray/stable 标记）+ 灰度权重选择实例
  2. 灰度比例支持：Header 指定（现有）+ 配置中心比例（Nacos——灰度百分比切分）
  3. 配套：`TrafficColoringFilter`（染色）与灰度联动（压测流量走灰度实例）
- **涉及文件**：`gateway/filter/GrayRouteFilter.java` + 新增 `gateway/loadbalancer/GrayLoadBalancer.java` + `gateway/.../config`
- **预期收益**：发布策略基建化（金丝雀/灰度权重——docs 21 主要内容②）
- **验证**：灰度实例部署 → X-Gray-Tag 路由验证 + 权重切分压测对比

### P1-2 建立真实压测基线与前后对比（docs 每节核心动作）
- **现状**（02/06/09 篇共识）：my-xhs-benchmark 3 个 JMH 类为**模拟 payload**（`OrderServiceBenchmark` 的 `simulateOrderCreation` = Math.sqrt 循环——不能当性能结论）；无 JMeter 端到端；无 JFR 启用；无 TPS/QPS/RT 基线数据
- **差距**：docs 每节要求"对比升级前后性能变化"——**测量面全程缺失**（02 篇 6 步循环第②步无法执行）
- **优化动作**：
  1. **JMH 真实化**：benchmark 注入真实 service（连接池/DB/Feign 调用）——至少覆盖订单创建、Feed 聚合、搜索三条核心路径
  2. **JMeter 端到端**：七链核心链路压测脚本（参照 test-2 全链路测试文档的链路划分）——输出 TPS/QPS/RT 分布（P95/P99）
  3. **JFR 启用**：`jcmd <pid> JFR.start duration=60s filename=baseline.jfr`（零重启动态启用——04 篇）——GC/锁/CPU 现场取证
  4. **基线归档**：每次优化前后对比记录（建 `docs/benchmark-baselines/` 或 Nacos 配置管理）
- **涉及**：`my-xhs-benchmark/` + 压测脚本 + start-all.sh（可选持久化 JFR）
- **预期收益**：docs 全部"对比性能变化"动作可执行；优化决策有数据支撑
- **验证**：基线数据产出 + 首次优化（如 P2-1 NMT/GC 参数）前后对比

---

## 三、P2 优化项（短期）

### P2-1 开启 NativeMemoryTracking（docs 案例一直接教训）
- **现状**（18 篇）：`start-all.sh` JAVA_OPTS 无 `-XX:NativeMemoryTracking`（grep 实证）
- **差距**：堆外谜团只能猜（docs 案例一"600M vs 319M"之谜靠 NMT 解开）
- **动作**：`start-all.sh:16-20` 各档 JAVA_OPTS 加 `-XX:NativeMemoryTracking=summary`（~1% 开销）→ 排障时 `jcmd <pid> VM.native_memory summary`
- **涉及**：`start-all.sh` | **收益**：堆外内存定位能力 | **验证**：jcmd 输出 committed 分布

### P2-2 HTTP/2 补 SSL（h2 的 TLS 前提）
- **现状**（20 篇）：`user/application.yml:18` `http2.enabled: true` 无 `server.ssl`——h2 需 ALPN/TLS（无 ssl 走 h2c 明文或降级）
- **动作**：①keytool/OpenSSL 生成证书（docs 20 命令：`keytool -genkeypair -keyalg RSA -keysize 4096 -storetype PKCS12`）②`server.ssl` 配置（key-store/key-store-password/key-store-type: PKCS12/key-alias）③证书托管（生产 CA 或 mTLS——08 篇 Triple 衔接）
- **涉及**：各服务 `application.yml` | **收益**：h2 真正生效 + 安全 | **验证**：`curl -v --http2` 检查 ALPN 协商

### P2-3 ES 日志 ILM 清理策略（磁盘防线）
- **现状**（29 篇）：`logstash.conf` 按日索引 `myxhs-logs-%{+YYYY.MM.dd}`——**ILM/清理策略未配**（索引无限增长）
- **动作**：ES ILM 策略（hot→warm→delete，如 7 天/30 天滚动删除）或 curator 定时清理
- **涉及**：ES 策略 API + logstash.conf 索引模板 | **收益**：磁盘可控 | **验证**：索引生命周期观察

### P2-4 动态刷新链路核对（@RefreshScope vs rebinder 配合）
- **现状**（27 篇）：SCA 配置加载完整（spring.config.import/shared-configs）——**@RefreshScope 使用面未核**（HANDOVER 教训 4：@RefreshScope 与 rebinder 是配合关系）
- **动作**：核对哪些 Bean 依赖动态配置（数据源/线程池/限流参数）→ 确认 @RefreshScope 或 ConfigurationProperties 刷新覆盖
- **涉及**：common/config + 各服务 | **收益**：配置变更真正动态生效 | **验证**：Nacos 改配置 → 服务热生效

### P2-5 动态路由刷新确认（配置变更 → 路由生效）
- **现状**（19 篇）：路由配置在 Nacos（`my-xhs-gateway.yaml`）——**RefreshRoutesEvent 显式监听未发现**
- **动作**：确认 SCG 内建刷新链路（配置变更 → EnvironmentChangeEvent → RefreshRoutesEvent）或补监听；验证路由热更新
- **涉及**：gateway | **收益**：路由动态化 | **验证**：Nacos 改路由 → 网关生效不重启

### P2-6 Nacos 运维细节核对（健康保护阈值/存储后端/集群形态）
- **现状**（25 篇）：Nacos 使用面良好（namespace/shared-configs）——**健康保护阈值/存储后端（Derby vs MySQL）/集群形态未核**
- **动作**：①健康保护阈值配置（docs 25 KP-03 防雪崩机制）②存储后端确认（docs ② DB 模型——高可用 MySQL 支撑）③集群规模确认
- **涉及**：Nacos 部署（compose——03 篇）| **收益**：注册中心稳健性 | **验证**：故障演练（摘实例观察阈值行为）

### P2-7 JDK21 迁移评估（虚拟线程高价值点）
- **现状**（33 篇）：JDK17 LTS（var 使用实证）——**21 未迁移**
- **动作**：评估虚拟线程（JEP 444）对高并发服务的收益（17 篇线程模型衔接）——试点服务迁移 + 压测对比（P1-2 基线）
- **涉及**：服务 pom（java.version）+ 线程模型 | **收益**：线程模型革命 | **验证**：试点服务压测对比

### P2-8 Sentinel 指标接入 Prometheus + Kibana 部署确认
- **现状**（03 篇）：Sentinel 有（Bulkhead/Nacos 规则）但**指标未适配进 Micrometer/Prometheus**；Kibana 部署未确认（29 篇）
- **动作**：①Sentinel metrics → Micrometer（适配器/自定义 Meter）→ Prometheus 采集 ②Kibana 部署 + 日志面板
- **涉及**：common（metrics）+ config（grafana/logstash）| **收益**：容错指标可观测 + 日志可视化 | **验证**：Prometheus 查询 sentinel 指标

---

## 四、P3 优化项（演进/触发条件驱动，决策待定）

| # | 项 | 触发条件 | 涉及 | 关联 docs |
|:---:|---|---------|------|:---:|
| P3-1 | Kafka 日志中间层（削峰） | 日志峰值超直连能力 | logstash + Kafka | 29 |
| P3-2 | VictoriaMetrics 迁移 | 保留期 >15 天或写入规模超单机 | Prometheus → VM | 30 |
| P3-3 | Native 迁移（GraalVM+Spring AOT） | 启动敏感（Serverless/边缘） | 构建链 | 28/31/32 |
| P3-4 | Mesh 引入（Istio/Kiali） | 网内流量管理/零侵入诉求 | k8s | 21/22 |
| P3-5 | RSocket/背压服务间传输 | 服务间背压场景 | 订单等 | 17 |
| P3-6 | Dubbo 引入（泛化/Proxyless） | RPC 栈诉求/多语言互通 | 服务调用 | 10/23/24 |
| P3-7 | gateway server.tomcat 生效性 | reactive 栈配置核对 | gateway yml | 05/09 |
| P3-8 | ShardingSphere 分片规则细节 | 分片键/算法核对 | order config | 12 |
| P3-9 | Canal 下游消费核对 | 缓存/ES/事件联动 | canal + 消费端 | 11/16 |
| P3-10 | Record/Sealed 采用 | 代码现代化 | 各服务 | 33 |
| P3-11 | 强封装影响核对（--add-opens） | 内部 API 依赖审计 | 启动参数 | 33 |
| P3-12 | 线程池拒绝策略显式化 + 告警 | AsyncConfig 默认 AbortPolicy | common/config | 18 |
| P3-13 | 备份策略显式化 | 副本备份未发现证据 | 运维 | 11 |
| P3-14 | 命名空间多环境隔离（dev/prod） | 环境隔离 | Nacos | 25 |
| P3-15 | IM 传输方案核对 | IM 模块传输 | my-xhs-im | 16 |

---

## 五、执行顺序建议（依赖关系）

```
P1-2 压测基线（一切优化的前提——02 篇 6 步循环第②步）
  ├── P2-1 NMT（基线建立的一部分——内存真相）
  ├── P2-2 HTTP2/SSL（性能面配置补全——基线后对比）
  ├── P2-7 JDK21 评估（需要基线支撑对比）
  └── P1-1 灰度 LB（独立可并行——发布能力）
P2-4/P2-5（配置/路由动态化——治理面）
P2-3/P2-8（可观测补全——ILM/Kibana/Sentinel 指标）
P2-6（注册中心稳健性）
P3-*（触发条件驱动——按需评估）
```

**关键原则**：每一项优化执行前，先跑 P1-2 的基线测量（02 篇 6 步循环）；每项完成后记录"改前/改后"对比并归档。

---

## 六、与 docs 章节映射（本规划的依据来源）

| 优化项 | 依据 docs 节 | 提取文档 |
|--------|-------------|---------|
| P1-1 灰度 LB | 21（流量控制） | stage-3-21-Istio.md |
| P1-2 压测基线 | 02（方法论）+ 09（HTTP 升级对比） | stage-3-02/09 |
| P2-1 NMT | 18（案例一） | stage-3-18 |
| P2-2 SSL | 20（HTTP/2 服务器） | stage-3-20 |
| P2-3 ILM | 29（日志定期清理） | stage-3-29 |
| P2-4 动态刷新 | 27（配置客户端）+ HANDOVER 教训 4 | stage-3-27 |
| P2-5 动态路由 | 19（动态配置） | stage-3-19 |
| P2-6 Nacos 运维 | 25（健康保护阈值/DB 模型） | stage-3-25 |
| P2-7 JDK21 | 33（虚拟线程 JEP444） | stage-3-33 |
| P2-8 Sentinel 指标 | 03（Sentinel→Micrometer）+ 29（Kibana） | stage-3-03/29 |
| P3-* | 各对应 docs 节 | 对应提取文档 |

---

## 七、状态追踪

| 项 | 状态 | 完成日期 | 改前/改后对比 |
|----|------|---------|--------------|
| P1-1 灰度 LB | ⬜ 待执行 | - | - |
| P1-2 压测基线 | ⬜ 待执行 | - | - |
| P2-1 NMT | ⬜ 待执行 | - | - |
| P2-2 HTTP2/SSL | ⬜ 待执行 | - | - |
| P2-3 ILM | ⬜ 待执行 | - | - |
| P2-4 动态刷新 | ⬜ 待执行 | - | - |
| P2-5 动态路由 | ⬜ 待执行 | - | - |
| P2-6 Nacos 运维 | ⬜ 待执行 | - | - |
| P2-7 JDK21 | ⬜ 待执行 | - | - |
| P2-8 Sentinel 指标/Kibana | ⬜ 待执行 | - | - |
| P3-1~15 | ⬜ 待评估 | - | - |
