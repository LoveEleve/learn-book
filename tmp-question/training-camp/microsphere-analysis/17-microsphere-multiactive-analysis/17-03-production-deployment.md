# 17-03：异地多活生产落地指南

> **核心命题**：源码分析和架构对比解决的是"能不能做"的问题，生产落地解决的是"做得好不好"的问题。Gateway 区域路由、优雅切换、熔断配合、监控告警、灾备演练——这些才是决定异地多活系统能不能在生产环境稳定运行的关键。
> **本文覆盖**：8 个生产落地场景 + 配置清单 + 灾备方案。

---

## 场景一：Spring Cloud Gateway 区域路由

### 问题

Gateway 是微服务的总入口。一个请求到达 Gateway 后，Gateway 需要决定路由到哪个区域的下游服务。

```
客户端请求 -> Gateway (beijing-1)
  -> 下游服务有: beijing-1(6个实例), shanghai-1(4个实例)
  -> 期望: Gateway 优先路由到 beijing-1 的实例
  -> 实际: 取决于 Gateway 的 LoadBalancer 配置
```

Gateway 本身也是一个 Spring Cloud 应用，它的 LoadBalancer 同样会经过 ZonePreferenceServiceInstanceListSupplier。但有两个问题：

### 问题一：Gateway 的区域感知

Gateway 部署在北京机房，ZoneLocator 返回 zone = "beijing-1"。Gateway 的 RouteLocator 配置了 `lb://user-service`。当 Gateway 转发请求时，LoadBalancer 拿到的 ServiceInstance 列表中包含北京的 user-service 实例和上海的 user-service 实例。ZonePreferenceFilter 过滤后，北京的实例被优先选中。

这不需要额外的配置——ZonePreferenceServiceInstanceListSupplier 已经注册在默认的 LoadBalancer 配置中。但 Gateway 需要确保 LoadBalancer 配置被正确加载：

```yaml
spring:
  cloud:
    loadbalancer:
      configurations: optimized-zone-preference  # 启用 ZonePreferenceServiceInstanceListSupplier
    gateway:
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
```

### 问题二：Gateway 的 zone 信息来源

Gateway 的 zone 来源和普通微服务一样——通过 ZoneLocator 自动检测。但 Gateway 通常部署在独立的网络层，可能和业务服务的部署环境不同。

```
K8s 集群 1 (beijing) 中部署:
  - Gateway Pod (zone = beijing-1)
  - user-service Pod (zone = beijing-1)
  - content-service Pod (zone = beijing-1)

K8s 集群 2 (shanghai) 中部署:
  - Gateway Pod (zone = shanghai-1)
  - user-service Pod (zone = shanghai-1)
  - content-service Pod (zone = shanghai-1)
```

每个集群中的 Pod 通过 K8s Downward API 注入 `topology.kubernetes.io/zone` 到环境变量（如 `MYXHS_ZONE=beijing-1`）。ZoneLocator 的 DefaultZoneLocator 配置为优先读取环境变量，兜底读 application.yml。

如果 Gateway 和业务服务不在同一个部署单元中（例如 Gateway 在物理机负载均衡上，业务服务在 K8s 中），Gateway 的 zone 需要手动配置：

```yaml
myxhs:
  availability:
    zone: beijing-1
```

### 问题三：Gateway 的跨区域重试

Gateway 转发请求到下游服务时，如果同区域的下游服务全部不可用。

默认的 LoadBalancer 行为：从 ZonePreferenceFilter 过滤后的列表中选中一个实例。请求失败后，LoadBalancer 重试下一个实例。当重试完所有"同区域实例"后，请求失败。

**不会 fallback 到其他区域的实例**——ZonePreferenceFilter 已经把其他区域的实例过滤掉了。LoadBalancer 只在 ZonePreferenceFilter 返回的列表中重试。

解决这个问题有 3 种方案：

**方案 A：配合重试 + 降级**

```yaml
spring:
  cloud:
    loadbalancer:
      retry:
        enabled: true
        maxRetriesOnSameServiceInstance: 1
        maxRetriesOnNextServiceInstance: 3  # 同区域有 3 个实例时，最多重试 3 次
```

同区域实例全部失败后，下游服务返回 5xx。Gateway 的 fallback 机制可以返回缓存的响应或提示信息。

**方案 B：调整 same-zone-min-available**

如果同区域实例数波动大（如滚动更新期间），降低 same-zone-min-available 或根据实例数动态调整。

**方案 C：两级路由——先区域后实例**

Gateway 层不做区域偏好路由，只做负载均衡。把区域偏好路由放在第一跳内部服务上。

```
客户端 -> Gateway (不做区域偏好，随机路由)
  -> 第一跳内部服务 A (随机选择)
    -> 服务 A 调用服务 B 时 ZonePreferenceFilter 生效

方案缺点：第一跳的延迟不可控（可能路由到跨区域实例）
```

方案 A 是最实用的。同区域实例全挂是极端场景，对这部分请求返回降级响应比跨区域调用更合理（跨区域调用可能导致连锁故障）。

### 配置示例

完整的 Gateway 区域路由配置：

```yaml
spring:
  cloud:
    loadbalancer:
      configurations: optimized-zone-preference
      retry:
        enabled: true
        maxRetriesOnSameServiceInstance: 1
        maxRetriesOnNextServiceInstance: 3
    gateway:
      default-filters:
        - name: Retry
          args:
            retries: 3
            statuses: BAD_GATEWAY, SERVICE_UNAVAILABLE, GATEWAY_TIMEOUT
            methods: GET, POST, PUT, DELETE
      routes:
        - id: user-service
          uri: lb://user-service
          predicates:
            - Path=/api/users/**
          filters:
            - name: CircuitBreaker
              args:
                name: userServiceCircuitBreaker
                fallbackUri: forward:/fallback/users

myxhs:
  availability:
    zone: beijing-1
    preference:
      enabled: true
      upstream:
        zone-ready-percentage: 80
        same-zone-min-available: 3
```

---

## 场景二：区域切换的优雅关闭

### 问题

区域切换时（从 beijing-1 切换到 shanghai-1），现有连接怎么处理？

```
切换命令发出
  -> ZoneContext.setZone("shanghai-1")
    -> DynamicDataSource.onZoneChanged()
      -> waitForActiveConnections()  // my-xhs 的机制
      -> switchDataSource("shanghai-1")
```

my-xhs 的 DynamicDataSource 有 `waitForActiveConnections()` 机制，等待进行中的数据库操作完成后再切换。但还有 4 个资源同样需要优雅关闭：

### 需要优雅关闭的资源

| 资源 | 关闭方式 | 超时时间 |
|------|---------|---------|
| DataSource 连接池 | 等待活跃连接完成，然后 close() | 30s（my-xhs 已有） |
| Redis 连接池 | 等待正在执行的命令完成，close() | 10s |
| HTTP 客户端连接池 | 等待正在进行的请求完成，close() | 30s |
| MQ 消费者 | 等待消息处理完成，暂停消费 | 60s |
| WebSocket 连接 | 通知客户端重连 | 5s |

### Spring 的优雅关闭机制

Spring Boot 2.3+ 支持优雅关闭（graceful shutdown）：

```yaml
server:
  shutdown: graceful  # 默认是 immediate

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 每阶段关闭等待时间
```

但区域切换不是 JVM 重启——它不需要关闭 Tomcat、不需要销毁 Spring 容器。区域切换只需要**切换数据源连接**，不需要停止接受新请求。

### 区域切换的优雅关闭流程

```java
public void switchDataSource(String newZone) {
    // 1. 标记自己为不健康（可选）
    //    Actuator 的 /actuator/health 端点开始返回 DOWN
    //    LoadBalancer 不再向这个实例发送新请求
    
    // 2. 等待活跃连接完成
    waitForActiveConnections();  // 已有机制，最长等待 30 秒
    
    // 3. 切换数据源
    synchronized (mutex) {
        DataSource old = this.delegate;
        this.delegate = newDataSource;
        this.currentZone = newZone;
        // 4. 关闭旧数据源连接池
        if (old instanceof DisposableBean) {
            ((DisposableBean) old).destroy();
        }
    }
}
```

### 关不关旧连接池？

有争议。两种做法：

**做法 A：立即关闭旧连接池**

```
switchDataSource("shanghai-1")
  -> 关闭 beijing-1 的连接池（所有连接立即关闭）
  -> 打开 shanghai-1 的连接池
```

优点：资源立即释放
缺点：如果切换后马上又切回北京，需要重新创建连接池，增加切换延迟

**做法 B：延迟关闭旧连接池**

```
switchDataSource("shanghai-1")
  -> 保留 beijing-1 的连接池（存到 standby 映射中）
  -> 打开 shanghai-1 的连接池
  -> 30 秒后如果还没有切回北京，关闭 beijing-1 的连接池
```

优点：快速回切（保留旧连接池，直接切换 delegate）
缺点：30 秒内双倍连接数

**my-xhs 的 DynamicDataSource 当前没有关闭旧连接池。** `switchDataSource()` 只切换 delegate 指针，旧的 DataSource 仍然在 `zoneDataSources` Map 中引用着，连接不会被回收。建议改为做法 B：关闭旧连接池但保留引用，30 秒后再 close()。

---

## 场景三：和 Sentinel/Resilience4j 的配合

### 问题

从 17-02 的面试题 8 我们已经知道：ZonePreferenceFilter 不感知 CircuitBreaker 的熔断状态。同区域实例被熔断后，ZonePreferenceFilter 仍然返回这些实例。

### 生产级别的解法

解法一（17-02 已讲过）：LoadBalancer 级别重试——在同区域实例中被熔断，重试其他同区域实例。全部被熔断后，请求失败。

解法二（生产级别的方案）：**实例级别 + 区域级别的双层熔断**

第一层：实例级别的 CircuitBreaker——单个实例异常时熔断这个实例
第二层：区域级别的 CircuitBreaker——整个区域的实例都异常时熔断这个区域

区域级别的 CircuitBreaker 可以通过 `disabled-zone` 手动实现。运维监控到某个区域的错误率上升后，在配置中心设置 `disabled-zone=beijing-1`，ZonePreferenceFilter 自动过滤掉北京的所有实例。

### Sentinel 的配合

Sentinel 可以用来实现区域级别的监控和告警：

```java
@Component
public class ZoneHealthMonitor {
    private final ZoneContext zoneContext;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelay = 10000)
    public void monitorZoneHealth() {
        String currentZone = zoneContext.getZone();
        // 监控同区域调用的错误率
        double errorRate = meterRegistry
            .get("sentinel.flow.pass")
            .tag("zone", currentZone)
            .gauge()
            .value();
        
        // 错误率高于阈值时发出告警（不自动切换）
        if (errorRate > 0.5) {
            alertService.send(
                "Zone " + currentZone + " error rate: " + errorRate,
                AlertLevel.WARNING
            );
        }
    }
}
```

### Resilience4j 的配合

Resilience4j 的 CircuitBreaker 需要和 ZonePreferenceFilter 的 `same-zone-min-available` 配合使用。

```yaml
resilience4j:
  circuitbreaker:
    instances:
      userService:
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 30s

myxhs:
  availability:
    zone:
      preference:
        upstream:
          same-zone-min-available: 5
          # 如果同区域有 6 个实例，被熔断 4 个后剩下 2 个
          # 2 < 5，ZonePreferenceFilter 不再偏好同区域
          # 流量分散到其他区域
```

注意：same-zone-min-available 不感知熔断，它只看 ZonePreferenceFilter 可用的实例数（从 DiscoveryClient 获取的列表）。CircuitBreaker 不修改 DiscoveryClient 返回的实例列表——熔断后的实例仍然在列表中，只是调用时立即返回 `CallNotPermittedException`。所以 same-zone-min-available 不会因为熔断而自动调整偏好。解法是手动通过 `disabled-zone` 禁用故障区域。

---

## 场景四：配置切换的生产清单

### 配置中心切换步骤

```
1. 确认目标区域的 Redis/DB 可用
2. 确认目标区域的服务实例已注册到 Nacos
3. 切换目标区域的 same-zone-min-available
4. 切换 zone
5. 观察错误率
6. 确认无误后禁用源区域
```

### 回滚步骤

```
1. 把 zone 改回源区域（或 "originalZone" 触发自动检测）
2. 删除 disabled-zone 配置
3. 观察错误率
```

### 配置模板

Nacos 配置 data-id: myxhs-zone.yml

```yaml
# 正常状态
myxhs:
  availability:
    zone: beijing-1
    preference:
      enabled: true
      upstream:
        zone-ready-percentage: 100
        same-zone-min-available: 5
        disabled-zone:

# 切换状态（只需要改 3 行）
myxhs:
  availability:
    zone: shanghai-1            # ← 改：目标区域
    preference:
      enabled: true
      upstream:
        zone-ready-percentage: 80    # ← 改：放宽到 80%，更多实例参与
        same-zone-min-available: 3   # ← 改：降低阈值，更快切换
        # disabled-zone:             # ← 先不填，确认后再填

# 确认稳定后，禁用源区域
myxhs:
  availability:
    zone: shanghai-1
    preference:
      enabled: true
      upstream:
        disabled-zone: beijing-1     # ← 加：禁用北京

# 恢复状态（回滚）
myxhs:
  availability:
    zone: originalZone           # ← 改：触发自动定位
    preference:
      enabled: true
      upstream:
        zone-ready-percentage: 100
        same-zone-min-available: 5
        # disabled-zone:         # ← 删：不再禁用
```

恢复时的事件链（跨模块）：

```
配置中心改 zone=originalZone
  → 17-multiactive 的 ZoneContextChangedListener 收到 EnvironmentChangeEvent
    → 发现值是 "originalZone"
    → 调用 ZoneLocator.locate() 重新自动检测区域
    → ZoneContext.setZone("beijing-1") 触发 PropertyChangeSupport
      → 发布 ZoneContextChangedEvent
        → 18-dynamic 的 PropagatingDynamicJdbcConfigChangedEventListener 捕获
          → 对每个 HA 数据源配置发布 DynamicJdbcConfigChangedEvent
            → DynamicDataSource.RefreshingDynamicDataSourceListener
              → 重建子上下文 → 原子切换 delegate → 异步关闭旧上下文
            → LoadBalancer 侧：下一个请求走 ZonePreferenceFilter
              → zoneContext.getZone() 返回 "beijing-1"
              → 偏好 beijing-1 实例
```

### 切换前检查清单

| 检查项 | 确认方法 | 期望结果 |
|--------|---------|---------|
| 目标区域服务实例 | Nacos 控制台 | 服务列表中有目标区域的实例 |
| 目标区域数据库 | telnet ip port | 可达 |
| 目标区域 Redis | ping | 可达 |
| 目标区域 MQ | 控制台 | 正常 |
| 数据同步延迟 | 监控 | < 5s |
| 错误率基线 | Prometheus | < 1% |
| 当前活跃连接数 | /actuator/metrics | < 100 |

---

## 场景五：监控与告警

### 需要监控的指标

| 指标 | 来源 | 告警阈值 |
|------|------|---------|
| ZoneContext 变更次数 | ZoneContext 日志 | 1 次/变更 |
| 区域偏好命中率 | LoadBalancer 指标 | < 80%（说明区域路由不生效） |
| 跨区域请求比例 | 调用链追踪 | 根据部署比例 |
| 同区域实例数 | Nacos | < 5 |
| DynamicDataSource 切换次数 | DynamicDataSource 日志 | 应等于 ZoneContext 变更次数 |
| DataSource 切换等待超时次数 | DynamicDataSource 日志 | > 0 时需要关注 |

### 区域偏好命中率监控

```java
@Component
public class ZonePreferenceMetrics {
    private final MeterRegistry meterRegistry;
    private final ZoneContext zoneContext;

    private final AtomicLong sameZoneCalls = new AtomicLong(0);
    private final AtomicLong crossZoneCalls = new AtomicLong(0);

    public void recordCall(String instanceZone) {
        String currentZone = zoneContext.getZone();
        if (currentZone.equals(instanceZone)) {
            sameZoneCalls.incrementAndGet();
        } else {
            crossZoneCalls.incrementAndGet();
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void recordMetrics() {
        long total = sameZoneCalls.get() + crossZoneCalls.get();
        if (total > 0) {
            double hitRate = (double) sameZoneCalls.get() / total;
            meterRegistry.gauge("zone.preference.hit-rate", hitRate);
            // 同区域命中率低于 50% 时发出告警
            if (hitRate < 0.5) {
                alertService.send(
                    "Zone preference hit rate below 50%: " + hitRate,
                    AlertLevel.WARNING
                );
            }
        }
    }
}
```

### 区域切换事件日志

每次区域切换应该留下完整的审计记录：

```json
{
  "event": "ZONE_SWITCH",
  "from": "beijing-1",
  "to": "shanghai-1",
  "trigger": "NACOS_CONFIG_CHANGE",
  "timestamp": "2026-07-30T10:00:00Z",
  "operator": "admin",
  "component": "ZoneContextChangedListener",
  "downstream": [
    {"component": "DynamicDataSource", "result": "SUCCESS", "duration_ms": 5200},
    {"component": "RedisConnection", "result": "SUCCESS", "duration_ms": 1500}
  ]
}
```

### Prometheus + Grafana 面板

建议在 Grafana 中创建异地多活 Dashboard：

```
面板 1：区域偏好命中率（当前值 + 24 小时趋势）
面板 2：各区域实例数（按服务分组）
面板 3：跨区域请求延迟分布
面板 4：切换事件时间线
面板 5：DynamicDataSource 连接池状态
面板 6：ZoneContext 的 enabled/preferenceEnabled 状态
```

---

## 场景六：灾备演练

### 日常演练

每季度执行一次区域切换演练。流程：

```
1. 通知：提前 72 小时通知所有依赖方
2. 预检查：按切换前检查清单确认
3. 执行：在配置中心改 zone
4. 观察：确认 LoadBalancer 路由切换、确认 DataSource 切换、确认错误率未上升
5. 回滚：改回原 zone
6. 复盘：记录切换时长、确认数据一致性
```

### 故障模拟

可以在测试环境中执行以下故障模拟：

```
场景 1：同区域 50% 实例被熔断
  预期：ZonePreferenceFilter 的 same-zone-min-available 触发，流量分散到其他区域

场景 2：某区域的 Nacos 注册中心下线
  预期：该区域实例从可用列表中摘除，流量自动路由到其他区域

场景 3：同区域 DataSource 不可达
  预期：DynamicDataSource.onZoneChanged() -> switchDataSource -> 连接其他区域数据源

场景 4：配置中心改禁用区域
  预期：ZonePreferenceFilter 过滤掉禁用区域实例
```

### 演练记录模板

```
演练编号：DR-2026-Q3
演练目的：验证 beijing-1 到 shanghai-1 的区域切换
演练时间：2026-07-30 10:00 - 10:30
参与人员：张三（执行）、李四（观察）、王五（回退）

时间线：
  10:00  预检查完成
  10:05  配置中心改 zone=shanghai-1
  10:05  观察到 LoadBalancer 开始路由到 shanghai-1 实例
  10:07  DynamicDataSource 切换完成（耗时 2.3s）
  10:08  错误率未上升（0.02%）
  10:15  执行回滚
  10:17  恢复

结论：通过
备注：DataSource 切换耗时 2.3s（预期 < 5s）
```

---

## 场景七：连接池切换的深度分析

### 问题

区域切换时 DynamicDataSource 从 beijing-1 切换到 shanghai-1。切换过程中：

```
切换前：
  beijing-1 连接池: 10 个活跃连接, 20 个空闲连接
  shanghai-1 连接池: 0 个连接

切换命令：
  DynamicDataSource.switchDataSource("shanghai-1")
    -> waitForActiveConnections()  // 等待进行中的操作完成
    -> delegate = shanghai-1 连接池
    -> close() beijing-1 连接池
```

### waitForActiveConnections 的局限

my-xhs 的 DynamicDataSource 有 `waitForActiveConnections()` 机制：

```java
private void waitForActiveConnections() {
    int count = activeConnectionCount.get();
    if (count == 0) return;
    
    long deadline = System.currentTimeMillis() + SWITCH_WAIT_TIMEOUT_SECONDS * 1000L;
    while (System.currentTimeMillis() < deadline) {
        count = activeConnectionCount.get();
        if (count == 0) {
            return;  // 所有活跃连接已完成
        }
        Thread.sleep(500);
    }
    // 超时后强制切换
    log.warn("Wait timeout, {} active connections still running, force switch", count);
}
```

局限一：ConnectionWrapper 只在 DataSource.getConnection() 和 Connection.close() 之间计数。如果在 Spring 事务管理器中，`DataSourceUtils.getConnection()` 可能会复用同一个 Connection，导致 `activeConnectionCount` 不准确。

局限二：等待 30 秒意味着最多阻塞 30 秒。期间新的请求还在进来，`activeConnectionCount` 可能从 1 变成 1（旧的完成，新的进来）。如果切换线程一直等，永远超时。

改进方案：

```java
public void switchDataSource(String newZone, int waitSeconds) {
    int initialCount = activeConnectionCount.get();
    if (initialCount == 0) {
        // 没有活跃连接，立即切换
        doSwitch(newZone);
        return;
    }
    
    // 尝试等待，但超过 waitSeconds 不继续等
    long deadline = System.currentTimeMillis() + Math.min(waitSeconds, SWITCH_MAX_WAIT) * 1000L;
    int lastCount = initialCount;
    int noProgressCycles = 0;
    
    while (System.currentTimeMillis() < deadline) {
        int currentCount = activeConnectionCount.get();
        
        if (currentCount == 0) {
            doSwitch(newZone);
            return;
        }
        
        // 如果 5 秒内活跃连接数没有减少，说明有新连接持续进入
        if (currentCount == lastCount) {
            noProgressCycles++;
            if (noProgressCycles >= 10) {  // 10 * 500ms = 5s
                log.warn("Active connection count not decreasing, force switch");
                break;
            }
        } else {
            noProgressCycles = 0;
        }
        lastCount = currentCount;
        Thread.sleep(500);
    }
    
    // 强制切换
    doSwitch(newZone);
}
```

改进点：如果活跃连接数 5 秒内没有减少（旧的完成、新的进来替换），说明系统正在持续使用数据库，切换不会等到"零活跃连接"的时候。这时应该强制切换。

### shanghai-1 连接池预热

区域切换时，shanghai-1 的连接池从 0 开始。第一个请求需要建连接（耗时 50-100ms）。对于延迟敏感的服务，这会导致 P99 抖动。

HikariCP 支持连接池预热：

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:mysql://shanghai-host:3306/db");
config.setMaximumPoolSize(20);
config.setMinimumIdle(5);

// 在切换前提前创建 shanghai-1 的配置
// 但不在 ZoneContext 切换前创建连接池
// 可以在配置文件中预先配置两个 DataSource，只在切换时改变 delegate

// 更推荐的方案：使用 Map<String, DataSource> 预先配置所有区域的连接池
// 切换时只改变 delegate 指针，所有连接池在启动时已建好
```

my-xhs 的 DynamicDataSource 已经使用了 Map 模式。构造时接收 `Map<String, DataSource>`，每个区域预先创建好连接池。切换时 `switchDataSource(newZone)` 从 Map 中取出目标 DataSource，替换 delegate 指针。切换是纯指针操作，微秒级完成：

```java
public void switchDataSource(String zone) {
    DataSource newDataSource = zoneDataSources.get(zone);
    if (newDataSource != null && newDataSource != delegate) {
        waitForActiveConnections();
        synchronized (mutex) {
            DataSource old = this.delegate;
            this.delegate = newDataSource;  // 纯指针切换
            this.currentZone = zone;
            // 不立即关闭 old——保留在 zoneDataSources 中
            // 如果切回 beijing-1，可以直接用
        }
    }
}
```

这个模式的优点：切换是纯指针操作——微秒级完成。不需要等待连接池初始化——所有连接池在应用启动时就创建好了。

代价：每个区域都有一个连接池，内存开销加倍。对于 3 个区域 * 20 个连接 * 每个连接 ~10MB = ~600MB。在 8GB 的微服务实例中，这不是大问题。

---

## 场景八：跨区域数据同步

### 问题

microsphere-multiactive 本身不解决数据同步问题。但生产环境中区域切换后，数据需要在区域之间同步。

三个层面的数据同步：

### 第一层：应用层缓存

Redis/Memcached 中的缓存数据。区域切换后，新区域的 Redis 中没有旧区域的缓存数据。

解法一：缓存预热。区域切换触发后，慢速预热 Redis 缓存（首批请求穿透到 DB）。

```properties
# spring cache 配置
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000
spring.cache.redis.cache-null-values=false

# 切换后缓存雪崩保护
myxhs.cache.zone-switch-protection.enabled=true  
myxhs.cache.zone-switch-protection.rate-limit=100  # 每 10s 最多 100 个缓存穿透
```

解法二：双读双写。切换窗口期内读两个 Redis（优先读新区域，穿透读旧区域），写两个 Redis。

### 第二层：数据库

数据库数据通过主从复制同步。区域切换后，目标区域的数据库需要是主数据库。

典型的 MySQL 同步方案：

```
正常：beijing-1 数据库主，shanghai-1 数据库从（通过 DTS/Canal 同步）
切换：shanghai-1 数据库升级为主（同步延迟消化的前提）
```

### 第三层：消息队列

区域切换时，消息队列中积压的消息如果还在旧区域，新区域的消费者可能消费不到。

解法：消息队列部署为跨区域集群（如 RocketMQ 的 DCN 同步）。

---

## 配置清单：生产部署总检

### 基础配置

```yaml
myxhs:
  availability:
    zone:
      enabled: true
      locator:
        fast-fail: false
        timeout: 3000
    preference:
      enabled: true
      filter:
        order: 10
      upstream:
        zone-ready-percentage: 80      # 生产建议放宽到 80%
        same-zone-min-available: 5
        disabled-zone:                 # 故障时填写
```

### 服务器配置

| 组件 | 配置项 | 示例值 |
|------|--------|--------|
| HikariCP | maximumPoolSize | 20 |
| HikariCP | minimumIdle | 5 |
| HikariCP | connectionTimeout | 5000 |
| Spring Boot 优雅关闭 | server.shutdown | graceful |
| Spring Boot 优雅关闭 | spring.lifecycle.timeout-per-shutdown-phase | 30s |
| Resilience4j | CircuitBreaker slidingWindowSize | 10 |
| Resilience4j | CircuitBreaker failureRateThreshold | 50 |
| Resilience4j | CircuitBreaker waitDurationInOpenState | 30s |

### 监控指标

| 指标 | 告警阈值 |
|------|---------|
| zone preference hit rate | < 50% |
| cross-zone request ratio | > 部署比例 + 20% |
| datasource switch duration | > 5s |
| active connection during switch | > 0 持续 10s 以上 |
| zone context change count | 异常变更 |

---

## 总结

| 场景 | 核心问题 | 生产级别方案 |
|------|---------|------------|
| Gateway 区域路由 | Gateway 也走 LoadBalancer，但没有跨区域 fallback | LoadBalancer 重试 + 降级，不做跨区域 fallback |
| 优雅切换 | 切换时进行中的请求怎么处理 | waitForActiveConnections（my-xhs 已有）+ 改进版超时检测 |
| 熔断配合 | ZonePreferenceFilter 不感知熔断 | Layered CircuitBreaker（实例级 + 区域级手动 disabled-zone）|
| 切换清单 | 切换前要检查什么 | 8 项检查（服务、DB、Redis、MQ、同步延迟、错误率、活跃连接） |
| 监控 | 切换后怎么知道系统状态 | 6 个 Grafana 面板 + 7 个告警指标 |
| 演练 | 怎么验证切换能力 | 季度演练 + 4 个故障模拟场景 |
| 连接池切换 | 切换时连接怎么迁移 | Map 模式预建连接池，切换纯指针操作 |
| 数据同步 | 切换后数据在新区域吗 | 三层：缓存双读写 -> DB 主从切换 -> MQ 跨区域集群 |
