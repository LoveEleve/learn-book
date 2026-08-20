# Phase 6：云原生与运维体系（第 41-46 周）

## 目标
补齐运维侧能力，让 my-xhs 达到生产级标准。

---

## Week 41：SkyWalking → OpenTelemetry 全面迁移

### 迁移方案

**为什么换？**
- OpenTelemetry 是 CNCF 标准，SkyWalking 协议私有
- OTel 统一的 Traces + Metrics + Logs 协议（OTLP）
- 生态更广：Grafana/Jaeger/Datadog/New Relic 都原生支持

**迁移步骤**：
1. 移除 SkyWalking Agent：删除 `-javaagent:skywalking-agent.jar` JVM 参数
2. 引入 OTel Java Agent：`-javaagent:opentelemetry-javaagent.jar`
3. 配置 OTLP Exporter：
```properties
otel.exporter.otlp.endpoint=http://otel-collector:4317
otel.service.name=my-xhs-order
otel.traces.sampler=parentbased_always_on
```
4. 部署 OTel Collector（接收 → 处理 → 导出）：
```yaml
# docker-compose.yaml
otel-collector:
  image: otel/opentelemetry-collector-contrib:latest
  volumes:
    - ./otel-collector-config.yaml:/etc/otel/config.yaml
```
5. 部署 Grafana LGTM 栈：
```yaml
tempo:   # Trace 存储
loki:    # Log 存储  
mimir:   # Metrics 存储
grafana: # 统一 UI，DataSource 配置 Tempo/Loki/Mimir
```

### 手动埋点
```java
// OrderService.createOrder
Span span = tracer.spanBuilder("order.create")
    .setAttribute("order.id", orderId)
    .setAttribute("user.id", userId)
    .startSpan();
try (Scope scope = span.makeCurrent()) {
    // 业务逻辑
    span.setAttribute("order.status", "success");
} catch (Exception e) {
    span.setStatus(StatusCode.ERROR);
    span.recordException(e);
} finally {
    span.end();
}
```

### 实践任务
1. SkyWalking → OTel 完整迁移（注意：my-xhs 当前同时运行 SkyWalking 和你的 OTel 做对比，不要删除 SkyWalking）
2. 搭建 Grafana 5 个 Dashboard：API 概览 / JVM 监控 / 中间件健康 / 业务指标 / 链路拓扑
3. 压测对比 SkyWalking vs OTel 的 Agent 开销（CPU/Memory）

---

## Week 42：生产级日志体系

### 结构化日志改造
```xml
<!-- logback-spring.xml 改为 JSON 输出 -->
<appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
        <providers>
            <timestamp/>
            <logLevel/>
            <loggerName/>
            <threadName/>
            <mdc/>             <!-- MDC 自动注入 traceId/userId/spanId -->
            <message/>
            <stackTrace/>
        </providers>
    </encoder>
</appender>
```

### MDC TraceId 注入
```java
// TraceIdFilter (for incoming HTTP requests)
MDC.put("traceId", request.getHeader("X-Trace-Id"));
MDC.put("userId", request.getHeader("X-User-Id"));
MDC.put("spanId", UUID.randomUUID().toString());
try {
    chain.doFilter(request, response);
} finally {
    MDC.clear();
}
```

### ELK Pipeline
```
Filebeat（采集日志文件）
  → Logstash（解析/过滤/脱敏/JSON 格式化）
    → Elasticsearch（存储 + 索引）
      → Kibana（查询 + 可视化）
```

### 日志脱敏
```ruby
# logstash.conf 脱敏规则
filter {
  mutate {
    gsub => [
      "message", "\b(\d{15,19})\b", "****-****-****-****",  # 银行卡号
      "message", "\b1[3-9]\d{9}\b", "1*********",            # 手机号
      "message", "(password|token)=[^&\\s]+", "\1=****"       # 密码/Token
    ]
  }
}
```

### 实践任务
1. 15 个微服务全部改为 JSON 结构化日志输出
2. 改造 MDC：确保所有 HTTP/Feign/MQ 请求都携带 traceId
3. 搭建 ELK 环境 + 日志脱敏规则

---

## Week 43：安全体系加固

### 安全走读 my-xhs 现有防护
```
my-xhs-gateway/.../GatewayAuthFilter.java    # JWT 鉴权
my-xhs-gateway/.../HmacAuthFilter.java       # HMAC 签名
my-xhs-common/.../SqlGuardInterceptor.java   # SQL 注入防护
```

### OAuth2.0 + OIDC 集成
- Authorization Code Flow + PKCE（移动端）
- Spring Security Resource Server 配置
- OIDC Discovery 端点：自动获取 token/authorize/userinfo/end_session

### 微信社交登录
```java
// SocialLoginService
public LoginResult loginWithWechat(String code) {
    // Step 1: code → access_token + openid
    // Step 2: access_token + openid → userinfo (unionid)
    // Step 3: unionid → my-xhs user (创建或关联)
    // Step 4: 签发 JWT Token
}
```

### 安全渗透测试
```java
@SpringBootTest
class SecurityPenetrationTest {
    @Test void testSqlInjection() { /* ' OR '1'='1 */ }
    @Test void testXssAttack() { /* <script>alert(1)</script> */ }
    @Test void testCsrfToken() { /* 跨站请求伪造 */ }
    @Test void testExpiredToken() { /* 过期 JWT 测试 */ }
    @Test void testReplayAttack() { /* Nonce 防重放 */ }
}
```

### 实践任务
1. 安全渗透测试 5 个场景
2. OAuth2.0 微信登录集成
3. 镜像安全扫描：Trivy 扫描所有 Docker 镜像

---

## Week 44：混沌工程实战

### 混沌实验矩阵（10 个 CER）
| # | 场景 | 工具 | 注入方式 | 验证指标 | 预期行为 |
|---|------|------|---------|---------|---------|
| 1 | 网络延迟 200ms | ChaosBlade | tc qdisc | P99 ≤ 500ms | 超时重试 |
| 2 | 网络延迟 3000ms | ChaosBlade | tc qdisc | 熔断触发 | 断路器打开 |
| 3 | 网络丢包 30% | ChaosBlade | tc qdisc | 成功率 > 90% | 重试 + 降级 |
| 4 | CPU 满载 90% | ChaosBlade | stress | CPU 节流 | HPA 扩容 |
| 5 | Pod Kill | kubectl delete pod | - | 恢复时间 < 30s | K8s 自动重启 |
| 6 | Redis 主从切换 | redis-cli failover | - | 恢复时间 < 15s | Sentinel 切换 |
| 7 | Redis Cluster 单节点宕机 | kubectl delete pod | - | 服务正常 | 槽位迁移 |
| 8 | MySQL 主库宕机 | kubectl delete pod | - | 恢复时间 < 60s | 主从切换 |
| 9 | RocketMQ Broker 宕机 | kubectl delete pod | - | 消费不丢消息 | DLedger 切换 |
| 10 | Nacos 全部宕机 | kubectl delete pod -n nacos | - | 服务正常运行 | 本地缓存 |

### my-xhs 混沌代码走读
```
my-xhs-common/.../chaos/
├── ChaosInterceptor.java         // 混沌注入拦截器
└── ChaosProperties.java          // 混沌配置
```

### 实践任务
1. 执行 10 个混沌实验，每个实验记录：注入时间、恢复时间（RTO）、数据丢失（RPO）、改进清单
2. 输出：《my-xhs 混沌工程实验报告》

---

## Week 45：成本优化

### JVM 内存调优
```bash
# 轻量级服务（gateway / counter）
-Xms256m -Xmx512m -XX:MaxMetaspaceSize=128m

# 中等负载（user / content / product / coupon / analytics / notification / im）
-Xms512m -Xmx1024m -XX:MaxMetaspaceSize=256m

# 高负载（order / inventory / payment / search / home）
-Xms1024m -Xmx2048m -XX:MaxMetaspaceSize=384m
```

### GC 选型建议
- Gateway/Counter：ZGC（<1ms 停顿）
- Order/Inventory：G1（平衡吞吐和延迟）
- 其他服务：G1 默认

### 容器资源精准化
```yaml
# gateway（轻量级）
resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 500m
    memory: 1Gi
```

### K8s HPA 配置
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
spec:
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300   # 缩容稳定窗口
```

### 实践任务
1. JVM 内存 + GC 三套参数模板对比压测
2. 15 微服务容器 Limit/Request 精准化
3. HPA 弹性伸缩验证：CPU → 扩容 + 缩容
4. 输出：成本优化前后对比（目标：月度成本降低 40%）

---

## Week 46：my-xhs V3 交付周

### V3 交付物清单
1. **生产级运维手册**（10 章）：
   - 第 1 章：系统架构总览
   - 第 2 章：部署指南（Docker Compose + K8s）
   - 第 3 章：配置管理（Nacos 配置清单）
   - 第 4 章：监控告警（Grafana Dashboard + AlertManager 规则）
   - 第 5 章：日志平台（ELK 使用指南）
   - 第 6 章：故障处理 SOP（10 个常见故障的处理流程）
   - 第 7 章：灰度发布流程
   - 第 8 章：备份与恢复
   - 第 9 章：容量规划
   - 第 10 章：紧急联系人

2. **Docker Compose + K8s 双模部署**：
   - Docker Compose：本地开发一键启动
   - K8s：生产部署（Deployment + Service + Ingress + HPA + ConfigMap）

3. **混沌工程验证报告**

4. **安全测试报告**

5. **成本优化报告**

---

## Phase 6 检验标准
- [ ] SkyWalking → OTel 迁移完成，5 个 Grafana Dashboard 可用
- [ ] 15 个微服务全部 JSON 结构化日志 + MDC TraceId
- [ ] 安全渗透测试 5 个场景全部通过
- [ ] 10 个混沌实验全部执行，有 RTO/RPO 记录
- [ ] 月度成本降低 ≥ 40%
- [ ] K8s 双模部署可用
- [ ] 完整运维手册
