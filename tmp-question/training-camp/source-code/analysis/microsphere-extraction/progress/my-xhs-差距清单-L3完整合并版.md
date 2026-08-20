# my-xhs 差距清单（L3 完整合并版——v2）

> **定位**：合并**两套差距清单**为一份可执行的完整清单——①原始 `my-xhs-优化规划.md`（stage-3 B2 模式——P1 2/P2 8/P3 15 = 25 项）②L3 汇总清单（33 篇"该用没用"判定 + TOP①~④）
> **合并原则**：以原始编号体系（P1-x/P2-x/P3-x）为主干，L3 提取的差距项映射归入；冲突项去重；L3 独有的补充项用字母后缀（如 P2-A）
> 依据：`my-xhs-优化规划.md`（原始 25 项）+ `my-xhs-差距清单-L3汇总.md`（L3 22 项）
> 使用方式：按 P1→P2→P3 逐项执行；每项完成后更新状态

---

## 一、总览

| 优先级 | 项数 | 定位 |
|:---:|:---:|------|
| **P1** | **4** | 立即执行（原始 2 + L3 补充 2） |
| **P2** | **13** | 短期（原始 8 + L3 补充 5） |
| **P3** | **16** | 演进项（原始 15 + L3 补充 1，触发条件驱动） |
| **合计** | **33** | 覆盖原始 25 项全部 + L3 新增 8 项 |

---

## 二、P1 立即执行（4 项）

### P1-1 灰度按标实例过滤补全（原始——"看起来在做≠真的实现"现场）
- **现状**：`GrayRouteFilter.java` 打标已实现（:61/:63-75/:86），**`GrayLoadBalancer`（按标记过滤实例）注释"需后续实现"未实现**（:43）；:47 注释与实际矛盾
- **动作**：实现 `GrayLoadBalancer`（扩展 ReactorServiceInstanceLoadBalancer——参照 LeastConnectionsLoadBalancer:53）+ 灰度比例配置化（GRAY_PERCENT 硬编码 10 → Nacos）
- **关联**：L3 2.3（灰度 LB 未实现 P1-1 差距）
- **验证**：灰度实例部署 → grayTag 打标后实例过滤验证

### P1-2 建立真实压测基线与前后对比（原始——docs 每节核心动作）
- **现状**：benchmark 3 个 JMH 类为模拟 payload（`simulateOrderCreation` = Math.sqrt 循环）；无 JMeter 端到端；无基线数据
- **动作**：①JMH 真实化（注入真实 service——连接池/DB/Feign）②JMeter 七链端到端脚本（TPS/QPS/RT 分布 P95/P99）③JFR 启用配合
- **关联**：L3 4.4（"最大的工程差距"）、5.2（三工具分工）
- **验证**：改前基线 + 改后对比（02 篇 6 步循环）

### P1-A 反射整改：RocketMQHealthIndicator 用官方 getProducer()（L3 补充——4.7）
- **现状**：`RocketMQHealthIndicator.java:89-90` 反射读 `RocketMQTemplate.producer`——官方 `getProducer()`（RocketMQTemplate.java:75）就摆在那
- **动作**：改官方 API（一行）；反射失败显式报错（fail-fast）
- **验证**：健康检查正常 + RocketMQ 升级无反射依赖

### P1-C JFR 启用（L3 补充——5.2）
- **现状**：start-all.sh 无 `-XX:StartFlightRecording`（grep 实证 0 处）
- **动作**：start-all.sh 加 `-XX:StartFlightRecording=defaultrecording=true`（低开销常驻）+ jcmd 动态启用兜底
- **验证**：jcmd JFR.check 正常

---

## 三、P2 短期（13 项）

### 原始 8 项（编号保留）

| # | 项 | 现状 | 动作 | 验证 |
|:---:|---|------|------|------|
| P2-1 | 开启 NativeMemoryTracking | start-all.sh 无 NMT | `-XX:NativeMemoryTracking=summary`（~1% 开销） | jcmd VM.native_memory summary |
| P2-2 | HTTP/2 补 SSL | `http2.enabled: true` 无 `server.ssl`（h2 需 ALPN/TLS） | keytool 生成证书 + server.ssl 配置 + 证书托管 | curl -v --http2 查 ALPN |
| P2-3 | ES 日志 ILM 清理 | logstash.conf 按日索引无清理策略 | ES ILM（hot→warm→delete 7/30 天） | 索引生命周期观察 |
| P2-4 | 动态刷新链路核对 | @RefreshScope 使用面未核 | 核对哪些 Bean 依赖动态配置 + 刷新覆盖 | Nacos 改配置热生效 |
| P2-5 | 动态路由刷新确认 | RefreshRoutesEvent 显式监听未发现 | 确认 SCG 内建刷新链路或补监听 | Nacos 改路由生效 |
| P2-6 | Nacos 运维细节核对 | 健康保护阈值/存储后端/集群形态未核 | 阈值配置 + Derby→MySQL + 集群规模 | 故障演练摘实例 |
| P2-7 | JDK21 迁移评估 | JDK17 LTS 未迁移 | 虚拟线程（JEP 444）试点 + 压测对比 | 试点压测 |
| P2-8 | Sentinel 指标接入 Prometheus | Sentinel 有但指标未进 Micrometer | Sentinel metrics→Micrometer→Prometheus + Kibana 部署 | Prometheus 查 sentinel 指标 |

### L3 补充 5 项（字母后缀）

| # | 项 | 来源 | 动作 |
|:---:|---|:--:|------|
| P2-A | 端点粒度路由（差距 TOP①） | L3 2.7 | 参照源码 09 we://——端点元数据 → 网关端点级路由表（自主实现） |
| P2-B | 命令级限流（差距 TOP②） | L3 2.4/2.6 | Redis/MyBatis 命令拦截 + Sentinel 埋点（官方空白自主实现） |
| P2-C | 跨区复制（差距 TOP④） | L3 2.6/2.8 | Kafka 逻辑复制评估（10 2.1 按域配置）vs Redis 7 Multi-AZ |
| P2-D | 特性开关集中化 | L3 4.8 | 灰度/功能开关从"代码 if 散落"→配置 + 条件注解 + 排序 |
| P2-I | 自定义系统/cgroup 指标 binder | L3 2.9（用户指出） | 参照源码 11 CGroupMemoryMetrics（K8s 容器内存感知） |

---

## 四、P3 演进项（16 项）

### 原始 15 项（编号保留）

| # | 项 | 触发条件 |
|:---:|---|---------|
| P3-1 | Kafka 日志中间层 | 日志峰值超直连能力 |
| P3-2 | VictoriaMetrics 迁移 | 保留期 >15 天或写入规模超单机 |
| P3-3 | Native 迁移（GraalVM） | 启动敏感（Serverless/边缘） |
| P3-4 | Mesh 引入（Istio/Kiali） | 网内流量管理/零侵入诉求 |
| P3-5 | RSocket/背压服务间传输 | 服务间背压场景 |
| P3-6 | Dubbo 引入（泛化/Proxyless） | RPC 栈诉求/多语言互通 |
| P3-7 | gateway server.tomcat 生效性 | reactive 栈配置核对 |
| P3-8 | ShardingSphere 分片规则细节 | 分片键/算法核对 |
| P3-9 | Canal 下游消费核对 | 缓存/ES/事件联动 |
| P3-10 | Record/Sealed 采用 | 代码现代化 |
| P3-11 | 强封装影响核对（--add-opens） | 内部 API 依赖审计 |
| P3-12 | 线程池拒绝策略显式化 + 告警 | AsyncConfig 默认 AbortPolicy |
| P3-13 | 备份策略显式化 | 副本备份未发现证据 |
| P3-14 | 命名空间多环境隔离 | 环境隔离 |
| P3-15 | IM 传输方案核对 | IM 模块传输 |

### L3 补充 1 项

| # | 项 | 来源 | 触发条件 |
|:---:|---|:--:|---------|
| P3-A | 异地多活演进 | L3 3.4/2.8 | 跨地域部署诉求（RTO/RPO 未显式定义——P2-6 相关） |

---

## 五、执行顺序（依赖关系）

```
P1-2 压测基线（一切优化的前提）
  ├── P2-1 NMT（基线的一部分——内存真相）
  ├── P1-C JFR（诊断前置——与基线同步部署）
  └── P1-A 反射整改（独立快改）
P1-1 灰度 → P2-D 特性开关（灰度集中化依赖）
P2-A 端点路由 → P2-B 命令级限流（自主实现面）
P2-C 跨区复制 → P3-5 RSocket（服务间传输演进）
```

---

## 六、L3 判定为"不该用"的知识（执行时不要误入）

Eureka/Ribbon/Hystrix（过时）/ ZK 注册中心（Nacos 覆盖）/ 自研 SPI（SpringFactories）/ 自研 JDK 工具（原生覆盖）/ 本地 LRU Caffeine（决策不用——多实例一致性）/ Native（P3-3 例外——触发条件）/ JPA（MyBatis 精确 SQL）

---

## 七、状态追踪

| 项 | 状态 | 完成日期 | 备注 |
|:---:|:---:|:---:|------|
| P1-1 | ⬜ | | |
| P1-2 | ⬜ | | |
| P1-A | ⬜ | | |
| P1-C | ⬜ | | |
| P2-1~P2-8 | ⬜ | | |
| P2-A~P2-I | ⬜ | | |
| P3-1~P3-15 | ⬜ | | |
| P3-A | ⬜ | | |
