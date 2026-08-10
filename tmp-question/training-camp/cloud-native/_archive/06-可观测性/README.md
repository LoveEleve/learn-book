# 06 可观测性（Week 12-13 · 5课时）

## 学完能干什么
能搭出 Prometheus + Grafana 监控平台，理解指标/日志/链路的采集原理，知道 2026 年用什么替代过时的 Sleuth。

---

## 第 1-2 课时：Micrometer 指标体系

### 课时 27：Micrometer 核心模型

**📖 读什么**
- `stage-1/docs/13. 第十三节：Micrometer 基础.md`（全文）
- `stage-1/docs/14. 第十四节：Micrometer 整合第三方框架.md`（全文）

**四大指标类型**：

| 类型 | 含义 | 示例 |
|------|------|------|
| Counter | 只增不减的计数 | HTTP 请求总数 |
| Gauge | 瞬时值 | 当前连接数、队列长度 |
| Timer | 时间段分布 | 请求耗时 P50/P99 |
| DistributionSummary | 值分布 | 消息大小分布 |

**MeterRegistry 注册表**：所有指标的存储中心，Prometheus/InfluxDB/JMX 各有自己的 Registry 实现。

**MeterBinder 自动注册**：JVM/Kafka/Logging/Tomcat 等内建 Binder 无需手动编码。

---

### 课时 28：三方框架整合

**🔍 看什么代码**

| 文件 | 整合了什么 |
|------|-----------|
| `biz-api/.../micrometer/MicrometerConfiguration.java` | 基础配置 |
| `biz-api/.../micrometer/binder/feign/FeignCallCounterMetrics.java` | Feign 调用计数 |
| `biz-api/.../micrometer/binder/servo/ServoMetrics.java` | Ribbon Servo→Micrometer 桥接 |
| `biz-web/.../data/bean/RedisOperationMetricsInterceptor.java` | Redis 操作监控 |
| `biz-api/.../jdbc/wrapper/DataSourceWrapper.java` | JDBC 包装模式 |

**三步整合法则**：
- JDBC → DataSource 代理 → 记录连接获取/释放时间
- MyBatis → Plugin 拦截 → 记录 SQL 执行时间
- Redis → Connection 拦截 → 记录命令耗时

---

## 第 3-4 课时：Prometheus 双模式

### 课时 29：Pull 模式

**📖 读什么**
- `stage-1/docs/15. 第十五节：基于 Pull 方式指标监控平台设计.md`

**配置要点**：
```properties
management.endpoints.web.exposure.include=*
```
依赖 `micrometer-registry-prometheus` 后访问 `/actuator/prometheus` 即可看到 Prometheus 格式输出。

**服务发现**：Prometheus 通过 Eureka `sd_configs` 自动发现所有微服务实例，无需手动配置 target。

**动手**：启动 biz-web 后访问 `http://localhost:8080/actuator/prometheus`，看输出格式。

---

### 课时 30：Push 模式

**📖 读什么**
- `stage-1/docs/16. 第十六节：基于 Push 方式指标监控平台设计.md`

**Pushgateway 场景**：短生命周期任务（Job/CronJob），来不及被 Prometheus Pull 就结束了。

**Pull vs Push 选型**：
- 长生命周期服务 → Pull（避免 Pushgateway 成为单点）
- 短生命周期任务 → Push（必须主动推送）
- InfluxDB 作为时序数据库的另一个选择

---

### 补充：microsphere-observability

**🔍 看什么代码**
```bash
cd cloud-native-code/projects/microsphere-observability && mvn compile
```

| 组件 | 作用 |
|------|------|
| `Log4j2AutoConfiguration` | Log4j2 Kafka Appender 自动配置（topic: "java-app-logs"） |
| `SentinelMetricsConfiguration` | Sentinel → Prometheus Collector 指标桥接 |
| `CGGroupConfiguration` | CGroup 容器内存自动检测 |
| `PrometheusMetricsConfiguration` | Prometheus PushGateway 推模式 |

---

## 第 5 课时：链路追踪原理

### 课时 31：Span 模型 + 字节码增强

**📖 读什么**
- `stage-1/docs/17. 第十七节：基于 Java 应用层追踪服务链路.md`（全文）
- `stage-1/docs/18. 第十八节：基于 Java Instrument 追踪服务链路重构.md`（全文）

**Span 模型**（不过时）：
```
Trace ID（全局唯一）→ 包含多个 Span
每个 Span = {traceId, spanId, parentSpanId, timestamp, duration, tags}
```

**传递方式**：
- HTTP → 请求头 `X-B3-TraceId`
- RPC → Dubbo `RpcContext` / gRPC Metadata

**ByteBuddy 字节码增强**（两种实现）：
1. BeanFactory 阶段：修改 `BeanDefinition.setBeanClass()` 为 ByteBuddy 子类
2. BeanPostProcessor 阶段：MethodDelegation + @Pipe 委托给 LoggingBeanInterceptor

**2026 年等价物**：Sleuth 已从 Spring Boot 3.x 移除，换用 **Micrometer Tracing + OpenTelemetry**。

| 概念 | Sleuth | OpenTelemetry |
|------|--------|-------------|
| 追踪 API | Tracer / Span | Tracer / Span |
| 头部传播 | `X-B3-TraceId` | `traceparent` (W3C 标准) |
| 导出端 | Zipkin | OTLP → Jaeger / Tempo / Zipkin |
| 自动注入 | Sleuth AutoConfiguration | OpenTelemetry Java Agent |

概念完全相通，训练营教的 Span 模型和字节码增强原理 100% 适用。

---

### 课时 32：ELK 日志平台

**📖 读什么**
- `stage-3/docs/29. 第十九节：日志平台.md`

**日志管道**：
```
Java App (logback-kafka-appender) → Kafka → Logstash → Elasticsearch → Kibana
```

**为什么经 Kafka**？解耦 + 削峰——Elasticsearch 写入慢时不会丢日志。

**动手**：
```bash
# docker-elk 一键部署
# logback.xml 添加 KafkaAppender
```

---

## 本阶段自检清单
- [ ] 能说清 Counter 和 Gauge 的本质区别
- [ ] 能配出一套 Micrometer + Prometheus + Grafana
- [ ] 能画图解释 Trace ID 在三层服务间怎么传递
- [ ] 知道 Pull 和 Push 模式各适合什么场景
- [ ] 知道 2026 年 Sleuth → OTel 的等价替换
