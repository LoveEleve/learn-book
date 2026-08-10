# 07 容错模式（Week 14 · 5课时）

## 学完能干什么
理解断路器状态机、舱壁隔离、限流算法的原理。知道 2026 年各层容错的分工——什么用 Istio、什么自己写代码。

---

## 2026 年容错分工

```
HTTP 调用的熔断/重试/超时 → Istio/Envoy（无需改代码）
入口流量限流/降级          → Gateway/Kong
进程挂了自动重启            → K8s health check + restart
DB 连接池熔断/业务降级       → 业务代码自己写
```

---

## 课时 33：断路器状态机（⭐ 核心）

**手画三遍这个图，必须刻在脑子里**：

```
      成功次数达标
  CLOSED ─────────→ (正常处理请求)
    │ 失败率超过阈值
    ▼
  OPEN ──────────→ (立即拒绝所有请求)
    │ 超时时间到
    ▼
  HALF_OPEN ────→ (允许少量请求试探)
    │ 成功 → CLOSED（恢复正常）
    │ 失败 → OPEN（继续拒绝）
```

---

## 课时 34：Resilience4j 完整实现

**📖 读什么**
- `stage-1/docs/08. 第八节：基于 Resilience4j 实现 Web 服务容错性.md`
- `stage-1/docs/09. 第九节：Resilience4j 整合第三方框架.md`

**🔍 看什么代码**

| 文件 | 容错方式 |
|------|---------|
| `biz-web/.../servlet/filter/GlobalCircuitBreakerFilter.java` | Servlet Filter 级别全局熔断 |
| `biz-web/.../servlet/filter/ResourceCircuitBreakerFilter.java` | 反射获取 servlet 名做细粒度熔断 |
| `biz-web/.../servlet/mvc/interceptor/GlobalBulkheadHandlerInterceptor.java` | HandlerInterceptor 级别全局舱壁 |
| `biz-web/.../servlet/mvc/interceptor/ResourceBulkheadHandlerInterceptor.java` | ContextRefreshedEvent 扫描所有 @RequestMapping |
| `biz-data/.../fault/tolerance/mybatis/Resilience4jMyBatisInterceptor.java` | MyBatis Executor 包装 |
| `biz-data/.../fault/tolerance/mybatis/CircuitBreakerExecutorDecorator.java` | before/after 熔断 |

**ChainableResilience4jFacade 责任链**（microsphere-resilience4j）：
```
CircuitBreaker(Retry(RateLimiter(实际调用)))
```
CallbackChain 按 position 递归调用，最后一个 filter 执行真实回调。

---

## 课时 35：JMX 指标驱动的负载均衡

**📖 读什么**
- `stage-1/docs/11. 第十一节：基于监控指标的负载均衡实现.md`
- `stage-1/docs/12. 第十二节：基于动态权重的负载均衡实现.md`

**🔍 看什么代码**

| 文件 | 算法 |
|------|------|
| `biz-client/.../cloud/loadbalancer/CpuUsageLoadBalancer.java` | 从 metadata 读 cpu-usage 选实例 |
| `biz-client/.../loadbalancer/UserServiceServiceInstanceListSupplier.java` | 自定义 ListSupplier |
| `biz-client-ribbon/.../ribbon/UserServiceRibbonClientConfiguration.java` | WeightedResponseTimeRule |

**JMX 指标采集**：
- `OperatingSystemMXBean.getProcessCpuLoad()` → CPU 使用率
- `ThreadMXBean.getThreadCount()` → 线程数
- 综合 CPU + RT + QPS 三指标计算动态权重

---

## 课时 36：动态变更

**📖 读什么**
- `stage-1/docs/10. 第十节：服务容错性动态变更设计.md`

**动态 Tomcat 配置**：`DynamicTomcatConfiguration` 监听 `EnvironmentChangeEvent`，直接调 `AbstractProtocol.setMaxThreads()`。

---

## 课时 37：Istio Service Mesh

**📖 读什么**
- `stage-3/docs/21. 第十五节：Istio.md`

**Istio 接管什么**：

| 功能 | Istio 配置 | 对应代码实现 |
|------|-----------|-------------|
| 熔断 | `DestinationRule` → connectionPool/tcp/http | Resilience4j CircuitBreaker |
| 重试 | `VirtualService` → retries | Resilience4j Retry |
| 超时 | `VirtualService` → timeout | Spring `@Transactional` timeout |
| 灰度 | `VirtualService` → weight/mirror | 自定义 Router |

**为什么 2026 年 Istio 是首选**：不用改代码、统一管控、自动注入 Envoy Sidecar。

---

## 现代等价对照

### segfault-lessons Hystrix（lesson-8/9/10）→ 2026 等价物

| Hystrix 概念 | 2026 等价物 |
|-------------|-----------|
| HystrixCommand | `@CircuitBreaker(name="xxx")` (Resilience4j) |
| Hystrix Dashboard | Sentinel Dashboard / Grafana 面板 |
| Hystrix Thread Pool 隔离 | Bulkhead + ThreadPoolTaskExecutor |
| Hystrix Fallback | `@CircuitBreaker(fallbackMethod="xxx")` |
| Hystrix 指标流 | Micrometer → Prometheus |

**概念留、API 换**。lesson-10 手写 HystrixCommand 继承类帮你理解断路器怎么工作，但生产用 Resilience4j。

---

## 本阶段自检清单
- [ ] 能手画断路器状态机三种状态转换
- [ ] 知道 Semaphore Bulkhead 和 ThreadPool Bulkhead 的取舍
- [ ] 能区分哪些容错用 Istio、哪些必须自己写代码
- [ ] 理解 HystrixCommand 继承类的设计模式（ThreadPool 隔离 + Fallback）
